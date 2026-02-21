package client.event;

import net.minecraft.client.entity.EntityPlayerSP;

public final class MotionUpdateEvent implements Event
{
    public enum Phase
    {
        PRE_UPDATE,
        PRE_MOTION,
        POST_MOTION
    }

    private final Phase phase;
    private final EntityPlayerSP player;
    private final long tickCounter;

    public MotionUpdateEvent(Phase phase, EntityPlayerSP player, long tickCounter)
    {
        this.phase = phase;
        this.player = player;
        this.tickCounter = tickCounter;
    }

    public Phase getPhase()
    {
        return this.phase;
    }

    public EntityPlayerSP getPlayer()
    {
        return this.player;
    }

    public long getTickCounter()
    {
        return this.tickCounter;
    }
}
