package client.i18n;

/**
 * Compile-time known UI locales for the EnumSetting dropdown.
 * Each constant maps to a locale code recognised by {@link I18nManager}.
 */
public enum ClientLocale
{
    EN_US("en_us"),
    ZH_CN("zh_cn"),
    JA_JP("ja_jp");

    private final String code;

    ClientLocale(String code)
    {
        this.code = code;
    }

    public String getCode()
    {
        return this.code;
    }

    /**
     * Resolve a locale code string to the matching enum constant.
     * Returns {@link #EN_US} if no match is found.
     */
    public static ClientLocale fromCode(String code)
    {
        if (code == null || code.isEmpty())
        {
            return EN_US;
        }

        for (ClientLocale locale : values())
        {
            if (locale.code.equalsIgnoreCase(code))
            {
                return locale;
            }
        }

        return EN_US;
    }
}
