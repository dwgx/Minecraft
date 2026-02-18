package client.setting;

public final class StringSetting extends Setting<String>
{
    private final int maxLength;

    public StringSetting(String key, String name, String description, String defaultValue, int maxLength)
    {
        super(key, name, description, defaultValue == null ? "" : defaultValue);
        this.maxLength = Math.max(1, maxLength);
        this.set(this.getDefaultValue());
    }

    protected String normalize(String value)
    {
        String text = value == null ? "" : value;

        if (text.length() <= this.maxLength)
        {
            return text;
        }

        return text.substring(0, this.maxLength);
    }

    public int getMaxLength()
    {
        return this.maxLength;
    }
}
