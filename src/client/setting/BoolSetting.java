package client.setting;

public final class BoolSetting extends Setting<Boolean>
{
    public BoolSetting(String key, String name, String description, boolean defaultValue)
    {
        super(key, name, description, Boolean.valueOf(defaultValue));
    }

    public boolean isEnabled()
    {
        return this.get().booleanValue();
    }

    public void setEnabled(boolean enabled)
    {
        this.set(Boolean.valueOf(enabled));
    }
}
