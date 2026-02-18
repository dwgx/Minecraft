package client.module.impl.movement;

import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.EnumSetting;
import client.setting.FloatSetting;
import client.setting.IntSetting;
import client.setting.SettingGroup;
import dwgx.scaffold.ScaffoldUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public final class ScaffoldModule extends Module
{
    private final BoolSetting onlyWhenMoving;
    private final BoolSetting autoSwitch;
    private final BoolSetting swingHand;
    private final BoolSetting keepBridgeY;
    private final BoolSetting adaptivePrediction;
    private final BoolSetting strictAim;
    private final EnumSetting<ScaffoldUtils.PlaceMode> placeMode;
    private final EnumSetting<ScaffoldUtils.AimMode> aimMode;
    private final EnumSetting<ScaffoldUtils.RotationMode> rotationMode;
    private final IntSetting searchRadius;
    private final IntSetting placeDelay;
    private final IntSetting extensionDistance;
    private final FloatSetting lookAheadDistance;
    private final FloatSetting predictionGain;
    private final FloatSetting stabilityBias;
    private final FloatSetting minMoveSpeed;
    private final FloatSetting aimTolerance;
    private final FloatSetting turnSpeed;
    private int placeCooldown;
    private int lockedBridgeY;
    private BlockPos lastPlacedPos;
    private int ticksSinceLastPlace;

    public ScaffoldModule()
    {
        super("scaffold", "Scaffold", Category.MOVEMENT);
        SettingGroup general = this.addGroup("general", "General");
        SettingGroup tuning = this.addGroup("tuning", "Tuning");
        this.onlyWhenMoving = this.addSetting(new BoolSetting("only_moving", "Only When Moving", "Place blocks only while movement input is active", true));
        this.autoSwitch = this.addSetting(new BoolSetting("auto_switch", "Auto Switch", "Switch to a valid hotbar block slot automatically", true));
        this.swingHand = this.addSetting(new BoolSetting("swing_hand", "Swing Hand", "Play hand swing animation after placement", true));
        this.keepBridgeY = this.addSetting(new BoolSetting("keep_y", "Keep Bridge Y", "Keep bridge placement on last grounded Y level", true));
        this.adaptivePrediction = this.addSetting(new BoolSetting("adaptive_prediction", "Adaptive Prediction", "Use movement speed to predict forward bridge point", true));
        this.strictAim = this.addSetting(new BoolSetting("strict_aim", "Strict Aim", "In smooth mode, place only when current aim is close to target", true));
        this.strictAim.visibleWhen(new client.setting.Visibility()
        {
            public boolean isVisible()
            {
                return ScaffoldModule.this.rotationMode != null && ScaffoldModule.this.rotationMode.get() == ScaffoldUtils.RotationMode.SMOOTH;
            }
        });
        this.placeMode = this.addSetting(new EnumSetting<ScaffoldUtils.PlaceMode>("place_mode", "Place Mode", "Bridge preference: smart / under / forward", ScaffoldUtils.PlaceMode.class, ScaffoldUtils.PlaceMode.SMART));
        this.aimMode = this.addSetting(new EnumSetting<ScaffoldUtils.AimMode>("aim_mode", "Aim Mode", "Legit moves camera, Silence restores local camera after place", ScaffoldUtils.AimMode.class, ScaffoldUtils.AimMode.LEGIT));
        this.rotationMode = this.addSetting(new EnumSetting<ScaffoldUtils.RotationMode>("rotation_mode", "Rotation Mode", "Rotation style while aiming to place target", ScaffoldUtils.RotationMode.class, ScaffoldUtils.RotationMode.SMOOTH));
        this.searchRadius = this.addSetting(new IntSetting("search_radius", "Search Radius", "Extra horizontal scan distance around target point", 1, 0, 4, 1));
        this.placeDelay = this.addSetting(new IntSetting("place_delay", "Place Delay", "Tick interval between placements", 0, 0, 6, 1));
        this.extensionDistance = this.addSetting(new IntSetting("extension", "Extension", "Extra forward scan steps for longer bridge reach", 1, 0, 4, 1));
        this.lookAheadDistance = this.addSetting(new FloatSetting("look_ahead_distance", "Look Ahead", "Base forward bridging distance in blocks", 0.80F, 0.35F, 2.40F, 0.05F));
        this.predictionGain = this.addSetting(new FloatSetting("prediction_gain", "Prediction Gain", "How much speed affects forward prediction", 0.65F, 0.00F, 1.40F, 0.05F));
        this.predictionGain.visibleWhen(new client.setting.Visibility()
        {
            public boolean isVisible()
            {
                return ScaffoldModule.this.adaptivePrediction.isEnabled();
            }
        });
        this.stabilityBias = this.addSetting(new FloatSetting("stability_bias", "Stability Bias", "Prefer continuous block chain instead of switching lanes", 0.85F, 0.00F, 2.00F, 0.05F));
        this.minMoveSpeed = this.addSetting(new FloatSetting("min_move_speed", "Min Move Speed", "Minimum horizontal speed before scaffold starts placing", 0.02F, 0.00F, 0.25F, 0.01F));
        this.aimTolerance = this.addSetting(new FloatSetting("aim_tolerance", "Aim Tolerance", "Max yaw/pitch error before strict smooth placement is allowed", 7.0F, 1.0F, 30.0F, 1.0F));
        this.aimTolerance.visibleWhen(new client.setting.Visibility()
        {
            public boolean isVisible()
            {
                return ScaffoldModule.this.rotationMode != null && ScaffoldModule.this.rotationMode.get() == ScaffoldUtils.RotationMode.SMOOTH && ScaffoldModule.this.strictAim.isEnabled();
            }
        });
        this.turnSpeed = this.addSetting(new FloatSetting("turn_speed", "Turn Speed", "Max yaw/pitch delta per tick for smooth mode", 35.0F, 1.0F, 180.0F, 1.0F));
        this.turnSpeed.visibleWhen(new client.setting.Visibility()
        {
            public boolean isVisible()
            {
                return ScaffoldModule.this.rotationMode.get() == ScaffoldUtils.RotationMode.SMOOTH;
            }
        });
        general.add(this.onlyWhenMoving);
        general.add(this.autoSwitch);
        general.add(this.swingHand);
        general.add(this.keepBridgeY);
        general.add(this.adaptivePrediction);
        general.add(this.strictAim);
        general.add(this.placeMode);
        general.add(this.aimMode);
        general.add(this.rotationMode);
        tuning.add(this.searchRadius);
        tuning.add(this.placeDelay);
        tuning.add(this.extensionDistance);
        tuning.add(this.lookAheadDistance);
        tuning.add(this.predictionGain);
        tuning.add(this.stabilityBias);
        tuning.add(this.minMoveSpeed);
        tuning.add(this.aimTolerance);
        tuning.add(this.turnSpeed);
        this.placeCooldown = 0;
        this.lockedBridgeY = Integer.MIN_VALUE;
        this.lastPlacedPos = null;
        this.ticksSinceLastPlace = 0;
    }

    public void onEnable()
    {
        this.placeCooldown = 0;
        this.lockedBridgeY = Integer.MIN_VALUE;
        this.lastPlacedPos = null;
        this.ticksSinceLastPlace = 0;
    }

    public void onDisable()
    {
        this.placeCooldown = 0;
        this.lockedBridgeY = Integer.MIN_VALUE;
        this.lastPlacedPos = null;
        this.ticksSinceLastPlace = 0;
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.playerController == null || mc.currentScreen != null)
        {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;
        ++this.ticksSinceLastPlace;

        if (mc.playerController.isSpectatorMode() || player.capabilities.isFlying || player.isUsingItem())
        {
            return;
        }

        if (this.onlyWhenMoving.isEnabled() && !isMoving(player, this.minMoveSpeed.get().floatValue()))
        {
            return;
        }

        if (this.keepBridgeY.isEnabled())
        {
            if (player.onGround)
            {
                this.lockedBridgeY = MathHelper.floor_double(player.posY - 1.0D);
            }
        }
        else
        {
            this.lockedBridgeY = Integer.MIN_VALUE;
        }

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
            return;
        }

        if (slotToUse != currentSlot)
        {
            if (!this.autoSwitch.isEnabled())
            {
                return;
            }

            inventory.currentItem = slotToUse;
            mc.playerController.updateController();
        }

        ItemStack held = inventory.getCurrentItem();

        if (!ScaffoldUtils.isValidScaffoldBlock(held))
        {
            return;
        }

        int targetY = this.keepBridgeY.isEnabled() && this.lockedBridgeY != Integer.MIN_VALUE ? this.lockedBridgeY : MathHelper.floor_double(player.posY - 1.0D);
        BlockPos continuity = this.ticksSinceLastPlace <= 18 ? this.lastPlacedPos : null;
        ScaffoldUtils.PlacementTarget placement = ScaffoldUtils.findPlacementTarget(
            mc,
            player,
            this.placeMode.get(),
            targetY,
            this.lookAheadDistance.get().floatValue(),
            this.searchRadius.get().intValue(),
            mc.playerController.getBlockReachDistance(),
            this.adaptivePrediction.isEnabled(),
            this.predictionGain.get().floatValue(),
            this.stabilityBias.get().floatValue(),
            this.extensionDistance.get().intValue(),
            continuity
        );

        if (placement == null)
        {
            return;
        }

        ScaffoldUtils.AimMode aim = this.aimMode.get();
        ScaffoldUtils.RotationMode rotationMode = this.rotationMode.get();
        ScaffoldUtils.RotationSnapshot snapshot = null;

        if (aim == ScaffoldUtils.AimMode.SILENCE)
        {
            snapshot = ScaffoldUtils.captureRotation(player);
            rotationMode = ScaffoldUtils.RotationMode.SNAP;
        }

        ScaffoldUtils.applyRotation(player, placement.getYaw(), placement.getPitch(), rotationMode, this.turnSpeed.get().floatValue());

        if (rotationMode == ScaffoldUtils.RotationMode.SMOOTH && this.strictAim.isEnabled() && !ScaffoldUtils.isAimAligned(player, placement.getYaw(), placement.getPitch(), this.aimTolerance.get().floatValue()))
        {
            return;
        }

        int previousCount = held.stackSize;
        boolean placed;

        try
        {
            placed = mc.playerController.onPlayerRightClick(player, mc.theWorld, held, placement.getSupportPos(), placement.getSide(), placement.getHitVec());
        }
        finally
        {
            if (snapshot != null)
            {
                ScaffoldUtils.restoreRotation(player, snapshot);
            }
        }

        if (!placed)
        {
            return;
        }

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
        this.ticksSinceLastPlace = 0;
        this.placeCooldown = this.placeDelay.get().intValue();
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

        return ScaffoldUtils.findBestBlockSlot(inventory, preferredSlot);
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

        double horizontalSpeedSq = player.motionX * player.motionX + player.motionZ * player.motionZ;
        double min = Math.max(0.0D, threshold);
        return horizontalSpeedSq > min * min;
    }
}
