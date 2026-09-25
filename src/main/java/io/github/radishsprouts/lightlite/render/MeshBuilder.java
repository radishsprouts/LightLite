package io.github.radishsprouts.lightlite.render;

import io.github.radishsprouts.lightlite.config.LightLiteConfig;
import io.github.radishsprouts.lightlite.scan.OverlayManager;
import io.github.radishsprouts.lightlite.scan.Region;
import io.github.radishsprouts.lightlite.scan.SpawnScanner;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.core.SectionPos;

import java.util.Arrays;

/**
 * Turns a region's cached markers into flat quads (positions relative to the region
 * origin on X/Z, absolute on Y). Reuses its arrays between calls.
 */
final class MeshBuilder {
    /** Lift above the floor to avoid z-fighting. */
    private static final float LIFT = 0.02F;
    private static final float TILE_INSET = 0.0625F;
    private static final float CROSS_INSET = 0.1F;
    private static final float CROSS_HALF_WIDTH = 0.045F;
    private static final int CROSS_MIN_ALPHA = 0xC0;

    float[] positions = new float[4096 * 3];
    int[] colors = new int[4096];
    int vertexCount;

    int quads() {
        return vertexCount >> 2;
    }

    void build(Region region, OverlayManager manager, LightLiteConfig cfg) {
        vertexCount = 0;
        boolean cross = cfg.mode == LightLiteConfig.Mode.CROSS;
        int always = cfg.alwaysArgb();
        int night = cfg.nightArgb();
        if (cross) {
            always = withMinAlpha(always);
            night = withMinAlpha(night);
        }
        int ox = region.originX();
        int oz = region.originZ();

        for (LongIterator it = region.sections.iterator(); it.hasNext(); ) {
            long key = it.nextLong();
            int[] markers = manager.section(key);
            if (markers.length == 0) continue;
            int bx = SectionPos.sectionToBlockCoord(SectionPos.x(key)) - ox;
            int by = SectionPos.sectionToBlockCoord(SectionPos.y(key));
            int bz = SectionPos.sectionToBlockCoord(SectionPos.z(key)) - oz;
            ensureCapacity(vertexCount + markers.length * (cross ? 8 : 4));
            for (int m : markers) {
                float x = bx + SpawnScanner.x(m);
                float y = by + SpawnScanner.y(m) + SpawnScanner.heightMilli(m) / 1000.0F + LIFT;
                float z = bz + SpawnScanner.z(m);
                int color = SpawnScanner.kind(m) == SpawnScanner.KIND_ALWAYS ? always : night;
                if (cross) {
                    addCross(x, y, z, color);
                } else {
                    addTile(x, y, z, color);
                }
            }
        }
    }

    private void addTile(float x, float y, float z, int color) {
        float x0 = x + TILE_INSET, x1 = x + 1 - TILE_INSET;
        float z0 = z + TILE_INSET, z1 = z + 1 - TILE_INSET;
        vertex(x0, y, z0, color);
        vertex(x0, y, z1, color);
        vertex(x1, y, z1, color);
        vertex(x1, y, z0, color);
    }

    private void addCross(float x, float y, float z, int color) {
        float a = CROSS_INSET, b = 1 - CROSS_INSET, w = CROSS_HALF_WIDTH;
        // Diagonal (a,a) -> (b,b), offset along (1,-1)
        vertex(x + a + w, y, z + a - w, color);
        vertex(x + b + w, y, z + b - w, color);
        vertex(x + b - w, y, z + b + w, color);
        vertex(x + a - w, y, z + a + w, color);
        // Diagonal (b,a) -> (a,b), offset along (1,1)
        vertex(x + b + w, y, z + a + w, color);
        vertex(x + a + w, y, z + b + w, color);
        vertex(x + a - w, y, z + b - w, color);
        vertex(x + b - w, y, z + a - w, color);
    }

    private void vertex(float x, float y, float z, int color) {
        int p = vertexCount * 3;
        positions[p] = x;
        positions[p + 1] = y;
        positions[p + 2] = z;
        colors[vertexCount] = color;
        vertexCount++;
    }

    private void ensureCapacity(int vertices) {
        if (vertices > colors.length) {
            int size = Math.max(vertices, colors.length * 2);
            colors = Arrays.copyOf(colors, size);
            positions = Arrays.copyOf(positions, size * 3);
        }
    }

    private static int withMinAlpha(int argb) {
        int alpha = argb >>> 24;
        return alpha >= CROSS_MIN_ALPHA ? argb : (argb & 0x00FFFFFF) | (CROSS_MIN_ALPHA << 24);
    }
}
