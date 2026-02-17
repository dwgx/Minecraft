package client.hud;

import client.render.RenderContext2D;
import client.setting.Setting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class HudElement
{
    private final String id;
    private final String name;
    private final HudLayer layer;
    private final HudTransform transform = new HudTransform();
    private boolean enabled = true;
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();

    protected HudElement(String id, String name, HudLayer layer)
    {
        this.id = id;
        this.name = name;
        this.layer = layer;
    }

    public String getId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public HudLayer getLayer()
    {
        return this.layer;
    }

    public HudTransform getTransform()
    {
        return this.transform;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public <T extends Setting<?>> T addSetting(T setting)
    {
        this.settings.add(setting);
        return setting;
    }

    public List<Setting<?>> getSettings()
    {
        return Collections.unmodifiableList(this.settings);
    }

    public abstract void render(RenderContext2D context);
}
