package client.event;

public final class MouseEvent extends CancellableEvent
{
    private final int button;
    private final int x;
    private final int y;
    private final boolean pressed;

    public MouseEvent(int button, int x, int y, boolean pressed)
    {
        this.button = button;
        this.x = x;
        this.y = y;
        this.pressed = pressed;
    }

    public int getButton()
    {
        return this.button;
    }

    public int getX()
    {
        return this.x;
    }

    public int getY()
    {
        return this.y;
    }

    public boolean isPressed()
    {
        return this.pressed;
    }
}
