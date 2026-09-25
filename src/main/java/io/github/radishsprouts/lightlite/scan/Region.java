package io.github.radishsprouts.lightlite.scan;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * A 4x4 chunk column group (64x64 blocks). Owns one mesh that is rebuilt only
 * when one of its sections changes.
 */
public final class Region {
    public static final int SHIFT = 2;

    public final int rx;
    public final int rz;
    public final LongOpenHashSet sections = new LongOpenHashSet();
    public boolean dirty = true;

    /** Backend-specific GPU handle, or {@code null}. */
    public Object gpu;
    /** CPU copy of the mesh, kept only by the immediate backend. */
    public float[] cpuPositions;
    public int[] cpuColors;
    public int quads;
    public long gpuBytes;

    public Region(int rx, int rz) {
        this.rx = rx;
        this.rz = rz;
    }

    public int originX() {
        return rx << (SHIFT + 4);
    }

    public int originZ() {
        return rz << (SHIFT + 4);
    }

    public static long key(int rx, int rz) {
        return ((long) rx << 32) | (rz & 0xFFFFFFFFL);
    }
}
