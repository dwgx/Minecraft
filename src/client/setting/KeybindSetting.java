package client.setting;

import java.util.Locale;
import org.lwjgl.input.Keyboard;

public class KeybindSetting extends Setting<Integer>
{
    public static final int NONE_KEY_CODE = 0;

    public enum BindMode
    {
        TOGGLE
    }

    private BindMode mode;

    public KeybindSetting(String key, String name, String description, int defaultKeyCode, BindMode defaultMode)
    {
        super(key, name, description, Integer.valueOf(defaultKeyCode <= 0 ? NONE_KEY_CODE : defaultKeyCode));
        this.mode = BindMode.TOGGLE;
    }

    public int getKeyCode()
    {
        return this.get().intValue();
    }

    public void setKeyCode(int keyCode)
    {
        this.set(Integer.valueOf(keyCode <= 0 ? NONE_KEY_CODE : keyCode));
    }

    public BindMode getMode()
    {
        return this.mode;
    }

    public void setMode(BindMode mode)
    {
        this.mode = BindMode.TOGGLE;
    }

    public boolean isBound()
    {
        return this.getKeyCode() > NONE_KEY_CODE;
    }

    public void clear()
    {
        this.setKeyCode(NONE_KEY_CODE);
    }

    public String getDisplayName()
    {
        int keyCode = this.getKeyCode();

        if (keyCode <= NONE_KEY_CODE)
        {
            return "NONE";
        }

        String keyName = Keyboard.getKeyName(keyCode);
        return keyName == null || keyName.isEmpty() ? String.valueOf(keyCode) : keyName.toUpperCase(Locale.ROOT);
    }
}
