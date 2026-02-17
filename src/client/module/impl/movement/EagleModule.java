package client.module.impl.movement;

import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.SettingGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;

public class EagleModule extends Module
{
    private final BoolSetting onlyOnGround;
    private final BoolSetting requireBlocks;
    private boolean appliedSneak;

    public EagleModule()
    {
        super("eage", "Eage", Category.MOVEMENT);
        SettingGroup group = this.addGroup("general", "通用");
        this.onlyOnGround = this.addSetting(new BoolSetting("only_ground", "仅地面生效", "只在玩家着地时触发", true));
        this.requireBlocks = this.addSetting(new BoolSetting("require_blocks", "需要手持方块", "仅在手持可放置方块时触发", true));
        group.add(this.onlyOnGround);
        group.add(this.requireBlocks);
    }

    public void onDisable()
    {
        this.restoreSneakState();
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null)
        {
            this.restoreSneakState();
            return;
        }

        EntityPlayerSP player = mc.thePlayer;

        if (this.onlyOnGround.isEnabled() && !player.onGround)
        {
            this.restoreSneakState();
            return;
        }

        if (this.requireBlocks.isEnabled() && !this.isHoldingBlock(player.getHeldItem()))
        {
            this.restoreSneakState();
            return;
        }

        BlockPos below = new BlockPos(player.posX, player.getEntityBoundingBox().minY - 0.6D, player.posZ);
        boolean shouldSneak = mc.theWorld.isAirBlock(below);
        int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
        KeyBinding.setKeyBindState(sneakKey, shouldSneak || this.isPhysicalKeyDown(sneakKey));
        this.appliedSneak = shouldSneak;
    }

    private void restoreSneakState()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null)
        {
            this.appliedSneak = false;
            return;
        }

        int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();

        if (this.appliedSneak)
        {
            KeyBinding.setKeyBindState(sneakKey, this.isPhysicalKeyDown(sneakKey));
            this.appliedSneak = false;
        }
    }

    private boolean isHoldingBlock(ItemStack heldItem)
    {
        return heldItem != null && heldItem.getItem() instanceof ItemBlock;
    }

    private boolean isPhysicalKeyDown(int keyCode)
    {
        return keyCode > 0 && keyCode < 256 && Keyboard.isKeyDown(keyCode);
    }
}
