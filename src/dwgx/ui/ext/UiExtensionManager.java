package dwgx.ui.ext;

import java.io.File;
import java.util.Locale;
import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Central toggle/factory for menu/loading extensions.
 */
public final class UiExtensionManager
{
    public enum MainMenuBackgroundMode
    {
        DEFAULT("default", "Default Background"),
        FLAPPY_GLSL("flappy", "Flappy GLSL"),
        ATARI_SOKOBAN_GLSL("atari", "Atari Sokoban GLSL"),
        NYAN_CAT_GLSL("nyan", "Nyan Cat GLSL"),
        KIRBY_JUMP_GLSL("kirby", "Kirby Jump GLSL"),
        GLSL_FILE("glsl", "External GLSL"),
        STATIC_IMAGE("image", "Static Image"),
        VIDEO_STREAM("video", "Video Stream");

        private final String id;
        private final String displayName;

        MainMenuBackgroundMode(String id, String displayName)
        {
            this.id = id;
            this.displayName = displayName;
        }

        public String id()
        {
            return this.id;
        }

        public String displayName()
        {
            return this.displayName;
        }

        public boolean usesShader()
        {
            return this != DEFAULT;
        }

        public static MainMenuBackgroundMode fromId(String value)
        {
            if (value == null)
            {
                return null;
            }

            String normalized = value.trim().toLowerCase(Locale.ROOT);

            for (MainMenuBackgroundMode mode : values())
            {
                if (mode.id.equals(normalized))
                {
                    return mode;
                }
            }

            return null;
        }
    }

    public enum MainMenuBackgroundContent
    {
        SKYBOX,
        SHADER_SCENE,
        STATIC_IMAGE,
        VIDEO_STREAM
    }

    /**
     * Data model for one menu background option.
     * Adding a new mini-game only needs one new entry in {@link #MAIN_MENU_BACKGROUND_OPTIONS}.
     */
    public static final class MainMenuBackgroundOption
    {
        private final MainMenuBackgroundMode mode;
        private final String controlHint;
        private final MainMenuBackgroundSceneFactory sceneFactory;
        private final MainMenuBackgroundContent content;

        private MainMenuBackgroundOption(
            MainMenuBackgroundMode mode,
            String controlHint,
            MainMenuBackgroundSceneFactory sceneFactory,
            MainMenuBackgroundContent content
        )
        {
            this.mode = mode;
            this.controlHint = controlHint == null ? "" : controlHint;
            this.sceneFactory = sceneFactory;
            this.content = content == null ? MainMenuBackgroundContent.SHADER_SCENE : content;
        }

        public MainMenuBackgroundMode mode()
        {
            return this.mode;
        }

        public String controlHint()
        {
            return this.controlHint;
        }

        public MainMenuBackgroundSceneFactory sceneFactory()
        {
            return this.sceneFactory;
        }

        public MainMenuBackgroundContent content()
        {
            return this.content;
        }
    }

    public interface MainMenuFactory
    {
        GuiMainMenu create();
    }

    public interface LoadingScreenFactory
    {
        LoadingScreenRenderer create(Minecraft mc);
    }

    public interface MainMenuBackgroundSceneFactory
    {
        MainMenuBackgroundScene create();
    }

    public interface MainMenuVideoFrameProvider
    {
        void tick();
        boolean hasFrame();
        int frameWidth();
        int frameHeight();
        int[] copyFrameArgb();
        String overlayHint();
        void close();
    }

    public interface MainMenuVideoFrameProviderFactory
    {
        MainMenuVideoFrameProvider create(String sourcePath);
    }

    private static final Logger LOGGER = LogManager.getLogger(UiExtensionManager.class);
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

    private static final MainMenuBackgroundSceneFactory DEFAULT_MAIN_MENU_BACKGROUND_SCENE_FACTORY = new MainMenuBackgroundSceneFactory()
    {
        public MainMenuBackgroundScene create()
        {
            return new FlappyBirdBackgroundScene();
        }
    };

    private static final MainMenuVideoFrameProviderFactory DEFAULT_VIDEO_FRAME_PROVIDER_FACTORY = new MainMenuVideoFrameProviderFactory()
    {
        public MainMenuVideoFrameProvider create(String sourcePath)
        {
            return null;
        }
    };

