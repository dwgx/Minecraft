package client.module;

import client.bridge.MinecraftBridge;
import client.event.KeyEvent;
import client.event.MotionUpdateEvent;
import client.render.RenderContext2D;
import client.setting.KeybindSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.network.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 模块注册中心：负责索引、查询与生命周期分发。
 */
public final class ModuleManager implements ModuleStateListener
{
    private static final Logger LOGGER = LogManager.getLogger(ModuleManager.class);
    private final List<Module> modules = new ArrayList<Module>();
    private final Map<String, Module> byId = new HashMap<String, Module>();
    private final Map<String, Module> byName = new HashMap<String, Module>();
    private ModuleStateListener externalStateListener;

    public synchronized void register(Module module)
    {
        String idKey = normalize(module == null ? null : module.getId());

        if (module == null || idKey == null || this.byId.containsKey(idKey))
        {
            return;
        }

        this.modules.add(module);
        this.byId.put(idKey, module);
        this.byName.put(normalize(module.getName()), module);
        module.setStateListener(this);
    }

    public synchronized Module getById(String id)
    {
        String key = normalize(id);
        return key == null ? null : this.byId.get(key);
    }

    public synchronized Module getByName(String name)
    {
        String key = normalize(name);
        return key == null ? null : this.byName.get(key);
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
                try
                {
                    module.onTick();
                }
                catch (Throwable throwable)
                {
                    this.logModuleFailure("onTick", module, throwable);
                }
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
                try
                {
                    module.onRender2D(context);
                }
                catch (Throwable throwable)
                {
                    this.logModuleFailure("onRender2D", module, throwable);
                }
            }
        }
    }

    public void onMotionUpdate(MotionUpdateEvent event)
    {
        List<Module> snapshot = this.getAll();

        for (int i = 0; i < snapshot.size(); ++i)
        {
            Module module = snapshot.get(i);

            if (module.isEnabled())
            {
                try
                {
                    module.onMotionUpdate(event);
                }
                catch (Throwable throwable)
                {
                    this.logModuleFailure("onMotionUpdate", module, throwable);
                }
            }
        }
    }

    public void onKey(KeyEvent event)
    {
        if (MinecraftBridge.shared().isScreenOpen())
        {
            return;
        }

        List<Module> snapshot = this.getAll();

        for (int i = 0; i < snapshot.size(); ++i)
        {
            Module module = snapshot.get(i);
            int bindKeyCode = module.getBind().getKeyCode();

            if (bindKeyCode > KeybindSetting.NONE_KEY_CODE && bindKeyCode == event.getKeyCode())
            {
                if (event.isPressed())
                {
                    try
                    {
                        module.toggle();
                        if (MinecraftBridge.shared().isScreenOpen())
                        {
                            return;
                        }
                    }
                    catch (Throwable throwable)
                    {
                        this.logModuleFailure("toggle", module, throwable);
                    }
                }
            }

            if (module.isEnabled())
            {
                try
                {
                    module.onKey(event);
                }
                catch (Throwable throwable)
                {
                    this.logModuleFailure("onKey", module, throwable);
                }
            }
        }
    }

    public boolean onPacketSend(Packet<?> packet)
    {
        boolean cancelled = false;
        List<Module> snapshot = this.getAll();

        for (int i = 0; i < snapshot.size(); ++i)
        {
            Module module = snapshot.get(i);

            if (!module.isEnabled())
            {
                continue;
            }

            try
            {
                if (module.onPacketSend(packet))
                {
                    cancelled = true;
                }
            }
            catch (Throwable throwable)
            {
                this.logModuleFailure("onPacketSend", module, throwable);
            }
        }

        return cancelled;
    }

    public boolean onPacketReceive(Packet<?> packet)
    {
        boolean cancelled = false;
        List<Module> snapshot = this.getAll();

        for (int i = 0; i < snapshot.size(); ++i)
        {
            Module module = snapshot.get(i);

            if (!module.isEnabled())
            {
                continue;
            }

            try
            {
                if (module.onPacketReceive(packet))
                {
                    cancelled = true;
                }
            }
            catch (Throwable throwable)
            {
                this.logModuleFailure("onPacketReceive", module, throwable);
            }
        }

        return cancelled;
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

    private static String normalize(String value)
    {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private void logModuleFailure(String stage, Module module, Throwable throwable)
    {
        String moduleId = module == null ? "unknown" : module.getId();
        LOGGER.error("Module '{}' failed in {}.", moduleId, stage, throwable);
    }
}
