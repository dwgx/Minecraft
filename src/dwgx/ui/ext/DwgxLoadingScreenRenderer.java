package dwgx.ui.ext;

import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.client.Minecraft;

/**
 * Replaceable loading screen entry point.
 * Keep rendering behavior aligned with vanilla by default and expose small hooks for future customization.
 */
public class DwgxLoadingScreenRenderer extends LoadingScreenRenderer
{
    public DwgxLoadingScreenRenderer(Minecraft mcIn)
    {
        super(mcIn);
    }

    public void resetProgressAndMessage(String message)
    {
        super.resetProgressAndMessage(this.normalizeMessage(message));
    }

    public void displaySavingString(String message)
    {
        super.displaySavingString(this.normalizeMessage(message));
    }

    public void displayLoadingString(String message)
    {
        super.displayLoadingString(this.normalizeMessage(message));
    }

    public void setLoadingProgress(int progress)
    {
        super.setLoadingProgress(this.normalizeProgress(progress));
    }

    protected String normalizeMessage(String message)
    {
        return message == null ? "" : message;
    }

    protected int normalizeProgress(int progress)
    {
        return progress;
    }
}
