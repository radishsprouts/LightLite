package io.github.radishsprouts.lightlite.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.radishsprouts.lightlite.scan.Region;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Collection;

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
    public void upload(Region region, MeshBuilder mesh) {
        release(region);
        int vertices = mesh.vertexCount;
        if (vertices == 0) return;
        region.cpuPositions = Arrays.copyOf(mesh.positions, vertices * 3);
        region.cpuColors = Arrays.copyOf(mesh.colors, vertices);
        region.quads = vertices >> 2;
    }

    @Override
    public void release(Region region) {
        region.cpuPositions = null;
        region.cpuColors = null;
        region.quads = 0;
        region.gpuBytes = 0;
    }

    @Override
    public int submit(LevelRenderContext context, Collection<Region> regions) {
        Vec3 camera = context.levelState().cameraRenderState.pos;
        PoseStack poseStack = context.poseStack();
        int calls = 0;
        for (Region region : regions) {
            float[] positions = region.cpuPositions;
            int[] colors = region.cpuColors;
            if (positions == null || colors == null) continue;
            poseStack.pushPose();
            poseStack.translate(region.originX() - camera.x, -camera.y, region.originZ() - camera.z);
            context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> {
                for (int v = 0, p = 0; v < colors.length; v++, p += 3) {
                    buffer.addVertex(pose, positions[p], positions[p + 1], positions[p + 2]).setColor(colors[v]);
                }
            });
            poseStack.popPose();
            calls++;
        }
        return calls;
    }

    @Override
    public void close() {
    }
}
