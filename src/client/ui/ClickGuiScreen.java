package client.ui;

import client.core.ClientBootstrap;
import client.hud.HudEditorScreen;
import client.module.Category;
import client.module.Module;
import client.module.ModuleManager;
import client.module.impl.client.ClickGuiModule;
import client.module.impl.client.UiScaleEditModule;
import client.render.RenderContext2D;
import client.setting.BoolSetting;
import client.setting.ColorSetting;
import client.setting.ColorValue;
import client.setting.EnumSetting;
import client.setting.FloatSetting;
import client.setting.IntSetting;
import client.setting.MultiSelectSetting;
import client.setting.NumberSetting;
import client.setting.Setting;
import client.setting.StringSetting;
import client.ui.component.NanoColorPicker;
import client.ui.template.NanoSliderController;
import client.ui.template.NanoTextInput;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiAnimProfiles;
import client.ui.template.UiAnimation;
import client.ui.template.UiAnimationBus;
import client.ui.template.NanoScreenKit;
import client.ui.template.UiControlAnimations;
import client.ui.template.UiLayoutProfile;
import client.ui.template.UiMotion;
import client.ui.template.UiSelectionBox;
import client.ui.template.UiStateToggle;
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
import client.runtime.lwjgl.GlfwMouse;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.MemoryStack.stackPush;

public final class ClickGuiScreen extends GuiScreen implements NanoRenderableScreen
{
    private enum ContentPage
    {
        MODULES,
        SETTINGS
    }

    private enum SettingsSection
    {
        THEME,
        ANIMATION
    }

    private enum TransitionMode
    {
        NONE,
        CLOSE,
        SWITCH,
        BACK
    }

    private static final float BASE_WINDOW_WIDTH = 1120.0F;
    private static final float BASE_WINDOW_HEIGHT = 700.0F;
    private static final float MIN_WINDOW_WIDTH = 260.0F;
    private static final float MIN_WINDOW_HEIGHT = 170.0F;
    private static final UiLayoutProfile LAYOUT = new UiLayoutProfile();
    private static final float SCREEN_MARGIN = LAYOUT.screenMargin();
    private static final float HEADER_HEIGHT = LAYOUT.headerHeight();
    private static final float OUTER_PAD = LAYOUT.outerPad();
    private static final float GAP_MAJOR = LAYOUT.gapMajor();
    private static final float ROW_CATEGORY = LAYOUT.rowCategory();
    private static final float ROW_MODULE = LAYOUT.rowModule();
    private static final float ROW_SETTING = LAYOUT.rowSetting();
    private static final float BTN_HEIGHT = LAYOUT.btnHeight();
    private static final float RADIUS_WINDOW = LAYOUT.radiusWindow();
    private static final float RADIUS_PANEL = LAYOUT.radiusPanel();
    private static final float RADIUS_ROW = LAYOUT.radiusRow();
    private static final float RADIUS_CONTROL = LAYOUT.radiusControl();
    private static final float VALUE_COL_WIDTH = LAYOUT.valueColWidth();
    private static final float RESET_COL_WIDTH = LAYOUT.resetColWidth();
    private static final float LIST_STAGGER_STEP = 0.075F;
    private static final float SETTING_SCROLL_STEP = 0.78F;
    private static final int KEY_ESCAPE = 1;
    private static final int KEY_RETURN = 28;
    private static final int KEY_NUMPADENTER = 156;

    private final ModuleManager modules;
    private final UiWindowState window = new UiWindowState(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT);
    private ContentPage contentPage = ContentPage.MODULES;
    private SettingsSection settingsSection = SettingsSection.THEME;
    private Category restoreCategory;
    private String restoreModuleId;

    private Category selectedCategory;
    private Module selectedModule;
    private boolean waitingBind;
    private int categoryScroll;
    private int moduleScroll;
    private int settingScroll;
    private int moduleSettingScroll;
    private int compatSettingScroll;
    private float settingScrollTarget;
    private float categoryScrollVisual;
    private float moduleScrollVisual;
    private float settingScrollVisual;
    private float settingsPageProgress;
    private int mouseX;
    private int mouseY;
    private long lastNanoAt;
    private long lastNanoVg;

    private ColorSetting activeColor;
    private StringSetting activeTextSetting;
    private Setting<?> activeNumberSetting;
    private Setting<?> expandedChoiceSetting;
    private final StringSetting textInputBuffer = new StringSetting("__clickgui_text_input_buffer", "Text Input", "Inline text input buffer", "", 120);
    private final StringSetting numberInputBuffer = new StringSetting("__clickgui_number_input_buffer", "Number Input", "Inline numeric input buffer", "", 40);
    private final NanoTextInput textInput = new NanoTextInput();
    private final NanoColorPicker colorPicker = new NanoColorPicker("clickgui.picker");
    private Setting<?> draggingSettingSlider;
    private boolean draggingSliderTrackLocked;
    private float draggingSliderTrackX;
    private float draggingSliderTrackW;
    private long lastSliderDragNanos;
    private float categoryTransition = 1.0F;
    private int categoryTransitionDir = 1;
    private long categoryTransitionNanos;
    private TransitionMode transitionMode = TransitionMode.NONE;
    private GuiScreen transitionTarget;
    private boolean transitioningOut;
    private boolean transitionExecuted;
    private float transitionProgress = 1.0F;
    private long transitionLastNanos;

    public ClickGuiScreen(ModuleManager moduleManager)
    {
        this.modules = moduleManager;
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }

    public void initGui()
    {
        this.ensureSelection();
        this.validateActiveColor();
        this.transitionMode = TransitionMode.NONE;
        this.transitionTarget = null;
        this.transitioningOut = false;
        this.transitionExecuted = false;
        this.transitionProgress = 0.0F;
        this.transitionLastNanos = System.nanoTime();
        this.categoryTransition = 1.0F;
        this.categoryTransitionDir = 1;
        this.categoryTransitionNanos = System.nanoTime();
        this.draggingSettingSlider = null;
        this.expandedChoiceSetting = null;
        this.clearSliderTrackLock();
        this.clearInlineEditors(false);
        this.contentPage = ContentPage.MODULES;
        this.settingsSection = SettingsSection.THEME;
        this.restoreCategory = null;
        this.restoreModuleId = null;
        this.lastSliderDragNanos = 0L;
        this.colorPicker.stopDragging();
        this.settingsPageProgress = 0.0F;
        this.restorePersistedScrollState();
        this.categoryScrollVisual = (float)this.categoryScroll;
        this.moduleScrollVisual = (float)this.moduleScroll;
        this.settingScrollVisual = this.settingScrollTarget;
        this.syncLocaleFromI18n();
    }

    public void onGuiClosed()
    {
        this.persistScrollState();
        this.waitingBind = false;
        this.colorPicker.stopDragging();
        this.draggingSettingSlider = null;
        this.expandedChoiceSetting = null;
        this.clearSliderTrackLock();
        this.clearInlineEditors(true);
        this.window.endInteraction();
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.activeTextSetting != null && this.textInput.isFocused())
        {
            if (keyCode == KEY_RETURN || keyCode == KEY_NUMPADENTER)
            {
                this.commitActiveTextInput();
                return;
            }

            if (keyCode == KEY_ESCAPE)
            {
                this.cancelActiveTextInput();
                return;
            }

            if (this.textInput.handleKeyTyped(typedChar, keyCode, this.textInputBuffer))
            {
                if (!this.textInput.isFocused())
                {
                    this.cancelActiveTextInput();
                }

                return;
            }
        }

        if (this.activeNumberSetting != null && this.textInput.isFocused())
        {
            if (this.textInput.handleKeyTyped(typedChar, keyCode, this.numberInputBuffer))
            {
                if (!this.textInput.isFocused())
                {
                    this.commitActiveNumberInput();
                }

                return;
            }
        }

        if (this.waitingBind && this.selectedModule != null)
        {
            if (keyCode == KEY_ESCAPE)
            {
                this.selectedModule.getBind().clear();
            }
            else if (keyCode > 0)
            {
                this.selectedModule.getBind().setKeyCode(keyCode);
            }

            this.waitingBind = false;
            return;
        }

        if (keyCode == KEY_ESCAPE)
        {
            if (this.isSettingsPage())
            {
                this.exitSettingsCompatPage();
                return;
            }

            this.requestTransition(TransitionMode.CLOSE, null);
            return;
        }

        if (!this.isSettingsPage() && this.selectedModule != null && (keyCode == 57 || keyCode == KEY_RETURN))
        {
            this.selectedModule.toggle();
            return;
        }

        if (!this.isSettingsPage() && this.selectedModule != null && keyCode == 19)
        {
            this.waitingBind = true;
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        this.ensureSelection();
        this.validateActiveColor();
        this.validateActiveTextSetting();
        this.validateActiveNumberSetting();
        this.validateExpandedChoiceSetting();
        Layout l = this.layout();
        this.clampScroll(l);

        if (mouseButton == 0 && (this.activeTextSetting != null || this.activeNumberSetting != null))
        {
            Rect activeRect = this.activeInlineInputRect(l);

            if (activeRect == null || !activeRect.contains(mouseX, mouseY))
            {
                this.clearInlineEditors(true);
            }
        }

        if (mouseButton == 0 && this.handleExpandedChoiceClick(l, mouseX, mouseY))
        {
            return;
        }

        if (mouseButton == 0)
        {
            if (l.topHudEdit.contains(mouseX, mouseY))
            {
                this.openHudEdit();
                return;
            }

            if (l.topUiScale.contains(mouseX, mouseY))
            {
                this.openUiScaleEdit();
                return;
            }

            if (l.topClientSettings.contains(mouseX, mouseY))
            {
                this.toggleSettingsCompatPage();
                return;
            }

            if (this.isSettingsPage())
            {
                float tabShiftX = (1.0F - UiMotion.clamp01(this.settingsPageProgress)) * scaled(12.0F, l.scale);
                Rect themeTab = new Rect(l.settingsThemeTab.x + tabShiftX, l.settingsThemeTab.y, l.settingsThemeTab.w, l.settingsThemeTab.h);
                Rect animationTab = new Rect(l.settingsAnimationTab.x + tabShiftX, l.settingsAnimationTab.y, l.settingsAnimationTab.w, l.settingsAnimationTab.h);

                if (themeTab.contains(mouseX, mouseY))
                {
                    this.switchSettingsSection(SettingsSection.THEME);
                    return;
                }

                if (animationTab.contains(mouseX, mouseY))
                {
                    this.switchSettingsSection(SettingsSection.ANIMATION);
                    return;
                }
            }

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

            if (l.hasPicker)
            {
                this.syncPickerSubBounds(l);

                if (this.colorPicker.mouseClicked(mouseX, mouseY, 0))
                {
                    this.commitPickerColor(false);
                    return;
                }
            }
        }

        if (!this.isSettingsPage() && l.sidebarRows.contains(mouseX, mouseY))
        {
            int scrollBase = (int)Math.floor((double)this.categoryScrollVisual);
            float scrollOffset = (this.categoryScrollVisual - (float)scrollBase) * l.rowCategory;
            int index = scrollBase + (int)(((float)mouseY - l.sidebarRows.y + scrollOffset) / l.rowCategory);
            List<CategoryEntry> entries = this.categoryEntries();

            if (index >= 0 && index < entries.size())
            {
                Category previous = this.selectedCategory;
                Category next = entries.get(index).category;

                if (next != previous)
                {
                    int prevIndex = this.categoryIndex(entries, previous);
                    int nextIndex = this.categoryIndex(entries, next);
                    this.categoryTransitionDir = nextIndex >= prevIndex ? 1 : -1;
                    this.categoryTransition = 0.0F;
                    this.categoryTransitionNanos = System.nanoTime();
                }

                this.selectedCategory = next;
                this.moduleScroll = 0;
                this.settingScroll = 0;
                this.settingScrollTarget = 0.0F;
                this.moduleSettingScroll = 0;
                this.moduleScrollVisual = 0.0F;
                this.settingScrollVisual = 0.0F;
                this.draggingSettingSlider = null;
                this.expandedChoiceSetting = null;
                this.clearInlineEditors(true);
                this.ensureSelection();
                this.validateActiveColor();
                this.persistScrollState();
                this.rememberSelection();
            }

            return;
        }

        if (!this.isSettingsPage() && l.moduleRows.contains(mouseX, mouseY))
        {
            int scrollBase = (int)Math.floor((double)this.moduleScrollVisual);
            float scrollOffset = (this.moduleScrollVisual - (float)scrollBase) * l.rowModule;
            int index = scrollBase + (int)(((float)mouseY - l.moduleRows.y + scrollOffset) / l.rowModule);
            List<Module> list = this.currentModules();

            if (index >= 0 && index < list.size())
            {
                this.selectedModule = list.get(index);
                this.settingScroll = 0;
                this.settingScrollTarget = 0.0F;
                this.moduleSettingScroll = 0;
                this.settingScrollVisual = 0.0F;
                this.draggingSettingSlider = null;
                this.expandedChoiceSetting = null;
                this.clearInlineEditors(true);
                this.validateActiveColor();
                this.persistScrollState();
                this.rememberSelection();

                if (mouseButton == 1)
                {
                    this.selectedModule.toggle();
                }
                else if (mouseButton == 2)
                {
                    this.waitingBind = true;
                }
            }

            return;
        }

        if (this.selectedModule != null)
        {
            if (!this.isSettingsPage() && l.btnToggle.contains(mouseX, mouseY))
            {
                this.selectedModule.toggle();
                return;
            }

            if (!this.isSettingsPage() && l.btnBind.contains(mouseX, mouseY))
            {
                if (mouseButton == 1)
                {
                    this.selectedModule.getBind().clear();
                    this.waitingBind = false;
                }
                else
                {
                    this.waitingBind = true;
                }

                return;
            }

            if (l.settingsRows.contains(mouseX, mouseY))
            {
                int scrollBase = (int)Math.floor((double)this.settingScrollVisual);
                float scrollOffset = (this.settingScrollVisual - (float)scrollBase) * l.rowSetting;
                int index = scrollBase + (int)(((float)mouseY - l.settingsRows.y + scrollOffset) / l.rowSetting);
                List<Setting<?>> settings = this.visibleSettings(this.selectedModule);

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
                    }

                    if (mouseButton == 0 && this.isChoiceSetting(setting) && visibleIndex >= 0)
                    {
                        Rect row = this.settingRowRect(l, visibleIndex, scrollOffset);
                        Rect valueRect = this.settingValueRect(row, l.scale);

                        if (valueRect.contains(mouseX, mouseY))
                        {
                            this.toggleChoiceSettingExpanded(setting);
                            return;
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
                                this.applySettingSliderFromMouse(setting, track, mouseX);
                                return;
                            }
                        }
                    }

                    if (mouseButton == 0 && setting instanceof StringSetting && visibleIndex >= 0)
                    {
                        Rect row = this.settingRowRect(l, visibleIndex, scrollOffset);
                        Rect inputRect = this.settingTextInputRect(row, l.scale);

                        if (inputRect.contains(mouseX, mouseY))
                        {
                            this.activateTextInput(inputRect, l.scale, mouseX, mouseY, (StringSetting)setting);
                            return;
                        }
                    }

                    this.onSettingClick(setting, mouseButton);
                }

