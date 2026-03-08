package client.module.impl.movement;

import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.EnumSetting;
import client.setting.FloatSetting;
import client.setting.SettingGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;

/**
 * Speed — increases movement speed using various methods.
 */
public final class SpeedModule extends Module
{
    public enum SpeedMode
    {
        VANILLA,
        STRAFE,
        BHOP
    }

    private final EnumSetting<SpeedMode> mode;
    private final FloatSetting speed;
    private final BoolSetting potionCheck;

    private int stage;
    private double moveSpeed;
    private double lastDist;

    public SpeedModule()
    {
        super("speed", "Speed", Category.MOVEMENT);
        SettingGroup general = this.addGroup("general", "General");
        this.mode = this.addSetting(new EnumSetting<SpeedMode>("mode", "Mode", "Speed method", SpeedMode.class, SpeedMode.VANILLA));
        this.speed = this.addSetting(new FloatSetting("speed", "Speed", "Speed multiplier", 1.5F, 0.5F, 5.0F, 0.1F));
        this.potionCheck = this.addSetting(new BoolSetting("potion_check", "Potion Check", "Boost speed with speed potion", true));
        general.add(this.mode);
        general.add(this.speed);
        general.add(this.potionCheck);
    }

    public void onEnable()
    {
        this.stage = 0;
        this.moveSpeed = 0.0D;
        this.lastDist = 0.0D;
    }

    public void onDisable()
    {
        this.stage = 0;
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null)
        {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;
        float multiplier = this.speed.get().floatValue();

        switch (this.mode.get())
        {
            case VANILLA:
                this.handleVanilla(player, multiplier);
                break;

            case STRAFE:
                this.handleStrafe(player, multiplier);
                break;

            case BHOP:
                this.handleBhop(player, multiplier);
                break;
        }
    }

    private void handleVanilla(EntityPlayerSP player, float multiplier)
    {
        if (!this.isMoving(player))
        {
            return;
        }

        float baseSpeed = 0.2873F * multiplier;

        if (this.potionCheck.isEnabled() && player.isPotionActive(Potion.moveSpeed))
        {
            int amp = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            baseSpeed *= 1.0F + 0.2F * (float)(amp + 1);
        }

        float forward = player.moveForward;
        float strafe = player.moveStrafing;
        float yaw = player.rotationYaw;

        if (forward == 0.0F && strafe == 0.0F)
        {
            return;
        }

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
        player.motionX = (double)(forward * baseSpeed) * -Math.sin(rad) + (double)(strafe * baseSpeed) * Math.cos(rad);
        player.motionZ = (double)(forward * baseSpeed) * Math.cos(rad) + (double)(strafe * baseSpeed) * -Math.sin(rad);
    }

    private void handleStrafe(EntityPlayerSP player, float multiplier)
    {
        if (!this.isMoving(player))
        {
            this.moveSpeed = 0.0D;
            return;
        }

        double baseSpeed = 0.2873D * (double) multiplier;

        if (this.potionCheck.isEnabled() && player.isPotionActive(Potion.moveSpeed))
        {
            int amp = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            baseSpeed *= 1.0D + 0.2D * (double)(amp + 1);
        }

        if (player.onGround)
        {
            this.moveSpeed = baseSpeed * 1.38D;
            player.motionY = 0.42D;
        }
        else
        {
            this.moveSpeed = this.lastDist - this.lastDist / 159.0D;
        }

        this.moveSpeed = Math.max(this.moveSpeed, baseSpeed);
        this.setMotion(player, this.moveSpeed);
        double dx = player.posX - player.prevPosX;
        double dz = player.posZ - player.prevPosZ;
        this.lastDist = Math.sqrt(dx * dx + dz * dz);
    }

    private void handleBhop(EntityPlayerSP player, float multiplier)
    {
        if (!this.isMoving(player))
        {
            return;
        }

        if (player.onGround)
        {
            player.motionY = 0.42D;
        }

        float baseSpeed = 0.2873F * multiplier;

        if (this.potionCheck.isEnabled() && player.isPotionActive(Potion.moveSpeed))
        {
            int amp = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            baseSpeed *= 1.0F + 0.2F * (float)(amp + 1);
        }

        this.setMotion(player, (double) baseSpeed);
    }

    private void setMotion(EntityPlayerSP player, double speed)
    {
        float forward = player.moveForward;
        float strafe = player.moveStrafing;
        float yaw = player.rotationYaw;

        if (forward == 0.0F && strafe == 0.0F)
        {
            player.motionX = 0.0D;
            player.motionZ = 0.0D;
            return;
        }

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
        player.motionX = (double) forward * speed * -Math.sin(rad) + (double) strafe * speed * Math.cos(rad);
        player.motionZ = (double) forward * speed * Math.cos(rad) + (double) strafe * speed * -Math.sin(rad);
    }

    private boolean isMoving(EntityPlayerSP player)
    {
        return player.moveForward != 0.0F || player.moveStrafing != 0.0F;
    }
}
