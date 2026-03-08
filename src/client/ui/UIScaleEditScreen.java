package client.ui;

import client.core.ClientBootstrap;
import client.hud.HudEditorScreen;
import client.module.Module;
import client.module.impl.client.ClickGuiModule;
import client.module.impl.client.UiScaleEditModule;
import client.render.RenderContext2D;
import client.setting.StringSetting;
import client.ui.template.NanoScreenKit;
import client.ui.template.NanoSliderController;
import client.ui.template.NanoTextInput;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiAnimProfiles;
import client.ui.template.UiLayoutProfile;
import client.ui.template.UiMotion;
import client.ui.template.UiAnimation;
import client.ui.template.UiAnimationBus;
import client.ui.template.ScreenTransitionController;
import client.ui.template.UiWindowState;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoPalette;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoThemes;
import dwgx.nano.NanoUi;
import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.MemoryStack.stackPush;

public final class UIScaleEditScreen extends GuiScreen implements NanoRenderableScreen
{
    private static final float BASE_WIDTH = 690.0F;
    private static final float BASE_HEIGHT = 430.0F;
    private static final float MIN_WIDTH = 360.0F;
    private static final float MIN_HEIGHT = 230.0F;
    private static final float SCREEN_MARGIN = 8.0F;
    private static final ScreenTransitionController.Tuning TRANSITION_TUNING = ScreenTransitionController.Tuning.of(1.90F, 1.18F, 1.48F, 0.95F, 0.90F, 1.12F, 1.24F, 0.08F);

    private final UiScaleEditModule module;
    private final GuiScreen parentScreen;
    private final UiWindowState window = new UiWindowState(MIN_WIDTH, MIN_HEIGHT);
    private int mouseX;
    private int mouseY;
    private boolean draggingScale;
    private boolean draggingMotion;
    private boolean draggingAnchorX;
    private boolean draggingAnchorY;
    private boolean draggingSliderTrackLocked;
    private float draggingSliderTrackX;
    private float draggingSliderTrackW;
    private long lastSliderDragNanos;
    private boolean draggingAnimSpeed;
    private boolean draggingAnimSmooth;
    private boolean draggingSidebarRatio;
    private boolean draggingModulesRatio;
    private boolean draggingRowCategory;
    private boolean draggingRowModule;
    private boolean draggingRowSetting;
    private boolean draggingBtnHeight;
    private boolean draggingGapMajor;
    private boolean draggingValueCol;
    private int activeTab;
    private long lastNanoAt;
    private long lastNanoVg;
    private String activeSliderInputKey;
    private final StringSetting numberInputBuffer = new StringSetting("__uiscale_number_input_buffer", "Number Input", "UIScale inline number input buffer", "", 32);
    private final NanoTextInput numberInput = new NanoTextInput();
    private ScreenTransitionController.Mode transitionMode = ScreenTransitionController.Mode.NONE;
    private GuiScreen transitionTarget;
    private boolean transitioningOut;
    private boolean transitionExecuted;
    private float transitionProgress = 1.0F;
    private long transitionLastNanos;

    public UIScaleEditScreen(UiScaleEditModule module)
    {
        this(module, null);
    }

    public UIScaleEditScreen(UiScaleEditModule module, GuiScreen parentScreen)
    {
        this.module = module;
        this.parentScreen = parentScreen;
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }

    public void initGui()
    {
        this.transitionMode = ScreenTransitionController.Mode.NONE;
        this.transitionTarget = null;
        this.transitioningOut = false;
        this.transitionExecuted = false;
        this.transitionProgress = 0.0F;
        this.transitionLastNanos = System.nanoTime();
        this.clearSliderTrackLock();
        this.activeSliderInputKey = null;
        this.numberInputBuffer.set("");
        this.numberInput.blur();
        this.lastNanoVg = 0L;
    }

    public void onGuiClosed()
    {
        this.window.endInteraction();
        this.draggingScale = false;
        this.draggingMotion = false;
        this.draggingAnchorX = false;
        this.draggingAnchorY = false;
        this.draggingSidebarRatio = false;
        this.draggingModulesRatio = false;
        this.draggingRowCategory = false;
        this.draggingRowModule = false;
        this.draggingRowSetting = false;
        this.draggingBtnHeight = false;
        this.draggingGapMajor = false;
        this.draggingValueCol = false;
        this.clearSliderTrackLock();
        this.commitActiveNumberInput();
        this.activeSliderInputKey = null;
        this.numberInput.blur();
        UiAnimationBus.clearPrefix("uiscale.");
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.activeSliderInputKey != null && this.numberInput.isFocused())
        {
            if (this.numberInput.handleKeyTyped(typedChar, keyCode, this.numberInputBuffer))
            {
                if (!this.numberInput.isFocused())
                {
                    this.commitActiveNumberInput();
                }

                return;
            }
        }

