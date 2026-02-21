package client.module.impl.client;

import client.core.ClientBootstrap;
import client.hud.HudEditorScreen;
import client.module.Category;
import client.module.Module;
import net.minecraft.client.Minecraft;

public final class HudEditModule extends Module
{
    public HudEditModule()
    {
        super("hud_edit", "HudEdit", Category.RENDER);
    }

    public void onEnable()
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (!ClientBootstrap.instance().isNanoAvailable())
        {
            ClientBootstrap.instance().notifyUser(ClientBootstrap.instance().getI18n().translateOrDefault("error.nanovg_unavailable.hudedit", "\u00a7cNanoVG unavailable; HudEdit disabled."));
            return;
        }
        Module uiScale = ClientBootstrap.instance().getModules().getById("ui_scale_edit");

        if (uiScale instanceof UiScaleEditModule)
        {
            ((UiScaleEditModule)uiScale).setEditTarget(UiScaleEditModule.UiTarget.HUD_EDIT);
        }

        if (mc != null)
        {
            if (mc.currentScreen instanceof HudEditorScreen)
            {
                mc.displayGuiScreen(null);
            }
            else
            {
                mc.displayGuiScreen(new HudEditorScreen(ClientBootstrap.instance().getHud()));
            }
        }
    }

    public boolean isActionModule()
    {
        return true;
    }
}
