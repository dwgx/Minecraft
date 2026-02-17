package client.setting;

public final class EnumSetting<E extends Enum<E>> extends Setting<E>
{
    private final Class<E> enumType;

    public EnumSetting(String key, String name, String description, Class<E> enumType, E defaultValue)
    {
        super(key, name, description, defaultValue);
        this.enumType = enumType;
    }

    public Class<E> getEnumType()
    {
        return this.enumType;
    }

    public void setByName(String name)
    {
        if (name == null)
        {
            return;
        }

        E[] values = this.enumType.getEnumConstants();

        for (int i = 0; i < values.length; ++i)
        {
            E candidate = values[i];

            if (candidate.name().equalsIgnoreCase(name))
            {
                this.set(candidate);
                return;
            }
        }
    }
}