    private static final MainMenuBackgroundOption[] MAIN_MENU_BACKGROUND_OPTIONS = new MainMenuBackgroundOption[]
    {
        new MainMenuBackgroundOption(MainMenuBackgroundMode.DEFAULT, "Skybox", null, MainMenuBackgroundContent.SKYBOX),
        new MainMenuBackgroundOption(
            MainMenuBackgroundMode.FLAPPY_GLSL,
            "Space",
            new MainMenuBackgroundSceneFactory()
            {
                public MainMenuBackgroundScene create()
                {
                    return new FlappyBirdBackgroundScene();
                }
            },
            MainMenuBackgroundContent.SHADER_SCENE
        ),
        new MainMenuBackgroundOption(
            MainMenuBackgroundMode.ATARI_SOKOBAN_GLSL,
            "WASD",
            new MainMenuBackgroundSceneFactory()
            {
                public MainMenuBackgroundScene create()
                {
                    return new AtariSokobanBackgroundScene();
                }
            },
            MainMenuBackgroundContent.SHADER_SCENE
        ),
        new MainMenuBackgroundOption(
            MainMenuBackgroundMode.NYAN_CAT_GLSL,
            "Rainbow",
            new MainMenuBackgroundSceneFactory()
            {
                public MainMenuBackgroundScene create()
                {
                    return new NyanCatBackgroundScene();
                }
            },
            MainMenuBackgroundContent.SHADER_SCENE
        ),
        new MainMenuBackgroundOption(
            MainMenuBackgroundMode.KIRBY_JUMP_GLSL,
            "Kirby Jump",
            new MainMenuBackgroundSceneFactory()
            {
                public MainMenuBackgroundScene create()
                {
                    return new KirbyJumpBackgroundScene();
                }
            },
            MainMenuBackgroundContent.SHADER_SCENE
        ),
        new MainMenuBackgroundOption(
            MainMenuBackgroundMode.GLSL_FILE,
            "File",
            new MainMenuBackgroundSceneFactory()
            {
                public MainMenuBackgroundScene create()
                {
                    return new ExternalFileShaderBackgroundScene(UiExtensionManager.getMainMenuBackgroundGlslPath());
                }
            },
            MainMenuBackgroundContent.SHADER_SCENE
        ),
        new MainMenuBackgroundOption(
            MainMenuBackgroundMode.STATIC_IMAGE,
            "Image",
            new MainMenuBackgroundSceneFactory()
            {
                public MainMenuBackgroundScene create()
                {
                    return new PassiveBackgroundScene("Image");
                }
            },
            MainMenuBackgroundContent.STATIC_IMAGE
        ),
        new MainMenuBackgroundOption(
            MainMenuBackgroundMode.VIDEO_STREAM,
            "Video",
            new MainMenuBackgroundSceneFactory()
            {
                public MainMenuBackgroundScene create()
                {
                    return new PassiveBackgroundScene("Video");
                }
            },
            MainMenuBackgroundContent.VIDEO_STREAM
        )
    };

    private static volatile boolean mainMenuReplaceEnabled = true;
    private static volatile boolean loadingReplaceEnabled = true;
    private static volatile boolean splashShaderEnabled = true;
    private static volatile boolean backgroundShaderEnabled = true;
    private static volatile MainMenuBackgroundMode mainMenuBackgroundMode = MainMenuBackgroundMode.FLAPPY_GLSL;
    private static volatile boolean mainMenuGameOnlyEnabled = false;
    private static volatile String mainMenuBackgroundImagePath = "";
    private static volatile String mainMenuBackgroundVideoPath = "";
    private static volatile String mainMenuBackgroundGlslPath = "";
    private static volatile MainMenuFactory mainMenuFactory = DEFAULT_MAIN_MENU_FACTORY;
    private static volatile LoadingScreenFactory loadingFactory = DEFAULT_LOADING_FACTORY;
    private static volatile MainMenuBackgroundSceneFactory mainMenuBackgroundSceneFactory = DEFAULT_MAIN_MENU_BACKGROUND_SCENE_FACTORY;
    private static volatile MainMenuVideoFrameProviderFactory videoFrameProviderFactory = DEFAULT_VIDEO_FRAME_PROVIDER_FACTORY;
    private static volatile String pendingBingBackgroundPath = "";
    private static volatile boolean bingBackgroundApplied;

    static
    {
        initializeBingBackground();
    }

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

    public static boolean isMainMenuBackgroundShaderEnabled()
    {
        return backgroundShaderEnabled && mainMenuBackgroundMode.usesShader();
    }

    public static MainMenuBackgroundMode getMainMenuBackgroundMode()
    {
        return mainMenuBackgroundMode;
    }

    public static void setMainMenuBackgroundMode(MainMenuBackgroundMode mode)
    {
        mainMenuBackgroundMode = mode == null ? MainMenuBackgroundMode.FLAPPY_GLSL : mode;
    }

    public static MainMenuBackgroundOption[] getMainMenuBackgroundOptions()
    {
        return MAIN_MENU_BACKGROUND_OPTIONS.clone();
    }

    public static MainMenuBackgroundOption getMainMenuBackgroundOption(MainMenuBackgroundMode mode)
    {
        MainMenuBackgroundMode resolved = mode == null ? MainMenuBackgroundMode.FLAPPY_GLSL : mode;

        for (MainMenuBackgroundOption option : MAIN_MENU_BACKGROUND_OPTIONS)
        {
            if (option.mode() == resolved)
            {
                return option;
            }
        }

        return null;
    }

    public static String getMainMenuBackgroundImagePath()
    {
        return mainMenuBackgroundImagePath;
    }

    public static String getMainMenuBackgroundVideoPath()
    {
        return mainMenuBackgroundVideoPath;
    }

    public static String getMainMenuBackgroundGlslPath()
    {
        return mainMenuBackgroundGlslPath;
    }

    public static boolean isMainMenuGameOnlyEnabled()
    {
        return mainMenuGameOnlyEnabled;
    }

