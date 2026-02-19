package client.module.impl.world;

import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.IntSetting;
import client.setting.SettingGroup;
import dwgx.modulekit.ModuleKit;
import java.util.Locale;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.init.Blocks;

/**
 * Chest stealer with delay cadence and useful-loot heuristics.
 */
public final class StealerModule extends Module
{
    private final IntSetting minDelayMs;
    private final IntSetting maxDelayMs;
    private final IntSetting closeAfterEmptyChecks;
    private final BoolSetting usefulOnly;
    private final BoolSetting autoClose;
    private final BoolSetting chestTitleGuard;
    private final Random random = new Random();
    private long nextStealAtMs;
    private int emptyChecks;
    private int lastWindowId = -1;

    public StealerModule()
    {
        super("stealer", "Stealer", Category.WORLD);
        SettingGroup group = this.addGroup("general", "General");
        this.minDelayMs = this.addSetting(new IntSetting("min_delay_ms", "Min Delay", "Minimum delay between steals (ms)", 80, 0, 500, 5));
        this.maxDelayMs = this.addSetting(new IntSetting("max_delay_ms", "Max Delay", "Maximum delay between steals (ms)", 145, 0, 700, 5));
        this.closeAfterEmptyChecks = this.addSetting(new IntSetting("close_after_empty_checks", "Close Delay Checks", "How many empty scans before closing chest", 2, 1, 8, 1));
        this.usefulOnly = this.addSetting(new BoolSetting("useful_only", "Useful Only", "Skip low-value stacks", true));
        this.autoClose = this.addSetting(new BoolSetting("auto_close", "Auto Close", "Close chest when nothing useful remains", true));
        this.chestTitleGuard = this.addSetting(new BoolSetting("chest_title_guard", "Chest Title Guard", "Only operate in normal chest windows", false));
        group.add(this.minDelayMs);
        group.add(this.maxDelayMs);
        group.add(this.closeAfterEmptyChecks);
        group.add(this.usefulOnly);
        group.add(this.autoClose);
        group.add(this.chestTitleGuard);
    }

    public void onEnable()
    {
        this.resetSession();
    }

    public void onDisable()
    {
        this.resetSession();
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.playerController == null)
        {
            this.resetSession();
            return;
        }

        if (!(mc.currentScreen instanceof GuiChest) || !(mc.thePlayer.openContainer instanceof ContainerChest))
        {
            this.resetSession();
            return;
        }

        ContainerChest container = (ContainerChest)mc.thePlayer.openContainer;

        if (this.chestTitleGuard.isEnabled() && !this.isVanillaChest(container))
        {
            this.resetSession();
            return;
        }

        if (container.windowId != this.lastWindowId)
        {
            this.lastWindowId = container.windowId;
            this.nextStealAtMs = 0L;
            this.emptyChecks = 0;
        }

        long now = System.currentTimeMillis();

        if (now < this.nextStealAtMs)
        {
            return;
        }

        int slot = ModuleKit.findStealableSlot(container, this.usefulOnly.isEnabled());

        if (slot >= 0)
        {
            mc.playerController.windowClick(container.windowId, slot, 0, 1, mc.thePlayer);
            this.emptyChecks = 0;
            this.nextStealAtMs = now + ModuleKit.randomDelayMs(this.random, this.minDelayMs.get().intValue(), this.maxDelayMs.get().intValue());
            return;
        }

        ++this.emptyChecks;
        this.nextStealAtMs = now + ModuleKit.randomDelayMs(this.random, this.minDelayMs.get().intValue(), this.maxDelayMs.get().intValue());

        if (this.autoClose.isEnabled() && this.emptyChecks >= Math.max(1, this.closeAfterEmptyChecks.get().intValue()))
        {
            mc.thePlayer.closeScreen();
            this.resetSession();
        }
    }

    private boolean isVanillaChest(ContainerChest container)
    {
        if (container == null || container.getLowerChestInventory() == null)
        {
            return false;
        }

        String name = container.getLowerChestInventory().getName();

        if (name == null)
        {
            return false;
        }

        String lower = name.toLowerCase(Locale.ROOT);
        String vanilla = Blocks.chest.getLocalizedName().toLowerCase(Locale.ROOT);
        return lower.contains("chest") || lower.contains(vanilla);
    }

    private void resetSession()
    {
        this.nextStealAtMs = 0L;
        this.emptyChecks = 0;
        this.lastWindowId = -1;
    }
}

