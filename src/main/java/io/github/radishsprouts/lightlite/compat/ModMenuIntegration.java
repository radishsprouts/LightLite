package io.github.radishsprouts.lightlite.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.radishsprouts.lightlite.config.LightLiteConfigScreen;

/** Loaded only when Mod Menu is installed. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return LightLiteConfigScreen::new;
    }
}
