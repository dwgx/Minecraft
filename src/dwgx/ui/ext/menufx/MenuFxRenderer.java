package dwgx.ui.ext.menufx;

import client.module.impl.client.ClickGuiModule;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiAnimProfiles;
import client.ui.template.UiControlAnimations;
import client.ui.template.UiMotion;
import client.ui.template.UiSelectionBox;
import client.ui.template.UiStateToggle;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoUi;
import dwgx.ui.ext.UiExtensionManager;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.MemoryStack.stackPush;

public final class MenuFxRenderer
{
    private static final float TOGGLE_HEIGHT = 28.0F;
    private static final float PANEL_SHORT_FOOTER_MIN_W = 240.0F;

    private MenuFxRenderer()
    {
    }

    public static void render(
        long vg,
        int mouseX,
        int mouseY,
        MenuFxState state,
        MenuFxLayout layout,
        ClickGuiModule clickGui,
        UiAnimProfile animProfile,
        NanoTheme theme,
        UiExtensionManager.MainMenuBackgroundMode mode,
        boolean gameOnlyEnabled
    )
    {
        if (vg == 0L || state == null || layout == null || animProfile == null || theme == null)
        {
            return;
        }

        float open = UiMotion.clamp01(UiControlAnimations.open("mainmenu.ext.panel", state.isExpanded(), animProfile));
        float presenceSpeed = UiMotion.clamp(animProfile.controlSpeed() * 1.12F + 0.08F, 0.05F, 1.0F);
        float presence = UiControlAnimations.presence("mainmenu.ext.panel", state.isExpanded(), animProfile, presenceSpeed);
        float panelProgress = UiMotion.clamp01(open * 0.60F + presence * 0.40F);
        boolean toggleHovered = layout.toggle().contains(mouseX, mouseY);
        float toggleHover = UiControlAnimations.hover("mainmenu.ext.toggle", toggleHovered, animProfile);

        try (MemoryStack stack = stackPush())
        {
            NanoFontBook.ensureLoaded(vg);
            int regular = NanoFontBook.uiRegular();
            int bold = NanoFontBook.uiBold();
            drawToggle(vg, stack, theme, regular, layout.toggle(), toggleHover, open);

            if (!state.isExpanded() || panelProgress <= 0.001F)
            {
                return;
            }

            float yOffset = (1.0F - panelProgress) * 7.0F;
            MenuFxLayout.Rect panel = layout.panel();
            int panelFill = NanoRenderUtils.mulAlpha(theme.cardArgb(), panelProgress);
            int panelBorder = NanoRenderUtils.mulAlpha(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 105), panelProgress);
            NanoUi.drawSurface(vg, stack, panel.x(), panel.y() + yOffset, panel.w(), panel.h(), theme.cardRadius(), panelFill, panelBorder);
            NanoUi.drawLeftText(vg, stack, bold, panel.x() + scaled(11.0F, layout.scale()), panel.y() + scaled(14.0F, layout.scale()) + yOffset, scaled(11.0F, layout.scale()), NanoRenderUtils.mulAlpha(theme.textArgb(), panelProgress), "Background Scene");
            NanoUi.drawRightText(vg, stack, regular, panel.x2() - scaled(10.0F, layout.scale()), panel.y() + scaled(14.0F, layout.scale()) + yOffset, scaled(9.3F, layout.scale()), NanoRenderUtils.mulAlpha(theme.textWeakArgb(), panelProgress), "MenuFX");

            float rowSpeed = UiMotion.clamp(animProfile.controlSpeed() * 1.10F + 0.07F, 0.05F, 1.0F);
            float scrollSpeed = UiMotion.clamp(UiAnimProfiles.listSpeed(clickGui, animProfile), 0.05F, 1.0F);
            MenuFxController.clampScroll(state, layout);

            if (animProfile.isEnabled())
            {
                state.setScrollVisual(client.ui.template.UiAnimationBus.animateWithSpeed("mainmenu.ext.scroll", (float)state.getScrollRows(), animProfile, scrollSpeed));
            }
            else
            {
                state.setScrollVisual((float)state.getScrollRows());
            }

            float maxScrollRows = (float)MenuFxController.maxScroll(layout);
            state.setScrollVisual(UiMotion.clamp(state.getScrollVisual(), 0.0F, maxScrollRows));
            float scrollOffset = state.getScrollVisual() * layout.rowStep();
            MenuFxLayout.Rect modeSelectionRow = null;
            float modeSelectionReveal = 0.0F;
            MenuFxLayout.Rect rowsClip = layout.rowsClip().offset(0.0F, yOffset);

            NanoUi.beginClip(vg, rowsClip.x(), rowsClip.y(), rowsClip.w(), rowsClip.h());

            UiExtensionManager.MainMenuBackgroundOption[] options = layout.backgroundOptions();
            MenuFxLayout.Rect[] rows = layout.modeRows();

            for (int i = 0; i < options.length; ++i)
            {
                UiExtensionManager.MainMenuBackgroundOption option = options[i];
                boolean modeSelected = mode == option.mode();
                MenuFxLayout.Rect shiftedRow = rows[i].offset(0.0F, -scrollOffset);

                if (!shiftedRow.intersects(rowsClip))
                {
                    continue;
                }

                int staggerIndex = Math.max(0, i - (int)Math.floor((double)state.getScrollVisual()));
                float rowReveal = UiControlAnimations.stagger("mainmenu.ext.rows", panelProgress, staggerIndex, 0.08F, animProfile, rowSpeed);
                drawRow(vg, stack, animProfile, theme, regular, state.isExpanded(), mouseX, mouseY, shiftedRow, yOffset, rowReveal, option.mode().id(), option.mode().displayName(), modeSelected, false, option.controlHint());

                if (modeSelected)
                {
                    modeSelectionRow = resolveRowVisualRect(shiftedRow, yOffset, rowReveal);
                    modeSelectionReveal = rowReveal;
                }
            }

            MenuFxLayout.Rect shiftedGameOnlyRow = layout.rowGameOnly().offset(0.0F, -scrollOffset);
            int gameOnlyStagger = Math.max(0, options.length - (int)Math.floor((double)state.getScrollVisual()));
            float gameOnlyReveal = UiControlAnimations.stagger("mainmenu.ext.rows", panelProgress, gameOnlyStagger, 0.08F, animProfile, rowSpeed);

            if (shiftedGameOnlyRow.intersects(rowsClip))
            {
                drawRow(vg, stack, animProfile, theme, regular, state.isExpanded(), mouseX, mouseY, shiftedGameOnlyRow, yOffset, gameOnlyReveal, "game_only", "Game-only UI", gameOnlyEnabled, true, gameOnlyEnabled ? "ON" : "OFF");
            }

            NanoUi.endClip(vg);

            if (modeSelectionRow != null)
            {
                float selectionSpeed = UiAnimProfiles.selectionSpeed(clickGui, animProfile);
                float selectionVisibility = UiMotion.clamp01(panelProgress * modeSelectionReveal);
                float selectionRadius = Math.min(modeSelectionRow.h() * 0.5F, theme.controlRadius());
                UiSelectionBox.draw(
                    vg,
                    stack,
                    "mainmenu.ext.mode.selection",
                    selectionVisibility > 0.01F,
                    selectionVisibility,
                    modeSelectionRow.x(),
                    modeSelectionRow.y(),
                    modeSelectionRow.w(),
                    modeSelectionRow.h(),
                    selectionRadius,
                    theme,
                    animProfile,
                    selectionSpeed
                );
            }

            float footerX = panel.x() + scaled(10.0F, layout.scale());
            float footerY = panel.y2() - scaled(10.0F, layout.scale()) + yOffset;
            float footerWidth = Math.max(0.0F, panel.w() - scaled(20.0F, layout.scale()));
            boolean scrollable = MenuFxController.maxScroll(layout) > 0;
            String footerText = scrollable ? "Mouse wheel to scroll mode list." : (panel.w() >= scaled(PANEL_SHORT_FOOTER_MIN_W, layout.scale()) ? "Hide menu chrome and keep only game + switcher." : "Keep only game + switcher.");
            NanoUi.beginClip(vg, footerX - 1.0F, panel.y() + yOffset + 1.0F, footerWidth, Math.max(1.0F, panel.h() - 2.0F));
            NanoUi.drawLeftText(vg, stack, regular, footerX, footerY, scaled(9.1F, layout.scale()), NanoRenderUtils.mulAlpha(theme.textWeakArgb(), panelProgress), footerText);
            NanoUi.endClip(vg);
        }
    }

    private static void drawToggle(long vg, MemoryStack stack, NanoTheme theme, int regular, MenuFxLayout.Rect toggle, float hover, float open)
    {
        float k = UiMotion.clamp(toggle.h() / TOGGLE_HEIGHT, 0.68F, 1.18F);
        int toggleFill = mixArgb(theme.controlArgb(), theme.controlHoverArgb(), 0.22F + hover * 0.78F);
        int border = mixArgb(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 92), NanoRenderUtils.withAlpha(theme.accentArgb(), 132), hover * 0.45F);
        NanoUi.drawSurface(vg, stack, toggle.x(), toggle.y(), toggle.w(), toggle.h(), theme.controlRadius(), toggleFill, border);
        NanoUi.drawLeftText(vg, stack, regular, toggle.x() + scaled(10.0F, k), toggle.y() + toggle.h() * 0.5F, scaled(9.2F, k), theme.textArgb(), "Menu FX");
        NanoUi.drawRightText(vg, stack, regular, toggle.x2() - scaled(10.0F, k), toggle.y() + toggle.h() * 0.5F, scaled(9.0F, k), theme.textWeakArgb(), open > 0.5F ? "-" : "+");
    }

    private static void drawRow(
        long vg,
        MemoryStack stack,
        UiAnimProfile animProfile,
        NanoTheme theme,
        int regular,
        boolean expanded,
        int mouseX,
        int mouseY,
        MenuFxLayout.Rect row,
        float yOffset,
        float reveal,
        String key,
        String label,
        boolean active,
        boolean booleanRow,
        String rightLabel
    )
    {
        float alpha = UiMotion.clamp01(reveal);
        boolean hovered = expanded && alpha > 0.97F && row.contains(mouseX, mouseY);
        float hover = UiControlAnimations.hover("mainmenu.ext.row." + key, hovered, animProfile);
        int base = active ? theme.rowSelectedArgb() : theme.rowArgb();
        int fill = mixArgb(base, theme.rowHoverArgb(), hover);
        int border = active ? NanoRenderUtils.withAlpha(theme.accentArgb(), 140) : NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 75);
        int finalFill = NanoRenderUtils.mulAlpha(fill, alpha);
        int finalBorder = NanoRenderUtils.mulAlpha(border, alpha);
        MenuFxLayout.Rect visualRow = resolveRowVisualRect(row, yOffset, reveal);
        float x = visualRow.x();
        float y = visualRow.y();
        float width = visualRow.w();
        float textScale = UiMotion.clamp(visualRow.h() / 24.0F, 0.58F, 1.05F);
        float leftSize = scaled(9.8F, textScale);
        float rightSize = scaled(9.0F, textScale);
        NanoUi.drawSurface(vg, stack, x, y, width, visualRow.h(), theme.controlRadius(), finalFill, finalBorder);

        if (active)
        {
            NanoUi.drawAccentFlag(vg, stack, x + scaled(4.5F, textScale), y + scaled(4.0F, textScale), scaled(2.4F, textScale), Math.max(scaled(2.0F, textScale), visualRow.h() - scaled(8.0F, textScale)), NanoRenderUtils.mulAlpha(theme.accentArgb(), alpha));
        }

        float labelX = x + (active ? scaled(13.0F, textScale) : scaled(10.0F, textScale));
        String safeRightLabel = rightLabel == null ? "" : rightLabel;
        float rightTextWidth = NanoRenderUtils.textWidth(vg, regular, rightSize, safeRightLabel);
        float boolReserve = booleanRow ? Math.min(width * 0.56F, scaled(92.0F, textScale)) : 0.0F;
        float rightReserve = booleanRow ? boolReserve : (safeRightLabel.isEmpty() ? 0.0F : Math.min(width * 0.38F, rightTextWidth + scaled(12.0F, textScale)));
        float leftClipWidth = Math.max(scaled(30.0F, textScale), width - (labelX - x) - rightReserve - scaled(8.0F, textScale));
        NanoUi.beginClip(vg, labelX - 1.0F, y + 1.0F, leftClipWidth, Math.max(2.0F, visualRow.h() - 2.0F));
        NanoUi.drawLeftText(vg, stack, regular, labelX, y + visualRow.h() * 0.5F, leftSize, NanoRenderUtils.mulAlpha(theme.textArgb(), alpha), label);
        NanoUi.endClip(vg);

        if (booleanRow)
        {
            float k = UiMotion.clamp(visualRow.h() / 24.0F, 0.56F, 1.35F);
            float valueMinW = scaled(62.0F, k);
            float valueCapW = Math.max(valueMinW, width - scaled(12.0F, k));
            float valueW = UiMotion.clamp(scaled(88.0F, k), valueMinW, valueCapW);
            float valueH = visualRow.h() - scaled(3.0F, k);
            float valueX = x + width - scaled(8.0F, k) - valueW;
            float valueY = y + scaled(1.5F, k);
            boolean toggleHovered = hovered && (float)mouseX >= valueX && (float)mouseX <= valueX + valueW && (float)mouseY >= valueY && (float)mouseY <= valueY + valueH;
            UiStateToggle.draw(vg, stack, valueX, valueY, valueW, valueH, active, toggleHovered, theme, animProfile, "clickgui.toggle.mainmenu." + key, regular, k, "DISABLE", "ENABLE");
            return;
        }

        if (!safeRightLabel.isEmpty() && width >= scaled(120.0F, textScale))
        {
            int rightColor = theme.textWeakArgb();
            float rightPadding = scaled(7.0F, textScale);
            float rightClipWidth = Math.max(scaled(16.0F, textScale), width * 0.36F);
            float rightClipX = x + width - rightClipWidth - rightPadding;
            NanoUi.beginClip(vg, rightClipX, y + 1.0F, rightClipWidth, Math.max(2.0F, visualRow.h() - 2.0F));
            NanoUi.drawRightText(vg, stack, regular, x + width - rightPadding, y + visualRow.h() * 0.5F, rightSize, NanoRenderUtils.mulAlpha(rightColor, alpha), safeRightLabel);
            NanoUi.endClip(vg);
        }
    }

    private static MenuFxLayout.Rect resolveRowVisualRect(MenuFxLayout.Rect row, float yOffset, float reveal)
    {
        float alpha = UiMotion.clamp01(reveal);
        float shiftX = (1.0F - alpha) * 5.0F;
        float shiftY = (1.0F - alpha) * 1.8F;
        float x = row.x() + shiftX;
        float y = row.y() + yOffset + shiftY;
        float w = Math.max(0.0F, row.w() - shiftX);
        return new MenuFxLayout.Rect(x, y, w, row.h());
    }

    private static int mixArgb(int from, int to, float ratio)
    {
        float t = UiMotion.clamp01(ratio);
        int ar = from >>> 16 & 255;
        int ag = from >>> 8 & 255;
        int ab = from & 255;
        int aa = from >>> 24 & 255;
        int br = to >>> 16 & 255;
        int bg = to >>> 8 & 255;
        int bb = to & 255;
        int ba = to >>> 24 & 255;
        int r = Math.round((float)ar + (float)(br - ar) * t);
        int g = Math.round((float)ag + (float)(bg - ag) * t);
        int b = Math.round((float)ab + (float)(bb - ab) * t);
        int a = Math.round((float)aa + (float)(ba - aa) * t);
        return (NanoRenderUtils.clamp255(a) & 255) << 24 | (NanoRenderUtils.clamp255(r) & 255) << 16 | (NanoRenderUtils.clamp255(g) & 255) << 8 | NanoRenderUtils.clamp255(b) & 255;
    }

    private static float scaled(float value, float scale)
    {
        return value * scale;
    }
}
