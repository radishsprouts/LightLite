package io.github.radishsprouts.lightlite.render;

//? if >=26.3 {
/*import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import java.util.Optional;
*///?} elif >=26.2 {
/*import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import java.util.Optional;
*///?} else {
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.OptionalInt;
//?}
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.radishsprouts.lightlite.scan.Region;
import io.github.radishsprouts.lightlite.scan.RegionMesh;
import io.github.radishsprouts.lightlite.util.PerfStats;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;

/**
 * Default path: each region's quads live in a GPU vertex buffer that is uploaded only when
 * the region changes. A frame costs one render pass with one draw call per visible range,
 * using {@link LightLitePipelines#OVERLAY} and vanilla's shared quad index buffer.
 */
final class RetainedBackend implements MeshBackend {
    /** POSITION_COLOR: 3 floats + 4 unsigned bytes (RGBA). */
    private static final int VERTEX_SIZE = 16;
    private static final Vector4f WHITE = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f NO_OFFSET = new Vector3f();
    private static final Matrix4f IDENTITY = new Matrix4f();

    private GpuBufferSlice[] transforms = new GpuBufferSlice[64];

    @Override
    public String name() {
        return "retained";
    }

    @Override
    public void upload(RegionMesh target, MeshBuilder mesh) {
        release(target);
        int vertices = mesh.vertexCount;
        if (vertices == 0) return;

        long bytes = (long) vertices * VERTEX_SIZE;
        ByteBuffer data = MemoryUtil.memAlloc((int) bytes);
        try {
            // Relative ByteBuffer puts are JIT intrinsics; MemoryUtil.memPut* goes through FFM per call
            float[] pos = mesh.positions;
            int[] colors = mesh.colors;
            for (int v = 0, p = 0; v < vertices; v++, p += 3) {
                int argb = colors[v];
                data.putFloat(pos[p]).putFloat(pos[p + 1]).putFloat(pos[p + 2])
                        .put((byte) (argb >>> 16)).put((byte) (argb >>> 8)).put((byte) argb).put((byte) (argb >>> 24));
            }
            data.flip();
            target.gpu = RenderSystem.getDevice().createBuffer(() -> "LightLite region", GpuBuffer.USAGE_VERTEX, data);
            target.quads = vertices >> 2;
            target.gpuBytes = bytes;
            PerfStats.uploadedBytes += bytes;
        } finally {
            MemoryUtil.memFree(data);
        }
    }

    @Override
    public void release(RegionMesh mesh) {
        if (mesh.gpu instanceof GpuBuffer buffer) {
            buffer.close();
        }
        mesh.gpu = null;
        mesh.quads = 0;
        mesh.gpuBytes = 0;
    }

    @Override
    public int submit(LevelRenderContext context, DrawList draws) {
        if (draws.size == 0) return 0;

        Vec3 camera = context.levelState().cameraRenderState.pos;
        //? if >=26.2 {
        /*Matrix4f base = RenderSystem.getModelViewMatrixCopy();
        *///?} else {
        Matrix4f base = new Matrix4f(RenderSystem.getModelViewMatrix());
        //?}
        base.mul(context.poseStack().last().pose());

        // Uniform writes must happen outside the render pass; one transform per region
        if (transforms.length < draws.size) transforms = new GpuBufferSlice[draws.regions.length];
        Region previous = null;
        GpuBufferSlice current = null;
        for (int i = 0; i < draws.size; i++) {
            Region region = draws.regions[i];
            if (region != previous) {
                Matrix4f modelView = new Matrix4f(base).translate(
                        (float) (region.originX() - camera.x),
                        (float) -camera.y,
                        (float) (region.originZ() - camera.z));
                current = RenderSystem.getDynamicUniforms().writeTransform(modelView, WHITE, NO_OFFSET, IDENTITY);
                previous = region;
            }
            transforms[i] = current;
        }

        //? if >=26.2 {
        /*RenderSystem.AutoStorageIndexBuffer quadIndices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        *///?} else {
        RenderSystem.AutoStorageIndexBuffer quadIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        //?}
        GpuBuffer indices = quadIndices.getBuffer(draws.maxQuads * 6);
        var color = target.getColorTextureView();
        var depth = target.getDepthTextureView();
        if (color == null) return 0;

        //? if >=26.3 {
        /*CompiledRenderPipeline pipeline = RenderSystem.getCompiledPipelineNullable(LightLitePipelines.OVERLAY);
        if (pipeline == null) return 0;
        *///?}

        int calls = 0;
        //? if >=26.2 {
        /*try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "LightLite overlay", color, Optional.empty(), depth, OptionalDouble.empty())) {
        *///?} else {
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "LightLite overlay", color, OptionalInt.empty(), depth, OptionalDouble.empty())) {
        //?}
            //? if >=26.3 {
            /*pass.setPipeline(pipeline);
            *///?} else {
            pass.setPipeline(LightLitePipelines.OVERLAY);
            //?}
            RenderSystem.bindDefaultUniforms(pass);
            pass.setIndexBuffer(indices, quadIndices.type());
            GpuBufferSlice boundTransform = null;
            RegionMesh boundMesh = null;
            for (int i = 0; i < draws.size; i++) {
                RegionMesh mesh = draws.meshes[i];
                if (!(mesh.gpu instanceof GpuBuffer vertices)) continue;
                if (transforms[i] != boundTransform) {
                    pass.setUniform("DynamicTransforms", transforms[i]);
                    boundTransform = transforms[i];
                }
                if (mesh != boundMesh) {
                    //? if >=26.2 {
                    /*pass.setVertexBuffer(0, vertices.slice());
                    *///?} else {
                    pass.setVertexBuffer(0, vertices);
                    //?}
                    boundMesh = mesh;
                }
                int baseVertex = draws.firstQuad[i] * 4;
                int indexCount = draws.quadCount[i] * 6;
                //? if >=26.2 {
                /*pass.drawIndexed(indexCount, 1, 0, baseVertex, 0);
                *///?} else {
                pass.drawIndexed(baseVertex, 0, indexCount, 1);
                //?}
                calls++;
            }
        }
        return calls;
    }

    @Override
    public void close() {
    }
}
