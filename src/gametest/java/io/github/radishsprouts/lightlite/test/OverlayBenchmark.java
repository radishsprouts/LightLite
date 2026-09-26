package io.github.radishsprouts.lightlite.test;

import io.github.radishsprouts.lightlite.config.LightLiteConfig;
import io.github.radishsprouts.lightlite.scan.OverlayManager;
import io.github.radishsprouts.lightlite.util.PerfStats;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

/**
 * Relative cost benchmark, enabled with {@code -Dlightlite.bench=<target>}.
 *
 * <p>Client gametests advance exactly one tick per rendered frame, so the wall time of a fixed
 * number of ticks measures "one frame + one tick" including every mod's overlay work. The camera
 * turns a full circle while measuring. Targets: {@code none}, {@code lightlite},
 * {@code lightoverlay}, {@code lighty}, {@code minihud}. Other overlay mods must be present in
 * {@code bench-mods/<minecraft version>/<target>/}.
 *
 * <p>This runs on software rendering, so GPU work shows up as CPU time. Treat results as a
 * relative comparison only.
 */
@SuppressWarnings("UnstableApiUsage")
public class OverlayBenchmark implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        String target = System.getProperty("lightlite.bench");
        if (target == null) return;
        int ticks = Integer.getInteger("lightlite.bench.ticks", 600);
        int warmup = Integer.getInteger("lightlite.bench.warmup", 300);
        int range = Integer.getInteger("lightlite.bench.range", 0);

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            //? if >=26.2 {
            /*singleplayer.getConnection().waitForChunksRender();
            *///?} else {
            singleplayer.getClientLevel().waitForChunksRender();
            //?}
            singleplayer.getServer().runCommand("tp @a 0 -60 0 0 25");

            context.runOnClient(client -> {
                LightLiteConfig cfg = LightLiteConfig.get();
                cfg.enabled = target.equals("lightlite");
                if (range > 0) {
                    cfg.horizontalRange = range;
                    cfg.verticalRange = Math.min(range, LightLiteConfig.MAX_VERTICAL_RANGE);
                }
                enableOther(target);
            });
            context.waitTicks(warmup);
            PerfStats.reset();

            FrameTimer.start(ticks * 4);
            for (int i = 0; i < ticks; i++) {
                float yaw = i * 360.0F / ticks;
                context.runOnClient(client -> {
                    if (client.player != null) client.player.setYRot(yaw);
                });
                context.waitTick();
            }
            FrameTimer.recording = false;
            long[] frames = java.util.Arrays.copyOf(FrameTimer.frames, FrameTimer.frameCount);
            long[] tickTimes = java.util.Arrays.copyOf(FrameTimer.ticks, FrameTimer.tickCount);

            int markers = context.computeOnClient(client -> OverlayManager.get().countMarkers(
                    Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,
                    Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 0));
            writeResult(target, frames, tickTimes, markers, range);
            // Visual proof that the overlay under test was actually on
            context.takeScreenshot("bench-" + target + (range > 0 ? "-" + range : ""));
        }
    }

    private static String stats(String name, long[] nanos) {
        if (nanos.length == 0) return String.format(Locale.ROOT, "\"%sAvgMs\":0,\"%sP50Ms\":0,\"%sP99Ms\":0", name, name, name);
        long[] sorted = nanos.clone();
        Arrays.sort(sorted);
        double sum = 0;
        for (long n : sorted) sum += n;
        return String.format(Locale.ROOT, "\"%sAvgMs\":%.3f,\"%sP50Ms\":%.3f,\"%sP99Ms\":%.3f",
                name, sum / sorted.length / 1e6,
                name, sorted[sorted.length / 2] / 1e6,
                name, sorted[Math.min(sorted.length - 1, (int) (sorted.length * 0.99))] / 1e6);
    }

    /** Turns on another mod's overlay through its own toggle, as its hotkey would. */
    private static void enableOther(String target) {
        try {
            switch (target) {
                case "lightoverlay" -> {
                    Class<?> handler = Class.forName("net.lugo.lightoverlay.OverlayHandler");
                    Field active = handler.getDeclaredField("isActive");
                    active.setAccessible(true);
                    if (!active.getBoolean(null)) handler.getMethod("toggle").invoke(null);
                }
                case "lighty" -> {
                    Class<?> smach = Class.forName("dev.schmarrn.lighty.overlaystate.SMACH");
                    if (!(boolean) smach.getMethod("isEnabled").invoke(null)) smach.getMethod("toggle").invoke(null);
                }
                case "minihud" -> {
                    setMalilibBoolean("fi.dy.masa.minihud.config.Configs$Generic", "MAIN_RENDERING_TOGGLE");
                    setMalilibBoolean("fi.dy.masa.minihud.config.RendererToggle", "OVERLAY_LIGHT_LEVEL");
                }
                default -> {
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not enable overlay for " + target, e);
        }
    }

    private static void setMalilibBoolean(String owner, String field) throws ReflectiveOperationException {
        Object option = Class.forName(owner).getField(field).get(null);
        Method setter = option.getClass().getMethod("setBooleanValue", boolean.class);
        setter.invoke(option, true);
    }

    private static void writeResult(String target, long[] frames, long[] tickTimes, int markers, int range) {
        String json = String.format(Locale.ROOT,
                "{\"target\":\"%s\",\"minecraft\":\"%s\",\"range\":%d,\"frames\":%d,%s,%s,"
                        + "\"lightliteMarkers\":%d,\"lightliteScanMsPerTick\":%.4f,\"lightliteRenderUsPerFrame\":%.2f}%n",
                target, SharedConstants.getCurrentVersion().name(), range, frames.length,
                stats("frame", frames), stats("tick", tickTimes),
                markers, PerfStats.scanMsPerTick, PerfStats.renderUsPerFrame);
        try {
            // The gametest run directory is wiped before every run, so results go elsewhere
            String out = System.getProperty("lightlite.bench.out");
            Path file = out != null ? Path.of(out) : FabricLoader.getInstance().getGameDir().resolve("bench/results.jsonl");
            Files.createDirectories(file.getParent());
            Files.writeString(file, json, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            throw new AssertionError("Could not write benchmark result", e);
        }
    }
}
