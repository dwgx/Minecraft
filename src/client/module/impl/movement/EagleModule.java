package client.module.impl.movement;

import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.FloatSetting;
import client.setting.SettingGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;

public class EagleModule extends Module
{
    private final BoolSetting onlyOnGround;
    private final BoolSetting onlyWhenMoving;
    private final BoolSetting disableWhenFlying;
    private final FloatSetting sampleDepth;
    private final FloatSetting sampleInset;
    private boolean appliedSneak;

    public EagleModule()
    {
        super("eagle", "Eagle", Category.MOVEMENT);
        SettingGroup group = this.addGroup("general", "General");
        this.onlyOnGround = this.addSetting(new BoolSetting("only_ground", "Only On Ground", "Activate only while the player is on ground", true));
        this.onlyWhenMoving = this.addSetting(new BoolSetting("only_moving", "Only When Moving", "Activate only while moving", true));
        this.disableWhenFlying = this.addSetting(new BoolSetting("disable_flying", "Disable While Flying", "Do not force sneak while flying", true));
        this.sampleDepth = this.addSetting(new FloatSetting("sample_depth", "Sample Depth", "How far below feet to check support blocks", 0.60F, 0.20F, 1.20F, 0.05F));
        this.sampleInset = this.addSetting(new FloatSetting("sample_inset", "Sample Inset", "Inset from hitbox corners when sampling", 0.08F, 0.00F, 0.35F, 0.01F));
        group.add(this.onlyOnGround);
        group.add(this.onlyWhenMoving);
        group.add(this.disableWhenFlying);
        group.add(this.sampleDepth);
        group.add(this.sampleInset);
    }

    public void onDisable()
    {
        this.restoreSneakState();
    }

    public void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null)
        {
            this.restoreSneakState();
            return;
        }

        EntityPlayerSP player = mc.thePlayer;

        if (this.onlyOnGround.isEnabled() && !player.onGround)
        {
            this.restoreSneakState();
            return;
        }

        if (this.disableWhenFlying.isEnabled() && player.capabilities.isFlying)
        {
            this.restoreSneakState();
            return;
        }

        if (this.onlyWhenMoving.isEnabled() && Math.abs(player.moveForward) < 0.001F && Math.abs(player.moveStrafing) < 0.001F)
        {
            this.restoreSneakState();
            return;
        }

        boolean shouldSneak = this.shouldSneakAtEdge(player);
        int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
        KeyBinding.setKeyBindState(sneakKey, shouldSneak || this.isPhysicalKeyDown(sneakKey));
        this.appliedSneak = shouldSneak;
    }

    private boolean shouldSneakAtEdge(EntityPlayerSP player)
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

        double depth = this.sampleDepth.get().floatValue();
        double y = bb.minY - depth;
        double inset = Math.max(0.0D, this.sampleInset.get().floatValue());
        double minX = bb.minX + inset;
        double maxX = bb.maxX - inset;
        double minZ = bb.minZ + inset;
        double maxZ = bb.maxZ - inset;

        if (minX > maxX)
        {
            double midX = (bb.minX + bb.maxX) * 0.5D;
            minX = midX;
            maxX = midX;
        }

        if (minZ > maxZ)
        {
            double midZ = (bb.minZ + bb.maxZ) * 0.5D;
            minZ = midZ;
            maxZ = midZ;
        }

        return this.isAirAt(minX, y, minZ)
            || this.isAirAt(minX, y, maxZ)
            || this.isAirAt(maxX, y, minZ)
            || this.isAirAt(maxX, y, maxZ)
            || this.isAirAt(player.posX, y, player.posZ);
    }

    private boolean isAirAt(double x, double y, double z)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.theWorld == null)
        {
            return false;
        }

        return mc.theWorld.isAirBlock(new BlockPos(x, y, z));
    }

    private void restoreSneakState()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null)
        {
            this.appliedSneak = false;
            return;
        }

        int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();

        if (this.appliedSneak)
        {
            KeyBinding.setKeyBindState(sneakKey, this.isPhysicalKeyDown(sneakKey));
            this.appliedSneak = false;
        }
    }

    private boolean isPhysicalKeyDown(int keyCode)
    {
        return keyCode > 0 && keyCode < 256 && Keyboard.isKeyDown(keyCode);
    }
}
