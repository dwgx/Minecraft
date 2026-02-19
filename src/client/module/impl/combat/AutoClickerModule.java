package client.module.impl.combat;

import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.IntSetting;
import client.setting.SettingGroup;
import dwgx.modulekit.ModuleKit;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Lightweight CPS scheduler inspired by modern combat modules.
 */
public final class AutoClickerModule extends Module
{
    private final BoolSetting requireAttackKey;
    private final BoolSetting weaponOnly;
    private final BoolSetting randomizeCps;
    private final IntSetting minCps;
    private final IntSetting maxCps;
    private final Random random = new Random();
    private long nextClickAtMs;

    public AutoClickerModule()
    {
        super("auto_clicker", "AutoClicker", Category.COMBAT);
        SettingGroup general = this.addGroup("general", "General");
        this.requireAttackKey = this.addSetting(new BoolSetting("require_attack_key", "Require Attack Key", "Only click while physical attack key is held", true));
        this.weaponOnly = this.addSetting(new BoolSetting("weapon_only", "Weapon Only", "Only click when holding sword/tool", false));
        this.randomizeCps = this.addSetting(new BoolSetting("randomize_cps", "Randomize CPS", "Randomly vary CPS between min and max", true));
        this.minCps = this.addSetting(new IntSetting("min_cps", "Min CPS", "Minimum clicks per second", 9, 1, 25, 1));
        this.maxCps = this.addSetting(new IntSetting("max_cps", "Max CPS", "Maximum clicks per second", 13, 1, 25, 1));
        general.add(this.requireAttackKey);
        general.add(this.weaponOnly);
        general.add(this.randomizeCps);
        general.add(this.minCps);
        general.add(this.maxCps);
    }

    public void onEnable()
    {
        this.nextClickAtMs = 0L;
    }

    public void onDisable()
    {
        this.nextClickAtMs = 0L;
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || mc.gameSettings == null)
        {
            return;
        }

        int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
        boolean attackHeld = this.isKeyOrMouseDown(attackKey);

        if (this.requireAttackKey.isEnabled() && !attackHeld)
        {
            this.nextClickAtMs = 0L;
            return;
        }

        if (this.weaponOnly.isEnabled() && !this.isHoldingWeapon(mc.thePlayer))
        {
            return;
        }

        long now = System.currentTimeMillis();

        if (now < this.nextClickAtMs)
        {
            return;
        }

        KeyBinding.onTick(attackKey);
        this.nextClickAtMs = now + ModuleKit.cpsDelayMs(this.random, this.minCps.get().intValue(), this.maxCps.get().intValue(), this.randomizeCps.isEnabled());
    }

    private boolean isHoldingWeapon(EntityPlayerSP player)
    {
        ItemStack held = player == null ? null : player.getHeldItem();

        if (held == null || held.getItem() == null)
        {
            return false;
        }

        return held.getItem() instanceof ItemSword || held.getItem() instanceof ItemTool;
    }

    private boolean isKeyOrMouseDown(int keyCode)
    {
        if (keyCode >= 0)
        {
            return keyCode < 256 && Keyboard.isKeyDown(keyCode);
        }

        int mouseButton = keyCode + 100;
        return mouseButton >= 0 && mouseButton < 16 && Mouse.isButtonDown(mouseButton);
    }
}

