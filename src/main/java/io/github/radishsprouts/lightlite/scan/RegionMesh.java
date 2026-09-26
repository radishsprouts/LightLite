package io.github.radishsprouts.lightlite.scan;

/**
 * One uploaded mesh of a {@link Region}. Quads are grouped by chunk column (16 per region),
 * so a frame can cull and pick a level of detail per column while drawing ranges of one buffer.
 */
public final class RegionMesh {
    public static final int COLUMNS = 16;

    /** Backend-specific GPU handle, or {@code null}. */
    public Object gpu;
    /** CPU copy, kept only by the immediate backend. */
    public float[] cpuPositions;
    public int[] cpuColors;
    public int quads;
    public long gpuBytes;

    /** First quad and quad count per chunk column (index {@code (cz & 3) << 2 | (cx & 3)}). */
    public final int[] columnStart = new int[COLUMNS];
    public final int[] columnQuads = new int[COLUMNS];
    /** Block Y bounds of each column's quads, for culling. */
    public final int[] columnMinY = new int[COLUMNS];
    public final int[] columnMaxY = new int[COLUMNS];

    public boolean dirty = true;

    public boolean hasData() {
        return quads > 0;
    }

    public static int columnIndex(int chunkX, int chunkZ) {
        return ((chunkZ & 3) << 2) | (chunkX & 3);
    }
}
