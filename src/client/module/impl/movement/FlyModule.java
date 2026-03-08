package client.module.impl.movement;

import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.EnumSetting;
import client.setting.FloatSetting;
import client.setting.SettingGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

/**
 * Fly — allows the player to fly in survival mode.
 */
public final class FlyModule extends Module
{
    public enum FlyMode
    {
        VANILLA,
        CREATIVE,
        GLIDE
    }

    private final EnumSetting<FlyMode> mode;
    private final FloatSetting speed;
    private final BoolSetting antiKick;

    private int tickCounter;

    public FlyModule()
    {
        super("fly", "Fly", Category.MOVEMENT);
        SettingGroup general = this.addGroup("general", "General");
        this.mode = this.addSetting(new EnumSetting<FlyMode>("mode", "Mode", "Fly method", FlyMode.class, FlyMode.VANILLA));
        this.speed = this.addSetting(new FloatSetting("speed", "Speed", "Fly speed", 2.0F, 0.5F, 10.0F, 0.5F));
        this.antiKick = this.addSetting(new BoolSetting("anti_kick", "Anti Kick", "Prevent fly kick detection", true));
        general.add(this.mode);
        general.add(this.speed);
        general.add(this.antiKick);
    }

    public void onEnable()
    {
        this.tickCounter = 0;
    }

    public void onDisable()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc != null && mc.thePlayer != null)
        {
            mc.thePlayer.capabilities.isFlying = false;
            mc.thePlayer.capabilities.setFlySpeed(0.05F);
        }

        this.tickCounter = 0;
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null)
        {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;
        float flySpeed = this.speed.get().floatValue();
        ++this.tickCounter;

        switch (this.mode.get())
        {
            case VANILLA:
                this.handleVanilla(player, flySpeed);
                break;

            case CREATIVE:
                this.handleCreative(player, flySpeed);
                break;

            case GLIDE:
                this.handleGlide(player, flySpeed);
                break;
        }

        if (this.antiKick.isEnabled() && this.tickCounter % 40 == 0)
        {
            player.motionY = -0.04D;
        }
    }

    private void handleVanilla(EntityPlayerSP player, float speed)
    {
        player.motionY = 0.0D;
        player.motionX = 0.0D;
        player.motionZ = 0.0D;

        float forward = player.moveForward;
        float strafe = player.moveStrafing;
        float yaw = player.rotationYaw;

        if (forward != 0.0F || strafe != 0.0F)
        {
            if (forward != 0.0F)
            {
                if (strafe > 0.0F)
                {
                    yaw -= forward > 0.0F ? 45.0F : -45.0F;
                }
                else if (strafe < 0.0F)
                {
                    yaw += forward > 0.0F ? 45.0F : -45.0F;
                }

                strafe = 0.0F;
                forward = forward > 0.0F ? 1.0F : -1.0F;
            }

            double rad = Math.toRadians((double) yaw);
            player.motionX = (double) forward * (double) speed * 0.2D * -Math.sin(rad) + (double) strafe * (double) speed * 0.2D * Math.cos(rad);
            player.motionZ = (double) forward * (double) speed * 0.2D * Math.cos(rad) + (double) strafe * (double) speed * 0.2D * -Math.sin(rad);
        }

        if (player.movementInput.jump)
        {
            player.motionY = (double) speed * 0.2D;
        }

        if (player.movementInput.sneak)
        {
            player.motionY = (double) -speed * 0.2D;
        }
    }

    private void handleCreative(EntityPlayerSP player, float speed)
    {
        player.capabilities.isFlying = true;
        player.capabilities.setFlySpeed(speed * 0.05F);
    }

    private void handleGlide(EntityPlayerSP player, float speed)
    {
        if (player.motionY < 0.0D)
        {
            player.motionY *= 0.6D;
        }

        if (player.moveForward != 0.0F || player.moveStrafing != 0.0F)
        {
            float yaw = player.rotationYaw;
            float forward = player.moveForward;
            float strafe = player.moveStrafing;

            if (forward != 0.0F)
            {
                if (strafe > 0.0F)
                {
                    yaw -= forward > 0.0F ? 45.0F : -45.0F;
                }
                else if (strafe < 0.0F)
                {
                    yaw += forward > 0.0F ? 45.0F : -45.0F;
                }

                strafe = 0.0F;
                forward = forward > 0.0F ? 1.0F : -1.0F;
            }

            double rad = Math.toRadians((double) yaw);
            double glideSpeed = (double) speed * 0.12D;
            player.motionX = (double) forward * glideSpeed * -Math.sin(rad);
            player.motionZ = (double) forward * glideSpeed * Math.cos(rad);
        }
    }
}