    public static void setMainMenuGameOnlyEnabled(boolean enabled)
    {
        mainMenuGameOnlyEnabled = enabled;
    }

    public static MainMenuBackgroundScene createMainMenuBackgroundScene()
    {
        MainMenuBackgroundOption option = getMainMenuBackgroundOption(mainMenuBackgroundMode);

        if (option != null && option.sceneFactory() != null)
        {
            return createMainMenuBackgroundSceneSafely(option.sceneFactory());
        }

        return createMainMenuBackgroundSceneSafely(mainMenuBackgroundSceneFactory);
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

    public static void setMainMenuBackgroundShaderEnabled(boolean enabled)
    {
        backgroundShaderEnabled = enabled;
    }

    public static void setMainMenuFactory(MainMenuFactory factory)
    {
        mainMenuFactory = factory == null ? DEFAULT_MAIN_MENU_FACTORY : factory;
    }

    public static void setLoadingFactory(LoadingScreenFactory factory)
    {
        loadingFactory = factory == null ? DEFAULT_LOADING_FACTORY : factory;
    }

    public static void setMainMenuBackgroundSceneFactory(MainMenuBackgroundSceneFactory factory)
    {
        mainMenuBackgroundSceneFactory = factory == null ? DEFAULT_MAIN_MENU_BACKGROUND_SCENE_FACTORY : factory;
    }

    public static void setMainMenuBackgroundImagePath(String path)
    {
        mainMenuBackgroundImagePath = path == null ? "" : path.trim();
    }

    public static void setMainMenuBackgroundVideoPath(String path)
    {
        mainMenuBackgroundVideoPath = path == null ? "" : path.trim();
    }

    public static void setMainMenuBackgroundGlslPath(String path)
    {
        mainMenuBackgroundGlslPath = path == null ? "" : path.trim();
    }

    public static void setMainMenuVideoFrameProviderFactory(MainMenuVideoFrameProviderFactory factory)
    {
        videoFrameProviderFactory = factory == null ? DEFAULT_VIDEO_FRAME_PROVIDER_FACTORY : factory;
    }

    private static void initializeBingBackground()
    {
        try
        {
            if (mainMenuBackgroundGlslPath.isEmpty())
            {
                File builtin = new File("glsl/mainmenu_custom.glsl");

                if (builtin.isFile())
                {
                    mainMenuBackgroundGlslPath = builtin.getAbsolutePath();
                    LOGGER.info("Configured default main-menu GLSL file: {}", mainMenuBackgroundGlslPath);
                }
            }

            BingWallpaperFetcher.downloadOnceAsync(new BingWallpaperFetcher.Callback()
            {
                public void onComplete(String localPath)
                {
                    if (localPath != null && !localPath.isEmpty())
                    {
                        pendingBingBackgroundPath = localPath;
                        LOGGER.info("Bing wallpaper downloaded in background: {}", localPath);
                    }
                }
            });
        }
        catch (Throwable throwable)
        {
            LOGGER.warn("Failed to initialize Bing wallpaper. Falling back to default background.", throwable);
        }
    }

    public static boolean applyPendingBingBackgroundIfReady()
    {
        if (bingBackgroundApplied)
        {
            return false;
        }

        String path = pendingBingBackgroundPath;

        if (path == null || path.isEmpty())
        {
            return false;
        }

        mainMenuBackgroundMode = MainMenuBackgroundMode.STATIC_IMAGE;
        mainMenuBackgroundImagePath = path;
        backgroundShaderEnabled = false;
        pendingBingBackgroundPath = "";
        bingBackgroundApplied = true;
        LOGGER.info("Applied Bing wallpaper to main menu: {}", path);
        return true;
    }

    public static MainMenuVideoFrameProvider createMainMenuVideoFrameProvider(String sourcePath)
    {
        try
        {
            MainMenuVideoFrameProvider provider = videoFrameProviderFactory == null ? null : videoFrameProviderFactory.create(sourcePath);
            return provider;
        }
        catch (Throwable throwable)
        {
            LOGGER.warn("Main-menu video frame provider factory failed.", throwable);
            return null;
        }
    }

    public static void resetFactories()
    {
        mainMenuFactory = DEFAULT_MAIN_MENU_FACTORY;
        loadingFactory = DEFAULT_LOADING_FACTORY;
        mainMenuBackgroundSceneFactory = DEFAULT_MAIN_MENU_BACKGROUND_SCENE_FACTORY;
        videoFrameProviderFactory = DEFAULT_VIDEO_FRAME_PROVIDER_FACTORY;
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

    private static MainMenuBackgroundScene createMainMenuBackgroundSceneSafely(MainMenuBackgroundSceneFactory factory)
    {
        try
        {
            MainMenuBackgroundScene scene = factory == null ? null : factory.create();
            return scene == null ? new FlappyBirdBackgroundScene() : scene;
        }
        catch (Throwable throwable)
        {
            LOGGER.warn("Main menu background scene factory failed, falling back to FlappyBirdBackgroundScene.", throwable);
            return new FlappyBirdBackgroundScene();
        }
    }

}
