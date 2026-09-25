package io.github.radishsprouts.lightlite.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.radishsprouts.lightlite.LightLite;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain JSON config stored at {@code config/lightlite.json}.
 */
public final class LightLiteConfig {
    public enum Mode { TILE, CROSS }

    public enum Backend { AUTO, RETAINED, IMMEDIATE }

    public static final int MIN_HORIZONTAL_RANGE = 8;
    public static final int MAX_HORIZONTAL_RANGE = 128;
    public static final int MIN_VERTICAL_RANGE = 4;
    public static final int MAX_VERTICAL_RANGE = 64;
    public static final double MIN_TICK_BUDGET_MS = 0.2;
    public static final double MAX_TICK_BUDGET_MS = 10.0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(LightLite.MOD_ID + ".json");

    private static LightLiteConfig instance = new LightLiteConfig();

    public boolean enabled = true;
    public Mode mode = Mode.TILE;
    public int horizontalRange = 32;
    public int verticalRange = 16;
    /** ARGB hex, e.g. "66FF2A2A". Shown where mobs can spawn at any time. */
    public String alwaysColor = "70FF2A2A";
    /** ARGB hex. Shown where mobs can spawn only at night. */
    public String nightColor = "70FFD21E";
    public double tickBudgetMs = 1.5;
    public Backend backend = Backend.AUTO;
    public boolean onlyWhenHoldingLight = false;
    public List<String> excludedBiomes = new ArrayList<>(List.of("minecraft:mushroom_fields", "minecraft:deep_dark"));

    public static LightLiteConfig get() {
        return instance;
    }

    public static void load() {
        if (Files.isRegularFile(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                LightLiteConfig loaded = GSON.fromJson(reader, LightLiteConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                LightLite.LOGGER.warn("Failed to read {}, using defaults", PATH, e);
            }
        }
        instance.sanitize();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            LightLite.LOGGER.warn("Failed to write {}", PATH, e);
        }
    }

    public void sanitize() {
        if (mode == null) mode = Mode.TILE;
        if (backend == null) backend = Backend.AUTO;
        horizontalRange = clamp(horizontalRange, MIN_HORIZONTAL_RANGE, MAX_HORIZONTAL_RANGE);
        verticalRange = clamp(verticalRange, MIN_VERTICAL_RANGE, MAX_VERTICAL_RANGE);
        if (!(tickBudgetMs >= MIN_TICK_BUDGET_MS)) tickBudgetMs = MIN_TICK_BUDGET_MS;
        if (tickBudgetMs > MAX_TICK_BUDGET_MS) tickBudgetMs = MAX_TICK_BUDGET_MS;
        if (parseArgb(alwaysColor) == null) alwaysColor = "70FF2A2A";
        if (parseArgb(nightColor) == null) nightColor = "70FFD21E";
        if (excludedBiomes == null) excludedBiomes = new ArrayList<>();
    }

    public int alwaysArgb() {
        Integer c = parseArgb(alwaysColor);
        return c != null ? c : 0x70FF2A2A;
    }

    public int nightArgb() {
        Integer c = parseArgb(nightColor);
        return c != null ? c : 0x70FFD21E;
    }

    private static Integer parseArgb(String s) {
        if (s == null) return null;
        String hex = s.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() == 6) hex = "FF" + hex;
        if (hex.length() != 8) return null;
        try {
            return (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
