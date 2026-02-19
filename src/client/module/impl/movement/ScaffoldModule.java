package client.module.impl.movement;

import client.event.EventBusPriorities;
import client.event.Listener;
import client.event.TickEvent;
import client.event.annotations.EventLink;
import client.module.Category;
import client.module.Module;
import client.rotation.RotationUtils;
import client.setting.BoolSetting;
import client.setting.EnumSetting;
import client.setting.FloatSetting;
import client.setting.IntSetting;
import client.setting.SettingGroup;
import client.setting.Visibility;
import dwgx.modulekit.ModuleKit;
import dwgx.scaffold.ScaffoldUtils;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

public final class ScaffoldModule extends Module
{
    private final BoolSetting onlyWhenMoving;
    private final BoolSetting autoSwitch;
    private final BoolSetting swingHand;
    private final BoolSetting keepBridgeY;
    private final BoolSetting adaptivePrediction;
    private final BoolSetting strictAim;
    private final BoolSetting omniDirectionalExpand;
    private final BoolSetting safeEdgeSneak;
    private final BoolSetting airSafeEdge;
    private final EnumSetting<ScaffoldPreset> preset;
    private final EnumSetting<ScaffoldUtils.PlaceMode> placeMode;
    private final EnumSetting<ScaffoldUtils.AimMode> aimMode;
    private final EnumSetting<ScaffoldUtils.RotationMode> rotationMode;
    private final EnumSetting<BridgeProfile> bridgeProfile;
    private final EnumSetting<RaycastMode> raycastMode;
    private final EnumSetting<SlotSelectionMode> slotSelection;
    private final EnumSetting<SameYMode> sameYMode;
    private final EnumSetting<DownwardsMode> downwardsMode;
    private final EnumSetting<TowerMode> towerMode;
    private final EnumSetting<SprintMode> sprintMode;
    private final EnumSetting<EagleMode> eagleMode;
    private final IntSetting searchRadius;
    private final IntSetting placeDelay;
    private final IntSetting placeDelayJitter;
    private final IntSetting extensionDistance;
    private final IntSetting extraPlaceAttempts;
    private final IntSetting eagleBlocks;
    private final IntSetting eagleTicks;
    private final FloatSetting lookAheadDistance;
    private final FloatSetting predictionGain;
    private final FloatSetting stabilityBias;
    private final FloatSetting minMoveSpeed;
    private final FloatSetting aimTolerance;
    private final FloatSetting turnSpeed;
    private final FloatSetting eagleEdgeDistance;
    private final Random random = new Random();
    private int placeCooldown;
    private int lockedBridgeY;
    private BlockPos lastPlacedPos;
    private int ticksSinceLastPlacement;
    private int blocksPlacedWithoutEagle;
    private int eagleSneakTicks;
    private boolean forcedKeySneak;
    private boolean forcedSilentSneak;
    private boolean suppressedSprint;
    private ScaffoldPreset lastPreset;
    private float modeStrafeDamping = 1.0F;
    private boolean modeAutoJumpAssist;
    private boolean modeStrictRaycast;

    @EventLink(value = EventBusPriorities.HIGH)
    private final Listener<TickEvent> onTickEvent = new Listener<TickEvent>()
    {
        public void onEvent(TickEvent event)
        {
            if (event == null || event.getPhase() != TickEvent.Phase.PRE)
            {
                return;
            }

            ScaffoldModule.this.onPreTick();
        }
    };