                return;
            }
        }

        if (this.activeColor != null && l.pickerCard != null && !l.pickerCard.contains(mouseX, mouseY))
        {
            this.activeColor = null;
            this.colorPicker.stopDragging();
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

        if (this.colorPicker.isDragging())
        {
            this.colorPicker.mouseDragged(mouseX, mouseY);
        }

        if (this.draggingSettingSlider != null)
        {
            this.updateDraggedSettingSlider(l, mouseX);
        }
    }

    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        if (this.colorPicker.isDragging())
        {
            this.commitPickerColor(true);
            this.colorPicker.mouseReleased(mouseX, mouseY, 0);
        }

        this.window.endInteraction();
        this.draggingSettingSlider = null;
        this.clearSliderTrackLock();
        this.textInput.onMouseUp();
        super.mouseReleased(mouseX, mouseY, state);
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int wheel = GlfwMouse.getEventDWheel();

        if (wheel == 0)
        {
            return;
        }

        int x = GlfwMouse.getEventX() * this.width / this.mc.displayWidth;
        int y = this.height - GlfwMouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int delta = wheel > 0 ? -1 : 1;
        Layout l = this.layout();

        if (!this.isSettingsPage() && l.sidebarRows.contains(x, y))
        {
            this.categoryScroll += delta;
        }
        else if (!this.isSettingsPage() && l.moduleRows.contains(x, y))
        {
            this.moduleScroll += delta;
        }
        else if (l.settingsRows.contains(x, y))
        {
            this.settingScrollTarget += (float)delta * this.resolveSettingScrollStep(wheel);
            this.settingScroll = Math.round(this.settingScrollTarget);
        }

        this.clampScroll(l);
        this.captureActiveSettingScroll();
        this.persistScrollState();
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
        this.ensureSelection();
        this.validateActiveColor();
        this.validateActiveTextSetting();
        this.validateActiveNumberSetting();
        this.validateExpandedChoiceSetting();
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        this.updateTransition(clickGui);
        this.updateCategoryTransition(clickGui);
        this.updateSettingsPageProgress(clickGui);

        if (this.transitioningOut && this.transitionExecuted)
        {
            return;
        }

        UiScaleEditModule uiScale = this.resolveUiScaleModule();
        this.syncWindowTarget(uiScale);

        if (this.window.isInteracting())
        {
            this.window.updateInteraction(this.liveMouseX(), this.liveMouseY(), (float)this.width, (float)this.height, SCREEN_MARGIN);
            this.syncScaleAndAnchorFromWindow(uiScale);
        }

        UiAnimProfile windowAnim = this.resolveWindowAnimationProfile(clickGui, uiScale);
        this.window.tick(windowAnim);
        Layout l = this.layout();
        this.clampScroll(l);
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
            this.drawTopBar(vg, stack, l, theme, regular, bold);
            if (!this.isSettingsPage() || this.settingsPageProgress < 0.995F)
            {
                this.drawSidebar(vg, stack, l, theme, regular, bold);
                this.drawModules(vg, stack, l, theme, regular, bold);
            }
            this.drawSettings(vg, stack, l, theme, regular, bold);
            this.drawResizeHandle(vg, stack, l, theme);
            context.getNanoVG().resetScissor();
        }
    }

    private void drawTopBar(long vg, MemoryStack stack, Layout l, NanoTheme theme, int regular, int bold)
    {
        NanoUi.drawLeftText(vg, stack, bold, l.header.x + scaled(12.0F, l.scale), l.header.y + l.header.h * 0.5F, scaled(16.0F, l.scale), theme.textArgb(), this.tr("clickgui.title.client", "Client"));
        NanoUi.drawLeftText(vg, stack, regular, l.header.x + scaled(140.0F, l.scale), l.header.y + l.header.h * 0.5F + scaled(0.5F, l.scale), scaled(11.0F, l.scale), theme.textWeakArgb(), this.tr("ui.powered_by", "Powered by DWGX"));
        this.drawTopButton(vg, stack, l.topClientSettings, this.tr("clickgui.top.setting", "Setting"), regular, theme, this.isSettingsPage());
        this.drawTopButton(vg, stack, l.topUiScale, this.tr("clickgui.top.uiscale", "UIScale"), regular, theme, false);
        this.drawTopButton(vg, stack, l.topHudEdit, this.tr("clickgui.top.hudedit", "HudEdit"), regular, theme, false);
    }

    private void drawTopButton(long vg, MemoryStack stack, Rect button, String label, int font, NanoTheme theme, boolean active)
    {
        boolean hovered = button.contains(this.mouseX, this.mouseY);
        int fill = active ? theme.controlActiveArgb() : (hovered ? theme.controlHoverArgb() : theme.controlArgb());
        NanoUi.drawSurface(vg, stack, button.x, button.y, button.w, button.h, this.stableControlRadius(button.h / BTN_HEIGHT), fill, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 80));
        NanoUi.drawCenterText(vg, stack, font, button.x + button.w * 0.5F, button.y + button.h * 0.5F, scaled(11.0F, UiMotion.clamp(button.h / 20.0F, 0.35F, 1.85F)), theme.textArgb(), label);
    }

    private void drawSidebar(long vg, MemoryStack stack, Layout l, NanoTheme theme, int regular, int bold)
    {
        if (l.sidebar.w <= 1.0F || l.sidebar.h <= 1.0F || l.sidebarRows.w <= 1.0F || l.sidebarRows.h <= 1.0F)
        {
            return;
        }

        NanoUi.drawSurface(vg, stack, l.sidebar.x, l.sidebar.y, l.sidebar.w, l.sidebar.h, this.stablePanelRadius(l.scale), theme.sidebarArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 56));
        NanoUi.drawLeftText(vg, stack, bold, l.sidebar.x + scaled(12.0F, l.scale), l.sidebar.y + scaled(14.0F, l.scale), scaled(14.0F, l.scale), theme.textArgb(), this.tr("clickgui.sidebar.categories", "Categories"));

        List<CategoryEntry> entries = this.categoryEntries();
        int visible = Math.max(0, (int)Math.ceil((double)(l.sidebarRows.h / l.rowCategory)) + 2);
        int scrollBase = (int)Math.floor((double)this.categoryScrollVisual);
        float scrollOffset = (this.categoryScrollVisual - (float)scrollBase) * l.rowCategory;
        NanoUi.beginClip(vg, l.sidebarRows.x, l.sidebarRows.y, l.sidebarRows.w, l.sidebarRows.h);
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);
        float pageReveal = UiAnimationBus.animateWithSpeed("clickgui.page.modules.sidebar", this.isSettingsPage() ? 0.0F : 1.0F, animProfile, this.resolvePageAnimationSpeed(clickGui, animProfile));
        float listSpeed = this.resolveListAnimationSpeed(clickGui, animProfile);
        float rowRadius = this.stableRowRadius(l.scale);
        Rect selectedRow = null;

        for (int i = 0; i < visible; ++i)
        {
            int idx = scrollBase + i;

            if (idx >= entries.size())
            {
                break;
            }

            CategoryEntry entry = entries.get(idx);
            float presence = this.resolveListPresence("clickgui.category.row." + entry.category.name().toLowerCase(Locale.ROOT), i, pageReveal, animProfile, listSpeed);
            Rect baseRow = new Rect(l.sidebarRows.x, l.sidebarRows.y + l.rowCategory * (float)i - scrollOffset, l.sidebarRows.w, Math.max(6.0F, l.rowCategory - 1.0F));
            float slideX = (1.0F - presence) * scaled(6.0F, l.scale);
            float slideY = (1.0F - presence) * scaled(4.0F, l.scale);
            Rect row = new Rect(baseRow.x - slideX, baseRow.y + slideY, baseRow.w, baseRow.h);
            boolean hovered = row.contains(this.mouseX, this.mouseY);
            boolean selected = this.selectedCategory == entry.category;
            float hoverRatio = UiAnimationBus.animateControl("clickgui.category.hover." + entry.category.name(), hovered ? 1.0F : 0.0F, animProfile);
            float selectRatio = UiAnimationBus.animateControl("clickgui.category.select." + entry.category.name(), selected ? 1.0F : 0.0F, animProfile);
            float rowAlpha = UiMotion.clamp01(presence);

            if (rowAlpha <= 0.001F && !selected && hoverRatio <= 0.001F)
            {
                continue;
            }

            int base = i % 2 == 0 ? theme.rowArgb() : theme.cardAltArgb();
            NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, NanoRenderUtils.mulAlpha(base, rowAlpha), 0);

            if (hoverRatio > 0.001F)
            {
                NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, NanoRenderUtils.mulAlpha(theme.rowHoverArgb(), UiMotion.clamp01(hoverRatio * 0.8F) * rowAlpha), 0);
            }

            if (selectRatio > 0.001F)
            {
                NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, NanoRenderUtils.mulAlpha(theme.rowSelectedArgb(), selectRatio * rowAlpha), 0);
            }

            float dotRatio = UiMotion.clamp01(Math.max(selectRatio, hoverRatio * 0.55F));
            float dotSize = scaled(4.0F, l.scale) + scaled(1.5F, l.scale) * dotRatio;
            float dotX = row.x + scaled(8.0F, l.scale);
            float dotY = row.y + row.h * 0.5F - dotSize * 0.5F;
            int dotColor = selected ? theme.accentArgb() : NanoRenderUtils.mulAlpha(theme.textWeakArgb(), UiMotion.clamp(0.45F + hoverRatio * 0.25F, 0.0F, 1.0F));
            NanoUi.drawSurface(vg, stack, dotX, dotY, dotSize, dotSize, dotSize * 0.5F, NanoRenderUtils.mulAlpha(dotColor, rowAlpha), 0);

            int nameColor = selected ? theme.textArgb() : NanoScreenKit.mixArgb(theme.textWeakArgb(), theme.textArgb(), UiMotion.clamp01(hoverRatio * 0.7F));
            NanoUi.drawLeftText(vg, stack, regular, row.x + scaled(18.0F, l.scale), row.y + row.h * 0.5F, scaled(12.5F, l.scale), NanoRenderUtils.mulAlpha(nameColor, rowAlpha), this.categoryDisplayName(entry.category));
            NanoUi.drawRightText(vg, stack, regular, row.x2() - scaled(8.0F, l.scale), row.y + row.h * 0.5F, scaled(10.5F, l.scale), NanoRenderUtils.mulAlpha(theme.textWeakArgb(), rowAlpha), Integer.toString(entry.count));

            if (selected)
            {
                selectedRow = row;
            }
        }

        this.drawAnimatedSelectionBox(vg, stack, "clickgui.category.selection", selectedRow, theme, l.scale, animProfile, clickGui, pageReveal);
        NanoUi.endClip(vg);
    }

    private void drawModules(long vg, MemoryStack stack, Layout l, NanoTheme theme, int regular, int bold)
    {
        if (l.modulesCard.w <= 1.0F || l.modulesCard.h <= 1.0F || l.moduleRows.w <= 1.0F || l.moduleRows.h <= 1.0F)
        {
            return;
        }

        NanoUi.drawSurface(vg, stack, l.modulesCard.x, l.modulesCard.y, l.modulesCard.w, l.modulesCard.h, this.stablePanelRadius(l.scale), theme.cardArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 58));
        List<Module> list = this.currentModules();
        NanoUi.drawLeftText(vg, stack, bold, l.modulesCard.x + scaled(12.0F, l.scale), l.modulesCard.y + scaled(14.0F, l.scale), scaled(15.0F, l.scale), theme.textArgb(), this.tr("clickgui.modules.title", "Modules ({0})", Integer.valueOf(list.size())));
        String categoryLabel = this.categoryDisplayName(this.selectedCategory);
        NanoUi.drawRightText(vg, stack, regular, l.modulesCard.x2() - scaled(12.0F, l.scale), l.modulesCard.y + scaled(14.0F, l.scale), scaled(10.5F, l.scale), theme.textWeakArgb(), categoryLabel);

        ClickGuiModule clickGui = this.resolveClickGuiModule();
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);
        float pageReveal = UiAnimationBus.animateWithSpeed("clickgui.page.modules.list", this.isSettingsPage() ? 0.0F : 1.0F, animProfile, this.resolvePageAnimationSpeed(clickGui, animProfile));
        float listSpeed = this.resolveListAnimationSpeed(clickGui, animProfile);
        float listSlide = (1.0F - this.categoryTransition) * scaled(12.0F, l.scale) * (float)this.categoryTransitionDir;
        float listAlpha = UiMotion.clamp((0.35F + this.categoryTransition * 0.65F) * pageReveal, 0.0F, 1.0F);
        float rowRadius = this.stableRowRadius(l.scale);
        int visible = Math.max(0, (int)Math.ceil((double)(l.moduleRows.h / l.rowModule)) + 2);
        int scrollBase = (int)Math.floor((double)this.moduleScrollVisual);
        float scrollOffset = (this.moduleScrollVisual - (float)scrollBase) * l.rowModule;
        NanoUi.beginClip(vg, l.moduleRows.x, l.moduleRows.y, l.moduleRows.w, l.moduleRows.h);
        Rect selectedRow = null;

        for (int i = 0; i < visible; ++i)
        {
            int idx = scrollBase + i;

            if (idx >= list.size())
            {
                break;
            }

            Module module = list.get(idx);
            float presence = this.resolveListPresence("clickgui.module.row." + module.getId(), i, listAlpha, animProfile, listSpeed);
            float rowSlideX = (1.0F - presence) * scaled(8.0F, l.scale);
            float rowSlideY = (1.0F - presence) * scaled(4.5F, l.scale);
            Rect row = new Rect(l.moduleRows.x + listSlide - rowSlideX, l.moduleRows.y + l.rowModule * (float)i - scrollOffset + rowSlideY, l.moduleRows.w, Math.max(10.0F, l.rowModule - 3.0F));
            boolean hovered = row.contains(this.mouseX, this.mouseY);
            boolean selected = this.selectedModule == module;
            float hoverRatio = UiAnimationBus.animateControl("clickgui.module.hover." + module.getId(), hovered ? 1.0F : 0.0F, animProfile);
            float selectRatio = UiAnimationBus.animateControl("clickgui.module.select." + module.getId(), selected ? 1.0F : 0.0F, animProfile);
            float rowAlpha = UiMotion.clamp01(presence * listAlpha);

            if (rowAlpha <= 0.001F && !selected && hoverRatio <= 0.001F)
            {
                continue;
            }

            int base = i % 2 == 0 ? theme.rowArgb() : theme.cardAltArgb();
            NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, NanoRenderUtils.mulAlpha(base, rowAlpha), 0);

            if (hoverRatio > 0.001F)
            {
                NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, NanoRenderUtils.mulAlpha(theme.rowHoverArgb(), UiMotion.clamp01(hoverRatio * 0.78F) * rowAlpha), 0);
            }

            if (selectRatio > 0.001F)
            {
                NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, NanoRenderUtils.mulAlpha(theme.rowSelectedArgb(), selectRatio * rowAlpha), 0);
            }

            float accentSize = scaled(3.0F, l.scale) + scaled(1.6F, l.scale) * UiMotion.clamp01(Math.max(selectRatio, hoverRatio * 0.4F));
            float accentX = row.x + scaled(8.0F, l.scale);
            float accentY = row.y + row.h * 0.5F - accentSize * 0.5F;
            int accentColor = module.isEnabled() ? theme.accentArgb() : NanoRenderUtils.mulAlpha(theme.textWeakArgb(), UiMotion.clamp(0.38F + hoverRatio * 0.2F, 0.0F, 1.0F));
            NanoUi.drawSurface(vg, stack, accentX, accentY, accentSize, accentSize, accentSize * 0.5F, NanoRenderUtils.mulAlpha(accentColor, rowAlpha), 0);
            int moduleNameColor = selected ? theme.textArgb() : NanoScreenKit.mixArgb(theme.textWeakArgb(), theme.textArgb(), UiMotion.clamp01(hoverRatio * 0.6F));
            String moduleDisplayName = module.getDisplayName();
            NanoUi.drawLeftText(vg, stack, bold, row.x + scaled(16.0F, l.scale), row.y + scaled(12.0F, l.scale), scaled(14.5F, l.scale), NanoRenderUtils.mulAlpha(moduleNameColor, rowAlpha), this.waitingBind && selected ? this.tr("clickgui.module.bind_waiting", "[bind] {0}", moduleDisplayName) : moduleDisplayName);
            NanoUi.drawLeftText(vg, stack, regular, row.x + scaled(16.0F, l.scale), row.y + scaled(24.0F, l.scale), scaled(10.0F, l.scale), NanoRenderUtils.mulAlpha(theme.textWeakArgb(), rowAlpha), this.categoryDisplayName(module.getCategory()));
            this.drawModuleStatus(vg, stack, row, module, theme, l.scale, rowAlpha, regular);

            if (selected)
            {
                selectedRow = row;
            }
        }

        this.drawAnimatedSelectionBox(vg, stack, "clickgui.module.selection", selectedRow, theme, l.scale, animProfile, clickGui, pageReveal * this.categoryTransition);
        NanoUi.endClip(vg);
    }
    private void drawSettings(long vg, MemoryStack stack, Layout l, NanoTheme theme, int regular, int bold)
    {
        NanoUi.drawSurface(vg, stack, l.settingsCard.x, l.settingsCard.y, l.settingsCard.w, l.settingsCard.h, this.stablePanelRadius(l.scale), theme.cardAltArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 50));
        boolean settingsPage = this.isSettingsPage();
        float settingsBlend = UiMotion.clamp01(this.settingsPageProgress);
        float slideDistance = scaled(12.0F, l.scale);

        if (this.selectedModule == null)
        {
            NanoUi.drawCenterText(vg, stack, regular, l.settingsCard.x + l.settingsCard.w * 0.5F, l.settingsCard.y + l.settingsCard.h * 0.5F, scaled(13.0F, l.scale), theme.textWeakArgb(), this.tr("clickgui.settings.none_selected", "No module selected"));
            return;
        }

        if (settingsPage)
        {
            float shiftX = (1.0F - settingsBlend) * slideDistance;
            int titleColor = NanoRenderUtils.mulAlpha(theme.textArgb(), settingsBlend);
            int subtitleColor = NanoRenderUtils.mulAlpha(theme.textWeakArgb(), settingsBlend);
            NanoUi.drawLeftText(vg, stack, bold, l.settingsHead.x + shiftX, l.settingsHead.y + scaled(7.0F, l.scale), scaled(16.0F, l.scale), titleColor, this.tr("clickgui.top.setting", "Setting"));
            NanoUi.drawLeftText(vg, stack, regular, l.settingsHead.x + shiftX, l.settingsHead.y + scaled(22.0F, l.scale), scaled(10.5F, l.scale), subtitleColor, this.tr("clickgui.settings.compat.subtitle", "ClickGUI global compatibility settings"));
            this.drawSettingsCompatTabs(vg, stack, l, theme, regular, shiftX, settingsBlend);
        }
        else
        {
            float shiftX = settingsBlend * slideDistance;
            int titleColor = NanoRenderUtils.mulAlpha(theme.textArgb(), 1.0F - settingsBlend);
            int subtitleColor = NanoRenderUtils.mulAlpha(theme.textWeakArgb(), 1.0F - settingsBlend);
            NanoUi.drawLeftText(vg, stack, bold, l.settingsHead.x + shiftX, l.settingsHead.y + scaled(7.0F, l.scale), scaled(16.0F, l.scale), titleColor, this.tr("clickgui.settings.title", "{0} Settings", this.selectedModule.getDisplayName()));
            NanoUi.drawLeftText(vg, stack, regular, l.settingsHead.x + shiftX, l.settingsHead.y + scaled(22.0F, l.scale), scaled(10.5F, l.scale), subtitleColor, this.selectedModule.getId() + " / " + this.categoryDisplayName(this.selectedModule.getCategory()));
            this.drawToggleButton(vg, stack, l.btnToggle, this.selectedModule.isEnabled(), regular, theme);
            this.drawButton(vg, stack, l.btnBind, this.tr("clickgui.bind.key_label", "Key: {0}", this.moduleBindLabel(this.selectedModule)), this.waitingBind, regular, theme);
        }

        if (l.hasPicker)
        {
            this.drawColorPicker(vg, stack, l, theme, regular);
        }

        List<Setting<?>> settings = this.visibleSettings(this.selectedModule);

        if (settings.isEmpty() && this.isClickGuiModuleSelected() && !settingsPage)
        {
            NanoUi.drawCenterText(vg, stack, regular, l.settingsRows.x + l.settingsRows.w * 0.5F, l.settingsRows.y + l.settingsRows.h * 0.5F, scaled(10.8F, l.scale), theme.textWeakArgb(), this.tr("clickgui.settings.global_hint", "Global client options moved to top 'Setting' page"));
            return;
        }

        int visible = Math.max(0, (int)Math.ceil((double)(l.settingsRows.h / l.rowSetting)) + 2);
        int scrollBase = (int)Math.floor((double)this.settingScrollVisual);
        float scrollOffset = (this.settingScrollVisual - (float)scrollBase) * l.rowSetting;
        NanoUi.beginClip(vg, l.settingsRows.x, l.settingsRows.y, l.settingsRows.w, l.settingsRows.h);
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);
        float listSpeed = this.resolveListAnimationSpeed(clickGui, animProfile);
        float rowMaster = UiMotion.clamp01(settingsPage ? settingsBlend : (1.0F - settingsBlend));
        float rowRadius = this.stableRowRadius(l.scale);

        for (int i = 0; i < visible; ++i)
        {
            int idx = scrollBase + i;

            if (idx >= settings.size())
            {
                break;
            }

            Setting<?> setting = settings.get(idx);
            String rowKey = "clickgui.setting.row." + this.selectedModule.getId() + "." + setting.getKey() + "." + (settingsPage ? this.settingsSection.name().toLowerCase(Locale.ROOT) : "module");
            float rowPresence = this.resolveListPresence(rowKey, idx, rowMaster, animProfile, listSpeed);
            float rowShiftX = rowPresence >= 0.99F ? 0.0F : (1.0F - rowPresence) * scaled(settingsPage ? 10.0F : 6.0F, l.scale);
            float rowShiftY = rowPresence >= 0.99F ? 0.0F : (1.0F - rowPresence) * scaled(4.0F, l.scale);
            Rect row = new Rect(l.settingsRows.x + rowShiftX, l.settingsRows.y + l.rowSetting * (float)i - scrollOffset + rowShiftY, l.settingsRows.w, Math.max(6.0F, l.rowSetting - 1.0F));
            boolean hovered = row.contains(this.mouseX, this.mouseY);
            boolean modified = this.isSettingModified(setting);
            boolean animationSetting = this.isAnimationSetting(setting);
            boolean prevAnimationSetting = idx > 0 && this.isAnimationSetting(settings.get(idx - 1));
            Rect valueRect = this.settingValueRect(row, l.scale);
            Rect resetRect = this.settingResetRect(row, l.scale);
            float rowAlpha = UiMotion.clamp01(rowPresence);

            if (rowAlpha <= 0.001F)
            {
                continue;
            }

            if (!settingsPage && animationSetting && !prevAnimationSetting)
            {
                NanoUi.drawLeftText(vg, stack, regular, row.x + scaled(2.0F, l.scale), row.y - scaled(7.0F, l.scale), scaled(9.0F, l.scale), NanoRenderUtils.mulAlpha(theme.textWeakArgb(), rowAlpha), this.tr("clickgui.settings.group.animation", "Animation"));
            }

            int base = i % 2 == 0 ? theme.rowArgb() : theme.cardAltArgb();
            NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, NanoRenderUtils.mulAlpha(base, rowAlpha), 0);
            float hoverRatio = UiAnimationBus.animateControl("clickgui.setting.hover." + this.selectedModule.getId() + "." + setting.getKey(), hovered ? 1.0F : 0.0F, animProfile);

            if (hoverRatio > 0.001F)
            {
                NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, NanoRenderUtils.mulAlpha(theme.rowHoverArgb(), UiMotion.clamp01(hoverRatio * 0.66F) * rowAlpha), 0);
            }

            float nameX = row.x + scaled(10.0F, l.scale);

            if (modified)
            {
                float md = scaled(4.0F, l.scale);
                NanoUi.drawSurface(vg, stack, nameX, row.y + row.h * 0.5F - md * 0.5F, md, md, md * 0.5F, NanoRenderUtils.mulAlpha(theme.accentArgb(), rowAlpha), 0);
                nameX += scaled(8.0F, l.scale);
            }

            NanoUi.drawLeftText(vg, stack, regular, nameX, row.y + row.h * 0.5F, scaled(11.5F, l.scale), NanoRenderUtils.mulAlpha(theme.textArgb(), rowAlpha), this.settingDisplayName(setting));
            this.drawResetButton(vg, stack, resetRect, modified, regular, theme, rowAlpha);

            if (rowAlpha <= 0.18F)
            {
                continue;
            }

            if (setting instanceof ColorSetting)
            {
                ColorSetting colorSetting = (ColorSetting)setting;
                boolean editing = this.activeColor == colorSetting;

                if (editing)
                {
                    NanoUi.drawAccentFlag(vg, stack, row.x + scaled(4.0F, l.scale), row.y + scaled(5.0F, l.scale), scaled(2.5F, l.scale), row.h - scaled(10.0F, l.scale), theme.accentArgb());
                }

                NanoUi.drawRightText(vg, stack, regular, valueRect.x2() - scaled(14.0F, l.scale), row.y + row.h * 0.5F, scaled(10.0F, l.scale), theme.textWeakArgb(), this.settingValue(setting));
                NanoUi.drawColorSwatch(vg, stack, valueRect.x2() - scaled(9.0F, l.scale), row.y + scaled(5.0F, l.scale), scaled(11.0F, l.scale), colorSetting.get().toArgb(), true);
            }
            else if (setting instanceof BoolSetting)
            {
                boolean enabled = ((BoolSetting)setting).isEnabled();
                String animKey = "clickgui.bool." + this.selectedModule.getId() + "." + setting.getKey();
                this.drawStateLabel(vg, stack, valueRect, enabled, hovered, theme, l.scale, animKey, regular);
            }
            else if (setting instanceof StringSetting)
            {
                this.drawSettingTextInput(vg, stack, row, (StringSetting)setting, hovered, theme, l.scale, regular);
            }
            else if (this.isSliderSetting(setting))
            {
                this.drawSettingSlider(vg, stack, row, setting, hovered, theme, l.scale, regular);
            }
            else if (this.isChoiceSetting(setting))
            {
                this.drawChoiceControl(vg, stack, row, setting, hovered, theme, l.scale, regular);
            }
            else
            {
                NanoUi.drawRightText(vg, stack, regular, valueRect.x2() - scaled(3.0F, l.scale), row.y + row.h * 0.5F, scaled(10.5F, l.scale), theme.textMutedArgb(), this.settingValue(setting));
            }
        }

        NanoUi.endClip(vg);
        this.drawExpandedChoicePopup(vg, stack, l, theme, regular);
    }

    private void drawSettingsCompatTabs(long vg, MemoryStack stack, Layout l, NanoTheme theme, int font, float offsetX, float alpha)
    {
        if (alpha <= 0.001F)
        {
            return;
        }

        this.drawSettingsCompatTab(vg, stack, l.settingsThemeTab, SettingsSection.THEME, this.settingsSection == SettingsSection.THEME, theme, font, offsetX, alpha);
        this.drawSettingsCompatTab(vg, stack, l.settingsAnimationTab, SettingsSection.ANIMATION, this.settingsSection == SettingsSection.ANIMATION, theme, font, offsetX, alpha);
    }

    private void drawSettingsCompatTab(long vg, MemoryStack stack, Rect rect, SettingsSection section, boolean active, NanoTheme theme, int font, float offsetX, float alpha)
    {
        Rect shifted = new Rect(rect.x + offsetX, rect.y, rect.w, rect.h);
        boolean hovered = shifted.contains(this.mouseX, this.mouseY);
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);
        String key = "clickgui.settings.tab." + section.name().toLowerCase(Locale.ROOT);
        float ratio = UiAnimationBus.animateControl(key, (active || hovered) ? 1.0F : 0.0F, animProfile);
        int fill = active ? theme.controlActiveArgb() : NanoScreenKit.mixArgb(theme.controlArgb(), theme.controlHoverArgb(), UiMotion.clamp01(0.24F + ratio * 0.72F));
        NanoUi.drawSurface(vg, stack, shifted.x, shifted.y, shifted.w, shifted.h, this.stableControlRadius(shifted.h / BTN_HEIGHT), NanoRenderUtils.mulAlpha(fill, alpha), NanoRenderUtils.mulAlpha(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 80), alpha));
        float k = UiMotion.clamp(shifted.h / BTN_HEIGHT, 0.35F, 1.85F);
        int textColor = active ? theme.textArgb() : NanoScreenKit.mixArgb(theme.textWeakArgb(), theme.textArgb(), UiMotion.clamp01(ratio * 0.72F));
        NanoUi.drawCenterText(vg, stack, font, shifted.x + shifted.w * 0.5F, shifted.y + shifted.h * 0.5F, scaled(10.8F, k), NanoRenderUtils.mulAlpha(textColor, alpha), this.settingsSectionLabel(section));
    }

    private void drawButton(long vg, MemoryStack stack, Rect rect, String label, boolean active, int font, NanoTheme theme)
    {
        boolean hovered = rect.contains(this.mouseX, this.mouseY);
        int fill = active ? theme.controlActiveArgb() : (hovered ? theme.controlHoverArgb() : theme.controlArgb());
        NanoUi.drawSurface(vg, stack, rect.x, rect.y, rect.w, rect.h, this.stableControlRadius(rect.h / BTN_HEIGHT), fill, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 84));
        float k = UiMotion.clamp(rect.h / BTN_HEIGHT, 0.35F, 1.85F);
        NanoUi.drawCenterText(vg, stack, font, rect.x + rect.w * 0.5F, rect.y + rect.h * 0.5F + scaled(0.5F, k), scaled(11.0F, k), theme.textArgb(), label);
    }

    private void drawResetButton(long vg, MemoryStack stack, Rect rect, boolean visible, int font, NanoTheme theme, float alpha)
    {
        if (!visible)
        {
            return;
        }

        float drawAlpha = UiMotion.clamp01(alpha);
        boolean hovered = rect.contains(this.mouseX, this.mouseY);
        int fill = hovered ? theme.controlHoverArgb() : theme.controlArgb();
        NanoUi.drawSurface(vg, stack, rect.x, rect.y, rect.w, rect.h, this.stableControlRadius(rect.h / 16.0F), NanoRenderUtils.mulAlpha(fill, drawAlpha), NanoRenderUtils.mulAlpha(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 88), drawAlpha));
        float k = UiMotion.clamp(rect.h / 16.0F, 0.35F, 1.85F);
        NanoUi.drawCenterText(vg, stack, font, rect.x + rect.w * 0.5F, rect.y + rect.h * 0.5F, scaled(8.6F, k), NanoRenderUtils.mulAlpha(theme.textWeakArgb(), drawAlpha), this.tr("clickgui.reset.short", "RST"));
    }

    private void drawToggleButton(long vg, MemoryStack stack, Rect rect, boolean enabled, int font, NanoTheme theme)
    {
        boolean hovered = rect.contains(this.mouseX, this.mouseY);
        float k = UiMotion.clamp(rect.h / BTN_HEIGHT, 0.35F, 1.85F);
        int fill = hovered ? theme.controlHoverArgb() : theme.controlArgb();
        NanoUi.drawSurface(vg, stack, rect.x, rect.y, rect.w, rect.h, this.stableControlRadius(k), fill, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 84));
        NanoUi.drawLeftText(vg, stack, font, rect.x + scaled(10.0F, k), rect.y + rect.h * 0.5F, scaled(10.8F, k), theme.textWeakArgb(), this.tr("clickgui.state.label", "State"));
        String moduleId = this.selectedModule == null ? "none" : this.selectedModule.getId();
        Rect valueRect = new Rect(rect.x2() - scaled(92.0F, k), rect.y + scaled(2.0F, k), scaled(84.0F, k), rect.h - scaled(4.0F, k));
        this.drawStateLabel(vg, stack, valueRect, enabled, hovered, theme, k, "clickgui.toggle." + moduleId, font);
    }

    private void drawStateLabel(long vg, MemoryStack stack, Rect valueRect, boolean enabled, boolean hovered, NanoTheme theme, float scale, String animKey, int font)
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);
        UiStateToggle.draw(
            vg,
            stack,
            valueRect.x,
            valueRect.y,
            valueRect.w,
            valueRect.h,
            enabled,
            hovered,
            theme,
            animProfile,
            animKey,
            font,
            scale,
            this.tr("clickgui.state.disable", "DISABLE"),
            this.tr("clickgui.state.enable", "ENABLE")
        );
    }

    private void drawModuleStatus(long vg, MemoryStack stack, Rect row, Module module, NanoTheme theme, float scale, float alpha, int font)
    {
        String bind = this.moduleBindLabel(module);
        boolean hasBind = this.moduleHasBind(module);
        boolean enabled = module.isEnabled();
        String status = hasBind ? bind : (enabled ? this.tr("clickgui.state.enable", "ENABLE") : this.tr("clickgui.state.disable", "DISABLE"));
        float badgeH = scaled(15.0F, scale);
        float badgeW = UiMotion.clamp(scaled(hasBind ? 96.0F : 82.0F, scale), scaled(62.0F, scale), scaled(124.0F, scale));
        float badgeX = row.x2() - scaled(8.0F, scale) - badgeW;
        float badgeY = row.y + (row.h - badgeH) * 0.5F;
        int badgeFill = hasBind ? NanoScreenKit.mixArgb(theme.controlArgb(), theme.controlHoverArgb(), 0.38F) : (enabled ? theme.controlActiveArgb() : theme.controlArgb());
        int textColor = enabled ? theme.textArgb() : NanoScreenKit.mixArgb(theme.textMutedArgb(), theme.textWeakArgb(), hasBind ? 0.38F : 0.10F);
        NanoUi.drawSurface(vg, stack, badgeX, badgeY, badgeW, badgeH, badgeH * 0.5F, NanoRenderUtils.mulAlpha(badgeFill, alpha), NanoRenderUtils.mulAlpha(theme.windowBorderArgb(), alpha * 0.45F));
        NanoUi.drawCenterText(vg, stack, font, badgeX + badgeW * 0.5F, badgeY + badgeH * 0.5F, scaled(9.8F, scale), NanoRenderUtils.mulAlpha(textColor, alpha), status);
    }

    private void drawSettingSlider(long vg, MemoryStack stack, Rect row, Setting<?> setting, boolean hovered, NanoTheme theme, float scale, int font)
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        Rect track = this.settingSliderTrackRect(row, k);
        Rect valueRect = this.settingValueRect(row, k);
        float ratio = this.settingSliderRatio(setting);
        String key = "clickgui.setting.slider." + (this.selectedModule == null ? "none" : this.selectedModule.getId()) + "." + setting.getKey();
        boolean dragging = this.draggingSettingSlider == setting;
        Rect dragTrack = dragging ? this.resolveSliderDragTrack(track) : track;
        float visualTarget = dragging ? NanoSliderController.mouseRatio((float)this.mouseX, dragTrack.x, dragTrack.w) : ratio;
        float displayRatio = NanoSliderController.resolveDisplayRatio(key + ".track", visualTarget, dragging, animProfile);
        float fillRatio = NanoSliderController.resolveFillRatio(key, visualTarget, dragging, animProfile);
        float knobRatio = NanoSliderController.resolveKnobRatio(key, visualTarget, dragging, animProfile);
        float focus = NanoSliderController.resolveFocus(key + ".focus", hovered, dragging, animProfile);
        float glowRatio = NanoSliderController.resolveGlow(key + ".glow", hovered, dragging, animProfile);
        NanoScreenKit.drawSliderTrack(vg, stack, theme, track.x, track.y, track.w, track.h, k, fillRatio, knobRatio, displayRatio, focus, glowRatio, dragging);
        this.drawNumberValueInput(vg, stack, valueRect, row, setting, hovered, theme, k, font, "clickgui.setting.number." + (this.selectedModule == null ? "none" : this.selectedModule.getId()) + "." + setting.getKey());
    }

    private void drawSettingTextInput(long vg, MemoryStack stack, Rect row, StringSetting setting, boolean hovered, NanoTheme theme, float scale, int font)
    {
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        Rect inputRect = this.settingTextInputRect(row, k);
        boolean active = this.activeTextSetting == setting && this.textInput.isFocused();
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        UiAnimProfile animProfile = this.resolveInputAnimationProfile(clickGui, this.resolveAnimationProfile(clickGui));
        String desc = setting.getDisplayDescription(this.selectedModuleId());
        String placeholder = desc == null || desc.isEmpty() ? this.tr("clickgui.input.text.placeholder", "Input text...") : desc;
        String animKey = "clickgui.setting.text." + (this.selectedModule == null ? "none" : this.selectedModule.getId()) + "." + setting.getKey();
        boolean fieldHovered = inputRect.contains(this.mouseX, this.mouseY);
        String text = this.activeTextSetting == setting ? this.textInputBuffer.get() : setting.get();
        this.textInput.draw(vg, stack, font, theme, inputRect.x, inputRect.y, inputRect.w, inputRect.h, k, scaled(10.2F, k), text, placeholder, hovered || fieldHovered, active, animKey, animProfile);
    }

    private void drawChoiceControl(long vg, MemoryStack stack, Rect row, Setting<?> setting, boolean hovered, NanoTheme theme, float scale, int font)
    {
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        Rect valueRect = this.settingValueRect(row, k);
        boolean active = this.expandedChoiceSetting == setting;
        boolean controlHovered = valueRect.contains(this.mouseX, this.mouseY);
        UiAnimProfile animProfile = this.resolveAnimationProfile(this.resolveClickGuiModule());
        String key = "clickgui.setting.choice." + (this.selectedModule == null ? "none" : this.selectedModule.getId()) + "." + setting.getKey();
        UiControlAnimations.ChoiceState anim = UiControlAnimations.choice(key, controlHovered || hovered, active, animProfile);
        float focus = anim.focus();
        float open = anim.open();
        int fill = NanoScreenKit.mixArgb(theme.cardAltArgb(), theme.controlArgb(), UiMotion.clamp01(0.34F + anim.hover() * 0.22F + focus * 0.30F));
        int border = NanoScreenKit.mixArgb(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 96), NanoRenderUtils.withAlpha(theme.accentArgb(), 150), UiMotion.clamp01(open * 0.64F + focus * 0.20F));
        float radius = Math.min(valueRect.h * 0.5F, this.stableControlRadius(k));
        NanoUi.drawSurface(vg, stack, valueRect.x, valueRect.y, valueRect.w, valueRect.h, radius, fill, border);
        String arrow = open > 0.55F ? "^" : "v";
        int arrowColor = NanoScreenKit.mixArgb(theme.textWeakArgb(), theme.textArgb(), UiMotion.clamp01(open * 0.70F + focus * 0.20F));
        int valueColor = NanoScreenKit.mixArgb(theme.textMutedArgb(), theme.textArgb(), UiMotion.clamp01(anim.hover() * 0.45F + focus * 0.36F));
        NanoUi.drawRightText(vg, stack, font, valueRect.x2() - scaled(6.0F, k), row.y + row.h * 0.5F, scaled(9.8F, k), arrowColor, arrow);
        NanoUi.drawLeftText(vg, stack, font, valueRect.x + scaled(6.0F, k), row.y + row.h * 0.5F, scaled(10.0F, k), valueColor, this.settingValue(setting));
    }

    private void drawExpandedChoicePopup(long vg, MemoryStack stack, Layout l, NanoTheme theme, int font)
    {
        ChoicePopupData popup = this.resolveChoicePopupData(l, this.expandedChoiceSetting);

        if (popup == null)
        {
            this.expandedChoiceSetting = null;
            return;
        }

        UiAnimProfile animProfile = this.resolveAnimationProfile(this.resolveClickGuiModule());
        String popupKey = "clickgui.setting.choice.popup." + (this.selectedModule == null ? "none" : this.selectedModule.getId()) + "." + popup.setting.getKey();
        float open = UiControlAnimations.open(popupKey, true, animProfile);

        if (open <= 0.01F)
        {
            return;
        }

        int popupFill = NanoRenderUtils.mulAlpha(theme.cardArgb(), open);
        int popupBorder = NanoRenderUtils.mulAlpha(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 102), open);
        NanoUi.drawSurface(vg, stack, popup.popupRect.x, popup.popupRect.y, popup.popupRect.w, popup.popupRect.h, this.stableControlRadius(l.scale), popupFill, popupBorder);
        NanoUi.beginClip(vg, popup.popupRect.x, popup.popupRect.y, popup.popupRect.w, popup.popupRect.h);

        for (int i = 0; i < popup.options.size(); ++i)
        {
            String option = popup.options.get(i);
            Rect optionRect = popup.optionRect(i);
            boolean selected = popup.isSelected(option);
            boolean hovered = optionRect.contains(this.mouseX, this.mouseY);
            float rowOpen = UiMotion.clamp01(open * 1.28F - (float)i * 0.08F);
            int fill = selected ? NanoRenderUtils.withAlpha(theme.controlActiveArgb(), 214) : (hovered ? theme.controlHoverArgb() : theme.controlArgb());
            float slideY = (1.0F - rowOpen) * scaled(4.0F, l.scale);
            NanoUi.drawSurface(vg, stack, optionRect.x + scaled(3.0F, l.scale), optionRect.y + scaled(1.0F, l.scale) + slideY, optionRect.w - scaled(6.0F, l.scale), optionRect.h - scaled(2.0F, l.scale), this.stableControlRadius(optionRect.h / BTN_HEIGHT), NanoRenderUtils.mulAlpha(fill, rowOpen), 0);

            String mark;
            if (popup.setting instanceof MultiSelectSetting)
            {
                mark = selected ? "[x]" : "[ ]";
            }
            else
            {
                mark = selected ? ">" : "-";
            }

            NanoUi.drawLeftText(vg, stack, font, optionRect.x + scaled(8.0F, l.scale), optionRect.y + optionRect.h * 0.5F + slideY, scaled(9.8F, l.scale), NanoRenderUtils.mulAlpha(theme.textWeakArgb(), rowOpen), mark);
            NanoUi.drawLeftText(vg, stack, font, optionRect.x + scaled(28.0F, l.scale), optionRect.y + optionRect.h * 0.5F + slideY, scaled(10.1F, l.scale), NanoRenderUtils.mulAlpha(theme.textArgb(), rowOpen), popup.optionLabel(option));
        }

        NanoUi.endClip(vg);
    }

    private boolean handleExpandedChoiceClick(Layout l, int mouseX, int mouseY)
    {
        ChoicePopupData popup = this.resolveChoicePopupData(l, this.expandedChoiceSetting);

        if (popup == null)
        {
            this.expandedChoiceSetting = null;
            return false;
        }

        if (popup.valueRect.contains(mouseX, mouseY))
        {
            this.expandedChoiceSetting = null;
            return true;
        }

        if (!popup.popupRect.contains(mouseX, mouseY))
        {
            this.expandedChoiceSetting = null;
            return false;
        }

        int index = popup.optionIndex(mouseY);

        if (index < 0 || index >= popup.options.size())
        {
            return true;
        }

        String option = popup.options.get(index);

        if (popup.setting instanceof EnumSetting)
        {
            this.applyEnumOption((EnumSetting<?>)popup.setting, option);
            this.expandedChoiceSetting = null;
        }
        else if (popup.setting instanceof MultiSelectSetting)
        {
            ((MultiSelectSetting)popup.setting).toggle(option);
        }

        return true;
    }

    private void toggleChoiceSettingExpanded(Setting<?> setting)
    {
        if (!this.isChoiceSetting(setting))
        {
            return;
        }

        this.commitActiveNumberInput();
        this.cancelActiveTextInput();
        this.draggingSettingSlider = null;
        this.clearSliderTrackLock();
        this.expandedChoiceSetting = this.expandedChoiceSetting == setting ? null : setting;
    }

    private boolean isChoiceSetting(Setting<?> setting)
    {
        return setting instanceof EnumSetting || setting instanceof MultiSelectSetting;
    }

    private void applyEnumOption(EnumSetting<?> setting, String optionName)
    {
        if (setting == null || optionName == null)
        {
            return;
        }

        setting.setByName(optionName);

        if ("locale".equals(setting.getKey()))
        {
            this.syncLocaleToI18n();
        }
    }

    private void syncLocaleToI18n()
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();

        if (clickGui == null)
        {
            return;
        }

        client.i18n.ClientLocale locale = clickGui.getLocale();
        client.i18n.I18nManager i18n = ClientBootstrap.instance().getI18n();

        if (i18n != null && locale != null)
        {
            i18n.setCurrentLocale(locale.getCode());
            ClientBootstrap.instance().getConfigManager().markClientDirty();
        }
    }

    private void syncLocaleFromI18n()
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();

        if (clickGui == null)
        {
            return;
        }

        client.i18n.I18nManager i18n = ClientBootstrap.instance().getI18n();

        if (i18n != null)
        {
            client.i18n.ClientLocale resolved = client.i18n.ClientLocale.fromCode(i18n.getCurrentLocale());
            clickGui.setLocale(resolved);
        }
    }

    private ChoicePopupData resolveChoicePopupData(Layout l, Setting<?> setting)
    {
        if (l == null || setting == null || this.selectedModule == null || !this.isChoiceSetting(setting))
        {
            return null;
        }

        List<Setting<?>> settings = this.visibleSettings(this.selectedModule);
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
        Rect valueRect = this.settingValueRect(row, l.scale);
        List<String> options = this.choiceOptions(setting);

        if (options.isEmpty())
        {
            return null;
        }

        float optionH = Math.max(scaled(14.0F, l.scale), row.h - scaled(4.0F, l.scale));
        float popupW = Math.max(valueRect.w, scaled(132.0F, l.scale));
        float popupH = optionH * (float)options.size() + scaled(6.0F, l.scale);
        float popupX = valueRect.x2() - popupW;
        float popupY = valueRect.y2() + scaled(4.0F, l.scale);
        float minX = l.window.x + scaled(8.0F, l.scale);
        float maxX = l.window.x2() - popupW - scaled(8.0F, l.scale);
        popupX = UiMotion.clamp(popupX, minX, Math.max(minX, maxX));
        float minY = l.header.y2() + scaled(6.0F, l.scale);
        float maxY = l.window.y2() - popupH - scaled(8.0F, l.scale);

        if (popupY > maxY)
        {
            popupY = valueRect.y - popupH - scaled(4.0F, l.scale);
        }

        popupY = UiMotion.clamp(popupY, minY, Math.max(minY, maxY));
        Rect popupRect = new Rect(popupX, popupY, popupW, popupH);
        return new ChoicePopupData(setting, valueRect, popupRect, optionH, l.scale, options);
    }

    private List<String> choiceOptions(Setting<?> setting)
    {
        if (setting instanceof EnumSetting)
        {
            EnumSetting<?> enumSetting = (EnumSetting<?>)setting;
            Enum<?>[] values = enumSetting.getEnumType().getEnumConstants();

            if (values == null || values.length == 0)
            {
                return Collections.emptyList();
            }

            List<String> options = new ArrayList<String>(values.length);

            for (int i = 0; i < values.length; ++i)
            {
                options.add(values[i].name());
            }

            return options;
        }

        if (setting instanceof MultiSelectSetting)
        {
            return ((MultiSelectSetting)setting).getOptions();
        }

        return Collections.emptyList();
    }

    private void drawNumberValueInput(long vg, MemoryStack stack, Rect valueRect, Rect row, Setting<?> setting, boolean hoveredRow, NanoTheme theme, float scale, int font, String animKey)
    {
        if (valueRect == null || setting == null)
        {
            return;
        }

        boolean fieldHovered = valueRect.contains(this.mouseX, this.mouseY);
        boolean active = this.activeNumberSetting == setting && this.textInput.isFocused();
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        UiAnimProfile animProfile = this.resolveInputAnimationProfile(clickGui, this.resolveAnimationProfile(clickGui));

        if (active)
        {
            this.textInput.draw(vg, stack, font, theme, valueRect.x, valueRect.y, valueRect.w, valueRect.h, scale, scaled(10.2F, scale), this.numberInputBuffer.get(), this.tr("clickgui.input.number.placeholder", "Input..."), hoveredRow || fieldHovered, true, animKey, animProfile);
            return;
        }

        float focus = UiAnimationBus.animateControl(animKey + ".idle.focus", fieldHovered ? 1.0F : 0.0F, animProfile);
        int fill = NanoScreenKit.mixArgb(theme.cardAltArgb(), theme.controlArgb(), UiMotion.clamp01(0.38F + focus * 0.32F));
        int border = NanoScreenKit.mixArgb(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 92), NanoRenderUtils.withAlpha(theme.accentArgb(), 142), UiMotion.clamp01(focus * 0.72F));
        float radius = Math.min(valueRect.h * 0.5F, this.stableControlRadius(scale));
        NanoUi.drawSurface(vg, stack, valueRect.x, valueRect.y, valueRect.w, valueRect.h, radius, fill, border);
        NanoUi.drawRightText(vg, stack, font, valueRect.x2() - scaled(4.0F, scale), row.y + row.h * 0.5F + scaled(0.45F, scale), scaled(10.2F, scale), theme.textMutedArgb(), this.settingValue(setting));
    }

    private void drawColorPicker(long vg, MemoryStack stack, Layout l, NanoTheme theme, int regular)
    {
        float k = l.scale;
        NanoUi.drawSurface(vg, stack, l.pickerCard.x, l.pickerCard.y, l.pickerCard.w, l.pickerCard.h, this.stableControlRadius(k), theme.cardArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 72));
        NanoUi.drawLeftText(vg, stack, regular, l.pickerCard.x + scaled(8.0F, k), l.pickerCard.y + scaled(11.0F, k), scaled(10.5F, k), theme.textWeakArgb(), this.tr("clickgui.picker.title", "HSV Color Picker"));

        this.syncPickerSubBounds(l);
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);
        this.colorPicker.setBounds(l.pickerCard.x, l.pickerCard.y, l.pickerCard.w, l.pickerCard.h);
        this.colorPicker.render(vg, stack, theme, animProfile, k, this.mouseX, this.mouseY);

        NanoRenderUtils.strokeRoundedRect(vg, l.pickerSv.x, l.pickerSv.y, l.pickerSv.w, l.pickerSv.h, scaled(4.0F, k), 1.0F, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 112)));
        NanoRenderUtils.strokeRoundedRect(vg, l.pickerAlpha.x, l.pickerAlpha.y, l.pickerAlpha.w, l.pickerAlpha.h, scaled(3.0F, k), 1.0F, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 112)));

        int previewArgb = this.colorPicker.getCurrentArgb();
        NanoUi.drawSurface(vg, stack, l.pickerPreview.x, l.pickerPreview.y, l.pickerPreview.w, l.pickerPreview.h, scaled(4.0F, k), previewArgb, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 98));
        NanoUi.drawLeftText(vg, stack, regular, l.pickerPreview.x2() + scaled(8.0F, k), l.pickerPreview.y + scaled(7.0F, k), scaled(10.0F, k), theme.textMutedArgb(), String.format(Locale.ROOT, "#%08X", Integer.valueOf(previewArgb)));
        NanoUi.drawLeftText(vg, stack, regular, l.pickerPreview.x2() + scaled(8.0F, k), l.pickerPreview.y + scaled(20.0F, k), scaled(10.0F, k), theme.textWeakArgb(), this.tr("clickgui.picker.hsva", "H {0} S {1} V {2} A {3}", this.trimDecimal((double)this.colorPicker.getHue(), 2), this.trimDecimal((double)this.colorPicker.getSaturation(), 2), this.trimDecimal((double)this.colorPicker.getBrightness(), 2), this.trimDecimal((double)this.colorPicker.getAlpha() / 255.0D, 2)));
    }

    private void syncPickerSubBounds(Layout l)
    {
        if (l.pickerSv == null || l.pickerHue == null || l.pickerAlpha == null)
        {
            return;
        }

        this.colorPicker.setSubBounds(
            l.pickerSv.x, l.pickerSv.y, l.pickerSv.w, l.pickerSv.h,
            l.pickerHue.x, l.pickerHue.y, l.pickerHue.w, l.pickerHue.h,
            l.pickerAlpha.x, l.pickerAlpha.y, l.pickerAlpha.w, l.pickerAlpha.h,
            true);
    }

    private void drawResizeHandle(long vg, MemoryStack stack, Layout l, NanoTheme theme)
    {
        NanoUi.drawSurface(vg, stack, l.resizeHandle.x, l.resizeHandle.y, l.resizeHandle.w, l.resizeHandle.h, scaled(4.0F, l.scale), NanoRenderUtils.withAlpha(theme.controlActiveArgb(), 190), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 90));
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

    private Rect settingTextInputRect(Rect row, float scale)
    {
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        Rect resetRect = this.settingResetRect(row, k);
        float rightGap = scaled(8.0F, k);
        float inputX = row.x + scaled(118.0F, k);
        float inputRight = resetRect.x - rightGap;
        float inputW = Math.max(scaled(78.0F, k), inputRight - inputX);
        float inputH = Math.max(scaled(14.0F, k), row.h - scaled(6.0F, k));
        float inputY = row.y + (row.h - inputH) * 0.5F;
        return new Rect(inputX, inputY, inputW, inputH);
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
        if (this.draggingSettingSlider == null || this.selectedModule == null)
        {
            this.draggingSettingSlider = null;
            this.clearSliderTrackLock();
            return;
        }

        List<Setting<?>> settings = this.visibleSettings(this.selectedModule);
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

    private Rect activeTextInputRect(Layout l, StringSetting setting)
    {
        if (l == null || setting == null || this.selectedModule == null)
        {
            return null;
        }

        List<Setting<?>> settings = this.visibleSettings(this.selectedModule);
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
        return this.settingTextInputRect(row, l.scale);
    }

    private Rect activeNumberInputRect(Layout l, Setting<?> setting)
    {
        if (l == null || setting == null || this.selectedModule == null)
        {
            return null;
        }

        List<Setting<?>> settings = this.visibleSettings(this.selectedModule);
        int index = settings.indexOf(setting);

        if (index < 0 || !this.isSliderSetting(setting))
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

    private Rect activeInlineInputRect(Layout l)
    {
        if (this.activeTextSetting != null)
        {
            return this.activeTextInputRect(l, this.activeTextSetting);
        }

        if (this.activeNumberSetting != null)
        {
            return this.activeNumberInputRect(l, this.activeNumberSetting);
        }

        return null;
    }

    private void activateTextInput(Rect inputRect, float scale, int mouseX, int mouseY, StringSetting setting)
    {
        if (setting == null || inputRect == null)
        {
            return;
        }

        this.commitActiveNumberInput();
        this.activeNumberSetting = null;

        if (this.activeTextSetting != null && this.activeTextSetting != setting)
        {
            this.cancelActiveTextInput();
        }

        if (this.activeTextSetting != setting)
        {
            this.activeTextSetting = setting;
            this.textInputBuffer.set(setting.get());
        }

        if (this.lastNanoVg != 0L)
        {
            NanoFontBook.ensureLoaded(this.lastNanoVg);
        }

        this.textInput.onMouseDown(mouseX, mouseY, inputRect.x, inputRect.y, inputRect.w, inputRect.h, this.lastNanoVg, NanoFontBook.uiRegular(), scaled(10.2F, UiMotion.clamp(scale, 0.35F, 1.85F)), this.textInputBuffer.get());
    }

    private void activateNumberInput(Rect inputRect, float scale, int mouseX, int mouseY, Setting<?> setting)
    {
        if (setting == null || inputRect == null || !this.isSliderSetting(setting))
        {
            return;
        }

        this.commitActiveNumberInput();
        this.cancelActiveTextInput();
        this.activeNumberSetting = setting;
        this.numberInputBuffer.set(this.settingRawInputValue(setting));

        if (this.lastNanoVg != 0L)
        {
            NanoFontBook.ensureLoaded(this.lastNanoVg);
        }

        this.textInput.onMouseDown(mouseX, mouseY, inputRect.x, inputRect.y, inputRect.w, inputRect.h, this.lastNanoVg, NanoFontBook.uiRegular(), scaled(10.2F, UiMotion.clamp(scale, 0.35F, 1.85F)), this.numberInputBuffer.get());
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

        if (this.activeTextSetting != null && this.textInput.isFocused())
        {
            Rect inputRect = this.activeTextInputRect(l, this.activeTextSetting);

            if (inputRect != null)
            {
                this.textInput.onMouseDrag(this.mouseX, inputRect.x, inputRect.w, this.lastNanoVg, NanoFontBook.uiRegular(), scaled(10.2F, l.scale), this.textInputBuffer.get());
            }
        }
        else if (this.activeNumberSetting != null && this.textInput.isFocused())
        {
            Rect inputRect = this.activeNumberInputRect(l, this.activeNumberSetting);

            if (inputRect != null)
            {
                this.textInput.onMouseDrag(this.mouseX, inputRect.x, inputRect.w, this.lastNanoVg, NanoFontBook.uiRegular(), scaled(10.2F, l.scale), this.numberInputBuffer.get());
            }
        }

        if (this.colorPicker.isDragging())
        {
            this.colorPicker.mouseDragged(this.mouseX, this.mouseY);
            this.commitPickerColor(false);
        }
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
            if (button == 1)
            {
                this.cycleEnum((EnumSetting<?>)setting, -1);
            }
            else
            {
                this.toggleChoiceSettingExpanded(setting);
            }
            return;
        }

        if (setting instanceof MultiSelectSetting)
        {
            this.toggleChoiceSettingExpanded(setting);
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

    private void commitPickerColor(boolean force)
    {
        if (this.activeColor == null)
        {
            return;
        }

        int rgb = this.colorPicker.getCurrentArgb();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int a = this.colorPicker.getAlpha();
        ColorValue current = this.activeColor.get();

        if (!force && current.getR() == r && current.getG() == g && current.getB() == b && current.getA() == a && !current.isRainbow())
        {
            return;
        }

        this.activeColor.set(new ColorValue(r, g, b, a, false));
    }

    private void loadPickerFrom(ColorValue color)
    {
        this.colorPicker.setColor(color.getR(), color.getG(), color.getB(), color.getA(), color.isRainbow());
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
            return value == null ? this.tr("clickgui.value.null", "null") : setting.getDisplayOption(this.selectedModuleId(), value);
        }

        if (setting instanceof MultiSelectSetting)
        {
            MultiSelectSetting multi = (MultiSelectSetting)setting;
            int count = multi.get().size();

            if (count <= 0)
            {
                return this.tr("clickgui.multiselect.none", "None");
            }

            if (count == 1)
            {
                for (String option : multi.get())
                {
                    return setting.getDisplayOption(this.selectedModuleId(), option);
                }
            }

            return this.tr("clickgui.multiselect.count", "{0} selected", Integer.valueOf(count));
        }

        if (setting instanceof ColorSetting)
        {
            return String.format(Locale.ROOT, "#%08X", Integer.valueOf(((ColorSetting)setting).get().toArgb()));
        }

        Object value = setting.get();
        return value == null ? this.tr("clickgui.value.null", "null") : String.valueOf(value);
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

    private String moduleBindLabel(Module module)
    {
        if (module == null || module.getBind() == null)
        {
            return this.tr("clickgui.bind.none", "None");
        }

        String bind = module.getBind().getDisplayName();
        return bind == null || "NONE".equalsIgnoreCase(bind) ? this.tr("clickgui.bind.none", "None") : bind;
    }

    private boolean moduleHasBind(Module module)
    {
        if (module == null || module.getBind() == null)
        {
            return false;
        }

        String bind = module.getBind().getDisplayName();
        return bind != null && !bind.trim().isEmpty() && !"NONE".equalsIgnoreCase(bind);
    }

    private String categoryDisplayName(Category category)
    {
        if (category == null)
        {
            return this.tr("category.none", "none");
        }

        return category.getDisplayName();
    }

    private String settingDisplayName(Setting<?> setting)
    {
        if (setting == null)
        {
            return "";
        }

        return setting.getDisplayName(this.selectedModuleId());
    }

    private String selectedModuleId()
    {
        return this.selectedModule == null ? null : this.selectedModule.getId();
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

    private boolean isClickGuiModuleSelected()
    {
        return this.selectedModule != null && "click_gui".equalsIgnoreCase(this.selectedModule.getId());
    }

    private boolean isGlobalClientSetting(Setting<?> setting)
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
            || key.startsWith("ui_back_")
            || "palette".equals(key)
            || "corner_radius".equals(key)
            || "panel_alpha".equals(key)
            || "backdrop".equals(key)
            || "backdrop_alpha".equals(key)
            || "accent_override_enabled".equals(key)
            || "accent_override".equals(key)
            || "locale".equals(key);
    }

    private void ensureSelection()
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        List<CategoryEntry> categories = this.categoryEntries();

        if (categories.isEmpty())
        {
            this.selectedCategory = null;
            this.selectedModule = null;
            this.waitingBind = false;
            return;
        }

        boolean categoryOk = false;
        int rememberedCategory = clickGui == null ? -1 : clickGui.getLastCategoryOrdinal();

        for (int i = 0; i < categories.size(); ++i)
        {
            if (categories.get(i).category == this.selectedCategory)
            {
                categoryOk = true;
                break;
            }
        }

        if (!categoryOk)
        {
            Category restored = null;

            for (int i = 0; i < categories.size(); ++i)
            {
                if (categories.get(i).category.ordinal() == rememberedCategory)
                {
                    restored = categories.get(i).category;
                    break;
                }
            }

            this.selectedCategory = restored == null ? categories.get(0).category : restored;
        }

        this.ensureModuleSelected(clickGui);
        this.rememberSelection();
    }

    private void ensureModuleSelected(ClickGuiModule clickGui)
    {
        List<Module> list = this.currentModules();

        if (list.isEmpty())
        {
            this.selectedModule = null;
            this.waitingBind = false;
            this.draggingSettingSlider = null;
            return;
        }

        if (this.selectedModule != null && list.contains(this.selectedModule))
        {
            return;
        }

        int restoredIndex = clickGui == null ? 0 : clickGui.getLastModuleIndex();
        restoredIndex = Math.max(0, Math.min(restoredIndex, list.size() - 1));
        this.selectedModule = list.get(restoredIndex);
    }

    private void rememberSelection()
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();

        if (clickGui == null || this.selectedCategory == null)
        {
            return;
        }

        clickGui.setLastCategoryOrdinal(this.selectedCategory.ordinal());
        List<Module> modulesInCategory = this.currentModules();
        int idx = this.selectedModule == null ? 0 : modulesInCategory.indexOf(this.selectedModule);
        clickGui.setLastModuleIndex(Math.max(0, idx));
    }

    private void restorePersistedScrollState()
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();

        if (clickGui == null)
        {
            this.categoryScroll = Math.max(0, this.categoryScroll);
            this.moduleScroll = Math.max(0, this.moduleScroll);
            this.moduleSettingScroll = Math.max(0, this.moduleSettingScroll);
            this.compatSettingScroll = Math.max(0, this.compatSettingScroll);
            this.settingScroll = this.isSettingsPage() ? this.compatSettingScroll : this.moduleSettingScroll;
            this.settingScrollTarget = (float)this.settingScroll;
            return;
        }

        this.categoryScroll = Math.max(0, clickGui.getLastCategoryScroll());
        this.moduleScroll = Math.max(0, clickGui.getLastModuleScroll());
        this.moduleSettingScroll = Math.max(0, clickGui.getLastSettingScroll());
        this.compatSettingScroll = Math.max(0, clickGui.getLastCompatSettingScroll());
        this.settingScroll = this.isSettingsPage() ? this.compatSettingScroll : this.moduleSettingScroll;
        this.settingScrollTarget = (float)this.settingScroll;
    }

    private void persistScrollState()
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();

        if (clickGui == null)
        {
            return;
        }

        this.captureActiveSettingScroll();
        clickGui.setLastCategoryScroll(Math.max(0, this.categoryScroll));
        clickGui.setLastModuleScroll(Math.max(0, this.moduleScroll));
        clickGui.setLastSettingScroll(Math.max(0, this.moduleSettingScroll));
        clickGui.setLastCompatSettingScroll(Math.max(0, this.compatSettingScroll));
    }

    private void captureActiveSettingScroll()
    {
        int resolved = Math.max(0, Math.round(this.settingScrollTarget));
        this.settingScroll = resolved;

        if (this.isSettingsPage())
        {
            this.compatSettingScroll = resolved;
        }
        else
        {
            this.moduleSettingScroll = resolved;
        }
    }

    private void validateActiveColor()
    {
        if (this.activeColor == null || this.selectedModule == null)
        {
            return;
        }

        List<Setting<?>> settings = this.selectedModule.getSettings();

        if (!settings.contains(this.activeColor) || !this.activeColor.isVisible() || !this.visibleSettings(this.selectedModule).contains(this.activeColor))
        {
            this.activeColor = null;
        }
    }

    private void validateActiveTextSetting()
    {
        if (this.activeTextSetting == null)
        {
            return;
        }

        if (this.selectedModule == null)
        {
            this.cancelActiveTextInput();
            return;
        }

        List<Setting<?>> settings = this.selectedModule.getSettings();

        if (!settings.contains(this.activeTextSetting) || !this.activeTextSetting.isVisible() || !this.visibleSettings(this.selectedModule).contains(this.activeTextSetting))
        {
            this.cancelActiveTextInput();
            return;
        }

        if (!this.textInput.isFocused())
        {
            this.cancelActiveTextInput();
        }
    }

    private void validateActiveNumberSetting()
    {
        if (this.activeNumberSetting == null)
        {
            return;
        }

        if (!this.textInput.isFocused() || this.selectedModule == null || !this.isSliderSetting(this.activeNumberSetting))
        {
            this.activeNumberSetting = null;
            this.textInput.blur();
            return;
        }

        List<Setting<?>> settings = this.selectedModule.getSettings();

        if (!settings.contains(this.activeNumberSetting) || !this.activeNumberSetting.isVisible() || !this.visibleSettings(this.selectedModule).contains(this.activeNumberSetting))
        {
            this.activeNumberSetting = null;
            this.textInput.blur();
        }
    }

    private void validateExpandedChoiceSetting()
    {
        if (this.expandedChoiceSetting == null)
        {
            return;
        }

        if (!this.isChoiceSetting(this.expandedChoiceSetting) || this.selectedModule == null)
        {
            this.expandedChoiceSetting = null;
            return;
        }

        List<Setting<?>> settings = this.selectedModule.getSettings();

        if (!settings.contains(this.expandedChoiceSetting) || !this.expandedChoiceSetting.isVisible() || !this.visibleSettings(this.selectedModule).contains(this.expandedChoiceSetting))
        {
            this.expandedChoiceSetting = null;
        }
    }

    private void clearInlineEditors(boolean commitNumber)
    {
        if (commitNumber)
        {
            this.commitActiveNumberInput();
        }
        else
        {
            this.activeNumberSetting = null;
        }

        this.cancelActiveTextInput();
    }

    private void commitActiveTextInput()
    {
        if (this.activeTextSetting == null)
        {
            return;
        }

        this.activeTextSetting.set(this.textInputBuffer.get());
        this.activeTextSetting = null;
        this.textInput.blur();
    }

    private void cancelActiveTextInput()
    {
        if (this.activeTextSetting != null)
        {
            this.textInputBuffer.set(this.activeTextSetting.get());
            this.activeTextSetting = null;
        }

        this.textInput.blur();
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

    private List<Module> currentModules()
    {
        return this.selectedCategory == null ? Collections.<Module>emptyList() : this.modules.getByCategory(this.selectedCategory);
    }

    private int categoryIndex(List<CategoryEntry> entries, Category category)
    {
        if (entries == null || entries.isEmpty() || category == null)
        {
            return 0;
        }

        for (int i = 0; i < entries.size(); ++i)
        {
            if (entries.get(i).category == category)
            {
                return i;
            }
        }

        return 0;
    }

    private List<CategoryEntry> categoryEntries()
    {
        List<CategoryEntry> out = new ArrayList<CategoryEntry>();
        Category[] values = Category.values();

        for (int i = 0; i < values.length; ++i)
        {
            int count = this.modules.getByCategory(values[i]).size();

            if (count > 0)
            {
                out.add(new CategoryEntry(values[i], count));
            }
        }

        return out;
    }

    private List<Setting<?>> visibleSettings(Module module)
    {
        if (module == null)
        {
            return Collections.emptyList();
        }

        List<Setting<?>> out = new ArrayList<Setting<?>>();
        List<Setting<?>> all = module.getSettings();
        boolean clickGuiModule = "click_gui".equalsIgnoreCase(module.getId());

        for (int i = 0; i < all.size(); ++i)
        {
            Setting<?> setting = all.get(i);

            if (!setting.isVisible())
            {
                continue;
            }

            if (clickGuiModule && this.isSettingsPage() && !this.isGlobalClientSetting(setting))
            {
                continue;
            }

            if (clickGuiModule && this.isSettingsPage())
            {
                boolean animationSetting = this.isAnimationSetting(setting);

                if (this.settingsSection == SettingsSection.ANIMATION && !animationSetting)
                {
                    continue;
                }

                if (this.settingsSection == SettingsSection.THEME && animationSetting)
                {
                    continue;
                }
            }

            if (clickGuiModule && !this.isSettingsPage() && this.isGlobalClientSetting(setting))
            {
                continue;
            }

            out.add(setting);
        }

        return out;
    }

    private void syncWindowTarget(UiScaleEditModule uiScale)
    {
        float scale = uiScale == null ? 1.0F : uiScale.getUiScale(UiScaleEditModule.UiTarget.CLICK_GUI);
        float targetWidth = UiMotion.clamp(BASE_WINDOW_WIDTH * scale, MIN_WINDOW_WIDTH, (float)this.width - SCREEN_MARGIN * 2.0F);
        float targetHeight = UiMotion.clamp(BASE_WINDOW_HEIGHT * scale, MIN_WINDOW_HEIGHT, (float)this.height - SCREEN_MARGIN * 2.0F);
        float targetX = ((float)this.width - targetWidth) * 0.5F;
        float targetY = ((float)this.height - targetHeight) * 0.5F;

        if (uiScale != null)
        {
            targetX = uiScale.getWindowAnchorX(UiScaleEditModule.UiTarget.CLICK_GUI) * (float)this.width - targetWidth * 0.5F;
            targetY = uiScale.getWindowAnchorY(UiScaleEditModule.UiTarget.CLICK_GUI) * (float)this.height - targetHeight * 0.5F;
        }

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

    private void syncScaleAndAnchorFromWindow(UiScaleEditModule uiScale)
    {
        if (uiScale == null)
        {
            return;
        }

        float widthRatio = this.window.getTargetWidth() / BASE_WINDOW_WIDTH;
        float heightRatio = this.window.getTargetHeight() / BASE_WINDOW_HEIGHT;
        uiScale.setUiScale(UiScaleEditModule.UiTarget.CLICK_GUI, Math.min(widthRatio, heightRatio));

        float cx = this.window.getTargetX() + this.window.getTargetWidth() * 0.5F;
        float cy = this.window.getTargetY() + this.window.getTargetHeight() * 0.5F;
        float anchorX = cx / Math.max(1.0F, (float)this.width);
        float anchorY = cy / Math.max(1.0F, (float)this.height);
        uiScale.setWindowAnchor(UiScaleEditModule.UiTarget.CLICK_GUI, anchorX, anchorY);
    }

    private UiAnimProfile resolveAnimationProfile(ClickGuiModule clickGui)
    {
        return UiAnimProfiles.clickGuiProfile(clickGui);
    }

    private UiAnimProfile resolveInputAnimationProfile(ClickGuiModule clickGui, UiAnimProfile profile)
    {
        return UiAnimProfiles.inputProfile(clickGui, profile);
    }

    private float resolvePageAnimationSpeed(ClickGuiModule clickGui, UiAnimProfile profile)
    {
        return UiAnimProfiles.pageSpeed(clickGui, profile);
    }

    private float resolveListAnimationSpeed(ClickGuiModule clickGui, UiAnimProfile profile)
    {
        return UiAnimProfiles.listSpeed(clickGui, profile);
    }

    private float resolveSelectionAnimationSpeed(ClickGuiModule clickGui, UiAnimProfile profile)
    {
        return UiAnimProfiles.selectionSpeed(clickGui, profile);
    }

    private float resolveListPresence(String key, int index, float master, UiAnimProfile profile, float speed)
    {
        return UiControlAnimations.stagger(key, master, index, LIST_STAGGER_STEP, profile, speed);
    }

    private void drawAnimatedSelectionBox(long vg, MemoryStack stack, String key, Rect target, NanoTheme theme, float scale, UiAnimProfile profile, ClickGuiModule clickGui, float visibility)
    {
        float speed = this.resolveSelectionAnimationSpeed(clickGui, profile);
        boolean hasTarget = target != null;
        float x = hasTarget ? target.x : 0.0F;
        float y = hasTarget ? target.y : 0.0F;
        float w = hasTarget ? target.w : 0.0F;
        float h = hasTarget ? target.h : 0.0F;
        float radius = hasTarget ? Math.min(target.h * 0.5F, this.stableRowRadius(scale)) : this.stableRowRadius(scale);
        UiSelectionBox.draw(vg, stack, key, hasTarget, visibility, x, y, w, h, radius, theme, profile, speed);
    }

    private UiAnimProfile resolveWindowAnimationProfile(ClickGuiModule clickGui, UiScaleEditModule uiScale)
    {
        boolean interacting = this.window.isInteracting() || this.draggingSettingSlider != null || this.colorPicker.isDragging();
        return UiAnimProfiles.clickGuiWindowProfile(clickGui, uiScale, interacting);
    }

    private ClickGuiModule resolveClickGuiModule()
    {
        Module module = this.modules.getById("click_gui");
        return module instanceof ClickGuiModule ? (ClickGuiModule)module : null;
    }

    private UiScaleEditModule resolveUiScaleModule()
    {
        Module module = this.modules.getById("ui_scale_edit");
        return module instanceof UiScaleEditModule ? (UiScaleEditModule)module : null;
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

    private void openHudEdit()
    {
        Module uiScale = this.modules.getById("ui_scale_edit");

        if (uiScale instanceof UiScaleEditModule)
        {
            ((UiScaleEditModule)uiScale).setEditTarget(UiScaleEditModule.UiTarget.HUD_EDIT);
        }

        this.requestTransition(TransitionMode.SWITCH, new HudEditorScreen(ClientBootstrap.instance().getHud()));
    }

    private void openUiScaleEdit()
    {
        Module module = this.modules.getById("ui_scale_edit");

        if (module instanceof UiScaleEditModule)
        {
            this.requestTransition(TransitionMode.SWITCH, new UIScaleEditScreen((UiScaleEditModule)module, this));
            return;
        }

        this.requestTransition(TransitionMode.CLOSE, null);
    }

    private boolean isSettingsPage()
    {
        return this.contentPage == ContentPage.SETTINGS;
    }

    private void toggleSettingsCompatPage()
    {
        if (this.isSettingsPage())
        {
            this.exitSettingsCompatPage();
            return;
        }

        this.enterSettingsCompatPage();
    }

    private void enterSettingsCompatPage()
    {
        if (this.isSettingsPage())
        {
            return;
        }

        this.moduleSettingScroll = Math.max(0, Math.round(this.settingScrollTarget));
        this.restoreCategory = this.selectedCategory;
        this.restoreModuleId = this.selectedModule == null ? null : this.selectedModule.getId();
        this.contentPage = ContentPage.SETTINGS;
        this.waitingBind = false;
        this.clearInlineEditors(true);
        this.draggingSettingSlider = null;
        this.expandedChoiceSetting = null;
        this.activeColor = null;
        this.settingsSection = SettingsSection.THEME;
        this.settingScroll = Math.max(0, this.compatSettingScroll);
        this.settingScrollTarget = (float)this.settingScroll;
        this.settingScrollVisual = this.settingScrollTarget;
        Module clickGui = this.modules.getById("click_gui");

        if (clickGui != null)
        {
            this.selectedCategory = clickGui.getCategory();
            this.selectedModule = clickGui;
        }

        this.persistScrollState();
        this.rememberSelection();
    }

    private void exitSettingsCompatPage()
    {
        if (!this.isSettingsPage())
        {
            return;
        }

        this.compatSettingScroll = Math.max(0, Math.round(this.settingScrollTarget));
        this.contentPage = ContentPage.MODULES;
        this.waitingBind = false;
        this.clearInlineEditors(true);
        this.draggingSettingSlider = null;
        this.expandedChoiceSetting = null;
        this.activeColor = null;

        if (this.restoreCategory != null)
        {
            this.selectedCategory = this.restoreCategory;
        }

        if (this.restoreModuleId != null)
        {
            Module restore = this.modules.getById(this.restoreModuleId);

            if (restore != null)
            {
                this.selectedModule = restore;
            }
        }

        this.restoreCategory = null;
        this.restoreModuleId = null;
        this.ensureSelection();
        this.settingScroll = Math.max(0, this.moduleSettingScroll);
        this.settingScrollTarget = (float)this.settingScroll;
        this.settingScrollVisual = this.settingScrollTarget;
        this.persistScrollState();
        this.rememberSelection();
    }

    private void switchSettingsSection(SettingsSection section)
    {
        if (section == null || this.settingsSection == section)
        {
            return;
        }

        this.settingsSection = section;
        this.settingScroll = 0;
        this.settingScrollTarget = 0.0F;
        this.settingScrollVisual = 0.0F;
        this.compatSettingScroll = 0;
        this.draggingSettingSlider = null;
        this.expandedChoiceSetting = null;
        this.clearSliderTrackLock();
        this.clearInlineEditors(true);
        this.activeColor = null;
        this.persistScrollState();
    }

    private String settingsSectionLabel(SettingsSection section)
    {
        if (section == SettingsSection.ANIMATION)
        {
            return this.tr("client.settings.section.animation", "Animation");
        }

        return this.tr("client.settings.section.theme", "Theme");
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
                    boost = 1.95F;
                    break;
                case BACK:
                    boost = 1.20F;
                    break;
                case SWITCH:
                default:
                    boost = 1.55F;
                    break;
            }

            speed = UiMotion.clamp(speed * boost + 0.08F, 0.05F, 1.0F);
            smooth = UiMotion.clamp(smooth * (this.transitionMode == TransitionMode.BACK ? 0.95F : 0.88F), 0.0F, 1.0F);
            dt *= this.transitionMode == TransitionMode.BACK ? 1.12F : 1.25F;
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

    private void updateCategoryTransition(ClickGuiModule clickGui)
    {
        if (this.categoryTransition >= 0.999F)
        {
            this.categoryTransition = 1.0F;
            this.categoryTransitionNanos = System.nanoTime();
            return;
        }

        long now = System.nanoTime();
        float dt = this.categoryTransitionNanos == 0L ? (1.0F / 60.0F) : (float)((double)(now - this.categoryTransitionNanos) * 1.0E-9D);
        this.categoryTransitionNanos = now;
        float speed = clickGui == null ? 0.62F : UiMotion.clamp(clickGui.getGlobalAnimationSpeed() * 1.15F, 0.05F, 1.0F);
        float smooth = this.resolveAnimationProfile(clickGui).smooth();
        UiAnimation.Type type = clickGui == null ? UiAnimation.Type.EASE_OUT : clickGui.getGuiSwitchAnimationType();
        float response = UiAnimation.responseFromSpeed(speed, smooth, type, false);
        this.categoryTransition = UiAnimation.step(this.categoryTransition, 1.0F, response, dt, type, smooth);
    }

    private void updateSettingsPageProgress(ClickGuiModule clickGui)
    {
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);
        float target = this.isSettingsPage() ? 1.0F : 0.0F;
        float speed = this.resolvePageAnimationSpeed(clickGui, animProfile);
        this.settingsPageProgress = UiAnimationBus.animateWithSpeed("clickgui.page.settings", target, animProfile, speed);
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

        float k = UiMotion.clamp(baseRect.w / BASE_WINDOW_WIDTH, 0.35F, 1.85F);
        float shiftX = 0.0F;
        float shiftY = 0.0F;
        float scale = 0.95F + p * 0.05F;

        if (this.transitioningOut)
        {
            switch (this.transitionMode)
            {
                case BACK:
                    shiftX = scaled(28.0F, k) * inv;
                    shiftY = scaled(4.0F, k) * inv;
                    break;
                case CLOSE:
                    shiftY = scaled(18.0F, k) * inv;
                    break;
                case SWITCH:
                default:
                    shiftX = -scaled(12.0F, k) * inv;
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

    private NanoTheme resolveTheme(ClickGuiModule module)
    {
        if (module == null)
        {
            return NanoThemes.create(NanoPalette.COBALT, 228, 108, 10.0F, null);
        }

        Integer accent = null;

        if (module.isAccentOverrideEnabled() && module.getAccentOverride() != null)
        {
            accent = Integer.valueOf(module.getAccentOverride().toArgb());
        }

        int backdrop = module.isBackdropEnabled() ? module.getBackdropAlpha() : 0;
        float corner = UiMotion.clamp(module.getCornerRadius(), 6.0F, 26.0F);
        return NanoThemes.create(module.getPalette(), module.getPanelAlpha(), backdrop, corner, accent);
    }

    private void clampScroll(Layout l)
    {
        int catVisible = Math.max(1, (int)(l.sidebarRows.h / l.rowCategory));
        int moduleVisible = Math.max(1, (int)(l.moduleRows.h / l.rowModule));
        int settingVisible = Math.max(1, (int)(l.settingsRows.h / l.rowSetting));
        int catMax = Math.max(0, this.categoryEntries().size() - catVisible);
        int moduleMax = Math.max(0, this.currentModules().size() - moduleVisible);
        int settingMax = Math.max(0, this.visibleSettings(this.selectedModule).size() - settingVisible);
        this.categoryScroll = this.clamp(this.categoryScroll, catMax);
        this.moduleScroll = this.clamp(this.moduleScroll, moduleMax);
        this.settingScrollTarget = UiMotion.clamp(this.settingScrollTarget, 0.0F, (float)settingMax);
        this.settingScroll = this.clamp(Math.round(this.settingScrollTarget), settingMax);
        this.categoryScrollVisual = UiMotion.clamp(this.categoryScrollVisual, 0.0F, (float)catMax);
        this.moduleScrollVisual = UiMotion.clamp(this.moduleScrollVisual, 0.0F, (float)moduleMax);
        this.settingScrollVisual = UiMotion.clamp(this.settingScrollVisual, 0.0F, (float)settingMax);
        this.captureActiveSettingScroll();
    }

    private void updateScrollAnimation(Layout l, ClickGuiModule clickGui)
    {
        if (l == null)
        {
            return;
        }

        int catVisible = Math.max(1, (int)(l.sidebarRows.h / l.rowCategory));
        int moduleVisible = Math.max(1, (int)(l.moduleRows.h / l.rowModule));
        int settingVisible = Math.max(1, (int)(l.settingsRows.h / l.rowSetting));
        int catMax = Math.max(0, this.categoryEntries().size() - catVisible);
        int moduleMax = Math.max(0, this.currentModules().size() - moduleVisible);
        int settingMax = Math.max(0, this.visibleSettings(this.selectedModule).size() - settingVisible);
        this.categoryScroll = this.clamp(this.categoryScroll, catMax);
        this.moduleScroll = this.clamp(this.moduleScroll, moduleMax);
        this.settingScrollTarget = UiMotion.clamp(this.settingScrollTarget, 0.0F, (float)settingMax);
        this.settingScroll = this.clamp(Math.round(this.settingScrollTarget), settingMax);
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);

        if (!animProfile.isEnabled())
        {
            this.categoryScrollVisual = (float)this.categoryScroll;
            this.moduleScrollVisual = (float)this.moduleScroll;
            this.settingScrollVisual = this.settingScrollTarget;
            return;
        }

        float speed = UiAnimProfiles.scrollSpeed(animProfile);
        this.categoryScrollVisual = UiAnimationBus.animateWithSpeed("clickgui.scroll.category", (float)this.categoryScroll, animProfile, speed);
        this.moduleScrollVisual = UiAnimationBus.animateWithSpeed("clickgui.scroll.module", (float)this.moduleScroll, animProfile, speed);
        float settingDistance = Math.abs(this.settingScrollTarget - this.settingScrollVisual);
        float settingSpeed = UiMotion.clamp(speed + Math.min(0.38F, settingDistance * 0.16F), 0.05F, 1.0F);
        this.settingScrollVisual = this.draggingSettingSlider == null ? UiAnimationBus.animateWithSpeed("clickgui.scroll.setting", this.settingScrollTarget, animProfile, settingSpeed) : this.settingScrollTarget;
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

    private int clamp(int value, int max)
    {
        return Math.max(0, Math.min(max, value));
    }

    private float resolveSettingScrollStep(int wheelDelta)
    {
        float notches = Math.max(1.0F, Math.abs((float)wheelDelta) / 120.0F);
        return UiMotion.clamp(SETTING_SCROLL_STEP * notches, 0.45F, 2.4F);
    }

    private Layout layout()
    {
        Rect baseRect = this.window.isInitialized() ? new Rect(this.window.getX(), this.window.getY(), this.window.getWidth(), this.window.getHeight()) : this.fallbackWindow();
        Rect windowRect = this.transitionWindow(baseRect);
        float k = UiMotion.clamp(windowRect.w / BASE_WINDOW_WIDTH, 0.35F, 1.85F);
        float headerHeight = scaled(HEADER_HEIGHT, k);
        float outerPad = scaled(OUTER_PAD, k);
        float gapMajor = scaled(GAP_MAJOR, k);
        float topButtonH = scaled(20.0F, k);
        Rect header = new Rect(windowRect.x + scaled(1.0F, k), windowRect.y + scaled(1.0F, k), windowRect.w - scaled(2.0F, k), headerHeight - scaled(2.0F, k));
        Rect topHudEdit = new Rect(header.x2() - scaled(80.0F, k), header.y + scaled(8.0F, k), scaled(70.0F, k), topButtonH);
        Rect topUiScale = new Rect(topHudEdit.x - scaled(84.0F, k), header.y + scaled(8.0F, k), scaled(74.0F, k), topButtonH);
        Rect topClientSettings = new Rect(topUiScale.x - scaled(80.0F, k), header.y + scaled(8.0F, k), scaled(70.0F, k), topButtonH);
        Rect headerDrag = new Rect(header.x + scaled(2.0F, k), header.y + scaled(2.0F, k), topClientSettings.x - header.x - scaled(8.0F, k), header.h - scaled(4.0F, k));
        boolean settingsPage = this.isSettingsPage();
        float settingsBlend = UiMotion.clamp01(this.settingsPageProgress);

        float bodyY = windowRect.y + headerHeight + scaled(10.0F, k);
        float bodyH = windowRect.h - headerHeight - outerPad;
        float bodyCardH = bodyH - outerPad;
        float contentW = windowRect.w - outerPad * 2.0F - gapMajor;
        float leftColumnW = UiMotion.clamp(contentW * 0.24F, scaled(160.0F, k), Math.min(scaled(270.0F, k), contentW * 0.34F));
        float leftX = windowRect.x + outerPad;
        Rect sidebarModule = new Rect(leftX, bodyY, leftColumnW, bodyCardH);
        Rect sidebarRowsModule = new Rect(sidebarModule.x + scaled(10.0F, k), sidebarModule.y + scaled(32.0F, k), sidebarModule.w - scaled(20.0F, k), Math.max(scaled(24.0F, k), sidebarModule.h - scaled(42.0F, k)));
        float mainX = leftX + leftColumnW + gapMajor;
        float mainW = Math.max(scaled(170.0F, k), windowRect.x2() - mainX - outerPad);
        Rect mainModule = new Rect(mainX, bodyY, mainW, bodyCardH);
        boolean hasPickerPreview = this.activeColor != null;
        float minSettingsHeight = hasPickerPreview ? scaled(370.0F, k) : scaled(280.0F, k);
        float modulesBaseRatio = hasPickerPreview ? 0.26F : 0.31F;
        float maxModulesHeight = Math.max(scaled(138.0F, k), mainModule.h - minSettingsHeight - gapMajor);
        float modulesH = UiMotion.clamp(mainModule.h * modulesBaseRatio, scaled(96.0F, k), maxModulesHeight);
        Rect modulesCardModule = new Rect(mainModule.x, mainModule.y, mainModule.w, modulesH);
        Rect moduleRowsModule = new Rect(modulesCardModule.x + scaled(10.0F, k), modulesCardModule.y + scaled(34.0F, k), modulesCardModule.w - scaled(20.0F, k), modulesCardModule.h - scaled(44.0F, k));
        Rect settingsCardModule = new Rect(mainModule.x, modulesCardModule.y2() + gapMajor, mainModule.w, Math.max(scaled(120.0F, k), mainModule.y2() - modulesCardModule.y2() - gapMajor));

        float fullW = Math.max(scaled(220.0F, k), windowRect.w - outerPad * 2.0F);
        Rect sidebarSettings = new Rect(leftX, bodyY, 0.0F, 0.0F);
        Rect sidebarRowsSettings = new Rect(leftX, bodyY, 0.0F, 0.0F);
        Rect mainSettings = new Rect(leftX, bodyY, fullW, bodyCardH);
        Rect modulesCardSettings = new Rect(mainSettings.x, mainSettings.y, 0.0F, 0.0F);
        Rect moduleRowsSettings = new Rect(mainSettings.x, mainSettings.y, 0.0F, 0.0F);
        Rect settingsCardSettings = new Rect(mainSettings.x, mainSettings.y, mainSettings.w, mainSettings.h);

        Rect sidebar = this.lerpRect(sidebarModule, sidebarSettings, settingsBlend);
        Rect sidebarRows = this.lerpRect(sidebarRowsModule, sidebarRowsSettings, settingsBlend);
        Rect main = this.lerpRect(mainModule, mainSettings, settingsBlend);
        Rect modulesCard = this.lerpRect(modulesCardModule, modulesCardSettings, settingsBlend);
        Rect moduleRows = this.lerpRect(moduleRowsModule, moduleRowsSettings, settingsBlend);
        Rect settingsCard = this.lerpRect(settingsCardModule, settingsCardSettings, settingsBlend);

        boolean hasPicker = this.activeColor != null;

        float settingsHeadH = NanoScreenKit.lerp(scaled(36.0F, k), scaled(62.0F, k), settingsBlend);
        Rect settingsHead = new Rect(settingsCard.x + scaled(12.0F, k), settingsCard.y + scaled(12.0F, k), settingsCard.w - scaled(24.0F, k), settingsHeadH);
        Rect settingsThemeTab = new Rect(0.0F, 0.0F, 0.0F, 0.0F);
        Rect settingsAnimationTab = new Rect(0.0F, 0.0F, 0.0F, 0.0F);

        if (settingsBlend > 0.001F)
        {
            float tabGap = scaled(6.0F, k);
            float tabY = settingsHead.y + scaled(38.0F, k);
            float tabW = (settingsHead.w - tabGap) * 0.5F;
            settingsThemeTab = new Rect(settingsHead.x, tabY, tabW, scaled(BTN_HEIGHT, k));
            settingsAnimationTab = new Rect(settingsThemeTab.x2() + tabGap, tabY, tabW, settingsThemeTab.h);
        }

        float buttonY = settingsHead.y2() + scaled(8.0F, k);
        float buttonW = (settingsCard.w - scaled(24.0F, k) - scaled(4.0F, k)) * 0.5F;
        Rect btnToggle = new Rect(settingsCard.x + scaled(12.0F, k), buttonY, buttonW, scaled(BTN_HEIGHT, k));
        Rect btnBind = new Rect(btnToggle.x2() + scaled(4.0F, k), buttonY, buttonW, scaled(BTN_HEIGHT, k));
        float rowSetting = ROW_SETTING * k;

        Rect pickerCard = null;
        Rect pickerSv = null;
        Rect pickerHue = null;
        Rect pickerAlpha = null;
        Rect pickerPreview = null;
        float moduleRowsTop = buttonY + scaled(BTN_HEIGHT, k) + scaled(10.0F, k);
        float compatRowsTop = settingsHead.y2() + scaled(8.0F, k);
        float rowsTop = NanoScreenKit.lerp(moduleRowsTop, compatRowsTop, settingsBlend);
        Rect settingsRows = new Rect(settingsCard.x + scaled(12.0F, k), rowsTop, settingsCard.w - scaled(24.0F, k), Math.max(scaled(30.0F, k), settingsCard.y2() - rowsTop - scaled(10.0F, k)));
        Rect resizeHandle = new Rect(windowRect.x2() - scaled(14.0F, k), windowRect.y2() - scaled(14.0F, k), scaled(10.0F, k), scaled(10.0F, k));

        Rect pickerAnchor = null;

        if (hasPicker && this.selectedModule != null)
        {
            List<Setting<?>> settings = this.visibleSettings(this.selectedModule);
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

        if (hasPicker)
        {
            float pickerW = scaled(142.0F, k);
            float pickerH = scaled(118.0F, k);
            float anchorX = pickerAnchor != null ? pickerAnchor.x2() : windowRect.x2() - scaled(12.0F, k);
            float anchorY = pickerAnchor != null ? pickerAnchor.y : header.y2() + scaled(10.0F, k);
            float pickerX = Math.min(windowRect.x2() - scaled(8.0F, k) - pickerW, Math.max(windowRect.x + scaled(8.0F, k), anchorX - pickerW + scaled(12.0F, k)));
            float desiredY = anchorY - pickerH - scaled(6.0F, k);
            float minY = header.y2() + scaled(6.0F, k);
            float pickerY = desiredY >= minY ? desiredY : anchorY + (pickerAnchor != null ? pickerAnchor.h : 0.0F) + scaled(8.0F, k);
            pickerY = Math.min(Math.max(minY, pickerY), windowRect.y2() - pickerH - scaled(10.0F, k));
            pickerCard = new Rect(pickerX, pickerY, pickerW, pickerH);
            float svWidth = UiMotion.clamp(pickerW * 0.46F, scaled(74.0F, k), scaled(108.0F, k));
            float svHeight = Math.max(scaled(50.0F, k), pickerH - scaled(36.0F, k));
            pickerSv = new Rect(pickerCard.x + scaled(8.0F, k), pickerCard.y + scaled(12.0F, k), svWidth, svHeight);
            pickerHue = new Rect(pickerSv.x2() + scaled(6.0F, k), pickerSv.y, scaled(7.0F, k), pickerSv.h);
            pickerAlpha = new Rect(pickerSv.x, pickerSv.y2() + scaled(6.0F, k), pickerSv.w + scaled(13.0F, k), scaled(7.0F, k));
            pickerPreview = new Rect(pickerCard.x2() - scaled(44.0F, k), pickerCard.y + scaled(8.0F, k), scaled(38.0F, k), scaled(20.0F, k));
        }

        return new Layout(windowRect, header, headerDrag, topHudEdit, topUiScale, topClientSettings, sidebar, sidebarRows, main, modulesCard, moduleRows, settingsCard, settingsHead, settingsThemeTab, settingsAnimationTab, btnToggle, btnBind, settingsRows, resizeHandle, pickerCard, pickerSv, pickerHue, pickerAlpha, pickerPreview, hasPicker, k);
    }

    private Rect fallbackWindow()
    {
        float width = Math.min(Math.max(MIN_WINDOW_WIDTH, BASE_WINDOW_WIDTH), (float)this.width - 16.0F);
        float height = Math.min(Math.max(MIN_WINDOW_HEIGHT, BASE_WINDOW_HEIGHT), (float)this.height - 16.0F);
        return new Rect(((float)this.width - width) * 0.5F, ((float)this.height - height) * 0.5F, width, height);
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

    private Rect lerpRect(Rect from, Rect to, float t)
    {
        if (from == null && to == null)
        {
            return new Rect(0.0F, 0.0F, 0.0F, 0.0F);
        }

        if (from == null)
        {
            return to;
        }

        if (to == null)
        {
            return from;
        }

        return new Rect(
            NanoScreenKit.lerp(from.x, to.x, t),
            NanoScreenKit.lerp(from.y, to.y, t),
            NanoScreenKit.lerp(from.w, to.w, t),
            NanoScreenKit.lerp(from.h, to.h, t)
        );
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

    private final class ChoicePopupData
    {
        private final Setting<?> setting;
        private final Rect valueRect;
        private final Rect popupRect;
        private final float optionHeight;
        private final float scale;
        private final List<String> options;

        private ChoicePopupData(Setting<?> setting, Rect valueRect, Rect popupRect, float optionHeight, float scale, List<String> options)
        {
            this.setting = setting;
            this.valueRect = valueRect;
            this.popupRect = popupRect;
            this.optionHeight = Math.max(1.0F, optionHeight);
            this.scale = UiMotion.clamp(scale, 0.35F, 1.85F);
            this.options = options == null ? Collections.<String>emptyList() : options;
        }

        private Rect optionRect(int index)
        {
            float y = this.popupRect.y + scaled(3.0F, this.scale) + this.optionHeight * (float)Math.max(0, index);
            return new Rect(this.popupRect.x + scaled(2.0F, this.scale), y, this.popupRect.w - scaled(4.0F, this.scale), this.optionHeight);
        }

        private int optionIndex(int mouseY)
        {
            float local = (float)mouseY - this.popupRect.y - scaled(3.0F, this.scale);

            if (local < 0.0F)
            {
                return -1;
            }

            return (int)(local / this.optionHeight);
        }

        private boolean isSelected(String option)
        {
            if (this.setting instanceof EnumSetting)
            {
                Object current = this.setting.get();
                return current != null && option != null && option.equalsIgnoreCase(String.valueOf(current));
            }

            if (this.setting instanceof MultiSelectSetting)
            {
                return ((MultiSelectSetting)this.setting).isSelected(option);
            }

            return false;
        }

        private String optionLabel(String option)
        {
            return this.setting.getDisplayOption(ClickGuiScreen.this.selectedModuleId(), option);
        }
    }

    private static final class CategoryEntry
    {
        private final Category category;
        private final int count;

        private CategoryEntry(Category category, int count)
        {
            this.category = category;
            this.count = count;
        }
    }

    private static final class Layout
    {
        private final Rect window;
        private final Rect header;
        private final Rect headerDrag;
        private final Rect topHudEdit;
        private final Rect topUiScale;
        private final Rect topClientSettings;
        private final Rect sidebar;
        private final Rect sidebarRows;
        private final Rect main;
        private final Rect modulesCard;
        private final Rect moduleRows;
        private final Rect settingsCard;
        private final Rect settingsHead;
        private final Rect settingsThemeTab;
        private final Rect settingsAnimationTab;
        private final Rect btnToggle;
        private final Rect btnBind;
        private final Rect settingsRows;
        private final Rect resizeHandle;
        private final Rect pickerCard;
        private final Rect pickerSv;
        private final Rect pickerHue;
        private final Rect pickerAlpha;
        private final Rect pickerPreview;
        private final boolean hasPicker;
        private final float scale;
        private final float rowCategory;
        private final float rowModule;
        private final float rowSetting;

        private Layout(Rect window, Rect header, Rect headerDrag, Rect topHudEdit, Rect topUiScale, Rect topClientSettings, Rect sidebar, Rect sidebarRows, Rect main, Rect modulesCard, Rect moduleRows, Rect settingsCard, Rect settingsHead, Rect settingsThemeTab, Rect settingsAnimationTab, Rect btnToggle, Rect btnBind, Rect settingsRows, Rect resizeHandle, Rect pickerCard, Rect pickerSv, Rect pickerHue, Rect pickerAlpha, Rect pickerPreview, boolean hasPicker, float scale)
        {
            this.window = window;
            this.header = header;
            this.headerDrag = headerDrag;
            this.topHudEdit = topHudEdit;
            this.topUiScale = topUiScale;
            this.topClientSettings = topClientSettings;
            this.sidebar = sidebar;
            this.sidebarRows = sidebarRows;
            this.main = main;
            this.modulesCard = modulesCard;
            this.moduleRows = moduleRows;
            this.settingsCard = settingsCard;
            this.settingsHead = settingsHead;
            this.settingsThemeTab = settingsThemeTab;
            this.settingsAnimationTab = settingsAnimationTab;
            this.btnToggle = btnToggle;
            this.btnBind = btnBind;
            this.settingsRows = settingsRows;
            this.resizeHandle = resizeHandle;
            this.pickerCard = pickerCard;
            this.pickerSv = pickerSv;
            this.pickerHue = pickerHue;
            this.pickerAlpha = pickerAlpha;
            this.pickerPreview = pickerPreview;
            this.hasPicker = hasPicker;
            this.scale = UiMotion.clamp(scale, 0.35F, 1.85F);
            this.rowCategory = ROW_CATEGORY * this.scale;
            this.rowModule = ROW_MODULE * this.scale;
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
