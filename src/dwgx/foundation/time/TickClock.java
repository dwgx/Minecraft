package dwgx.foundation.time;

/**
 * Simple delta-time clock for frame/tick smoothing code.
 */
public final class TickClock
{
    private long lastNanos;

    public float deltaSeconds()
    {
        long now = System.nanoTime();
        float dt = this.lastNanos == 0L ? (1.0F / 60.0F) : (float)((double)(now - this.lastNanos) * 1.0E-9D);
        this.lastNanos = now;
        return Math.max(0.001F, Math.min(0.050F, dt));
    }

    public void reset()
    {
        this.lastNanos = System.nanoTime();
    }
}

