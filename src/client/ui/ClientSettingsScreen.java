package client.ui;

import client.core.ClientBootstrap;
import client.module.Module;
import client.module.ModuleManager;
import client.module.impl.client.ClickGuiModule;
import client.render.RenderContext2D;
import client.setting.BoolSetting;
import client.setting.ColorSetting;
import client.setting.ColorValue;
import client.setting.EnumSetting;
import client.setting.FloatSetting;
import client.setting.IntSetting;
import client.setting.NumberSetting;
import client.setting.Setting;
import client.setting.StringSetting;
import client.ui.template.NanoSliderController;
import client.ui.template.NanoTextInput;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiAnimProfiles;
import client.ui.template.UiAnimation;
import client.ui.template.UiAnimationBus;
import client.ui.template.UiMotion;
import client.ui.template.UiWindowState;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoPalette;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoThemes;
import dwgx.nano.NanoUi;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.MemoryStack.stackPush;

public final class ClientSettingsScreen extends GuiScreen implements NanoRenderableScreen
{
    private enum TransitionMode
    {
        NONE,
        CLOSE,
        SWITCH,
        BACK
    }

    private enum SettingsSection
    {
        THEME,
        ANIMATION
    }

    private static final float BASE_WIDTH = 900.0F;
    private static final float BASE_HEIGHT = 620.0F;
    private static final float MIN_WIDTH = 520.0F;
    private static final float MIN_HEIGHT = 340.0F;
    private static final float SCREEN_MARGIN = 8.0F;
    private static final float HEADER_HEIGHT = 36.0F;
    private static final float OUTER_PAD = 12.0F;
    private static final float ROW_SETTING = 26.0F;
    private static final float INFO_CARD_HEIGHT = 62.0F;
    private static final float DEBUG_CARD_HEIGHT = 34.0F;
    private static final float RADIUS_WINDOW = 9.0F;
    private static final float RADIUS_PANEL = 8.0F;
    private static final float RADIUS_ROW = 6.0F;
    private static final float RADIUS_CONTROL = 6.0F;
    private static final float VALUE_COL_WIDTH = 132.0F;
    private static final float RESET_COL_WIDTH = 38.0F;

    private final ModuleManager modules;
    private final GuiScreen parentScreen;
    private final UiWindowState window = new UiWindowState(MIN_WIDTH, MIN_HEIGHT);

    private int mouseX;
    private int mouseY;
    private int settingScroll;
    private float settingScrollVisual;
    private Setting<?> draggingSettingSlider;
    private boolean draggingSliderTrackLocked;
    private float draggingSliderTrackX;
    private float draggingSliderTrackW;
    private long lastSliderDragNanos;
    private Setting<?> activeNumberSetting;
    private final StringSetting numberInputBuffer = new StringSetting("__client_number_input_buffer", "Number Input", "Inline numeric input buffer", "", 40);
    private final NanoTextInput numberInput = new NanoTextInput();

    private ColorSetting activeColor;
    private boolean draggingSv;
    private boolean draggingHue;
    private boolean draggingAlpha;
    private boolean draggingPicker;
    private float pickerDragDx;
    private float pickerDragDy;
    private float pickerOffsetRelX = Float.NaN;
    private float pickerOffsetRelY = Float.NaN;
    private float pickerHue;
    private float pickerSat;
    private float pickerVal;
    private float pickerAlpha;
    private boolean pickerFloating;
    private long lastPickerCommitNanos;
    private long lastNanoAt;
    private long lastNanoVg;
    private SettingsSection section = SettingsSection.THEME;

    private TransitionMode transitionMode = TransitionMode.NONE;
    private GuiScreen transitionTarget;
    private boolean transitioningOut;
    private boolean transitionExecuted;
    private float transitionProgress = 1.0F;
    private long transitionLastNanos;

    public ClientSettingsScreen(ModuleManager modules, GuiScreen parentScreen)
    {
        this.modules = modules;
        this.parentScreen = parentScreen;
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }

    public void initGui()
    {
        this.transitionMode = TransitionMode.NONE;
        this.transitionTarget = null;
        this.transitioningOut = false;
        this.transitionExecuted = false;
        this.transitionProgress = 0.0F;
        this.transitionLastNanos = System.nanoTime();
        this.settingScroll = 0;
        this.settingScrollVisual = 0.0F;
        this.draggingSettingSlider = null;
        this.clearSliderTrackLock();
        this.lastSliderDragNanos = 0L;
        this.activeNumberSetting = null;
        this.numberInput.blur();
        this.lastPickerCommitNanos = 0L;
        this.pickerFloating = false;
        this.draggingPicker = false;
        this.pickerDragDx = 0.0F;
        this.pickerDragDy = 0.0F;
        this.pickerOffsetRelX = Float.NaN;
        this.pickerOffsetRelY = Float.NaN;
    }

    public void onGuiClosed()
    {
        this.window.endInteraction();
        this.draggingSettingSlider = null;
        this.clearSliderTrackLock();
        this.commitActiveNumberInput();
        this.activeNumberSetting = null;
        this.numberInput.blur();
        this.draggingSv = false;
        this.draggingHue = false;
        this.draggingAlpha = false;
        this.draggingPicker = false;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.activeNumberSetting != null && this.numberInput.isFocused())
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
            this.requestTransition(TransitionMode.BACK, this.parentScreen);
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        Layout l = this.layout();
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        this.validateActiveNumberSetting(clickGui);

        if (mouseButton == 0 && this.activeNumberSetting != null)
        {
            Rect activeRect = this.activeNumberInputRect(l, clickGui, this.activeNumberSetting);

            if (activeRect == null || !activeRect.contains(mouseX, mouseY))
            {
                this.commitActiveNumberInput();
                this.numberInput.blur();
            }
        }

        if (mouseButton == 0)
        {
            if (l.backButton.contains(mouseX, mouseY))
            {
                this.requestTransition(TransitionMode.BACK, this.parentScreen);
                return;
            }

            if (l.resizeHandle.contains(mouseX, mouseY))
            {
                this.window.startResize((float)mouseX, (float)mouseY, UiWindowState.ResizeMode.BOTTOM_RIGHT);
                return;
            }

            if (l.hasPicker && l.pickerCard != null && l.pickerCard.contains(mouseX, mouseY) && !l.pickerSv.contains(mouseX, mouseY) && !l.pickerHue.contains(mouseX, mouseY) && !l.pickerAlpha.contains(mouseX, mouseY) && !l.pickerPreview.contains(mouseX, mouseY))
            {
                this.draggingPicker = true;
                this.pickerDragDx = (float)mouseX - l.pickerCard.x;
                this.pickerDragDy = (float)mouseY - l.pickerCard.y;
                this.updatePickerManual(l, mouseX, mouseY);
                return;
            }

            if (clickGui != null)
            {
                if (l.debugResetTheme.contains(mouseX, mouseY))
                {
                    this.resetThemeSettings(clickGui);
                    return;
                }

                if (l.debugResetAnimation.contains(mouseX, mouseY))
                {
                    this.resetAnimationSettings(clickGui);
                    return;
                }

                if (l.debugFlushAnimation.contains(mouseX, mouseY))
                {
                    UiAnimationBus.clearAll();
                    return;
                }
            }

            if (l.resetSectionButton.contains(mouseX, mouseY))
            {
                clickGui = this.resolveClickGuiModule();

                if (clickGui != null)
                {
                    this.resetSectionSettings(clickGui);
                }

                return;
            }

            if (l.themeTab.contains(mouseX, mouseY))
            {
                this.section = SettingsSection.THEME;
                this.settingScroll = 0;
                this.settingScrollVisual = 0.0F;
                return;
            }

            if (l.animationTab.contains(mouseX, mouseY))
            {
                this.section = SettingsSection.ANIMATION;
                this.settingScroll = 0;
                this.settingScrollVisual = 0.0F;
                this.activeColor = null;
                return;
            }

            if (l.headerDrag.contains(mouseX, mouseY))
            {
                this.window.startMove((float)mouseX, (float)mouseY);
                return;
            }

            if (l.hasPicker)
            {
                if (l.pickerSv.contains(mouseX, mouseY))
                {
                    this.draggingSv = true;
                    this.updatePickerSv(l, mouseX, mouseY);
                    return;
                }

                if (l.pickerHue.contains(mouseX, mouseY))
                {
                    this.draggingHue = true;
                    this.updatePickerHue(l, mouseY);
                    return;
                }

                if (l.pickerAlpha.contains(mouseX, mouseY))
                {
                    this.draggingAlpha = true;
                    this.updatePickerAlpha(l, mouseX);
                    return;
                }
            }
        }

        if (clickGui != null && l.settingsRows.contains(mouseX, mouseY))
        {
            List<Setting<?>> settings = this.visibleSettings(clickGui);
            int scrollBase = (int)Math.floor((double)this.settingScrollVisual);
            float scrollOffset = (this.settingScrollVisual - (float)scrollBase) * l.rowSetting;
            int index = scrollBase + (int)(((float)mouseY - l.settingsRows.y + scrollOffset) / l.rowSetting);

            if (index >= 0 && index < settings.size())
            {
                Setting<?> setting = settings.get(index);
                int visibleIndex = index - scrollBase;

                if (visibleIndex >= 0 && mouseButton == 0)
                {
                    Rect row = this.settingRowRect(l, visibleIndex, scrollOffset);
                    Rect resetRect = this.settingResetRect(row, l.scale);

                    if (this.isSettingModified(setting) && resetRect.contains(mouseX, mouseY))
                    {
                        setting.reset();

                        if (setting == this.activeColor && setting instanceof ColorSetting)
                        {
                            this.loadPickerFrom(((ColorSetting)setting).get());
                        }

                        return;
                    }

                    if (this.isPaletteSetting(setting))
                    {
                        if (this.handlePaletteRowClick(setting, row, l.scale, mouseX, mouseY))
                        {
                            return;
                        }
                    }
                }

                if (mouseButton == 0 && this.isSliderSetting(setting))
                {
                    if (visibleIndex >= 0)
                    {
                        Rect row = this.settingRowRect(l, visibleIndex, scrollOffset);
                        Rect valueRect = this.settingValueRect(row, l.scale);

                        if (valueRect.contains(mouseX, mouseY))
                        {
                            this.activateNumberInput(valueRect, l.scale, mouseX, mouseY, setting);
                            return;
                        }

                        Rect track = this.settingSliderTrackRect(row, l.scale);
                        Rect hit = this.sliderHitRect(track, l.scale);

                        if (hit.contains(mouseX, mouseY))
                        {
                            this.draggingSettingSlider = setting;
                            this.lockSliderTrack(track);
                            this.commitActiveNumberInput();
                            this.numberInput.blur();
                            this.applySettingSliderFromMouse(setting, track, mouseX);
                            return;
                        }
                    }
                }

                this.onSettingClick(setting, mouseButton);
                return;
            }
        }

        if (this.activeColor != null && l.pickerCard != null && !l.pickerCard.contains(mouseX, mouseY))
        {
            this.activeColor = null;
            this.pickerFloating = false;
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

        if (this.draggingPicker)
        {
            this.updatePickerManual(l, mouseX, mouseY);
        }

        if (this.draggingSettingSlider != null)
        {
            this.updateDraggedSettingSlider(l, mouseX);
        }

        if (this.draggingSv)
        {
            this.updatePickerSv(l, mouseX, mouseY);
        }

        if (this.draggingHue)
        {
            this.updatePickerHue(l, mouseY);
        }

        if (this.draggingAlpha)
        {
            this.updatePickerAlpha(l, mouseX);
        }
    }

    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        if (this.draggingSv || this.draggingHue || this.draggingAlpha)
        {
            this.commitPickerColor(true);
        }

        if (this.draggingPicker)
        {
            Layout l = this.layout();
            this.persistPickerPosition(l);
        }

        this.draggingPicker = false;

        this.persistWindowState();
        this.window.endInteraction();
        this.draggingSettingSlider = null;
        this.clearSliderTrackLock();
        this.draggingSv = false;
        this.draggingHue = false;
        this.draggingAlpha = false;
        this.draggingPicker = false;
        this.numberInput.onMouseUp();
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

        int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        Layout l = this.layout();

