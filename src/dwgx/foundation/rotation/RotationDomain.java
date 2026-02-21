package dwgx.foundation.rotation;

import java.util.Random;

/**
 * Rotation helpers for combat/world modules.
 *
 * <p>Goals:
 * <ul>
 * <li>Keep compatibility with the previous RotationUtils API surface.</li>
 * <li>Provide smooth and configurable turn stepping for future modules.</li>
 * <li>Support optional vanilla mouse-GCD quantization for legit-like output.</li>
 * </ul>
 */
public final class RotationDomain
{
    private static final float MIN_PITCH = -89.9F;
    private static final float MAX_PITCH = 89.9F;
    private static final float DEFAULT_DT_SECONDS = 1.0F / 20.0F;
    private static final float MIN_DT_SECONDS = 0.001F;
    private static final float MAX_DT_SECONDS = 0.100F;
    private static final float EPSILON = 1.0E-4F;

    private RotationDomain()
    {
    }

    public static Rotation fromEyeTo(net.minecraft.client.entity.EntityPlayerSP player, net.minecraft.util.Vec3 targetVec)
    {
        if (player == null || targetVec == null)
        {
            return new Rotation(0.0F, 0.0F);
        }

        return fromEyeTo(
            player.posX,
            player.posY + (double)player.getEyeHeight(),
            player.posZ,
            targetVec.xCoord,
            targetVec.yCoord,
            targetVec.zCoord
        );
    }

