package client.core;

import client.command.ClientCommandManager;
import client.config.ConfigManager;
import client.event.EventBus;
import client.event.KeyEvent;
import client.event.MouseEvent;
import client.event.Render2DEvent;
import client.event.TickEvent;
import client.hud.HudEditorScreen;
import client.hud.HudManager;
import client.i18n.I18nManager;
import client.module.ModuleManager;
import client.module.impl.client.ClickGuiModule;
import client.module.impl.client.HudEditModule;
import client.module.impl.client.UiScaleEditModule;
import client.module.impl.movement.EagleModule;
import client.module.impl.movement.KeepSprintModule;
import client.module.impl.movement.ScaffoldModule;
import client.render.RenderContext2D;
import client.render.NanoVGContext;
import client.ui.NanoRenderableScreen;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentText;

/**
 * Main runtime entry for client layers.
 */
public final class ClientBootstrap
{
    private static final long AUTOSAVE_INTERVAL_MS = 15000L;
    private static final ClientBootstrap INSTANCE = new ClientBootstrap();

    private final ClientInfo clientInfo = ClientInfo.defaults();
    private final EventBus eventBus = new EventBus();
    private final ModuleManager modules = new ModuleManager();
    private final ClientCommandManager commandManager = new ClientCommandManager(this.modules);
    private final HudManager hud = new HudManager();
    private final I18nManager i18n = new I18nManager();
    private final ShutdownHook shutdownHook = new ShutdownHook();

    private ConfigManager configManager;
    private boolean initialized;
    private boolean modulesRegistered;
    private boolean hudElementsRegistered;
    private boolean jvmShutdownHookRegistered;
    private long lastAutosaveAtMs;
    private boolean nanoAvailable;

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

        this.i18n.reload();
        this.configManager = new ConfigManager(configRoot, this.clientInfo, this.modules, this.hud, this.i18n);
        this.registerBuiltinModules();
        this.registerBuiltinHudElements();

        try
        {
            this.configManager.loadAll();
        }
        catch (IOException ignored)
        {
        }
        catch (RuntimeException ignored)
        {
        }

        this.shutdownHook.addAction(new Runnable()
        {
            public void run()
            {
                ClientBootstrap.this.flushConfigSafely();
            }
        });

        this.installJvmShutdownHook();
        this.lastAutosaveAtMs = System.currentTimeMillis();
        this.initialized = true;
    }

    private void registerBuiltinModules()
    {
        if (this.modulesRegistered)
        {
            return;
        }

        this.modules.register(new ClickGuiModule());
        this.modules.register(new HudEditModule());
        this.modules.register(new UiScaleEditModule());
        this.modules.register(new EagleModule());
        this.modules.register(new KeepSprintModule());
        this.modules.register(new ScaffoldModule());
        this.modulesRegistered = true;
    }

    private void registerBuiltinHudElements()
    {
        if (this.hudElementsRegistered)
        {
            return;
        }

        this.hud.register(new client.hud.HudFpsElement());
        this.hudElementsRegistered = true;
    }

    private synchronized void installJvmShutdownHook()
    {
        if (this.jvmShutdownHookRegistered)
        {
            return;
        }

        try
        {
            Runtime.getRuntime().addShutdownHook(new Thread("Client Config Shutdown Hook")
            {
                public void run()
                {
                    ClientBootstrap.this.shutdownHook.run();
                }
            });
            this.jvmShutdownHookRegistered = true;
        }
        catch (IllegalStateException ignored)
        {
        }
        catch (SecurityException ignored)
        {
        }
    }

    private void flushConfigSafely()
    {
        if (this.configManager == null)
        {
            return;
        }

        try
        {
            this.configManager.saveAll();
        }
        catch (IOException ignored)
        {
        }
        catch (RuntimeException ignored)
        {
        }
    }

    private void autosaveIfDue()
    {
        if (!this.initialized || this.configManager == null)
        {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - this.lastAutosaveAtMs < AUTOSAVE_INTERVAL_MS)
        {
            return;
        }

        this.flushConfigSafely();
        this.lastAutosaveAtMs = now;
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

    public I18nManager getI18n()
    {
        return this.i18n;
    }

    public ConfigManager getConfigManager()
    {
        return this.configManager;
    }

    public ShutdownHook getShutdownHook()
    {
        return this.shutdownHook;
    }

    public void notifyUser(String message)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc != null && mc.ingameGUI != null && mc.ingameGUI.getChatGUI() != null)
        {
            mc.ingameGUI.getChatGUI().printChatMessage(new net.minecraft.util.ChatComponentText(message));
        }
    }

    public synchronized void setNanoAvailable(boolean available)
    {
        this.nanoAvailable = available;
    }

    public synchronized boolean isNanoAvailable()
    {
        return this.nanoAvailable;
    }

    public boolean handleChatCommand(String rawInput)
    {
        return this.initialized && this.commandManager.execute(rawInput);
    }

    public List<String> completeChatCommand(String input, int cursor)
    {
        return this.initialized ? this.commandManager.complete(input, cursor) : null;
    }

    public void onTick(boolean post)
    {
        TickEvent event = new TickEvent(post ? TickEvent.Phase.POST : TickEvent.Phase.PRE);
        this.eventBus.post(event);

        if (!post)
        {
            this.modules.onTick();
        }
        else
        {
            this.autosaveIfDue();
        }
    }

    public void onRender2D(RenderContext2D context)
    {
        this.eventBus.post(new Render2DEvent(context));
        this.modules.onRender2D(context);

        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen current = mc == null ? null : mc.currentScreen;
        boolean inWorld = mc != null && mc.theWorld != null;

        if (inWorld && (current == null || current instanceof HudEditorScreen))
        {
            this.hud.render(context);
        }

        if (current instanceof NanoRenderableScreen)
        {
            NanoVGContext nano = context == null ? null : context.getNanoVG();

            if (nano == null || !nano.isFrameActive())
            {
                return;
            }

            nano.save();

            try
            {
                ((NanoRenderableScreen)current).renderNano(context);
            }
            finally
            {
                nano.resetTransform();
                nano.resetScissor();
                nano.restore();
            }
        }
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
