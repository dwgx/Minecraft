package dwgx.ui.ext;

/**
 * No-op scene used by non-shader backgrounds (image/video) so menu lifecycle
 * remains unified.
 */
public final class PassiveBackgroundScene implements MainMenuBackgroundScene
{
    private final String label;

    public PassiveBackgroundScene(String label)
    {
        this.label = label == null ? "Passive" : label;
    }

    public String fragmentShaderSource()
    {
        return null;
    }

    public MainMenuSplashShader.TextureAlphaMode textureAlphaMode()
    {
        return MainMenuSplashShader.TextureAlphaMode.USE_SOURCE_ALPHA;
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

    public String getOverlayHint(boolean sceneEnabled)
    {
        return null;
    }

    public String toString()
    {
        return "PassiveBackgroundScene{" + this.label + "}";
    }
}
