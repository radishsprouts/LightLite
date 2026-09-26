package io.github.radishsprouts.lightlite.scan;

import io.github.radishsprouts.lightlite.config.LightLiteConfig;
import io.github.radishsprouts.lightlite.render.OverlayRenderer;
import io.github.radishsprouts.lightlite.util.PerfStats;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Collection;

/**
 * Owns the per-section marker cache and decides which sections to (re)scan.
 *
 * <p>Nothing here runs per frame. Sections are scanned once, then only again when vanilla
 * reports a block or light change inside them. Scanning happens at the end of the client
 * tick under a fixed time budget, nearest sections first.
 */
public final class OverlayManager {
    private static final OverlayManager INSTANCE = new OverlayManager();
    private static final int[] EMPTY = new int[0];

    private final SpawnScanner scanner = new SpawnScanner();
    private final Long2ObjectOpenHashMap<int[]> sections = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet dirty = new LongOpenHashSet();
    private final Long2ObjectOpenHashMap<Region> regions = new Long2ObjectOpenHashMap<>();

    private ClientLevel level;
    private boolean active;
    private boolean needsEnumerate = true;
    private int centerX = Integer.MIN_VALUE;
    private int centerY;
    private int centerZ;
    private int radiusH;
    private int radiusV;
    private int markerCount;

