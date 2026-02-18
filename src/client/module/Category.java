package client.module;

import java.util.Locale;

public enum Category
{
    COMBAT,
    MOVEMENT,
    RENDER,
    HUD,
    WORLD,
    MISC,
    CLIENT;

    public String getDisplayName()
    {
        String raw = this.name().toLowerCase(Locale.ROOT);
        client.i18n.I18nManager i18n = client.core.ClientBootstrap.instance().getI18n();
        return i18n == null ? raw : i18n.translateOrDefault("category." + raw, raw);
    }
}
