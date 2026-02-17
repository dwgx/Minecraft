package client.module.impl.movement;

import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.SettingGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

public final class KeepSprintModule extends Module
{
    private final BoolSetting airSprint;
    private final BoolSetting useItemSprint;

    public KeepSprintModule()
    {
        super("keep_sprint", "KeepSprint", Category.MOVEMENT);
        SettingGroup group = this.addGroup("general", "通用");
        this.airSprint = this.addSetting(new BoolSetting("air_sprint", "空中疾跑", "允许在离地状态维持疾跑", true));
        this.useItemSprint = this.addSetting(new BoolSetting("use_item_sprint", "使用物品时疾跑", "手持食物/物品时保持疾跑", false));
        group.add(this.airSprint);
        group.add(this.useItemSprint);
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null)
        {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;

        if (player.isSneaking())
        {
            return;
        }

        if (!this.airSprint.isEnabled() && !player.onGround)
        {
            return;
        }

        if (!this.useItemSprint.isEnabled() && player.isUsingItem())
        {
            return;
        }

        boolean movingForward = player.moveForward > 0.0F;
        boolean hasFood = player.capabilities.allowFlying || player.getFoodStats().getFoodLevel() > 6;

        if (movingForward && hasFood)
        {
            player.setSprinting(true);
        }
    }
}
