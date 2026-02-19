package client.event;

public final class EventBusPriorities
{
    public static final byte LOWEST = Priorities.VERY_LOW;
    public static final byte LOW = Priorities.LOW;
    public static final byte MEDIUM = Priorities.MEDIUM;
    public static final byte HIGH = Priorities.HIGH;
    public static final byte HIGHEST = Priorities.VERY_HIGH;

    private EventBusPriorities()
    {
    }
}
