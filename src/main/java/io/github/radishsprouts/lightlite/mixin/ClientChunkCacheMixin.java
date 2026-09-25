package io.github.radishsprouts.lightlite.mixin;

import io.github.radishsprouts.lightlite.scan.OverlayManager;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Light recomputed on the client (e.g. after placing a torch) reaches this method
 * once per changed section.
 */
@Mixin(ClientChunkCache.class)
abstract class ClientChunkCacheMixin {
    @Inject(method = "onLightUpdate", at = @At("HEAD"))
    private void lightlite$onLightUpdate(LightLayer layer, SectionPos pos, CallbackInfo ci) {
        OverlayManager.get().onSectionDirty(pos.x(), pos.y(), pos.z());
    }
}
