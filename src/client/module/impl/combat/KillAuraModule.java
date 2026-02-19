package client.module.impl.combat;

import client.core.ClientBootstrap;
import client.module.Category;
import client.module.Module;
import client.rotation.RotationUtils;
import client.setting.BoolSetting;
import client.setting.EnumSetting;
import client.setting.FloatSetting;
import client.setting.IntSetting;
import client.setting.MultiSelectSetting;
import client.setting.SettingGroup;
import client.setting.Visibility;
import dwgx.modulekit.ModuleKit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C0APacketAnimation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class KillAuraModule extends Module
{
    private final EnumSetting<AttackMode> attackMode;
    private final EnumSetting<SortMode> sortMode;
    private final EnumSetting<RotationMode> rotationMode;
    private final EnumSetting<AimMode> aimMode;
    private final EnumSetting<AutoBlockMode> autoBlockMode;
    private final BoolSetting attackWhileScaffold;
    private final BoolSetting keepSprint;
    private final BoolSetting noSwing;
    private final BoolSetting rayCast;
    private final BoolSetting randomizeCps;
    private final BoolSetting rightClickOnly;
    private final MultiSelectSetting targetFilters;
    private final IntSetting minCps;
    private final IntSetting maxCps;
    private final IntSetting switchDelayTicks;
    private final IntSetting multiTargets;
    private final FloatSetting range;
    private final FloatSetting wallRange;
    private final FloatSetting fieldOfView;
    private final FloatSetting turnSpeed;
    private final FloatSetting blockRange;
    private final Random random = new Random();
    private EntityLivingBase currentTarget;
    private long nextAttackAtMs;
    private long nextSwitchAtMs;
    private int switchIndex;
    private boolean forcedUseItemState;
    private float silentYaw;
    private float silentPitch;
    private boolean silentRotationInitialized;

    public KillAuraModule()
    {
        super("killaura", "KillAura", Category.COMBAT);
        SettingGroup general = this.addGroup("general", "General");
        SettingGroup targeting = this.addGroup("targeting", "Targeting");
        SettingGroup rotation = this.addGroup("rotation", "Rotation");
        SettingGroup blocking = this.addGroup("blocking", "Blocking");

        this.attackMode = this.addSetting(new EnumSetting<AttackMode>("attack_mode", "Attack Mode", "Target cycle style", AttackMode.class, AttackMode.SINGLE));
        this.sortMode = this.addSetting(new EnumSetting<SortMode>("sort_mode", "Sort Mode", "How targets are prioritized", SortMode.class, SortMode.DISTANCE));
        this.rotationMode = this.addSetting(new EnumSetting<RotationMode>("rotation_mode", "Rotation Mode", "Aim rotation behavior", RotationMode.class, RotationMode.SMOOTH));
        this.aimMode = this.addSetting(new EnumSetting<AimMode>("aim_mode", "Aim Mode", "Legit moves camera; Silent sends look packet only", AimMode.class, AimMode.LEGIT));
        this.autoBlockMode = this.addSetting(new EnumSetting<AutoBlockMode>("auto_block_mode", "Auto Block", "Sword block behavior while targeting", AutoBlockMode.class, AutoBlockMode.FAKE));
        this.attackWhileScaffold = this.addSetting(new BoolSetting("attack_while_scaffold", "Attack While Scaffold", "Allow aura to run while scaffold is enabled", false));
        this.keepSprint = this.addSetting(new BoolSetting("keep_sprint", "Keep Sprint", "Restore sprint state after attack", true));
        this.noSwing = this.addSetting(new BoolSetting("no_swing", "No Swing", "Use packet animation instead of local arm swing", false));
        this.rayCast = this.addSetting(new BoolSetting("ray_cast", "Ray Cast", "Require crosshair ray-trace to hit current target", false));
        this.randomizeCps = this.addSetting(new BoolSetting("randomize_cps", "Randomize CPS", "Randomly vary clicks between min and max CPS", true));
        this.rightClickOnly = this.addSetting(new BoolSetting("right_click_only", "Right Click Only", "Attack only while holding use-item key", false));
        this.targetFilters = this.addSetting(new MultiSelectSetting(
            "target_filters",
            "Target Filters",
            "Choose which target categories can be attacked",
            new String[] {"players", "mobs", "animals", "invisible", "teammates"},
            new String[] {"players"}
        ));
        this.minCps = this.addSetting(new IntSetting("min_cps", "Min CPS", "Minimum clicks per second", 9, 1, 20, 1));
        this.maxCps = this.addSetting(new IntSetting("max_cps", "Max CPS", "Maximum clicks per second", 13, 1, 20, 1));
        this.switchDelayTicks = this.addSetting(new IntSetting("switch_delay_ticks", "Switch Delay", "Target switch interval in ticks", 8, 1, 40, 1));
        this.switchDelayTicks.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return KillAuraModule.this.attackMode.get() == AttackMode.SWITCH;
            }
        });
        this.multiTargets = this.addSetting(new IntSetting("multi_targets", "Multi Targets", "How many targets to attack per cycle", 2, 1, 6, 1));
        this.multiTargets.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return KillAuraModule.this.attackMode.get() == AttackMode.MULTIPLE;
            }
        });
        this.range = this.addSetting(new FloatSetting("range", "Range", "Visible target range", 3.20F, 2.00F, 6.00F, 0.05F));
        this.wallRange = this.addSetting(new FloatSetting("wall_range", "Wall Range", "Hidden target range when line of sight is blocked", 2.50F, 0.50F, 6.00F, 0.05F));
        this.fieldOfView = this.addSetting(new FloatSetting("field_of_view", "Field of View", "Horizontal target scan cone in degrees", 180.0F, 30.0F, 360.0F, 1.0F));
        this.turnSpeed = this.addSetting(new FloatSetting("turn_speed", "Turn Speed", "Max yaw/pitch step per tick in smooth mode", 40.0F, 1.0F, 180.0F, 1.0F));
        this.turnSpeed.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return KillAuraModule.this.rotationMode.get() == RotationMode.SMOOTH;
            }
        });
        this.blockRange = this.addSetting(new FloatSetting("block_range", "Block Range", "Distance where vanilla autoblock is held", 4.20F, 1.00F, 7.00F, 0.05F));
        this.blockRange.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return KillAuraModule.this.autoBlockMode.get() == AutoBlockMode.VANILLA;
            }
        });

        general.add(this.attackMode);
        general.add(this.keepSprint);
        general.add(this.noSwing);
        general.add(this.randomizeCps);
        general.add(this.minCps);
        general.add(this.maxCps);
        general.add(this.switchDelayTicks);
        general.add(this.multiTargets);
        general.add(this.attackWhileScaffold);

        targeting.add(this.range);
        targeting.add(this.wallRange);
        targeting.add(this.fieldOfView);
        targeting.add(this.sortMode);
        targeting.add(this.targetFilters);

        rotation.add(this.rotationMode);
        rotation.add(this.aimMode);
        rotation.add(this.turnSpeed);
        rotation.add(this.rayCast);

        blocking.add(this.autoBlockMode);
        blocking.add(this.blockRange);
        blocking.add(this.rightClickOnly);
    }

    public void onEnable()
    {
        this.currentTarget = null;
        this.nextAttackAtMs = 0L;
        this.nextSwitchAtMs = 0L;
        this.switchIndex = 0;
        this.forcedUseItemState = false;
        this.silentRotationInitialized = false;
    }

    public void onDisable()
    {
        this.currentTarget = null;
        this.releaseManagedUseItem();
        this.silentRotationInitialized = false;
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.playerController == null || mc.currentScreen != null)
        {
            this.currentTarget = null;
            this.releaseManagedUseItem();
            return;
        }

        EntityPlayerSP player = mc.thePlayer;

        if (player.isDead || player.getHealth() <= 0.0F)
        {
            this.currentTarget = null;
            this.releaseManagedUseItem();
            return;
        }

        if (!this.attackWhileScaffold.isEnabled() && this.isScaffoldEnabled())
        {
            this.currentTarget = null;
            this.releaseManagedUseItem();
            return;
        }

        if (this.rightClickOnly.isEnabled() && !this.isKeyOrMouseDown(mc.gameSettings.keyBindUseItem.getKeyCode()))
        {
            this.currentTarget = null;
            this.releaseManagedUseItem();
            return;
        }

        List<EntityLivingBase> targets = this.collectTargets(mc, player);

        if (targets.isEmpty())
        {
            this.currentTarget = null;
            this.releaseManagedUseItem();
            return;
        }

        this.sortTargets(player, targets);
        EntityLivingBase selected = this.selectPrimaryTarget(targets);
        this.currentTarget = selected;
        long now = System.currentTimeMillis();

        if (now >= this.nextAttackAtMs)
        {
            boolean attacked = false;

            if (this.attackMode.get() == AttackMode.MULTIPLE)
            {
                int cap = Math.max(1, this.multiTargets.get().intValue());
                int limit = Math.min(cap, targets.size());

                for (int i = 0; i < limit; ++i)
                {
                    EntityLivingBase target = targets.get(i);

                    if (this.tryAttackTarget(mc, player, target))
                    {
                        attacked = true;
                    }
                }
            }
            else if (selected != null)
            {
                attacked = this.tryAttackTarget(mc, player, selected);
            }

            if (attacked)
            {
                this.scheduleNextAttack(now);
            }
        }

        this.updateAutoBlockState(mc, player, this.currentTarget);
    }

    private List<EntityLivingBase> collectTargets(Minecraft mc, EntityPlayerSP player)
    {
        List<EntityLivingBase> targets = new ArrayList<EntityLivingBase>();
        List<?> entities = mc.theWorld.loadedEntityList;

        for (int i = 0; i < entities.size(); ++i)
        {
            Object value = entities.get(i);

            if (!(value instanceof EntityLivingBase))
            {
                continue;
            }

            EntityLivingBase target = (EntityLivingBase)value;

            if (!this.isValidTarget(player, target))
            {
                continue;
            }

            targets.add(target);
        }

        return targets;
    }

    private boolean isValidTarget(EntityPlayerSP player, EntityLivingBase target)
    {
        if (target == null || target == player)
        {
            return false;
        }

        if (target instanceof EntityArmorStand || target.isDead || target.getHealth() <= 0.0F)
        {
            return false;
        }

        if (!this.targetFilters.isSelected("invisible") && target.isInvisible())
        {
            return false;
        }

        if (!this.isAllowedEntityType(player, target))
        {
            return false;
        }

        if (!this.isWithinFieldOfView(player, target))
        {
            return false;
        }

        float distance = player.getDistanceToEntity(target);
        float visibleRange = this.range.get().floatValue();
        float blockedRange = this.wallRange.get().floatValue();
        boolean visible = player.canEntityBeSeen(target);
        float allowedRange = visible ? visibleRange : blockedRange;

        return distance <= allowedRange;
    }

    private boolean isAllowedEntityType(EntityPlayerSP player, EntityLivingBase target)
    {
        if (target instanceof EntityPlayer)
        {
            if (!this.targetFilters.isSelected("players"))
            {
                return false;
            }

            if (!this.targetFilters.isSelected("teammates") && player.isOnSameTeam(target))
            {
                return false;
            }

            return true;
        }

        if (target instanceof IMob)
        {
            return this.targetFilters.isSelected("mobs");
        }

        if (target instanceof IAnimals)
        {
            return this.targetFilters.isSelected("animals");
        }

        return false;
    }

    private boolean isWithinFieldOfView(EntityPlayerSP player, Entity target)
    {
        if (player == null || target == null)
        {
            return false;
        }

        float fov = Math.max(1.0F, this.fieldOfView.get().floatValue());

        if (fov >= 359.9F)
        {
            return true;
        }

        float targetYaw = RotationUtils.fromEntity(player, target).getYaw();
        float diff = Math.abs(RotationUtils.yawDelta(player.rotationYaw, targetYaw));
        return diff <= fov * 0.5F;
    }

    private void sortTargets(final EntityPlayerSP player, List<EntityLivingBase> targets)
    {
        final SortMode mode = this.sortMode.get();
        Collections.sort(targets, new Comparator<EntityLivingBase>()
        {
            public int compare(EntityLivingBase left, EntityLivingBase right)
            {
                if (mode == SortMode.HEALTH)
                {
                    return Float.compare(left.getHealth(), right.getHealth());
                }
                else if (mode == SortMode.HURT_TIME)
                {
                    return left.hurtResistantTime - right.hurtResistantTime;
                }

                float dl = player.getDistanceToEntity(left);
                float dr = player.getDistanceToEntity(right);
                return Float.compare(dl, dr);
            }
        });
    }

    private EntityLivingBase selectPrimaryTarget(List<EntityLivingBase> targets)
    {
        if (targets.isEmpty())
        {
            return null;
        }

        if (this.attackMode.get() != AttackMode.SWITCH)
        {
            this.switchIndex = 0;
            return targets.get(0);
        }

        long now = System.currentTimeMillis();

        if (now >= this.nextSwitchAtMs)
        {
            this.switchIndex++;
            int delayTicks = Math.max(1, this.switchDelayTicks.get().intValue());
            this.nextSwitchAtMs = now + delayTicks * 50L;
        }

        if (this.switchIndex < 0)
        {
            this.switchIndex = 0;
        }

        if (this.switchIndex >= targets.size())
        {
            this.switchIndex = this.switchIndex % targets.size();
        }

        return targets.get(this.switchIndex);
    }

    private boolean tryAttackTarget(Minecraft mc, EntityPlayerSP player, EntityLivingBase target)
    {
        if (target == null || target.isDead || target.getHealth() <= 0.0F)
        {
            return false;
        }

        this.updateAim(mc, player, target);

        if (!this.passesRayCastCheck(mc, target))
        {
            return false;
        }

        this.releaseManagedUseItem();
        this.performAttack(mc, player, target);
        return true;
    }

    private void updateAim(Minecraft mc, EntityPlayerSP player, EntityLivingBase target)
    {
        RotationMode mode = this.rotationMode.get();

        if (mode == RotationMode.NONE)
        {
            return;
        }

        RotationUtils.Rotation targetRotation = RotationUtils.fromEntity(player, target);
        RotationUtils.Rotation nextRotation = targetRotation;

        if (mode == RotationMode.SMOOTH)
        {
            float speed = Math.max(1.0F, this.turnSpeed.get().floatValue());

            if (this.aimMode.get() == AimMode.SILENT)
            {
                if (!this.silentRotationInitialized)
                {
                    this.silentYaw = player.rotationYaw;
                    this.silentPitch = player.rotationPitch;
                    this.silentRotationInitialized = true;
                }

                nextRotation = RotationUtils.stepSmooth(this.silentYaw, this.silentPitch, targetRotation.getYaw(), targetRotation.getPitch(), speed);
                this.silentYaw = nextRotation.getYaw();
                this.silentPitch = nextRotation.getPitch();
            }
            else
            {
                nextRotation = RotationUtils.stepSmooth(player.rotationYaw, player.rotationPitch, targetRotation.getYaw(), targetRotation.getPitch(), speed);
            }
        }

        if (this.aimMode.get() == AimMode.SILENT)
        {
            RotationUtils.sendSilentLook(player, nextRotation.getYaw(), nextRotation.getPitch());
        }
        else
        {
            RotationUtils.applyLegit(player, nextRotation.getYaw(), nextRotation.getPitch());
        }
    }

    private void performAttack(Minecraft mc, EntityPlayerSP player, EntityLivingBase target)
    {
        boolean sprinting = player.isSprinting();

        if (this.noSwing.isEnabled())
        {
            player.sendQueue.addToSendQueue(new C0APacketAnimation());
        }
        else
        {
            player.swingItem();
        }

        mc.playerController.attackEntity(player, target);

        if (this.keepSprint.isEnabled())
        {
            player.setSprinting(sprinting);
        }
    }

    private boolean passesRayCastCheck(Minecraft mc, EntityLivingBase target)
    {
        if (!this.rayCast.isEnabled() || target == null)
        {
            return true;
        }

        if (this.aimMode.get() == AimMode.SILENT)
        {
            return true;
        }

        return ModuleKit.raycastHitsEntity(mc, target, 1.0F);
    }

    private void updateAutoBlockState(Minecraft mc, EntityPlayerSP player, EntityLivingBase target)
    {
        if (mc == null || mc.gameSettings == null)
        {
            return;
        }

        if (this.autoBlockMode.get() != AutoBlockMode.VANILLA || player == null || target == null || !this.hasSwordInHand(player))
        {
            this.releaseManagedUseItem();
            return;
        }

        float distance = player.getDistanceToEntity(target);

        if (distance > this.blockRange.get().floatValue())
        {
            this.releaseManagedUseItem();
            return;
        }

        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        boolean physical = this.isKeyOrMouseDown(useKey);
        KeyBinding.setKeyBindState(useKey, true);
        this.forcedUseItemState = !physical;
    }

    private void releaseManagedUseItem()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.gameSettings == null)
        {
            this.forcedUseItemState = false;
            return;
        }

        if (!this.forcedUseItemState)
        {
            return;
        }

        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(useKey, this.isKeyOrMouseDown(useKey));
        this.forcedUseItemState = false;
    }

    private boolean hasSwordInHand(EntityPlayerSP player)
    {
        return player != null && player.getHeldItem() != null && player.getHeldItem().getItem() instanceof ItemSword;
    }

    private boolean isScaffoldEnabled()
    {
        ClientBootstrap bootstrap = ClientBootstrap.instance();

        if (bootstrap == null || bootstrap.getModules() == null)
        {
            return false;
        }

        Module scaffold = bootstrap.getModules().getById("scaffold");
        return scaffold != null && scaffold.isEnabled();
    }

    private void scheduleNextAttack(long now)
    {
        long delayMs = ModuleKit.cpsDelayMs(
            this.random,
            this.minCps.get().intValue(),
            this.maxCps.get().intValue(),
            this.randomizeCps.isEnabled()
        );
        this.nextAttackAtMs = now + delayMs;
    }

    public EntityLivingBase getCurrentTarget()
    {
        return this.currentTarget;
    }

    private boolean isKeyOrMouseDown(int keyCode)
    {
        if (keyCode >= 0)
        {
            return keyCode < 256 && Keyboard.isKeyDown(keyCode);
        }

        int button = keyCode + 100;
        return button >= 0 && button < 16 && Mouse.isButtonDown(button);
    }

    public static enum AttackMode
    {
        SINGLE,
        SWITCH,
        MULTIPLE
    }

    public static enum SortMode
    {
        DISTANCE,
        HEALTH,
        HURT_TIME
    }

    public static enum RotationMode
    {
        NONE,
        SNAP,
        SMOOTH
    }

    public static enum AimMode
    {
        LEGIT,
        SILENT
    }

    public static enum AutoBlockMode
    {
        OFF,
        FAKE,
        VANILLA
    }
}
