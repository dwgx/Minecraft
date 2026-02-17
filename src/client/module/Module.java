package client.module;

import client.event.KeyEvent;
import client.render.RenderContext2D;
import client.setting.KeybindSetting;
import client.setting.Setting;
import client.setting.SettingGroup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 模块基类：统一生命周期与设置容器。
 */
public abstract class Module
{
    private final String id;
    private String name;
    private final Category category;
    private final KeybindSetting bind;
    private boolean enabled;
    private ModuleStateListener stateListener;
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();
    private final List<SettingGroup> groups = new ArrayList<SettingGroup>();

    protected Module(String id, String name, Category category)
    {
        this.id = id;
        this.name = name;
        this.category = category;
        this.bind = new KeybindSetting("bind", "Bind", "Keyboard bind", 0, KeybindSetting.BindMode.TOGGLE);
    }

    public String getId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Category getCategory()
    {
        return this.category;
    }

    public KeybindSetting getBind()
    {
        return this.bind;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public final void setEnabled(boolean enabled)
    {
        if (this.enabled == enabled)
        {
            return;
        }

        this.enabled = enabled;

        if (enabled)
        {
            this.onEnable();
        }
        else
        {
            this.onDisable();
        }

        if (this.stateListener != null)
        {
            this.stateListener.onModuleChanged(this);
        }
    }

    public final void toggle()
    {
        this.setEnabled(!this.enabled);
    }

    public final <T extends Setting<?>> T addSetting(T setting)
    {
        this.settings.add(setting);
        return setting;
    }

    public final SettingGroup addGroup(String key, String name)
    {
        SettingGroup group = new SettingGroup(key, name);
        this.groups.add(group);
        return group;
    }

    public final List<Setting<?>> getSettings()
    {
        return Collections.unmodifiableList(this.settings);
    }

    public final List<SettingGroup> getGroups()
    {
        return Collections.unmodifiableList(this.groups);
    }

    void setStateListener(ModuleStateListener stateListener)
    {
        this.stateListener = stateListener;
    }

    public void onEnable()
    {
    }

    public void onDisable()
    {
    }

    public void onTick()
    {
    }

    public void onRender2D(RenderContext2D context)
    {
    }

    public void onRender3D()
    {
    }

    public void onKey(KeyEvent event)
    {
    }
}
