package client.bridge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;

/**
 * Read-only runtime bridge for high-level client code.
 */
public interface GameBridge
{
    Minecraft raw();

    GuiScreen currentScreen();

    EntityPlayerSP localPlayer();

    boolean isScreenOpen();

    boolean isWorldLoaded();
}

