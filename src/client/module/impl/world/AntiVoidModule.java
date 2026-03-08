package client.module.impl.world;

import client.module.Category;
import client.module.Module;
import client.setting.FloatSetting;
import client.setting.SettingGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;

/**
 * AntiVoid — detects when the player is falling into the void and
 * teleports them back to the last safe position.
 */
public final class AntiVoidModule extends Module
{
    private final FloatSetting fallDistance;

    private double safeX;
    private double safeY;
    private double safeZ;
    private boolean hasSafePos;

    public AntiVoidModule()
    {
        super("anti_void", "AntiVoid", Category.WORLD);
        SettingGroup general = this.addGroup("general", "General");
        this.fallDistance = this.addSetting(new FloatSetting("fall_distance", "Fall Distance", "Minimum fall distance to trigger", 5.0F, 2.0F, 15.0F, 1.0F));
        general.add(this.fallDistance);
    }

    public void onEnable()
    {
        this.hasSafePos = false;
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.theWorld == null)
        {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;

        // Save safe position when on ground
        if (player.onGround && player.posY > 0.0D)
        {
            this.safeX = player.posX;
            this.safeY = player.posY;
            this.safeZ = player.posZ;
            this.hasSafePos = true;
        }

        // Check for void fall
        if (!this.hasSafePos)
        {
            return;
        }

        if (player.fallDistance >= this.fallDistance.get().floatValue() && !this.hasBlockBelow(player))
        {
            player.setPosition(this.safeX, this.safeY, this.safeZ);
            player.motionX = 0.0D;
            player.motionY = 0.0D;
            player.motionZ = 0.0D;
            player.fallDistance = 0.0F;
        }
    }

    private boolean hasBlockBelow(EntityPlayerSP player)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.theWorld == null)
        {
            return false;
        }

        int px = (int) Math.floor(player.posX);
        int pz = (int) Math.floor(player.posZ);

        for (int y = (int) player.posY; y >= 0; --y)
        {
            BlockPos pos = new BlockPos(px, y, pz);

            if (!mc.theWorld.isAirBlock(pos))
            {
                return true;
            }
        }

        return false;
    }
}