        if (keyCode == 1)
        {
            this.requestTransition(ScreenTransitionController.Mode.BACK, this.parentScreen);
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        Layout l = this.layout();
        this.validateActiveSliderInput();

        if (mouseButton == 0 && this.activeSliderInputKey != null)
        {
            Rect activeRect = this.activeSliderInputRect(l, this.activeSliderInputKey);

            if (activeRect == null || !activeRect.contains(mouseX, mouseY))
            {
                this.commitActiveNumberInput();
                this.numberInput.blur();
            }
        }

        if (mouseButton == 0)
        {
            if (l.headerDrag.contains(mouseX, mouseY))
            {
                this.window.startMove((float)mouseX, (float)mouseY);
                return;
            }

            if (l.resizeHandle.contains(mouseX, mouseY))
            {
                this.window.startResize((float)mouseX, (float)mouseY, UiWindowState.ResizeMode.BOTTOM_RIGHT);
                return;
            }

            if (l.targetClickGui.contains(mouseX, mouseY))
            {
                this.module.setEditTarget(UiScaleEditModule.UiTarget.CLICK_GUI);
                return;
            }

            if (l.targetHudEdit.contains(mouseX, mouseY))
            {
                this.module.setEditTarget(UiScaleEditModule.UiTarget.HUD_EDIT);
                return;
            }

            if (l.scaleValue.contains(mouseX, mouseY))
            {
                this.activateSliderInput(l.scaleValue, l.scale, mouseX, mouseY, "ui_scale", this.module.getUiScale(this.module.getEditTarget()));
                return;
            }

            if (l.motionValue.contains(mouseX, mouseY))
            {
                this.activateSliderInput(l.motionValue, l.scale, mouseX, mouseY, "motion_speed", this.module.getMotionSpeed(this.module.getEditTarget()));
                return;
            }

            if (l.anchorXValue.contains(mouseX, mouseY))
            {
                this.activateSliderInput(l.anchorXValue, l.scale, mouseX, mouseY, "anchor_x", this.module.getWindowAnchorX(this.module.getEditTarget()));
                return;
            }

            if (l.anchorYValue.contains(mouseX, mouseY))
            {
                this.activateSliderInput(l.anchorYValue, l.scale, mouseX, mouseY, "anchor_y", this.module.getWindowAnchorY(this.module.getEditTarget()));
                return;
            }

            if (l.scaleTrack.contains(mouseX, mouseY))
            {
                this.commitActiveNumberInput();
                this.numberInput.blur();
                this.draggingScale = true;
                this.lockSliderTrack(l.scaleTrack);
                this.applyScaleFromMouse(l, mouseX);
                return;
            }

            if (l.motionTrack.contains(mouseX, mouseY))
            {
                this.commitActiveNumberInput();
                this.numberInput.blur();
                this.draggingMotion = true;
                this.lockSliderTrack(l.motionTrack);
                this.applyMotionFromMouse(l, mouseX);
                return;
            }

            if (l.anchorXTrack.contains(mouseX, mouseY))
            {
                this.commitActiveNumberInput();
                this.numberInput.blur();
                this.draggingAnchorX = true;
                this.lockSliderTrack(l.anchorXTrack);
                this.applyAnchorXFromMouse(l, mouseX);
                return;
            }

            if (l.anchorYTrack.contains(mouseX, mouseY))
            {
                this.commitActiveNumberInput();
                this.numberInput.blur();
                this.draggingAnchorY = true;
                this.lockSliderTrack(l.anchorYTrack);
                this.applyAnchorYFromMouse(l, mouseX);
                return;
            }

            if (l.tabScale.contains(mouseX, mouseY))
            {
                this.activeTab = 0;
                return;
            }

            if (l.tabLayout.contains(mouseX, mouseY))
            {
                this.activeTab = 1;
                return;
            }

            if (this.activeTab == 1)
            {
                if (this.handleLayoutSliderClick(l, mouseX))
                {
                    return;
                }
            }

            if (l.resetButton.contains(mouseX, mouseY))
            {
                this.resetLayoutDefaults();
                return;
            }

            if (l.openButton.contains(mouseX, mouseY))
            {
                this.requestTransition(ScreenTransitionController.Mode.SWITCH, this.createTargetScreen());
                return;
            }

            if (l.backButton.contains(mouseX, mouseY))
            {
                this.requestTransition(ScreenTransitionController.Mode.BACK, this.parentScreen);
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick)
    {
        if (clickedMouseButton != 0)
        {
            return;
        }

        this.mouseX = mouseX;
        this.mouseY = mouseY;

        Layout l = this.layout();

        if (this.draggingScale)
        {
            this.applyScaleFromMouse(l, mouseX);
        }

        if (this.draggingMotion)
        {
            this.applyMotionFromMouse(l, mouseX);
        }

        if (this.draggingAnchorX)
        {
            this.applyAnchorXFromMouse(l, mouseX);
        }

        if (this.draggingAnchorY)
        {
            this.applyAnchorYFromMouse(l, mouseX);
        }

        this.updateLayoutSliderDrags(l, mouseX);

        if (this.activeSliderInputKey != null && this.numberInput.isFocused())
        {
            Rect inputRect = this.activeSliderInputRect(l, this.activeSliderInputKey);

            if (inputRect != null)
            {
                this.numberInput.onMouseDrag(mouseX, inputRect.x, inputRect.w, this.lastNanoVg, NanoFontBook.uiRegular(), scaled(11.5F, l.scale), this.numberInputBuffer.get());
            }
        }
    }

    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        this.window.endInteraction();
        this.draggingScale = false;
        this.draggingMotion = false;
        this.draggingAnchorX = false;
        this.draggingAnchorY = false;
        this.draggingSidebarRatio = false;
        this.draggingModulesRatio = false;
        this.draggingRowCategory = false;
        this.draggingRowModule = false;
        this.draggingRowSetting = false;
        this.draggingBtnHeight = false;
        this.draggingGapMajor = false;
        this.draggingValueCol = false;
        this.clearSliderTrackLock();
        this.lastSliderDragNanos = 0L;
        this.draggingAnimSpeed = false;
        this.draggingAnimSmooth = false;
        this.numberInput.onMouseUp();
        super.mouseReleased(mouseX, mouseY, state);
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

        this.lastNanoAt = System.currentTimeMillis();
        this.lastNanoVg = vg;
        this.refreshLiveMousePosition();
        this.validateActiveSliderInput();
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        this.updateTransition(clickGui);

        if (this.transitioningOut && this.transitionExecuted)
        {
            return;
        }

        this.syncWindowTarget();
        UiScaleEditModule.UiTarget target = this.module.getEditTarget();

        if (this.window.isInteracting())
        {
            this.window.updateInteraction(this.liveMouseX(), this.liveMouseY(), (float)this.width, (float)this.height, SCREEN_MARGIN);
            this.syncProfileFromWindow();
        }

        UiAnimProfile windowAnim = this.resolveAnimationProfile(clickGui, target);
        this.window.tick(windowAnim);
        NanoTheme theme = this.applyThemeTransition(this.resolveTheme(), this.transitionVisual());
        NanoFontBook.ensureLoaded(vg);
        int regular = NanoFontBook.uiRegular();
        int bold = NanoFontBook.uiBold();
        Layout l = this.layout();
        this.updateActiveDrags(l);
        float k = l.scale;
        float uiScale = this.module.getUiScale(target);
        float motion = this.module.getMotionSpeed(target);
        float anchorX = this.module.getWindowAnchorX(target);
        float anchorY = this.module.getWindowAnchorY(target);

        try (MemoryStack stack = stackPush())
        {
            if ((theme.backdropArgb() >>> 24) > 0)
            {
                NanoUi.drawBackdrop(vg, stack, (float)this.width, (float)this.height, theme);
            }

            NanoUi.drawWindow(vg, stack, l.window.x, l.window.y, l.window.w, l.window.h, theme);
            NanoUi.drawLeftText(vg, stack, bold, l.header.x + scaled(14.0F, k), l.header.y + l.header.h * 0.5F, scaled(18.0F, k), theme.textArgb(), this.tr("uiedit.title", "UIEdit"));
            NanoUi.drawRightText(vg, stack, regular, l.header.x2() - scaled(14.0F, k), l.header.y + l.header.h * 0.5F, scaled(12.0F, k), theme.textWeakArgb(), this.tr("uiedit.subtitle", "Scale & Layout editor"));

            NanoUi.drawSurface(vg, stack, l.body.x, l.body.y, l.body.w, l.body.h, theme.surfaceRadius(), theme.mainArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 55));
            this.drawTargetChip(vg, stack, regular, theme, l.targetClickGui, this.tr("uiscale.target.clickgui", "ClickGUI"), target == UiScaleEditModule.UiTarget.CLICK_GUI);
            this.drawTargetChip(vg, stack, regular, theme, l.targetHudEdit, this.tr("uiscale.target.hudedit", "HudEdit"), target == UiScaleEditModule.UiTarget.HUD_EDIT);

            this.drawTargetChip(vg, stack, regular, theme, l.tabScale, this.tr("uiedit.tab.scale", "Scale"), this.activeTab == 0);
            this.drawTargetChip(vg, stack, regular, theme, l.tabLayout, this.tr("uiedit.tab.layout", "Layout"), this.activeTab == 1);

            if (this.activeTab == 0)
            {
                this.drawSlider(vg, stack, regular, bold, theme, l.scaleTrack, l.scaleValue, "ui_scale", this.tr("uiscale.slider.ui_scale", "UI Scale"), uiScale, this.module.getUiScaleMin(), this.module.getUiScaleMax(), this.tr("uiscale.hint.ui_scale", "Global interface scale"), l.showSliderHints, this.draggingScale, l.scaleTrack.contains(this.mouseX, this.mouseY));
                this.drawSlider(vg, stack, regular, bold, theme, l.motionTrack, l.motionValue, "motion_speed", this.tr("uiscale.slider.motion_speed", "Motion Speed"), motion, this.module.getMotionSpeedMin(), this.module.getMotionSpeedMax(), this.tr("uiscale.hint.motion_speed", "Drag/resize response"), l.showSliderHints, this.draggingMotion, l.motionTrack.contains(this.mouseX, this.mouseY));
                this.drawSlider(vg, stack, regular, bold, theme, l.anchorXTrack, l.anchorXValue, "anchor_x", this.tr("uiscale.slider.horizontal", "Horizontal"), anchorX, 0.0F, 1.0F, this.tr("uiscale.hint.horizontal", "Left <-> Right balance"), l.showSliderHints, this.draggingAnchorX, l.anchorXTrack.contains(this.mouseX, this.mouseY));
                this.drawSlider(vg, stack, regular, bold, theme, l.anchorYTrack, l.anchorYValue, "anchor_y", this.tr("uiscale.slider.vertical", "Vertical"), anchorY, 0.0F, 1.0F, this.tr("uiscale.hint.vertical", "Top <-> Bottom balance"), l.showSliderHints, this.draggingAnchorY, l.anchorYTrack.contains(this.mouseX, this.mouseY));
            }
            else
            {
                this.drawLayoutSliders(vg, stack, regular, bold, theme, l);
            }

            this.drawButton(vg, stack, regular, theme, l.openButton, this.tr("uiscale.open_target", "Open Target"), false);
            this.drawButton(vg, stack, regular, theme, l.backButton, this.tr("ui.back", "Back"), false);
            if (this.activeTab == 1)
            {
                this.drawButton(vg, stack, regular, theme, l.resetButton, this.tr("uiedit.reset", "Reset"), false);
            }
            this.drawResizeHandle(vg, stack, theme, l.resizeHandle);
            context.getNanoVG().resetScissor();
        }
    }

