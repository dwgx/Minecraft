package client.module.impl.world;

import client.module.Category;
import client.module.Module;
import client.setting.IntSetting;
import client.setting.SettingGroup;
import net.minecraft.client.Minecraft;

/**
 * FastPlace — reduces the right-click delay timer for faster block placement.
 */
public final class FastPlaceModule extends Module
{
    private final IntSetting delay;

    public FastPlaceModule()
    {
        super("fast_place", "FastPlace", Category.WORLD);
        SettingGroup general = this.addGroup("general", "General");
        this.delay = this.addSetting(new IntSetting("delay", "Delay", "Right-click delay ticks", 0, 0, 4, 1));
        general.add(this.delay);
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null)
        {
            return;
        }

        mc.setRightClickDelayTimer(this.delay.get().intValue());
    }
}
