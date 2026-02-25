package client.module.impl.world;

import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.SettingGroup;
import dwgx.modulekit.ModuleKit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import client.runtime.lwjgl.GlfwKeyboard;
import client.runtime.lwjgl.GlfwMouse;

/**
 * Switches to the best hotbar tool while mining.
 */
public final class AutoToolModule extends Module
{
    private final BoolSetting requireAttackKey;
    private final BoolSetting switchBack;
    private int previousSlot = -1;
    private int forcedSlot = -1;

    public AutoToolModule()
    {
        super("auto_tool", "AutoTool", Category.WORLD);
        SettingGroup group = this.addGroup("general", "General");
        this.requireAttackKey = this.addSetting(new BoolSetting("require_attack_key", "Require Attack Key", "Switch only while physical attack key is held", true));
        this.switchBack = this.addSetting(new BoolSetting("switch_back", "Switch Back", "Restore previous slot when mining stops", true));
        group.add(this.requireAttackKey);
        group.add(this.switchBack);
    }

    public void onDisable()
    {
        this.restoreSlot();
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.playerController == null || mc.currentScreen != null || mc.gameSettings == null)
        {
            this.restoreSlot();
            return;
        }

        int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
        boolean attackHeld = this.isKeyOrMouseDown(attackKey);

        if (this.requireAttackKey.isEnabled() && !attackHeld)
        {
            this.restoreSlot();
            return;
        }

        MovingObjectPosition hit = mc.objectMouseOver;

        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || hit.getBlockPos() == null)
        {
            if (mc.entityRenderer != null)
            {
                mc.entityRenderer.getMouseOver(1.0F);
            }

            hit = mc.objectMouseOver;
        }

        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || hit.getBlockPos() == null)
        {
            this.restoreSlot();
            return;
        }

        BlockPos blockPos = hit.getBlockPos();
        EntityPlayerSP player = mc.thePlayer;
        int bestSlot = ModuleKit.findBestToolHotbarSlot(player, blockPos);

        if (bestSlot < 0 || bestSlot == player.inventory.currentItem)
        {
            if (bestSlot < 0)
            {
                this.restoreSlot();
            }
            return;
        }

        if (this.previousSlot < 0)
        {
            this.previousSlot = player.inventory.currentItem;
        }

        player.inventory.currentItem = bestSlot;
        this.forcedSlot = bestSlot;
        mc.playerController.updateController();
    }

    private void restoreSlot()
    {
        if (!this.switchBack.isEnabled())
        {
            this.previousSlot = -1;
            this.forcedSlot = -1;
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.playerController == null)
        {
            this.previousSlot = -1;
            this.forcedSlot = -1;
            return;
        }

        if (this.previousSlot >= 0 && this.forcedSlot >= 0 && mc.thePlayer.inventory.currentItem == this.forcedSlot)
        {
            mc.thePlayer.inventory.currentItem = this.previousSlot;
            mc.playerController.updateController();
        }

        this.previousSlot = -1;
        this.forcedSlot = -1;
    }

    private boolean isKeyOrMouseDown(int keyCode)
    {
        if (keyCode >= 0)
        {
            return keyCode < 256 && GlfwKeyboard.isKeyDown(keyCode);
        }

        int mouseButton = keyCode + 100;
        return mouseButton >= 0 && mouseButton < 16 && GlfwMouse.isButtonDown(mouseButton);
    }
}