        if (l.settingsRows.contains(x, y))
        {
            this.settingScroll += wheel > 0 ? -1 : 1;
            this.clampScroll(l, this.resolveClickGuiModule());
        }
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
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        this.validateActiveColor(clickGui);
        this.validateActiveNumberSetting(clickGui);
        this.updateTransition(clickGui);

        if (this.transitioningOut && this.transitionExecuted)
        {
            return;
        }

        this.syncWindowTarget();

        if (this.window.isInteracting())
        {
            this.window.updateInteraction(this.liveMouseX(), this.liveMouseY(), (float)this.width, (float)this.height, SCREEN_MARGIN);
        }

        UiAnimProfile windowAnim = this.resolveWindowAnimationProfile(clickGui);
        this.window.tick(windowAnim);
        Layout l = this.layout();
        this.clampScroll(l, clickGui);
        this.updateScrollAnimation(l, clickGui);
        this.updateActiveDrags(l);
        NanoTheme theme = this.applyThemeTransition(this.resolveTheme(clickGui), this.transitionVisual());
        NanoFontBook.ensureLoaded(vg);
        int regular = NanoFontBook.uiRegular();
        int bold = NanoFontBook.uiBold();

        try (MemoryStack stack = stackPush())
        {
            if ((theme.backdropArgb() >>> 24) > 0)
            {
                NanoUi.drawBackdrop(vg, stack, (float)this.width, (float)this.height, theme);
            }

            NanoUi.drawSurface(vg, stack, l.window.x, l.window.y, l.window.w, l.window.h, this.stableWindowRadius(l.scale), theme.windowTopArgb(), theme.windowBorderArgb());
            NanoUi.drawLeftText(vg, stack, bold, l.header.x + scaled(12.0F, l.scale), l.header.y + l.header.h * 0.5F, scaled(16.0F, l.scale), theme.textArgb(), this.tr("client.settings.title", "Setting Center"));
            this.drawButton(vg, stack, l.backButton, this.tr("ui.back", "Back"), false, regular, theme);

            NanoUi.drawSurface(vg, stack, l.settingsCard.x, l.settingsCard.y, l.settingsCard.w, l.settingsCard.h, this.stablePanelRadius(l.scale), theme.cardAltArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 52));

            if (clickGui == null)
            {
                NanoUi.drawCenterText(vg, stack, regular, l.settingsCard.x + l.settingsCard.w * 0.5F, l.settingsCard.y + l.settingsCard.h * 0.5F, scaled(12.0F, l.scale), theme.textWeakArgb(), this.tr("client.settings.clickgui_unavailable", "ClickGUI module unavailable"));
            }
            else
            {
                this.drawInfoPanel(vg, stack, l, theme, regular, bold);
                this.drawDebugPanel(vg, stack, l, theme, regular);
                this.drawSettingRows(vg, stack, l, theme, clickGui, regular, bold);

                if (l.hasPicker)
                {
                    this.drawColorPicker(vg, stack, l, theme, regular);
                }
            }

