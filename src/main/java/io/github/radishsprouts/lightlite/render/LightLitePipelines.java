package io.github.radishsprouts.lightlite.render;

//? if >=26.3 {
/*import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
*///?} elif >=26.2 {
/*import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.BindGroupLayouts;
*///?} else {
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
//?}
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.github.radishsprouts.lightlite.mixin.RenderTypeInvoker;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * LightLite's own copy of vanilla's {@code debug_quads} pipeline (translucent position-color
 * quads, depth-tested, not written to depth, no culling).
 *
 * <p>It exists so shader mods can tell the overlay apart: Iris maps only the pipelines it knows
 * onto shader pack programs, and draws everything else with the vanilla shader into the main
 * framebuffer, which the pack's final pass then overwrites. {@link
 * io.github.radishsprouts.lightlite.compat.IrisCompat} assigns this pipeline to the pack's
 * basic program instead.
 */
public final class LightLitePipelines {
    public static final RenderPipeline OVERLAY = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("lightlite", "pipeline/overlay"))
            //? if >=26.3 {
            /*.withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            *///?} elif >=26.2 {
            /*.withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            *///?} else {
            .withUniform("Globals", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            //?}
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .build();

    /** The same pipeline as a render type, for the submit-collector path. */
    public static final RenderType OVERLAY_TYPE = RenderTypeInvoker.lightlite$create(
            "lightlite_overlay", RenderSetup.builder(OVERLAY).sortOnUpload().createRenderSetup());

    private LightLitePipelines() {
    }
}
