package dwgx.modulekit;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

/**
 * Shared inventory/chest heuristics extracted from player patterns.
 */
public final class InventoryProbe
{
    private InventoryProbe()
    {
    }

    public static int findStealableSlot(ContainerChest container, boolean usefulOnly)
    {
        if (container == null || container.getLowerChestInventory() == null)
        {
            return -1;
        }

        int size = container.getLowerChestInventory().getSizeInventory();
        int bestSlot = -1;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < size; ++i)
        {
            ItemStack stack = container.getLowerChestInventory().getStackInSlot(i);

            if (stack == null || stack.stackSize <= 0)
            {
                continue;
            }

            int score = scoreLoot(stack);

            if (usefulOnly && score <= 0)
            {
                continue;
            }

            if (score > bestScore)
            {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    public static boolean isUsefulChestLoot(ItemStack stack)
    {
        return scoreLoot(stack) > 0;
    }

    public static int findBestToolHotbarSlot(EntityPlayerSP player, BlockPos blockPos)
    {
        if (player == null || blockPos == null || player.worldObj == null || player.inventory == null)
        {
            return -1;
        }

        Block block = player.worldObj.getBlockState(blockPos).getBlock();

        if (block == null)
        {
            return -1;
        }

        int bestSlot = -1;
        float bestSpeed = 1.0F;

        for (int i = 0; i < 9; ++i)
        {
            ItemStack stack = player.inventory.mainInventory[i];

            if (stack == null || stack.getItem() == null)
            {
                continue;
            }

            float speed = stack.getStrVsBlock(block);

            if (speed > bestSpeed + 1.0E-4F)
            {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    private static int scoreLoot(ItemStack stack)
    {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0)
        {
            return 0;
        }

        Item item = stack.getItem();

        if (item instanceof ItemSword)
        {
            ItemSword sword = (ItemSword)item;
            return 140 + Math.round(sword.getDamageVsEntity() * 11.0F);
        }

        if (item instanceof ItemTool)
        {
            ItemTool tool = (ItemTool)item;
            return 118 + Math.round(tool.getToolMaterial().getEfficiencyOnProperMaterial() * 8.5F);
        }

        if (item instanceof ItemArmor)
        {
            ItemArmor armor = (ItemArmor)item;
            return 110 + armor.damageReduceAmount * 15;
        }

        if (item instanceof ItemBow)
        {
            return 96;
        }

        if (item instanceof ItemFood)
        {
            ItemFood food = (ItemFood)item;
            return 72 + food.getHealAmount(stack) * 6;
        }

        if (item instanceof ItemPotion)
        {
            return 66;
        }

        if (item instanceof ItemBlock)
        {
            if (!isValidBuildingBlock(stack))
            {
                return 6;
            }

            return 84 + MathHelper.clamp_int(stack.stackSize, 1, 64);
        }

        if (stack.isItemEnchanted())
        {
            return 40;
        }

        return 4;
    }

    private static boolean isValidBuildingBlock(ItemStack stack)
    {
        if (stack == null || stack.stackSize <= 0)
        {
            return false;
        }

        Item item = stack.getItem();

        if (!(item instanceof ItemBlock))
        {
            return false;
        }

        Block block = ((ItemBlock)item).getBlock();

        if (block == null || block instanceof BlockAir || block instanceof BlockLiquid || block instanceof BlockContainer || block instanceof BlockFalling)
        {
            return false;
        }

        return block.isFullCube() && block.isFullBlock();
    }
}
