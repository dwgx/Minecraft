package dwgx.modulekit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Combat-related utilities shared across combat modules.
 */
public final class CombatKit
{
    public enum TargetMode
    {
        NEAREST,
        LOWEST_HEALTH,
        HIGHEST_HEALTH,
        HURT_TIME
    }

    private CombatKit()
    {
    }

    /**
     * Get all valid living targets within range.
     */
    public static List<EntityLivingBase> getTargetsInRange(WorldClient world, EntityPlayerSP player,
                                                           float range, boolean playersOnly, boolean throughWalls)
    {
        if (world == null || player == null)
        {
            return Collections.emptyList();
        }

        List<EntityLivingBase> targets = new ArrayList<EntityLivingBase>();
        double rangeSq = (double) range * (double) range;

        for (int i = 0; i < world.loadedEntityList.size(); ++i)
        {
            Entity entity = (Entity) world.loadedEntityList.get(i);

            if (!(entity instanceof EntityLivingBase))
            {
                continue;
            }

            EntityLivingBase living = (EntityLivingBase) entity;

            if (!isValidTarget(living, player, range, playersOnly, throughWalls))
            {
                continue;
            }

            targets.add(living);
        }

        return targets;
    }

    /**
     * Check if an entity is a valid attack target.
     */
    public static boolean isValidTarget(EntityLivingBase entity, EntityPlayerSP player,
                                        float range, boolean playersOnly, boolean throughWalls)
    {
        if (entity == null || player == null || entity == player)
        {
            return false;
        }

        if (entity.isDead || entity.getHealth() <= 0.0F)
        {
            return false;
        }

        if (player.getDistanceToEntity(entity) > range)
        {
            return false;
        }

        if (playersOnly && !(entity instanceof EntityPlayer))
        {
            return false;
        }

        if (entity instanceof EntityPlayer)
        {
            EntityPlayer targetPlayer = (EntityPlayer) entity;

            if (targetPlayer.isSpectator() || targetPlayer.capabilities.isCreativeMode)
            {
                return false;
            }
        }

        if (!throughWalls && !player.canEntityBeSeen(entity))
        {
            return false;
        }

        return true;
    }

    /**
     * Select the best target from a list based on the given mode.
     */
    public static EntityLivingBase selectTarget(List<EntityLivingBase> targets,
                                                final EntityPlayerSP player, TargetMode mode)
    {
        if (targets == null || targets.isEmpty() || player == null)
        {
            return null;
        }

        switch (mode)
        {
            case LOWEST_HEALTH:
                return selectByComparator(targets, new Comparator<EntityLivingBase>()
                {
                    public int compare(EntityLivingBase a, EntityLivingBase b)
                    {
                        return Float.compare(a.getHealth(), b.getHealth());
                    }
                });

            case HIGHEST_HEALTH:
                return selectByComparator(targets, new Comparator<EntityLivingBase>()
                {
                    public int compare(EntityLivingBase a, EntityLivingBase b)
                    {
                        return Float.compare(b.getHealth(), a.getHealth());
                    }
                });

            case HURT_TIME:
                return selectByComparator(targets, new Comparator<EntityLivingBase>()
                {
                    public int compare(EntityLivingBase a, EntityLivingBase b)
                    {
                        return Integer.compare(a.hurtTime, b.hurtTime);
                    }
                });

            case NEAREST:
            default:
                return selectByComparator(targets, new Comparator<EntityLivingBase>()
                {
                    public int compare(EntityLivingBase a, EntityLivingBase b)
                    {
                        return Float.compare(player.getDistanceToEntity(a), player.getDistanceToEntity(b));
                    }
                });
        }
    }

    private static EntityLivingBase selectByComparator(List<EntityLivingBase> targets,
                                                       Comparator<EntityLivingBase> comparator)
    {
        EntityLivingBase best = null;

        for (int i = 0; i < targets.size(); ++i)
        {
            EntityLivingBase candidate = targets.get(i);

            if (best == null || comparator.compare(candidate, best) < 0)
            {
                best = candidate;
            }
        }

        return best;
    }
}
