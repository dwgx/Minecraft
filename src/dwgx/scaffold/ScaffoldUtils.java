package dwgx.scaffold;

import client.rotation.RotationUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public final class ScaffoldUtils
{
    private static final float DEFAULT_PREDICTION_GAIN = 0.65F;
    private static final float DEFAULT_STABILITY_BIAS = 0.75F;
    private static final int DEFAULT_EXTENSION_DISTANCE = 1;

    private ScaffoldUtils()
    {
    }

    public static PlacementTarget findPlacementTarget(Minecraft mc, EntityPlayerSP player, PlaceMode mode, int targetY, double forwardOffset, int searchRadius, float reachDistance)
    {
        return findPlacementTarget(mc, player, mode, targetY, forwardOffset, searchRadius, reachDistance, true, DEFAULT_PREDICTION_GAIN, DEFAULT_STABILITY_BIAS, DEFAULT_EXTENSION_DISTANCE, null);
    }

    public static PlacementTarget findPlacementTarget(Minecraft mc, EntityPlayerSP player, PlaceMode mode, int targetY, double forwardOffset, int searchRadius, float reachDistance, boolean useMotionPrediction, float predictionGain, float stabilityBias, BlockPos lastPlacedPos)
    {
        return findPlacementTarget(mc, player, mode, targetY, forwardOffset, searchRadius, reachDistance, useMotionPrediction, predictionGain, stabilityBias, DEFAULT_EXTENSION_DISTANCE, lastPlacedPos);
    }

    public static PlacementTarget findPlacementTarget(Minecraft mc, EntityPlayerSP player, PlaceMode mode, int targetY, double forwardOffset, int searchRadius, float reachDistance, boolean useMotionPrediction, float predictionGain, float stabilityBias, int extensionDistance, BlockPos lastPlacedPos)
    {
        return findPlacementTarget(mc, player, mode, targetY, forwardOffset, searchRadius, reachDistance, useMotionPrediction, predictionGain, stabilityBias, extensionDistance, false, lastPlacedPos);
    }

    public static PlacementTarget findPlacementTarget(Minecraft mc, EntityPlayerSP player, PlaceMode mode, int targetY, double forwardOffset, int searchRadius, float reachDistance, boolean useMotionPrediction, float predictionGain, float stabilityBias, int extensionDistance, boolean omniDirectionalExpand, BlockPos lastPlacedPos)
    {
        if (mc == null || mc.theWorld == null || player == null)
        {
            return null;
        }

        PlaceMode resolvedMode = mode == null ? PlaceMode.SMART : mode;
        Vec3 viewDir = resolveViewDirection(player);
        double speed = horizontalSpeed(player);
        double adaptiveLead = Math.max(0.35D, forwardOffset);

        if (useMotionPrediction)
        {
            double gain = Math.max(0.0D, predictionGain);
            adaptiveLead += Math.min(1.20D, speed * (0.70D + gain * 2.0D));
        }

        int extension = Math.max(0, extensionDistance);
        List<BlockPos> seeds = buildSeedTargets(player, targetY, resolvedMode, adaptiveLead, extension, omniDirectionalExpand);
        Set<BlockPos> tested = new HashSet<BlockPos>();
        int radius = Math.max(0, searchRadius);

        if (useMotionPrediction && speed > 0.22D && radius < 6)
        {
            ++radius;
        }

        Vec3 desired = new Vec3(player.posX + viewDir.xCoord * adaptiveLead, (double)targetY + 0.5D, player.posZ + viewDir.zCoord * adaptiveLead);
        PlacementTarget best = null;
        double bestScore = Double.MAX_VALUE;

        for (int i = 0; i < seeds.size(); ++i)
        {
            BlockPos seed = seeds.get(i);

            for (int r = 0; r <= radius; ++r)
            {
                for (int dx = -r; dx <= r; ++dx)
                {
                    for (int dz = -r; dz <= r; ++dz)
                    {
                        if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r)
                        {
                            continue;
                        }

                        BlockPos candidate = new BlockPos(seed.getX() + dx, seed.getY(), seed.getZ() + dz);

                        if (!tested.add(candidate))
                        {
                            continue;
                        }

                        PlacementTarget target = buildPlacementTarget(mc.theWorld, player, candidate, reachDistance);

                        if (target == null)
                        {
                            continue;
                        }

                        double score = scorePlacement(player, target, desired, lastPlacedPos, resolvedMode, stabilityBias);

                        if (score < bestScore)
                        {
                            bestScore = score;
                            best = target;
                        }
                    }
                }
            }
        }

        return best;
    }

    public static int findBestBlockSlot(InventoryPlayer inventory, int preferredSlot)
    {
        if (inventory == null)
        {
            return -1;
        }

        if (preferredSlot >= 0 && preferredSlot < 9)
        {
            ItemStack preferred = inventory.mainInventory[preferredSlot];

            if (isValidScaffoldBlock(preferred))
            {
                return preferredSlot;
            }
        }

        int bestSlot = -1;
        int bestCount = -1;

        for (int i = 0; i < 9; ++i)
        {
            ItemStack stack = inventory.mainInventory[i];

            if (!isValidScaffoldBlock(stack))
            {
                continue;
            }

            if (stack.stackSize > bestCount)
            {
                bestCount = stack.stackSize;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    public static int findFirstBlockSlot(InventoryPlayer inventory, int preferredSlot)
    {
        if (inventory == null)
        {
            return -1;
        }

        if (preferredSlot >= 0 && preferredSlot < 9)
        {
            ItemStack preferred = inventory.mainInventory[preferredSlot];

            if (isValidScaffoldBlock(preferred))
            {
                return preferredSlot;
            }
        }

        for (int i = 0; i < 9; ++i)
        {
            if (isValidScaffoldBlock(inventory.mainInventory[i]))
            {
                return i;
            }
        }

        return -1;
    }

    public static boolean isValidScaffoldBlock(ItemStack stack)
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

    public static float[] calculateRotations(EntityPlayerSP player, Vec3 targetVec)
    {
        RotationUtils.Rotation rotation = RotationUtils.fromEyeTo(player, targetVec);
        return new float[] {rotation.getYaw(), rotation.getPitch()};
    }

    public static void applyRotation(EntityPlayerSP player, float targetYaw, float targetPitch, RotationMode mode, float turnSpeed)
    {
        if (player == null || mode == null || mode == RotationMode.NONE)
        {
            return;
        }

        RotationUtils.Rotation rotation = RotationUtils.snap(targetYaw, targetPitch);

        if (mode == RotationMode.SMOOTH)
        {
            RotationUtils.Rotation current = RotationUtils.current(player);
            rotation = RotationUtils.stepSmooth(current, rotation, turnSpeed);
        }

        RotationUtils.applyLegit(player, rotation.getYaw(), rotation.getPitch());
    }

    public static boolean isAimAligned(EntityPlayerSP player, float targetYaw, float targetPitch, float tolerance)
    {
        return RotationUtils.isAligned(player, targetYaw, targetPitch, tolerance);
    }

    public static Vec3 resolveViewDirection(EntityPlayerSP player)
    {
        if (player == null)
        {
            return new Vec3(0.0D, 0.0D, 0.0D);
        }

        double rad = Math.toRadians(player.rotationYaw);
        double moveX = -Math.sin(rad);
        double moveZ = Math.cos(rad);

        double len = Math.sqrt(moveX * moveX + moveZ * moveZ);

        if (len < 1.0E-4D)
        {
            return new Vec3(0.0D, 0.0D, 0.0D);
        }

        return new Vec3(moveX / len, 0.0D, moveZ / len);
    }

    private static PlacementTarget buildPlacementTarget(World world, EntityPlayerSP player, BlockPos placePos, float reachDistance)
    {
        if (!canPlaceAt(world, placePos))
        {
            return null;
        }

        Vec3 eye = getEyePosition(player);
        double maxReachSq = (double)reachDistance * (double)reachDistance + 0.25D;
        EnumFacing[] searchOrder = getSupportSearchOrder(player);

        for (int i = 0; i < searchOrder.length; ++i)
        {
            EnumFacing directionToSupport = searchOrder[i];
            BlockPos supportPos = placePos.offset(directionToSupport);

            if (!isSupportBlock(world, supportPos))
            {
                continue;
            }

            EnumFacing hitSide = directionToSupport.getOpposite();
            Vec3 hitVec = getFaceCenter(supportPos, hitSide);

            if (eye.squareDistanceTo(hitVec) > maxReachSq)
            {
                continue;
            }

            float[] rotations = calculateRotations(player, hitVec);
            return new PlacementTarget(placePos, supportPos, hitSide, hitVec, rotations[0], rotations[1]);
        }

        return null;
    }

    private static List<BlockPos> buildSeedTargets(EntityPlayerSP player, int y, PlaceMode mode, double forwardOffset, int extensionDistance, boolean omniDirectionalExpand)
    {
        List<BlockPos> result = new ArrayList<BlockPos>(26);
        int baseX = MathHelper.floor_double(player.posX);
        int baseZ = MathHelper.floor_double(player.posZ);
        BlockPos under = new BlockPos(baseX, y, baseZ);
        Vec3 direction = resolveViewDirection(player);
        int stepX = direction.xCoord > 0.15D ? 1 : (direction.xCoord < -0.15D ? -1 : 0);
        int stepZ = direction.zCoord > 0.15D ? 1 : (direction.zCoord < -0.15D ? -1 : 0);
        int sideX = direction.zCoord > 0.15D ? 1 : (direction.zCoord < -0.15D ? -1 : 0);
        int sideZ = direction.xCoord > 0.15D ? -1 : (direction.xCoord < -0.15D ? 1 : 0);
        int forwardSteps = Math.max(1, extensionDistance + 1);

        if (mode == PlaceMode.UNDER)
        {
            addUnique(result, under);
        }

        for (int step = 1; step <= forwardSteps; ++step)
        {
            double lead = Math.max(0.35D, forwardOffset + (double)(step - 1) * 0.95D);
            BlockPos axisForward = new BlockPos(baseX + stepX * step, y, baseZ + stepZ * step);
            BlockPos projectedForward = new BlockPos(MathHelper.floor_double(player.posX + direction.xCoord * lead), y, MathHelper.floor_double(player.posZ + direction.zCoord * lead));

            if (mode == PlaceMode.FORWARD)
            {
                addUnique(result, axisForward);
                addUnique(result, projectedForward);
            }
            else if (mode == PlaceMode.EXPAND)
            {
                addUnique(result, projectedForward);
                addUnique(result, axisForward);

                if (omniDirectionalExpand && (sideX != 0 || sideZ != 0))
                {
                    addUnique(result, new BlockPos(projectedForward.getX() + sideX, y, projectedForward.getZ() + sideZ));
                    addUnique(result, new BlockPos(projectedForward.getX() - sideX, y, projectedForward.getZ() - sideZ));
                    addUnique(result, new BlockPos(axisForward.getX() + sideX, y, axisForward.getZ() + sideZ));
                    addUnique(result, new BlockPos(axisForward.getX() - sideX, y, axisForward.getZ() - sideZ));
                }
            }
            else if (mode == PlaceMode.UNDER)
            {
                addUnique(result, axisForward);
                addUnique(result, projectedForward);
            }
            else
            {
                if (step == 1)
                {
                    addUnique(result, axisForward);
                    addUnique(result, projectedForward);
                }
                else
                {
                    addUnique(result, projectedForward);
                    addUnique(result, axisForward);
                }
            }
        }

        if (mode != PlaceMode.UNDER)
        {
            addUnique(result, under);
        }

        if (sideX != 0 || sideZ != 0)
        {
            BlockPos leadPos = result.size() > 0 ? result.get(0) : under;
            addUnique(result, new BlockPos(leadPos.getX() + sideX, y, leadPos.getZ() + sideZ));
            addUnique(result, new BlockPos(leadPos.getX() - sideX, y, leadPos.getZ() - sideZ));
        }

        return result;
    }

    private static void addUnique(List<BlockPos> list, BlockPos pos)
    {
        for (int i = 0; i < list.size(); ++i)
        {
            if (list.get(i).equals(pos))
            {
                return;
            }
        }

        list.add(pos);
    }

    private static EnumFacing[] getSupportSearchOrder(EntityPlayerSP player)
    {
        EnumFacing forward = EnumFacing.fromAngle((double)player.rotationYaw);
        EnumFacing back = forward.getOpposite();
        EnumFacing left = forward.rotateYCCW();
        EnumFacing right = forward.rotateY();
        return new EnumFacing[] {EnumFacing.DOWN, back, left, right, forward, EnumFacing.UP};
    }

    private static boolean canPlaceAt(World world, BlockPos pos)
    {
        if (world == null || pos == null)
        {
            return false;
        }

        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        return world.isAirBlock(pos) || block.isReplaceable(world, pos) || block.getMaterial().isReplaceable();
    }

    private static boolean isSupportBlock(World world, BlockPos pos)
    {
        if (world == null || pos == null || world.isAirBlock(pos))
        {
            return false;
        }

        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (block.getMaterial().isReplaceable())
        {
            return false;
        }

        return block.canCollideCheck(state, false);
    }

    private static Vec3 getEyePosition(EntityPlayerSP player)
    {
        return new Vec3(player.posX, player.posY + (double)player.getEyeHeight(), player.posZ);
    }

    private static double horizontalSpeed(EntityPlayerSP player)
    {
        if (player == null)
        {
            return 0.0D;
        }

        return Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
    }

    private static double scorePlacement(EntityPlayerSP player, PlacementTarget target, Vec3 desired, BlockPos lastPlacedPos, PlaceMode mode, float stabilityBias)
    {
        Vec3 eye = getEyePosition(player);
        Vec3 center = new Vec3((double)target.placePos.getX() + 0.5D, (double)target.placePos.getY() + 0.5D, (double)target.placePos.getZ() + 0.5D);
        double dx = center.xCoord - desired.xCoord;
        double dz = center.zCoord - desired.zCoord;
        double forwardDistSq = dx * dx + dz * dz;
        double reachDist = eye.distanceTo(target.hitVec);
        float yawDelta = Math.abs(RotationUtils.yawDelta(player.rotationYaw, target.yaw));
        float pitchDelta = Math.abs(RotationUtils.pitchDelta(player.rotationPitch, target.pitch));
        double rotationPenalty = (double)yawDelta * 0.0105D + (double)pitchDelta * 0.0140D;
        double score = forwardDistSq * 1.30D + reachDist * 0.17D + rotationPenalty;
        Vec3 viewDir = resolveViewDirection(player);
        double forwardDot = (center.xCoord - player.posX) * viewDir.xCoord + (center.zCoord - player.posZ) * viewDir.zCoord;

        if (mode == PlaceMode.UNDER)
        {
            int playerX = MathHelper.floor_double(player.posX);
            int playerZ = MathHelper.floor_double(player.posZ);
            int axisDist = Math.abs(target.placePos.getX() - playerX) + Math.abs(target.placePos.getZ() - playerZ);
            score += axisDist * 0.20D;
        }
        else if (mode == PlaceMode.FORWARD)
        {
            int playerX = MathHelper.floor_double(player.posX);
            int playerZ = MathHelper.floor_double(player.posZ);
            int axisDist = Math.abs(target.placePos.getX() - playerX) + Math.abs(target.placePos.getZ() - playerZ);
            score += axisDist == 0 ? 0.30D : 0.0D;

            if (forwardDot < 0.05D)
            {
                score += 0.85D;
            }
        }
        else if (mode == PlaceMode.EXPAND)
        {
            int playerX = MathHelper.floor_double(player.posX);
            int playerZ = MathHelper.floor_double(player.posZ);
            int axisDist = Math.abs(target.placePos.getX() - playerX) + Math.abs(target.placePos.getZ() - playerZ);
            score += axisDist == 0 ? 0.40D : Math.min(0.90D, (double)axisDist * 0.06D);

            if (forwardDot < 0.02D)
            {
                score += 1.05D;
            }
        }
        else if (forwardDot < -0.15D)
        {
            score += 0.45D;
        }

        if (target.side == EnumFacing.DOWN)
        {
            score += 0.28D;
        }
        else if (target.side == EnumFacing.UP)
        {
            score -= 0.10D;
        }

        float stable = Math.max(0.0F, stabilityBias);

        if (lastPlacedPos != null && stable > 0.001F)
        {
            if (target.placePos.equals(lastPlacedPos))
            {
                score -= 0.25D * (double)stable;
            }
            else
            {
                int manhattan = Math.abs(target.placePos.getX() - lastPlacedPos.getX()) + Math.abs(target.placePos.getY() - lastPlacedPos.getY()) + Math.abs(target.placePos.getZ() - lastPlacedPos.getZ());
                score += Math.min(1.8D, (double)manhattan * 0.22D) * (double)stable;
            }
        }

        return score;
    }

    private static Vec3 getFaceCenter(BlockPos blockPos, EnumFacing side)
    {
        return new Vec3((double)blockPos.getX() + 0.5D + (double)side.getFrontOffsetX() * 0.5D, (double)blockPos.getY() + 0.5D + (double)side.getFrontOffsetY() * 0.5D, (double)blockPos.getZ() + 0.5D + (double)side.getFrontOffsetZ() * 0.5D);
    }

    public static RotationSnapshot captureRotation(EntityPlayerSP player)
    {
        RotationUtils.RotationSnapshot snapshot = RotationUtils.capture(player);

        if (snapshot == null)
        {
            return null;
        }

        return new RotationSnapshot(snapshot.getYaw(), snapshot.getPitch(), snapshot.getYawHead(), snapshot.getRenderYawOffset());
    }

    public static void restoreRotation(EntityPlayerSP player, RotationSnapshot snapshot)
    {
        if (player == null || snapshot == null)
        {
            return;
        }

        RotationUtils.restore(player, new RotationUtils.RotationSnapshot(snapshot.yaw, snapshot.pitch, snapshot.yawHead, snapshot.renderYawOffset));
    }

    public static final class PlacementTarget
    {
        private final BlockPos placePos;
        private final BlockPos supportPos;
        private final EnumFacing side;
        private final Vec3 hitVec;
        private final float yaw;
        private final float pitch;

        public PlacementTarget(BlockPos placePos, BlockPos supportPos, EnumFacing side, Vec3 hitVec, float yaw, float pitch)
        {
            this.placePos = placePos;
            this.supportPos = supportPos;
            this.side = side;
            this.hitVec = hitVec;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public BlockPos getPlacePos()
        {
            return this.placePos;
        }

        public BlockPos getSupportPos()
        {
            return this.supportPos;
        }

        public EnumFacing getSide()
        {
            return this.side;
        }

        public Vec3 getHitVec()
        {
            return this.hitVec;
        }

        public float getYaw()
        {
            return this.yaw;
        }

        public float getPitch()
        {
            return this.pitch;
        }
    }

    public static final class RotationSnapshot
    {
        private final float yaw;
        private final float pitch;
        private final float yawHead;
        private final float renderYawOffset;

        public RotationSnapshot(float yaw, float pitch, float yawHead, float renderYawOffset)
        {
            this.yaw = yaw;
            this.pitch = pitch;
            this.yawHead = yawHead;
            this.renderYawOffset = renderYawOffset;
        }
    }

    public static enum AimMode
    {
        LEGIT,
        SILENCE
    }

    public static enum RotationMode
    {
        NONE,
        SNAP,
        SMOOTH
    }

    public static enum PlaceMode
    {
        SMART,
        UNDER,
        FORWARD,
        EXPAND
    }
}
