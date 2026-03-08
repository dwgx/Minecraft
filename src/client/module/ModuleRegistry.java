package client.module;

import client.module.impl.client.AccountManagerModule;
import client.module.impl.client.IRCGuiModule;
import client.module.impl.client.ClickGuiModule;
import client.module.impl.client.HudEditModule;
import client.module.impl.client.UiScaleEditModule;
import client.module.impl.combat.AutoClickerModule;
import client.module.impl.combat.KillAuraModule;
import client.module.impl.combat.VelocityModule;
import client.module.impl.misc.InventoryMoveModule;
import client.module.impl.misc.SkinChangerModule;
import client.module.impl.movement.EagleModule;
import client.module.impl.movement.FlyModule;
import client.module.impl.movement.KeepSprintModule;
import client.module.impl.movement.NoFallModule;
import client.module.impl.movement.NoPushModule;
import client.module.impl.movement.NoSlowModule;
import client.module.impl.movement.SpeedModule;
import client.module.impl.movement.SprintModule;
import client.module.impl.world.AntiVoidModule;
import client.module.impl.world.AutoToolModule;
import client.module.impl.world.FastPlaceModule;
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
        modules.register(new AccountManagerModule());
        modules.register(new HudEditModule());
        modules.register(new UiScaleEditModule());
        modules.register(new IRCGuiModule());
        modules.register(new AutoClickerModule());
        modules.register(new KillAuraModule());
        modules.register(new VelocityModule());
        modules.register(new EagleModule());
        modules.register(new KeepSprintModule());
        modules.register(new NoFallModule());
        modules.register(new SpeedModule());
        modules.register(new FlyModule());
        modules.register(new NoSlowModule());
        modules.register(new SprintModule());
        modules.register(new NoPushModule());
        modules.register(new AutoToolModule());
        modules.register(new StealerModule());
        modules.register(new FastPlaceModule());
        modules.register(new AntiVoidModule());
        modules.register(new SkinChangerModule());
        modules.register(new InventoryMoveModule());
    }
}
