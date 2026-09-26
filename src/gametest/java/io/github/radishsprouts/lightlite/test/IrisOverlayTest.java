package io.github.radishsprouts.lightlite.test;

import io.github.radishsprouts.lightlite.config.LightLiteConfig;
import io.github.radishsprouts.lightlite.render.OverlayRenderer;
import io.github.radishsprouts.lightlite.scan.OverlayManager;
import io.github.radishsprouts.lightlite.scan.SpawnScanner;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Checks that the overlay survives a shader pack. Enabled with {@code -Dlightlite.iris=true},
 * which also puts Iris and Sodium on the classpath (see build.gradle.kts).
 *
 * <p>Iris writes the shader pack's final image over the main framebuffer at the end of level
 * rendering, so anything drawn outside the pack's programs disappears. The test loads a tiny
 * pass-through pack, then compares screenshots with the overlay on and off: if the overlay
 * reaches the screen, many pixels differ.
 */
@SuppressWarnings("UnstableApiUsage")
public class IrisOverlayTest implements FabricClientGameTest {
    private static final String PACK = "lightlite-test-pack";
    private static final String[] PACK_FILES = {"gbuffers_basic.vsh", "gbuffers_basic.fsh", "final.vsh", "final.fsh"};
    /** Share of the screen that must change when the overlay is toggled. */
    private static final double MIN_CHANGED = 0.02;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("lightlite.iris")) return;

        context.runOnClient(client -> enableTestPack());
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            //? if >=26.2 {
            /*singleplayer.getConnection().waitForChunksRender();
            *///?} else {
            singleplayer.getClientLevel().waitForChunksRender();
            //?}
            var server = singleplayer.getServer();
            server.runCommand("time set noon");
            // Look down at open grass, which carries night-only markers in daylight
            server.runCommand("tp @a 0 -59 0 0 50");
            context.waitFor(client -> OverlayManager.get().countMarkers(
                    -16, -64, -16, 16, -50, 16, SpawnScanner.KIND_NIGHT) > 100, 400);

            boolean shaders = context.computeOnClient(client -> shaderPackInUse());
            if (!shaders) throw new AssertionError("the test shader pack is not active");

            StringBuilder failures = new StringBuilder();
            for (LightLiteConfig.Backend backend : new LightLiteConfig.Backend[]{
                    LightLiteConfig.Backend.RETAINED, LightLiteConfig.Backend.IMMEDIATE}) {
                context.runOnClient(client -> {
                    LightLiteConfig.get().backend = backend;
                    LightLiteConfig.get().enabled = true;
                    OverlayRenderer.get().resetBackend();
                });
                context.waitTicks(20);
                String active = context.computeOnClient(client -> OverlayRenderer.get().backendName());
                Path on = context.takeScreenshot("iris-" + backend.name().toLowerCase() + "-on");
                context.runOnClient(client -> LightLiteConfig.get().enabled = false);
                context.waitTicks(20);
                Path off = context.takeScreenshot("iris-" + backend.name().toLowerCase() + "-off");
                context.runOnClient(client -> LightLiteConfig.get().enabled = true);

                double changed = changedShare(on, off);
                System.out.printf("[LightLite] Iris %s (active: %s): %.1f%% of pixels change with the overlay%n",
                        backend, active, changed * 100);
                if (changed < MIN_CHANGED) {
                    failures.append(String.format("%s (active %s): only %.2f%% of pixels changed; ",
                            backend, active, changed * 100));
                }
            }
            if (!failures.isEmpty()) {
                throw new AssertionError("overlay not visible with a shader pack: " + failures);
            }

            // With Iris installed, the default must still pick the fast path
            context.runOnClient(client -> {
                LightLiteConfig.get().backend = LightLiteConfig.Backend.AUTO;
                OverlayRenderer.get().resetBackend();
            });
            context.waitTicks(5);
            String auto = context.computeOnClient(client -> OverlayRenderer.get().backendName());
            if (!"retained".equals(auto)) throw new AssertionError("AUTO picked " + auto + " with Iris installed");
        }
    }

    /** Copies the pass-through pack into Iris's shaderpacks folder and turns it on. */
    private static void enableTestPack() {
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Path packs = (Path) iris.getMethod("getShaderpacksDirectory").invoke(null);
            Path shaders = packs.resolve(PACK).resolve("shaders");
            Files.createDirectories(shaders);
            for (String file : PACK_FILES) {
                try (InputStream in = IrisOverlayTest.class.getResourceAsStream("/" + PACK + "/shaders/" + file)) {
                    if (in == null) throw new IOException("missing test pack file " + file);
                    Files.copy(in, shaders.resolve(file), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Object config = iris.getMethod("getIrisConfig").invoke(null);
            config.getClass().getMethod("setShaderPackName", String.class).invoke(config, PACK);
            config.getClass().getMethod("setShadersEnabled", boolean.class).invoke(config, true);
            config.getClass().getMethod("save").invoke(config);
            iris.getMethod("reload").invoke(null);
        } catch (ReflectiveOperationException | IOException e) {
            throw new AssertionError("could not enable the test shader pack", e);
        }
    }

    private static boolean shaderPackInUse() {
        try {
            Object api = Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance").invoke(null);
            return (boolean) api.getClass().getMethod("isShaderPackInUse").invoke(api);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Iris API not available", e);
        }
    }

    private static double changedShare(Path a, Path b) {
        try {
            BufferedImage imageA = ImageIO.read(a.toFile());
            BufferedImage imageB = ImageIO.read(b.toFile());
            int width = Math.min(imageA.getWidth(), imageB.getWidth());
            int height = Math.min(imageA.getHeight(), imageB.getHeight());
            long changed = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int p = imageA.getRGB(x, y);
                    int q = imageB.getRGB(x, y);
                    int diff = Math.abs(((p >> 16) & 0xFF) - ((q >> 16) & 0xFF))
                            + Math.abs(((p >> 8) & 0xFF) - ((q >> 8) & 0xFF))
                            + Math.abs((p & 0xFF) - (q & 0xFF));
                    if (diff > 24) changed++;
                }
            }
            return (double) changed / ((long) width * height);
        } catch (IOException e) {
            throw new AssertionError("could not read screenshots", e);
        }
    }
}
