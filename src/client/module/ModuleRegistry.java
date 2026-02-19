package client.module;

import client.module.impl.client.ClickGuiModule;
import client.module.impl.client.HudEditModule;
import client.module.impl.client.UiScaleEditModule;
import client.module.impl.combat.AutoClickerModule;
import client.module.impl.combat.KillAuraModule;
import client.module.impl.misc.InventoryMoveModule;
import client.module.impl.movement.EagleModule;
import client.module.impl.movement.KeepSprintModule;
import client.module.impl.movement.ScaffoldModule;
import client.module.impl.render.TargetPanelModule;
import client.module.impl.world.AutoToolModule;
import client.module.impl.world.StealerModule;

/**
 * Central builtin module list. Keep registration order stable for config behavior.
 */
public final class ModuleRegistry
{
    private ModuleRegistry()
    {
    }

    public static void registerBuiltins(ModuleManager modules)
    {
        if (modules == null)
        {
            return;
        }

        modules.register(new ClickGuiModule());
        modules.register(new HudEditModule());
        modules.register(new UiScaleEditModule());
        modules.register(new KillAuraModule());
        modules.register(new AutoClickerModule());
        modules.register(new EagleModule());
        modules.register(new KeepSprintModule());
        modules.register(new ScaffoldModule());
        modules.register(new AutoToolModule());
        modules.register(new StealerModule());
        modules.register(new InventoryMoveModule());
        modules.register(new TargetPanelModule());
    }
}
