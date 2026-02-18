package client.hud;

import client.core.ClientBootstrap;
import client.module.Module;
import client.module.impl.client.ClickGuiModule;
import client.render.RenderContext2D;
import client.ui.NanoRenderableScreen;
import client.ui.template.NanoSliderController;
import client.ui.template.UiAnimation;
import client.ui.template.UiMotion;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoPalette;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoThemes;
import dwgx.nano.NanoUi;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Minimal HUD edit mode:
 * - Left click + drag background to move selected element.
 * - Right click HUD element to open a small parameter panel near cursor.
 */
public class HudEditorScreen extends GuiScreen implements NanoRenderableScreen
{
    private static final float OFFSET_MIN = -600.0F;
    private static final float OFFSET_MAX = 600.0F;
    private static final float OFFSET_STEP = 5.0F;
    private static final float SCALE_MIN = 0.20F;
    private static final float SCALE_MAX = 3.00F;
    private static final float SCALE_STEP = 0.05F;
    private static final float PANEL_WIDTH = 300.0F;
    private static final float PANEL_HEIGHT = 206.0F;
    private static final float PANEL_MARGIN = 8.0F;
    private static final float ELEMENT_HIT_EXPAND_X = 8.0F;
    private static final float ELEMENT_HIT_EXPAND_Y = 6.0F;

    private final HudManager hudManager;
    private final SelectionModel selection = new SelectionModel();
    private final SnapGrid grid = new SnapGrid(4);

    private int mouseX;
    private int mouseY;
    private long lastSliderDragNanos;

    private boolean draggingElement;
    private int dragStartMouseX;
    private int dragStartMouseY;
    private float dragStartOffsetX;
    private float dragStartOffsetY;

    private boolean panelOpen;
    private float panelX;
    private float panelY;
    private boolean draggingOffsetX;
    private boolean draggingOffsetY;
    private boolean draggingScale;

    public HudEditorScreen(HudManager hudManager)
    {
        this.hudManager = hudManager;
    }

    public HudManager getHudManager()
    {
        return this.hudManager;
    }

    public SelectionModel getSelection()
    {
        return this.selection;
    }

    public SnapGrid getGrid()
    {
        return this.grid;
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }

    public void initGui()
    {
        this.selection.clear();

        if (this.panelX <= 0.0F || this.panelY <= 0.0F)
        {
            this.panelX = (float)this.width - PANEL_WIDTH - 20.0F;
            this.panelY = 20.0F;
        }

        this.clampPanelToScreen();
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == 1)
        {
            this.mc.displayGuiScreen((GuiScreen)null);
            return;
        }

        HudElement selected = this.selection.getSelected();

