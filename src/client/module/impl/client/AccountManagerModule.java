package client.module.impl.client;

import client.auth.gui.GuiAccountManagerScreen;
import client.core.ClientBootstrap;
import client.module.Category;
import client.module.Module;
import net.minecraft.client.Minecraft;

/**
 * Client action module that opens multi-account manager UI.
 */
public final class AccountManagerModule extends Module
{
    public AccountManagerModule()
    {
        super("account_manager", "AccountManager", Category.CLIENT);
    }

    public void onEnable()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (!ClientBootstrap.instance().isNanoAvailable())
        {
            ClientBootstrap.instance().notifyUser(ClientBootstrap.instance().getI18n().translateOrDefault("error.nanovg_unavailable.accountmanager", "\u00a7cNanoVG unavailable; AccountManager disabled."));
            return;
        }

        if (mc != null)
        {
            if (mc.currentScreen instanceof GuiAccountManagerScreen)
            {
                mc.displayGuiScreen(null);
            }
            else
            {
                mc.displayGuiScreen(new GuiAccountManagerScreen(mc.currentScreen));
            }
        }
    }

    public boolean isActionModule()
    {
        return true;
    }
}
