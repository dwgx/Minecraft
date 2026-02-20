package dwgx.foundation.input;

/**
 * Immutable input state snapshot for controller logic.
 */
public final class InputSnapshot
{
    private final boolean forward;
    private final boolean back;
    private final boolean left;
    private final boolean right;
    private final boolean jump;
    private final boolean sneak;

    public InputSnapshot(boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sneak)
    {
        this.forward = forward;
        this.back = back;
        this.left = left;
        this.right = right;
        this.jump = jump;
        this.sneak = sneak;
    }

    public boolean forward()
    {
        return this.forward;
    }

    public boolean back()
    {
        return this.back;
    }

    public boolean left()
    {
        return this.left;
    }

    public boolean right()
    {
        return this.right;
    }

    public boolean jump()
    {
        return this.jump;
    }

    public boolean sneak()
    {
        return this.sneak;
    }
}