        if (selected != null)
        {
            float step = isShiftKeyDown() ? 1.0F : OFFSET_STEP;

            if (keyCode == 203)
            {
                float value = selected.getTransform().getOffsetX() - step;
                selected.getTransform().setOffsetX(UiMotion.clamp(UiMotion.roundToStep(value, OFFSET_STEP), OFFSET_MIN, OFFSET_MAX));
                return;
            }

            if (keyCode == 205)
            {
                float value = selected.getTransform().getOffsetX() + step;
                selected.getTransform().setOffsetX(UiMotion.clamp(UiMotion.roundToStep(value, OFFSET_STEP), OFFSET_MIN, OFFSET_MAX));
                return;
            }

            if (keyCode == 200)
            {
                float value = selected.getTransform().getOffsetY() - step;
                selected.getTransform().setOffsetY(UiMotion.clamp(UiMotion.roundToStep(value, OFFSET_STEP), OFFSET_MIN, OFFSET_MAX));
                return;
            }

            if (keyCode == 208)
            {
                float value = selected.getTransform().getOffsetY() + step;
                selected.getTransform().setOffsetY(UiMotion.clamp(UiMotion.roundToStep(value, OFFSET_STEP), OFFSET_MIN, OFFSET_MAX));
                return;
            }

            if (keyCode == 13 || keyCode == 78)
            {
                float value = selected.getTransform().getScale() + SCALE_STEP;
                selected.getTransform().setScale(UiMotion.clamp(UiMotion.roundToStep(value, SCALE_STEP), SCALE_MIN, SCALE_MAX));
                return;
            }

            if (keyCode == 12 || keyCode == 74)
            {
                float value = selected.getTransform().getScale() - SCALE_STEP;
                selected.getTransform().setScale(UiMotion.clamp(UiMotion.roundToStep(value, SCALE_STEP), SCALE_MIN, SCALE_MAX));
                return;
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        HudElement selected = this.selection.getSelected();
        PanelLayout p = this.panelLayout();

        if (mouseButton == 1)
        {
            HudElement hit = this.pickElementAt(mouseX, mouseY);

            if (hit != null)
            {
                this.selection.setSelected(hit);
                selected = hit;
            }

            if (hit != null)
            {
                this.panelOpen = true;
                this.panelX = (float)mouseX + 12.0F;
                this.panelY = (float)mouseY + 10.0F;
                this.clampPanelToScreen();
                return;
            }

            this.selection.clear();
            this.closeConfigPanel();

            return;
        }

        if (mouseButton != 0)
        {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        HudElement hit = this.pickElementAt(mouseX, mouseY);

        if (hit != null)
        {
            this.selection.setSelected(hit);
            selected = hit;
        }
        else if (!(this.panelOpen && p.panel.contains(mouseX, mouseY)))
        {
            this.selection.clear();
            selected = null;
        }

        if (this.panelOpen && p.panel.contains(mouseX, mouseY))
        {
            if (p.closeButton.contains(mouseX, mouseY))
            {
                this.closeConfigPanel();
                return;
            }

            if (p.resetButton.contains(mouseX, mouseY))
            {
                this.resetSelectedTransform();
                return;
            }

            if (selected != null)
            {
                if (p.enabledChip.contains(mouseX, mouseY))
                {
                    selected.setEnabled(!selected.isEnabled());
                    return;
                }

                if (p.offsetXTrack.contains(mouseX, mouseY, 4.0F, 6.0F))
                {
                    this.draggingOffsetX = true;
                    this.applyOffsetXFromMouse(p, mouseX);
                    this.lastSliderDragNanos = System.nanoTime();
                    return;
                }

                if (p.offsetYTrack.contains(mouseX, mouseY, 4.0F, 6.0F))
                {
                    this.draggingOffsetY = true;
                    this.applyOffsetYFromMouse(p, mouseX);
                    this.lastSliderDragNanos = System.nanoTime();
                    return;
                }

                if (p.scaleTrack.contains(mouseX, mouseY, 4.0F, 6.0F))
                {
                    this.draggingScale = true;
                    this.applyScaleFromMouse(p, mouseX);
                    this.lastSliderDragNanos = System.nanoTime();
                    return;
                }

                if (selected instanceof HudFpsElement && p.sourceChip.contains(mouseX, mouseY))
                {
                    ((HudFpsElement)selected).cycleSource();
                    return;
                }

                if (p.anchorChip.contains(mouseX, mouseY))
                {
                    this.cycleAnchor(selected, 1);
                    return;
                }

                if (p.dockChip.contains(mouseX, mouseY))
                {
                    this.cycleDock(selected, 1);
                    return;
                }

            }

            return;
        }

        if (this.panelOpen)
        {
            this.closeConfigPanel();
        }

        if (selected != null)
        {
            this.draggingElement = true;
            this.dragStartMouseX = mouseX;
            this.dragStartMouseY = mouseY;
            this.dragStartOffsetX = selected.getTransform().getOffsetX();
            this.dragStartOffsetY = selected.getTransform().getOffsetY();
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick)
    {
        if (clickedMouseButton != 0)
        {
            return;
        }

        PanelLayout p = this.panelLayout();

        if (this.draggingElement)
        {
            this.updateElementDrag(mouseX, mouseY);
            return;
        }

        if (this.draggingOffsetX)
        {
            this.applyOffsetXFromMouse(p, mouseX);
            this.lastSliderDragNanos = System.nanoTime();
        }

        if (this.draggingOffsetY)
        {
            this.applyOffsetYFromMouse(p, mouseX);
            this.lastSliderDragNanos = System.nanoTime();
        }

        if (this.draggingScale)
        {
            this.applyScaleFromMouse(p, mouseX);
            this.lastSliderDragNanos = System.nanoTime();
        }
    }

    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        this.draggingElement = false;
        this.draggingOffsetX = false;
        this.draggingOffsetY = false;
        this.draggingScale = false;
        this.lastSliderDragNanos = 0L;
        super.mouseReleased(mouseX, mouseY, state);
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();

        if (wheel == 0)
        {
            return;
        }

        List<HudElement> elements = this.hudManager.getElements();

        if (elements.size() <= 1)
        {
            return;
        }

        HudElement selected = this.selection.getSelected();
        int idx = elements.indexOf(selected);

        if (idx < 0)
        {
            return;
        }

        idx += wheel > 0 ? -1 : 1;

        if (idx < 0)
        {
            idx = elements.size() - 1;
        }
        else if (idx >= elements.size())
        {
            idx = 0;
        }

        this.selection.setSelected(elements.get(idx));
    }

    public void updateScreen()
    {
        super.updateScreen();

        if (!Display.isActive())
        {
            this.closeConfigPanel();

            if (this.mc != null && this.mc.currentScreen == this)
            {
                this.mc.displayGuiScreen((GuiScreen)null);
            }
        }
    }

    public void onGuiClosed()
    {
        super.onGuiClosed();
        this.closeConfigPanel();
        this.draggingElement = false;
    }

    public void renderNano(RenderContext2D context)
    {
        if (context == null || context.getNanoVG() == null)
        {
            return;
        }

        long vg = context.getNanoVG().getHandle();

        if (vg == 0L)
        {
            return;
        }

        this.clampPanelToScreen();

        if (this.draggingElement)
        {
            this.updateElementDrag(this.mouseX, this.mouseY);
        }

        NanoTheme theme = this.resolveTheme();
        NanoFontBook.ensureLoaded(vg);
        int regular = NanoFontBook.uiRegular();
        int bold = NanoFontBook.uiBold();
        HudElement selected = this.selection.getSelected();

        try (MemoryStack stack = stackPush())
        {
            if ((theme.backdropArgb() >>> 24) > 0)
            {
                NanoUi.drawBackdrop(vg, stack, (float)this.width, (float)this.height, theme);
            }

            this.drawElementOverlays(vg, stack, theme);

            String title = selected == null ? this.tr("hud.editor.title.none", "HUD Layout - no element") : this.tr("hud.editor.title.selected", "HUD Layout - {0}", this.hudElementName(selected));
            NanoUi.drawLeftText(vg, stack, bold, 14.0F, 18.0F, 17.0F, theme.textArgb(), title);
            NanoUi.drawLeftText(vg, stack, regular, 14.0F, 35.0F, 12.0F, theme.textWeakArgb(), this.tr("hud.editor.developer", "Developer DWGX"));

            if (selected != null)
            {
                HudTransform t = selected.getTransform();
                String status = selected.isEnabled() ? this.tr("hud.state.on", "ON") : this.tr("hud.state.off", "OFF");
                String text = this.tr("hud.editor.status_line", "x:{0} y:{1} s:{2} {3}", formatOffset(t.getOffsetX()), formatOffset(t.getOffsetY()), formatScale(t.getScale()), status);
                NanoUi.drawLeftText(vg, stack, regular, 14.0F, 52.0F, 11.5F, theme.textMutedArgb(), text);
            }

            if (this.panelOpen && selected != null)
            {
                this.drawConfigPanel(vg, stack, regular, bold, theme, selected);
            }
        }
    }

    private void drawConfigPanel(long vg, MemoryStack stack, int regular, int bold, NanoTheme theme, HudElement selected)
    {
        PanelLayout p = this.panelLayout();
        float scale = UiMotion.clamp(p.panel.w / PANEL_WIDTH, 0.35F, 1.85F);
        HudTransform t = selected.getTransform();

        NanoUi.drawSurface(vg, stack, p.panel.x, p.panel.y, p.panel.w, p.panel.h, theme.cardRadius(), theme.cardAltArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 100));
        NanoUi.drawLeftText(vg, stack, bold, p.panel.x + scaled(10.0F, scale), p.panel.y + scaled(15.0F, scale), scaled(13.5F, scale), theme.textArgb(), this.tr("hud.editor.panel.title", "Element Config"));
        NanoUi.drawLeftText(vg, stack, regular, p.panel.x + scaled(10.0F, scale), p.panel.y + scaled(31.0F, scale), scaled(10.5F, scale), theme.textWeakArgb(), selected.getId());
        NanoUi.drawSurface(vg, stack, p.resetButton.x, p.resetButton.y, p.resetButton.w, p.resetButton.h, theme.controlRadius(), theme.controlArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 90));
        NanoUi.drawCenterText(vg, stack, regular, p.resetButton.x + p.resetButton.w * 0.5F, p.resetButton.y + p.resetButton.h * 0.5F, scaled(10.0F, scale), theme.textArgb(), this.tr("hud.editor.reset", "Reset"));
        NanoUi.drawSurface(vg, stack, p.closeButton.x, p.closeButton.y, p.closeButton.w, p.closeButton.h, theme.controlRadius(), theme.controlArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 90));
        NanoUi.drawCenterText(vg, stack, regular, p.closeButton.x + p.closeButton.w * 0.5F, p.closeButton.y + p.closeButton.h * 0.5F, scaled(10.0F, scale), theme.textArgb(), this.tr("hud.editor.close", "Close"));
        this.drawChip(vg, stack, regular, theme, p.enabledChip, selected.isEnabled() ? this.tr("hud.editor.enabled", "Enabled") : this.tr("hud.editor.disabled", "Disabled"), selected.isEnabled());

