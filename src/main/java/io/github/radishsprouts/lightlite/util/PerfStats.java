package io.github.radishsprouts.lightlite.util;

/**
 * Cheap self-measurement shown by {@code /lightlite stats}. All values are updated
 * on the client thread.
 */
public final class PerfStats {
    private static final double ALPHA = 0.05;

    public static double scanMsPerTick;
    public static double scanMsPeak;
    public static double renderUsPerFrame;
    public static double rebuildUsPerFrame;
    public static long uploadedBytes;
    public static long sectionsScanned;
    public static int trackedSections;
    public static int pendingSections;
    public static int markers;
    public static int regions;
    public static int drawCalls;
    public static int drawnQuads;
    public static int culledColumns;
    public static long gpuBytes;
    public static String backend = "-";

    private PerfStats() {
    }

    public static void recordScan(long nanos) {
        double ms = nanos / 1_000_000.0;
        scanMsPerTick += (ms - scanMsPerTick) * ALPHA;
        // Peak decays slowly so a single spike stays visible for a while
        scanMsPeak = Math.max(ms, scanMsPeak * 0.995);
    }

    public static void recordRender(long nanos) {
        renderUsPerFrame += (nanos / 1_000.0 - renderUsPerFrame) * ALPHA;
    }

    public static void recordRebuild(long nanos) {
        rebuildUsPerFrame += (nanos / 1_000.0 - rebuildUsPerFrame) * ALPHA;
    }

    public static void reset() {
        scanMsPerTick = 0;
        scanMsPeak = 0;
        renderUsPerFrame = 0;
        rebuildUsPerFrame = 0;
        uploadedBytes = 0;
        sectionsScanned = 0;
        drawCalls = 0;
        drawnQuads = 0;
        culledColumns = 0;
    }
}
