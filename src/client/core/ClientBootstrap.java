package client.core;

import client.auth.AccountRepository;
import client.auth.MicrosoftAuthResult;
import client.auth.MicrosoftAuthService;
import client.auth.MicrosoftSessionManager;
import client.chat.cache.IRCLocalCache;
import client.command.ClientCommandManager;
import client.config.ConfigManager;
import client.event.EventBus;
import client.event.KeyEvent;
import client.event.MouseEvent;
import client.event.Render2DEvent;
import client.event.TickEvent;
import client.hud.HudEditorScreen;
import client.hud.HudManager;
import client.hud.HudRegistry;
import client.i18n.I18nManager;
import client.module.ModuleManager;
import client.module.ModuleRegistry;
import client.render.RenderContext2D;
import client.render.NanoVGContext;
import client.ui.NanoRenderableScreen;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main runtime entry for client layers.
 */
public final class ClientBootstrap
{
    private static final Logger LOGGER = LogManager.getLogger(ClientBootstrap.class);
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
    private IRCLocalCache ircLocalCache;
    // Main-thread-only flags; synchronized in initialize() for safe publication.
    private volatile boolean initialized;
    private boolean modulesRegistered;
    private boolean hudElementsRegistered;
    private boolean jvmShutdownHookRegistered;
    private long lastAutosaveAtMs;
    private volatile boolean nanoAvailable;

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

        // Set persistent wallpaper dir early so the cached Bing image is
        // available before GuiMainMenu is created.
        dwgx.ui.ext.UiExtensionManager.setBingWallpaperDir(configRoot);

        this.i18n.reload();
        this.configManager = new ConfigManager(configRoot, this.clientInfo, this.modules, this.hud, this.i18n);
        this.registerBuiltinModules();
        this.registerBuiltinHudElements();

        try
        {
            this.configManager.loadAll();
            this.reportConfigLoadIssues();
        }
        catch (IOException ex)
        {
            LOGGER.warn("Failed to load client config from {}", configRoot, ex);
            this.notifyUser(this.i18n.translateOrDefault("config.load.failed", "\u00a7cFailed to load client config. See logs."));
        }
        catch (RuntimeException ex)
        {
            LOGGER.warn("Unexpected runtime error while loading client config from {}", configRoot, ex);
            this.notifyUser(this.i18n.translateOrDefault("config.load.failed", "\u00a7cFailed to load client config. See logs."));
        }

        // Initialize SQLite local cache for IRC data
        try
        {
            this.ircLocalCache = new IRCLocalCache(
                    configRoot.resolve("irc_cache.db").toString());
            this.ircLocalCache.open();
        }
        catch (Exception ex)
        {
            LOGGER.warn("Failed to open IRC local cache", ex);
            this.ircLocalCache = null;
        }

