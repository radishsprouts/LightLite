package io.github.radishsprouts.lightlite.test.mixin;

import io.github.radishsprouts.lightlite.test.FrameTimer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftTimingMixin {
    @Inject(method = "renderFrame", at = @At("HEAD"))
    private void lightliteTest$frameBegin(boolean advanceGameTime, CallbackInfo ci) {
        FrameTimer.frameBegin();
    }

    @Inject(method = "renderFrame", at = @At("RETURN"))
    private void lightliteTest$frameEnd(boolean advanceGameTime, CallbackInfo ci) {
        FrameTimer.frameEnd();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void lightliteTest$tickBegin(CallbackInfo ci) {
        FrameTimer.tickBegin();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void lightliteTest$tickEnd(CallbackInfo ci) {
        FrameTimer.tickEnd();
    }
}
