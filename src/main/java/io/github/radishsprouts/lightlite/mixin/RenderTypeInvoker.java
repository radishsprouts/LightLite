package io.github.radishsprouts.lightlite.mixin;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** {@code RenderType.create} is package-private; LightLite needs one render type of its own. */
@Mixin(RenderType.class)
public interface RenderTypeInvoker {
    @Invoker("create")
    static RenderType lightlite$create(String name, RenderSetup setup) {
        throw new AssertionError();
    }
}
