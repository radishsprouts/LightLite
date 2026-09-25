package io.github.radishsprouts.lightlite.render;

import io.github.radishsprouts.lightlite.LightLite;
import io.github.radishsprouts.lightlite.config.LightLiteConfig;
import io.github.radishsprouts.lightlite.scan.OverlayManager;
import io.github.radishsprouts.lightlite.scan.Region;
import io.github.radishsprouts.lightlite.util.PerfStats;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Collection;

/**
 * Glue between the marker cache and the active mesh backend. Rebuilds only regions whose
 * markers changed, with a small per-frame cap so a burst of changes never causes a spike.
 */
public final class OverlayRenderer {
    private static final OverlayRenderer INSTANCE = new OverlayRenderer();
    private static final int MAX_REBUILDS_PER_FRAME = 8;

    private final MeshBuilder builder = new MeshBuilder();
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
        OverlayManager manager = OverlayManager.get();
        if (backend != null) {
            for (Region region : manager.regions()) {
                backend.release(region);
                region.dirty = true;
            }
            backend.close();
            backend = null;
        }
    }

    public void release(Region region) {
        if (backend != null) backend.release(region);
    }

    /** {@code LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN}: direct GPU drawing. */
    public void onAfterTranslucentTerrain(LevelRenderContext context) {
        if (backend() instanceof RetainedBackend) render(context);
    }

    /** {@code LevelRenderEvents.COLLECT_SUBMITS}: vanilla submit collector. */
    public void onCollectSubmits(LevelRenderContext context) {
        if (backend() instanceof ImmediateBackend) render(context);
    }

    private void render(LevelRenderContext context) {
        OverlayManager manager = OverlayManager.get();
        if (!manager.isActive()) return;
        long start = System.nanoTime();
        MeshBackend current = backend();
        Collection<Region> regions = manager.regions();
        try {
            rebuildDirty(manager, regions, current);
            long drawStart = System.nanoTime();
            PerfStats.recordRebuild(drawStart - start);
            PerfStats.drawCalls = current.submit(context, regions);
        } catch (RuntimeException e) {
            if (current instanceof RetainedBackend) {
                LightLite.LOGGER.error("Retained overlay rendering failed; falling back to the immediate backend", e);
                retainedFailed = true;
                resetBackend();
            } else {
                throw e;
            }
        }
        PerfStats.recordRender(System.nanoTime() - start);
    }

    private void rebuildDirty(OverlayManager manager, Collection<Region> regions, MeshBackend current) {
        LightLiteConfig cfg = LightLiteConfig.get();
        int rebuilt = 0;
        long gpuBytes = 0;
        for (Region region : regions) {
            if (region.dirty && rebuilt < MAX_REBUILDS_PER_FRAME) {
                builder.build(region, manager, cfg);
                current.upload(region, builder);
                region.dirty = false;
                rebuilt++;
            }
            gpuBytes += region.gpuBytes;
        }
        PerfStats.gpuBytes = gpuBytes;
    }

    public void close() {
        if (backend != null) {
            for (Region region : OverlayManager.get().regions()) backend.release(region);
            backend.close();
            backend = null;
        }
    }
}
