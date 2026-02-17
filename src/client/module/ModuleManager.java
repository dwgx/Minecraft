package client.module;

import client.event.KeyEvent;
import client.render.RenderContext2D;
import client.setting.KeybindSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 模块注册中心：负责索引、查询与生命周期分发。
 */
public final class ModuleManager implements ModuleStateListener
{
    private final List<Module> modules = new ArrayList<Module>();
    private final Map<String, Module> byId = new HashMap<String, Module>();
    private final Map<String, Module> byName = new HashMap<String, Module>();
    private ModuleStateListener externalStateListener;

    public synchronized void register(Module module)
    {
        if (module == null || this.byId.containsKey(module.getId()))
        {
            return;
        }

        this.modules.add(module);
        this.byId.put(module.getId(), module);
        this.byName.put(module.getName().toLowerCase(Locale.ROOT), module);
        module.setStateListener(this);
    }

    public synchronized Module getById(String id)
    {
        return id == null ? null : this.byId.get(id);
    }

    public synchronized Module getByName(String name)
    {
        return name == null ? null : this.byName.get(name.toLowerCase(Locale.ROOT));
    }

    public synchronized List<Module> getByCategory(Category category)
    {
        List<Module> out = new ArrayList<Module>();

        for (int i = 0; i < this.modules.size(); ++i)
        {
            Module module = this.modules.get(i);

            if (module.getCategory() == category)
            {
                out.add(module);
            }
        }

        return out;
    }

    public synchronized List<Module> getAll()
    {
        return Collections.unmodifiableList(new ArrayList<Module>(this.modules));
    }

    public void onTick()
    {
        List<Module> snapshot = this.getAll();

        for (int i = 0; i < snapshot.size(); ++i)
        {
            Module module = snapshot.get(i);

            if (module.isEnabled())
            {
                module.onTick();
            }
        }
    }

    public void onRender2D(RenderContext2D context)
    {
        List<Module> snapshot = this.getAll();

        for (int i = 0; i < snapshot.size(); ++i)
        {
            Module module = snapshot.get(i);

            if (module.isEnabled())
            {
                module.onRender2D(context);
            }
        }
    }

    public void onKey(KeyEvent event)
    {
        List<Module> snapshot = this.getAll();

        for (int i = 0; i < snapshot.size(); ++i)
        {
            Module module = snapshot.get(i);

            if (module.getBind().getKeyCode() == event.getKeyCode())
            {
                if (module.getBind().getMode() == KeybindSetting.BindMode.TOGGLE && event.isPressed())
                {
                    module.toggle();
                }
                else if (module.getBind().getMode() == KeybindSetting.BindMode.HOLD)
                {
                    module.setEnabled(event.isPressed());
                }
            }

            if (module.isEnabled())
            {
                module.onKey(event);
            }
        }
    }

    public void setStateListener(ModuleStateListener externalStateListener)
    {
        this.externalStateListener = externalStateListener;
    }

    public void onModuleChanged(Module module)
    {
        if (this.externalStateListener != null)
        {
            this.externalStateListener.onModuleChanged(module);
        }
    }
}
