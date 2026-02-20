package client.bridge;

import net.minecraft.client.entity.EntityPlayerSP;

/**
 * Player-specific bridge contract.
 */
public interface PlayerBridge
{
    EntityPlayerSP localPlayer();

    boolean hasPlayer();

    float yaw();

    float pitch();
}

