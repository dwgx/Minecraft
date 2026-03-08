package client.module.impl.client;

import client.core.ClientBootstrap;
import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.FloatSetting;
import client.setting.IntSetting;
import client.ui.template.UiAnimationBus;
import client.ui.irc.IRCGuiScreen;
import net.minecraft.client.Minecraft;

public final class IRCGuiModule extends Module
{
    private final BoolSetting soundEnabled;
    private final BoolSetting notificationsEnabled;
    private final IntSetting maxHistorySize;
    private final FloatSetting overlayOpacity;
    private final BoolSetting compactMode;
    private final BoolSetting allowDm;
    private final BoolSetting showOnlineStatus;

    public IRCGuiModule()
    {
        super("chat_overlay", "IRCGUI", Category.CLIENT);

        this.soundEnabled = this.addSetting(
            new BoolSetting("chat_sound", "Sound", "Enable message sounds", true));
        this.notificationsEnabled = this.addSetting(
            new BoolSetting("chat_notifications", "Notifications", "Show message notifications", true));
        this.maxHistorySize = this.addSetting(
            new IntSetting("chat_max_history", "Max History", "Messages per conversation", 500, 100, 5000, 100));
        this.overlayOpacity = this.addSetting(
            new FloatSetting("chat_overlay_opacity", "Overlay Opacity", "Background dimming", 0.65F, 0.0F, 1.0F, 0.05F));
        this.compactMode = this.addSetting(
            new BoolSetting("chat_compact", "Compact Mode", "Reduce message spacing", false));
        this.allowDm = this.addSetting(
            new BoolSetting("chat_allow_dm", "Allow DM", "Allow others to send you direct messages", true));
        this.showOnlineStatus = this.addSetting(
            new BoolSetting("chat_show_status", "Show Status", "Show your online status to others", true));
    }

    public void onEnable()
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (!ClientBootstrap.instance().isNanoAvailable()) return;
        if (mc != null)
        {
            if (mc.currentScreen instanceof IRCGuiScreen)
            {
                mc.displayGuiScreen(null);
            }
            else
            {
                UiAnimationBus.clearPrefix("clickgui.");
                mc.displayGuiScreen(new IRCGuiScreen(this));
            }
        }
    }

    @Override
    public void onDisable()
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.currentScreen instanceof IRCGuiScreen)
        {
            mc.displayGuiScreen(null);
        }
        UiAnimationBus.clearPrefix("chat.");
    }

    public boolean isActionModule() { return true; }

    public boolean isSoundEnabled() { return this.soundEnabled.isEnabled(); }
    public boolean isNotificationsEnabled() { return this.notificationsEnabled.isEnabled(); }
    public int getMaxHistorySize() { return this.maxHistorySize.get().intValue(); }
    public float getOverlayOpacity() { return this.overlayOpacity.get().floatValue(); }
    public boolean isCompactMode() { return this.compactMode.isEnabled(); }
    public boolean isAllowDm() { return this.allowDm.isEnabled(); }
    public boolean isShowOnlineStatus() { return this.showOnlineStatus.isEnabled(); }
}
