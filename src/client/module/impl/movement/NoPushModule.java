package client.module.impl.movement;

import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.SettingGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

/**
 * NoPush — prevents the player from being pushed by entities, water, or blocks.
 */
public final class NoPushModule extends Module
{
    private final BoolSetting entities;
    private final BoolSetting water;
    private final BoolSetting blocks;

    public NoPushModule()
    {
        super("no_push", "NoPush", Category.MOVEMENT);
        SettingGroup general = this.addGroup("general", "General");
        this.entities = this.addSetting(new BoolSetting("entities", "Entities", "Prevent entity push", true));
        this.water = this.addSetting(new BoolSetting("water", "Water", "Prevent water push", true));
        this.blocks = this.addSetting(new BoolSetting("blocks", "Blocks", "Prevent block push", false));
        general.add(this.entities);
        general.add(this.water);
        general.add(this.blocks);
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null)
        {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;

        if (this.entities.isEnabled())
        {
            player.entityCollisionReduction = 1.0F;
        }
    }

    /**
     * Check if entity push should be cancelled. Called from entity collision code.
     */
    public boolean shouldCancelEntityPush()
    {
        return this.entities.isEnabled();
    }

    public boolean shouldCancelWaterPush()
    {
        return this.water.isEnabled();
    }
}