        this.drawSlider(vg, stack, regular, bold, theme, p.offsetXTrack, "offset_x", this.tr("hud.editor.offset_x", "Offset X"), t.getOffsetX(), OFFSET_MIN, OFFSET_MAX, this.draggingOffsetX, p.offsetXTrack.contains(this.mouseX, this.mouseY, 4.0F, 6.0F));
        this.drawSlider(vg, stack, regular, bold, theme, p.offsetYTrack, "offset_y", this.tr("hud.editor.offset_y", "Offset Y"), t.getOffsetY(), OFFSET_MIN, OFFSET_MAX, this.draggingOffsetY, p.offsetYTrack.contains(this.mouseX, this.mouseY, 4.0F, 6.0F));
        this.drawSlider(vg, stack, regular, bold, theme, p.scaleTrack, "scale", this.tr("hud.editor.scale", "Scale"), t.getScale(), SCALE_MIN, SCALE_MAX, this.draggingScale, p.scaleTrack.contains(this.mouseX, this.mouseY, 4.0F, 6.0F));

        if (selected instanceof HudFpsElement)
        {
            HudFpsElement fps = (HudFpsElement)selected;
            this.drawChip(vg, stack, regular, theme, p.sourceChip, this.tr("hud.editor.fps_source", "FPS Source: {0}", fps.sourceText()), p.sourceChip.contains(this.mouseX, this.mouseY));
        }

