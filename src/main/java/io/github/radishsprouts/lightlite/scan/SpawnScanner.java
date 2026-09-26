package io.github.radishsprouts.lightlite.scan;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
//? if >=26.2 {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes spawnable positions for one 16x16x16 section, using vanilla's own
 * ground-spawn checks ({@code SpawnPlacementTypes.ON_GROUND}) for a 2-block-tall mob
 * and the dimension's monster light rules.
 *
 * <p>Per-block-state results are memoized so a section scan is mostly array lookups.
 */
public final class SpawnScanner {
    /** Spawnable regardless of time of day. */
    public static final int KIND_ALWAYS = 1;
    /** Spawnable only while the sky is dark (night / thunder). */
    public static final int KIND_NIGHT = 2;

    /** Maximum sky darkening applied by vanilla at night. */
    private static final int MAX_SKY_DARKEN = 11;
    //? if >=26.2 {
    /*private static final EntityType<?> REFERENCE_MOB = EntityTypes.ZOMBIE;
    *///?} else {
    private static final EntityType<?> REFERENCE_MOB = EntityType.ZOMBIE;
    //?}
    private static final int[] EMPTY = new int[0];

    private static final byte UNKNOWN = 0;
    private static final byte NO = 1;
    private static final byte YES = 2;

    private byte[] floorMemo = new byte[0];
    private byte[] spaceMemo = new byte[0];
    private short[] heightMemo = new short[0];
    private final List<ResourceKey<Biome>> excludedBiomes = new ArrayList<>();
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
    private final IntArrayList out = new IntArrayList();

    public void reset(List<String> excludedBiomeIds) {
        int size = Block.BLOCK_STATE_REGISTRY.size();
        floorMemo = new byte[size];
        spaceMemo = new byte[size];
        heightMemo = new short[size];
        java.util.Arrays.fill(heightMemo, (short) -1);
        excludedBiomes.clear();
        for (String id : excludedBiomeIds) {
            try {
                excludedBiomes.add(ResourceKey.create(Registries.BIOME, Identifier.parse(id)));
            } catch (Exception ignored) {
                // invalid id in config; skip
            }
        }
    }

    /**
     * Packs a marker: bits 0-3 x, 4-7 z, 8-11 y (section-local), 12-13 kind,
     * 16-26 surface height in 1/1000 block.
     */
    public static int pack(int x, int y, int z, int kind, int heightMilli) {
        return x | (z << 4) | (y << 8) | (kind << 12) | (heightMilli << 16);
    }

    public static int x(int m) { return m & 15; }
    public static int z(int m) { return (m >>> 4) & 15; }
    public static int y(int m) { return (m >>> 8) & 15; }
    public static int kind(int m) { return (m >>> 12) & 3; }
    public static int heightMilli(int m) { return (m >>> 16) & 0x7FF; }

