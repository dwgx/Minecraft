package dwgx.ui.ext;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * External fragment-shader backed scene.
 *
 * The file is polled and hot-reloaded while main menu is open, so shader edits
 * can be previewed without restarting the client.
 */
public final class ExternalFileShaderBackgroundScene implements MainMenuBackgroundScene
{
    private static final Logger LOGGER = LogManager.getLogger(ExternalFileShaderBackgroundScene.class);
    private static final long RELOAD_INTERVAL_MS = 350L;

    private final String shaderFilePath;
    private String cachedSource;
    private long cachedLastModified = Long.MIN_VALUE;
    private long nextReloadCheckAtMs;
    private boolean missingWarned;
    private boolean emptyWarned;

    public ExternalFileShaderBackgroundScene(String shaderFilePath)
    {
        this.shaderFilePath = shaderFilePath == null ? "" : shaderFilePath.trim();
    }

    public String fragmentShaderSource()
    {
        this.refreshSource(true);
        return this.cachedSource;
    }

    public MainMenuSplashShader.TextureAlphaMode textureAlphaMode()
    {
        return MainMenuSplashShader.TextureAlphaMode.IGNORE_SOURCE_ALPHA;
    }

    public void initialize()
    {
        this.refreshSource(true);
    }

    public boolean handleKeyInput(char typedChar, int keyCode, boolean sceneEnabled)
    {
        return false;
    }

    public void tick(boolean sceneEnabled)
    {
        if (!sceneEnabled)
        {
            return;
        }

        this.refreshSource(false);
    }

    public void applyShaderUniforms(MainMenuSplashShader shader)
    {
    }

    public String getOverlayHint(boolean sceneEnabled)
    {
        if (!sceneEnabled)
        {
            return null;
        }

        if (this.shaderFilePath.isEmpty())
        {
            return "GLSL Built-in: CRT Terminal";
        }

        File file = new File(this.shaderFilePath);

        if (!file.isFile())
        {
            return "GLSL File missing: " + file.getPath();
        }

        return "GLSL File: " + file.getName();
    }

    private void refreshSource(boolean force)
    {
        if (this.shaderFilePath.isEmpty())
        {
            this.cachedSource = BuiltinMainMenuGlslShader.source();
            this.cachedLastModified = Long.MIN_VALUE;
            return;
        }

        long now = System.currentTimeMillis();

        if (!force && now < this.nextReloadCheckAtMs)
        {
            return;
        }

        this.nextReloadCheckAtMs = now + RELOAD_INTERVAL_MS;
        File file = new File(this.shaderFilePath);

        if (!file.isFile())
        {
            this.cachedSource = BuiltinMainMenuGlslShader.source();
            this.cachedLastModified = Long.MIN_VALUE;

            if (!this.missingWarned)
            {
                this.missingWarned = true;
                LOGGER.warn("Main-menu external GLSL file is missing: {}, fallback to built-in shader.", file.getPath());
            }

            return;
        }

        long modified = file.lastModified();

        if (!force && this.cachedSource != null && this.cachedLastModified == modified)
        {
            return;
        }

        try
        {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String source = new String(bytes, StandardCharsets.UTF_8);

            if (source.trim().isEmpty())
            {
                if (!this.emptyWarned)
                {
                    this.emptyWarned = true;
                    LOGGER.warn("Main-menu external GLSL file is empty: {}", file.getPath());
                }

                this.cachedSource = BuiltinMainMenuGlslShader.source();
                this.cachedLastModified = modified;
                this.missingWarned = false;
                return;
            }

            this.cachedSource = source;
            this.cachedLastModified = modified;
            this.missingWarned = false;
            this.emptyWarned = false;
        }
        catch (Throwable throwable)
        {
            LOGGER.warn("Failed to load main-menu external GLSL file: {}", file.getPath(), throwable);
        }
    }
}
