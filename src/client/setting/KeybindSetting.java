package client.setting;

public class KeybindSetting extends Setting<Integer>
{
    public enum BindMode
    {
        TOGGLE,
        HOLD
    }

    private BindMode mode;

    public KeybindSetting(String key, String name, String description, int defaultKeyCode, BindMode defaultMode)
    {
        super(key, name, description, Integer.valueOf(defaultKeyCode));
        this.mode = defaultMode == null ? BindMode.TOGGLE : defaultMode;
    }

    public int getKeyCode()
    {
        return this.get().intValue();
    }

    public void setKeyCode(int keyCode)
    {
        this.set(Integer.valueOf(keyCode));
    }

    public BindMode getMode()
    {
        return this.mode;
    }

    public void setMode(BindMode mode)
    {
        this.mode = mode == null ? BindMode.TOGGLE : mode;
    }
}
