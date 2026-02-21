package client.ui.template;

import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoUi;
import org.lwjgl.system.MemoryStack;

/**
 * Shared sliding selection box used by list-like controls.
 */
public final class UiSelectionBox
{
    private UiSelectionBox()
    {
    }

    public static void draw(
        long vg,
        MemoryStack stack,
        String key,
        boolean targetVisible,
        float visibility,
        float targetX,
        float targetY,
        float targetW,
        float targetH,
        float radius,
        NanoTheme theme,
        UiAnimProfile profile,
        float speed
    )
    {
        if (vg == 0L || stack == null || theme == null || key == null || key.isEmpty())
        {
            return;
        }

        UiAnimProfile animProfile = profile == null ? UiAnimProfile.defaults() : profile;
        float resolvedSpeed = UiMotion.clamp(speed, 0.05F, 1.0F);
        boolean validTarget = targetVisible && targetW > 0.001F && targetH > 0.001F;
        float visible = UiControlAnimations.presence(key, validTarget && visibility > 0.01F, animProfile, resolvedSpeed);
        float alpha = UiMotion.clamp01(visible * UiMotion.clamp01(visibility));

        if (alpha <= 0.001F || !validTarget)
        {
            return;
        }

        float x = UiAnimationBus.animateWithSpeed(key + ".x", targetX, animProfile, resolvedSpeed);
        float y = UiAnimationBus.animateWithSpeed(key + ".y", targetY, animProfile, resolvedSpeed);
        float w = UiAnimationBus.animateWithSpeed(key + ".w", targetW, animProfile, resolvedSpeed);
        float h = UiAnimationBus.animateWithSpeed(key + ".h", targetH, animProfile, resolvedSpeed);
        float drawRadius = Math.max(0.0F, Math.min(h * 0.5F, radius));
        int fill = NanoRenderUtils.mulAlpha(theme.accentSoftArgb(), alpha * 0.38F);
        int border = NanoRenderUtils.mulAlpha(theme.accentArgb(), alpha * 0.70F);
        NanoUi.drawSurface(vg, stack, x, y, w, h, drawRadius, fill, border);
    }
}
