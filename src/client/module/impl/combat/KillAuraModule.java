package client.module.impl.combat;

import client.event.MotionUpdateEvent;
import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.EnumSetting;
import client.setting.FloatSetting;
import client.setting.IntSetting;
import client.setting.SettingGroup;
import dwgx.foundation.math.RotationMath;
import dwgx.modulekit.CombatKit;
import dwgx.modulekit.ModuleKit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.Vec3;

/**
 * KillAura — Southside-style AABB closest-point targeting with raycast
 * validation before attacking. Uses discover offset for target acquisition
 * and strict range for attacks.
 */
public final class KillAuraModule extends Module
{
    public enum TargetMode { SWITCH, SINGLE }

    private final FloatSetting range;
    private final FloatSetting discoverOffset;
    private final IntSetting cps;
    private final EnumSetting<TargetMode> targetMode;
    private final FloatSetting rotationSpeed;
    private final BoolSetting autoBlock;
    private final BoolSetting playersOnly;
    private final BoolSetting throughWalls;

    private final Random random = new Random();
    private final List<Entity> switchQueue = new ArrayList<Entity>();
    private EntityLivingBase currentTarget;
    private float serverYaw;
    private float serverPitch;
    private long nextAttackMs;
    private boolean blocking;
    private boolean rotationActive;

    public KillAuraModule()
    {
        super("kill_aura", "KillAura", Category.COMBAT);
        SettingGroup general = this.addGroup("general", "General");
        this.range = this.addSetting(new FloatSetting("range", "Range", "Attack range", 3.8F, 1.0F, 6.0F, 0.1F));
        this.discoverOffset = this.addSetting(new FloatSetting("discover_offset", "Discover Offset", "Extra range for target discovery", 1.0F, 0.5F, 3.0F, 0.1F));
        this.cps = this.addSetting(new IntSetting("cps", "CPS", "Clicks per second", 10, 1, 20, 1));
        this.targetMode = this.addSetting(new EnumSetting<TargetMode>("target_mode", "Target Mode", "Target selection mode", TargetMode.class, TargetMode.SWITCH));
        this.rotationSpeed = this.addSetting(new FloatSetting("rotation_speed", "Rotation Speed", "Degrees per tick", 180.0F, 30.0F, 180.0F, 5.0F));
        this.autoBlock = this.addSetting(new BoolSetting("auto_block", "Auto Block", "Block with sword between attacks", true));
        this.playersOnly = this.addSetting(new BoolSetting("players_only", "Players Only", "Only target players", true));
        this.throughWalls = this.addSetting(new BoolSetting("through_walls", "Through Walls", "Attack through walls", false));
        general.add(this.range);
        general.add(this.discoverOffset);
        general.add(this.cps);
        general.add(this.targetMode);
        general.add(this.rotationSpeed);
        general.add(this.autoBlock);
        general.add(this.playersOnly);
        general.add(this.throughWalls);
    }

    public void onEnable()
    {
        this.currentTarget = null;
        this.switchQueue.clear();
        this.nextAttackMs = 0L;
        this.blocking = false;
        this.rotationActive = false;
    }

    public void onDisable()
    {
        if (this.blocking)
        {
            this.stopBlocking();
        }

        this.currentTarget = null;
        this.switchQueue.clear();
        this.rotationActive = false;
    }

    public void onMotionUpdate(MotionUpdateEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null)
        {
            return;
        }