        this.shutdownHook.addAction(new Runnable()
        {
            public void run()
            {
                if (ClientBootstrap.this.ircLocalCache != null)
                {
                    ClientBootstrap.this.ircLocalCache.close();
                }
            }
        });
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
        this.attemptAutoLogin(configRoot);
    }

    private void registerBuiltinModules()
    {
        if (this.modulesRegistered)
        {
            return;
        }

        ModuleRegistry.registerBuiltins(this.modules);
        this.modulesRegistered = true;
    }

    private void registerBuiltinHudElements()
    {
        if (this.hudElementsRegistered)
        {
            return;
        }

        HudRegistry.registerBuiltins(this.hud);
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

    private void attemptAutoLogin(Path configRoot)
    {
        String autoLoginProp = System.getProperty("client.autologin");
        if (autoLoginProp != null && "false".equalsIgnoreCase(autoLoginProp.trim()))
        {
            LOGGER.info("Auto-login disabled by JVM property: client.autologin=false");
            return;
        }

        try
        {
            File mcDataDir = configRoot.toFile().getParentFile().getParentFile();
            AccountRepository repo = new AccountRepository(mcDataDir);
            repo.load();
            String selectedId = repo.getSelectedId();

            if (selectedId == null || selectedId.isEmpty())
            {
                return;
            }

            AccountRepository.AccountEntry entry = repo.findById(selectedId);

            if (entry == null)
            {
                return;
            }

            if (entry.isMicrosoft())
            {
                String refresh = entry.getRefreshToken();

                if (refresh == null || refresh.isEmpty())
                {
                    LOGGER.info("Auto-login skipped: Microsoft account '{}' has no refresh token.", entry.getName());
                    return;
                }

                LOGGER.info("Auto-login: attempting Microsoft re-auth for '{}'...", entry.getName());
                final AccountRepository finalRepo = repo;
                final AccountRepository.AccountEntry finalEntry = entry;
                final String finalRefresh = refresh;

                Thread worker = new Thread(new Runnable()
                {
                    public void run()
                    {
                        ClientBootstrap.this.doMicrosoftAutoLogin(finalRepo, finalEntry, finalRefresh);
                    }
                }, "Auto-Login");
                worker.setDaemon(true);
                worker.start();
            }
            else
            {
                Minecraft mc = Minecraft.getMinecraft();

                if (mc != null)
                {
                    MicrosoftSessionManager.applyOfflineSession(mc, entry.getName());
                    LOGGER.info("Auto-login: applied offline session for '{}'.", entry.getName());
                }
            }
        }
        catch (Exception ex)
        {
            LOGGER.warn("Auto-login failed.", ex);
        }
    }

    private void doMicrosoftAutoLogin(AccountRepository repo, AccountRepository.AccountEntry entry, String refreshToken)
    {
        try
        {
            MicrosoftAuthService authService = new MicrosoftAuthService();
            MicrosoftAuthResult result = authService.loginWithRefreshToken(refreshToken);
            Minecraft mc = Minecraft.getMinecraft();

            if (mc == null)
            {
                return;
            }

            MicrosoftSessionManager.applyMicrosoftSession(mc, result);
            repo.upsertMicrosoft(result);
            repo.save();
            LOGGER.info("Auto-login: Microsoft session applied for '{}'.", result.getPlayerName());
        }
        catch (Exception ex)
        {
            LOGGER.warn("Auto-login: Microsoft re-auth failed for '{}'. User must login manually.", entry.getName(), ex);
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

    private void reportConfigLoadIssues()
    {
        if (this.configManager == null)
        {
            return;
        }

        List<String> issues = this.configManager.consumeLoadIssues();

        if (issues.isEmpty())
        {
            return;
        }

        LOGGER.warn("Client config load completed with {} issue(s).", Integer.valueOf(issues.size()));

        for (int i = 0; i < issues.size(); ++i)
        {
            LOGGER.warn("Config issue {}: {}", Integer.valueOf(i + 1), issues.get(i));
        }

        this.notifyUser(this.i18n.translateOrDefault("config.load.warning", "\u00a7eClient config load reported {0} issue(s). See logs.", Integer.valueOf(issues.size())));
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

    public IRCLocalCache getIrcLocalCache()
    {
        return this.ircLocalCache;
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

    public void setNanoAvailable(boolean available)
    {
        this.nanoAvailable = available;
    }

    public boolean isNanoAvailable()
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
        this.dispatchRenderEvent(context);
        this.renderModules(context);
        this.renderHud(context);
        this.renderNanoScreen(context);
    }

    private void dispatchRenderEvent(RenderContext2D context)
    {
        try
        {
            this.eventBus.post(new Render2DEvent(context));
        }
        catch (Throwable throwable)
        {
            LOGGER.error("Render2D event dispatch failed.", throwable);
        }
    }

    private void renderModules(RenderContext2D context)
    {
        try
        {
            this.modules.onRender2D(context);
        }
        catch (Throwable throwable)
        {
            LOGGER.error("Module Render2D dispatch failed.", throwable);
        }
    }

    private void renderHud(RenderContext2D context)
    {
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen current = mc == null ? null : mc.currentScreen;
        boolean inWorld = mc != null && mc.theWorld != null;

        if (inWorld && (current == null || current instanceof HudEditorScreen))
        {
            try
            {
                this.hud.render(context);
            }
            catch (Throwable throwable)
            {
                LOGGER.error("HUD render failed.", throwable);
            }
        }
    }

    private void renderNanoScreen(RenderContext2D context)
    {
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen current = mc == null ? null : mc.currentScreen;

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
            catch (Throwable throwable)
            {
                LOGGER.error("Nano screen render failed: {}", current.getClass().getName(), throwable);
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

    public void onMotionUpdate(client.event.MotionUpdateEvent event)
    {
        this.eventBus.post(event);
        this.modules.onMotionUpdate(event);
    }

    public boolean onPacketSend(Packet<?> packet)
    {
        return this.modules.onPacketSend(packet);
    }

    public boolean onPacketReceive(Packet<?> packet)
    {
        return this.modules.onPacketReceive(packet);
    }
}
