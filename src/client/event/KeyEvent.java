package client.event;

public final class KeyEvent extends CancellableEvent
{
    private final int keyCode;
    private final boolean pressed;

    public KeyEvent(int keyCode, boolean pressed)
    {
        this.keyCode = keyCode;
        this.pressed = pressed;
    }

    public int getKeyCode()
    {
        return this.keyCode;
    }

    public boolean isPressed()
    {
        return this.pressed;
    }
}
