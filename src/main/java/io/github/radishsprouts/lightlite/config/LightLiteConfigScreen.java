package io.github.radishsprouts.lightlite.config;

import io.github.radishsprouts.lightlite.compat.ScreenCompat;
import io.github.radishsprouts.lightlite.render.OverlayRenderer;
import io.github.radishsprouts.lightlite.scan.OverlayManager;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * Settings screen built only from vanilla widgets, so it needs no config library.
 */
public final class LightLiteConfigScreen extends Screen {
    private static final int ROW_WIDTH = 210;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_STEP = 24;

    private final @Nullable Screen parent;
    private final LightLiteConfig cfg = LightLiteConfig.get();
    private final LightLiteConfig.Backend initialBackend = LightLiteConfig.get().backend;
    private final String initialBiomes = String.join(",", LightLiteConfig.get().excludedBiomes);

    public LightLiteConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("lightlite.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = (this.width - ROW_WIDTH) / 2;
        int y = 20;
        addRenderableWidget(new StringWidget(x, y, ROW_WIDTH, 12, this.title, this.font));
        y += 20;

        addRenderableWidget(CycleButton.onOffBuilder(cfg.enabled)
                .create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("lightlite.config.enabled"),
                        (button, value) -> cfg.enabled = value));
        y += ROW_STEP;

        addRenderableWidget(CycleButton.<LightLiteConfig.Mode>builder(
                        mode -> Component.translatable("lightlite.mode." + mode.name().toLowerCase(Locale.ROOT)), cfg.mode)
                .withValues(LightLiteConfig.Mode.values())
                .create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("lightlite.config.mode"),
                        (button, value) -> {
                            cfg.mode = value;
                            OverlayManager.get().invalidateMeshes();
                        }));
        y += ROW_STEP;

        addRenderableWidget(new ValueSlider(x, y, "lightlite.config.horizontal_range",
                LightLiteConfig.MIN_HORIZONTAL_RANGE, LightLiteConfig.MAX_HORIZONTAL_RANGE, 8, cfg.horizontalRange,
                v -> String.valueOf((int) v), v -> cfg.horizontalRange = (int) v));
        y += ROW_STEP;

        addRenderableWidget(new ValueSlider(x, y, "lightlite.config.vertical_range",
                LightLiteConfig.MIN_VERTICAL_RANGE, LightLiteConfig.MAX_VERTICAL_RANGE, 4, cfg.verticalRange,
                v -> String.valueOf((int) v), v -> cfg.verticalRange = (int) v));
        y += ROW_STEP;

        addRenderableWidget(new ValueSlider(x, y, "lightlite.config.grid_distance",
                0, 64, 4, cfg.gridDistance,
                v -> String.valueOf((int) v), v -> {
                    cfg.gridDistance = (int) v;
                }));
        y += ROW_STEP;

        addRenderableWidget(CycleButton.onOffBuilder(cfg.onlyWhenHoldingLight)
                .create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("lightlite.config.only_when_holding_light"),
                        (button, value) -> cfg.onlyWhenHoldingLight = value));
        y += ROW_STEP;

        addRenderableWidget(new ValueSlider(x, y, "lightlite.config.tick_budget",
                0.5, 5.0, 0.25, cfg.tickBudgetMs,
                v -> String.format(Locale.ROOT, "%.2f ms", v), v -> cfg.tickBudgetMs = v));
        y += ROW_STEP;

        addRenderableWidget(CycleButton.<LightLiteConfig.Backend>builder(
                        backend -> Component.translatable("lightlite.backend." + backend.name().toLowerCase(Locale.ROOT)), cfg.backend)
                .withValues(LightLiteConfig.Backend.values())
                .create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("lightlite.config.backend"),
                        (button, value) -> cfg.backend = value));
        y += ROW_STEP + 8;

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
                .build());
    }

    @Override
    public void onClose() {
        cfg.sanitize();
        LightLiteConfig.save();
        if (cfg.backend != initialBackend) OverlayRenderer.get().resetBackend();
        if (!initialBiomes.equals(String.join(",", cfg.excludedBiomes))) OverlayManager.get().rescanAll();
        OverlayManager.get().invalidateMeshes();
        ScreenCompat.setScreen(parent);
    }

    /** Slider over [min, max] snapped to {@code step}. */
    private static final class ValueSlider extends AbstractSliderButton {
        private final String key;
        private final double min;
        private final double max;
        private final double step;
        private final DoubleFunction<String> format;
        private final DoubleConsumer apply;

        ValueSlider(int x, int y, String key, double min, double max, double step, double current,
                    DoubleFunction<String> format, DoubleConsumer apply) {
            super(x, y, ROW_WIDTH, ROW_HEIGHT, Component.empty(), (clamp(current, min, max) - min) / (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.step = step;
            this.format = format;
            this.apply = apply;
            updateMessage();
        }

        private double current() {
            double raw = min + value * (max - min);
            return clamp(Math.round(raw / step) * step, min, max);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(key, format.apply(current())));
        }

        @Override
        protected void applyValue() {
            apply.accept(current());
        }

        private static double clamp(double v, double lo, double hi) {
            return Math.max(lo, Math.min(hi, v));
        }
    }
}
