package io.github.radishsprouts.lightlite.compat;

import io.github.radishsprouts.lightlite.LightLite;
import io.github.radishsprouts.lightlite.render.LightLitePipelines;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

/**
 * Makes the overlay visible with Iris shader packs by assigning LightLite's pipeline to the
 * pack's basic program, the same way other overlay mods do.
 *
 * <p>Called through reflection so there is no compile-time dependency on Iris, and so the call
 * keeps working when {@code RenderPipeline} moves packages between Minecraft versions.
 */
public final class IrisCompat {
    private IrisCompat() {
    }

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) return;
        try {
            Object api = Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance").invoke(null);
            Class<?> programType = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
            Object basic = programType.getField("BASIC").get(null);
            Method assign = null;
            for (Method method : api.getClass().getMethods()) {
                if (method.getName().equals("assignPipeline") && method.getParameterCount() == 2
                        && method.getParameterTypes()[1] == programType
                        && method.getParameterTypes()[0].isInstance(LightLitePipelines.OVERLAY)) {
                    assign = method;
                    break;
                }
            }
            if (assign == null) throw new NoSuchMethodException("IrisApi.assignPipeline");
            assign.invoke(api, LightLitePipelines.OVERLAY, basic);
            LightLite.LOGGER.info("Assigned the overlay pipeline to Iris's basic program");
        } catch (ReflectiveOperationException | RuntimeException e) {
            LightLite.LOGGER.warn("Could not register with Iris; the overlay may be hidden while a shader pack is active", e);
        }
    }
}
