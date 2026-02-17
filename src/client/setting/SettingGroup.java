package client.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public <T extends Setting<?>> T add(T setting)
    {
        this.settings.add(setting);
        return setting;
    }

    public List<Setting<?>> getSettings()
    {
        return Collections.unmodifiableList(this.settings);
    }
}
