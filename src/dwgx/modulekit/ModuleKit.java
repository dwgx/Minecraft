package dwgx.modulekit;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

/**
 * Unified facade for module-shared helpers under dwgx package.
 */
public final class ModuleKit
{
    private ModuleKit()
    {
    }

    public static int randomDelayMs(Random random, int minMs, int maxMs)
    {
        return PulseMath.randomDelayMs(random, minMs, maxMs);
    }

    public static long cpsDelayMs(Random random, int minCps, int maxCps, boolean randomize)
    {
        return PulseMath.cpsDelayMs(random, minCps, maxCps, randomize);
    }

    public static int findStealableSlot(ContainerChest container, boolean usefulOnly)
    {
        return InventoryProbe.findStealableSlot(container, usefulOnly);
    }

    public static boolean isUsefulChestLoot(ItemStack stack)
    {
        return InventoryProbe.isUsefulChestLoot(stack);
    }

    public static int findBestToolHotbarSlot(EntityPlayerSP player, BlockPos blockPos)
    {
        return InventoryProbe.findBestToolHotbarSlot(player, blockPos);
    }

    public static boolean raycastHitsEntity(Minecraft mc, Entity target, float partialTicks)
    {
        return SightProbe.hitsEntity(mc, target, partialTicks);
    }

    public static boolean raycastMatchesPlacement(Minecraft mc, BlockPos blockPos, EnumFacing side, boolean strictSide)
    {
        return SightProbe.matchesPlacement(mc, blockPos, side, strictSide);
    }
}

