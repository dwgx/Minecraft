package dwgx.modulekit;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

/**
 * Shared raycast helpers for entity and block facing checks.
 */
public final class SightProbe
{
    private SightProbe()
    {
    }

    public static boolean hitsEntity(Minecraft mc, Entity target, float partialTicks)
    {
        if (mc == null || target == null)
        {
            return false;
        }

        if (mc.entityRenderer != null)
        {
            mc.entityRenderer.getMouseOver(partialTicks <= 0.0F ? 1.0F : partialTicks);
        }

        MovingObjectPosition hit = mc.objectMouseOver;
        return hit != null
            && hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
            && hit.entityHit == target;
    }

    public static boolean matchesPlacement(Minecraft mc, BlockPos blockPos, EnumFacing side, boolean strictSide)
    {
        if (mc == null || blockPos == null || side == null)
        {
            return false;
        }

        if (mc.entityRenderer != null)
        {
            mc.entityRenderer.getMouseOver(1.0F);
        }

        MovingObjectPosition hit = mc.objectMouseOver;

        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || hit.getBlockPos() == null)
        {
            return false;
        }

        if (!blockPos.equals(hit.getBlockPos()))
        {
            return false;
        }

        return !strictSide || hit.sideHit == side;
    }
}

