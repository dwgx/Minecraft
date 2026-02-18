package client.setting;

import java.util.Locale;

/**
 * Base setting model shared by modules and UI controls.
 */
public abstract class Setting<T>
{
    private final String key;
    private final String name;
    private final String description;
    private final T defaultValue;
    private T value;
    private Visibility visibility;

    protected Setting(String key, String name, String description, T defaultValue)
    {
        this.key = key;
        this.name = name;
        this.description = description == null ? "" : description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.visibility = Visibility.ALWAYS;
    }

    public String getKey()
    {
        return this.key;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDisplayName()
    {
        return this.getDisplayName(null);
    }

    public String getDisplayName(String ownerId)
    {
        String normalizedKey = normalizeToken(this.key);
        String fallback = this.name == null ? "" : this.name;

        if (normalizedKey.isEmpty())
        {
            return fallback;
        }

        String owner = normalizeToken(ownerId);

        if (!owner.isEmpty())
        {
            String scoped = tryTranslate("setting." + owner + "." + normalizedKey + ".name");

            if (scoped != null)
            {
                return scoped;
            }
        }

        return translateOrDefault("setting." + normalizedKey + ".name", fallback);
    }

    public String getDescription()
    {
        return this.description;
    }

    public String getDisplayDescription()
    {
        return this.getDisplayDescription(null);
    }

    public String getDisplayDescription(String ownerId)
    {
        String normalizedKey = normalizeToken(this.key);
        String fallback = this.description == null ? "" : this.description;

        if (normalizedKey.isEmpty())
        {
            return fallback;
        }

        String owner = normalizeToken(ownerId);

        if (!owner.isEmpty())
        {
            String scoped = tryTranslate("setting." + owner + "." + normalizedKey + ".description");

            if (scoped != null)
            {
                return scoped;
            }
        }

        return translateOrDefault("setting." + normalizedKey + ".description", fallback);
    }

    public String getDisplayOption(Object option)
    {
        return this.getDisplayOption(null, option);
    }

    public String getDisplayOption(String ownerId, Object option)
    {
        if (option == null)
        {
            return "null";
        }

        String normalizedKey = normalizeToken(this.key);
        String normalizedOption = normalizeToken(String.valueOf(option));
        String fallback = String.valueOf(option).toLowerCase(Locale.ROOT);

        if (normalizedKey.isEmpty() || normalizedOption.isEmpty())
        {
            return fallback;
        }

        String owner = normalizeToken(ownerId);

        if (!owner.isEmpty())
        {
            String scoped = tryTranslate("setting." + owner + "." + normalizedKey + ".option." + normalizedOption);

            if (scoped != null)
            {
                return scoped;
            }
        }

        return translateOrDefault("setting." + normalizedKey + ".option." + normalizedOption, fallback);
    }

    public T getDefaultValue()
    {
        return this.defaultValue;
    }

    public T get()
    {
        return this.value;
    }

    public void set(T value)
    {
        T normalized = this.normalize(value);

        if (equalsValue(this.value, normalized))
        {
            return;
        }

        this.value = normalized;
        markConfigDirty();
    }

    public boolean isVisible()
    {
        return this.visibility == null || this.visibility.isVisible();
    }

    public Setting<T> visibleWhen(Visibility visibility)
    {
        this.visibility = visibility == null ? Visibility.ALWAYS : visibility;
        return this;
    }

    public void reset()
    {
        this.set(this.defaultValue);
    }

    protected T normalize(T value)
    {
        return value;
    }

    private static String tryTranslate(String key)
    {
        if (key == null || key.isEmpty())
        {
            return null;
        }

        client.i18n.I18nManager i18n = client.core.ClientBootstrap.instance().getI18n();

        if (i18n == null)
        {
            return null;
        }

        String value = i18n.translateOrDefault(key, key);
        return key.equals(value) ? null : value;
    }

    private static String translateOrDefault(String key, String fallback)
    {
        client.i18n.I18nManager i18n = client.core.ClientBootstrap.instance().getI18n();
        return i18n == null ? fallback : i18n.translateOrDefault(key, fallback);
    }

    private static String normalizeToken(String value)
    {
        if (value == null)
        {
            return "";
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        StringBuilder out = new StringBuilder(normalized.length());

        for (int i = 0; i < normalized.length(); ++i)
        {
            char c = normalized.charAt(i);

            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')
            {
                out.append(c);
            }
        }

        return out.toString();
    }

    private static boolean equalsValue(Object left, Object right)
    {
        return left == right || left != null && left.equals(right);
    }

    private static void markConfigDirty()
    {
        client.config.ConfigManager config = client.core.ClientBootstrap.instance().getConfigManager();

        if (config != null)
        {
            config.markModulesDirty();
            config.markHudDirty();
        }
    }
}
