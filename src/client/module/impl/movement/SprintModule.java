package client.module.impl.movement;

import client.module.Category;
import client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;

/**
 * Sprint — automatically holds sprint while moving.
 */
public final class SprintModule extends Module
{
    public SprintModule()
    {
        super("sprint", "Sprint", Category.MOVEMENT);
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.currentScreen != null)
        {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;

        if (player.moveForward > 0.0F && !player.isSneaking()
                && !player.isCollidedHorizontally && player.getFoodStats().getFoodLevel() > 6)
        {
            player.setSprinting(true);
        }
    }
}
