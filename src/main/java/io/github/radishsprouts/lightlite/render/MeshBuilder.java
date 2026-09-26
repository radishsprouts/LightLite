package io.github.radishsprouts.lightlite.render;

import io.github.radishsprouts.lightlite.config.LightLiteConfig;
import io.github.radishsprouts.lightlite.scan.OverlayManager;
import io.github.radishsprouts.lightlite.scan.Region;
import io.github.radishsprouts.lightlite.scan.RegionMesh;
import io.github.radishsprouts.lightlite.scan.SpawnScanner;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.core.SectionPos;

import java.util.Arrays;

/**
 * Turns a region's cached markers into quads grouped by chunk column (positions relative to
 * the region origin on X/Z, absolute on Y). Reuses its arrays between calls.
 *
 * <p>In merged mode, same-colored tiles at the same height are combined into rectangles with a
 * greedy scan, so a lit-up floor of N blocks costs a handful of quads instead of N.
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

    private final LongArrayList[] columnSections = new LongArrayList[RegionMesh.COLUMNS];
    private final Int2ObjectOpenHashMap<long[]> groups = new Int2ObjectOpenHashMap<>();

    MeshBuilder() {
        for (int i = 0; i < columnSections.length; i++) columnSections[i] = new LongArrayList();
    }

    int quads() {
        return vertexCount >> 2;
    }

    /**
     * @param merged build merged tiles (used at a distance in both display modes); otherwise the
     *               detailed mesh: one inset tile per block, or crosses in cross mode
     */
    void build(Region region, RegionMesh mesh, OverlayManager manager, LightLiteConfig cfg, boolean merged) {
        vertexCount = 0;
        boolean cross = !merged && cfg.mode == LightLiteConfig.Mode.CROSS;
        int always = cfg.alwaysArgb();
        int night = cfg.nightArgb();
        if (cross) {
            always = withMinAlpha(always);
            night = withMinAlpha(night);
        }
        int ox = region.originX();
        int oz = region.originZ();

        for (LongArrayList list : columnSections) list.clear();
        for (LongIterator it = region.sections.iterator(); it.hasNext(); ) {
            long key = it.nextLong();
            columnSections[RegionMesh.columnIndex(SectionPos.x(key), SectionPos.z(key))].add(key);
        }

        for (int c = 0; c < RegionMesh.COLUMNS; c++) {
            int startQuad = quads();
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            LongArrayList keys = columnSections[c];
            for (int i = 0; i < keys.size(); i++) {
                long key = keys.getLong(i);
                int[] markers = manager.section(key);
                if (markers.length == 0) continue;
                int bx = SectionPos.sectionToBlockCoord(SectionPos.x(key)) - ox;
                int by = SectionPos.sectionToBlockCoord(SectionPos.y(key));
                int bz = SectionPos.sectionToBlockCoord(SectionPos.z(key)) - oz;
                for (int m : markers) {
                    int y = by + SpawnScanner.y(m);
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
                if (cross) {
                    ensureCapacity(vertexCount + markers.length * 8);
                    for (int m : markers) {
                        addCross(bx + SpawnScanner.x(m), surfaceY(by, m), bz + SpawnScanner.z(m), colorOf(m, always, night));
                    }
                } else if (merged) {
                    addMerged(markers, bx, by, bz, always, night);
                } else {
                    ensureCapacity(vertexCount + markers.length * 4);
                    for (int m : markers) {
                        addRect(bx + SpawnScanner.x(m), bz + SpawnScanner.z(m), 1, 1, surfaceY(by, m), colorOf(m, always, night));
                    }
                }
            }
            mesh.columnStart[c] = startQuad;
            mesh.columnQuads[c] = quads() - startQuad;
            mesh.columnMinY[c] = minY == Integer.MAX_VALUE ? 0 : minY;
            mesh.columnMaxY[c] = maxY == Integer.MIN_VALUE ? 0 : maxY;
        }
    }

    /** Greedy rectangle merge of one section's markers, per (layer, surface height, kind). */
    private void addMerged(int[] markers, int bx, int by, int bz, int always, int night) {
        groups.clear();
        for (int m : markers) {
            // Group key: local y (4 bits) | kind (2 bits) | surface height (11 bits)
            int key = SpawnScanner.y(m) | (SpawnScanner.kind(m) << 4) | (SpawnScanner.heightMilli(m) << 6);
            long[] mask = groups.get(key);
            if (mask == null) {
                mask = new long[4];
                groups.put(key, mask);
            }
            int bit = (SpawnScanner.z(m) << 4) | SpawnScanner.x(m);
            mask[bit >>> 6] |= 1L << (bit & 63);
        }
        for (Int2ObjectOpenHashMap.Entry<long[]> e : groups.int2ObjectEntrySet()) {
            int key = e.getIntKey();
            long[] mask = e.getValue();
            int ly = key & 15;
            int kind = (key >>> 4) & 3;
            int heightMilli = key >>> 6;
            float y = by + ly + heightMilli / 1000.0F + LIFT;
            int color = kind == SpawnScanner.KIND_ALWAYS ? always : night;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (!isSet(mask, x, z)) continue;
                    int w = 1;
                    while (x + w < 16 && isSet(mask, x + w, z)) w++;
                    int h = 1;
                    outer:
                    while (z + h < 16) {
                        for (int i = 0; i < w; i++) if (!isSet(mask, x + i, z + h)) break outer;
                        h++;
                    }
                    for (int dz = 0; dz < h; dz++) for (int dx = 0; dx < w; dx++) clear(mask, x + dx, z + dz);
                    ensureCapacity(vertexCount + 4);
                    addRect(bx + x, bz + z, w, h, y, color);
                }
            }
        }
    }

    private static boolean isSet(long[] mask, int x, int z) {
        int bit = (z << 4) | x;
        return (mask[bit >>> 6] & (1L << (bit & 63))) != 0;
    }

    private static void clear(long[] mask, int x, int z) {
        int bit = (z << 4) | x;
        mask[bit >>> 6] &= ~(1L << (bit & 63));
    }

    private static float surfaceY(int sectionBaseY, int m) {
        return sectionBaseY + SpawnScanner.y(m) + SpawnScanner.heightMilli(m) / 1000.0F + LIFT;
    }

    private static int colorOf(int m, int always, int night) {
        return SpawnScanner.kind(m) == SpawnScanner.KIND_ALWAYS ? always : night;
    }

    /** Tile covering w x h blocks, inset only on its outer edges. */
    private void addRect(float x, float z, int w, int h, float y, int color) {
        float x0 = x + TILE_INSET, x1 = x + w - TILE_INSET;
        float z0 = z + TILE_INSET, z1 = z + h - TILE_INSET;
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
