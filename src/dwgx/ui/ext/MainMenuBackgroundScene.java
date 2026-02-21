package dwgx.ui.ext;

/**
 * Main-menu background scene contract.
 * Implementations can provide gameplay state, input handling, shader uniforms and overlay hints.
 */
public interface MainMenuBackgroundScene
{
    /**
     * Fragment shader source used for this scene.
     */
    String fragmentShaderSource();

    /**
     * Texture alpha mode used by this scene pass.
     */
    MainMenuSplashShader.TextureAlphaMode textureAlphaMode();

    /**
     * Initialize internal state on first menu open.
     */
    void initialize();

    /**
     * Handle menu key input.
     *
     * @return true if the input was consumed by scene logic.
     */
    boolean handleKeyInput(char typedChar, int keyCode, boolean sceneEnabled);

    /**
     * Advance scene state once per frame.
     */
    void tick(boolean sceneEnabled);

    /**
     * Push uniforms to shader before drawing background.
     */
    void applyShaderUniforms(MainMenuSplashShader shader);

    /**
     * Optional hint text shown at top-left of main menu.
     *
     * @return hint text or null when no hint should be drawn.
     */
    String getOverlayHint(boolean sceneEnabled);
}
