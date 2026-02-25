package dwgx.ui.ext;

/**
 * Base class for shader-only background scenes that need no input,
 * tick, or uniform logic. Subclasses only provide fragmentShaderSource()
 * and getOverlayHint().
 */
public abstract class AbstractShaderScene implements MainMenuBackgroundScene
{
    public MainMenuSplashShader.TextureAlphaMode textureAlphaMode()
    {
        return MainMenuSplashShader.TextureAlphaMode.IGNORE_SOURCE_ALPHA;
    }

    public void initialize()
    {
    }

    public boolean handleKeyInput(char typedChar, int keyCode, boolean sceneEnabled)
    {
        return false;
    }

    public void tick(boolean sceneEnabled)
    {
    }

    public void applyShaderUniforms(MainMenuSplashShader shader)
    {
    }
}
