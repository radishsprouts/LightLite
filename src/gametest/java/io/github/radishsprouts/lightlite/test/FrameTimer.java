package io.github.radishsprouts.lightlite.test;

/**
 * Collects the CPU time of client frames and ticks while recording. Written from the
 * client thread only, read by the benchmark after recording stops.
 */
public final class FrameTimer {
    static volatile boolean recording;
    static long[] frames = new long[0];
    static long[] ticks = new long[0];
    static int frameCount;
    static int tickCount;

    private static long frameStart;
    private static long tickStart;

    private FrameTimer() {
    }

    static void start(int capacity) {
        frames = new long[capacity];
        ticks = new long[capacity];
        frameCount = 0;
        tickCount = 0;
        recording = true;
    }

    public static void frameBegin() {
        if (recording) frameStart = System.nanoTime();
    }

    public static void frameEnd() {
        if (recording && frameStart != 0 && frameCount < frames.length) frames[frameCount++] = System.nanoTime() - frameStart;
        frameStart = 0;
    }

    public static void tickBegin() {
        if (recording) tickStart = System.nanoTime();
    }

    public static void tickEnd() {
        if (recording && tickStart != 0 && tickCount < ticks.length) ticks[tickCount++] = System.nanoTime() - tickStart;
        tickStart = 0;
    }
}
