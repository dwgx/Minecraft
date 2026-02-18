package dwgx.nano;

public final class NanoThemes
{
    private NanoThemes()
    {
    }

    public static NanoTheme create(NanoPalette palette, int panelAlpha, int backdropAlpha, float cornerRadius, Integer accentOverrideArgb)
    {
        int accent = accentOverrideArgb == null ? paletteAccent(palette) : NanoRenderUtils.withAlpha(accentOverrideArgb.intValue(), 255);
        int uiAlpha = NanoRenderUtils.clamp255(panelAlpha);
        int shellAlpha = NanoRenderUtils.clamp255(uiAlpha + 10);
        int sectionAlpha = NanoRenderUtils.clamp255(Math.max(92, uiAlpha - 14));
        int rowAlpha = NanoRenderUtils.clamp255(Math.max(84, uiAlpha - 12));
        int controlAlpha = NanoRenderUtils.clamp255(Math.max(110, uiAlpha - 6));
        int overlayAlpha = NanoRenderUtils.clamp255(backdropAlpha);

        int windowTop = NanoRenderUtils.withAlpha(0xFF0E1823, shellAlpha);
        int windowBottom = NanoRenderUtils.withAlpha(0xFF0A141D, shellAlpha);
        int windowBorder = NanoRenderUtils.withAlpha(blend(0xFF263344, accent, 0.10F), NanoRenderUtils.clamp255(shellAlpha - 8));
        int sidebar = NanoRenderUtils.withAlpha(0xFF0C151F, sectionAlpha);
        int main = NanoRenderUtils.withAlpha(0xFF0F1B26, sectionAlpha);
        int card = NanoRenderUtils.withAlpha(0xFF122130, NanoRenderUtils.clamp255(sectionAlpha + 6));
        int cardAlt = NanoRenderUtils.withAlpha(0xFF101C29, NanoRenderUtils.clamp255(sectionAlpha + 2));
        int row = NanoRenderUtils.withAlpha(0xFF1A2736, rowAlpha);
        int rowHover = NanoRenderUtils.withAlpha(0xFF243447, NanoRenderUtils.clamp255(rowAlpha + 18));
        int rowSelected = NanoRenderUtils.withAlpha(blend(0xFF203245, accent, 0.22F), NanoRenderUtils.clamp255(rowAlpha + 22));
        int control = NanoRenderUtils.withAlpha(0xFF223142, controlAlpha);
        int controlHover = NanoRenderUtils.withAlpha(0xFF2A3D54, NanoRenderUtils.clamp255(controlAlpha + 16));
        int controlActive = NanoRenderUtils.withAlpha(blend(0xFF244975, accent, 0.58F), NanoRenderUtils.clamp255(controlAlpha + 18));
        int text = 0xFFE8EEF8;
        int textMuted = 0xFFB4C0D0;
        int textWeak = 0xFF7D8B9E;
        int accentSoft = NanoRenderUtils.withAlpha(accent, 136);
        int success = 0xFF58CC8E;
        int danger = 0xFFC96874;
        int backdrop = NanoRenderUtils.withAlpha(0xFF02060B, overlayAlpha);

        float stableCorner = Math.max(6.0F, Math.min(26.0F, cornerRadius));
        float windowRadius = stableCorner + 1.6F;
        float surfaceRadius = stableCorner - 0.2F;
        float cardRadius = stableCorner - 1.0F;
        float controlRadius = Math.max(4.4F, stableCorner - 3.1F);
        return new NanoTheme(backdrop, windowTop, windowBottom, windowBorder, sidebar, main, card, cardAlt, row, rowHover, rowSelected, control, controlHover, controlActive, text, textMuted, textWeak, accent, accentSoft, success, danger, windowRadius, surfaceRadius, cardRadius, controlRadius);
    }

    private static int paletteAccent(NanoPalette palette)
    {
        switch (palette)
        {
            case MINT:
                return 0xFF2FBF90;

            case AMBER:
                return 0xFFCE9740;

            case RUBY:
                return 0xFFBF5B72;

            case MONO:
                return 0xFF7B8EA3;

            case COBALT:
            default:
                return 0xFF1E6EFF;
        }
    }

    private static int blend(int baseArgb, int mixArgb, float t)
    {
        float ratio = Math.max(0.0F, Math.min(1.0F, t));
        int br = baseArgb >>> 16 & 255;
        int bg = baseArgb >>> 8 & 255;
        int bb = baseArgb & 255;
        int mr = mixArgb >>> 16 & 255;
        int mg = mixArgb >>> 8 & 255;
        int mb = mixArgb & 255;
        int r = Math.round((float)br + (float)(mr - br) * ratio);
        int g = Math.round((float)bg + (float)(mg - bg) * ratio);
        int b = Math.round((float)bb + (float)(mb - bb) * ratio);
        return 0xFF000000 | (NanoRenderUtils.clamp255(r) & 255) << 16 | (NanoRenderUtils.clamp255(g) & 255) << 8 | NanoRenderUtils.clamp255(b) & 255;
    }
}