    public static Rotation fromEyeTo(double eyeX, double eyeY, double eyeZ, double targetX, double targetY, double targetZ)
    {
        double dx = targetX - eyeX;
        double dy = targetY - eyeY;
        double dz = targetZ - eyeZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontal)));
        return snap(yaw, pitch);
    }

    /**
     * Returns "smart" net.minecraft.entity.Entity aim rotation by clamping to a stable point in target bounding box.
     */
    public static Rotation fromEntity(net.minecraft.client.entity.EntityPlayerSP player, net.minecraft.entity.Entity target)
    {
        if (player == null || target == null)
        {
            return new Rotation(0.0F, 0.0F);
        }

        double eyeX = player.posX;
        double eyeY = player.posY + (double)player.getEyeHeight();
        double eyeZ = player.posZ;

        net.minecraft.util.AxisAlignedBB bb = target.getEntityBoundingBox();

        if (bb == null)
        {
            return fromEyeTo(
                eyeX,
                eyeY,
                eyeZ,
                target.posX,
                target.posY + (double)(Math.max(0.8F, target.height) * 0.85F),
                target.posZ
            );
        }

        double targetX = net.minecraft.util.MathHelper.clamp_double(eyeX, bb.minX, bb.maxX);
        double minY = bb.minY + Math.min(0.20D, Math.max(0.01D, bb.maxY - bb.minY));
        double maxY = bb.maxY - 0.10D;

        if (maxY < minY)
        {
            maxY = bb.maxY;
        }

        double targetY = net.minecraft.util.MathHelper.clamp_double(eyeY, minY, maxY);
        double targetZ = net.minecraft.util.MathHelper.clamp_double(eyeZ, bb.minZ, bb.maxZ);
        return fromEyeTo(eyeX, eyeY, eyeZ, targetX, targetY, targetZ);
    }

    public static Rotation current(net.minecraft.client.entity.EntityPlayerSP player)
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

        float capped = Math.max(1.0F, maxStep);
        float minStep = Math.min(2.5F, Math.max(0.05F, capped * 0.18F));
        return stepSmooth(
            current,
            target,
            minStep,
            capped,
            0.62F,
            false,
            null,
            DEFAULT_DT_SECONDS,
            null,
            0.0F
        );
    }

    public static Rotation stepSmooth(float currentYaw, float currentPitch, float targetYaw, float targetPitch, float maxStep)
    {
        return stepSmooth(new Rotation(currentYaw, currentPitch), new Rotation(targetYaw, targetPitch), maxStep);
    }

    /**
     * Tick-scaled smooth rotation step.
     *
     * @param minStep minimum per-tick movement (degrees)
     * @param maxStep maximum per-tick movement (degrees)
     * @param smoothness response factor in [0, 1]
     * @param quantizeMouseGcd whether to quantize resulting delta to vanilla mouse gcd
     * @param mc net.minecraft.client.Minecraft instance required only when quantizeMouseGcd is true
     * @param deltaSeconds elapsed seconds for this step
     * @param random optional random for micro jitter
     * @param jitterAmount optional small jitter in degrees
     */
    public static Rotation stepSmooth(
        Rotation current,
        Rotation target,
        float minStep,
        float maxStep,
        float smoothness,
        boolean quantizeMouseGcd,
        net.minecraft.client.Minecraft mc,
        float deltaSeconds,
        Random random,
        float jitterAmount
    )
    {
        if (current == null || target == null)
        {
            return new Rotation(0.0F, 0.0F);
        }

        float dtScale = normalizedTickScale(deltaSeconds);
        float lo = Math.max(0.0F, Math.min(minStep, maxStep)) * dtScale;
        float hi = Math.max(lo, Math.max(minStep, maxStep)) * dtScale;
        float response = 0.16F + net.minecraft.util.MathHelper.clamp_float(smoothness, 0.0F, 1.0F) * 0.72F;
        float targetYaw = closestEquivalentYaw(current.getYaw(), target.getYaw());
        float yawError = targetYaw - current.getYaw();
        float pitchError = pitchDelta(current.getPitch(), target.getPitch());
        float yawStep = solveAxisStep(yawError, lo, hi, response);
        float pitchStep = solveAxisStep(pitchError, lo, hi, response);

        if (random != null && jitterAmount > EPSILON)
        {
            float yawWeight = net.minecraft.util.MathHelper.clamp_float(Math.abs(yawError) / 70.0F, 0.0F, 1.0F);
            float pitchWeight = net.minecraft.util.MathHelper.clamp_float(Math.abs(pitchError) / 55.0F, 0.0F, 1.0F);
            yawStep += signedRandom(random) * jitterAmount * yawWeight;
            pitchStep += signedRandom(random) * jitterAmount * 0.70F * pitchWeight;
            yawStep = clampStep(yawStep, yawError, hi);
            pitchStep = clampStep(pitchStep, pitchError, hi);
        }

        Rotation stepped = snap(current.getYaw() + yawStep, current.getPitch() + pitchStep);

        if (quantizeMouseGcd)
        {
            stepped = quantizeByMouseGcd(mc, current, stepped);
        }

        float snapTolerance = 0.01F;

        if (quantizeMouseGcd)
        {
            snapTolerance = Math.max(snapTolerance, mouseGcd(mc) * 0.5F);
        }

        if (Math.abs(stepped.getYaw() - targetYaw) <= snapTolerance)
        {
            stepped = new Rotation(targetYaw, stepped.getPitch());
        }

        if (Math.abs(pitchDelta(stepped.getPitch(), target.getPitch())) <= snapTolerance)
        {
            stepped = new Rotation(stepped.getYaw(), target.getPitch());
        }

        return snap(stepped.getYaw(), stepped.getPitch());
    }

    /**
     * Stateful spring-like smoother.
     *
     * <p>This is recommended for long-running target tracking (KillAura/Scaffold). Keep one
     * SmoothState per module instance and reset it when target context changes.
     */
    public static Rotation stepSmoothDamped(
        Rotation current,
        Rotation target,
        SmoothState state,
        float maxYawSpeedDegPerSec,
        float maxPitchSpeedDegPerSec,
        float responsiveness,
        float damping,
        float deltaSeconds,
        boolean quantizeMouseGcd,
        net.minecraft.client.Minecraft mc
    )
    {
        if (current == null || target == null || state == null)
        {
            return new Rotation(0.0F, 0.0F);
        }

        float dt = clampDt(deltaSeconds);
        float maxYawSpeed = Math.max(20.0F, maxYawSpeedDegPerSec);
        float maxPitchSpeed = Math.max(20.0F, maxPitchSpeedDegPerSec);
        float accel = net.minecraft.util.MathHelper.clamp_float(responsiveness, 2.0F, 40.0F);
        float damp = net.minecraft.util.MathHelper.clamp_float(damping, 0.0F, 0.995F);
        float targetYaw = closestEquivalentYaw(current.getYaw(), target.getYaw());
        float yawError = targetYaw - current.getYaw();
        float pitchError = pitchDelta(current.getPitch(), target.getPitch());
        state.yawVelocity = state.yawVelocity * damp + yawError * accel * dt;
        state.pitchVelocity = state.pitchVelocity * damp + pitchError * accel * dt;
        state.yawVelocity = net.minecraft.util.MathHelper.clamp_float(state.yawVelocity, -maxYawSpeed, maxYawSpeed);
        state.pitchVelocity = net.minecraft.util.MathHelper.clamp_float(state.pitchVelocity, -maxPitchSpeed, maxPitchSpeed);
        float yawStep = clampStep(state.yawVelocity * dt, yawError, maxYawSpeed * dt);
        float pitchStep = clampStep(state.pitchVelocity * dt, pitchError, maxPitchSpeed * dt);
        Rotation stepped = snap(current.getYaw() + yawStep, current.getPitch() + pitchStep);

        if (Math.abs(yawError) <= Math.max(0.02F, maxYawSpeed * dt * 0.06F))
        {
            stepped = new Rotation(targetYaw, stepped.getPitch());
            state.yawVelocity = 0.0F;
        }

        if (Math.abs(pitchError) <= Math.max(0.02F, maxPitchSpeed * dt * 0.06F))
        {
            stepped = new Rotation(stepped.getYaw(), target.getPitch());
            state.pitchVelocity = 0.0F;
        }

        if (quantizeMouseGcd)
        {
            stepped = quantizeByMouseGcd(mc, current, stepped);
        }

        return snap(stepped.getYaw(), stepped.getPitch());
    }

    public static Rotation snap(float yaw, float pitch)
    {
        return new Rotation(yaw, clampPitch(pitch));
    }

    public static float yawDelta(float currentYaw, float targetYaw)
    {
        return net.minecraft.util.MathHelper.wrapAngleTo180_float(targetYaw - currentYaw);
    }

    /**
     * Selects the 360-degree equivalent of targetYaw that is closest to referenceYaw.
     * This keeps absolute yaw continuous across -180/180 boundaries.
     */
    public static float closestEquivalentYaw(float referenceYaw, float targetYaw)
    {
        return referenceYaw + yawDelta(referenceYaw, targetYaw);
    }

    public static float pitchDelta(float currentPitch, float targetPitch)
    {
        return targetPitch - currentPitch;
    }

    public static boolean isAligned(net.minecraft.client.entity.EntityPlayerSP player, float targetYaw, float targetPitch, float tolerance)
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

    public static void applyLegit(net.minecraft.client.entity.EntityPlayerSP player, float yaw, float pitch)
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

    public static void applyLegit(net.minecraft.client.entity.EntityPlayerSP player, Rotation rotation)
    {
        if (rotation == null)
        {
            return;
        }

        applyLegit(player, rotation.getYaw(), rotation.getPitch());
    }

    public static void sendSilentLook(net.minecraft.client.entity.EntityPlayerSP player, float yaw, float pitch)
    {
        if (player == null || player.sendQueue == null)
        {
            return;
        }

        Rotation snapped = snap(yaw, pitch);
        player.sendQueue.addToSendQueue(new net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook(snapped.getYaw(), snapped.getPitch(), player.onGround));
    }

    public static void applyToLookPacket(net.minecraft.network.play.client.C03PacketPlayer packet, float yaw, float pitch)
    {
        if (packet == null)
        {
            return;
        }

        Rotation snapped = snap(yaw, pitch);
        packet.setYaw(snapped.getYaw());
        packet.setPitch(snapped.getPitch());
    }

    public static RotationSnapshot capture(net.minecraft.client.entity.EntityPlayerSP player)
    {
        if (player == null)
        {
            return null;
        }

        return new RotationSnapshot(player.rotationYaw, player.rotationPitch, player.rotationYawHead, player.renderYawOffset);
    }

    public static void restore(net.minecraft.client.entity.EntityPlayerSP player, RotationSnapshot snapshot)
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
        return net.minecraft.util.MathHelper.clamp_float(pitch, MIN_PITCH, MAX_PITCH);
    }

    /**
     * Vanilla look-step granularity derived from mouse sensitivity.
     *
     * <p>Formula source: mouse delta -> net.minecraft.entity.Entity#setAngles path in 1.8.9.
     */
    public static float mouseGcd(net.minecraft.client.Minecraft mc)
    {
        if (mc == null || mc.gameSettings == null)
        {
            return 0.0F;
        }

        float sensitivity = net.minecraft.util.MathHelper.clamp_float(mc.gameSettings.mouseSensitivity, 0.0F, 1.0F);
        float curve = sensitivity * 0.6F + 0.2F;
        return curve * curve * curve * 1.2F;
    }

    public static Rotation quantizeByMouseGcd(net.minecraft.client.Minecraft mc, Rotation base, Rotation desired)
    {
        if (base == null && desired == null)
        {
            return new Rotation(0.0F, 0.0F);
        }

        if (base == null)
        {
            return snap(desired.getYaw(), desired.getPitch());
        }

        if (desired == null)
        {
            return snap(base.getYaw(), base.getPitch());
        }

        float gcd = mouseGcd(mc);

        if (gcd <= EPSILON)
        {
            return snap(desired.getYaw(), desired.getPitch());
        }

        float rawYawDelta = yawDelta(base.getYaw(), desired.getYaw());
        float rawPitchDelta = pitchDelta(base.getPitch(), desired.getPitch());
        float qYawDelta = quantizeTowardsZero(rawYawDelta, gcd);
        float qPitchDelta = quantizeTowardsZero(rawPitchDelta, gcd);

        if (Math.abs(qYawDelta) <= EPSILON && Math.abs(rawYawDelta) >= gcd * 0.5F)
        {
            qYawDelta = Math.copySign(gcd, rawYawDelta);
        }

        if (Math.abs(qPitchDelta) <= EPSILON && Math.abs(rawPitchDelta) >= gcd * 0.5F)
        {
            qPitchDelta = Math.copySign(gcd, rawPitchDelta);
        }

        if (Math.abs(qYawDelta) > Math.abs(rawYawDelta))
        {
            qYawDelta = rawYawDelta;
        }

        if (Math.abs(qPitchDelta) > Math.abs(rawPitchDelta))
        {
            qPitchDelta = rawPitchDelta;
        }

        return snap(base.getYaw() + qYawDelta, base.getPitch() + qPitchDelta);
    }

    private static float solveAxisStep(float error, float minStep, float maxStep, float response)
    {
        float absError = Math.abs(error);

        if (absError <= EPSILON)
        {
            return 0.0F;
        }

        float lo = Math.max(0.0F, Math.min(minStep, maxStep));
        float hi = Math.max(lo, Math.max(minStep, maxStep));
        float step = absError * net.minecraft.util.MathHelper.clamp_float(response, 0.0F, 1.0F);
        step = Math.max(step, lo);
        step = Math.min(step, hi);
        step = Math.min(step, absError);
        return Math.copySign(step, error);
    }

    private static float clampStep(float step, float error, float maxStep)
    {
        float limited = net.minecraft.util.MathHelper.clamp_float(step, -Math.abs(maxStep), Math.abs(maxStep));

        if (Math.abs(limited) > Math.abs(error))
        {
            limited = error;
        }

        return limited;
    }

    private static float quantizeTowardsZero(float value, float step)
    {
        if (step <= EPSILON)
        {
            return value;
        }

        float scaled = value / step;

        if (scaled > 0.0F)
        {
            return (float)Math.floor(scaled) * step;
        }

        return (float)Math.ceil(scaled) * step;
    }

    private static float normalizedTickScale(float deltaSeconds)
    {
        return clampDt(deltaSeconds) / DEFAULT_DT_SECONDS;
    }

    private static float clampDt(float deltaSeconds)
    {
        float dt = deltaSeconds <= 0.0F ? DEFAULT_DT_SECONDS : deltaSeconds;
        return net.minecraft.util.MathHelper.clamp_float(dt, MIN_DT_SECONDS, MAX_DT_SECONDS);
    }

    private static float signedRandom(Random random)
    {
        return (random.nextFloat() - 0.5F) * 2.0F;
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

    /**
     * Mutable damping state for stepSmoothDamped.
     */
    public static final class SmoothState
    {
        private float yawVelocity;
        private float pitchVelocity;

        public void reset()
        {
            this.yawVelocity = 0.0F;
            this.pitchVelocity = 0.0F;
        }

        public float getYawVelocity()
        {
            return this.yawVelocity;
        }

        public float getPitchVelocity()
        {
            return this.pitchVelocity;
        }
    }
}


