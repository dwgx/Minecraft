package client.hud;

/**
 * Central builtin HUD element list.
 */
public final class HudRegistry
{
    private HudRegistry()
    {
    }

    public static void registerBuiltins(HudManager hud)
    {
        if (hud == null)
        {
            return;
        }

        hud.register(new HudFpsElement());
    }
}