        if (event.getPhase() == MotionUpdateEvent.Phase.PRE_UPDATE)
        {
            this.handlePreUpdate(mc.thePlayer, mc.theWorld);
        }
        else if (event.getPhase() == MotionUpdateEvent.Phase.PRE_MOTION)
        {
            this.handlePreMotion(mc.thePlayer);
        }
    }

    private void handlePreUpdate(EntityPlayerSP player, WorldClient world)
    {
        float r = this.range.get().floatValue();
        float discover = r + this.discoverOffset.get().floatValue();

        // Discover targets in extended range
        List<EntityLivingBase> targets = CombatKit.getTargetsInRange(
                world, player, discover, this.playersOnly.isEnabled(), this.throughWalls.isEnabled());

        // Strict filter: remove targets outside attack range (eye-to-box)
        for (int i = targets.size() - 1; i >= 0; --i)
        {
            if (RotationMath.distanceToBox(player, targets.get(i)) > (double) r)
            {
                targets.remove(i);
            }
        }

        if (targets.isEmpty())
        {
            this.currentTarget = null;
            this.rotationActive = false;

            if (this.blocking)
            {
                this.stopBlocking();
            }

            return;
        }

        // Target selection
        if (this.targetMode.get() == TargetMode.SINGLE)
        {
            this.currentTarget = CombatKit.selectTarget(targets, player, CombatKit.TargetMode.NEAREST);
        }
        else
        {
            // Switch mode: rotate through targets
            this.switchQueue.removeAll(targets);

            if (this.switchQueue.isEmpty())
            {
                for (int i = 0; i < targets.size(); ++i)
                {
                    this.switchQueue.add(targets.get(i));
                }
            }

            if (!this.switchQueue.isEmpty())
            {
                Entity next = this.switchQueue.remove(0);
                this.currentTarget = next instanceof EntityLivingBase ? (EntityLivingBase) next : null;
            }
        }

        if (this.currentTarget != null)
        {
            // AABB closest-point targeting (Southside approach)
            Vec3 eye = new Vec3(player.posX, player.posY + (double) player.getEyeHeight(), player.posZ);
            Vec3 closest = RotationMath.closestPointOnBox(eye, this.currentTarget.getEntityBoundingBox());
            float[] desired = RotationMath.toVec3(eye, closest);
            float speed = this.rotationSpeed.get().floatValue();
            this.serverYaw = RotationMath.smoothYaw(player.rotationYaw, desired[0], speed);
            this.serverPitch = RotationMath.smoothPitch(player.rotationPitch, desired[1], speed);
            this.rotationActive = true;
        }
    }

    private void handlePreMotion(EntityPlayerSP player)
    {
        if (!this.rotationActive || this.currentTarget == null)
        {
            return;
        }

        player.rotationYawHead = this.serverYaw;
        player.renderYawOffset = this.serverYaw;
        player.rotationYaw = this.serverYaw;
        player.rotationPitch = this.serverPitch;
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.theWorld == null
                || mc.currentScreen != null || mc.playerController == null)
        {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;

        if (this.currentTarget == null || this.currentTarget.isDead || !this.rotationActive)
        {
            if (this.blocking)
            {
                this.stopBlocking();
            }

            return;
        }

        float r = this.range.get().floatValue();

        if (RotationMath.distanceToBox(player, this.currentTarget) > (double) r)
        {
            if (this.blocking)
            {
                this.stopBlocking();
            }

            return;
        }

        // Raycast validation (Southside: BoundBoxUtils.calculateIntercept)
        Vec3 hitCheck = RotationMath.raycastEntity(player, this.currentTarget,
                this.serverYaw, this.serverPitch, (double) r + 1.0D);

        if (hitCheck == null && !this.throughWalls.isEnabled())
        {
            return;
        }

        long now = System.currentTimeMillis();

        if (now >= this.nextAttackMs)
        {
            if (this.blocking)
            {
                this.stopBlocking();
            }

            player.swingItem();
            mc.playerController.attackEntity(player, this.currentTarget);
            this.nextAttackMs = now + ModuleKit.cpsDelayMs(
                    this.random, this.cps.get().intValue(),
                    this.cps.get().intValue(), false);

            if (this.autoBlock.isEnabled() && this.isHoldingSword(player))
            {
                this.startBlocking(player);
            }
        }
    }

    private void startBlocking(EntityPlayerSP player)
    {
        if (this.blocking)
        {
            return;
        }

        player.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(player.getHeldItem()));
        this.blocking = true;
    }

    private void stopBlocking()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc != null && mc.thePlayer != null && mc.playerController != null)
        {
            mc.playerController.onStoppedUsingItem(mc.thePlayer);
        }

        this.blocking = false;
    }

    private boolean isHoldingSword(EntityPlayerSP player)
    {
        ItemStack held = player == null ? null : player.getHeldItem();
        return held != null && held.getItem() instanceof ItemSword;
    }
}
