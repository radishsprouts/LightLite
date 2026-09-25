package io.github.radishsprouts.lightlite;

import com.mojang.brigadier.CommandDispatcher;
import io.github.radishsprouts.lightlite.config.LightLiteConfig;
import io.github.radishsprouts.lightlite.render.OverlayRenderer;
import io.github.radishsprouts.lightlite.scan.OverlayManager;
import io.github.radishsprouts.lightlite.util.PerfStats;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Locale;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * {@code /lightlite stats} prints LightLite's own cost, the main number to compare
 * against other overlay mods. {@code /lightlite reload} re-reads the JSON config.
 */
final class LightLiteCommand {
    private LightLiteCommand() {
    }

    static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("lightlite")
                .then(literal("stats").executes(ctx -> {
                    for (String line : statsLines()) ctx.getSource().sendFeedback(Component.literal(line));
                    return 1;
                }))
                .then(literal("resetstats").executes(ctx -> {
                    PerfStats.reset();
                    ctx.getSource().sendFeedback(Component.translatable("lightlite.message.stats_reset"));
                    return 1;
                }))
                .then(literal("reload").executes(ctx -> {
                    LightLiteConfig.load();
                    OverlayRenderer.get().resetBackend();
                    OverlayManager.get().rescanAll();
                    ctx.getSource().sendFeedback(Component.translatable("lightlite.message.reloaded"));
                    return 1;
                })));
    }

    static String[] statsLines() {
        return new String[] {
                "[LightLite] backend=" + PerfStats.backend
                        + " active=" + OverlayManager.get().isActive(),
                String.format(Locale.ROOT, "scan: %.3f ms/tick avg, %.3f ms peak, %d sections scanned total",
                        PerfStats.scanMsPerTick, PerfStats.scanMsPeak, PerfStats.sectionsScanned),
                String.format(Locale.ROOT, "render: %.1f us/frame (mesh rebuild %.1f us), %d draw calls",
                        PerfStats.renderUsPerFrame, PerfStats.rebuildUsPerFrame, PerfStats.drawCalls),
                String.format(Locale.ROOT, "cache: %d markers, %d sections (%d pending), %d regions",
                        PerfStats.markers, PerfStats.trackedSections, PerfStats.pendingSections, PerfStats.regions),
                String.format(Locale.ROOT, "gpu: %.1f KiB resident, %.1f KiB uploaded total",
                        PerfStats.gpuBytes / 1024.0, PerfStats.uploadedBytes / 1024.0),
        };
    }
}
