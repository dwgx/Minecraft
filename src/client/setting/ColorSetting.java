package client.setting;

public final class ColorSetting extends Setting<ColorValue>
{
    public ColorSetting(String key, String name, String description, ColorValue defaultValue)
    {
        super(key, name, description, defaultValue);
    }
}
