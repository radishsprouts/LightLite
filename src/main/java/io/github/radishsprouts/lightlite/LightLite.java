package io.github.radishsprouts.lightlite;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.radishsprouts.lightlite.compat.IrisCompat;
import io.github.radishsprouts.lightlite.config.LightLiteConfig;
import io.github.radishsprouts.lightlite.render.OverlayRenderer;
import io.github.radishsprouts.lightlite.scan.OverlayManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LightLite implements ClientModInitializer {
    public static final String MOD_ID = "lightlite";
    public static final Logger LOGGER = LoggerFactory.getLogger("LightLite");

    private static KeyMapping toggleKey;
    private static KeyMapping modeKey;

    @Override
    public void onInitializeClient() {
        LightLiteConfig.load();
        IrisCompat.init();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.lightlite.toggle", InputConstants.KEY_F9, category));
        modeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.lightlite.mode", InputConstants.KEY_F10, category));

        ClientTickEvents.END_CLIENT_TICK.register(LightLite::onEndTick);
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) ->
                OverlayManager.get().onChunkLoaded(chunk.getPos().x(), chunk.getPos().z()));
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
                OverlayManager.get().onChunkUnloaded(chunk.getPos().x(), chunk.getPos().z()));
        // END_MAIN runs after vanilla closed its main render pass (26.3 keeps it open during terrain events)
        LevelRenderEvents.END_MAIN.register(OverlayRenderer.get()::onEndMain);
        LevelRenderEvents.COLLECT_SUBMITS.register(OverlayRenderer.get()::onCollectSubmits);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> OverlayRenderer.get().close());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> LightLiteCommand.register(dispatcher));
    }

    private static void onEndTick(Minecraft client) {
        LightLiteConfig cfg = LightLiteConfig.get();
        while (toggleKey.consumeClick()) {
            cfg.enabled = !cfg.enabled;
            LightLiteConfig.save();
            notify(client, Component.translatable(cfg.enabled ? "lightlite.message.enabled" : "lightlite.message.disabled"));
        }
        while (modeKey.consumeClick()) {
            cfg.mode = cfg.mode == LightLiteConfig.Mode.TILE ? LightLiteConfig.Mode.CROSS : LightLiteConfig.Mode.TILE;
            LightLiteConfig.save();
            OverlayManager.get().invalidateMeshes();
            notify(client, Component.translatable("lightlite.message.mode",
                    Component.translatable("lightlite.mode." + cfg.mode.name().toLowerCase(java.util.Locale.ROOT))));
        }
        OverlayManager.get().tick(client);
    }

    private static void notify(Minecraft client, Component message) {
        if (client.player != null) client.player.sendOverlayMessage(message);
    }
}