            this.drawResizeHandle(vg, stack, l, theme);
            context.getNanoVG().resetScissor();
        }
    }

    private void drawSettingRows(long vg, MemoryStack stack, Layout l, NanoTheme theme, ClickGuiModule clickGui, int regular, int bold)
    {
        this.drawSectionTabs(vg, stack, l, theme, regular);
        NanoUi.drawLeftText(vg, stack, bold, l.settingsCard.x + scaled(12.0F, l.scale), l.themeTab.y - scaled(9.0F, l.scale), scaled(12.5F, l.scale), theme.textArgb(), this.tr("client.settings.section_title", "{0} Settings", this.sectionLabel(this.section)));
        List<Setting<?>> settings = this.visibleSettings(clickGui);
        boolean hasModified = this.hasModifiedSettings(settings);
        this.drawButton(vg, stack, l.resetSectionButton, hasModified ? this.tr("client.settings.reset_group", "Reset Group") : this.tr("client.settings.clean", "Clean"), hasModified, regular, theme);

        if (settings.isEmpty())
        {
            NanoUi.drawCenterText(vg, stack, regular, l.settingsRows.x + l.settingsRows.w * 0.5F, l.settingsRows.y + l.settingsRows.h * 0.5F, scaled(11.0F, l.scale), theme.textWeakArgb(), this.section == SettingsSection.ANIMATION ? this.tr("client.settings.empty.animation", "No animation settings") : this.tr("client.settings.empty.theme", "No theme settings"));
            return;
        }

        int visible = Math.max(0, (int)Math.ceil((double)(l.settingsRows.h / l.rowSetting)) + 2);
        int scrollBase = (int)Math.floor((double)this.settingScrollVisual);
        float scrollOffset = (this.settingScrollVisual - (float)scrollBase) * l.rowSetting;
        NanoUi.beginClip(vg, l.settingsRows.x, l.settingsRows.y, l.settingsRows.w, l.settingsRows.h);
        float rowRadius = this.stableRowRadius(l.scale);
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);

        for (int i = 0; i < visible; ++i)
        {
            int idx = scrollBase + i;

            if (idx >= settings.size())
            {
                break;
            }

            Setting<?> setting = settings.get(idx);
            Rect row = this.settingRowRect(l, i, scrollOffset);
            boolean hovered = row.contains(this.mouseX, this.mouseY);
            boolean modified = this.isSettingModified(setting);
            String group = this.sectionGroupLabel(setting);
            String prevGroup = idx > 0 ? this.sectionGroupLabel(settings.get(idx - 1)) : "";
            Rect valueRect = this.settingValueRect(row, l.scale);
            Rect resetRect = this.settingResetRect(row, l.scale);

            // Group labels are hidden to avoid overlapping with row content.

            int base = i % 2 == 0 ? theme.rowArgb() : theme.cardAltArgb();
            NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, base, 0);
            float hoverRatio = UiAnimationBus.animateControl("client.settings.hover." + setting.getKey(), hovered ? 1.0F : 0.0F, animProfile);

            if (hoverRatio > 0.001F)
            {
                NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, NanoRenderUtils.mulAlpha(theme.rowHoverArgb(), UiMotion.clamp01(hoverRatio * 0.66F)), 0);
            }

            float nameX = row.x + scaled(10.0F, l.scale);

            if (modified)
            {
                float md = scaled(4.0F, l.scale);
                NanoUi.drawSurface(vg, stack, nameX, row.y + row.h * 0.5F - md * 0.5F, md, md, md * 0.5F, theme.accentArgb(), 0);
                nameX += scaled(8.0F, l.scale);
            }

            NanoUi.drawLeftText(vg, stack, regular, nameX, row.y + row.h * 0.5F, scaled(11.4F, l.scale), theme.textArgb(), this.settingDisplayName(setting));
            this.drawResetButton(vg, stack, resetRect, modified, regular, theme);

            if (setting instanceof ColorSetting)
            {
                ColorSetting colorSetting = (ColorSetting)setting;
                boolean editing = this.activeColor == colorSetting;

                if (editing)
                {
                    NanoUi.drawAccentFlag(vg, stack, row.x + scaled(4.0F, l.scale), row.y + scaled(5.0F, l.scale), scaled(2.5F, l.scale), row.h - scaled(10.0F, l.scale), theme.accentArgb());
                }

                NanoUi.drawRightText(vg, stack, regular, valueRect.x2() - scaled(14.0F, l.scale), row.y + row.h * 0.5F, scaled(9.8F, l.scale), theme.textWeakArgb(), this.settingValue(setting));
                NanoUi.drawColorSwatch(vg, stack, valueRect.x2() - scaled(9.0F, l.scale), row.y + scaled(5.0F, l.scale), scaled(11.0F, l.scale), colorSetting.get().toArgb(), true);
            }
            else if (this.isPaletteSetting(setting))
            {
                this.drawPaletteRow(vg, stack, row, l.scale, regular, theme, clickGui);
            }
            else if (setting instanceof BoolSetting)
            {
                boolean enabled = ((BoolSetting)setting).isEnabled();
                this.drawStateLabel(vg, stack, valueRect, enabled, hovered, theme, l.scale, "client.settings.bool." + setting.getKey(), regular);
            }
            else if (this.isSliderSetting(setting))
            {
                this.drawSettingSlider(vg, stack, row, setting, hovered, theme, l.scale, regular, clickGui);
            }
            else
            {
                NanoUi.drawRightText(vg, stack, regular, valueRect.x2() - scaled(3.0F, l.scale), row.y + row.h * 0.5F, scaled(10.3F, l.scale), theme.textMutedArgb(), this.settingValue(setting));
            }
        }

        NanoUi.endClip(vg);
        int visibleRows = Math.max(1, (int)(l.settingsRows.h / l.rowSetting));
        this.drawRowsScrollbar(vg, stack, l, theme, settings.size(), visibleRows);
    }

    private void drawInfoPanel(long vg, MemoryStack stack, Layout l, NanoTheme theme, int regular, int bold)
    {
        NanoUi.drawSurface(vg, stack, l.infoCard.x, l.infoCard.y, l.infoCard.w, l.infoCard.h, this.stableControlRadius(l.scale), theme.cardArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 68));
        NanoUi.drawLeftText(vg, stack, bold, l.infoCard.x + scaled(10.0F, l.scale), l.infoCard.y + scaled(14.0F, l.scale), scaled(12.0F, l.scale), theme.textArgb(), this.tr("client.settings.profile.title", "Client Profile"));
        NanoUi.drawLeftText(vg, stack, regular, l.infoCard.x + scaled(10.0F, l.scale), l.infoCard.y + scaled(28.0F, l.scale), scaled(10.0F, l.scale), theme.textWeakArgb(), this.tr("client.settings.profile.subtitle", "DWGX Client / Setting Center"));
        int total = 0;
        int enabled = 0;

        if (this.modules != null)
        {
            List<Module> all = this.modules.getAll();
            total = all.size();

            for (int i = 0; i < all.size(); ++i)
            {
                if (all.get(i).isEnabled())
                {
                    ++enabled;
                }
            }
        }

        NanoUi.drawRightText(vg, stack, regular, l.infoCard.x2() - scaled(10.0F, l.scale), l.infoCard.y + scaled(16.0F, l.scale), scaled(10.0F, l.scale), theme.textMutedArgb(), this.tr("client.settings.profile.enabled", "Enabled {0}/{1}", Integer.valueOf(enabled), Integer.valueOf(total)));
        NanoUi.drawRightText(vg, stack, regular, l.infoCard.x2() - scaled(10.0F, l.scale), l.infoCard.y + scaled(30.0F, l.scale), scaled(10.0F, l.scale), theme.textWeakArgb(), this.tr("client.settings.profile.section", "Section: {0}", this.sectionLabel(this.section)));
    }

    private void drawDebugPanel(long vg, MemoryStack stack, Layout l, NanoTheme theme, int regular)
    {
        NanoUi.drawSurface(vg, stack, l.debugCard.x, l.debugCard.y, l.debugCard.w, l.debugCard.h, this.stableControlRadius(l.scale), theme.cardAltArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 58));
        NanoUi.drawLeftText(vg, stack, regular, l.debugCard.x + scaled(8.0F, l.scale), l.debugCard.y - scaled(6.0F, l.scale), scaled(9.6F, l.scale), theme.textWeakArgb(), this.tr("client.settings.debug_tools", "Debug Tools"));
        this.drawButton(vg, stack, l.debugResetTheme, this.tr("client.settings.debug.reset_theme", "Reset Theme"), false, regular, theme);
        this.drawButton(vg, stack, l.debugResetAnimation, this.tr("client.settings.debug.reset_animation", "Reset Animation"), false, regular, theme);
        this.drawButton(vg, stack, l.debugFlushAnimation, this.tr("client.settings.debug.flush_motion", "Flush Motion"), false, regular, theme);
    }

    private void drawRowsScrollbar(long vg, MemoryStack stack, Layout l, NanoTheme theme, int totalRows, int visibleRows)
    {
        if (totalRows <= visibleRows || visibleRows <= 0)
        {
            return;
        }

        float k = UiMotion.clamp(l.scale, 0.35F, 1.85F);
        float barW = scaled(3.0F, k);
        float barX = l.settingsRows.x2() - barW - scaled(1.0F, k);
        float barY = l.settingsRows.y + scaled(2.0F, k);
        float barH = Math.max(scaled(18.0F, k), l.settingsRows.h - scaled(4.0F, k));
        float ratio = UiMotion.clamp01((float)visibleRows / (float)totalRows);
        float thumbH = Math.max(scaled(12.0F, k), barH * ratio);
        int maxScroll = Math.max(1, totalRows - visibleRows);
        float scrollRatio = UiMotion.clamp01(this.settingScrollVisual / (float)maxScroll);
        float thumbY = barY + (barH - thumbH) * scrollRatio;
        NanoUi.drawSurface(vg, stack, barX, barY, barW, barH, barW * 0.5F, NanoRenderUtils.withAlpha(theme.controlArgb(), 90), 0);
        NanoUi.drawSurface(vg, stack, barX, thumbY, barW, thumbH, barW * 0.5F, NanoRenderUtils.withAlpha(theme.accentArgb(), 175), 0);
    }

    private void drawButton(long vg, MemoryStack stack, Rect rect, String label, boolean active, int font, NanoTheme theme)
    {
        boolean hovered = rect.contains(this.mouseX, this.mouseY);
        int fill = active ? theme.controlActiveArgb() : (hovered ? theme.controlHoverArgb() : theme.controlArgb());
        NanoUi.drawSurface(vg, stack, rect.x, rect.y, rect.w, rect.h, this.stableControlRadius(rect.h / 20.0F), fill, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 84));
        float k = UiMotion.clamp(rect.h / 20.0F, 0.35F, 1.85F);
        NanoUi.drawCenterText(vg, stack, font, rect.x + rect.w * 0.5F, rect.y + rect.h * 0.5F, scaled(11.0F, k), theme.textArgb(), label);
    }

    private void drawSectionTabs(long vg, MemoryStack stack, Layout l, NanoTheme theme, int font)
    {
        this.drawSectionTab(vg, stack, l.themeTab, SettingsSection.THEME, this.section == SettingsSection.THEME, theme, font);
        this.drawSectionTab(vg, stack, l.animationTab, SettingsSection.ANIMATION, this.section == SettingsSection.ANIMATION, theme, font);
    }

    private void drawSectionTab(long vg, MemoryStack stack, Rect rect, SettingsSection tab, boolean active, NanoTheme theme, int font)
    {
        boolean hovered = rect.contains(this.mouseX, this.mouseY);
        String key = "client.settings.tab." + tab.name().toLowerCase(Locale.ROOT);
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);
        float ratio = UiAnimationBus.animateControl(key, (active || hovered) ? 1.0F : 0.0F, animProfile);
        int fill = active ? theme.controlActiveArgb() : this.mixArgb(theme.controlArgb(), theme.controlHoverArgb(), UiMotion.clamp01(0.28F + ratio * 0.72F));
        NanoUi.drawSurface(vg, stack, rect.x, rect.y, rect.w, rect.h, this.stableControlRadius(rect.h / 18.0F), fill, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 80));
        float k = UiMotion.clamp(rect.h / 18.0F, 0.35F, 1.85F);
        int textColor = active ? theme.textArgb() : this.mixArgb(theme.textWeakArgb(), theme.textArgb(), UiMotion.clamp01(ratio * 0.75F));
        NanoUi.drawCenterText(vg, stack, font, rect.x + rect.w * 0.5F, rect.y + rect.h * 0.5F, scaled(10.8F, k), textColor, this.sectionLabel(tab));
    }

    private void drawResetButton(long vg, MemoryStack stack, Rect rect, boolean visible, int font, NanoTheme theme)
    {
        if (!visible)
        {
            return;
        }

        boolean hovered = rect.contains(this.mouseX, this.mouseY);
        int fill = hovered ? theme.controlHoverArgb() : theme.controlArgb();
        NanoUi.drawSurface(vg, stack, rect.x, rect.y, rect.w, rect.h, this.stableControlRadius(rect.h / 16.0F), fill, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 88));
        float k = UiMotion.clamp(rect.h / 16.0F, 0.35F, 1.85F);
        NanoUi.drawCenterText(vg, stack, font, rect.x + rect.w * 0.5F, rect.y + rect.h * 0.5F, scaled(8.6F, k), theme.textWeakArgb(), this.tr("clickgui.reset.short", "RST"));
    }

    private void drawStateLabel(long vg, MemoryStack stack, Rect valueRect, boolean enabled, boolean hovered, NanoTheme theme, float scale, String animKey, int font)
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        float targetRatio = enabled ? 1.0F : 0.0F;
        float ratio = UiAnimationBus.animateControl(animKey, targetRatio, animProfile);
        float focus = UiAnimationBus.animateControl(animKey + ".focus", hovered ? 1.0F : 0.0F, animProfile);
        float pillH = Math.min(valueRect.h, scaled(14.0F, k));
        float pillW = Math.min(valueRect.w, scaled(92.0F, k));
        float px = valueRect.x2() - pillW;
        float py = valueRect.y + (valueRect.h - pillH) * 0.5F;
        float thumbW = pillW * 0.5F - scaled(2.0F, k);
        float thumbX = px + scaled(1.0F, k) + (pillW - scaled(2.0F, k) - thumbW) * ratio;
        float thumbY = py + scaled(1.0F, k);
        float thumbH = pillH - scaled(2.0F, k);
        float thumbExpand = scaled(1.3F, k) * focus;
        int base = this.mixArgb(theme.controlArgb(), theme.controlHoverArgb(), UiMotion.clamp01(0.18F + focus * 0.48F));
        NanoUi.drawSurface(vg, stack, px, py, pillW, pillH, pillH * 0.5F, base, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 98));
        NanoUi.drawSurface(vg, stack, px + pillW * 0.5F, py + scaled(1.0F, k), scaled(1.0F, k), pillH - scaled(2.0F, k), scaled(0.5F, k), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 74), 0);
        int thumb = this.mixArgb(this.mixArgb(theme.cardAltArgb(), theme.controlHoverArgb(), 0.48F), theme.controlActiveArgb(), UiMotion.clamp01(ratio));
        NanoUi.drawSurface(vg, stack, thumbX - thumbExpand * 0.5F, thumbY - thumbExpand * 0.5F, thumbW + thumbExpand, thumbH + thumbExpand, (thumbH + thumbExpand) * 0.5F, thumb, NanoRenderUtils.withAlpha(0xFFFFFFFF, 84));
        int disableText = this.mixArgb(theme.textArgb(), theme.textMutedArgb(), UiMotion.clamp01(ratio * 0.95F));
        int enableText = this.mixArgb(theme.textMutedArgb(), theme.textArgb(), UiMotion.clamp01(ratio * 0.95F));
        float textCenterY = py + pillH * 0.5F;
        NanoUi.drawCenterText(vg, stack, font, px + pillW * 0.25F, textCenterY, scaled(8.2F, k), disableText, this.tr("clickgui.state.disable", "DISABLE"));
        NanoUi.drawCenterText(vg, stack, font, px + pillW * 0.75F, textCenterY, scaled(8.2F, k), enableText, this.tr("clickgui.state.enable", "ENABLE"));
    }

    private void drawSettingSlider(long vg, MemoryStack stack, Rect row, Setting<?> setting, boolean hovered, NanoTheme theme, float scale, int font, ClickGuiModule clickGui)
    {
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);
        Rect track = this.settingSliderTrackRect(row, k);
        Rect valueRect = this.settingValueRect(row, k);
        float ratio = this.settingSliderRatio(setting);
        String key = "client.settings.slider." + setting.getKey();
        boolean dragging = this.draggingSettingSlider == setting;
        Rect dragTrack = dragging ? this.resolveSliderDragTrack(track) : track;
        float visualTarget = dragging ? NanoSliderController.mouseRatio((float)this.mouseX, dragTrack.x, dragTrack.w) : ratio;
        float displayRatio = NanoSliderController.resolveDisplayRatio(key + ".track", visualTarget, dragging, animProfile);
        float fillRatio = NanoSliderController.resolveFillRatio(key, visualTarget, dragging, animProfile);
        float knobRatio = NanoSliderController.resolveKnobRatio(key, visualTarget, dragging, animProfile);
        float focus = NanoSliderController.resolveFocus(key + ".focus", hovered, dragging, animProfile);
        float glowRatio = NanoSliderController.resolveGlow(key + ".glow", hovered, dragging, animProfile);
        int trackFill = this.mixArgb(theme.cardAltArgb(), theme.controlArgb(), UiMotion.clamp01(0.44F + focus * 0.30F));
        float trackRadius = Math.min(track.h * 0.5F, this.stableControlRadius(k));
        NanoUi.drawSurface(vg, stack, track.x, track.y, track.w, track.h, trackRadius, trackFill, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 114));
        float rawHandleX = track.x + knobRatio * track.w;
        float knobSize = scaled(5.8F, k) + scaled(1.4F, k) * focus + scaled(1.0F, k) * glowRatio + (dragging ? scaled(1.1F, k) : 0.0F);
        float handleX = UiMotion.clamp(rawHandleX, track.x + knobSize * 0.5F, track.x + track.w - knobSize * 0.5F);
        float knobX = handleX - knobSize * 0.5F;
        float knobY = track.y + (track.h - knobSize) * 0.5F;
        float trackInnerX = track.x + scaled(1.0F, k);
        float trackInnerW = Math.max(0.0F, track.w - scaled(2.0F, k));
        float knobLeadX = handleX - knobSize * 0.5F;
        float fillTargetEnd = trackInnerX + trackInnerW * fillRatio;
        float fillEnd = Math.min(fillTargetEnd, knobLeadX);
        float fillW = Math.max(0.0F, fillEnd - trackInnerX);
        int activeFill = this.mixArgb(theme.controlActiveArgb(), theme.accentArgb(), 0.74F);
        NanoUi.drawSurface(vg, stack, trackInnerX, track.y + scaled(1.0F, k), fillW, track.h - scaled(2.0F, k), Math.max(scaled(1.6F, k), trackRadius - scaled(1.6F, k)), activeFill, 0);
        float lineGlowTargetEnd = trackInnerX + trackInnerW * displayRatio;
        float lineGlowEnd = Math.min(lineGlowTargetEnd, knobLeadX);
        float lineGlowW = Math.max(0.0F, lineGlowEnd - trackInnerX);
        NanoUi.drawSurface(vg, stack, trackInnerX, track.y + scaled(1.0F, k), lineGlowW, track.h - scaled(2.0F, k), Math.max(scaled(1.2F, k), trackRadius - scaled(2.2F, k)), NanoRenderUtils.withAlpha(theme.accentSoftArgb(), 40 + Math.round(glowRatio * 56.0F)), 0);
        float glow = knobSize + scaled(1.2F, k) * focus + scaled(1.8F, k) * glowRatio;
        NanoUi.drawSurface(vg, stack, handleX - glow * 0.5F, track.y + (track.h - glow) * 0.5F, glow, glow, glow * 0.5F, NanoRenderUtils.withAlpha(0xFFF5F9FF, 48 + Math.round((focus * 0.52F + glowRatio * 0.48F) * 74.0F)), 0);
        int knobColor = this.mixArgb(theme.accentArgb(), 0xFFF8FBFF, UiMotion.clamp01(0.40F + focus * 0.52F));
        NanoUi.drawSurface(vg, stack, knobX, knobY, knobSize, knobSize, knobSize * 0.5F, knobColor, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 110));
        this.drawNumberValueInput(vg, stack, valueRect, row, setting, hovered, theme, k, font, "client.settings.number." + setting.getKey());
    }

    private void drawNumberValueInput(long vg, MemoryStack stack, Rect valueRect, Rect row, Setting<?> setting, boolean hoveredRow, NanoTheme theme, float scale, int font, String animKey)
    {
        if (valueRect == null || setting == null)
        {
            return;
        }

        boolean fieldHovered = valueRect.contains(this.mouseX, this.mouseY);
        boolean active = this.activeNumberSetting == setting && this.numberInput.isFocused();
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        UiAnimProfile animProfile = this.resolveInputAnimationProfile(clickGui, this.resolveAnimationProfile(clickGui));

        if (active)
        {
            this.numberInput.draw(vg, stack, font, theme, valueRect.x, valueRect.y, valueRect.w, valueRect.h, scale, scaled(10.2F, scale), this.numberInputBuffer.get(), this.tr("clickgui.input.number.placeholder", "Input..."), hoveredRow || fieldHovered, true, animKey, animProfile);
            return;
        }

        float focus = UiAnimationBus.animateControl(animKey + ".idle.focus", fieldHovered ? 1.0F : 0.0F, animProfile);
        int fill = this.mixArgb(theme.cardAltArgb(), theme.controlArgb(), UiMotion.clamp01(0.38F + focus * 0.32F));
        int border = this.mixArgb(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 92), NanoRenderUtils.withAlpha(theme.accentArgb(), 142), UiMotion.clamp01(focus * 0.72F));
        float radius = Math.min(valueRect.h * 0.5F, this.stableControlRadius(scale));
        NanoUi.drawSurface(vg, stack, valueRect.x, valueRect.y, valueRect.w, valueRect.h, radius, fill, border);
        NanoUi.drawRightText(vg, stack, font, valueRect.x2() - scaled(4.0F, scale), row.y + row.h * 0.5F + scaled(0.45F, scale), scaled(10.2F, scale), theme.textMutedArgb(), this.settingValue(setting));
    }

    private void drawPaletteRow(long vg, MemoryStack stack, Rect row, float scale, int font, NanoTheme theme, ClickGuiModule clickGui)
    {
        if (clickGui == null)
        {
            return;
        }

        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);
        NanoPalette current = clickGui.getPalette();
        NanoPalette[] values = NanoPalette.values();
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        Rect valueRect = this.settingValueRect(row, k);

        for (int i = 0; i < values.length; ++i)
        {
            NanoPalette palette = values[i];
            Rect chip = this.paletteChipRect(valueRect, k, i, values.length);
            boolean selected = palette == current;
            boolean hovered = chip.contains(this.mouseX, this.mouseY);
            String key = "client.settings.palette.hover." + palette.name().toLowerCase(Locale.ROOT);
            float hoverRatio = UiAnimationBus.animateControl(key, hovered ? 1.0F : 0.0F, animProfile);
            float expand = scaled(0.5F, k) + scaled(0.8F, k) * hoverRatio + (selected ? scaled(0.9F, k) : 0.0F);
            float x = chip.x - expand * 0.5F;
            float y = chip.y - expand * 0.5F;
            float w = chip.w + expand;
            float h = chip.h + expand;
            int accent = this.paletteAccentArgb(palette);
            int fill = NanoRenderUtils.withAlpha(accent, selected ? 240 : 214);
            int border = selected ? NanoRenderUtils.withAlpha(0xFFFFFFFF, 188) : NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 90);
            NanoUi.drawSurface(vg, stack, x, y, w, h, h * 0.5F, fill, border);
        }
    }

    private void drawColorPicker(long vg, MemoryStack stack, Layout l, NanoTheme theme, int regular)
    {
        float k = l.scale;
        NanoUi.drawSurface(vg, stack, l.pickerCard.x, l.pickerCard.y, l.pickerCard.w, l.pickerCard.h, this.stableControlRadius(k), theme.cardArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 72));
        NanoUi.drawLeftText(vg, stack, regular, l.pickerCard.x + scaled(8.0F, k), l.pickerCard.y + scaled(11.0F, k), scaled(10.5F, k), theme.textWeakArgb(), this.tr("clickgui.picker.title", "HSV Color Picker"));

        int hueBase = hsvToArgb(this.pickerHue, 1.0F, 1.0F, 255);
        NanoRenderUtils.fillRoundedRect(vg, l.pickerSv.x, l.pickerSv.y, l.pickerSv.w, l.pickerSv.h, scaled(4.0F, k), NanoRenderUtils.argb(stack, hueBase));
        NanoRenderUtils.fillRoundedRectGradient(vg, stack, l.pickerSv.x, l.pickerSv.y, l.pickerSv.w, l.pickerSv.h, scaled(4.0F, k), 0xFFFFFFFF, 0x00FFFFFF, false);
        NanoRenderUtils.fillRoundedRectGradient(vg, stack, l.pickerSv.x, l.pickerSv.y, l.pickerSv.w, l.pickerSv.h, scaled(4.0F, k), 0x00000000, 0xFF000000, true);
        NanoRenderUtils.strokeRoundedRect(vg, l.pickerSv.x, l.pickerSv.y, l.pickerSv.w, l.pickerSv.h, scaled(4.0F, k), 1.0F, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 112)));
        float svX = l.pickerSv.x + this.pickerSat * l.pickerSv.w;
        float svY = l.pickerSv.y + (1.0F - this.pickerVal) * l.pickerSv.h;
        NanoUi.drawSurface(vg, stack, svX - scaled(4.0F, k), svY - scaled(4.0F, k), scaled(8.0F, k), scaled(8.0F, k), scaled(4.0F, k), 0xCCFFFFFF, NanoRenderUtils.withAlpha(0xFF000000, 140));

        this.drawHueBar(vg, stack, l.pickerHue, theme);
        float hueY = l.pickerHue.y + this.pickerHue * l.pickerHue.h;
        NanoUi.drawSurface(vg, stack, l.pickerHue.x - scaled(2.0F, k), hueY - scaled(2.0F, k), l.pickerHue.w + scaled(4.0F, k), scaled(4.0F, k), scaled(2.0F, k), 0xCCFFFFFF, NanoRenderUtils.withAlpha(0xFF000000, 120));

        int rgb = hsvToArgb(this.pickerHue, this.pickerSat, this.pickerVal, 255) & 0x00FFFFFF;
        int alphaFrom = rgb;
        int alphaTo = 0xFF000000 | rgb;
        NanoRenderUtils.fillRoundedRectGradient(vg, stack, l.pickerAlpha.x, l.pickerAlpha.y, l.pickerAlpha.w, l.pickerAlpha.h, scaled(3.0F, k), alphaFrom, alphaTo, false);
        NanoRenderUtils.strokeRoundedRect(vg, l.pickerAlpha.x, l.pickerAlpha.y, l.pickerAlpha.w, l.pickerAlpha.h, scaled(3.0F, k), 1.0F, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 112)));
        float alphaX = l.pickerAlpha.x + this.pickerAlpha * l.pickerAlpha.w;
        NanoUi.drawSurface(vg, stack, alphaX - scaled(2.0F, k), l.pickerAlpha.y - scaled(2.0F, k), scaled(4.0F, k), l.pickerAlpha.h + scaled(4.0F, k), scaled(2.0F, k), 0xCCFFFFFF, NanoRenderUtils.withAlpha(0xFF000000, 120));

        int previewArgb = hsvToArgb(this.pickerHue, this.pickerSat, this.pickerVal, Math.round(this.pickerAlpha * 255.0F));
        NanoUi.drawSurface(vg, stack, l.pickerPreview.x, l.pickerPreview.y, l.pickerPreview.w, l.pickerPreview.h, scaled(4.0F, k), previewArgb, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 98));
        NanoUi.drawLeftText(vg, stack, regular, l.pickerPreview.x2() + scaled(8.0F, k), l.pickerPreview.y + scaled(7.0F, k), scaled(10.0F, k), theme.textMutedArgb(), String.format(Locale.ROOT, "#%08X", Integer.valueOf(previewArgb)));
        NanoUi.drawLeftText(vg, stack, regular, l.pickerPreview.x2() + scaled(8.0F, k), l.pickerPreview.y + scaled(20.0F, k), scaled(10.0F, k), theme.textWeakArgb(), this.tr("clickgui.picker.hsva", "H {0} S {1} V {2} A {3}", this.trimDecimal((double)this.pickerHue, 2), this.trimDecimal((double)this.pickerSat, 2), this.trimDecimal((double)this.pickerVal, 2), this.trimDecimal((double)this.pickerAlpha, 2)));
    }

    private void drawHueBar(long vg, MemoryStack stack, Rect rect, NanoTheme theme)
    {
        int[] colors = new int[] {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
        float segment = rect.h / 6.0F;

        for (int i = 0; i < 6; ++i)
        {
            float y = rect.y + segment * (float)i;
            float h = i == 5 ? rect.h - segment * 5.0F : segment + 0.5F;
            float radius = (i == 0 || i == 5) ? 3.0F : 0.0F;
            NanoRenderUtils.fillRoundedRectGradient(vg, stack, rect.x, y, rect.w, h, radius, colors[i], colors[i + 1], true);
        }

        NanoRenderUtils.strokeRoundedRect(vg, rect.x, rect.y, rect.w, rect.h, 3.0F, 1.0F, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 112)));
    }

    private void drawResizeHandle(long vg, MemoryStack stack, Layout l, NanoTheme theme)
    {
        NanoUi.drawSurface(vg, stack, l.resizeHandle.x, l.resizeHandle.y, l.resizeHandle.w, l.resizeHandle.h, scaled(4.0F, l.scale), NanoRenderUtils.withAlpha(theme.controlActiveArgb(), 190), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 90));
    }

    private void onSettingClick(Setting<?> setting, int button)
    {
        if (setting instanceof ColorSetting)
        {
            this.onColorSettingClick((ColorSetting)setting, button);
            return;
        }

        if (setting instanceof BoolSetting)
        {
            BoolSetting s = (BoolSetting)setting;
            s.setEnabled(!s.isEnabled());
            return;
        }

        if (setting instanceof EnumSetting)
        {
            this.cycleEnum((EnumSetting<?>)setting, button == 1 ? -1 : 1);
            return;
        }

        if (setting instanceof IntSetting)
        {
            IntSetting s = (IntSetting)setting;
            s.set(Integer.valueOf(s.get().intValue() + (button == 1 ? -1 : 1) * s.getStep()));
            return;
        }

        if (setting instanceof FloatSetting)
        {
            FloatSetting s = (FloatSetting)setting;
            s.set(Float.valueOf(s.get().floatValue() + (button == 1 ? -1.0F : 1.0F) * s.getStep()));
            return;
        }

        if (setting instanceof NumberSetting)
        {
            NumberSetting s = (NumberSetting)setting;
            s.set(Double.valueOf(s.get().doubleValue() + (button == 1 ? -1.0D : 1.0D) * s.getStep()));
        }
    }

    private void onColorSettingClick(ColorSetting setting, int button)
    {
        ColorValue current = setting.get();

        if (button == 1)
        {
            setting.set(new ColorValue(current.getR(), current.getG(), current.getB(), current.getA(), !current.isRainbow()));
            return;
        }

        if (this.activeColor != setting)
        {
            this.activeColor = setting;
            this.loadPickerFrom(setting.get());
        }
    }

    private void updatePickerSv(Layout l, int mouseX, int mouseY)
    {
        if (!l.hasPicker || this.activeColor == null)
        {
            return;
        }

        float sat = ((float)mouseX - l.pickerSv.x) / Math.max(1.0F, l.pickerSv.w);
        float val = 1.0F - (((float)mouseY - l.pickerSv.y) / Math.max(1.0F, l.pickerSv.h));
        this.pickerSat = UiMotion.clamp01(sat);
        this.pickerVal = UiMotion.clamp01(val);
        this.commitPickerColor(false);
    }

    private void updatePickerHue(Layout l, int mouseY)
    {
        if (!l.hasPicker || this.activeColor == null)
        {
            return;
        }

        float hue = ((float)mouseY - l.pickerHue.y) / Math.max(1.0F, l.pickerHue.h);
        this.pickerHue = UiMotion.clamp01(hue);
        this.commitPickerColor(false);
    }

    private void updatePickerAlpha(Layout l, int mouseX)
    {
        if (!l.hasPicker || this.activeColor == null)
        {
            return;
        }

        float alpha = ((float)mouseX - l.pickerAlpha.x) / Math.max(1.0F, l.pickerAlpha.w);
        this.pickerAlpha = UiMotion.clamp01(alpha);
        this.commitPickerColor(false);
    }

    private void commitPickerColor(boolean force)
    {
        if (this.activeColor == null)
        {
            return;
        }

        long now = System.nanoTime();

        if (!force && this.lastPickerCommitNanos != 0L && now - this.lastPickerCommitNanos < 3_500_000L)
        {
            return;
        }

        int[] rgb = hsvToRgb(this.pickerHue, this.pickerSat, this.pickerVal);
        int a = NanoRenderUtils.clamp255(Math.round(this.pickerAlpha * 255.0F));
        ColorValue current = this.activeColor.get();

        if (!force && current.getR() == rgb[0] && current.getG() == rgb[1] && current.getB() == rgb[2] && current.getA() == a && !current.isRainbow())
        {
            return;
        }

        this.activeColor.set(new ColorValue(rgb[0], rgb[1], rgb[2], a, false));
        this.lastPickerCommitNanos = now;
    }

    private void updatePickerManual(Layout l, int mouseX, int mouseY)
    {
        if (!l.hasPicker || l.pickerCard == null)
        {
            return;
        }

        float k = l.scale;
        float newX = (float)mouseX - this.pickerDragDx;
        float newY = (float)mouseY - this.pickerDragDy;
        float minX = l.window.x + scaled(6.0F, k);
        float maxX = l.window.x2() - l.pickerCard.w - scaled(6.0F, k);
        float minY = l.window.y + scaled(6.0F, k);
        float maxY = l.window.y2() - l.pickerCard.h - scaled(10.0F, k);
        newX = UiMotion.clamp(newX, minX, maxX);
        newY = UiMotion.clamp(newY, minY, maxY);
        this.pickerOffsetRelX = (newX + l.pickerCard.w * 0.5F - l.window.x) / Math.max(1.0F, l.window.w);
        this.pickerOffsetRelY = (newY + l.pickerCard.h * 0.5F - l.window.y) / Math.max(1.0F, l.window.h);
    }

    private void persistPickerPosition(Layout l)
    {
        if (!l.hasPicker || l.pickerCard == null)
        {
            return;
        }

        ClickGuiModule clickGui = this.resolveClickGuiModule();

        if (clickGui == null)
        {
            return;
        }

        if (!Float.isNaN(this.pickerOffsetRelX) && !Float.isNaN(this.pickerOffsetRelY))
        {
            clickGui.setSettingCenterPicker(this.pickerOffsetRelX, this.pickerOffsetRelY);
        }
    }

    private void persistWindowState()
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();

        if (clickGui == null)
        {
            return;
        }

        float currentScale = Math.min(this.window.getTargetWidth() / BASE_WIDTH, this.window.getTargetHeight() / BASE_HEIGHT);
        clickGui.setSettingCenterScale(currentScale);
        float cx = this.window.getTargetX() + this.window.getTargetWidth() * 0.5F;
        float cy = this.window.getTargetY() + this.window.getTargetHeight() * 0.5F;
        float anchorX = cx / Math.max(1.0F, (float)this.width);
        float anchorY = cy / Math.max(1.0F, (float)this.height);
        clickGui.setSettingCenterAnchor(anchorX, anchorY);
    }

    private void loadPickerFrom(ColorValue color)
    {
        float[] hsv = rgbToHsv(color.getR(), color.getG(), color.getB());
        this.pickerHue = hsv[0];
        this.pickerSat = hsv[1];
        this.pickerVal = hsv[2];
        this.pickerAlpha = UiMotion.clamp01((float)color.getA() / 255.0F);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void cycleEnum(EnumSetting<?> setting, int direction)
    {
        Enum[] values = (Enum[])setting.getEnumType().getEnumConstants();

        if (values == null || values.length == 0)
        {
            return;
        }

        Enum current = (Enum)setting.get();
        int index = 0;

        for (int i = 0; i < values.length; ++i)
        {
            if (values[i] == current)
            {
                index = i;
                break;
            }
        }

        int next = (index + direction) % values.length;

        if (next < 0)
        {
            next += values.length;
        }

        ((EnumSetting)setting).set(values[next]);
    }

    private String settingValue(Setting<?> setting)
    {
        if (setting instanceof BoolSetting)
        {
            return ((BoolSetting)setting).isEnabled() ? this.tr("clickgui.state.enable", "ENABLE") : this.tr("clickgui.state.disable", "DISABLE");
        }

        if (setting instanceof IntSetting)
        {
            IntSetting s = (IntSetting)setting;
            int value = s.get().intValue();

            if (s.getMax() <= 255)
            {
                return value + "/" + s.getMax();
            }

            return Integer.toString(value);
        }

        if (setting instanceof FloatSetting)
        {
            FloatSetting s = (FloatSetting)setting;
            float value = s.get().floatValue();

            if (s.getMin() >= 0.0F && s.getMax() <= 1.0001F)
            {
                return Math.round(value * 100.0F) + "%";
            }

            return String.format(Locale.ROOT, "%.2f", Float.valueOf(value));
        }

        if (setting instanceof NumberSetting)
        {
            NumberSetting s = (NumberSetting)setting;
            double value = s.get().doubleValue();

            if (s.getMin() >= 0.0D && s.getMax() <= 1.0001D)
            {
                return Math.round(value * 100.0D) + "%";
            }

            return s.format();
        }

        if (setting instanceof EnumSetting)
        {
            Object value = setting.get();
            return value == null ? this.tr("clickgui.value.null", "null") : setting.getDisplayOption(this.settingOwnerId(), value);
        }

        if (setting instanceof ColorSetting)
        {
            return String.format(Locale.ROOT, "#%08X", Integer.valueOf(((ColorSetting)setting).get().toArgb()));
        }

        Object value = setting.get();
        return value == null ? this.tr("clickgui.value.null", "null") : String.valueOf(value);
    }

    private boolean isPaletteSetting(Setting<?> setting)
    {
        return setting instanceof EnumSetting && "palette".equals(setting.getKey());
    }

    private boolean handlePaletteRowClick(Setting<?> setting, Rect row, float scale, int mouseX, int mouseY)
    {
        if (!this.isPaletteSetting(setting))
        {
            return false;
        }

        NanoPalette[] values = NanoPalette.values();
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        Rect valueRect = this.settingValueRect(row, k);

        for (int i = 0; i < values.length; ++i)
        {
            Rect chip = this.paletteChipRect(valueRect, k, i, values.length);

            if (chip.contains(mouseX, mouseY))
            {
                if (setting instanceof EnumSetting)
                {
                    EnumSetting<?> paletteSetting = (EnumSetting<?>)setting;

                    if (paletteSetting.getEnumType() == NanoPalette.class)
                    {
                        this.applyPaletteSelection(paletteSetting, values[i]);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private void applyPaletteSelection(EnumSetting<?> setting, NanoPalette value)
    {
        ((EnumSetting<NanoPalette>)setting).set(value);
    }

    private Rect paletteChipRect(Rect valueRect, float scale, int index, int total)
    {
        float chipW = scaled(14.0F, scale);
        float chipH = scaled(10.0F, scale);
        float gap = scaled(3.0F, scale);
        float totalW = chipW * (float)Math.max(1, total) + gap * (float)Math.max(0, total - 1);
        float startX = valueRect.x + (valueRect.w - totalW) * 0.5F;
        float x = startX + (chipW + gap) * (float)Math.max(0, index);
        float y = valueRect.y + (valueRect.h - chipH) * 0.5F;
        return new Rect(x, y, chipW, chipH);
    }

    private int paletteAccentArgb(NanoPalette palette)
    {
        if (palette == null)
        {
            return 0xFF1E6EFF;
        }

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

    private String sectionGroupLabel(Setting<?> setting)
    {
        if (setting == null || setting.getKey() == null)
        {
            return "";
        }

        String key = setting.getKey();

        if (this.section == SettingsSection.ANIMATION)
        {
            if ("ui_anim_type".equals(key) || "ui_input_anim_type".equals(key))
            {
                return this.tr("client.settings.group.controls", "Controls");
            }

            if (key.startsWith("ui_anim_"))
            {
                return this.tr("client.settings.group.global", "Global");
            }

            if (key.startsWith("ui_control_") || key.startsWith("ui_slider_") || key.startsWith("ui_input_"))
            {
                return this.tr("client.settings.group.controls", "Controls");
            }

            if (key.startsWith("ui_page_")
                || key.startsWith("ui_list_")
                || key.startsWith("ui_selection_")
                || key.startsWith("ui_open_")
                || key.startsWith("ui_close_")
                || key.startsWith("ui_switch_")
                || key.startsWith("ui_back_"))
            {
                return this.tr("client.settings.group.page_transition", "Page Transition");
            }

            return this.tr("client.settings.group.animation", "Animation");
        }

        if ("palette".equals(key))
        {
            return this.tr("client.settings.group.palette", "Palette");
        }

        if ("accent_override_enabled".equals(key) || "accent_override".equals(key))
        {
            return this.tr("client.settings.group.accent", "Accent");
        }

        if ("corner_radius".equals(key) || "panel_alpha".equals(key) || "backdrop".equals(key) || "backdrop_alpha".equals(key))
        {
            return this.tr("client.settings.group.surface", "Surface");
        }

        return this.tr("client.settings.group.theme", "Theme");
    }

    private boolean hasModifiedSettings(List<Setting<?>> settings)
    {
        if (settings == null || settings.isEmpty())
        {
            return false;
        }

        for (int i = 0; i < settings.size(); ++i)
        {
            if (this.isSettingModified(settings.get(i)))
            {
                return true;
            }
        }

        return false;
    }

    private void resetSectionSettings(ClickGuiModule module)
    {
        List<Setting<?>> settings = this.visibleSettings(module);

        for (int i = 0; i < settings.size(); ++i)
        {
            Setting<?> setting = settings.get(i);
            setting.reset();
        }

        if (this.activeColor != null)
        {
            this.loadPickerFrom(this.activeColor.get());
        }
    }

    private void resetThemeSettings(ClickGuiModule module)
    {
        this.resetSettingsByFilter(module, false);
    }

    private void resetAnimationSettings(ClickGuiModule module)
    {
        this.resetSettingsByFilter(module, true);
    }

    private void resetSettingsByFilter(ClickGuiModule module, boolean animation)
    {
        if (module == null)
        {
            return;
        }

        List<Setting<?>> all = module.getSettings();

        for (int i = 0; i < all.size(); ++i)
        {
            Setting<?> setting = all.get(i);

            if (!setting.isVisible())
            {
                continue;
            }

            if (this.isAnimationSetting(setting) != animation)
            {
                continue;
            }

            setting.reset();
        }

        if (this.activeColor != null)
        {
            this.loadPickerFrom(this.activeColor.get());
        }
    }

    private boolean isSettingModified(Setting<?> setting)
    {
        if (setting == null)
        {
            return false;
        }

        Object current = setting.get();
        Object defaults = setting.getDefaultValue();

        if (setting instanceof ColorSetting)
        {
            if (!(current instanceof ColorValue) || !(defaults instanceof ColorValue))
            {
                return current != defaults;
            }

            ColorValue c = (ColorValue)current;
            ColorValue d = (ColorValue)defaults;
            return c.toArgb() != d.toArgb() || c.isRainbow() != d.isRainbow();
        }

        if (current instanceof Float && defaults instanceof Float)
        {
            return Math.abs(((Float)current).floatValue() - ((Float)defaults).floatValue()) > 0.0001F;
        }

        if (current instanceof Double && defaults instanceof Double)
        {
            return Math.abs(((Double)current).doubleValue() - ((Double)defaults).doubleValue()) > 0.0000001D;
        }

        if (current instanceof Enum && defaults instanceof Enum)
        {
            return current != defaults;
        }

        if (current == null)
        {
            return defaults != null;
        }

        return !current.equals(defaults);
    }

    private List<Setting<?>> visibleSettings(ClickGuiModule module)
    {
        if (module == null)
        {
            return Collections.emptyList();
        }

        List<Setting<?>> out = new ArrayList<Setting<?>>();
        List<Setting<?>> all = module.getSettings();

        for (int i = 0; i < all.size(); ++i)
        {
            Setting<?> setting = all.get(i);

            if (!setting.isVisible())
            {
                continue;
            }

            String key = setting.getKey();

            if ("last_category_ordinal".equals(key) || "last_module_index".equals(key))
            {
                continue;
            }

            boolean animationSetting = this.isAnimationSetting(setting);

            if (this.section == SettingsSection.ANIMATION && !animationSetting)
            {
                continue;
            }

            if (this.section == SettingsSection.THEME && animationSetting)
            {
                continue;
            }

            out.add(setting);
        }

        return out;
    }

    private boolean isAnimationSetting(Setting<?> setting)
    {
        if (setting == null)
        {
            return false;
        }

        String key = setting.getKey();

        if (key == null)
        {
            return false;
        }

        return key.startsWith("ui_anim_")
            || key.startsWith("ui_control_")
            || key.startsWith("ui_slider_")
            || key.startsWith("ui_page_")
            || key.startsWith("ui_list_")
            || key.startsWith("ui_selection_")
            || key.startsWith("ui_input_")
            || key.startsWith("ui_open_")
            || key.startsWith("ui_close_")
            || key.startsWith("ui_switch_")
            || key.startsWith("ui_back_");
    }

    private Rect settingRowRect(Layout l, int visibleIndex, float scrollOffset)
    {
        float y = l.settingsRows.y + l.rowSetting * (float)Math.max(0, visibleIndex) - scrollOffset;
        return new Rect(l.settingsRows.x, y, l.settingsRows.w, Math.max(6.0F, l.rowSetting - 1.0F));
    }

    private Rect settingSliderTrackRect(Rect row, float scale)
    {
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        Rect resetRect = this.settingResetRect(row, k);
        float rightGap = scaled(8.0F, k);
        float trackX = row.x + scaled(118.0F, k);
        float trackRight = resetRect.x - rightGap;
        float trackW = Math.max(scaled(78.0F, k), trackRight - trackX);
        float trackH = Math.max(scaled(2.6F, k), row.h - scaled(19.0F, k));
        float trackY = row.y + (row.h - trackH) * 0.5F;
        return new Rect(trackX, trackY, trackW, trackH);
    }

    private Rect sliderHitRect(Rect track, float scale)
    {
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        float padX = scaled(6.0F, k);
        float padY = scaled(6.0F, k);
        return new Rect(track.x - padX, track.y - padY, track.w + padX * 2.0F, track.h + padY * 2.0F);
    }

    private Rect settingValueRect(Rect row, float scale)
    {
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        float rightPad = scaled(8.0F, k);
        float w = scaled(VALUE_COL_WIDTH, k);
        float h = Math.max(scaled(14.0F, k), row.h - scaled(6.0F, k));
        float x = row.x2() - rightPad - w;
        float y = row.y + (row.h - h) * 0.5F;
        return new Rect(x, y, w, h);
    }

    private Rect settingResetRect(Rect row, float scale)
    {
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        float rightPad = scaled(8.0F, k);
        float w = scaled(RESET_COL_WIDTH, k);
        float h = Math.max(scaled(14.0F, k), row.h - scaled(6.0F, k));
        float x = row.x2() - rightPad - w - scaled(VALUE_COL_WIDTH, k) - scaled(6.0F, k);
        float y = row.y + (row.h - h) * 0.5F;
        return new Rect(x, y, w, h);
    }

    private Rect activeNumberInputRect(Layout l, ClickGuiModule clickGui, Setting<?> setting)
    {
        if (l == null || clickGui == null || setting == null || !this.isSliderSetting(setting))
        {
            return null;
        }

        List<Setting<?>> settings = this.visibleSettings(clickGui);
        int index = settings.indexOf(setting);

        if (index < 0)
        {
            return null;
        }

        int scrollBase = (int)Math.floor((double)this.settingScrollVisual);
        float scrollOffset = (this.settingScrollVisual - (float)scrollBase) * l.rowSetting;
        int visibleIndex = index - scrollBase;

        if (visibleIndex < 0)
        {
            return null;
        }

        Rect row = this.settingRowRect(l, visibleIndex, scrollOffset);
        return this.settingValueRect(row, l.scale);
    }

    private void activateNumberInput(Rect inputRect, float scale, int mouseX, int mouseY, Setting<?> setting)
    {
        if (setting == null || inputRect == null || !this.isSliderSetting(setting))
        {
            return;
        }

        this.commitActiveNumberInput();
        this.activeNumberSetting = setting;
        this.numberInputBuffer.set(this.settingRawInputValue(setting));

        if (this.lastNanoVg != 0L)
        {
            NanoFontBook.ensureLoaded(this.lastNanoVg);
        }

        this.numberInput.onMouseDown(mouseX, mouseY, inputRect.x, inputRect.y, inputRect.w, inputRect.h, this.lastNanoVg, NanoFontBook.uiRegular(), scaled(10.2F, UiMotion.clamp(scale, 0.35F, 1.85F)), this.numberInputBuffer.get());
    }

    private boolean isSliderSetting(Setting<?> setting)
    {
        return setting instanceof IntSetting || setting instanceof FloatSetting || setting instanceof NumberSetting;
    }

    private float settingSliderRatio(Setting<?> setting)
    {
        if (setting instanceof IntSetting)
        {
            IntSetting s = (IntSetting)setting;
            return UiMotion.clamp01(((float)s.get().intValue() - (float)s.getMin()) / Math.max(1.0F, (float)(s.getMax() - s.getMin())));
        }

        if (setting instanceof FloatSetting)
        {
            FloatSetting s = (FloatSetting)setting;
            return UiMotion.clamp01((s.get().floatValue() - s.getMin()) / Math.max(0.0001F, s.getMax() - s.getMin()));
        }

        if (setting instanceof NumberSetting)
        {
            NumberSetting s = (NumberSetting)setting;
            return UiMotion.clamp01((float)((s.get().doubleValue() - s.getMin()) / Math.max(0.0001D, s.getMax() - s.getMin())));
        }

        return 0.0F;
    }

    private void applySettingSliderFromMouse(Setting<?> setting, Rect track, int mouseX)
    {
        if (setting == null || track == null)
        {
            return;
        }

        float ratio = NanoSliderController.mouseRatio((float)mouseX, track.x, track.w);
        this.lastSliderDragNanos = System.nanoTime();

        if (setting instanceof IntSetting)
        {
            IntSetting s = (IntSetting)setting;
            float v = (float)s.getMin() + ratio * (float)(s.getMax() - s.getMin());
            int step = Math.max(1, s.getStep());
            int snapped = s.getMin() + Math.round((v - (float)s.getMin()) / (float)step) * step;
            snapped = Math.max(s.getMin(), Math.min(s.getMax(), snapped));
            if (snapped != s.get().intValue())
            {
                s.set(Integer.valueOf(snapped));
            }

            return;
        }

        if (setting instanceof FloatSetting)
        {
            FloatSetting s = (FloatSetting)setting;
            float v = s.getMin() + ratio * (s.getMax() - s.getMin());
            float step = Math.max(0.0001F, s.getStep());
            float snapped = s.getMin() + Math.round((v - s.getMin()) / step) * step;
            snapped = Math.max(s.getMin(), Math.min(s.getMax(), snapped));
            if (Math.abs(snapped - s.get().floatValue()) > 0.0001F)
            {
                s.set(Float.valueOf(snapped));
            }

            return;
        }

        if (setting instanceof NumberSetting)
        {
            NumberSetting s = (NumberSetting)setting;
            double v = s.getMin() + (double)ratio * (s.getMax() - s.getMin());
            double step = Math.max(0.0001D, s.getStep());
            double snapped = s.getMin() + Math.round((v - s.getMin()) / step) * step;
            snapped = Math.max(s.getMin(), Math.min(s.getMax(), snapped));
            if (Math.abs(snapped - s.get().doubleValue()) > 0.0000001D)
            {
                s.set(Double.valueOf(snapped));
            }
        }
    }

    private void updateDraggedSettingSlider(Layout l, int mouseX)
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();

        if (this.draggingSettingSlider == null || clickGui == null)
        {
            this.draggingSettingSlider = null;
            this.clearSliderTrackLock();
            return;
        }

        List<Setting<?>> settings = this.visibleSettings(clickGui);
        int index = settings.indexOf(this.draggingSettingSlider);

        if (index < 0)
        {
            this.draggingSettingSlider = null;
            this.clearSliderTrackLock();
            return;
        }

        int scrollBase = (int)Math.floor((double)this.settingScrollVisual);
        float scrollOffset = (this.settingScrollVisual - (float)scrollBase) * l.rowSetting;
        int visibleIndex = index - scrollBase;

        if (visibleIndex < 0)
        {
            return;
        }

        Rect row = this.settingRowRect(l, visibleIndex, scrollOffset);
        Rect track = this.settingSliderTrackRect(row, l.scale);
        track = this.resolveSliderDragTrack(track);
        this.applySettingSliderFromMouse(this.draggingSettingSlider, track, mouseX);
    }

    private void updateActiveDrags(Layout l)
    {
        if (l == null)
        {
            return;
        }

        if (this.draggingSettingSlider != null)
        {
            this.updateDraggedSettingSlider(l, this.mouseX);
        }

        if (this.activeNumberSetting != null && this.numberInput.isFocused())
        {
            ClickGuiModule clickGui = this.resolveClickGuiModule();
            Rect inputRect = this.activeNumberInputRect(l, clickGui, this.activeNumberSetting);

            if (inputRect != null)
            {
                this.numberInput.onMouseDrag(this.mouseX, inputRect.x, inputRect.w, this.lastNanoVg, NanoFontBook.uiRegular(), scaled(10.2F, l.scale), this.numberInputBuffer.get());
            }
        }

        if (this.draggingSv)
        {
            this.updatePickerSv(l, this.mouseX, this.mouseY);
        }

        if (this.draggingHue)
        {
            this.updatePickerHue(l, this.mouseY);
        }

        if (this.draggingAlpha)
        {
            this.updatePickerAlpha(l, this.mouseX);
        }
    }

    private void validateActiveColor(ClickGuiModule clickGui)
    {
        if (this.activeColor == null)
        {
            return;
        }

        if (clickGui == null)
        {
            this.activeColor = null;
            return;
        }

        List<Setting<?>> visible = this.visibleSettings(clickGui);

        if (!visible.contains(this.activeColor) || !this.activeColor.isVisible())
        {
            this.activeColor = null;
        }
    }

    private void validateActiveNumberSetting(ClickGuiModule clickGui)
    {
        if (this.activeNumberSetting == null)
        {
            return;
        }

        if (!this.numberInput.isFocused() || clickGui == null || !this.isSliderSetting(this.activeNumberSetting))
        {
            this.activeNumberSetting = null;
            this.numberInput.blur();
            return;
        }

        List<Setting<?>> visible = this.visibleSettings(clickGui);

        if (!visible.contains(this.activeNumberSetting) || !this.activeNumberSetting.isVisible())
        {
            this.activeNumberSetting = null;
            this.numberInput.blur();
        }
    }

    private void commitActiveNumberInput()
    {
        if (this.activeNumberSetting == null)
        {
            return;
        }

        this.applyNumericInput(this.activeNumberSetting, this.numberInputBuffer.get());
        this.activeNumberSetting = null;
    }

    private boolean applyNumericInput(Setting<?> setting, String text)
    {
        if (setting == null || text == null)
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

        if (this.isUnitRangeSetting(setting) && (percent || Math.abs(parsed) > 1.0001D))
        {
            parsed *= 0.01D;
        }

        if (setting instanceof IntSetting)
        {
            ((IntSetting)setting).set(Integer.valueOf((int)Math.round(parsed)));
            return true;
        }

        if (setting instanceof FloatSetting)
        {
            ((FloatSetting)setting).set(Float.valueOf((float)parsed));
            return true;
        }

        if (setting instanceof NumberSetting)
        {
            ((NumberSetting)setting).set(Double.valueOf(parsed));
            return true;
        }

        return false;
    }

    private String settingRawInputValue(Setting<?> setting)
    {
        if (setting instanceof IntSetting)
        {
            return Integer.toString(((IntSetting)setting).get().intValue());
        }

        if (setting instanceof FloatSetting)
        {
            return this.trimDecimal(((FloatSetting)setting).get().floatValue(), 4);
        }

        if (setting instanceof NumberSetting)
        {
            return this.trimDecimal(((NumberSetting)setting).get().doubleValue(), 6);
        }

        return this.settingValue(setting);
    }

    private boolean isUnitRangeSetting(Setting<?> setting)
    {
        if (setting instanceof FloatSetting)
        {
            FloatSetting s = (FloatSetting)setting;
            return s.getMin() >= 0.0F && s.getMax() <= 1.0001F;
        }

        if (setting instanceof NumberSetting)
        {
            NumberSetting s = (NumberSetting)setting;
            return s.getMin() >= 0.0D && s.getMax() <= 1.0001D;
        }

        return false;
    }

    private String trimDecimal(double value, int precision)
    {
        int safePrecision = Math.max(0, Math.min(10, precision));
        String pattern = "%." + safePrecision + "f";
        String out = String.format(Locale.ROOT, pattern, Double.valueOf(value));
        int dot = out.indexOf('.');

        if (dot < 0)
        {
            return out;
        }

        int end = out.length();

        while (end > dot + 1 && out.charAt(end - 1) == '0')
        {
            --end;
        }

        if (end > dot && out.charAt(end - 1) == '.')
        {
            --end;
        }

        return out.substring(0, end);
    }

    private void clampScroll(Layout l, ClickGuiModule clickGui)
    {
        List<Setting<?>> settings = this.visibleSettings(clickGui);
        int visibleRows = Math.max(1, (int)(l.settingsRows.h / l.rowSetting));
        int max = Math.max(0, settings.size() - visibleRows);
        this.settingScroll = Math.max(0, Math.min(max, this.settingScroll));
        this.settingScrollVisual = UiMotion.clamp(this.settingScrollVisual, 0.0F, (float)max);
    }

    private void updateScrollAnimation(Layout l, ClickGuiModule clickGui)
    {
        if (l == null)
        {
            return;
        }

        List<Setting<?>> settings = this.visibleSettings(clickGui);
        int visibleRows = Math.max(1, (int)(l.settingsRows.h / l.rowSetting));
        int max = Math.max(0, settings.size() - visibleRows);
        this.settingScroll = Math.max(0, Math.min(max, this.settingScroll));
        float target = (float)this.settingScroll;
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);

        if (!animProfile.isEnabled())
        {
            this.settingScrollVisual = target;
            return;
        }

        float speed = UiAnimProfiles.scrollSpeed(animProfile);
        this.settingScrollVisual = this.draggingSettingSlider == null ? UiAnimationBus.animateWithSpeed("client.settings.scroll", target, animProfile, speed) : target;
    }

    private void requestTransition(TransitionMode mode, GuiScreen target)
    {
        if (this.mc == null || this.transitioningOut)
        {
            return;
        }

        this.transitionMode = mode == null ? TransitionMode.SWITCH : mode;
        this.transitionTarget = target;
        this.transitioningOut = true;
        this.transitionExecuted = false;
    }

    private void updateTransition(ClickGuiModule clickGui)
    {
        long now = System.nanoTime();
        float dt = this.transitionLastNanos == 0L ? (1.0F / 60.0F) : (float)((double)(now - this.transitionLastNanos) * 1.0E-9D);
        this.transitionLastNanos = now;
        float speed = clickGui == null ? 0.56F : clickGui.getGlobalAnimationSpeed();
        float smooth = this.resolveAnimationProfile(clickGui).smooth();
        UiAnimation.Type type = this.resolveTransitionType(clickGui);

        if (this.transitioningOut)
        {
            float boost;

            switch (this.transitionMode)
            {
                case CLOSE:
                    boost = 1.90F;
                    break;
                case BACK:
                    boost = 1.18F;
                    break;
                case SWITCH:
                default:
                    boost = 1.48F;
                    break;
            }

            speed = UiMotion.clamp(speed * boost + 0.08F, 0.05F, 1.0F);
            smooth = UiMotion.clamp(smooth * (this.transitionMode == TransitionMode.BACK ? 0.95F : 0.90F), 0.0F, 1.0F);
            dt *= this.transitionMode == TransitionMode.BACK ? 1.12F : 1.24F;
        }

        float response = UiAnimation.responseFromSpeed(speed, smooth, type, false);
        float target = this.transitioningOut ? 0.0F : 1.0F;
        this.transitionProgress = UiAnimation.step(this.transitionProgress, target, response, dt, type, smooth);

        if (this.transitioningOut && !this.transitionExecuted && this.transitionProgress <= 0.02F && this.mc != null)
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

        if (!this.transitioningOut)
        {
            return clickGui.getGuiOpenAnimationType();
        }

        switch (this.transitionMode)
        {
            case CLOSE:
                return clickGui.getGuiCloseAnimationType();
            case BACK:
                return clickGui.getGuiBackAnimationType();
            case SWITCH:
            default:
                return clickGui.getGuiSwitchAnimationType();
        }
    }

    private float transitionVisual()
    {
        float p = UiMotion.clamp01(this.transitionProgress);
        float eased = (float)Math.pow((double)p, 0.82D);
        return UiMotion.clamp(eased, 0.0F, 1.0F);
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
                    shiftX = scaled(24.0F, k) * inv;
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
            shiftY = scaled(14.0F, k) * inv;
        }

        float newW = baseRect.w * scale;
        float newH = baseRect.h * scale;
        float cx = baseRect.x + baseRect.w * 0.5F + shiftX;
        float cy = baseRect.y + baseRect.h * 0.5F + shiftY;
        return new Rect(cx - newW * 0.5F, cy - newH * 0.5F, newW, newH);
    }

    private void syncWindowTarget()
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        float savedScale = clickGui == null ? 1.0F : clickGui.getSettingCenterScale();
        float targetWidth = UiMotion.clamp(BASE_WIDTH * savedScale, MIN_WIDTH, (float)this.width - SCREEN_MARGIN * 2.0F);
        float targetHeight = UiMotion.clamp(BASE_HEIGHT * savedScale, MIN_HEIGHT, (float)this.height - SCREEN_MARGIN * 2.0F);
        float anchorX = clickGui == null ? 0.5F : clickGui.getSettingCenterAnchorX();
        float anchorY = clickGui == null ? 0.5F : clickGui.getSettingCenterAnchorY();
        float targetX = anchorX * (float)this.width - targetWidth * 0.5F;
        float targetY = anchorY * (float)this.height - targetHeight * 0.5F;

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

        if (!this.window.isInteracting())
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

    private Rect resolveSliderDragTrack(Rect track)
    {
        if (track == null || !this.draggingSliderTrackLocked)
        {
            return track;
        }

        return new Rect(this.draggingSliderTrackX, track.y, this.draggingSliderTrackW, track.h);
    }

    private void clearSliderTrackLock()
    {
        this.draggingSliderTrackLocked = false;
        this.draggingSliderTrackX = 0.0F;
        this.draggingSliderTrackW = 0.0F;
    }

    private ClickGuiModule resolveClickGuiModule()
    {
        if (this.modules == null)
        {
            return null;
        }

        Module module = this.modules.getById("click_gui");
        return module instanceof ClickGuiModule ? (ClickGuiModule)module : null;
    }

    private void refreshLiveMousePosition()
    {
        this.mouseX = Math.round(this.liveMouseX());
        this.mouseY = Math.round(this.liveMouseY());
    }

    private float liveMouseX()
    {
        if (this.mc == null)
        {
            return (float)this.mouseX;
        }

        int displayWidth = Math.max(1, this.mc.displayWidth);
        float raw = (float)Mouse.getX() * (float)this.width / (float)displayWidth;
        return UiMotion.clamp(raw, 0.0F, Math.max(0.0F, (float)this.width - 1.0F));
    }

    private float liveMouseY()
    {
        if (this.mc == null)
        {
            return (float)this.mouseY;
        }

        int displayHeight = Math.max(1, this.mc.displayHeight);
        float raw = (float)this.height - (float)Mouse.getY() * (float)this.height / (float)displayHeight - 1.0F;
        return UiMotion.clamp(raw, 0.0F, Math.max(0.0F, (float)this.height - 1.0F));
    }

    private String sectionLabel(SettingsSection section)
    {
        if (section == SettingsSection.ANIMATION)
        {
            return this.tr("client.settings.section.animation", "Animation");
        }

        return this.tr("client.settings.section.theme", "Theme");
    }

    private String settingDisplayName(Setting<?> setting)
    {
        if (setting == null)
        {
            return "";
        }

        return setting.getDisplayName(this.settingOwnerId());
    }

    private String settingOwnerId()
    {
        return "click_gui";
    }

    private String tr(String key, String fallback, Object... args)
    {
        client.i18n.I18nManager i18n = ClientBootstrap.instance().getI18n();
        return i18n == null ? fallback : i18n.translateOrDefault(key, fallback, args);
    }

    private UiAnimProfile resolveAnimationProfile(ClickGuiModule clickGui)
    {
        return UiAnimProfiles.settingsProfile(clickGui);
    }

    private UiAnimProfile resolveInputAnimationProfile(ClickGuiModule clickGui, UiAnimProfile profile)
    {
        return UiAnimProfiles.inputProfile(clickGui, profile);
    }

    private UiAnimProfile resolveWindowAnimationProfile(ClickGuiModule clickGui)
    {
        boolean interacting = this.window.isInteracting() || this.draggingSettingSlider != null || this.draggingSv || this.draggingHue || this.draggingAlpha;
        return UiAnimProfiles.settingsWindowProfile(clickGui, interacting);
    }

    private NanoTheme resolveTheme(ClickGuiModule clickGui)
    {
        if (clickGui == null)
        {
            return NanoThemes.create(NanoPalette.COBALT, 220, 96, 10.0F, null);
        }

        Integer accent = null;

        if (clickGui.isAccentOverrideEnabled() && clickGui.getAccentOverride() != null)
        {
            accent = Integer.valueOf(clickGui.getAccentOverride().toArgb());
        }

        int backdrop = clickGui.isBackdropEnabled() ? clickGui.getBackdropAlpha() : 0;
        float corner = UiMotion.clamp(clickGui.getCornerRadius(), 6.0F, 26.0F);
        return NanoThemes.create(clickGui.getPalette(), clickGui.getPanelAlpha(), backdrop, corner, accent);
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
        float headerHeight = scaled(HEADER_HEIGHT, k);
        float outerPad = scaled(OUTER_PAD, k);

        Rect header = new Rect(windowRect.x + scaled(1.0F, k), windowRect.y + scaled(1.0F, k), windowRect.w - scaled(2.0F, k), headerHeight - scaled(2.0F, k));
        Rect backButton = new Rect(header.x2() - scaled(78.0F, k), header.y + scaled(8.0F, k), scaled(68.0F, k), scaled(20.0F, k));
        Rect headerDrag = new Rect(header.x + scaled(2.0F, k), header.y + scaled(2.0F, k), backButton.x - header.x - scaled(8.0F, k), header.h - scaled(4.0F, k));
        Rect body = new Rect(windowRect.x + outerPad, windowRect.y + headerHeight + scaled(10.0F, k), windowRect.w - outerPad * 2.0F, windowRect.h - headerHeight - outerPad * 2.0F);
        Rect settingsCard = new Rect(body.x, body.y, body.w, body.h);
        float infoH = UiMotion.clamp(scaled(INFO_CARD_HEIGHT, k), scaled(52.0F, k), scaled(82.0F, k));
        Rect infoCard = new Rect(settingsCard.x + scaled(12.0F, k), settingsCard.y + scaled(10.0F, k), settingsCard.w - scaled(24.0F, k), infoH);
        float debugH = UiMotion.clamp(scaled(DEBUG_CARD_HEIGHT, k), scaled(28.0F, k), scaled(42.0F, k));
        Rect debugCard = new Rect(settingsCard.x + scaled(12.0F, k), infoCard.y2() + scaled(8.0F, k), settingsCard.w - scaled(24.0F, k), debugH);
        float debugBtnH = scaled(20.0F, k);
        float debugBtnGap = scaled(4.0F, k);
        float debugBtnY = debugCard.y + (debugCard.h - debugBtnH) * 0.5F;
        float debugBtnW = Math.max(scaled(62.0F, k), (debugCard.w - debugBtnGap * 2.0F) / 3.0F);
        Rect debugResetTheme = new Rect(debugCard.x, debugBtnY, debugBtnW, debugBtnH);
        Rect debugResetAnimation = new Rect(debugResetTheme.x2() + debugBtnGap, debugBtnY, debugBtnW, debugBtnH);
        Rect debugFlushAnimation = new Rect(debugResetAnimation.x2() + debugBtnGap, debugBtnY, debugBtnW, debugBtnH);
        float tabY = debugCard.y2() + scaled(10.0F, k);
        float tabGap = scaled(6.0F, k);
        float topX = settingsCard.x + scaled(12.0F, k);
        float topW = settingsCard.w - scaled(24.0F, k);
        float resetW = scaled(86.0F, k);
        float tabW = (topW - resetW - tabGap * 2.0F) * 0.5F;
        Rect themeTab = new Rect(topX, tabY, tabW, scaled(20.0F, k));
        Rect animationTab = new Rect(themeTab.x2() + tabGap, tabY, tabW, themeTab.h);
        Rect resetSectionButton = new Rect(animationTab.x2() + tabGap, tabY, resetW, themeTab.h);
        float rowSetting = ROW_SETTING * k;

        float rowsTop = themeTab.y2() + scaled(10.0F, k);
        float rowsBottom = settingsCard.y2() - scaled(10.0F, k);
        Rect settingsRows = new Rect(settingsCard.x + scaled(12.0F, k), rowsTop, settingsCard.w - scaled(24.0F, k), Math.max(scaled(30.0F, k), rowsBottom - rowsTop));
        Rect resizeHandle = new Rect(windowRect.x2() - scaled(14.0F, k), windowRect.y2() - scaled(14.0F, k), scaled(10.0F, k), scaled(10.0F, k));

        boolean hasPicker = this.activeColor != null;
        Rect pickerCard = null;
        Rect pickerSv = null;
        Rect pickerHue = null;
        Rect pickerAlpha = null;
        Rect pickerPreview = null;

        Rect pickerAnchor = null;

        if (hasPicker)
        {
            ClickGuiModule clickGui = this.resolveClickGuiModule();

            if (clickGui != null)
            {
                List<Setting<?>> settings = this.visibleSettings(clickGui);
                int idx = settings.indexOf(this.activeColor);

                if (idx >= 0)
                {
                    int scrollBase = (int)Math.floor((double)this.settingScrollVisual);
                    float scrollOffset = (this.settingScrollVisual - (float)scrollBase) * rowSetting;
                    int visibleIndex = idx - scrollBase;

                    if (visibleIndex >= 0)
                    {
                        Rect row = new Rect(settingsRows.x, settingsRows.y + rowSetting * (float)visibleIndex - scrollOffset, settingsRows.w, Math.max(6.0F, rowSetting - 1.0F));
                        Rect valueRect = this.settingValueRect(row, k);
                        pickerAnchor = new Rect(valueRect.x, valueRect.y, valueRect.w, valueRect.h);
                    }
                }
            }

            float pickerW = scaled(142.0F, k);
            float pickerH = scaled(118.0F, k);
            float anchorX = pickerAnchor != null ? pickerAnchor.x2() : windowRect.x2() - scaled(12.0F, k);
            float anchorY = pickerAnchor != null ? pickerAnchor.y : header.y2() + scaled(10.0F, k);
            float pickerX = Math.min(windowRect.x2() - scaled(8.0F, k) - pickerW, Math.max(windowRect.x + scaled(8.0F, k), anchorX - pickerW + scaled(12.0F, k)));
            float desiredY = anchorY - pickerH - scaled(6.0F, k);
            float minY = header.y2() + scaled(6.0F, k);
            float pickerY = desiredY >= minY ? desiredY : anchorY + (pickerAnchor != null ? pickerAnchor.h : 0.0F) + scaled(8.0F, k);
            pickerY = Math.min(Math.max(minY, pickerY), windowRect.y2() - pickerH - scaled(10.0F, k));

            if (!Float.isNaN(this.pickerOffsetRelX) || !Float.isNaN(this.pickerOffsetRelY))
            {
                float relX = Float.isNaN(this.pickerOffsetRelX) ? 0.5F : this.pickerOffsetRelX;
                float relY = Float.isNaN(this.pickerOffsetRelY) ? 0.5F : this.pickerOffsetRelY;
                pickerX = UiMotion.clamp(windowRect.x + relX * windowRect.w - pickerW * 0.5F, windowRect.x + scaled(6.0F, k), windowRect.x2() - pickerW - scaled(6.0F, k));
                pickerY = UiMotion.clamp(windowRect.y + relY * windowRect.h - pickerH * 0.5F, minY, windowRect.y2() - pickerH - scaled(10.0F, k));
            }
            else if (clickGui != null)
            {
                float storedX = clickGui.getSettingCenterPickerX();
                float storedY = clickGui.getSettingCenterPickerY();

                if (storedX > 0.0F || storedY > 0.0F)
                {
                    pickerX = UiMotion.clamp(windowRect.x + storedX * windowRect.w - pickerW * 0.5F, windowRect.x + scaled(6.0F, k), windowRect.x2() - pickerW - scaled(6.0F, k));
                    pickerY = UiMotion.clamp(windowRect.y + storedY * windowRect.h - pickerH * 0.5F, minY, windowRect.y2() - pickerH - scaled(10.0F, k));
                    this.pickerOffsetRelX = storedX;
                    this.pickerOffsetRelY = storedY;
                }
            }
            pickerCard = new Rect(pickerX, pickerY, pickerW, pickerH);
            float svWidth = UiMotion.clamp(pickerW * 0.46F, scaled(74.0F, k), scaled(108.0F, k));
            float svHeight = Math.max(scaled(50.0F, k), pickerH - scaled(36.0F, k));
            pickerSv = new Rect(pickerCard.x + scaled(8.0F, k), pickerCard.y + scaled(12.0F, k), svWidth, svHeight);
            pickerHue = new Rect(pickerSv.x2() + scaled(6.0F, k), pickerSv.y, scaled(7.0F, k), pickerSv.h);
            pickerAlpha = new Rect(pickerSv.x, pickerSv.y2() + scaled(6.0F, k), pickerSv.w + scaled(13.0F, k), scaled(7.0F, k));
            pickerPreview = new Rect(pickerCard.x2() - scaled(44.0F, k), pickerCard.y + scaled(8.0F, k), scaled(38.0F, k), scaled(20.0F, k));
        }

        return new Layout(windowRect, header, headerDrag, backButton, settingsCard, infoCard, debugCard, debugResetTheme, debugResetAnimation, debugFlushAnimation, themeTab, animationTab, resetSectionButton, settingsRows, resizeHandle, pickerCard, pickerSv, pickerHue, pickerAlpha, pickerPreview, hasPicker, k);
    }

    private float stableWindowRadius(float scale)
    {
        float cornerScale = this.cornerRadiusScale();
        return scaled(RADIUS_WINDOW * cornerScale, UiMotion.clamp(scale, 0.35F, 1.85F));
    }

    private float stablePanelRadius(float scale)
    {
        float cornerScale = this.cornerRadiusScale();
        return scaled(RADIUS_PANEL * cornerScale, UiMotion.clamp(scale, 0.35F, 1.85F));
    }

    private float stableRowRadius(float scale)
    {
        float cornerScale = this.cornerRadiusScale();
        return scaled(RADIUS_ROW * cornerScale, UiMotion.clamp(scale, 0.35F, 1.85F));
    }

    private float stableControlRadius(float scale)
    {
        float cornerScale = this.cornerRadiusScale();
        return scaled(RADIUS_CONTROL * cornerScale, UiMotion.clamp(scale, 0.35F, 1.85F));
    }

    private float cornerRadiusScale()
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();

        if (clickGui == null)
        {
            return 1.0F;
        }

        float corner = UiMotion.clamp(clickGui.getCornerRadius(), 6.0F, 26.0F);
        return UiMotion.clamp(corner / 12.0F, 0.5F, 2.2F);
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

    private static int hsvToArgb(float hue, float saturation, float value, int alpha)
    {
        int[] rgb = hsvToRgb(hue, saturation, value);
        int a = NanoRenderUtils.clamp255(alpha);
        return (a & 255) << 24 | (rgb[0] & 255) << 16 | (rgb[1] & 255) << 8 | rgb[2] & 255;
    }

    private static int[] hsvToRgb(float hue, float saturation, float value)
    {
        float h = UiMotion.clamp01(hue) * 6.0F;
        float s = UiMotion.clamp01(saturation);
        float v = UiMotion.clamp01(value);
        int i = (int)Math.floor(h);
        float f = h - (float)i;
        float p = v * (1.0F - s);
        float q = v * (1.0F - s * f);
        float t = v * (1.0F - s * (1.0F - f));
        float r;
        float g;
        float b;

        switch (i % 6)
        {
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            default:
                r = v;
                g = p;
                b = q;
                break;
        }

        return new int[] {NanoRenderUtils.clamp255(Math.round(r * 255.0F)), NanoRenderUtils.clamp255(Math.round(g * 255.0F)), NanoRenderUtils.clamp255(Math.round(b * 255.0F))};
    }

    private static float[] rgbToHsv(int r, int g, int b)
    {
        float rf = (float)NanoRenderUtils.clamp255(r) / 255.0F;
        float gf = (float)NanoRenderUtils.clamp255(g) / 255.0F;
        float bf = (float)NanoRenderUtils.clamp255(b) / 255.0F;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;
        float h;

        if (delta <= 0.00001F)
        {
            h = 0.0F;
        }
        else if (max == rf)
        {
            h = ((gf - bf) / delta) % 6.0F;
        }
        else if (max == gf)
        {
            h = (bf - rf) / delta + 2.0F;
        }
        else
        {
            h = (rf - gf) / delta + 4.0F;
        }

        h /= 6.0F;

        if (h < 0.0F)
        {
            h += 1.0F;
        }

        float s = max <= 0.00001F ? 0.0F : delta / max;
        float v = max;
        return new float[] {UiMotion.clamp01(h), UiMotion.clamp01(s), UiMotion.clamp01(v)};
    }

    private static final class Layout
    {
        private final Rect window;
        private final Rect header;
        private final Rect headerDrag;
        private final Rect backButton;
        private final Rect settingsCard;
        private final Rect infoCard;
        private final Rect debugCard;
        private final Rect debugResetTheme;
        private final Rect debugResetAnimation;
        private final Rect debugFlushAnimation;
        private final Rect themeTab;
        private final Rect animationTab;
        private final Rect resetSectionButton;
        private final Rect settingsRows;
        private final Rect resizeHandle;
        private final Rect pickerCard;
        private final Rect pickerSv;
        private final Rect pickerHue;
        private final Rect pickerAlpha;
        private final Rect pickerPreview;
        private final boolean hasPicker;
        private final float scale;
        private final float rowSetting;

        private Layout(Rect window, Rect header, Rect headerDrag, Rect backButton, Rect settingsCard, Rect infoCard, Rect debugCard, Rect debugResetTheme, Rect debugResetAnimation, Rect debugFlushAnimation, Rect themeTab, Rect animationTab, Rect resetSectionButton, Rect settingsRows, Rect resizeHandle, Rect pickerCard, Rect pickerSv, Rect pickerHue, Rect pickerAlpha, Rect pickerPreview, boolean hasPicker, float scale)
        {
            this.window = window;
            this.header = header;
            this.headerDrag = headerDrag;
            this.backButton = backButton;
            this.settingsCard = settingsCard;
            this.infoCard = infoCard;
            this.debugCard = debugCard;
            this.debugResetTheme = debugResetTheme;
            this.debugResetAnimation = debugResetAnimation;
            this.debugFlushAnimation = debugFlushAnimation;
            this.themeTab = themeTab;
            this.animationTab = animationTab;
            this.resetSectionButton = resetSectionButton;
            this.settingsRows = settingsRows;
            this.resizeHandle = resizeHandle;
            this.pickerCard = pickerCard;
            this.pickerSv = pickerSv;
            this.pickerHue = pickerHue;
            this.pickerAlpha = pickerAlpha;
            this.pickerPreview = pickerPreview;
            this.hasPicker = hasPicker;
            this.scale = UiMotion.clamp(scale, 0.35F, 1.85F);
            this.rowSetting = ROW_SETTING * this.scale;
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