    private void drawTargetChip(long vg, MemoryStack stack, int regular, NanoTheme theme, Rect rect, String label, boolean active)
    {
        float k = UiMotion.clamp(rect.h / 22.0F, 0.35F, 1.85F);
        boolean hovered = rect.contains(this.mouseX, this.mouseY);
        int fill = active ? theme.controlActiveArgb() : (hovered ? theme.controlHoverArgb() : theme.controlArgb());
        NanoUi.drawSurface(vg, stack, rect.x, rect.y, rect.w, rect.h, theme.controlRadius(), fill, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 75));
        NanoUi.drawCenterText(vg, stack, regular, rect.x + rect.w * 0.5F, rect.y + rect.h * 0.5F, scaled(12.5F, k), theme.textArgb(), label);
    }

    private void drawSlider(long vg, MemoryStack stack, int regular, int bold, NanoTheme theme, Rect track, Rect valueRect, String sliderKey, String label, float value, float min, float max, String hint, boolean showHint, boolean dragging, boolean hovered)
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui, this.module.getEditTarget());
        float k = UiMotion.clamp(track.w / (BASE_WIDTH - 40.0F), 0.35F, 1.85F);
        float ratio = (value - min) / Math.max(0.0001F, max - min);
        ratio = UiMotion.clamp01(ratio);
        float visualTarget = dragging ? this.sliderRatioFromMouse(this.mouseX, track) : ratio;
        float displayRatio = NanoSliderController.resolveDisplayRatio("uiscale.slider." + sliderKey + ".track", visualTarget, dragging, animProfile);
        float fillRatio = NanoSliderController.resolveFillRatio("uiscale.slider." + sliderKey, visualTarget, dragging, animProfile);
        float knobRatio = NanoSliderController.resolveKnobRatio("uiscale.slider." + sliderKey, visualTarget, dragging, animProfile);
        float focus = NanoSliderController.resolveFocus("uiscale.slider.focus." + sliderKey, hovered, dragging, animProfile);
        float glowRatio = NanoSliderController.resolveGlow("uiscale.slider.glow." + sliderKey, hovered, dragging, animProfile);
        NanoUi.drawLeftText(vg, stack, bold, track.x, track.y - scaled(13.0F, k), scaled(16.0F, k), theme.textArgb(), label);
        this.drawSliderValueInput(vg, stack, regular, theme, valueRect, track, sliderKey, value, k, hovered);
        NanoScreenKit.drawSliderTrack(vg, stack, theme, track.x, track.y, track.w, track.h, k, fillRatio, knobRatio, displayRatio, focus, glowRatio, dragging);

        if (showHint && k >= 0.72F)
        {
            NanoUi.drawLeftText(vg, stack, regular, track.x, track.y + track.h + scaled(13.0F, k), scaled(11.5F, k), theme.textWeakArgb(), hint);
        }
    }

    private void drawSliderValueInput(long vg, MemoryStack stack, int font, NanoTheme theme, Rect valueRect, Rect track, String sliderKey, float value, float scale, boolean hoveredTrack)
    {
        if (valueRect == null)
        {
            NanoUi.drawRightText(vg, stack, font, track.x2(), track.y - scaled(13.0F, scale), scaled(14.0F, scale), theme.textMutedArgb(), this.formatSliderValue(sliderKey, value));
            return;
        }

        boolean fieldHovered = valueRect.contains(this.mouseX, this.mouseY);
        boolean active = sliderKey.equals(this.activeSliderInputKey) && this.numberInput.isFocused();
        String animKey = "uiscale.slider.input." + sliderKey;
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        UiAnimProfile animProfile = UiAnimProfiles.inputProfile(clickGui, UiAnimProfiles.uiScaleProfile(clickGui));

        if (active)
        {
            this.numberInput.draw(vg, stack, font, theme, valueRect.x, valueRect.y, valueRect.w, valueRect.h, scale, scaled(11.5F, scale), this.numberInputBuffer.get(), this.tr("clickgui.input.number.placeholder", "Input..."), hoveredTrack || fieldHovered, true, animKey, animProfile);
            return;
        }

        float focus = UiAnimationBus.animateControl(animKey + ".idle.focus", fieldHovered ? 1.0F : 0.0F, animProfile);
        int fill = NanoScreenKit.mixArgb(theme.cardAltArgb(), theme.controlArgb(), UiMotion.clamp01(0.38F + focus * 0.32F));
        int border = NanoScreenKit.mixArgb(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 92), NanoRenderUtils.withAlpha(theme.accentArgb(), 142), UiMotion.clamp01(focus * 0.72F));
        float radius = Math.min(valueRect.h * 0.5F, Math.max(2.0F, theme.controlRadius()));
        NanoUi.drawSurface(vg, stack, valueRect.x, valueRect.y, valueRect.w, valueRect.h, radius, fill, border);
        NanoUi.drawRightText(vg, stack, font, valueRect.x2() - scaled(6.0F, scale), valueRect.y + valueRect.h * 0.5F + scaled(0.40F, scale), scaled(11.8F, scale), theme.textMutedArgb(), this.formatSliderValue(sliderKey, value));
    }

    private void drawButton(long vg, MemoryStack stack, int regular, NanoTheme theme, Rect button, String label, boolean active)
    {
        float k = UiMotion.clamp(button.h / 20.0F, 0.35F, 1.85F);
        boolean hovered = button.contains(this.mouseX, this.mouseY);
        int fill = active ? theme.controlActiveArgb() : (hovered ? theme.controlHoverArgb() : theme.controlArgb());
        NanoUi.drawSurface(vg, stack, button.x, button.y, button.w, button.h, theme.controlRadius(), fill, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 80));
        NanoUi.drawCenterText(vg, stack, regular, button.x + button.w * 0.5F, button.y + button.h * 0.5F, scaled(12.5F, k), theme.textArgb(), label);
    }

    private String formatSliderValue(String sliderKey, float value)
    {
        if ("ui_scale".equals(sliderKey) || "anchor_x".equals(sliderKey) || "anchor_y".equals(sliderKey))
        {
            return String.format(Locale.ROOT, "%d%%", Integer.valueOf(Math.round(value * 100.0F)));
        }

        if ("sidebar_ratio".equals(sliderKey) || "modules_ratio".equals(sliderKey))
        {
            return String.format(Locale.ROOT, "%d%%", Integer.valueOf(Math.round(value * 100.0F)));
        }

        if ("row_category".equals(sliderKey) || "row_module".equals(sliderKey) || "row_setting".equals(sliderKey) || "btn_height".equals(sliderKey) || "gap_major".equals(sliderKey) || "value_col".equals(sliderKey))
        {
            return String.format(Locale.ROOT, "%.1f", Float.valueOf(value));
        }

        return String.format(Locale.ROOT, "%.3f", Float.valueOf(value));
    }

    private Rect activeSliderInputRect(Layout l, String sliderKey)
    {
        if (l == null || sliderKey == null)
        {
            return null;
        }

        if ("ui_scale".equals(sliderKey))
        {
            return l.scaleValue;
        }

        if ("motion_speed".equals(sliderKey))
        {
            return l.motionValue;
        }

        if ("anchor_x".equals(sliderKey))
        {
            return l.anchorXValue;
        }

        if ("anchor_y".equals(sliderKey))
        {
            return l.anchorYValue;
        }

        return null;
    }

    private void activateSliderInput(Rect inputRect, float scale, int mouseX, int mouseY, String sliderKey, float value)
    {
        if (inputRect == null || sliderKey == null)
        {
            return;
        }

        this.commitActiveNumberInput();
        this.activeSliderInputKey = sliderKey;
        this.numberInputBuffer.set(this.sliderRawInputValue(sliderKey, value));

        if (this.lastNanoVg != 0L)
        {
            NanoFontBook.ensureLoaded(this.lastNanoVg);
        }

        this.numberInput.onMouseDown(mouseX, mouseY, inputRect.x, inputRect.y, inputRect.w, inputRect.h, this.lastNanoVg, NanoFontBook.uiRegular(), scaled(11.5F, UiMotion.clamp(scale, 0.35F, 1.85F)), this.numberInputBuffer.get());
    }

    private void validateActiveSliderInput()
    {
        if (this.activeSliderInputKey == null)
        {
            return;
        }

        if (!this.numberInput.isFocused())
        {
            this.activeSliderInputKey = null;
        }
    }

    private void commitActiveNumberInput()
    {
        if (this.activeSliderInputKey == null)
        {
            return;
        }

        this.applySliderInput(this.activeSliderInputKey, this.numberInputBuffer.get());
        this.activeSliderInputKey = null;
    }

    private boolean applySliderInput(String sliderKey, String text)
    {
        if (sliderKey == null || text == null)
        {
            return false;
        }

        String raw = text.trim();

        if (raw.isEmpty())
        {
            return false;
        }

        boolean percent = raw.endsWith("%");
        String token = percent ? raw.substring(0, raw.length() - 1).trim() : raw;
        token = token.replace(',', '.');

        if (token.isEmpty())
        {
            return false;
        }

        double parsed;

        try
        {
            parsed = Double.parseDouble(token);
        }
        catch (NumberFormatException ignored)
        {
            return false;
        }

        if (this.isPercentSlider(sliderKey) && (percent || Math.abs(parsed) > 1.0001D))
        {
            parsed *= 0.01D;
        }

        UiScaleEditModule.UiTarget target = this.module.getEditTarget();
        float value = (float)parsed;

        if ("ui_scale".equals(sliderKey))
        {
            this.module.setUiScale(target, UiMotion.clamp(value, this.module.getUiScaleMin(), this.module.getUiScaleMax()));
            return true;
        }

        if ("motion_speed".equals(sliderKey))
        {
            this.module.setMotionSpeed(target, UiMotion.clamp(value, this.module.getMotionSpeedMin(), this.module.getMotionSpeedMax()));
            return true;
        }

        if ("anchor_x".equals(sliderKey))
        {
            this.module.setWindowAnchor(target, UiMotion.clamp01(value), this.module.getWindowAnchorY(target));
            return true;
        }

        if ("anchor_y".equals(sliderKey))
        {
            this.module.setWindowAnchor(target, this.module.getWindowAnchorX(target), UiMotion.clamp01(value));
            return true;
        }

        return false;
    }

    private boolean isPercentSlider(String sliderKey)
    {
        return "ui_scale".equals(sliderKey) || "anchor_x".equals(sliderKey) || "anchor_y".equals(sliderKey);
    }

    private String sliderRawInputValue(String sliderKey, float value)
    {
        if (this.isPercentSlider(sliderKey))
        {
            return Integer.toString(Math.round(value * 100.0F));
        }

        return this.trimDecimal(value, 3);
    }

    private String trimDecimal(double value, int digits)
    {
        String text = String.format(Locale.ROOT, "%." + Math.max(0, digits) + "f", Double.valueOf(value));
        int end = text.length();

        while (end > 0 && text.charAt(end - 1) == '0')
        {
            --end;
        }

        if (end > 0 && text.charAt(end - 1) == '.')
        {
            --end;
        }

        return end <= 0 ? "0" : text.substring(0, end);
    }

    private void drawResizeHandle(long vg, MemoryStack stack, NanoTheme theme, Rect handle)
    {
        float k = UiMotion.clamp(handle.w / 10.0F, 0.35F, 1.85F);
        NanoUi.drawSurface(vg, stack, handle.x, handle.y, handle.w, handle.h, scaled(4.0F, k), NanoRenderUtils.withAlpha(theme.controlActiveArgb(), 188), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 90));
    }

    private void updateActiveDrags(Layout l)
    {
        if (l == null)
        {
            return;
        }

        if (this.draggingScale)
        {
            this.applyScaleFromMouse(l, this.mouseX);
            this.lastSliderDragNanos = System.nanoTime();
        }

        if (this.draggingMotion)
        {
            this.applyMotionFromMouse(l, this.mouseX);
            this.lastSliderDragNanos = System.nanoTime();
        }

        if (this.draggingAnchorX)
        {
            this.applyAnchorXFromMouse(l, this.mouseX);
            this.lastSliderDragNanos = System.nanoTime();
        }

        if (this.draggingAnchorY)
        {
            this.applyAnchorYFromMouse(l, this.mouseX);
            this.lastSliderDragNanos = System.nanoTime();
        }

    }

    private void applyScaleFromMouse(Layout layout, int mouseX)
    {
        UiScaleEditModule.UiTarget target = this.module.getEditTarget();
        float ratio = this.sliderRatioFromMouse(mouseX, layout.scaleTrack);
        ratio = UiMotion.clamp01(ratio);
        float value = this.module.getUiScaleMin() + (this.module.getUiScaleMax() - this.module.getUiScaleMin()) * ratio;
        if (Math.abs(this.module.getUiScale(target) - value) > 0.0001F)
        {
            this.module.setUiScale(target, value);
        }
    }

    private void applyMotionFromMouse(Layout layout, int mouseX)
    {
        UiScaleEditModule.UiTarget target = this.module.getEditTarget();
        float ratio = this.sliderRatioFromMouse(mouseX, layout.motionTrack);
        ratio = UiMotion.clamp01(ratio);
        float value = this.module.getMotionSpeedMin() + (this.module.getMotionSpeedMax() - this.module.getMotionSpeedMin()) * ratio;
        if (Math.abs(this.module.getMotionSpeed(target) - value) > 0.0001F)
        {
            this.module.setMotionSpeed(target, value);
        }
    }

    private void applyAnchorXFromMouse(Layout layout, int mouseX)
    {
        UiScaleEditModule.UiTarget target = this.module.getEditTarget();
        float ratio = this.sliderRatioFromMouse(mouseX, layout.anchorXTrack);
        float value = UiMotion.clamp01(ratio);
        if (Math.abs(this.module.getWindowAnchorX(target) - value) > 0.0001F)
        {
            this.module.setWindowAnchor(target, value, this.module.getWindowAnchorY(target));
        }
    }

    private void applyAnchorYFromMouse(Layout layout, int mouseX)
    {
        UiScaleEditModule.UiTarget target = this.module.getEditTarget();
        float ratio = this.sliderRatioFromMouse(mouseX, layout.anchorYTrack);
        float value = UiMotion.clamp01(ratio);
        if (Math.abs(this.module.getWindowAnchorY(target) - value) > 0.0001F)
        {
            this.module.setWindowAnchor(target, this.module.getWindowAnchorX(target), value);
        }
    }

    private void applyAnimationSpeedFromMouse(Layout layout, int mouseX)
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();

        if (clickGui == null)
        {
            return;
        }

        float ratio = (mouseX - layout.animSpeedTrack.x) / Math.max(1.0F, layout.animSpeedTrack.w);
        ratio = UiMotion.clamp01(ratio);
        float value = 0.05F + (1.0F - 0.05F) * ratio;
        clickGui.setGlobalAnimationSpeed(value);
    }

    private void applyAnimationSmoothFromMouse(Layout layout, int mouseX)
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();

        if (clickGui == null)
        {
            return;
        }

        float ratio = (mouseX - layout.animSmoothTrack.x) / Math.max(1.0F, layout.animSmoothTrack.w);
        ratio = UiMotion.clamp01(ratio);
        clickGui.setGlobalAnimationSmooth(ratio);
    }

    private UiLayoutProfile liveLayout()
    {
        ClientBootstrap boot = ClientBootstrap.instance();

        if (boot != null && boot.getConfigManager() != null)
        {
            return boot.getConfigManager().getUiLayout();
        }

        return new UiLayoutProfile();
    }

    private void markLayoutDirty()
    {
        ClientBootstrap boot = ClientBootstrap.instance();

        if (boot != null && boot.getConfigManager() != null)
        {
            boot.getConfigManager().markClientDirty();
        }
    }

    private boolean handleLayoutSliderClick(Layout l, int mouseX)
    {
        Rect[] tracks = { l.sidebarRatioTrack, l.modulesRatioTrack, l.rowCategoryTrack, l.rowModuleTrack, l.rowSettingTrack, l.btnHeightTrack, l.gapMajorTrack, l.valueColTrack };
        boolean[] flags = new boolean[8];

        for (int i = 0; i < tracks.length; i++)
        {
            if (tracks[i] != null && tracks[i].contains(mouseX, this.mouseY))
            {
                this.commitActiveNumberInput();
                this.numberInput.blur();
                flags[i] = true;
                this.lockSliderTrack(tracks[i]);
                break;
            }
        }

        this.draggingSidebarRatio = flags[0];
        this.draggingModulesRatio = flags[1];
        this.draggingRowCategory = flags[2];
        this.draggingRowModule = flags[3];
        this.draggingRowSetting = flags[4];
        this.draggingBtnHeight = flags[5];
        this.draggingGapMajor = flags[6];
        this.draggingValueCol = flags[7];

        for (boolean f : flags)
        {
            if (f)
            {
                this.applyLayoutSliderFromMouse(l, mouseX);
                return true;
            }
        }

        return false;
    }

    private void updateLayoutSliderDrags(Layout l, int mouseX)
    {
        if (this.draggingSidebarRatio || this.draggingModulesRatio || this.draggingRowCategory || this.draggingRowModule || this.draggingRowSetting || this.draggingBtnHeight || this.draggingGapMajor || this.draggingValueCol)
        {
            this.applyLayoutSliderFromMouse(l, mouseX);
        }
    }

    private void applyLayoutSliderFromMouse(Layout l, int mouseX)
    {
        UiLayoutProfile lp = this.liveLayout();

        if (this.draggingSidebarRatio)
        {
            float ratio = this.sliderRatioFromMouse(mouseX, l.sidebarRatioTrack);
            float value = lp.clickGuiSidebarMinRatio() + (lp.clickGuiSidebarMaxRatio() - lp.clickGuiSidebarMinRatio()) * UiMotion.clamp01(ratio);
            lp.setClickGuiSidebarRatio(value);
            this.markLayoutDirty();
        }

        if (this.draggingModulesRatio)
        {
            float ratio = this.sliderRatioFromMouse(mouseX, l.modulesRatioTrack);
            float value = lp.clickGuiModulesMinRatio() + (lp.clickGuiModulesMaxRatio() - lp.clickGuiModulesMinRatio()) * UiMotion.clamp01(ratio);
            lp.setClickGuiModulesRatio(value);
            this.markLayoutDirty();
        }

        if (this.draggingRowCategory)
        {
            float ratio = this.sliderRatioFromMouse(mouseX, l.rowCategoryTrack);
            lp.setRowCategory(18.0F + (36.0F - 18.0F) * UiMotion.clamp01(ratio));
            this.markLayoutDirty();
        }

        if (this.draggingRowModule)
        {
            float ratio = this.sliderRatioFromMouse(mouseX, l.rowModuleTrack);
            lp.setRowModule(24.0F + (48.0F - 24.0F) * UiMotion.clamp01(ratio));
            this.markLayoutDirty();
        }

        if (this.draggingRowSetting)
        {
            float ratio = this.sliderRatioFromMouse(mouseX, l.rowSettingTrack);
            lp.setRowSetting(20.0F + (38.0F - 20.0F) * UiMotion.clamp01(ratio));
            this.markLayoutDirty();
        }

        if (this.draggingBtnHeight)
        {
            float ratio = this.sliderRatioFromMouse(mouseX, l.btnHeightTrack);
            lp.setBtnHeight(16.0F + (28.0F - 16.0F) * UiMotion.clamp01(ratio));
            this.markLayoutDirty();
        }

        if (this.draggingGapMajor)
        {
            float ratio = this.sliderRatioFromMouse(mouseX, l.gapMajorTrack);
            lp.setGapMajor(6.0F + (24.0F - 6.0F) * UiMotion.clamp01(ratio));
            this.markLayoutDirty();
        }

        if (this.draggingValueCol)
        {
            float ratio = this.sliderRatioFromMouse(mouseX, l.valueColTrack);
            lp.setValueColWidth(80.0F + (200.0F - 80.0F) * UiMotion.clamp01(ratio));
            this.markLayoutDirty();
        }
    }

    private void drawLayoutSliders(long vg, MemoryStack stack, int regular, int bold, NanoTheme theme, Layout l)
    {
        UiLayoutProfile lp = this.liveLayout();
        float k = l.scale;

        this.drawSlider(vg, stack, regular, bold, theme, l.sidebarRatioTrack, null, "sidebar_ratio", this.tr("uiedit.slider.sidebar_ratio", "Sidebar Width"), lp.clickGuiSidebarRatio(), lp.clickGuiSidebarMinRatio(), lp.clickGuiSidebarMaxRatio(), null, false, this.draggingSidebarRatio, l.sidebarRatioTrack.contains(this.mouseX, this.mouseY));
        this.drawSlider(vg, stack, regular, bold, theme, l.modulesRatioTrack, null, "modules_ratio", this.tr("uiedit.slider.modules_ratio", "Modules Height"), lp.clickGuiModulesRatio(), lp.clickGuiModulesMinRatio(), lp.clickGuiModulesMaxRatio(), null, false, this.draggingModulesRatio, l.modulesRatioTrack.contains(this.mouseX, this.mouseY));
        this.drawSlider(vg, stack, regular, bold, theme, l.rowCategoryTrack, null, "row_category", this.tr("uiedit.slider.row_category", "Row Category"), lp.rowCategory(), 18.0F, 36.0F, null, false, this.draggingRowCategory, l.rowCategoryTrack.contains(this.mouseX, this.mouseY));
        this.drawSlider(vg, stack, regular, bold, theme, l.rowModuleTrack, null, "row_module", this.tr("uiedit.slider.row_module", "Row Module"), lp.rowModule(), 24.0F, 48.0F, null, false, this.draggingRowModule, l.rowModuleTrack.contains(this.mouseX, this.mouseY));
        this.drawSlider(vg, stack, regular, bold, theme, l.rowSettingTrack, null, "row_setting", this.tr("uiedit.slider.row_setting", "Row Setting"), lp.rowSetting(), 20.0F, 38.0F, null, false, this.draggingRowSetting, l.rowSettingTrack.contains(this.mouseX, this.mouseY));
        this.drawSlider(vg, stack, regular, bold, theme, l.btnHeightTrack, null, "btn_height", this.tr("uiedit.slider.btn_height", "Button Height"), lp.btnHeight(), 16.0F, 28.0F, null, false, this.draggingBtnHeight, l.btnHeightTrack.contains(this.mouseX, this.mouseY));
        this.drawSlider(vg, stack, regular, bold, theme, l.gapMajorTrack, null, "gap_major", this.tr("uiedit.slider.gap_major", "Gap Major"), lp.gapMajor(), 6.0F, 24.0F, null, false, this.draggingGapMajor, l.gapMajorTrack.contains(this.mouseX, this.mouseY));
        this.drawSlider(vg, stack, regular, bold, theme, l.valueColTrack, null, "value_col", this.tr("uiedit.slider.value_col", "Value Column"), lp.valueColWidth(), 80.0F, 200.0F, null, false, this.draggingValueCol, l.valueColTrack.contains(this.mouseX, this.mouseY));
    }

    private void resetLayoutDefaults()
    {
        UiLayoutProfile lp = this.liveLayout();
        lp.setClickGuiSidebarRatio(0.24F);
        lp.setClickGuiModulesRatio(0.31F);
        lp.setRowCategory(24.0F);
        lp.setRowModule(34.0F);
        lp.setRowSetting(26.0F);
        lp.setBtnHeight(20.0F);
        lp.setGapMajor(14.0F);
        lp.setValueColWidth(132.0F);
        this.markLayoutDirty();
    }

    private void syncWindowTarget()
    {
        UiScaleEditModule.UiTarget target = this.module.getEditTarget();
        float profileScale = this.module.getUiScale(target);
        float targetWidth = UiMotion.clamp(BASE_WIDTH * profileScale, MIN_WIDTH, (float)this.width - SCREEN_MARGIN * 2.0F);
        float targetHeight = UiMotion.clamp(BASE_HEIGHT * profileScale, MIN_HEIGHT, (float)this.height - SCREEN_MARGIN * 2.0F);
        float targetX = this.module.getWindowAnchorX(target) * (float)this.width - targetWidth * 0.5F;
        float targetY = this.module.getWindowAnchorY(target) * (float)this.height - targetHeight * 0.5F;
        boolean freezeAnchor = this.draggingAnchorX || this.draggingAnchorY;

        if (!this.window.isInitialized())
        {
            float spawnScale = 0.94F;
            float spawnW = targetWidth * spawnScale;
            float spawnH = targetHeight * spawnScale;
            float spawnX = targetX + (targetWidth - spawnW) * 0.5F;
            float spawnY = targetY + (targetHeight - spawnH) * 0.5F;
            this.window.setImmediate(spawnX, spawnY, spawnW, spawnH);
            this.window.setTarget(targetX, targetY, targetWidth, targetHeight, (float)this.width, (float)this.height, SCREEN_MARGIN);
            return;
        }

        if (!this.window.isInteracting() && !freezeAnchor)
        {
            this.window.setTarget(targetX, targetY, targetWidth, targetHeight, (float)this.width, (float)this.height, SCREEN_MARGIN);
        }
    }

    private void lockSliderTrack(Rect track)
    {
        if (track == null)
        {
            this.clearSliderTrackLock();
            return;
        }

        this.draggingSliderTrackLocked = true;
        this.draggingSliderTrackX = track.x;
        this.draggingSliderTrackW = Math.max(1.0F, track.w);
    }

    private void clearSliderTrackLock()
    {
        this.draggingSliderTrackLocked = false;
        this.draggingSliderTrackX = 0.0F;
        this.draggingSliderTrackW = 0.0F;
    }

    private float sliderRatioFromMouse(int mouseX, Rect track)
    {
        if (track == null)
        {
            return 0.0F;
        }

        float x = this.draggingSliderTrackLocked ? this.draggingSliderTrackX : track.x;
        float w = this.draggingSliderTrackLocked ? this.draggingSliderTrackW : track.w;
        return NanoSliderController.mouseRatio((float)mouseX, x, w);
    }

    private void syncProfileFromWindow()
    {
        UiScaleEditModule.UiTarget target = this.module.getEditTarget();
        float widthRatio = this.window.getTargetWidth() / BASE_WIDTH;
        float heightRatio = this.window.getTargetHeight() / BASE_HEIGHT;
        this.module.setUiScale(target, Math.min(widthRatio, heightRatio));
        float cx = this.window.getTargetX() + this.window.getTargetWidth() * 0.5F;
        float cy = this.window.getTargetY() + this.window.getTargetHeight() * 0.5F;
        float anchorX = cx / Math.max(1.0F, (float)this.width);
        float anchorY = cy / Math.max(1.0F, (float)this.height);
        this.module.setWindowAnchor(target, anchorX, anchorY);
    }

    private void openSelectedTarget()
    {
        if (this.mc == null)
        {
            return;
        }

        UiScaleEditModule.UiTarget target = this.module.getEditTarget();

        if (target == UiScaleEditModule.UiTarget.HUD_EDIT)
        {
            Module hudEdit = ClientBootstrap.instance().getModules().getById("hud_edit");

            if (hudEdit != null)
            {
                hudEdit.setEnabled(true);
                return;
            }

            this.mc.displayGuiScreen(new HudEditorScreen(ClientBootstrap.instance().getHud()));
            return;
        }

        this.mc.displayGuiScreen(new ClickGuiScreen(ClientBootstrap.instance().getModules()));
    }

    private GuiScreen createTargetScreen()
    {
        UiScaleEditModule.UiTarget target = this.module.getEditTarget();

        if (target == UiScaleEditModule.UiTarget.HUD_EDIT)
        {
            return new HudEditorScreen(ClientBootstrap.instance().getHud());
        }

        return new ClickGuiScreen(ClientBootstrap.instance().getModules());
    }

    private void goBack()
    {
        if (this.mc == null)
        {
            return;
        }

        if (this.parentScreen != null && this.parentScreen != this)
        {
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        }

        this.mc.displayGuiScreen((GuiScreen)null);
    }

    private void requestTransition(ScreenTransitionController.Mode mode, GuiScreen target)
    {
        if (!ScreenTransitionController.canRequest(this.mc, this.transitioningOut))
        {
            return;
        }

        this.transitionMode = ScreenTransitionController.modeOrDefault(mode);
        this.transitionTarget = target;
        this.transitioningOut = true;
        this.transitionExecuted = false;
    }

    private void updateTransition(ClickGuiModule clickGui)
    {
        float speed = clickGui == null ? 0.56F : clickGui.getGlobalAnimationSpeed();
        float smooth = UiAnimProfiles.uiScaleProfile(clickGui).smooth();
        UiAnimation.Type type = this.resolveTransitionType(clickGui);
        ScreenTransitionController.StepResult step = ScreenTransitionController.step(
            this.transitioningOut,
            this.transitionMode,
            this.transitionProgress,
            this.transitionLastNanos,
            speed,
            smooth,
            type,
            TRANSITION_TUNING,
            this.transitionExecuted,
            this.transitionTarget,
            this.mc
        );
        this.transitionLastNanos = step.lastNanos();
        this.transitionProgress = step.progress();

        if (step.shouldNavigate())
        {
            this.transitionExecuted = true;
            this.mc.displayGuiScreen(this.transitionTarget);
        }
    }

    private UiAnimation.Type resolveTransitionType(ClickGuiModule clickGui)
    {
        if (clickGui == null)
        {
            return UiAnimation.Type.EASE_OUT;
        }

        return ScreenTransitionController.resolveType(
            this.transitioningOut,
            this.transitionMode,
            clickGui.getGuiOpenAnimationType(),
            clickGui.getGuiCloseAnimationType(),
            clickGui.getGuiSwitchAnimationType(),
            clickGui.getGuiBackAnimationType()
        );
    }

    private float transitionVisual()
    {
        return ScreenTransitionController.visual(this.transitionProgress);
    }

    private Rect transitionWindow(Rect baseRect)
    {
        if (baseRect == null)
        {
            return this.fallbackWindow();
        }

        float p = UiMotion.clamp01(this.transitionProgress);
        float inv = 1.0F - p;

        if (!this.transitioningOut && p >= 0.999F)
        {
            return baseRect;
        }

        float k = UiMotion.clamp(baseRect.w / BASE_WIDTH, 0.35F, 1.85F);
        float shiftX = 0.0F;
        float shiftY = 0.0F;
        float scale = 0.95F + p * 0.05F;

        if (this.transitioningOut)
        {
            switch (this.transitionMode)
            {
                case BACK:
                    shiftX = scaled(22.0F, k) * inv;
                    shiftY = scaled(4.0F, k) * inv;
                    break;
                case CLOSE:
                    shiftY = scaled(16.0F, k) * inv;
                    break;
                case SWITCH:
                default:
                    shiftX = -scaled(10.0F, k) * inv;
                    break;
            }
        }
        else
        {
            shiftY = scaled(12.0F, k) * inv;
        }

        float newW = baseRect.w * scale;
        float newH = baseRect.h * scale;
        float cx = baseRect.x + baseRect.w * 0.5F + shiftX;
        float cy = baseRect.y + baseRect.h * 0.5F + shiftY;
        return new Rect(cx - newW * 0.5F, cy - newH * 0.5F, newW, newH);
    }

    private NanoTheme applyThemeTransition(NanoTheme theme, float alpha)
    {
        return new NanoTheme(
            NanoRenderUtils.mulAlpha(theme.backdropArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.windowTopArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.windowBottomArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.windowBorderArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.sidebarArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.mainArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.cardArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.cardAltArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.rowArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.rowHoverArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.rowSelectedArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.controlArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.controlHoverArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.controlActiveArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.textArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.textMutedArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.textWeakArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.accentArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.accentSoftArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.successArgb(), alpha),
            NanoRenderUtils.mulAlpha(theme.dangerArgb(), alpha),
            theme.windowRadius(),
            theme.surfaceRadius(),
            theme.cardRadius(),
            theme.controlRadius()
        );
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

            int backdrop = clickGui.isBackdropEnabled() ? clickGui.getBackdropAlpha() : 0;
            return NanoThemes.create(clickGui.getPalette(), clickGui.getPanelAlpha(), backdrop, clickGui.getCornerRadius(), accent);
        }

        return NanoThemes.create(NanoPalette.COBALT, 220, 96, 12.0F, null);
    }

    private ClickGuiModule resolveClickGuiModule()
    {
        Module module = ClientBootstrap.instance().getModules().getById("click_gui");
        return module instanceof ClickGuiModule ? (ClickGuiModule)module : null;
    }

    private void refreshLiveMousePosition()
    {
        this.mouseX = Math.round(this.liveMouseX());
        this.mouseY = Math.round(this.liveMouseY());
    }

    private float liveMouseX()
    {
        return NanoScreenKit.liveMouseX(this.mc, this.mouseX, this.width);
    }

    private float liveMouseY()
    {
        return NanoScreenKit.liveMouseY(this.mc, this.mouseY, this.height);
    }

    private String tr(String key, String fallback, Object... args)
    {
        client.i18n.I18nManager i18n = ClientBootstrap.instance().getI18n();
        return i18n == null ? fallback : i18n.translateOrDefault(key, fallback, args);
    }

    private UiAnimProfile resolveAnimationProfile(ClickGuiModule clickGui, UiScaleEditModule.UiTarget target)
    {
        boolean interacting = this.window.isInteracting()
            || this.draggingScale
            || this.draggingMotion
            || this.draggingAnchorX
            || this.draggingAnchorY
            || this.draggingAnimSpeed
            || this.draggingAnimSmooth
            || this.draggingSidebarRatio
            || this.draggingModulesRatio
            || this.draggingRowCategory
            || this.draggingRowModule
            || this.draggingRowSetting
            || this.draggingBtnHeight
            || this.draggingGapMajor
            || this.draggingValueCol;
        return UiAnimProfiles.uiScaleWindowProfile(clickGui, this.module, target, interacting);
    }

    private Rect fallbackWindow()
    {
        float width = Math.min(Math.max(MIN_WIDTH, BASE_WIDTH), (float)this.width - SCREEN_MARGIN * 2.0F);
        float height = Math.min(Math.max(MIN_HEIGHT, BASE_HEIGHT), (float)this.height - SCREEN_MARGIN * 2.0F);
        return new Rect(((float)this.width - width) * 0.5F, ((float)this.height - height) * 0.5F, width, height);
    }

    private Layout layout()
    {
        Rect baseRect = this.window.isInitialized() ? new Rect(this.window.getX(), this.window.getY(), this.window.getWidth(), this.window.getHeight()) : this.fallbackWindow();
        Rect windowRect = this.transitionWindow(baseRect);
        float k = UiMotion.clamp(windowRect.w / BASE_WIDTH, 0.35F, 1.85F);
        Rect header = new Rect(windowRect.x + scaled(1.0F, k), windowRect.y + scaled(1.0F, k), windowRect.w - scaled(2.0F, k), scaled(38.0F, k));
        Rect headerDrag = new Rect(header.x + scaled(2.0F, k), header.y + scaled(2.0F, k), header.w - scaled(4.0F, k), header.h - scaled(4.0F, k));
        Rect body = new Rect(windowRect.x + scaled(12.0F, k), header.y2() + scaled(10.0F, k), windowRect.w - scaled(24.0F, k), windowRect.h - scaled(58.0F, k));

        float chipW = (body.w - scaled(12.0F, k)) * 0.5F;
        Rect targetClickGui = new Rect(body.x + scaled(4.0F, k), body.y + scaled(8.0F, k), chipW, scaled(26.0F, k));
        Rect targetHudEdit = new Rect(targetClickGui.x2() + scaled(4.0F, k), body.y + scaled(8.0F, k), chipW, scaled(26.0F, k));

        float tabW = (body.w - scaled(12.0F, k)) * 0.5F;
        float tabY = targetClickGui.y2() + scaled(8.0F, k);
        Rect tabScale = new Rect(body.x + scaled(4.0F, k), tabY, tabW, scaled(22.0F, k));
        Rect tabLayout = new Rect(tabScale.x2() + scaled(4.0F, k), tabY, tabW, scaled(22.0F, k));

        float trackX = body.x + scaled(8.0F, k);
        float trackW = body.w - scaled(16.0F, k);
        float trackH = scaled(5.2F, k);
        float sliderStartY = tabScale.y2() + scaled(24.0F, k);
        float sliderEndY = body.y2() - scaled(94.0F, k);

        if (sliderEndY < sliderStartY)
        {
            sliderEndY = sliderStartY;
        }

        float scaleRowGap = UiMotion.clamp((sliderEndY - sliderStartY) / 5.0F, scaled(20.0F, k), scaled(44.0F, k));
        boolean showSliderHints = scaleRowGap >= scaled(30.0F, k) && k >= 0.72F;
        Rect scaleTrack = new Rect(trackX, sliderStartY, trackW, trackH);
        Rect motionTrack = new Rect(trackX, sliderStartY + scaleRowGap, trackW, trackH);
        Rect anchorXTrack = new Rect(trackX, sliderStartY + scaleRowGap * 2.0F, trackW, trackH);
        Rect anchorYTrack = new Rect(trackX, sliderStartY + scaleRowGap * 3.0F, trackW, trackH);
        Rect scaleValue = this.sliderValueRect(scaleTrack, k);
        Rect motionValue = this.sliderValueRect(motionTrack, k);
        Rect anchorXValue = this.sliderValueRect(anchorXTrack, k);
        Rect anchorYValue = this.sliderValueRect(anchorYTrack, k);
        Rect animSpeedTrack = new Rect(trackX, sliderStartY + scaleRowGap * 4.0F, trackW, trackH);
        Rect animSmoothTrack = new Rect(trackX, sliderStartY + scaleRowGap * 5.0F, trackW, trackH);

        float layoutRowGap = UiMotion.clamp((sliderEndY - sliderStartY) / 9.0F, scaled(16.0F, k), scaled(36.0F, k));
        Rect sidebarRatioTrack = new Rect(trackX, sliderStartY, trackW, trackH);
        Rect modulesRatioTrack = new Rect(trackX, sliderStartY + layoutRowGap, trackW, trackH);
        Rect rowCategoryTrack = new Rect(trackX, sliderStartY + layoutRowGap * 2.0F, trackW, trackH);
        Rect rowModuleTrack = new Rect(trackX, sliderStartY + layoutRowGap * 3.0F, trackW, trackH);
        Rect rowSettingTrack = new Rect(trackX, sliderStartY + layoutRowGap * 4.0F, trackW, trackH);
        Rect btnHeightTrack = new Rect(trackX, sliderStartY + layoutRowGap * 5.0F, trackW, trackH);
        Rect gapMajorTrack = new Rect(trackX, sliderStartY + layoutRowGap * 6.0F, trackW, trackH);
        Rect valueColTrack = new Rect(trackX, sliderStartY + layoutRowGap * 7.0F, trackW, trackH);

        float buttonsY = body.y2() - scaled(28.0F, k);
        float buttonW = (body.w - scaled(28.0F, k)) / 3.0F;
        Rect openButton = new Rect(body.x + scaled(8.0F, k), buttonsY, buttonW, scaled(24.0F, k));
        Rect backButton = new Rect(openButton.x2() + scaled(4.0F, k), buttonsY, buttonW, scaled(24.0F, k));
        Rect resetButton = new Rect(backButton.x2() + scaled(4.0F, k), buttonsY, buttonW, scaled(24.0F, k));

        Rect animEnabledButton = new Rect(0.0F, 0.0F, 0.0F, 0.0F);
        Rect animTypeButton = new Rect(0.0F, 0.0F, 0.0F, 0.0F);

        Rect resize = new Rect(windowRect.x2() - scaled(14.0F, k), windowRect.y2() - scaled(14.0F, k), scaled(10.0F, k), scaled(10.0F, k));
        return new Layout(windowRect, header, headerDrag, body, targetClickGui, targetHudEdit, scaleTrack, motionTrack, anchorXTrack, anchorYTrack, scaleValue, motionValue, anchorXValue, anchorYValue, animSpeedTrack, animSmoothTrack, animEnabledButton, animTypeButton, openButton, backButton, resize, showSliderHints, k, tabScale, tabLayout, sidebarRatioTrack, modulesRatioTrack, rowCategoryTrack, rowModuleTrack, rowSettingTrack, btnHeightTrack, gapMajorTrack, valueColTrack, resetButton);
    }

    private static float scaled(float value, float scale)
    {
        return value * scale;
    }

    private Rect sliderValueRect(Rect track, float scale)
    {
        if (track == null)
        {
            return null;
        }

        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        float w = scaled(78.0F, k);
        float h = scaled(17.0F, k);
        float labelCenterY = track.y - scaled(13.0F, k);
        float y = labelCenterY - h * 0.5F;
        return new Rect(track.x2() - w, y, w, h);
    }

    private static final class Layout
    {
        private final Rect window;
        private final Rect header;
        private final Rect headerDrag;
        private final Rect body;
        private final Rect targetClickGui;
        private final Rect targetHudEdit;
        private final Rect scaleTrack;
        private final Rect motionTrack;
        private final Rect anchorXTrack;
        private final Rect anchorYTrack;
        private final Rect scaleValue;
        private final Rect motionValue;
        private final Rect anchorXValue;
        private final Rect anchorYValue;
        private final Rect animSpeedTrack;
        private final Rect animSmoothTrack;
        private final Rect animEnabledButton;
        private final Rect animTypeButton;
        private final Rect openButton;
        private final Rect backButton;
        private final Rect resizeHandle;
        private final boolean showSliderHints;
        private final float scale;
        private final Rect tabScale;
        private final Rect tabLayout;
        private final Rect sidebarRatioTrack;
        private final Rect modulesRatioTrack;
        private final Rect rowCategoryTrack;
        private final Rect rowModuleTrack;
        private final Rect rowSettingTrack;
        private final Rect btnHeightTrack;
        private final Rect gapMajorTrack;
        private final Rect valueColTrack;
        private final Rect resetButton;

        private Layout(Rect window, Rect header, Rect headerDrag, Rect body, Rect targetClickGui, Rect targetHudEdit, Rect scaleTrack, Rect motionTrack, Rect anchorXTrack, Rect anchorYTrack, Rect scaleValue, Rect motionValue, Rect anchorXValue, Rect anchorYValue, Rect animSpeedTrack, Rect animSmoothTrack, Rect animEnabledButton, Rect animTypeButton, Rect openButton, Rect backButton, Rect resizeHandle, boolean showSliderHints, float scale, Rect tabScale, Rect tabLayout, Rect sidebarRatioTrack, Rect modulesRatioTrack, Rect rowCategoryTrack, Rect rowModuleTrack, Rect rowSettingTrack, Rect btnHeightTrack, Rect gapMajorTrack, Rect valueColTrack, Rect resetButton)
        {
            this.window = window;
            this.header = header;
            this.headerDrag = headerDrag;
            this.body = body;
            this.targetClickGui = targetClickGui;
            this.targetHudEdit = targetHudEdit;
            this.scaleTrack = scaleTrack;
            this.motionTrack = motionTrack;
            this.anchorXTrack = anchorXTrack;
            this.anchorYTrack = anchorYTrack;
            this.scaleValue = scaleValue;
            this.motionValue = motionValue;
            this.anchorXValue = anchorXValue;
            this.anchorYValue = anchorYValue;
            this.animSpeedTrack = animSpeedTrack;
            this.animSmoothTrack = animSmoothTrack;
            this.animEnabledButton = animEnabledButton;
            this.animTypeButton = animTypeButton;
            this.openButton = openButton;
            this.backButton = backButton;
            this.resizeHandle = resizeHandle;
            this.showSliderHints = showSliderHints;
            this.scale = UiMotion.clamp(scale, 0.35F, 1.85F);
            this.tabScale = tabScale;
            this.tabLayout = tabLayout;
            this.sidebarRatioTrack = sidebarRatioTrack;
            this.modulesRatioTrack = modulesRatioTrack;
            this.rowCategoryTrack = rowCategoryTrack;
            this.rowModuleTrack = rowModuleTrack;
            this.rowSettingTrack = rowSettingTrack;
            this.btnHeightTrack = btnHeightTrack;
            this.gapMajorTrack = gapMajorTrack;
            this.valueColTrack = valueColTrack;
            this.resetButton = resetButton;
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

        private float y2()
        {
            return this.y + this.h;
        }

        private boolean contains(int mx, int my)
        {
            return (float)mx >= this.x && (float)my >= this.y && (float)mx <= this.x2() && (float)my <= this.y2();
        }
    }
}