        this.drawChip(vg, stack, regular, theme, p.anchorChip, this.tr("hud.editor.anchor", "Anchor: {0}", this.anchorDisplayName(t.getAnchor())), p.anchorChip.contains(this.mouseX, this.mouseY));
        this.drawChip(vg, stack, regular, theme, p.dockChip, this.tr("hud.editor.dock", "Dock: {0}", this.dockDisplayName(t.getDock())), p.dockChip.contains(this.mouseX, this.mouseY));
    }

    private void drawSlider(long vg, MemoryStack stack, int regular, int bold, NanoTheme theme, Rect track, String sliderKey, String label, float value, float min, float max, boolean dragging, boolean hovered)
    {
        float scale = UiMotion.clamp(track.h / 6.0F, 0.35F, 1.85F);
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        float ratio = UiMotion.clamp01((value - min) / Math.max(0.0001F, max - min));
        float targetRatio = dragging ? NanoSliderController.mouseRatio((float)this.mouseX, track.x, track.w) : ratio;
        String key = this.sliderAnimKey(sliderKey);
        boolean sliderAnimEnabled = clickGui == null || clickGui.isGlobalAnimationEnabled() && clickGui.isSliderAnimationEnabled();
        UiAnimation.Type animType = clickGui == null ? UiAnimation.Type.EASE_OUT : clickGui.getControlAnimationType();
        float smooth = clickGui == null ? 0.64F : clickGui.getGlobalAnimationSmooth();
        float displayRatio = NanoSliderController.resolveDisplayRatio("hud.editor.slider." + key, targetRatio, dragging, sliderAnimEnabled, clickGui == null ? 0.64F : clickGui.getSliderAnimationSpeed(), smooth, animType);
        float focus = NanoSliderController.resolveFocus("hud.editor.slider.focus." + key, hovered, dragging, clickGui == null ? 0.58F : clickGui.getControlAnimationSpeed(), smooth, animType, clickGui == null || clickGui.isGlobalAnimationEnabled());

        NanoUi.drawLeftText(vg, stack, bold, track.x, track.y - scaled(8.0F, scale), scaled(11.5F, scale), theme.textArgb(), label);
        NanoUi.drawRightText(vg, stack, regular, track.x2(), track.y - scaled(8.0F, scale), scaled(10.0F, scale), theme.textMutedArgb(), formatSliderValue(sliderKey, value));

        NanoUi.drawSurface(vg, stack, track.x, track.y, track.w, track.h, theme.controlRadius(), theme.controlArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 98));
        NanoUi.drawSurface(vg, stack, track.x + 1.0F, track.y + 1.0F, Math.max(0.0F, (track.w - 2.0F) * displayRatio), Math.max(0.0F, track.h - 2.0F), Math.max(2.0F, theme.controlRadius() - 1.0F), this.mixArgb(theme.controlActiveArgb(), theme.accentArgb(), 0.74F), 0);

        float handleX = track.x + track.w * displayRatio;
        float knobSize = 8.0F + 2.5F * focus + (dragging ? 1.2F : 0.0F);
        float knobX = handleX - knobSize * 0.5F;
        float knobY = track.y + (track.h - knobSize) * 0.5F;
        NanoUi.drawSurface(vg, stack, knobX, knobY, knobSize, knobSize, knobSize * 0.5F, this.mixArgb(theme.accentArgb(), 0xFFF8FBFF, 0.55F), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 110));
    }

    private void drawChip(long vg, MemoryStack stack, int regular, NanoTheme theme, Rect chip, String text, boolean active)
    {
        boolean hovered = chip.contains(this.mouseX, this.mouseY);
        int fill = active ? theme.controlActiveArgb() : (hovered ? theme.controlHoverArgb() : theme.controlArgb());
        NanoUi.drawSurface(vg, stack, chip.x, chip.y, chip.w, chip.h, theme.controlRadius(), fill, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 90));
        NanoUi.drawCenterText(vg, stack, regular, chip.x + chip.w * 0.5F, chip.y + chip.h * 0.5F, 10.0F, theme.textArgb(), text);
    }

    private void closeConfigPanel()
    {
        this.panelOpen = false;
        this.draggingOffsetX = false;
        this.draggingOffsetY = false;
        this.draggingScale = false;
        this.lastSliderDragNanos = 0L;
    }

    private void resetSelectedTransform()
    {
        HudElement selected = this.selection.getSelected();

        if (selected == null)
        {
            return;
        }

        HudTransform t = selected.getTransform();
        t.setAnchor(Anchor.TOP_LEFT);
        t.setDock(Dock.NONE);
        t.setOffsetX(0.0F);
        t.setOffsetY(0.0F);
        t.setScale(1.0F);
        t.setSnapToGrid(false);
        selected.setEnabled(true);
    }

    private void drawElementOverlays(long vg, MemoryStack stack, NanoTheme theme)
    {
        List<HudElement> elements = this.hudManager.getElements();
        HudElement selected = this.selection.getSelected();

        for (int i = 0; i < elements.size(); ++i)
        {
            HudElement element = elements.get(i);
            Rect bounds = this.resolveElementScreenRect(element);

            if (bounds == null)
            {
                continue;
            }

            boolean active = element == selected;
            int border = active ? theme.accentArgb() : NanoRenderUtils.withAlpha(theme.textWeakArgb(), 112);
            int fill = active ? NanoRenderUtils.withAlpha(theme.accentArgb(), 34) : NanoRenderUtils.withAlpha(theme.controlArgb(), 22);

            if (!element.isEnabled())
            {
                border = NanoRenderUtils.withAlpha(theme.textMutedArgb(), 90);
                fill = NanoRenderUtils.withAlpha(theme.textMutedArgb(), 16);
            }

            NanoUi.drawSurface(vg, stack, bounds.x - 1.0F, bounds.y - 1.0F, bounds.w + 2.0F, bounds.h + 2.0F, 3.0F, fill, border);
        }
    }

    private HudElement pickElementAt(int mouseX, int mouseY)
    {
        List<HudElement> elements = this.hudManager.getElements();

        for (int i = elements.size() - 1; i >= 0; --i)
        {
            HudElement element = elements.get(i);
            Rect bounds = this.resolveElementScreenRect(element);

            if (bounds != null && bounds.contains(mouseX, mouseY, ELEMENT_HIT_EXPAND_X, ELEMENT_HIT_EXPAND_Y))
            {
                return element;
            }
        }

        return null;
    }

    private Rect resolveElementScreenRect(HudElement element)
    {
        HudLayoutMath.Bounds bounds = HudLayoutMath.resolveScreenBounds(element, (float)this.width, (float)this.height);
        return bounds == null ? null : new Rect(bounds.x(), bounds.y(), bounds.w(), bounds.h());
    }

    private void updateElementDrag(int mouseX, int mouseY)
    {
        HudElement selected = this.selection.getSelected();

        if (selected == null)
        {
            return;
        }

        float dx = (float)(mouseX - this.dragStartMouseX);
        float dy = (float)(mouseY - this.dragStartMouseY);
        float newX = this.dragStartOffsetX + dx;
        float newY = this.dragStartOffsetY + dy;

        if (selected.getTransform().isSnapToGrid())
        {
            newX = this.grid.snap(newX);
            newY = this.grid.snap(newY);
        }

        selected.getTransform().setOffsetX(UiMotion.clamp(newX, OFFSET_MIN, OFFSET_MAX));
        selected.getTransform().setOffsetY(UiMotion.clamp(newY, OFFSET_MIN, OFFSET_MAX));
    }

    private void applyOffsetXFromMouse(PanelLayout p, int mouseX)
    {
        HudElement selected = this.selection.getSelected();

        if (selected == null)
        {
            return;
        }

        float ratio = ((float)mouseX - p.offsetXTrack.x) / Math.max(1.0F, p.offsetXTrack.w);
        float value = OFFSET_MIN + UiMotion.clamp01(ratio) * (OFFSET_MAX - OFFSET_MIN);
        value = UiMotion.roundToStep(value, OFFSET_STEP);
        selected.getTransform().setOffsetX(value);
    }

    private void applyOffsetYFromMouse(PanelLayout p, int mouseX)
    {
        HudElement selected = this.selection.getSelected();

        if (selected == null)
        {
            return;
        }

        float ratio = ((float)mouseX - p.offsetYTrack.x) / Math.max(1.0F, p.offsetYTrack.w);
        float value = OFFSET_MIN + UiMotion.clamp01(ratio) * (OFFSET_MAX - OFFSET_MIN);
        value = UiMotion.roundToStep(value, OFFSET_STEP);
        selected.getTransform().setOffsetY(value);
    }

    private void applyScaleFromMouse(PanelLayout p, int mouseX)
    {
        HudElement selected = this.selection.getSelected();

        if (selected == null)
        {
            return;
        }

        float ratio = ((float)mouseX - p.scaleTrack.x) / Math.max(1.0F, p.scaleTrack.w);
        float value = SCALE_MIN + UiMotion.clamp01(ratio) * (SCALE_MAX - SCALE_MIN);
        value = UiMotion.roundToStep(value, SCALE_STEP);
        selected.getTransform().setScale(value);
    }

    private void cycleAnchor(HudElement element, int step)
    {
        Anchor[] values = Anchor.values();
        int index = 0;

        for (int i = 0; i < values.length; ++i)
        {
            if (values[i] == element.getTransform().getAnchor())
            {
                index = i;
                break;
            }
        }

        int next = (index + step) % values.length;

        if (next < 0)
        {
            next += values.length;
        }

        element.getTransform().setAnchor(values[next]);
    }

    private void cycleDock(HudElement element, int step)
    {
        Dock[] values = Dock.values();
        int index = 0;

        for (int i = 0; i < values.length; ++i)
        {
            if (values[i] == element.getTransform().getDock())
            {
                index = i;
                break;
            }
        }

        int next = (index + step) % values.length;

        if (next < 0)
        {
            next += values.length;
        }

        element.getTransform().setDock(values[next]);
    }

    private PanelLayout panelLayout()
    {
        float x = UiMotion.clamp(this.panelX, PANEL_MARGIN, Math.max(PANEL_MARGIN, (float)this.width - PANEL_WIDTH - PANEL_MARGIN));
        float y = UiMotion.clamp(this.panelY, PANEL_MARGIN, Math.max(PANEL_MARGIN, (float)this.height - PANEL_HEIGHT - PANEL_MARGIN));
        float w = Math.min(PANEL_WIDTH, (float)this.width - PANEL_MARGIN * 2.0F);
        float h = Math.min(PANEL_HEIGHT, (float)this.height - PANEL_MARGIN * 2.0F);
        Rect panel = new Rect(x, y, w, h);
        float buttonY = panel.y + 8.0F;
        float buttonGap = 6.0F;
        float closeW = 52.0F;
        float enabledW = 62.0F;
        float resetW = 56.0F;
        float closeX = panel.x2() - 10.0F - closeW;
        float enabledX = closeX - buttonGap - enabledW;
        float resetX = enabledX - buttonGap - resetW;
        Rect resetButton = new Rect(resetX, buttonY, resetW, 18.0F);
        Rect enabledChip = new Rect(enabledX, buttonY, enabledW, 18.0F);
        Rect closeButton = new Rect(closeX, buttonY, closeW, 18.0F);
        Rect offsetXTrack = new Rect(panel.x + 10.0F, panel.y + 48.0F, panel.w - 20.0F, 5.0F);
        Rect offsetYTrack = new Rect(panel.x + 10.0F, panel.y + 82.0F, panel.w - 20.0F, 5.0F);
        Rect scaleTrack = new Rect(panel.x + 10.0F, panel.y + 116.0F, panel.w - 20.0F, 5.0F);
        Rect sourceChip = new Rect(panel.x + 10.0F, panel.y + 140.0F, panel.w - 20.0F, 20.0F);
        float chipW = (panel.w - 24.0F) * 0.5F;
        Rect anchorChip = new Rect(panel.x + 10.0F, panel.y + panel.h - 30.0F, chipW, 20.0F);
        Rect dockChip = new Rect(anchorChip.x2() + 4.0F, panel.y + panel.h - 30.0F, chipW, 20.0F);
        return new PanelLayout(panel, enabledChip, resetButton, closeButton, offsetXTrack, offsetYTrack, scaleTrack, sourceChip, anchorChip, dockChip);
    }

    private void clampPanelToScreen()
    {
        this.panelX = UiMotion.clamp(this.panelX, PANEL_MARGIN, Math.max(PANEL_MARGIN, (float)this.width - PANEL_WIDTH - PANEL_MARGIN));
        this.panelY = UiMotion.clamp(this.panelY, PANEL_MARGIN, Math.max(PANEL_MARGIN, (float)this.height - PANEL_HEIGHT - PANEL_MARGIN));
    }

    private ClickGuiModule resolveClickGuiModule()
    {
        Module module = ClientBootstrap.instance().getModules().getById("click_gui");
        return module instanceof ClickGuiModule ? (ClickGuiModule)module : null;
    }

    private String sliderAnimKey(String sliderKey)
    {
        return sliderKey == null || sliderKey.isEmpty() ? "generic" : sliderKey;
    }

    private NanoTheme resolveTheme()
    {
        Module module = ClientBootstrap.instance().getModules().getById("click_gui");

        if (module instanceof ClickGuiModule)
        {
            ClickGuiModule clickGui = (ClickGuiModule)module;
            Integer accent = null;

            if (clickGui.isAccentOverrideEnabled() && clickGui.getAccentOverride() != null)
            {
                accent = Integer.valueOf(clickGui.getAccentOverride().toArgb());
            }

            int backdrop = clickGui.isBackdropEnabled() ? clickGui.getBackdropAlpha() : 48;
            return NanoThemes.create(clickGui.getPalette(), clickGui.getPanelAlpha(), backdrop, clickGui.getCornerRadius(), accent);
        }

        return NanoThemes.create(NanoPalette.COBALT, 220, 56, 12.0F, null);
    }

    private int mixArgb(int from, int to, float t)
    {
        float k = UiMotion.clamp01(t);
        int a = this.lerpChannel((from >>> 24) & 255, (to >>> 24) & 255, k);
        int r = this.lerpChannel((from >>> 16) & 255, (to >>> 16) & 255, k);
        int g = this.lerpChannel((from >>> 8) & 255, (to >>> 8) & 255, k);
        int b = this.lerpChannel(from & 255, to & 255, k);
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
    }

    private int lerpChannel(int from, int to, float t)
    {
        return NanoRenderUtils.clamp255(Math.round((float)from + (float)(to - from) * UiMotion.clamp01(t)));
    }

    private static float scaled(float value, float scale)
    {
        return value * scale;
    }

    private static String formatOffset(float value)
    {
        return Integer.toString(Math.round(value));
    }

    private static String formatScale(float value)
    {
        return Integer.toString(Math.round(value * 100.0F)) + "%";
    }

    private static String formatSliderValue(String sliderKey, float value)
    {
        if ("scale".equals(sliderKey))
        {
            return formatScale(value);
        }

        return formatOffset(value);
    }

    private String anchorDisplayName(Anchor anchor)
    {
        if (anchor == null)
        {
            return this.tr("hud.anchor.top_left", "top_left");
        }

        switch (anchor)
        {
            case TOP_RIGHT:
                return this.tr("hud.anchor.top_right", "top_right");
            case BOTTOM_LEFT:
                return this.tr("hud.anchor.bottom_left", "bottom_left");
            case BOTTOM_RIGHT:
                return this.tr("hud.anchor.bottom_right", "bottom_right");
            case CENTER:
                return this.tr("hud.anchor.center", "center");
            case TOP_LEFT:
            default:
                return this.tr("hud.anchor.top_left", "top_left");
        }
    }

    private String dockDisplayName(Dock dock)
    {
        if (dock == null)
        {
            return this.tr("hud.dock.none", "none");
        }

        switch (dock)
        {
            case TOP:
                return this.tr("hud.dock.top", "top");
            case BOTTOM:
                return this.tr("hud.dock.bottom", "bottom");
            case LEFT:
                return this.tr("hud.dock.left", "left");
            case RIGHT:
                return this.tr("hud.dock.right", "right");
            case CENTER:
                return this.tr("hud.dock.center", "center");
            case NONE:
            default:
                return this.tr("hud.dock.none", "none");
        }
    }

    private String hudElementName(HudElement element)
    {
        if (element == null)
        {
            return this.tr("hud.element.none", "no element");
        }
        return element.getDisplayName();
    }

    private String tr(String key, String fallback, Object... args)
    {
        client.i18n.I18nManager i18n = ClientBootstrap.instance().getI18n();
        return i18n == null ? fallback : i18n.translateOrDefault(key, fallback, args);
    }

    private static final class PanelLayout
    {
        private final Rect panel;
        private final Rect enabledChip;
        private final Rect resetButton;
        private final Rect closeButton;
        private final Rect offsetXTrack;
        private final Rect offsetYTrack;
        private final Rect scaleTrack;
        private final Rect sourceChip;
        private final Rect anchorChip;
        private final Rect dockChip;

        private PanelLayout(Rect panel, Rect enabledChip, Rect resetButton, Rect closeButton, Rect offsetXTrack, Rect offsetYTrack, Rect scaleTrack, Rect sourceChip, Rect anchorChip, Rect dockChip)
        {
            this.panel = panel;
            this.enabledChip = enabledChip;
            this.resetButton = resetButton;
            this.closeButton = closeButton;
            this.offsetXTrack = offsetXTrack;
            this.offsetYTrack = offsetYTrack;
            this.scaleTrack = scaleTrack;
            this.sourceChip = sourceChip;
            this.anchorChip = anchorChip;
            this.dockChip = dockChip;
        }
    }

    private static final class Rect
    {
        private final float x;
        private final float y;
        private final float w;
        private final float h;

        private Rect(float x, float y, float w, float h)
        {
            this.x = x;
            this.y = y;
            this.w = Math.max(0.0F, w);
            this.h = Math.max(0.0F, h);
        }

        private float x2()
        {
            return this.x + this.w;
        }

        private boolean contains(int mx, int my)
        {
            return (float)mx >= this.x && (float)my >= this.y && (float)mx <= this.x + this.w && (float)my <= this.y + this.h;
        }

        private boolean contains(int mx, int my, float expandX, float expandY)
        {
            return (float)mx >= this.x - expandX && (float)my >= this.y - expandY && (float)mx <= this.x + this.w + expandX && (float)my <= this.y + this.h + expandY;
        }
    }
}
