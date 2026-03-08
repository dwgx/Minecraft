package client.module.impl.movement;

import client.module.Category;
import client.module.Module;
import client.setting.EnumSetting;
import client.setting.SettingGroup;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;

/**
 * NoFall — prevents fall damage by spoofing onGround in outbound packets.
 */
public final class NoFallModule extends Module
{
    public enum NoFallMode
    {
        PACKET,
        GROUND_SPOOF
    }

    private final EnumSetting<NoFallMode> mode;

    public NoFallModule()
    {
        super("no_fall", "NoFall", Category.MOVEMENT);
        SettingGroup general = this.addGroup("general", "General");
        this.mode = this.addSetting(new EnumSetting<NoFallMode>("mode", "Mode", "NoFall method", NoFallMode.class, NoFallMode.PACKET));
        general.add(this.mode);
    }

    public boolean onPacketSend(Packet<?> packet)
    {
        if (packet instanceof C03PacketPlayer)
        {
            C03PacketPlayer playerPacket = (C03PacketPlayer) packet;

            if (this.mode.get() == NoFallMode.PACKET)
            {
                playerPacket.setOnGround(true);
            }
            else if (this.mode.get() == NoFallMode.GROUND_SPOOF)
            {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();

                if (mc != null && mc.thePlayer != null && mc.thePlayer.fallDistance > 2.0F)
                {
                    playerPacket.setOnGround(true);
                }
            }
        }

        return false;
    }
}