    /**
     * Scans one section. The chunk must be loaded; light should be available.
     */
    public int[] scan(Level level, LevelChunk chunk, int sx, int sy, int sz) {
        int sectionIndex = level.getSectionIndexFromSectionY(sy);
        int belowIndex = sectionIndex - 1;
        LevelChunkSection[] sections = chunk.getSections();
        if (sectionIndex < 0 || sectionIndex >= sections.length) return EMPTY;
        boolean selfAir = sections[sectionIndex].hasOnlyAir();
        boolean belowAir = belowIndex < 0 || sections[belowIndex].hasOnlyAir();
        // Every spawn spot needs a non-air floor: in this section, or right below it
        if (selfAir && belowAir) return EMPTY;

        DimensionType dim = level.dimensionType();
        int blockLimit = dim.monsterSpawnBlockLightLimit();
        int lightTest = dim.monsterSpawnLightTest().maxInclusive();
        boolean hasSky = dim.hasSkyLight();

        out.clear();
        int baseX = SectionPos.sectionToBlockCoord(sx);
        int baseY = SectionPos.sectionToBlockCoord(sy);
        int baseZ = SectionPos.sectionToBlockCoord(sz);
        for (int ly = 0; ly < 16; ly++) {
            // When this section is all air, only its bottom layer can stand on the section below
            if (selfAir && ly > 0) break;
            int y = baseY + ly;
            for (int lz = 0; lz < 16; lz++) {
                for (int lx = 0; lx < 16; lx++) {
                    pos.set(baseX + lx, y, baseZ + lz);
                    BlockState space = chunk.getBlockState(pos);
                    if (!spaceOk(level, pos, space)) continue;

                    probe.setWithOffset(pos, Direction.DOWN);
                    BlockState floor = chunk.getBlockState(probe);
                    if (floor.isAir() || !floorOk(level, probe, floor)) continue;

                    probe.setWithOffset(pos, Direction.UP);
                    BlockState head = chunk.getBlockState(probe);
                    if (!spaceOk(level, probe, head)) continue;

                    int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
                    if (blockLimit < 15 && blockLight > blockLimit) continue;
                    int skyLight = hasSky ? level.getBrightness(LightLayer.SKY, pos) : 0;
                    int kind;
                    if (Math.max(blockLight, skyLight) <= lightTest) {
                        kind = KIND_ALWAYS;
                    } else if (hasSky && blockLight <= lightTest && skyLight - MAX_SKY_DARKEN <= lightTest) {
                        kind = KIND_NIGHT;
                    } else {
                        continue;
                    }

                    if (!excludedBiomes.isEmpty() && isExcludedBiome(level.getBiome(pos))) continue;

                    out.add(pack(lx, ly, lz, kind, surfaceHeightMilli(level, pos, space)));
                }
            }
        }
        return out.isEmpty() ? EMPTY : out.toIntArray();
    }

    private boolean isExcludedBiome(Holder<Biome> biome) {
        for (ResourceKey<Biome> key : excludedBiomes) {
            if (biome.is(key)) return true;
        }
        return false;
    }

    private static int stateId(BlockState state, byte[] memo) {
        if (state.getBlock().hasDynamicShape()) return -1;
        int id = Block.BLOCK_STATE_REGISTRY.getId(state);
        return id >= 0 && id < memo.length ? id : -1;
    }

    /** Vanilla {@code BlockState#isValidSpawn} for the reference mob. */
    public boolean floorOk(BlockGetter level, BlockPos at, BlockState state) {
        int id = stateId(state, floorMemo);
        if (id < 0) return state.isValidSpawn(level, at, REFERENCE_MOB);
        byte m = floorMemo[id];
        if (m != UNKNOWN) return m == YES;
        boolean ok = state.isValidSpawn(level, at, REFERENCE_MOB);
        floorMemo[id] = ok ? YES : NO;
        return ok;
    }

    /** Vanilla {@code NaturalSpawner#isValidEmptySpawnBlock} for the reference mob. */
    public boolean spaceOk(BlockGetter level, BlockPos at, BlockState state) {
        int id = stateId(state, spaceMemo);
        if (id < 0) return NaturalSpawner.isValidEmptySpawnBlock(level, at, state, state.getFluidState(), REFERENCE_MOB);
        byte m = spaceMemo[id];
        if (m != UNKNOWN) return m == YES;
        boolean ok = NaturalSpawner.isValidEmptySpawnBlock(level, at, state, state.getFluidState(), REFERENCE_MOB);
        spaceMemo[id] = ok ? YES : NO;
        return ok;
    }

    /** Top of the collision shape inside the spawn block (snow layers, carpets...). */
    private int surfaceHeightMilli(BlockGetter level, BlockPos at, BlockState state) {
        if (state.isAir()) return 0;
        int id = stateId(state, spaceMemo);
        if (id >= 0 && heightMemo[id] >= 0) return heightMemo[id];
        VoxelShape shape = state.getCollisionShape(level, at);
        int h = shape.isEmpty() ? 0 : (int) Math.round(Math.max(0.0, Math.min(1.0, shape.max(Direction.Axis.Y))) * 1000.0);
        if (id >= 0) heightMemo[id] = (short) h;
        return h;
    }
}
