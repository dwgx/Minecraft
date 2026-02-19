package dwgx.ui.ext;

import java.util.Locale;
import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Central toggle/factory for menu/loading extensions.
 *
 * JVM properties (optional):
 * - dwgx.ui.mainmenu.replace=true|false
 * - dwgx.ui.loading.replace=true|false
 * - dwgx.ui.mainmenu.splashShader=true|false
 */
public final class UiExtensionManager
{
    public interface MainMenuFactory
    {
        GuiMainMenu create();
    }

    public interface LoadingScreenFactory
    {
        LoadingScreenRenderer create(Minecraft mc);
    }

    private static final Logger LOGGER = LogManager.getLogger(UiExtensionManager.class);
    private static final String PROP_MAINMENU_REPLACE = "dwgx.ui.mainmenu.replace";
    private static final String PROP_LOADING_REPLACE = "dwgx.ui.loading.replace";
    private static final String PROP_SPLASH_SHADER = "dwgx.ui.mainmenu.splashShader";
    private static final MainMenuFactory DEFAULT_MAIN_MENU_FACTORY = new MainMenuFactory()
    {
        public GuiMainMenu create()
        {
            return new DwgxMainMenuScreen();
        }
    };
    private static final LoadingScreenFactory DEFAULT_LOADING_FACTORY = new LoadingScreenFactory()
    {
        public LoadingScreenRenderer create(Minecraft mc)
        {
            return new DwgxLoadingScreenRenderer(mc);
        }
    };
    private static volatile boolean mainMenuReplaceEnabled = readBooleanProperty(PROP_MAINMENU_REPLACE, true);
    private static volatile boolean loadingReplaceEnabled = readBooleanProperty(PROP_LOADING_REPLACE, true);
    private static volatile boolean splashShaderEnabled = readBooleanProperty(PROP_SPLASH_SHADER, true);
    private static volatile MainMenuFactory mainMenuFactory = DEFAULT_MAIN_MENU_FACTORY;
    private static volatile LoadingScreenFactory loadingFactory = DEFAULT_LOADING_FACTORY;

    private UiExtensionManager()
    {
    }

    public static GuiScreen createMainMenuScreen()
    {
        if (!mainMenuReplaceEnabled)
        {
            return new GuiMainMenu();
        }

        return createMainMenuSafely(mainMenuFactory);
    }

    public static LoadingScreenRenderer createLoadingScreen(Minecraft mc)
    {
        if (mc == null)
        {
            throw new IllegalArgumentException("Minecraft instance cannot be null");
        }

        if (!loadingReplaceEnabled)
        {
            return new LoadingScreenRenderer(mc);
        }

        return createLoadingScreenSafely(mc, loadingFactory);
    }

    public static boolean isSplashShaderEnabled()
    {
        return splashShaderEnabled;
    }

    public static void setMainMenuReplaceEnabled(boolean enabled)
    {
        mainMenuReplaceEnabled = enabled;
    }

    public static void setLoadingReplaceEnabled(boolean enabled)
    {
        loadingReplaceEnabled = enabled;
    }

    public static void setSplashShaderEnabled(boolean enabled)
    {
        splashShaderEnabled = enabled;
    }

    public static void setMainMenuFactory(MainMenuFactory factory)
    {
        mainMenuFactory = factory == null ? DEFAULT_MAIN_MENU_FACTORY : factory;
    }

    public static void setLoadingFactory(LoadingScreenFactory factory)
    {
        loadingFactory = factory == null ? DEFAULT_LOADING_FACTORY : factory;
    }

    public static void resetFactories()
    {
        mainMenuFactory = DEFAULT_MAIN_MENU_FACTORY;
        loadingFactory = DEFAULT_LOADING_FACTORY;
    }

    private static GuiScreen createMainMenuSafely(MainMenuFactory factory)
    {
        try
        {
            GuiMainMenu screen = factory == null ? null : factory.create();
            return screen == null ? new GuiMainMenu() : screen;
        }
        catch (Throwable throwable)
        {
            LOGGER.warn("Main menu factory failed, falling back to GuiMainMenu.", throwable);
            return new GuiMainMenu();
        }
    }

    private static LoadingScreenRenderer createLoadingScreenSafely(Minecraft mc, LoadingScreenFactory factory)
    {
        try
        {
            LoadingScreenRenderer renderer = factory == null ? null : factory.create(mc);
            return renderer == null ? new LoadingScreenRenderer(mc) : renderer;
        }
        catch (Throwable throwable)
        {
            LOGGER.warn("Loading screen factory failed, falling back to LoadingScreenRenderer.", throwable);
            return new LoadingScreenRenderer(mc);
        }
    }

    private static boolean readBooleanProperty(String key, boolean defaultValue)
    {
        String raw = System.getProperty(key);

        if (raw == null)
        {
            return defaultValue;
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);

        if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized))
        {
            return true;
        }

        if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized) || "off".equals(normalized))
        {
            return false;
        }

        return defaultValue;
    }
}