    public static OverlayManager get() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active;
    }

    public Collection<Region> regions() {
        return regions.values();
    }

    public int[] section(long key) {
        int[] m = sections.get(key);
        return m != null ? m : EMPTY;
    }

    /**
     * Counts cached markers of {@code kind} (0 = any) inside a block box. Used by tests.
     */
    public synchronized int countMarkers(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int kind) {
        int count = 0;
        for (Long2ObjectMap.Entry<int[]> e : sections.long2ObjectEntrySet()) {
            long key = e.getLongKey();
            int bx = SectionPos.sectionToBlockCoord(SectionPos.x(key));
            int by = SectionPos.sectionToBlockCoord(SectionPos.y(key));
            int bz = SectionPos.sectionToBlockCoord(SectionPos.z(key));
            for (int m : e.getValue()) {
                int x = bx + SpawnScanner.x(m);
                int y = by + SpawnScanner.y(m);
                int z = bz + SpawnScanner.z(m);
                if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) continue;
                if (kind == 0 || SpawnScanner.kind(m) == kind) count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------ tick

    public synchronized void tick(Minecraft mc) {
        ClientLevel currentLevel = mc.level;
        LocalPlayer player = mc.player;
        LightLiteConfig cfg = LightLiteConfig.get();

        if (currentLevel != level) {
            clear();
            level = currentLevel;
            scanner.reset(cfg.excludedBiomes);
        }
        if (level == null || player == null) return;

        boolean shouldBeActive = cfg.enabled && (!cfg.onlyWhenHoldingLight || isHoldingLight(player));
        if (!shouldBeActive) {
            if (active) clear();
            active = false;
            return;
        }
        active = true;

        int cx = SectionPos.blockToSectionCoord(player.getBlockX());
        int cy = SectionPos.blockToSectionCoord(player.getBlockY());
        int cz = SectionPos.blockToSectionCoord(player.getBlockZ());
        int rh = (cfg.horizontalRange + 15) >> 4;
        int rv = (cfg.verticalRange + 15) >> 4;
        if (needsEnumerate || cx != centerX || cy != centerY || cz != centerZ || rh != radiusH || rv != radiusV) {
            centerX = cx;
            centerY = cy;
            centerZ = cz;
            radiusH = rh;
            radiusV = rv;
            enumerate();
            needsEnumerate = false;
        }

        long start = System.nanoTime();
        processDirty(start + (long) (cfg.tickBudgetMs * 1_000_000.0));
        PerfStats.recordScan(System.nanoTime() - start);
        PerfStats.trackedSections = sections.size();
        PerfStats.pendingSections = dirty.size();
        PerfStats.markers = markerCount;
        PerfStats.regions = regions.size();
    }

    private static boolean isHoldingLight(LocalPlayer player) {
        return isLightSource(player.getMainHandItem()) || isLightSource(player.getOffhandItem());
    }

    private static boolean isLightSource(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock().defaultBlockState().getLightEmission() > 0;
    }

    private boolean inRange(int sx, int sy, int sz, int marginH, int marginV) {
        return Math.abs(sx - centerX) <= radiusH + marginH
                && Math.abs(sz - centerZ) <= radiusH + marginH
                && Math.abs(sy - centerY) <= radiusV + marginV;
    }

    /** Queues newly visible sections and forgets ones far out of range. */
    private void enumerate() {
        ObjectIterator<Long2ObjectMap.Entry<int[]>> it = sections.long2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            Long2ObjectMap.Entry<int[]> e = it.next();
            long key = e.getLongKey();
            if (!inRange(SectionPos.x(key), SectionPos.y(key), SectionPos.z(key), 1, 1)) {
                markerCount -= e.getValue().length;
                it.remove();
                detachFromRegion(key);
            }
        }
        for (LongIterator d = dirty.iterator(); d.hasNext(); ) {
            long key = d.nextLong();
            if (!inRange(SectionPos.x(key), SectionPos.y(key), SectionPos.z(key), 0, 0)) d.remove();
        }

        int minY = Math.max(level.getMinSectionY(), centerY - radiusV);
        int maxY = Math.min(level.getMaxSectionY(), centerY + radiusV);
        for (int x = centerX - radiusH; x <= centerX + radiusH; x++) {
            for (int z = centerZ - radiusH; z <= centerZ + radiusH; z++) {
                if (!level.hasChunk(x, z)) continue;
                for (int y = minY; y <= maxY; y++) {
                    long key = SectionPos.asLong(x, y, z);
                    if (!sections.containsKey(key)) dirty.add(key);
                }
            }
        }
    }

    private void processDirty(long deadline) {
        if (dirty.isEmpty()) return;
        long[] keys = dirty.toLongArray();
        long[] dist = new long[keys.length];
        for (int i = 0; i < keys.length; i++) {
            long k = keys[i];
            long dx = SectionPos.x(k) - centerX;
            long dy = SectionPos.y(k) - centerY;
            long dz = SectionPos.z(k) - centerZ;
            dist[i] = dx * dx + dy * dy * 4 + dz * dz;
        }
        LongArrays.quickSort(dist, keys);

        int processed = 0;
        for (long key : keys) {
            if (processed > 0 && System.nanoTime() > deadline) break;
            int sx = SectionPos.x(key);
            int sy = SectionPos.y(key);
            int sz = SectionPos.z(key);
            if (!inRange(sx, sy, sz, 0, 0) || !level.hasChunk(sx, sz)) {
                dirty.remove(key);
                continue;
            }
            // Wait for the chunk's light data; vanilla marks the column dirty once it arrives
            if (!level.getLightEngine().lightOnInColumn(SectionPos.getZeroNode(sx, sz))) continue;

            int[] markers = scanner.scan(level, level.getChunk(sx, sz), sx, sy, sz);
            dirty.remove(key);
            processed++;
            PerfStats.sectionsScanned++;

            int[] old = sections.put(key, markers);
            if (old != null) markerCount -= old.length;
            markerCount += markers.length;
            if (old == null || !Arrays.equals(old, markers)) attachToRegion(key);
        }
    }

    // ------------------------------------------------------------ regions

    private void attachToRegion(long sectionKey) {
        int rx = SectionPos.x(sectionKey) >> Region.SHIFT;
        int rz = SectionPos.z(sectionKey) >> Region.SHIFT;
        long rk = Region.key(rx, rz);
        Region region = regions.get(rk);
        if (region == null) {
            region = new Region(rx, rz);
            regions.put(rk, region);
        }
        region.sections.add(sectionKey);
        region.markDirty();
    }

    private void detachFromRegion(long sectionKey) {
        long rk = Region.key(SectionPos.x(sectionKey) >> Region.SHIFT, SectionPos.z(sectionKey) >> Region.SHIFT);
        Region region = regions.get(rk);
        if (region == null) return;
        region.sections.remove(sectionKey);
        region.markDirty();
        if (region.sections.isEmpty()) {
            regions.remove(rk);
            OverlayRenderer.get().release(region);
        }
    }

    /** Forces every region mesh to be rebuilt (e.g. display mode or colors changed). */
    public synchronized void invalidateMeshes() {
        for (Region region : regions.values()) region.markDirty();
    }

    /** Drops everything, including GPU buffers. Markers are recomputed on demand. */
    public synchronized void clear() {
        for (Region region : regions.values()) OverlayRenderer.get().release(region);
        regions.clear();
        sections.clear();
        dirty.clear();
        markerCount = 0;
        needsEnumerate = true;
        centerX = Integer.MIN_VALUE;
    }

    /** Config changed in a way that affects scan results (e.g. biome list). */
    public synchronized void rescanAll() {
        scanner.reset(LightLiteConfig.get().excludedBiomes);
        clear();
    }

    // ------------------------------------------------ change notifications

    public synchronized void onChunkLoaded(int cx, int cz) {
        if (!active) return;
        if (Math.abs(cx - centerX) <= radiusH && Math.abs(cz - centerZ) <= radiusH) needsEnumerate = true;
    }

    public synchronized void onChunkUnloaded(int cx, int cz) {
        if (!active || level == null) return;
        for (int y = level.getMinSectionY(); y <= level.getMaxSectionY(); y++) {
            long key = SectionPos.asLong(cx, y, cz);
            dirty.remove(key);
            int[] old = sections.remove(key);
            if (old != null) {
                markerCount -= old.length;
                detachFromRegion(key);
            }
        }
    }

    public synchronized void onBlockChanged(BlockPos pos) {
        if (!active) return;
        int sx = SectionPos.blockToSectionCoord(pos.getX());
        int sy = SectionPos.blockToSectionCoord(pos.getY());
        int sz = SectionPos.blockToSectionCoord(pos.getZ());
        markSection(sx, sy, sz);
        // A block is floor for the spot above it and headroom for the spot below it
        int ly = pos.getY() & 15;
        if (ly == 15) markSection(sx, sy + 1, sz);
        if (ly == 0) markSection(sx, sy - 1, sz);
    }

    public synchronized void onSectionRangeDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (!active) return;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    markSection(x, y, z);
                }
            }
        }
    }

    public synchronized void onSectionDirty(int sx, int sy, int sz) {
        if (!active) return;
        markSection(sx, sy, sz);
    }

    private void markSection(int sx, int sy, int sz) {
        long key = SectionPos.asLong(sx, sy, sz);
        if (sections.containsKey(key)) dirty.add(key);
    }
}
