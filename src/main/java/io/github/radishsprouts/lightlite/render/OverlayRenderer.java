package io.github.radishsprouts.lightlite.render;

import io.github.radishsprouts.lightlite.LightLite;
import io.github.radishsprouts.lightlite.config.LightLiteConfig;
import io.github.radishsprouts.lightlite.scan.OverlayManager;
import io.github.radishsprouts.lightlite.scan.Region;
import io.github.radishsprouts.lightlite.scan.RegionMesh;
import io.github.radishsprouts.lightlite.util.PerfStats;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

/**
 * Glue between the marker cache and the active mesh backend.
 *
 * <p>Per frame it only decides what to draw: chunk columns outside the view frustum are skipped,
 * columns within {@code gridDistance} use the per-block tile mesh (readable grid), farther ones the
 * merged mesh (far fewer vertices; the gaps between tiles are sub-pixel there anyway). Meshes are rebuilt
 * only when their region's markers changed, with a small per-frame cap.
 */
public final class OverlayRenderer {
    private static final OverlayRenderer INSTANCE = new OverlayRenderer();
    private static final int MAX_REBUILDS_PER_FRAME = 8;
    /** Near meshes are freed once every column of the region is this much beyond the grid distance. */
    private static final double NEAR_RELEASE_MARGIN = 32.0;

    private final MeshBuilder builder = new MeshBuilder();
    private final DrawList draws = new DrawList();
    private MeshBackend backend;
    private boolean retainedFailed;

    public static OverlayRenderer get() {
        return INSTANCE;
    }

    private MeshBackend backend() {
        if (backend == null) {
            backend = createBackend(LightLiteConfig.get().backend);
            PerfStats.backend = backend.name();
            LightLite.LOGGER.info("Using {} overlay backend", backend.name());
        }
        return backend;
    }

    private MeshBackend createBackend(LightLiteConfig.Backend choice) {
        boolean retained = switch (choice) {
            case RETAINED -> true;
            case IMMEDIATE -> false;
            // Shader packs replace the world pipeline; stay on vanilla render types there
            case AUTO -> !FabricLoader.getInstance().isModLoaded("iris");
        };
        return retained && !retainedFailed ? new RetainedBackend() : new ImmediateBackend();
    }

    /** Called when the backend setting changes. */
    public void resetBackend() {
        if (backend != null) {
            for (Region region : OverlayManager.get().regions()) {
                release(region);
                region.markDirty();
            }
            backend.close();
            backend = null;
        }
    }

    /** Name of the backend in use, or {@code null} before the first frame. */
    public String backendName() {
        return backend != null ? backend.name() : null;
    }

    public void release(Region region) {
        if (backend != null) {
            backend.release(region.far);
            backend.release(region.near);
        }
    }

    /** {@code LevelRenderEvents.END_MAIN}: direct GPU drawing in its own render pass. */
    public void onEndMain(LevelRenderContext context) {
        if (backend() instanceof RetainedBackend) render(context);
    }

    /** {@code LevelRenderEvents.COLLECT_SUBMITS}: vanilla submit collector. */
    public void onCollectSubmits(LevelRenderContext context) {
        if (backend() instanceof ImmediateBackend) render(context);
    }

    private void render(LevelRenderContext context) {
        OverlayManager manager = OverlayManager.get();
        if (!manager.isActive()) {
            PerfStats.drawCalls = 0;
            PerfStats.drawnQuads = 0;
            return;
        }
        long start = System.nanoTime();
        MeshBackend current = backend();
        try {
            collect(context, manager, current);
            long drawStart = System.nanoTime();
            PerfStats.recordRebuild(drawStart - start);
            PerfStats.drawCalls = current.submit(context, draws);
        } catch (RuntimeException e) {
            if (current instanceof RetainedBackend) {
                LightLite.LOGGER.error("Retained overlay rendering failed; falling back to the immediate backend", e);
                retainedFailed = true;
                resetBackend();
            } else {
                throw e;
            }
        } finally {
            draws.clear();
        }
        PerfStats.recordRender(System.nanoTime() - start);
    }

    /** Rebuilds what changed and fills {@link #draws} with the visible column ranges. */
    private void collect(LevelRenderContext context, OverlayManager manager, MeshBackend current) {
        LightLiteConfig cfg = LightLiteConfig.get();
        boolean tiles = cfg.mode == LightLiteConfig.Mode.TILE;
        double gridDistance = cfg.gridDistance;
        var camera = context.levelState().cameraRenderState;
        Vec3 cam = camera.pos;
        Frustum frustum = camera.cullFrustum;
        Collection<Region> regions = manager.regions();

        int rebuilds = 0;
        long gpuBytes = 0;
        int culled = 0;
        for (Region region : regions) {
            RegionMesh far = region.far;
            if (far.dirty && rebuilds < MAX_REBUILDS_PER_FRAME) {
                builder.build(region, far, manager, cfg, tiles);
                current.upload(far, builder);
                far.dirty = false;
                rebuilds++;
            }
            gpuBytes += far.gpuBytes;

            double nearest = Double.MAX_VALUE;
            for (int c = 0; c < RegionMesh.COLUMNS; c++) {
                if (far.columnQuads[c] == 0) continue;
                int x0 = region.originX() + ((c & 3) << 4);
                int z0 = region.originZ() + ((c >> 2) << 4);
                double dist = horizontalDistance(cam, x0, z0);
                nearest = Math.min(nearest, dist);
                if (frustum != null && !frustum.isVisible(new AABB(x0, far.columnMinY[c], z0, x0 + 16, far.columnMaxY[c] + 1.1, z0 + 16))) {
                    culled++;
                    continue;
                }
                RegionMesh mesh = far;
                if (tiles && dist <= gridDistance) {
                    RegionMesh near = region.near;
                    if (near.dirty && rebuilds < MAX_REBUILDS_PER_FRAME) {
                        builder.build(region, near, manager, cfg, false);
                        current.upload(near, builder);
                        near.dirty = false;
                        rebuilds++;
                    }
                    // Until the near mesh is current, the merged one shows the same markers
                    if (!near.dirty && near.hasData()) mesh = near;
                }
                if (mesh.columnQuads[c] > 0) draws.add(region, mesh, mesh.columnStart[c], mesh.columnQuads[c]);
            }

            if (region.near.hasData() && (!tiles || nearest > gridDistance + NEAR_RELEASE_MARGIN)) {
                current.release(region.near);
                region.near.dirty = true;
            }
            gpuBytes += region.near.gpuBytes;
        }
        PerfStats.gpuBytes = gpuBytes;
        PerfStats.culledColumns = culled;
        PerfStats.drawnQuads = sumQuads();
    }

    private int sumQuads() {
        int sum = 0;
        for (int i = 0; i < draws.size; i++) sum += draws.quadCount[i];
        return sum;
    }

    /** Distance from the camera to the nearest point of a 16x16 column, ignoring height. */
    private static double horizontalDistance(Vec3 cam, int x0, int z0) {
        double dx = Math.max(Math.max(x0 - cam.x, 0), cam.x - (x0 + 16));
        double dz = Math.max(Math.max(z0 - cam.z, 0), cam.z - (z0 + 16));
        return Math.sqrt(dx * dx + dz * dz);
    }

    public void close() {
        if (backend != null) {
            for (Region region : OverlayManager.get().regions()) release(region);
            backend.close();
            backend = null;
        }
    }
}
