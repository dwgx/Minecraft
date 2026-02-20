package client.bridge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;

/**
 * Centralized Minecraft runtime bridge to avoid scattered static access.
 */
public final class MinecraftBridge implements GameBridge, PlayerBridge, WorldBridge
{
    private static final MinecraftBridge SHARED = new MinecraftBridge();

    private MinecraftBridge()
    {
    }

    public static MinecraftBridge shared()
    {
        return SHARED;
    }

    public Minecraft raw()
    {
        return Minecraft.getMinecraft();
    }

    public GuiScreen currentScreen()
    {
        Minecraft mc = this.raw();
        return mc == null ? null : mc.currentScreen;
    }

    public EntityPlayerSP localPlayer()
    {
        Minecraft mc = this.raw();
        return mc == null ? null : mc.thePlayer;
    }

    public WorldClient localWorld()
    {
        Minecraft mc = this.raw();
        return mc == null ? null : mc.theWorld;
    }

    public boolean hasPlayer()
    {
        return this.localPlayer() != null;
    }

    public boolean hasWorld()
    {
        return this.localWorld() != null;
    }

    public boolean isScreenOpen()
    {
        return this.currentScreen() != null;
    }

    public boolean isWorldLoaded()
    {
        return this.hasWorld();
    }

    public float yaw()
    {
        EntityPlayerSP player = this.localPlayer();
        return player == null ? 0.0F : player.rotationYaw;
    }

    public float pitch()
    {
        EntityPlayerSP player = this.localPlayer();
        return player == null ? 0.0F : player.rotationPitch;
    }
}

