package client.rotation;

import dwgx.foundation.rotation.RotationDomain;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public final class RotationUtils
{
    private RotationUtils()
    {
    }

    public static Rotation fromEyeTo(EntityPlayerSP player, Vec3 targetVec)
    {
        if (player == null || targetVec == null)
        {
            return new Rotation(0.0F, 0.0F);
        }

        Vec3 eye = new Vec3(player.posX, player.posY + (double)player.getEyeHeight(), player.posZ);
        double dx = targetVec.xCoord - eye.xCoord;
        double dy = targetVec.yCoord - eye.yCoord;
        double dz = targetVec.zCoord - eye.zCoord;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontal)));
        return snap(yaw, pitch);
    }

    public static Rotation fromEntity(EntityPlayerSP player, Entity target)
    {
        if (player == null || target == null)
        {
            return new Rotation(0.0F, 0.0F);
        }

        double dx = target.posX - player.posX;
        double dz = target.posZ - player.posZ;
        double eyeY = player.posY + (double)player.getEyeHeight();
        AxisAlignedBB bb = target.getEntityBoundingBox();
        double targetBaseY = bb == null ? target.posY : bb.minY;
        double targetHeight = target.height <= 0.0F ? 1.0F : target.height;
        double targetY = targetBaseY + (double)(targetHeight * 0.85F);
        double dy = targetY - eyeY;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontal)));
        return snap(yaw, pitch);
    }

    public static Rotation current(EntityPlayerSP player)
    {
        if (player == null)
        {
            return new Rotation(0.0F, 0.0F);
        }

        return new Rotation(player.rotationYaw, player.rotationPitch);
    }

    public static Rotation stepSmooth(Rotation current, Rotation target, float maxStep)
    {
        if (current == null || target == null)
        {
            return new Rotation(0.0F, 0.0F);
        }

        return stepSmooth(current.getYaw(), current.getPitch(), target.getYaw(), target.getPitch(), maxStep);
    }

    public static Rotation stepSmooth(float currentYaw, float currentPitch, float targetYaw, float targetPitch, float maxStep)
    {
        float step = Math.max(1.0F, maxStep);
        float yaw = currentYaw + MathHelper.clamp_float(yawDelta(currentYaw, targetYaw), -step, step);
        float pitch = currentPitch + MathHelper.clamp_float(pitchDelta(currentPitch, targetPitch), -step, step);
        return snap(yaw, pitch);
    }

    public static Rotation snap(float yaw, float pitch)
    {
        return new Rotation(yaw, clampPitch(pitch));
    }

    public static float yawDelta(float currentYaw, float targetYaw)
    {
        return RotationDomain.yawDelta(currentYaw, targetYaw);
    }

    public static float pitchDelta(float currentPitch, float targetPitch)
    {
        return RotationDomain.pitchDelta(currentPitch, targetPitch);
    }

    public static boolean isAligned(EntityPlayerSP player, float targetYaw, float targetPitch, float tolerance)
    {
        if (player == null)
        {
            return false;
        }

        float limit = Math.max(0.0F, tolerance);
        float yawDiff = Math.abs(yawDelta(player.rotationYaw, targetYaw));
        float pitchDiff = Math.abs(pitchDelta(player.rotationPitch, targetPitch));
        return yawDiff <= limit && pitchDiff <= limit;
    }

    public static void applyLegit(EntityPlayerSP player, float yaw, float pitch)
    {
        if (player == null)
        {
            return;
        }

        Rotation snapped = snap(yaw, pitch);
        player.rotationYaw = snapped.getYaw();
        player.rotationPitch = snapped.getPitch();
        player.rotationYawHead = snapped.getYaw();
        player.renderYawOffset = snapped.getYaw();
    }

    public static void sendSilentLook(EntityPlayerSP player, float yaw, float pitch)
    {
        if (player == null || player.sendQueue == null)
        {
            return;
        }

        Rotation snapped = snap(yaw, pitch);
        player.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(snapped.getYaw(), snapped.getPitch(), player.onGround));
    }

    public static RotationSnapshot capture(EntityPlayerSP player)
    {
        if (player == null)
        {
            return null;
        }

        return new RotationSnapshot(player.rotationYaw, player.rotationPitch, player.rotationYawHead, player.renderYawOffset);
    }

    public static void restore(EntityPlayerSP player, RotationSnapshot snapshot)
    {
        if (player == null || snapshot == null)
        {
            return;
        }

        player.rotationYaw = snapshot.getYaw();
        player.rotationPitch = snapshot.getPitch();
        player.rotationYawHead = snapshot.getYawHead();
        player.renderYawOffset = snapshot.getRenderYawOffset();
    }

    public static float clampPitch(float pitch)
    {
        return RotationDomain.clampPitch(pitch);
    }

    public static final class Rotation
    {
        private final float yaw;
        private final float pitch;

        public Rotation(float yaw, float pitch)
        {
            this.yaw = yaw;
            this.pitch = pitch;
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

        public float getYaw()
        {
            return this.yaw;
        }

        public float getPitch()
        {
            return this.pitch;
        }

        public float getYawHead()
        {
            return this.yawHead;
        }

        public float getRenderYawOffset()
        {
            return this.renderYawOffset;
        }
    }
}
