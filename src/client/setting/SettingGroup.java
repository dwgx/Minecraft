package client.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class SettingGroup
{
    private final String key;
    private final String name;
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();

    public SettingGroup(String key, String name)
    {
        this.key = key;
        this.name = name;
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
        String fallback = this.name == null ? "" : this.name;
        String normalizedKey = normalizeToken(this.key);

        if (normalizedKey.isEmpty())
        {
            return fallback;
        }

        client.i18n.I18nManager i18n = client.core.ClientBootstrap.instance().getI18n();

        if (i18n == null)
        {
            return fallback;
        }

        String owner = normalizeToken(ownerId);

        if (!owner.isEmpty())
        {
            String scopedKey = "setting.group." + owner + "." + normalizedKey + ".name";
            String scoped = i18n.translateOrDefault(scopedKey, scopedKey);

            if (!scopedKey.equals(scoped))
            {
                return scoped;
            }
        }

        return i18n.translateOrDefault("setting.group." + normalizedKey + ".name", fallback);
    }

    public <T extends Setting<?>> T add(T setting)
    {
        this.settings.add(setting);
        return setting;
    }

    public List<Setting<?>> getSettings()
    {
        return Collections.unmodifiableList(this.settings);
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
}