    public ScaffoldModule()
    {
        super("scaffold", "Scaffold", Category.MOVEMENT);
        SettingGroup general = this.addGroup("general", "General");
        SettingGroup placement = this.addGroup("placement", "Placement");
        SettingGroup rotation = this.addGroup("rotation", "Rotation");
        SettingGroup movement = this.addGroup("movement", "Movement");
        SettingGroup safety = this.addGroup("safety", "Safety");

        this.preset = this.addSetting(new EnumSetting<ScaffoldPreset>("preset", "Preset", "Ready-made scaffold styles (normal / rewinside / expand / telly / godbridge / breezily / watchdog / eagle / custom)", ScaffoldPreset.class, ScaffoldPreset.NORMAL));
        this.onlyWhenMoving = this.addSetting(new BoolSetting("only_moving", "Only When Moving", "Place blocks only while movement input is active", true));
        this.autoSwitch = this.addSetting(new BoolSetting("auto_switch", "Auto Switch", "Switch to a valid hotbar block slot automatically", true));
        this.swingHand = this.addSetting(new BoolSetting("swing_hand", "Swing Hand", "Play hand swing animation after placement", true));
        this.slotSelection = this.addSetting(new EnumSetting<SlotSelectionMode>("slot_selection", "Slot Selection", "How block slots are picked when switching", SlotSelectionMode.class, SlotSelectionMode.BEST_COUNT));
        this.placeMode = this.addSetting(new EnumSetting<ScaffoldUtils.PlaceMode>("place_mode", "Place Mode", "Bridge preference: smart / under / forward / expand", ScaffoldUtils.PlaceMode.class, ScaffoldUtils.PlaceMode.SMART));
        this.extensionDistance = this.addSetting(new IntSetting("extension", "Extension", "Extra forward scan steps for longer bridge reach", 1, 0, 6, 1));
        this.omniDirectionalExpand = this.addSetting(new BoolSetting("omni_directional_expand", "Omni Expand", "Expand follows current yaw vector instead of axis only", false));
        this.omniDirectionalExpand.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return ScaffoldModule.this.placeMode.get() == ScaffoldUtils.PlaceMode.EXPAND;
            }
        });
        this.searchRadius = this.addSetting(new IntSetting("search_radius", "Search Radius", "Extra horizontal scan distance around target point", 1, 0, 6, 1));
        this.placeDelay = this.addSetting(new IntSetting("place_delay", "Place Delay", "Tick interval between placements", 0, 0, 8, 1));
        this.placeDelayJitter = this.addSetting(new IntSetting("place_delay_jitter", "Delay Jitter", "Random extra tick delay added after each placement", 0, 0, 4, 1));
        this.extraPlaceAttempts = this.addSetting(new IntSetting("extra_place_attempts", "Extra Attempts", "Extra placement retries in the same tick", 0, 0, 3, 1));
        this.lookAheadDistance = this.addSetting(new FloatSetting("look_ahead_distance", "Look Ahead", "Base forward bridging distance in blocks", 0.85F, 0.35F, 2.60F, 0.05F));
        this.adaptivePrediction = this.addSetting(new BoolSetting("adaptive_prediction", "Adaptive Prediction", "Use movement speed to predict forward bridge point", true));
        this.predictionGain = this.addSetting(new FloatSetting("prediction_gain", "Prediction Gain", "How much speed affects forward prediction", 0.65F, 0.00F, 1.60F, 0.05F));
        this.predictionGain.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return ScaffoldModule.this.adaptivePrediction.isEnabled();
            }
        });
        this.stabilityBias = this.addSetting(new FloatSetting("stability_bias", "Stability Bias", "Prefer continuous block chain instead of switching lanes", 0.85F, 0.00F, 2.00F, 0.05F));
        this.minMoveSpeed = this.addSetting(new FloatSetting("min_move_speed", "Min Move Speed", "Minimum horizontal speed before scaffold starts placing", 0.02F, 0.00F, 0.30F, 0.01F));
        this.bridgeProfile = this.addSetting(new EnumSetting<BridgeProfile>("bridge_profile", "Bridge Profile", "Behavior template for rhythm, look-ahead and aim discipline", BridgeProfile.class, BridgeProfile.BALANCED));

        this.aimMode = this.addSetting(new EnumSetting<ScaffoldUtils.AimMode>("aim_mode", "Aim Mode", "Legit moves camera, Silence restores local camera after place", ScaffoldUtils.AimMode.class, ScaffoldUtils.AimMode.LEGIT));
        this.rotationMode = this.addSetting(new EnumSetting<ScaffoldUtils.RotationMode>("rotation_mode", "Rotation Mode", "Rotation style while aiming to place target", ScaffoldUtils.RotationMode.class, ScaffoldUtils.RotationMode.SMOOTH));
        this.strictAim = this.addSetting(new BoolSetting("strict_aim", "Strict Aim", "In smooth mode, place only when current aim is close to target", true));
        this.strictAim.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return ScaffoldModule.this.rotationMode.get() == ScaffoldUtils.RotationMode.SMOOTH;
            }
        });
        this.aimTolerance = this.addSetting(new FloatSetting("aim_tolerance", "Aim Tolerance", "Max yaw/pitch error before strict smooth placement is allowed", 7.0F, 1.0F, 30.0F, 1.0F));
        this.aimTolerance.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return ScaffoldModule.this.rotationMode.get() == ScaffoldUtils.RotationMode.SMOOTH && ScaffoldModule.this.strictAim.isEnabled();
            }
        });
        this.turnSpeed = this.addSetting(new FloatSetting("turn_speed", "Turn Speed", "Max yaw/pitch delta per tick for smooth mode", 35.0F, 1.0F, 180.0F, 1.0F));
        this.turnSpeed.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return ScaffoldModule.this.rotationMode.get() == ScaffoldUtils.RotationMode.SMOOTH;
            }
        });
        this.raycastMode = this.addSetting(new EnumSetting<RaycastMode>("raycast_mode", "Raycast Mode", "Placement verification against current crosshair trace", RaycastMode.class, RaycastMode.NORMAL));

        this.keepBridgeY = this.addSetting(new BoolSetting("keep_y", "Keep Bridge Y", "Keep bridge placement on last grounded Y level", true));
        this.sameYMode = this.addSetting(new EnumSetting<SameYMode>("same_y_mode", "Same Y Mode", "Y-lock behavior while bridging", SameYMode.class, SameYMode.OFF));
        this.downwardsMode = this.addSetting(new EnumSetting<DownwardsMode>("downwards_mode", "Downwards", "How scaffold descends while sneaking", DownwardsMode.class, DownwardsMode.HOLD_SNEAK));
        this.downwardsMode.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return ScaffoldModule.this.sameYMode.get() == SameYMode.OFF;
            }
        });
        this.towerMode = this.addSetting(new EnumSetting<TowerMode>("tower_mode", "Tower Mode", "Vertical scaffold behavior while jumping", TowerMode.class, TowerMode.STABLE));
        this.sprintMode = this.addSetting(new EnumSetting<SprintMode>("sprint_mode", "Sprint Mode", "Sprint policy while rotating to place", SprintMode.class, SprintMode.LEGIT));

        this.safeEdgeSneak = this.addSetting(new BoolSetting("safe_edge_sneak", "Safe Edge Sneak", "Force sneak at unsafe edges to avoid falling", true));
        this.airSafeEdge = this.addSetting(new BoolSetting("air_safe_edge", "Air Safe Edge", "Allow edge protection while airborne", false));
        this.eagleMode = this.addSetting(new EnumSetting<EagleMode>("eagle_mode", "Eagle Mode", "Periodic edge sneak rhythm for speed-bridging", EagleMode.class, EagleMode.NORMAL));
        this.eagleBlocks = this.addSetting(new IntSetting("eagle_blocks", "Eagle Blocks", "Blocks to place before next eagle sneak cycle", 1, 0, 6, 1));
        this.eagleBlocks.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return ScaffoldModule.this.eagleMode.get() != EagleMode.OFF;
            }
        });
        this.eagleTicks = this.addSetting(new IntSetting("eagle_ticks", "Eagle Ticks", "How long each eagle sneak window lasts", 2, 0, 10, 1));
        this.eagleTicks.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return ScaffoldModule.this.eagleMode.get() != EagleMode.OFF;
            }
        });
        this.eagleEdgeDistance = this.addSetting(new FloatSetting("eagle_edge_distance", "Eagle Edge Distance", "Inset used for edge detection precision", 0.08F, 0.00F, 0.45F, 0.01F));
        this.eagleEdgeDistance.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return ScaffoldModule.this.eagleMode.get() != EagleMode.OFF || ScaffoldModule.this.safeEdgeSneak.isEnabled();
            }
        });

        general.add(this.preset);
        general.add(this.onlyWhenMoving);
        general.add(this.autoSwitch);
        general.add(this.swingHand);
        general.add(this.slotSelection);
        general.add(this.bridgeProfile);

        placement.add(this.placeMode);
        placement.add(this.extensionDistance);
        placement.add(this.omniDirectionalExpand);
        placement.add(this.searchRadius);
        placement.add(this.placeDelay);
        placement.add(this.placeDelayJitter);
        placement.add(this.extraPlaceAttempts);
        placement.add(this.lookAheadDistance);
        placement.add(this.adaptivePrediction);
        placement.add(this.predictionGain);
        placement.add(this.stabilityBias);
        placement.add(this.minMoveSpeed);

        rotation.add(this.aimMode);
        rotation.add(this.rotationMode);
        rotation.add(this.strictAim);
        rotation.add(this.aimTolerance);
        rotation.add(this.turnSpeed);
        rotation.add(this.raycastMode);

        movement.add(this.keepBridgeY);
        movement.add(this.sameYMode);
        movement.add(this.downwardsMode);
        movement.add(this.towerMode);
        movement.add(this.sprintMode);

        safety.add(this.safeEdgeSneak);
        safety.add(this.airSafeEdge);
        safety.add(this.eagleMode);
        safety.add(this.eagleBlocks);
        safety.add(this.eagleTicks);
        safety.add(this.eagleEdgeDistance);

        this.resetRuntimeState();
    }

    public void onEnable()
    {
        this.resetRuntimeState();
        this.lastPreset = null;
        this.attachEventLinks();
    }

    public void onDisable()
    {
        this.detachEventLinks();
        this.restoreSneakState();
        this.restoreSprintState();
        this.resetRuntimeState();
    }

    private void onPreTick()
    {
        this.ensurePresetApplied();
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.playerController == null || mc.currentScreen != null)
        {
            this.restoreSneakState();
            this.restoreSprintState();
            return;
        }

        EntityPlayerSP player = mc.thePlayer;
        ++this.ticksSinceLastPlacement;

        if (mc.playerController.isSpectatorMode() || player.capabilities.isFlying || player.isUsingItem())
        {
            this.restoreSneakState();
            this.restoreSprintState();
            return;
        }

        if (this.onlyWhenMoving.isEnabled() && !isMoving(player, this.minMoveSpeed.get().floatValue()))
        {
            this.restoreSneakState();
            this.restoreSprintState();
            return;
        }

        this.applyBridgePositioning(mc, player);
        this.applyModeAssist(mc, player);
        this.applyTowerMode(mc, player);

        boolean edgeUnsafe = this.isEdgeUnsafe(player, this.eagleEdgeDistance.get().floatValue());
        boolean placedBlock = false;

        try
        {
            if (this.placeCooldown > 0)
            {
                --this.placeCooldown;
                return;
            }

            InventoryPlayer inventory = player.inventory;
            int currentSlot = inventory.currentItem;
            int slotToUse = this.resolveSlot(inventory, currentSlot);

            if (slotToUse < 0)
            {
                this.applySprintPolicy(mc, player, null);
                return;
            }

            if (slotToUse != currentSlot)
            {
                if (!this.autoSwitch.isEnabled())
                {
                    this.applySprintPolicy(mc, player, null);
                    return;
                }

                inventory.currentItem = slotToUse;
                mc.playerController.updateController();
            }

            ItemStack held = inventory.getCurrentItem();

            if (!ScaffoldUtils.isValidScaffoldBlock(held))
            {
                this.applySprintPolicy(mc, player, null);
                return;
            }

            ProfileTuning tuning = this.resolveProfileTuning();
            int targetY = this.resolveTargetY(mc, player);
            BlockPos continuity = this.ticksSinceLastPlacement <= 20 ? this.lastPlacedPos : null;
            float lookAhead = this.lookAheadDistance.get().floatValue() * tuning.lookAheadMultiplier;
            ScaffoldUtils.PlacementTarget placement = ScaffoldUtils.findPlacementTarget(
                mc,
                player,
                this.placeMode.get(),
                targetY,
                lookAhead,
                this.searchRadius.get().intValue(),
                mc.playerController.getBlockReachDistance(),
                this.adaptivePrediction.isEnabled(),
                this.predictionGain.get().floatValue(),
                this.stabilityBias.get().floatValue(),
                this.extensionDistance.get().intValue(),
                this.omniDirectionalExpand.isEnabled(),
                continuity
            );

            this.applySprintPolicy(mc, player, placement);

            if (placement == null)
            {
                return;
            }

            placedBlock = this.tryPlaceTarget(mc, player, inventory, held, placement, tuning);
        }
        finally
        {
            this.applySneakPolicy(player, edgeUnsafe, placedBlock);
        }
    }

    private boolean tryPlaceTarget(Minecraft mc, EntityPlayerSP player, InventoryPlayer inventory, ItemStack held, ScaffoldUtils.PlacementTarget placement, ProfileTuning tuning)
    {
        ScaffoldUtils.AimMode aim = this.aimMode.get();
        ScaffoldUtils.RotationMode mode = this.rotationMode.get();

        if (aim == ScaffoldUtils.AimMode.SILENCE && mode == ScaffoldUtils.RotationMode.NONE)
        {
            mode = ScaffoldUtils.RotationMode.SNAP;
        }

        float tunedTurnSpeed = this.turnSpeed.get().floatValue() * tuning.turnSpeedMultiplier;
        ScaffoldUtils.RotationSnapshot snapshot = null;

        if (aim == ScaffoldUtils.AimMode.SILENCE)
        {
            snapshot = ScaffoldUtils.captureRotation(player);
        }

        ScaffoldUtils.applyRotation(player, placement.getYaw(), placement.getPitch(), mode, tunedTurnSpeed);

        try
        {
            if (!this.passesRaycastCheck(mc, placement))
            {
                return false;
            }

            boolean strict = this.strictAim.isEnabled() || tuning.forceStrictAim;
            float tolerance = this.aimTolerance.get().floatValue() * tuning.aimToleranceMultiplier;

            if (mode == ScaffoldUtils.RotationMode.SMOOTH && strict && !ScaffoldUtils.isAimAligned(player, placement.getYaw(), placement.getPitch(), tolerance))
            {
                return false;
            }

            int attempts = 1 + Math.max(0, this.extraPlaceAttempts.get().intValue());

            for (int i = 0; i < attempts; ++i)
            {
                int previousCount = held.stackSize;
                boolean placed = mc.playerController.onPlayerRightClick(player, mc.theWorld, held, placement.getSupportPos(), placement.getSide(), placement.getHitVec());

                if (placed)
                {
                    this.onPlacementSucceeded(mc, player, inventory, held, previousCount, placement, tuning);
                    return true;
                }
            }
        }
        finally
        {
            if (snapshot != null)
            {
                ScaffoldUtils.restoreRotation(player, snapshot);
            }
        }

        return false;
    }

    private void onPlacementSucceeded(Minecraft mc, EntityPlayerSP player, InventoryPlayer inventory, ItemStack held, int previousCount, ScaffoldUtils.PlacementTarget placement, ProfileTuning tuning)
    {
        if (this.swingHand.isEnabled())
        {
            player.swingItem();
        }

        if (held.stackSize == 0)
        {
            inventory.mainInventory[inventory.currentItem] = null;
        }
        else if (held.stackSize != previousCount || mc.playerController.isInCreativeMode())
        {
            mc.entityRenderer.itemRenderer.resetEquippedProgress();
        }

        this.lastPlacedPos = placement.getPlacePos();
        this.ticksSinceLastPlacement = 0;
        ++this.blocksPlacedWithoutEagle;
        this.placeCooldown = this.computePlacementCooldown(tuning);
    }

    private int resolveTargetY(Minecraft mc, EntityPlayerSP player)
    {
        if (this.isDownwardsActive(mc))
        {
            return MathHelper.floor_double(player.posY - 2.0D);
        }

        if ((this.sameYMode.get() != SameYMode.OFF || this.keepBridgeY.isEnabled()) && this.lockedBridgeY != Integer.MIN_VALUE)
        {
            return this.lockedBridgeY;
        }

        return MathHelper.floor_double(player.posY - 1.0D);
    }

    private void applyBridgePositioning(Minecraft mc, EntityPlayerSP player)
    {
        SameYMode mode = this.sameYMode.get();

        if (player.onGround && (mode != SameYMode.OFF || this.keepBridgeY.isEnabled()))
        {
            this.lockedBridgeY = MathHelper.floor_double(player.posY - 1.0D);
        }
        else if (mode == SameYMode.OFF && !this.keepBridgeY.isEnabled())
        {
            this.lockedBridgeY = Integer.MIN_VALUE;
        }

        if (mode == SameYMode.AUTO_JUMP && player.onGround && isMoving(player, this.minMoveSpeed.get().floatValue()) && !this.isDownwardsActive(mc))
        {
            int jumpKey = mc.gameSettings.keyBindJump.getKeyCode();

            if (!this.isPhysicalKeyDown(jumpKey))
            {
                player.jump();
            }
        }
    }

    private void applyModeAssist(Minecraft mc, EntityPlayerSP player)
    {
        if (player == null)
        {
            return;
        }

        if (this.modeStrafeDamping < 0.9999F && isMoving(player, 0.005F))
        {
            double damping = Math.max(0.85D, Math.min(1.0D, (double)this.modeStrafeDamping));
            player.motionX *= damping;
            player.motionZ *= damping;
        }

        if (this.modeAutoJumpAssist && player.onGround && isMoving(player, this.minMoveSpeed.get().floatValue()) && !this.isDownwardsActive(mc))
        {
            int jumpKey = mc.gameSettings.keyBindJump.getKeyCode();

            if (!this.isPhysicalKeyDown(jumpKey))
            {
                player.jump();
            }
        }
    }

    private boolean isDownwardsActive(Minecraft mc)
    {
        return this.sameYMode.get() == SameYMode.OFF && this.downwardsMode.get() == DownwardsMode.HOLD_SNEAK && this.isPhysicalKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
    }

    private void applyTowerMode(Minecraft mc, EntityPlayerSP player)
    {
        TowerMode mode = this.towerMode.get();

        if (mode == TowerMode.OFF)
        {
            return;
        }

        if (!this.isPhysicalKeyDown(mc.gameSettings.keyBindJump.getKeyCode()))
        {
            return;
        }

        if (Math.abs(player.moveForward) > 0.01F || Math.abs(player.moveStrafing) > 0.01F)
        {
            return;
        }

        if (mode == TowerMode.VANILLA)
        {
            if (player.onGround)
            {
                player.jump();
            }

            return;
        }

        if (player.onGround)
        {
            player.motionY = 0.42D;
        }
        else if (player.motionY < 0.1D && player.motionY > -0.08D)
        {
            player.motionY = -0.0784000015258789D;
        }

        player.motionX *= 0.98D;
        player.motionZ *= 0.98D;
    }

    private boolean passesRaycastCheck(Minecraft mc, ScaffoldUtils.PlacementTarget placement)
    {
        RaycastMode mode = this.modeStrictRaycast ? RaycastMode.STRICT : this.raycastMode.get();

        if (mode == RaycastMode.OFF)
        {
            return true;
        }

        return ModuleKit.raycastMatchesPlacement(mc, placement.getSupportPos(), placement.getSide(), mode == RaycastMode.STRICT);
    }

    private void applySprintPolicy(Minecraft mc, EntityPlayerSP player, ScaffoldUtils.PlacementTarget placement)
    {
        SprintMode mode = this.sprintMode.get();

        if (mode == SprintMode.KEEP)
        {
            this.restoreSprintState();
            return;
        }

        boolean suppress = mode == SprintMode.DISABLED;

        if (!suppress && mode == SprintMode.LEGIT && placement != null)
        {
            float yawDelta = Math.abs(RotationUtils.yawDelta(player.rotationYaw, placement.getYaw()));
            suppress = yawDelta > 82.0F;
        }

        int sprintKey = mc.gameSettings.keyBindSprint.getKeyCode();

        if (suppress)
        {
            if (sprintKey > 0)
            {
                KeyBinding.setKeyBindState(sprintKey, false);
            }

            player.setSprinting(false);
            this.suppressedSprint = true;
        }
        else
        {
            this.restoreSprintState();
        }
    }

    private void applySneakPolicy(EntityPlayerSP player, boolean edgeUnsafe, boolean placedBlock)
    {
        boolean edgeSneak = this.safeEdgeSneak.isEnabled() && edgeUnsafe && (this.airSafeEdge.isEnabled() || player.onGround);
        boolean eagleSneak = this.consumeEagleSneak(edgeUnsafe, placedBlock);
        boolean shouldSneak = edgeSneak || eagleSneak;
        boolean silentSneak = eagleSneak && !edgeSneak && this.eagleMode.get() == EagleMode.SILENT;
        this.setSneakState(player, shouldSneak, silentSneak);
    }

    private boolean consumeEagleSneak(boolean edgeUnsafe, boolean placedBlock)
    {
        if (this.eagleMode.get() == EagleMode.OFF)
        {
            this.blocksPlacedWithoutEagle = 0;
            this.eagleSneakTicks = 0;
            return false;
        }

        if (placedBlock)
        {
            ++this.blocksPlacedWithoutEagle;
        }

        int threshold = Math.max(0, this.eagleBlocks.get().intValue());

        if (edgeUnsafe && this.blocksPlacedWithoutEagle >= threshold)
        {
            int ticks = Math.max(0, this.eagleTicks.get().intValue());

            if (ticks > 0)
            {
                this.eagleSneakTicks = Math.max(this.eagleSneakTicks, ticks);
            }

            this.blocksPlacedWithoutEagle = 0;
        }

        if (this.eagleSneakTicks <= 0)
        {
            return false;
        }

        --this.eagleSneakTicks;
        return true;
    }

    private void setSneakState(EntityPlayerSP player, boolean shouldSneak, boolean silentMode)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.gameSettings == null)
        {
            return;
        }

        int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
        boolean physical = this.isPhysicalKeyDown(sneakKey);

        if (silentMode && shouldSneak && !physical)
        {
            if (this.forcedKeySneak && sneakKey > 0)
            {
                KeyBinding.setKeyBindState(sneakKey, false);
                this.forcedKeySneak = false;
            }

            if (player.movementInput != null)
            {
                player.movementInput.sneak = true;
            }

            player.setSneaking(true);
            this.forcedSilentSneak = true;
            return;
        }

        if (sneakKey > 0)
        {
            KeyBinding.setKeyBindState(sneakKey, shouldSneak || physical);
            this.forcedKeySneak = shouldSneak;
        }

        if (this.forcedSilentSneak)
        {
            if (player.movementInput != null)
            {
                player.movementInput.sneak = physical;
            }

            if (!physical)
            {
                player.setSneaking(false);
            }
        }

        this.forcedSilentSneak = false;
    }

    private void restoreSneakState()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.gameSettings == null)
        {
            this.forcedKeySneak = false;
            this.forcedSilentSneak = false;
            return;
        }

        int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
        boolean physical = this.isPhysicalKeyDown(sneakKey);

        if (this.forcedKeySneak && sneakKey > 0)
        {
            KeyBinding.setKeyBindState(sneakKey, physical);
        }

        if (this.forcedSilentSneak && mc.thePlayer != null)
        {
            if (mc.thePlayer.movementInput != null)
            {
                mc.thePlayer.movementInput.sneak = physical;
            }

            if (!physical)
            {
                mc.thePlayer.setSneaking(false);
            }
        }

        this.forcedKeySneak = false;
        this.forcedSilentSneak = false;
    }

    private void restoreSprintState()
    {
        if (!this.suppressedSprint)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (mc != null && mc.gameSettings != null)
        {
            int sprintKey = mc.gameSettings.keyBindSprint.getKeyCode();

            if (sprintKey > 0)
            {
                KeyBinding.setKeyBindState(sprintKey, this.isPhysicalKeyDown(sprintKey));
            }
        }

        this.suppressedSprint = false;
    }

    private boolean isEdgeUnsafe(EntityPlayerSP player, float inset)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.theWorld == null || player == null)
        {
            return false;
        }

        AxisAlignedBB bb = player.getEntityBoundingBox();

        if (bb == null)
        {
            return false;
        }

        double clampedInset = Math.max(0.0D, Math.min(0.45D, inset));
        double sampleY = bb.minY - 0.60D;
        double minX = bb.minX + clampedInset;
        double maxX = bb.maxX - clampedInset;
        double minZ = bb.minZ + clampedInset;
        double maxZ = bb.maxZ - clampedInset;

        if (minX > maxX)
        {
            double middle = (bb.minX + bb.maxX) * 0.5D;
            minX = middle;
            maxX = middle;
        }

        if (minZ > maxZ)
        {
            double middle = (bb.minZ + bb.maxZ) * 0.5D;
            minZ = middle;
            maxZ = middle;
        }

        return this.isAirAt(minX, sampleY, minZ)
            || this.isAirAt(minX, sampleY, maxZ)
            || this.isAirAt(maxX, sampleY, minZ)
            || this.isAirAt(maxX, sampleY, maxZ)
            || this.isAirAt(player.posX, sampleY, player.posZ);
    }

    private boolean isAirAt(double x, double y, double z)
    {
        Minecraft mc = Minecraft.getMinecraft();
        return mc != null && mc.theWorld != null && mc.theWorld.isAirBlock(new BlockPos(x, y, z));
    }

    private int resolveSlot(InventoryPlayer inventory, int preferredSlot)
    {
        if (inventory == null)
        {
            return -1;
        }

        if (preferredSlot >= 0 && preferredSlot < 9 && ScaffoldUtils.isValidScaffoldBlock(inventory.mainInventory[preferredSlot]))
        {
            return preferredSlot;
        }

        if (this.slotSelection.get() == SlotSelectionMode.FIRST_AVAILABLE)
        {
            return ScaffoldUtils.findFirstBlockSlot(inventory, preferredSlot);
        }

        return ScaffoldUtils.findBestBlockSlot(inventory, preferredSlot);
    }

    private int computePlacementCooldown(ProfileTuning tuning)
    {
        int base = this.placeDelay.get().intValue();
        int jitterBound = Math.max(0, this.placeDelayJitter.get().intValue());
        int jitter = jitterBound <= 0 ? 0 : this.random.nextInt(jitterBound + 1);
        int cooldown = base + jitter + tuning.delayOffset;
        return Math.max(0, cooldown);
    }

    private void ensurePresetApplied()
    {
        ScaffoldPreset current = this.preset.get();

        if (current == this.lastPreset)
        {
            return;
        }

        if (current == ScaffoldPreset.CUSTOM)
        {
            this.modeStrafeDamping = 1.0F;
            this.modeAutoJumpAssist = false;
            this.modeStrictRaycast = false;
            this.lastPreset = current;
            return;
        }

        this.applyPreset(current);
        this.lastPreset = current;
    }

    private void applyPreset(ScaffoldPreset preset)
    {
        if (preset == null)
        {
            return;
        }

        this.applyPresetBaseline();

        switch (preset)
        {
            case NORMAL:
                break;
            case REWINDSIDE:
                this.placeMode.set(ScaffoldUtils.PlaceMode.FORWARD);
                this.sameYMode.set(SameYMode.LOCK);
                this.keepBridgeY.set(true);
                this.rotationMode.set(ScaffoldUtils.RotationMode.SNAP);
                this.aimMode.set(ScaffoldUtils.AimMode.SILENCE);
                this.bridgeProfile.set(BridgeProfile.STABLE);
                this.raycastMode.set(RaycastMode.NORMAL);
                this.towerMode.set(TowerMode.OFF);
                this.sprintMode.set(SprintMode.LEGIT);
                this.safeEdgeSneak.setEnabled(true);
                this.eagleMode.set(EagleMode.OFF);
                this.turnSpeed.set(Float.valueOf(180.0F));
                this.modeStrafeDamping = 0.992F;
                break;
            case EXPAND:
                this.placeMode.set(ScaffoldUtils.PlaceMode.EXPAND);
                this.omniDirectionalExpand.setEnabled(true);
                this.extensionDistance.set(Integer.valueOf(2));
                this.searchRadius.set(Integer.valueOf(2));
                this.rotationMode.set(ScaffoldUtils.RotationMode.SMOOTH);
                this.aimMode.set(ScaffoldUtils.AimMode.LEGIT);
                this.bridgeProfile.set(BridgeProfile.RAPID);
                this.raycastMode.set(RaycastMode.NORMAL);
                this.eagleMode.set(EagleMode.SILENT);
                this.eagleBlocks.set(Integer.valueOf(0));
                this.eagleTicks.set(Integer.valueOf(0));
                this.safeEdgeSneak.setEnabled(true);
                this.airSafeEdge.setEnabled(false);
                this.sameYMode.set(SameYMode.OFF);
                this.keepBridgeY.set(true);
                this.placeDelay.set(Integer.valueOf(0));
                this.placeDelayJitter.set(Integer.valueOf(0));
                this.extraPlaceAttempts.set(Integer.valueOf(1));
                break;
            case TELLY:
                this.placeMode.set(ScaffoldUtils.PlaceMode.FORWARD);
                this.omniDirectionalExpand.setEnabled(false);
                this.extensionDistance.set(Integer.valueOf(1));
                this.searchRadius.set(Integer.valueOf(1));
                this.sameYMode.set(SameYMode.AUTO_JUMP);
                this.downwardsMode.set(DownwardsMode.OFF);
                this.towerMode.set(TowerMode.OFF);
                this.bridgeProfile.set(BridgeProfile.RAPID);
                this.rotationMode.set(ScaffoldUtils.RotationMode.SNAP);
                this.aimMode.set(ScaffoldUtils.AimMode.SILENCE);
                this.raycastMode.set(RaycastMode.STRICT);
                this.placeDelay.set(Integer.valueOf(0));
                this.extraPlaceAttempts.set(Integer.valueOf(1));
                this.keepBridgeY.set(false);
                this.safeEdgeSneak.setEnabled(false);
                this.eagleMode.set(EagleMode.OFF);
                this.modeAutoJumpAssist = true;
                break;
            case GODBRIDGE:
                this.placeMode.set(ScaffoldUtils.PlaceMode.UNDER);
                this.omniDirectionalExpand.setEnabled(false);
                this.extensionDistance.set(Integer.valueOf(1));
                this.searchRadius.set(Integer.valueOf(1));
                this.sameYMode.set(SameYMode.AUTO_JUMP);
                this.downwardsMode.set(DownwardsMode.OFF);
                this.towerMode.set(TowerMode.OFF);
                this.bridgeProfile.set(BridgeProfile.RAPID);
                this.rotationMode.set(ScaffoldUtils.RotationMode.SNAP);
                this.aimMode.set(ScaffoldUtils.AimMode.SILENCE);
                this.raycastMode.set(RaycastMode.OFF);
                this.placeDelay.set(Integer.valueOf(0));
                this.placeDelayJitter.set(Integer.valueOf(0));
                this.extraPlaceAttempts.set(Integer.valueOf(2));
                this.keepBridgeY.set(false);
                this.minMoveSpeed.set(Float.valueOf(0.12F));
                this.safeEdgeSneak.setEnabled(false);
                this.eagleMode.set(EagleMode.OFF);
                this.modeAutoJumpAssist = true;
                break;
            case BREEZILY:
                this.placeMode.set(ScaffoldUtils.PlaceMode.FORWARD);
                this.omniDirectionalExpand.setEnabled(false);
                this.extensionDistance.set(Integer.valueOf(1));
                this.searchRadius.set(Integer.valueOf(1));
                this.rotationMode.set(ScaffoldUtils.RotationMode.SMOOTH);
                this.aimMode.set(ScaffoldUtils.AimMode.LEGIT);
                this.bridgeProfile.set(BridgeProfile.PRECISION);
                this.aimTolerance.set(Float.valueOf(5.0F));
                this.turnSpeed.set(Float.valueOf(24.0F));
                this.eagleMode.set(EagleMode.NORMAL);
                this.eagleBlocks.set(Integer.valueOf(1));
                this.eagleTicks.set(Integer.valueOf(2));
                this.safeEdgeSneak.setEnabled(true);
                this.keepBridgeY.set(true);
                this.sameYMode.set(SameYMode.LOCK);
                break;
            case SNAP:
                this.placeMode.set(ScaffoldUtils.PlaceMode.SMART);
                this.omniDirectionalExpand.setEnabled(false);
                this.extensionDistance.set(Integer.valueOf(1));
                this.searchRadius.set(Integer.valueOf(1));
                this.rotationMode.set(ScaffoldUtils.RotationMode.SNAP);
                this.aimMode.set(ScaffoldUtils.AimMode.SILENCE);
                this.raycastMode.set(RaycastMode.NORMAL);
                this.bridgeProfile.set(BridgeProfile.STABLE);
                this.placeDelay.set(Integer.valueOf(1));
                this.placeDelayJitter.set(Integer.valueOf(0));
                this.eagleMode.set(EagleMode.OFF);
                this.safeEdgeSneak.setEnabled(false);
                this.keepBridgeY.set(true);
                break;
            case WATCHDOG:
                this.placeMode.set(ScaffoldUtils.PlaceMode.SMART);
                this.rotationMode.set(ScaffoldUtils.RotationMode.SMOOTH);
                this.aimMode.set(ScaffoldUtils.AimMode.LEGIT);
                this.bridgeProfile.set(BridgeProfile.STABLE);
                this.raycastMode.set(RaycastMode.STRICT);
                this.sameYMode.set(SameYMode.OFF);
                this.keepBridgeY.set(true);
                this.towerMode.set(TowerMode.STABLE);
                this.sprintMode.set(SprintMode.LEGIT);
                this.safeEdgeSneak.setEnabled(true);
                this.airSafeEdge.setEnabled(false);
                this.eagleMode.set(EagleMode.OFF);
                this.placeDelay.set(Integer.valueOf(1));
                this.placeDelayJitter.set(Integer.valueOf(0));
                this.extraPlaceAttempts.set(Integer.valueOf(1));
                this.aimTolerance.set(Float.valueOf(6.0F));
                this.turnSpeed.set(Float.valueOf(30.0F));
                this.modeStrafeDamping = 0.985F;
                this.modeStrictRaycast = true;
                break;
            case EAGLE:
                this.placeMode.set(ScaffoldUtils.PlaceMode.UNDER);
                this.omniDirectionalExpand.setEnabled(false);
                this.extensionDistance.set(Integer.valueOf(1));
                this.searchRadius.set(Integer.valueOf(1));
                this.rotationMode.set(ScaffoldUtils.RotationMode.SMOOTH);
                this.aimMode.set(ScaffoldUtils.AimMode.LEGIT);
                this.bridgeProfile.set(BridgeProfile.STABLE);
                this.eagleMode.set(EagleMode.NORMAL);
                this.eagleBlocks.set(Integer.valueOf(0));
                this.eagleTicks.set(Integer.valueOf(3));
                this.safeEdgeSneak.setEnabled(true);
                this.keepBridgeY.set(true);
                this.sameYMode.set(SameYMode.LOCK);
                this.raycastMode.set(RaycastMode.NORMAL);
                this.placeDelay.set(Integer.valueOf(0));
                this.placeDelayJitter.set(Integer.valueOf(0));
                break;
            case CUSTOM:
                break;
            default:
                break;
        }
    }

    private void applyPresetBaseline()
    {
        this.modeStrafeDamping = 1.0F;
        this.modeAutoJumpAssist = false;
        this.modeStrictRaycast = false;
        this.placeMode.set(ScaffoldUtils.PlaceMode.SMART);
        this.omniDirectionalExpand.setEnabled(false);
        this.extensionDistance.set(Integer.valueOf(1));
        this.searchRadius.set(Integer.valueOf(1));
        this.rotationMode.set(ScaffoldUtils.RotationMode.SMOOTH);
        this.aimMode.set(ScaffoldUtils.AimMode.LEGIT);
        this.bridgeProfile.set(BridgeProfile.BALANCED);
        this.raycastMode.set(RaycastMode.NORMAL);
        this.sameYMode.set(SameYMode.OFF);
        this.downwardsMode.set(DownwardsMode.HOLD_SNEAK);
        this.keepBridgeY.set(true);
        this.towerMode.set(TowerMode.STABLE);
        this.sprintMode.set(SprintMode.LEGIT);
        this.safeEdgeSneak.setEnabled(true);
        this.airSafeEdge.setEnabled(false);
        this.eagleMode.set(EagleMode.NORMAL);
        this.eagleBlocks.set(Integer.valueOf(1));
        this.eagleTicks.set(Integer.valueOf(2));
        this.placeDelay.set(Integer.valueOf(0));
        this.placeDelayJitter.set(Integer.valueOf(1));
        this.lookAheadDistance.set(Float.valueOf(0.90F));
        this.minMoveSpeed.set(Float.valueOf(0.02F));
        this.extraPlaceAttempts.set(Integer.valueOf(0));
    }

    private ProfileTuning resolveProfileTuning()
    {
        BridgeProfile profile = this.bridgeProfile.get();

        if (profile == BridgeProfile.STABLE)
        {
            return ProfileTuning.STABLE;
        }
        else if (profile == BridgeProfile.RAPID)
        {
            return ProfileTuning.RAPID;
        }
        else if (profile == BridgeProfile.PRECISION)
        {
            return ProfileTuning.PRECISION;
        }

        return ProfileTuning.BALANCED;
    }

    private void resetRuntimeState()
    {
        this.placeCooldown = 0;
        this.lockedBridgeY = Integer.MIN_VALUE;
        this.lastPlacedPos = null;
        this.ticksSinceLastPlacement = 200;
        this.blocksPlacedWithoutEagle = 0;
        this.eagleSneakTicks = 0;
        this.forcedKeySneak = false;
        this.forcedSilentSneak = false;
        this.suppressedSprint = false;
        this.modeStrafeDamping = 1.0F;
        this.modeAutoJumpAssist = false;
        this.modeStrictRaycast = false;
    }

    private boolean isPhysicalKeyDown(int keyCode)
    {
        return keyCode > 0 && keyCode < 256 && Keyboard.isKeyDown(keyCode);
    }

    private static boolean isMoving(EntityPlayerSP player, float threshold)
    {
        if (player == null)
        {
            return false;
        }

        if (Math.abs(player.moveForward) > 0.01F || Math.abs(player.moveStrafing) > 0.01F)
        {
            return true;
        }

        double speedSq = player.motionX * player.motionX + player.motionZ * player.motionZ;
        double min = Math.max(0.0D, threshold);
        return speedSq > min * min;
    }

    public static enum ScaffoldPreset
    {
        CUSTOM,
        NORMAL,
        REWINDSIDE,
        EXPAND,
        TELLY,
        GODBRIDGE,
        BREEZILY,
        WATCHDOG,
        SNAP,
        EAGLE
    }

    private static final class ProfileTuning
    {
        private static final ProfileTuning BALANCED = new ProfileTuning(1.00F, 0, 1.00F, 1.00F, false);
        private static final ProfileTuning STABLE = new ProfileTuning(0.90F, 1, 0.80F, 0.92F, false);
        private static final ProfileTuning RAPID = new ProfileTuning(1.12F, -1, 1.20F, 1.10F, false);
        private static final ProfileTuning PRECISION = new ProfileTuning(0.96F, 0, 0.65F, 0.85F, true);
        private final float lookAheadMultiplier;
        private final int delayOffset;
        private final float aimToleranceMultiplier;
        private final float turnSpeedMultiplier;
        private final boolean forceStrictAim;

        private ProfileTuning(float lookAheadMultiplier, int delayOffset, float aimToleranceMultiplier, float turnSpeedMultiplier, boolean forceStrictAim)
        {
            this.lookAheadMultiplier = lookAheadMultiplier;
            this.delayOffset = delayOffset;
            this.aimToleranceMultiplier = aimToleranceMultiplier;
            this.turnSpeedMultiplier = turnSpeedMultiplier;
            this.forceStrictAim = forceStrictAim;
        }
    }

    public static enum BridgeProfile
    {
        BALANCED,
        STABLE,
        RAPID,
        PRECISION
    }

    public static enum RaycastMode
    {
        OFF,
        NORMAL,
        STRICT
    }

    public static enum SlotSelectionMode
    {
        BEST_COUNT,
        FIRST_AVAILABLE
    }

    public static enum SameYMode
    {
        OFF,
        LOCK,
        AUTO_JUMP
    }

    public static enum DownwardsMode
    {
        OFF,
        HOLD_SNEAK
    }

    public static enum TowerMode
    {
        OFF,
        VANILLA,
        STABLE
    }

    public static enum SprintMode
    {
        KEEP,
        LEGIT,
        DISABLED
    }

    public static enum EagleMode
    {
        OFF,
        NORMAL,
        SILENT
    }
}
