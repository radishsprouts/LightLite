package io.github.radishsprouts.lightlite.mixin;

import io.github.radishsprouts.lightlite.scan.OverlayManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Listens to the same notifications vanilla uses to rebuild chunk meshes. These
 * {@code ClientLevel} methods are not overwritten by Sodium, unlike their
 * {@code LevelRenderer} counterparts.
 */
@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void lightlite$onBlockUpdated(BlockPos pos, BlockState old, BlockState current, int updateFlags, CallbackInfo ci) {
        OverlayManager.get().onBlockChanged(pos);
    }

    @Inject(method = "setSectionDirtyWithNeighbors", at = @At("HEAD"))
    private void lightlite$onSectionDirty(int chunkX, int chunkY, int chunkZ, CallbackInfo ci) {
        OverlayManager.get().onSectionRangeDirty(chunkX - 1, chunkY - 1, chunkZ - 1, chunkX + 1, chunkY + 1, chunkZ + 1);
    }

    @Inject(method = "setSectionRangeDirty", at = @At("HEAD"))
    private void lightlite$onSectionRangeDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CallbackInfo ci) {
        OverlayManager.get().onSectionRangeDirty(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
