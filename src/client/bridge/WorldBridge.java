package client.bridge;

import net.minecraft.client.multiplayer.WorldClient;

/**
 * World-specific bridge contract.
 */
public interface WorldBridge
{
    WorldClient localWorld();

    boolean hasWorld();
}

