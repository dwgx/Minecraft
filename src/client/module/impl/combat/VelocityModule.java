package client.module.impl.combat;

import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.FloatSetting;
import client.setting.SettingGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;

/**
 * Velocity — reduces or cancels knockback from damage and explosions.
 */
public final class VelocityModule extends Module
{
    private final FloatSetting horizontal;
    private final FloatSetting vertical;
    private final BoolSetting explosions;

    public VelocityModule()
    {
        super("velocity", "Velocity", Category.COMBAT);
        SettingGroup general = this.addGroup("general", "General");
        this.horizontal = this.addSetting(new FloatSetting("horizontal", "Horizontal", "Horizontal knockback multiplier", 0.0F, 0.0F, 1.0F, 0.05F));
        this.vertical = this.addSetting(new FloatSetting("vertical", "Vertical", "Vertical knockback multiplier", 0.0F, 0.0F, 1.0F, 0.05F));
        this.explosions = this.addSetting(new BoolSetting("explosions", "Explosions", "Also reduce explosion knockback", true));
        general.add(this.horizontal);
        general.add(this.vertical);
        general.add(this.explosions);
    }

    public boolean onPacketReceive(Packet<?> packet)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null)
        {
            return false;
        }

        if (packet instanceof S12PacketEntityVelocity)
        {
            S12PacketEntityVelocity velocity = (S12PacketEntityVelocity) packet;

            if (velocity.getEntityID() == mc.thePlayer.getEntityId())
            {
                float h = this.horizontal.get().floatValue();
                float v = this.vertical.get().floatValue();

                if (h == 0.0F && v == 0.0F)
                {
                    return true;
                }

                mc.thePlayer.motionX = (double) velocity.getMotionX() / 8000.0D * (double) h;
                mc.thePlayer.motionY = (double) velocity.getMotionY() / 8000.0D * (double) v;
                mc.thePlayer.motionZ = (double) velocity.getMotionZ() / 8000.0D * (double) h;
                return true;
            }
        }

        if (this.explosions.isEnabled() && packet instanceof S27PacketExplosion)
        {
            S27PacketExplosion explosion = (S27PacketExplosion) packet;
            float h = this.horizontal.get().floatValue();
            float v = this.vertical.get().floatValue();

            if (h == 0.0F && v == 0.0F)
            {
                mc.thePlayer.motionX += (double) explosion.func_149149_c() * 0.0D;
                mc.thePlayer.motionY += (double) explosion.func_149144_d() * 0.0D;
                mc.thePlayer.motionZ += (double) explosion.func_149147_e() * 0.0D;
            }
            else
            {
                mc.thePlayer.motionX += (double) explosion.func_149149_c() * (double) h;
                mc.thePlayer.motionY += (double) explosion.func_149144_d() * (double) v;
                mc.thePlayer.motionZ += (double) explosion.func_149147_e() * (double) h;
            }

            return true;
        }

        return false;
    }
}
