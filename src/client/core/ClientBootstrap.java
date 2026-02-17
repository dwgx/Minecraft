package client.core;

import client.config.ConfigManager;
import client.event.EventBus;
import client.event.KeyEvent;
import client.event.MouseEvent;
import client.event.Render2DEvent;
import client.event.TickEvent;
import client.hud.HudManager;
import client.module.ModuleManager;
import client.module.impl.movement.EageModule;
import client.module.impl.movement.KeepSprintModule;
import client.render.RenderContext2D;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 分层客户端运行时的统一入口。
 */
public final class ClientBootstrap
{
    private static final ClientBootstrap INSTANCE = new ClientBootstrap();

    private final ClientInfo clientInfo = ClientInfo.defaults();
    private final EventBus eventBus = new EventBus();
    private final ModuleManager modules = new ModuleManager();
    private final HudManager hud = new HudManager();
    private final ShutdownHook shutdownHook = new ShutdownHook();

    private ConfigManager configManager;
    private boolean initialized;
    private boolean modulesRegistered;

    private ClientBootstrap()
    {
    }

    public static   ClientBootstrap instance()
    {
        return INSTANCE;
    }

    public synchronized void initialize(Path configRoot)
    {
        if (this.initialized)
        {
            return;
        }

        this.configManager = new ConfigManager(configRoot, this.clientInfo, this.modules, this.hud);
        this.registerBuiltinModules();

        try
        {
            this.configManager.loadAll();
        }
        catch (IOException ignored)
        {
        }

        this.shutdownHook.addAction(new Runnable()
        {
            public void run()
            {
                try
                {
                    ClientBootstrap.this.configManager.saveAll();
                }
                catch (IOException ignored)
                {
                }
            }
        });
        this.initialized = true;
    }

    private void registerBuiltinModules()
    {
        if (this.modulesRegistered)
        {
            return;
        }

        this.modules.register(new EageModule());
        this.modules.register(new KeepSprintModule());
        this.modulesRegistered = true;
    }

    public ClientInfo getClientInfo()
    {
        return this.clientInfo;
    }

    public EventBus getEventBus()
    {
        return this.eventBus;
    }

    public ModuleManager getModules()
    {
        return this.modules;
    }

    public HudManager getHud()
    {
        return this.hud;
    }

    public ConfigManager getConfigManager()
    {
        return this.configManager;
    }

    public ShutdownHook getShutdownHook()
    {
        return this.shutdownHook;
    }

    public void onTick(boolean post)
    {
        TickEvent event = new TickEvent(post ? TickEvent.Phase.POST : TickEvent.Phase.PRE);
        this.eventBus.post(event);

        if (!post)
        {
            this.modules.onTick();
        }
    }

    public void onRender2D(RenderContext2D context)
    {
        this.eventBus.post(new Render2DEvent(context));
        this.modules.onRender2D(context);
        this.hud.render(context);
    }

    public void onKey(int keyCode, boolean pressed)
    {
        KeyEvent event = new KeyEvent(keyCode, pressed);
        this.eventBus.post(event);

        if (!event.isCancelled())
        {
            this.modules.onKey(event);
        }
    }

    public void onMouse(int button, int x, int y, boolean pressed)
    {
        this.eventBus.post(new MouseEvent(button, x, y, pressed));
    }
}
