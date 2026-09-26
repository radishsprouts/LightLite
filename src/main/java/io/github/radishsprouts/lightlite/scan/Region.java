package io.github.radishsprouts.lightlite.scan;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * A 4x4 chunk column group (64x64 blocks). Holds two meshes built from the same markers:
 * {@link #far} merges neighbouring tiles into rectangles (few vertices, used at a distance),
 * {@link #near} keeps one inset tile per block (readable grid, built only near the camera).
 */
public final class Region {
    public static final int SHIFT = 2;

    public final int rx;
    public final int rz;
    public final LongOpenHashSet sections = new LongOpenHashSet();
    public final RegionMesh far = new RegionMesh();
    public final RegionMesh near = new RegionMesh();

    public Region(int rx, int rz) {
        this.rx = rx;
        this.rz = rz;
    }

    public void markDirty() {
        far.dirty = true;
        near.dirty = true;
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
