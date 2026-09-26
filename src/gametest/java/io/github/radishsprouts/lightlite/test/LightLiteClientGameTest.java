package io.github.radishsprouts.lightlite.test;

import io.github.radishsprouts.lightlite.render.OverlayRenderer;
import io.github.radishsprouts.lightlite.scan.OverlayManager;
import io.github.radishsprouts.lightlite.scan.SpawnScanner;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/**
 * End-to-end check on a real client: markers appear in a sealed dark room, disappear when
 * a torch lights it, and come back when the torch is removed.
 */
@SuppressWarnings("UnstableApiUsage")
public class LightLiteClientGameTest implements FabricClientGameTest {
    private static final int TIMEOUT_TICKS = 400;

    // Sealed stone room; its floor is at y=-58, so mobs would stand at y=-57
    private static final int MIN = 4;
    private static final int MAX = 14;
    private static final int FLOOR_Y = -58;
    private static final int SPAWN_Y = FLOOR_Y + 1;
    private static final int ROOM_SPOTS = (MAX - MIN - 1) * (MAX - MIN - 1);

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getProperty("lightlite.bench") != null) return;
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            //? if >=26.2 {
            /*singleplayer.getConnection().waitForChunksRender();
            *///?} else {
            singleplayer.getClientLevel().waitForChunksRender();
            //?}
            var server = singleplayer.getServer();
            server.runCommand("time set noon");
            server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone hollow",
                    MIN, FLOOR_Y, MIN, MAX, FLOOR_Y + 4, MAX));

            expectRoom(context, SpawnScanner.KIND_ALWAYS, ROOM_SPOTS, "dark sealed room");

            server.runCommand(String.format("setblock %d %d %d minecraft:torch", (MIN + MAX) / 2, SPAWN_Y, (MIN + MAX) / 2));
            expectRoom(context, 0, 0, "room lit by a torch");

            // Soul sand and mud are lower than a full block but opaque: dark inside even next to a torch.
            // A mob does not fit in them, so they must not be reported as spawn spots
            int mid = (MIN + MAX) / 2;
            server.runCommand(String.format("setblock %d %d %d minecraft:soul_sand", mid + 2, SPAWN_Y, mid));
            server.runCommand(String.format("setblock %d %d %d minecraft:mud", mid, SPAWN_Y, mid + 2));
            context.waitFor(client -> client.level.getBlockState(new BlockPos(mid, SPAWN_Y, mid + 2)).is(Blocks.MUD), TIMEOUT_TICKS);
            context.waitTicks(40);
            expectRoom(context, 0, 0, "lit room with soul sand and mud");
            server.runCommand(String.format("setblock %d %d %d minecraft:air", mid + 2, SPAWN_Y, mid));
            server.runCommand(String.format("setblock %d %d %d minecraft:air", mid, SPAWN_Y, mid + 2));

            server.runCommand(String.format("setblock %d %d %d minecraft:air", (MIN + MAX) / 2, SPAWN_Y, (MIN + MAX) / 2));
            expectRoom(context, SpawnScanner.KIND_ALWAYS, ROOM_SPOTS, "torch removed");

            // Open grass under the midday sky: spawnable only at night
            int night = context.computeOnClient(client -> OverlayManager.get().countMarkers(
                    -24, -64, -24, -4, -40, -4, SpawnScanner.KIND_NIGHT));
            if (night == 0) throw new AssertionError("expected night-only markers on open ground, found none");

            // Without shader mods the fast path must stay active (no silent fallback)
            String backend = context.computeOnClient(client -> OverlayRenderer.get().backendName());
            if (!"retained".equals(backend)) throw new AssertionError("expected retained backend, got " + backend);

            context.takeScreenshot("lightlite-overlay");
        }
    }

    private static void expectRoom(ClientGameTestContext context, int kind, int expected, String what) {
        try {
            context.waitFor(client -> count(kind) == expected, TIMEOUT_TICKS);
        } catch (Throwable t) {
            int actual = context.computeOnClient(client -> count(kind));
            throw new AssertionError(what + ": expected " + expected + " markers, found " + actual, t);
        }
    }

    private static int count(int kind) {
        return OverlayManager.get().countMarkers(MIN + 1, SPAWN_Y, MIN + 1, MAX - 1, SPAWN_Y, MAX - 1, kind);
    }
}
