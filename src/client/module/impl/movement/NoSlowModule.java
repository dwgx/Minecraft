package client.module.impl.movement;

import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.FloatSetting;
import client.setting.SettingGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

/**
 * NoSlow — prevents movement slowdown while using items (eating, blocking, drawing bow).
 */
public final class NoSlowModule extends Module
{
    private final FloatSetting multiplier;
    private final BoolSetting swords;
    private final BoolSetting bows;
    private final BoolSetting food;
    private final BoolSetting potions;

    public NoSlowModule()
    {
        super("no_slow", "NoSlow", Category.MOVEMENT);
        SettingGroup general = this.addGroup("general", "General");
        this.multiplier = this.addSetting(new FloatSetting("multiplier", "Multiplier", "Speed multiplier while using items", 1.0F, 0.2F, 1.0F, 0.05F));
        this.swords = this.addSetting(new BoolSetting("swords", "Swords", "No slow while blocking", true));
        this.bows = this.addSetting(new BoolSetting("bows", "Bows", "No slow while drawing bow", true));
        this.food = this.addSetting(new BoolSetting("food", "Food", "No slow while eating", true));
        this.potions = this.addSetting(new BoolSetting("potions", "Potions", "No slow while drinking", true));
        general.add(this.multiplier);
        general.add(this.swords);
        general.add(this.bows);
        general.add(this.food);
        general.add(this.potions);
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.currentScreen != null)
        {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;

        if (!player.isUsingItem())
        {
            return;
        }

        ItemStack held = player.getHeldItem();

        if (held == null)
        {
            return;
        }

        boolean shouldApply = false;

        if (this.swords.isEnabled() && held.getItem() instanceof ItemSword)
        {
            shouldApply = true;
        }
        else if (this.bows.isEnabled() && held.getItem() instanceof ItemBow)
        {
            shouldApply = true;
        }
        else if (this.food.isEnabled() && held.getItem() instanceof ItemFood)
        {
            shouldApply = true;
        }
        else if (this.potions.isEnabled() && held.getItem() instanceof ItemPotion)
        {
            shouldApply = true;
        }

        if (shouldApply)
        {
            float mult = this.multiplier.get().floatValue();
            player.movementInput.moveForward *= mult / 0.2F;
            player.movementInput.moveStrafe *= mult / 0.2F;
        }
    }
}
