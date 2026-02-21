package dwgx.ui.ext;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;

/**
 * Optional custom renderer hook for complex backgrounds that do not fit the single-pass shader model.
 */
public interface MainMenuBackgroundCustomRenderer
{
    /**
     * @return true if the scene rendered a full background this frame; false to let runtime fallback paths continue.
     */
    boolean renderCustomBackground(Minecraft mc, int screenWidth, int screenHeight, float zLevel, Logger logger);

    void closeCustomBackground();
}
