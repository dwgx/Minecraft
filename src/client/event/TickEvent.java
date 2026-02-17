package client.event;

public final class TickEvent implements Event
{
    public enum Phase
    {
        PRE,
        POST
    }

    private final Phase phase;

    public TickEvent(Phase phase)
    {
        this.phase = phase;
    }

    public Phase getPhase()
    {
        return this.phase;
    }
}
