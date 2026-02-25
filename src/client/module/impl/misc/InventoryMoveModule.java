package client.module.impl.misc;

import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.SettingGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.KeyBinding;
import client.runtime.lwjgl.GlfwKeyboard;

/**
 * Allows movement keys while inventory/container screens are open.
 */
public final class InventoryMoveModule extends Module
{
    private final BoolSetting allowJump;
    private final BoolSetting allowSneak;
    private final BoolSetting allowSprint;
    private final BoolSetting allowInChat;

    public InventoryMoveModule()
    {
        super("inventory_move", "InventoryMove", Category.MISC);
        SettingGroup group = this.addGroup("general", "General");
        this.allowJump = this.addSetting(new BoolSetting("allow_jump", "Allow Jump", "Keep jump input active while GUI is open", true));
        this.allowSneak = this.addSetting(new BoolSetting("allow_sneak", "Allow Sneak", "Keep sneak input active while GUI is open", false));
        this.allowSprint = this.addSetting(new BoolSetting("allow_sprint", "Allow Sprint", "Keep sprint input active while GUI is open", true));
        this.allowInChat = this.addSetting(new BoolSetting("allow_in_chat", "Allow In Chat", "Apply inventory move in chat screen too", false));
        group.add(this.allowJump);
        group.add(this.allowSneak);
        group.add(this.allowSprint);
        group.add(this.allowInChat);
    }

    public void onDisable()
    {
        this.restoreAll();
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.gameSettings == null)
        {
            return;
        }

        if (mc.currentScreen == null)
        {
            this.restoreAll();
            return;
        }

        if (!this.allowInChat.isEnabled() && mc.currentScreen instanceof GuiChat)
        {
            this.restoreAll();
            return;
        }

        this.syncKey(mc.gameSettings.keyBindForward, true);
        this.syncKey(mc.gameSettings.keyBindBack, true);
        this.syncKey(mc.gameSettings.keyBindLeft, true);
        this.syncKey(mc.gameSettings.keyBindRight, true);
        this.syncKey(mc.gameSettings.keyBindJump, this.allowJump.isEnabled());
        this.syncKey(mc.gameSettings.keyBindSneak, this.allowSneak.isEnabled());
        this.syncKey(mc.gameSettings.keyBindSprint, this.allowSprint.isEnabled());
    }

    private void restoreAll()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.gameSettings == null)
        {
            return;
        }

        this.restoreKey(mc.gameSettings.keyBindForward);
        this.restoreKey(mc.gameSettings.keyBindBack);
        this.restoreKey(mc.gameSettings.keyBindLeft);
        this.restoreKey(mc.gameSettings.keyBindRight);
        this.restoreKey(mc.gameSettings.keyBindJump);
        this.restoreKey(mc.gameSettings.keyBindSneak);
        this.restoreKey(mc.gameSettings.keyBindSprint);
    }

    private void syncKey(KeyBinding binding, boolean allowed)
    {
        if (binding == null)
        {
            return;
        }

        int code = binding.getKeyCode();
        boolean physical = this.isPhysicalKeyDown(code);
        KeyBinding.setKeyBindState(code, allowed && physical);
    }

    private void restoreKey(KeyBinding binding)
    {
        if (binding == null)
        {
            return;
        }

        int code = binding.getKeyCode();
        KeyBinding.setKeyBindState(code, this.isPhysicalKeyDown(code));
    }

    private boolean isPhysicalKeyDown(int keyCode)
    {
        return keyCode > 0 && keyCode < 256 && GlfwKeyboard.isKeyDown(keyCode);
    }
}

