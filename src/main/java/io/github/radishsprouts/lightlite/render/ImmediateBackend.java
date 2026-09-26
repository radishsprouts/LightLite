package io.github.radishsprouts.lightlite.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.radishsprouts.lightlite.scan.Region;
import io.github.radishsprouts.lightlite.scan.RegionMesh;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

/**
 * Compatibility path: keeps meshes on the CPU and submits them through vanilla's
 * submit collector every frame. Costs more CPU than {@link RetainedBackend}, but uses only
 * vanilla render types, so shader mods can process it.
 */
final class ImmediateBackend implements MeshBackend {
    @Override
    public String name() {
        return "immediate";
    }

    @Override
    public void upload(RegionMesh mesh, MeshBuilder builder) {
        release(mesh);
        int vertices = builder.vertexCount;
        if (vertices == 0) return;
        mesh.cpuPositions = Arrays.copyOf(builder.positions, vertices * 3);
        mesh.cpuColors = Arrays.copyOf(builder.colors, vertices);
        mesh.quads = vertices >> 2;
    }

    @Override
    public void release(RegionMesh mesh) {
        mesh.cpuPositions = null;
        mesh.cpuColors = null;
        mesh.quads = 0;
        mesh.gpuBytes = 0;
    }

    @Override
    public int submit(LevelRenderContext context, DrawList draws) {
        Vec3 camera = context.levelState().cameraRenderState.pos;
        PoseStack poseStack = context.poseStack();
        int calls = 0;
        int i = 0;
        while (i < draws.size) {
            Region region = draws.regions[i];
            int end = i;
            while (end < draws.size && draws.regions[end] == region) end++;
            // Snapshot this region's ranges for the deferred callback
            RegionMesh[] meshes = Arrays.copyOfRange(draws.meshes, i, end);
            int[] first = Arrays.copyOfRange(draws.firstQuad, i, end);
            int[] count = Arrays.copyOfRange(draws.quadCount, i, end);
            poseStack.pushPose();
            poseStack.translate(region.originX() - camera.x, -camera.y, region.originZ() - camera.z);
            context.submitNodeCollector().submitCustomGeometry(poseStack, LightLitePipelines.OVERLAY_TYPE, (pose, buffer) -> {
                for (int r = 0; r < meshes.length; r++) {
                    float[] positions = meshes[r].cpuPositions;
                    int[] colors = meshes[r].cpuColors;
                    if (positions == null || colors == null) continue;
                    int v = first[r] * 4;
                    int vEnd = v + count[r] * 4;
                    for (int p = v * 3; v < vEnd; v++, p += 3) {
                        buffer.addVertex(pose, positions[p], positions[p + 1], positions[p + 2]).setColor(colors[v]);
                    }
                }
            });
            poseStack.popPose();
            calls++;
            i = end;
        }
        return calls;
    }

    @Override
    public void close() {
    }
}
