package dwgx.foundation.math;

import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * Rotation calculation utilities for combat and world modules.
 * Bounding-box closest-point targeting ported from Southside 1.20.4,
 * adapted for MCP 9.18 / 1.8.9 API.
 */
public final class RotationMath
{
    private RotationMath()
    {
    }

    // ── Bounding-box targeting ──

    /**
     * Closest point on an AABB to a given point (clamped to box surface).
     */
    public static Vec3 closestPointOnBox(Vec3 point, AxisAlignedBB box)
    {
        double cx = MathHelper.clamp_double(point.xCoord, box.minX, box.maxX);
        double cy = MathHelper.clamp_double(point.yCoord, box.minY, box.maxY);
        double cz = MathHelper.clamp_double(point.zCoord, box.minZ, box.maxZ);
        return new Vec3(cx, cy, cz);
    }

    /**
     * Yaw/pitch from eye to closest point on target bounding box.
     */
    public static float[] toTargetBox(Entity from, Entity to)
    {
        if (from == null || to == null)
        {
            return new float[] {0.0F, 0.0F};
        }

        Vec3 eye = eyeVec(from);
        Vec3 closest = closestPointOnBox(eye, to.getEntityBoundingBox());
        return toVec3(eye, closest);
    }

    // ── Vec3 rotation ──

    public static float[] toVec3(Vec3 from, Vec3 to)
    {
        double dx = to.xCoord - from.xCoord;
        double dy = to.yCoord - from.yCoord;
        double dz = to.zCoord - from.zCoord;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
        return new float[] {wrapDegrees(yaw), MathHelper.clamp_float(pitch, -90.0F, 90.0F)};
    }

    // ── Raycast validation ──

    public static Vec3 lookVector(float yaw, float pitch)
    {
        float yr = (float) Math.toRadians(yaw);
        float pr = (float) Math.toRadians(pitch);
        float cosPitch = MathHelper.cos(-pr);
        return new Vec3(
                (double)(MathHelper.sin(-yr) * cosPitch),
                (double) MathHelper.sin(-pr),
                (double)(MathHelper.cos(-yr) * cosPitch));
    }

    /**
     * Check if a rotation from entity eye hits target bounding box.
     * Returns hit point or null.
     */
    public static Vec3 raycastEntity(Entity from, Entity target, float yaw, float pitch, double range)
    {
        if (from == null || target == null)
        {
            return null;
        }

        Vec3 eye = eyeVec(from);
        Vec3 dir = lookVector(yaw, pitch);
        Vec3 end = eye.addVector(dir.xCoord * range, dir.yCoord * range, dir.zCoord * range);
        AxisAlignedBB box = target.getEntityBoundingBox().expand(0.1D, 0.1D, 0.1D);
        MovingObjectPosition hit = box.calculateIntercept(eye, end);
        return hit != null ? hit.hitVec : null;
    }

    // ── Block targeting ──

    public static float[] toBlockCenter(Entity from, double bx, double by, double bz)
    {
        if (from == null)
        {
            return new float[] {0.0F, 0.0F};
        }

        return toVec3(eyeVec(from), new Vec3(bx + 0.5D, by + 0.5D, bz + 0.5D));
    }

    public static float[] toBlockFace(Entity from, double bx, double by, double bz,
                                       int faceOffsetX, int faceOffsetY, int faceOffsetZ)
    {
        if (from == null)
        {
            return new float[] {0.0F, 0.0F};
        }

        double tx = bx + 0.5D + (double) faceOffsetX * 0.5D;
        double ty = by + 0.5D + (double) faceOffsetY * 0.5D;
        double tz = bz + 0.5D + (double) faceOffsetZ * 0.5D;
        return toVec3(eyeVec(from), new Vec3(tx, ty, tz));
    }

    // ── Legacy convenience ──

    public static float[] toTarget(Entity from, Entity to)
    {
        return toTargetBox(from, to);
    }

    public static float[] toPosition(Entity from, double x, double y, double z)
    {
        if (from == null)
        {
            return new float[] {0.0F, 0.0F};
        }

        return toVec3(eyeVec(from), new Vec3(x, y, z));
    }

    // ── Smooth rotation ──

    public static float smoothYaw(float current, float target, float speed)
    {
        float diff = wrapDegrees(target - current);
        float clamped = MathHelper.clamp_float(diff, -speed, speed);
        return wrapDegrees(current + clamped);
    }

    public static float smoothPitch(float current, float target, float speed)
    {
        float diff = target - current;
        float clamped = MathHelper.clamp_float(diff, -speed, speed);
        return MathHelper.clamp_float(current + clamped, -90.0F, 90.0F);
    }

    // ── Utilities ──

    public static float wrapDegrees(float angle)
    {
        float wrapped = angle % 360.0F;

        if (wrapped >= 180.0F)
        {
            wrapped -= 360.0F;
        }

        if (wrapped < -180.0F)
        {
            wrapped += 360.0F;
        }

        return wrapped;
    }

    public static double distanceTo(Entity from, Entity to)
    {
        if (from == null || to == null)
        {
            return Double.MAX_VALUE;
        }

        return from.getDistanceToEntity(to);
    }

    /**
     * Distance from entity eye to closest point on target bounding box.
     */
    public static double distanceToBox(Entity from, Entity to)
    {
        if (from == null || to == null)
        {
            return Double.MAX_VALUE;
        }

        Vec3 eye = eyeVec(from);
        Vec3 closest = closestPointOnBox(eye, to.getEntityBoundingBox());
        return eye.distanceTo(closest);
    }

    public static float angleDifference(Entity from, Entity to)
    {
        if (from == null || to == null)
        {
            return Float.MAX_VALUE;
        }

        float[] target = toTargetBox(from, to);
        float yawDiff = Math.abs(wrapDegrees(target[0] - from.rotationYaw));
        float pitchDiff = Math.abs(target[1] - from.rotationPitch);
        return yawDiff + pitchDiff;
    }

    private static Vec3 eyeVec(Entity entity)
    {
        return new Vec3(entity.posX, entity.posY + (double) entity.getEyeHeight(), entity.posZ);
    }
}