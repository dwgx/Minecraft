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
import client.setting.NumberSetting;
import client.setting.Setting;
import client.setting.StringSetting;
import client.ui.template.NanoTextInput;
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

public final class ClickGuiScreen extends GuiScreen implements NanoRenderableScreen
{
    private enum TransitionMode
    {
        NONE,
        CLOSE,
        SWITCH,
        BACK
    }

    private static final float BASE_WINDOW_WIDTH = 980.0F;
    private static final float BASE_WINDOW_HEIGHT = 620.0F;
    private static final float MIN_WINDOW_WIDTH = 260.0F;
    private static final float MIN_WINDOW_HEIGHT = 170.0F;
    private static final float SCREEN_MARGIN = 8.0F;
    private static final float HEADER_HEIGHT = 36.0F;
    private static final float OUTER_PAD = 12.0F;
    private static final float GAP_MAJOR = 14.0F;
    private static final float ROW_CATEGORY = 24.0F;
    private static final float ROW_MODULE = 34.0F;
    private static final float ROW_SETTING = 24.0F;
    private static final float BTN_HEIGHT = 20.0F;
    private static final float RADIUS_WINDOW = 9.0F;
    private static final float RADIUS_PANEL = 8.0F;
    private static final float RADIUS_ROW = 6.0F;
    private static final float RADIUS_CONTROL = 6.0F;
    private static final float VALUE_COL_WIDTH = 88.0F;
    private static final float RESET_COL_WIDTH = 34.0F;

    private final ModuleManager modules;
    private final UiWindowState window = new UiWindowState(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT);

    private Category selectedCategory;
    private Module selectedModule;
    private boolean waitingBind;
    private int categoryScroll;
    private int moduleScroll;
    private int settingScroll;
    private float categoryScrollVisual;
    private float moduleScrollVisual;
    private float settingScrollVisual;
    private int mouseX;
    private int mouseY;
    private long lastNanoAt;
    private long lastNanoVg;

    private ColorSetting activeColor;
    private StringSetting activeTextSetting;
    private Setting<?> activeNumberSetting;
    private final StringSetting numberInputBuffer = new StringSetting("__clickgui_number_input_buffer", "Number Input", "Inline numeric input buffer", "", 40);
    private final NanoTextInput textInput = new NanoTextInput();
    private boolean draggingSv;
    private boolean draggingHue;
    private boolean draggingAlpha;
    private Setting<?> draggingSettingSlider;
    private long lastSliderDragNanos;
    private float pickerHue;
    private float pickerSat;
    private float pickerVal;
    private float pickerAlpha;
    private long lastPickerCommitNanos;
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
        this.categoryScrollVisual = (float)this.categoryScroll;
        this.moduleScrollVisual = (float)this.moduleScroll;
        this.settingScrollVisual = (float)this.settingScroll;
        this.draggingSettingSlider = null;
        this.clearInlineEditors(false);
        this.lastSliderDragNanos = 0L;
        this.lastPickerCommitNanos = 0L;
    }

    public void onGuiClosed()
    {
        this.waitingBind = false;
        this.draggingSv = false;
        this.draggingHue = false;
        this.draggingAlpha = false;
        this.draggingSettingSlider = null;
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
            if (this.textInput.handleKeyTyped(typedChar, keyCode, this.activeTextSetting))
            {
                if (!this.textInput.isFocused())
                {
                    this.activeTextSetting = null;
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
            if (keyCode == 1)
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

        if (keyCode == 1)
        {
            this.requestTransition(TransitionMode.CLOSE, null);
            return;
        }

        if (this.selectedModule != null && (keyCode == 57 || keyCode == 28))
        {
            this.selectedModule.toggle();
            return;
        }

        if (this.selectedModule != null && keyCode == 19)
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
                this.openClientSettings();
                return;
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

        if (l.sidebarRows.contains(mouseX, mouseY))
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
                this.moduleScrollVisual = 0.0F;
                this.settingScrollVisual = 0.0F;
                this.draggingSettingSlider = null;
                this.clearInlineEditors(true);
                this.ensureSelection();
                this.validateActiveColor();
                this.rememberSelection();
            }

            return;
        }

        if (l.moduleRows.contains(mouseX, mouseY))
        {
            int scrollBase = (int)Math.floor((double)this.moduleScrollVisual);
            float scrollOffset = (this.moduleScrollVisual - (float)scrollBase) * l.rowModule;
            int index = scrollBase + (int)(((float)mouseY - l.moduleRows.y + scrollOffset) / l.rowModule);
            List<Module> list = this.currentModules();

            if (index >= 0 && index < list.size())
            {
                this.selectedModule = list.get(index);
                this.settingScroll = 0;
                this.settingScrollVisual = 0.0F;
                this.draggingSettingSlider = null;
                this.clearInlineEditors(true);
                this.validateActiveColor();
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
            if (l.btnToggle.contains(mouseX, mouseY))
            {
                this.selectedModule.toggle();
                return;
            }

            if (l.btnBind.contains(mouseX, mouseY))
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
                            this.commitActiveNumberInput();
                            this.activeNumberSetting = null;
                            this.activeTextSetting = (StringSetting)setting;
                            this.activateTextInput(inputRect, l.scale, mouseX, mouseY, this.activeTextSetting);
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
            this.draggingSv = false;
            this.draggingHue = false;
            this.draggingAlpha = false;
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

        if (this.window.isInteracting())
        {
            this.window.updateInteraction((float)mouseX, (float)mouseY, (float)this.width, (float)this.height, SCREEN_MARGIN);
            this.syncScaleAndAnchorFromWindow(this.resolveUiScaleModule());
        }

        Layout l = this.layout();

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

        if (this.draggingSettingSlider != null)
        {
            this.updateDraggedSettingSlider(l, mouseX);
        }
    }

    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        if (this.draggingSv || this.draggingHue || this.draggingAlpha)
        {
            this.commitPickerColor(true);
        }

        this.window.endInteraction();
        this.draggingSv = false;
        this.draggingHue = false;
        this.draggingAlpha = false;
        this.draggingSettingSlider = null;
        this.textInput.onMouseUp();
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
        int delta = wheel > 0 ? -1 : 1;
        Layout l = this.layout();

        if (l.sidebarRows.contains(x, y))
        {
            this.categoryScroll += delta;
        }
        else if (l.moduleRows.contains(x, y))
        {
            this.moduleScroll += delta;
        }
        else if (l.settingsRows.contains(x, y))
        {
            this.settingScroll += delta;
        }

        this.clampScroll(l);
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
        this.ensureSelection();
        this.validateActiveColor();
        this.validateActiveTextSetting();
        this.validateActiveNumberSetting();
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        this.updateTransition(clickGui);
        this.updateCategoryTransition(clickGui);

        if (this.transitioningOut && this.transitionExecuted)
        {
            return;
        }

        UiScaleEditModule uiScale = this.resolveUiScaleModule();
        this.syncWindowTarget(uiScale);

        if (this.window.isInteracting())
        {
            this.window.updateInteraction((float)this.mouseX, (float)this.mouseY, (float)this.width, (float)this.height, SCREEN_MARGIN);
            this.syncScaleAndAnchorFromWindow(uiScale);
        }

        this.window.tick(this.resolveAnimationSpeed(clickGui, uiScale), this.resolveAnimationType(clickGui), this.resolveAnimationSmooth(clickGui), this.resolveAnimationEnabled(clickGui));
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
            this.drawSidebar(vg, stack, l, theme, regular, bold);
            this.drawModules(vg, stack, l, theme, regular, bold);
            this.drawSettings(vg, stack, l, theme, regular, bold);
            this.drawResizeHandle(vg, stack, l, theme);
            context.getNanoVG().resetScissor();
        }
    }

    private void drawTopBar(long vg, MemoryStack stack, Layout l, NanoTheme theme, int regular, int bold)
    {
        NanoUi.drawLeftText(vg, stack, bold, l.header.x + scaled(12.0F, l.scale), l.header.y + l.header.h * 0.5F, scaled(16.0F, l.scale), theme.textArgb(), this.tr("clickgui.title.client", "Client"));
        NanoUi.drawLeftText(vg, stack, regular, l.header.x + scaled(140.0F, l.scale), l.header.y + l.header.h * 0.5F + scaled(0.5F, l.scale), scaled(11.0F, l.scale), theme.textWeakArgb(), this.tr("ui.powered_by", "Powered by DWGX"));
        this.drawTopButton(vg, stack, l.topClientSettings, this.tr("clickgui.top.setting", "Setting"), regular, theme);
        this.drawTopButton(vg, stack, l.topUiScale, this.tr("clickgui.top.uiscale", "UIScale"), regular, theme);
        this.drawTopButton(vg, stack, l.topHudEdit, this.tr("clickgui.top.hudedit", "HudEdit"), regular, theme);
    }

    private void drawTopButton(long vg, MemoryStack stack, Rect button, String label, int font, NanoTheme theme)
    {
        boolean hovered = button.contains(this.mouseX, this.mouseY);
        int fill = hovered ? theme.controlHoverArgb() : theme.controlArgb();
        NanoUi.drawSurface(vg, stack, button.x, button.y, button.w, button.h, this.stableControlRadius(button.h / BTN_HEIGHT), fill, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 80));
        NanoUi.drawCenterText(vg, stack, font, button.x + button.w * 0.5F, button.y + button.h * 0.5F, scaled(11.0F, UiMotion.clamp(button.h / 20.0F, 0.35F, 1.85F)), theme.textArgb(), label);
    }

    private void drawSidebar(long vg, MemoryStack stack, Layout l, NanoTheme theme, int regular, int bold)
    {
        NanoUi.drawSurface(vg, stack, l.sidebar.x, l.sidebar.y, l.sidebar.w, l.sidebar.h, this.stablePanelRadius(l.scale), theme.sidebarArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 56));
        NanoUi.drawLeftText(vg, stack, bold, l.sidebar.x + scaled(12.0F, l.scale), l.sidebar.y + scaled(14.0F, l.scale), scaled(14.0F, l.scale), theme.textArgb(), this.tr("clickgui.sidebar.categories", "Categories"));

        List<CategoryEntry> entries = this.categoryEntries();
        int visible = Math.max(0, (int)Math.ceil((double)(l.sidebarRows.h / l.rowCategory)) + 2);
        int scrollBase = (int)Math.floor((double)this.categoryScrollVisual);
        float scrollOffset = (this.categoryScrollVisual - (float)scrollBase) * l.rowCategory;
        NanoUi.beginClip(vg, l.sidebarRows.x, l.sidebarRows.y, l.sidebarRows.w, l.sidebarRows.h);
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        float rowRadius = this.stableRowRadius(l.scale);

        for (int i = 0; i < visible; ++i)
        {
            int idx = scrollBase + i;

            if (idx >= entries.size())
            {
                break;
            }

            CategoryEntry entry = entries.get(idx);
            Rect row = new Rect(l.sidebarRows.x, l.sidebarRows.y + l.rowCategory * (float)i - scrollOffset, l.sidebarRows.w, Math.max(6.0F, l.rowCategory - 1.0F));
            boolean hovered = row.contains(this.mouseX, this.mouseY);
            boolean selected = this.selectedCategory == entry.category;
            float hoverRatio = UiAnimationBus.animate("clickgui.category.hover." + entry.category.name(), hovered ? 1.0F : 0.0F, this.resolveControlAnimationSpeed(clickGui), this.resolveAnimationSmooth(clickGui), this.resolveAnimationType(clickGui), this.resolveAnimationEnabled(clickGui));
            float selectRatio = UiAnimationBus.animate("clickgui.category.select." + entry.category.name(), selected ? 1.0F : 0.0F, this.resolveControlAnimationSpeed(clickGui), this.resolveAnimationSmooth(clickGui), this.resolveAnimationType(clickGui), this.resolveAnimationEnabled(clickGui));
            int base = i % 2 == 0 ? theme.rowArgb() : theme.cardAltArgb();
            NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, base, 0);

            if (hoverRatio > 0.001F)
            {
                NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, NanoRenderUtils.mulAlpha(theme.rowHoverArgb(), UiMotion.clamp01(hoverRatio * 0.8F)), 0);
            }

            if (selectRatio > 0.001F)
            {
                NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, NanoRenderUtils.mulAlpha(theme.rowSelectedArgb(), selectRatio), 0);
            }

            float dotRatio = UiMotion.clamp01(Math.max(selectRatio, hoverRatio * 0.55F));
            float dotSize = scaled(4.0F, l.scale) + scaled(1.5F, l.scale) * dotRatio;
            float dotX = row.x + scaled(8.0F, l.scale);
            float dotY = row.y + row.h * 0.5F - dotSize * 0.5F;
            int dotColor = selected ? theme.accentArgb() : NanoRenderUtils.mulAlpha(theme.textWeakArgb(), UiMotion.clamp(0.45F + hoverRatio * 0.25F, 0.0F, 1.0F));
            NanoUi.drawSurface(vg, stack, dotX, dotY, dotSize, dotSize, dotSize * 0.5F, dotColor, 0);

            int nameColor = selected ? theme.textArgb() : this.mixArgb(theme.textWeakArgb(), theme.textArgb(), UiMotion.clamp01(hoverRatio * 0.7F));
            NanoUi.drawLeftText(vg, stack, regular, row.x + scaled(18.0F, l.scale), row.y + row.h * 0.5F, scaled(12.5F, l.scale), nameColor, this.categoryDisplayName(entry.category));
            NanoUi.drawRightText(vg, stack, regular, row.x2() - scaled(8.0F, l.scale), row.y + row.h * 0.5F, scaled(10.5F, l.scale), theme.textWeakArgb(), Integer.toString(entry.count));
        }

        NanoUi.endClip(vg);
    }

    private void drawModules(long vg, MemoryStack stack, Layout l, NanoTheme theme, int regular, int bold)
    {
        NanoUi.drawSurface(vg, stack, l.modulesCard.x, l.modulesCard.y, l.modulesCard.w, l.modulesCard.h, this.stablePanelRadius(l.scale), theme.cardArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 58));
        List<Module> list = this.currentModules();
        NanoUi.drawLeftText(vg, stack, bold, l.modulesCard.x + scaled(12.0F, l.scale), l.modulesCard.y + scaled(14.0F, l.scale), scaled(15.0F, l.scale), theme.textArgb(), this.tr("clickgui.modules.title", "Modules ({0})", Integer.valueOf(list.size())));
        String categoryLabel = this.categoryDisplayName(this.selectedCategory);
        NanoUi.drawRightText(vg, stack, regular, l.modulesCard.x2() - scaled(12.0F, l.scale), l.modulesCard.y + scaled(14.0F, l.scale), scaled(10.5F, l.scale), theme.textWeakArgb(), categoryLabel);

        ClickGuiModule clickGui = this.resolveClickGuiModule();
        float listSlide = (1.0F - this.categoryTransition) * scaled(12.0F, l.scale) * (float)this.categoryTransitionDir;
        float listAlpha = UiMotion.clamp(0.35F + this.categoryTransition * 0.65F, 0.0F, 1.0F);
        float rowRadius = this.stableRowRadius(l.scale);
        int visible = Math.max(0, (int)Math.ceil((double)(l.moduleRows.h / l.rowModule)) + 2);
        int scrollBase = (int)Math.floor((double)this.moduleScrollVisual);
        float scrollOffset = (this.moduleScrollVisual - (float)scrollBase) * l.rowModule;
        NanoUi.beginClip(vg, l.moduleRows.x, l.moduleRows.y, l.moduleRows.w, l.moduleRows.h);

        for (int i = 0; i < visible; ++i)
        {
            int idx = scrollBase + i;

            if (idx >= list.size())
            {
                break;
            }

            Module module = list.get(idx);
            Rect row = new Rect(l.moduleRows.x + listSlide, l.moduleRows.y + l.rowModule * (float)i - scrollOffset, l.moduleRows.w, Math.max(10.0F, l.rowModule - 3.0F));
            boolean hovered = row.contains(this.mouseX, this.mouseY);
            boolean selected = this.selectedModule == module;
            float hoverRatio = UiAnimationBus.animate("clickgui.module.hover." + module.getId(), hovered ? 1.0F : 0.0F, this.resolveControlAnimationSpeed(clickGui), this.resolveAnimationSmooth(clickGui), this.resolveAnimationType(clickGui), this.resolveAnimationEnabled(clickGui));
            float selectRatio = UiAnimationBus.animate("clickgui.module.select." + module.getId(), selected ? 1.0F : 0.0F, this.resolveControlAnimationSpeed(clickGui), this.resolveAnimationSmooth(clickGui), this.resolveAnimationType(clickGui), this.resolveAnimationEnabled(clickGui));
            int base = i % 2 == 0 ? theme.rowArgb() : theme.cardAltArgb();
            NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, NanoRenderUtils.mulAlpha(base, listAlpha), 0);

            if (hoverRatio > 0.001F)
            {
                NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, NanoRenderUtils.mulAlpha(theme.rowHoverArgb(), UiMotion.clamp01(hoverRatio * 0.78F) * listAlpha), 0);
            }

            if (selectRatio > 0.001F)
            {
                NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, NanoRenderUtils.mulAlpha(theme.rowSelectedArgb(), selectRatio * listAlpha), 0);
            }

            float accentSize = scaled(3.0F, l.scale) + scaled(1.6F, l.scale) * UiMotion.clamp01(Math.max(selectRatio, hoverRatio * 0.4F));
            float accentX = row.x + scaled(8.0F, l.scale);
            float accentY = row.y + row.h * 0.5F - accentSize * 0.5F;
            int accentColor = module.isEnabled() ? theme.accentArgb() : NanoRenderUtils.mulAlpha(theme.textWeakArgb(), UiMotion.clamp(0.38F + hoverRatio * 0.2F, 0.0F, 1.0F));
            NanoUi.drawSurface(vg, stack, accentX, accentY, accentSize, accentSize, accentSize * 0.5F, NanoRenderUtils.mulAlpha(accentColor, listAlpha), 0);
            int moduleNameColor = selected ? theme.textArgb() : this.mixArgb(theme.textWeakArgb(), theme.textArgb(), UiMotion.clamp01(hoverRatio * 0.6F));
            String moduleDisplayName = module.getDisplayName();
            NanoUi.drawLeftText(vg, stack, bold, row.x + scaled(16.0F, l.scale), row.y + scaled(12.0F, l.scale), scaled(14.5F, l.scale), NanoRenderUtils.mulAlpha(moduleNameColor, listAlpha), this.waitingBind && selected ? this.tr("clickgui.module.bind_waiting", "[bind] {0}", moduleDisplayName) : moduleDisplayName);
            NanoUi.drawLeftText(vg, stack, regular, row.x + scaled(16.0F, l.scale), row.y + scaled(24.0F, l.scale), scaled(10.0F, l.scale), NanoRenderUtils.mulAlpha(theme.textWeakArgb(), listAlpha), this.categoryDisplayName(module.getCategory()));
            this.drawModuleStatus(vg, stack, row, module, theme, l.scale, listAlpha, regular);
        }

        NanoUi.endClip(vg);
    }
    private void drawSettings(long vg, MemoryStack stack, Layout l, NanoTheme theme, int regular, int bold)
    {
        NanoUi.drawSurface(vg, stack, l.settingsCard.x, l.settingsCard.y, l.settingsCard.w, l.settingsCard.h, this.stablePanelRadius(l.scale), theme.cardAltArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 50));

        if (this.selectedModule == null)
        {
            NanoUi.drawCenterText(vg, stack, regular, l.settingsCard.x + l.settingsCard.w * 0.5F, l.settingsCard.y + l.settingsCard.h * 0.5F, scaled(13.0F, l.scale), theme.textWeakArgb(), this.tr("clickgui.settings.none_selected", "No module selected"));
            return;
        }

        NanoUi.drawLeftText(vg, stack, bold, l.settingsHead.x, l.settingsHead.y + scaled(7.0F, l.scale), scaled(16.0F, l.scale), theme.textArgb(), this.tr("clickgui.settings.title", "{0} Settings", this.selectedModule.getDisplayName()));
        NanoUi.drawLeftText(vg, stack, regular, l.settingsHead.x, l.settingsHead.y + scaled(22.0F, l.scale), scaled(10.5F, l.scale), theme.textWeakArgb(), this.selectedModule.getId() + " / " + this.categoryDisplayName(this.selectedModule.getCategory()));
        this.drawToggleButton(vg, stack, l.btnToggle, this.selectedModule.isEnabled(), regular, theme);
        this.drawButton(vg, stack, l.btnBind, this.tr("clickgui.bind.key_label", "Key: {0}", this.moduleBindLabel(this.selectedModule)), this.waitingBind, regular, theme);

        if (l.hasPicker)
        {
            this.drawColorPicker(vg, stack, l, theme, regular);
        }

        List<Setting<?>> settings = this.visibleSettings(this.selectedModule);

        if (settings.isEmpty() && this.isClickGuiModuleSelected())
        {
            NanoUi.drawCenterText(vg, stack, regular, l.settingsRows.x + l.settingsRows.w * 0.5F, l.settingsRows.y + l.settingsRows.h * 0.5F, scaled(10.8F, l.scale), theme.textWeakArgb(), this.tr("clickgui.settings.global_hint", "Global client options moved to top 'Setting' page"));
            return;
        }

        int visible = Math.max(0, (int)Math.ceil((double)(l.settingsRows.h / l.rowSetting)) + 2);
        int scrollBase = (int)Math.floor((double)this.settingScrollVisual);
        float scrollOffset = (this.settingScrollVisual - (float)scrollBase) * l.rowSetting;
        NanoUi.beginClip(vg, l.settingsRows.x, l.settingsRows.y, l.settingsRows.w, l.settingsRows.h);
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        float rowRadius = this.stableRowRadius(l.scale);

        for (int i = 0; i < visible; ++i)
        {
            int idx = scrollBase + i;

            if (idx >= settings.size())
            {
                break;
            }

            Setting<?> setting = settings.get(idx);
            Rect row = new Rect(l.settingsRows.x, l.settingsRows.y + l.rowSetting * (float)i - scrollOffset, l.settingsRows.w, Math.max(6.0F, l.rowSetting - 1.0F));
            boolean hovered = row.contains(this.mouseX, this.mouseY);
            boolean modified = this.isSettingModified(setting);
            boolean animationSetting = this.isAnimationSetting(setting);
            boolean prevAnimationSetting = idx > 0 && this.isAnimationSetting(settings.get(idx - 1));
            Rect valueRect = this.settingValueRect(row, l.scale);
            Rect resetRect = this.settingResetRect(row, l.scale);

            if (animationSetting && !prevAnimationSetting)
            {
                NanoUi.drawLeftText(vg, stack, regular, row.x + scaled(2.0F, l.scale), row.y - scaled(7.0F, l.scale), scaled(9.0F, l.scale), theme.textWeakArgb(), this.tr("clickgui.settings.group.animation", "Animation"));
            }

            int base = i % 2 == 0 ? theme.rowArgb() : theme.cardAltArgb();
            NanoUi.drawSurface(vg, stack, row.x, row.y, row.w, row.h, rowRadius, base, 0);
            float hoverRatio = UiAnimationBus.animate("clickgui.setting.hover." + this.selectedModule.getId() + "." + setting.getKey(), hovered ? 1.0F : 0.0F, this.resolveControlAnimationSpeed(clickGui), this.resolveAnimationSmooth(clickGui), this.resolveAnimationType(clickGui), this.resolveAnimationEnabled(clickGui));

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

            NanoUi.drawLeftText(vg, stack, regular, nameX, row.y + row.h * 0.5F, scaled(11.5F, l.scale), theme.textArgb(), this.settingDisplayName(setting));
            this.drawResetButton(vg, stack, resetRect, modified, regular, theme);

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
            else
            {
                NanoUi.drawRightText(vg, stack, regular, valueRect.x2() - scaled(3.0F, l.scale), row.y + row.h * 0.5F, scaled(10.5F, l.scale), theme.textMutedArgb(), this.settingValue(setting));
            }
        }

        NanoUi.endClip(vg);
    }

    private void drawButton(long vg, MemoryStack stack, Rect rect, String label, boolean active, int font, NanoTheme theme)
    {
        boolean hovered = rect.contains(this.mouseX, this.mouseY);
        int fill = active ? theme.controlActiveArgb() : (hovered ? theme.controlHoverArgb() : theme.controlArgb());
        NanoUi.drawSurface(vg, stack, rect.x, rect.y, rect.w, rect.h, this.stableControlRadius(rect.h / BTN_HEIGHT), fill, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 84));
        float k = UiMotion.clamp(rect.h / BTN_HEIGHT, 0.35F, 1.85F);
        NanoUi.drawCenterText(vg, stack, font, rect.x + rect.w * 0.5F, rect.y + rect.h * 0.5F + scaled(0.5F, k), scaled(11.0F, k), theme.textArgb(), label);
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
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        float targetRatio = enabled ? 1.0F : 0.0F;
        float ratio = UiAnimationBus.animate(animKey, targetRatio, this.resolveControlAnimationSpeed(clickGui), this.resolveAnimationSmooth(clickGui), this.resolveAnimationType(clickGui), this.resolveAnimationEnabled(clickGui));
        float focus = UiAnimationBus.animate(animKey + ".focus", hovered ? 1.0F : 0.0F, this.resolveControlAnimationSpeed(clickGui), this.resolveAnimationSmooth(clickGui), this.resolveAnimationType(clickGui), this.resolveAnimationEnabled(clickGui));
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
        float textCenterY = py + pillH * 0.5F + scaled(0.9F, k);
        NanoUi.drawCenterText(vg, stack, font, px + pillW * 0.25F, textCenterY, scaled(8.2F, k), disableText, this.tr("clickgui.state.disable", "DISABLE"));
        NanoUi.drawCenterText(vg, stack, font, px + pillW * 0.75F, textCenterY, scaled(8.2F, k), enableText, this.tr("clickgui.state.enable", "ENABLE"));
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
        int badgeFill = hasBind ? this.mixArgb(theme.controlArgb(), theme.controlHoverArgb(), 0.38F) : (enabled ? theme.controlActiveArgb() : theme.controlArgb());
        int textColor = enabled ? theme.textArgb() : this.mixArgb(theme.textMutedArgb(), theme.textWeakArgb(), hasBind ? 0.38F : 0.10F);
        NanoUi.drawSurface(vg, stack, badgeX, badgeY, badgeW, badgeH, badgeH * 0.5F, NanoRenderUtils.mulAlpha(badgeFill, alpha), NanoRenderUtils.mulAlpha(theme.windowBorderArgb(), alpha * 0.45F));
        NanoUi.drawCenterText(vg, stack, font, badgeX + badgeW * 0.5F, badgeY + badgeH * 0.5F, scaled(9.8F, scale), NanoRenderUtils.mulAlpha(textColor, alpha), status);
    }

    private void drawSettingSlider(long vg, MemoryStack stack, Rect row, Setting<?> setting, boolean hovered, NanoTheme theme, float scale, int font)
    {
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        Rect track = this.settingSliderTrackRect(row, k);
        Rect valueRect = this.settingValueRect(row, k);
        float ratio = this.settingSliderRatio(setting);
        String key = "clickgui.setting.slider." + (this.selectedModule == null ? "none" : this.selectedModule.getId()) + "." + setting.getKey();
        boolean dragging = this.draggingSettingSlider == setting;
        float dragRatio = UiMotion.clamp01(((float)this.mouseX - track.x) / Math.max(1.0F, track.w));
        float visualTarget = dragging ? dragRatio : ratio;
        boolean snap = dragging || (System.nanoTime() - this.lastSliderDragNanos < 150_000_000L);
        float animatedRatio = snap ? visualTarget : UiAnimationBus.animate(key, visualTarget, this.resolveSliderAnimationSpeed(clickGui), this.resolveAnimationSmooth(clickGui), this.resolveAnimationType(clickGui), this.resolveAnimationEnabled(clickGui));
        float displayRatio = animatedRatio;
        float focus = UiAnimationBus.animate(key + ".focus", (hovered || dragging) ? 1.0F : 0.0F, this.resolveControlAnimationSpeed(clickGui), this.resolveAnimationSmooth(clickGui), this.resolveAnimationType(clickGui), this.resolveAnimationEnabled(clickGui));
        int trackFill = this.mixArgb(theme.cardAltArgb(), theme.controlArgb(), UiMotion.clamp01(0.44F + focus * 0.30F));
        float trackRadius = Math.min(track.h * 0.5F, this.stableControlRadius(k));
        NanoUi.drawSurface(vg, stack, track.x, track.y, track.w, track.h, trackRadius, trackFill, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 114));
        float fillW = Math.max(0.0F, (track.w - scaled(2.0F, k)) * displayRatio);
        int activeFill = this.mixArgb(theme.controlActiveArgb(), theme.accentArgb(), 0.74F);
        NanoUi.drawSurface(vg, stack, track.x + scaled(1.0F, k), track.y + scaled(1.0F, k), fillW, track.h - scaled(2.0F, k), Math.max(scaled(1.6F, k), trackRadius - scaled(1.6F, k)), activeFill, 0);
        float handleX = track.x + displayRatio * track.w;
        float knobSize = scaled(5.8F, k) + scaled(1.6F, k) * focus + (dragging ? scaled(0.9F, k) : 0.0F);
        float knobX = handleX - knobSize * 0.5F;
        float knobY = track.y + (track.h - knobSize) * 0.5F;
        int knobColor = this.mixArgb(theme.accentArgb(), 0xFFF8FBFF, UiMotion.clamp01(0.40F + focus * 0.52F));
        NanoUi.drawSurface(vg, stack, knobX, knobY, knobSize, knobSize, knobSize * 0.5F, knobColor, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 110));
        float glow = knobSize + scaled(1.0F, k) * focus;
        NanoUi.drawSurface(vg, stack, handleX - glow * 0.5F, track.y + (track.h - glow) * 0.5F, glow, glow, glow * 0.5F, NanoRenderUtils.withAlpha(0xFFF5F9FF, 62 + Math.round(focus * 92.0F)), 0);
        this.drawNumberValueInput(vg, stack, valueRect, row, setting, hovered, theme, k, font, "clickgui.setting.number." + (this.selectedModule == null ? "none" : this.selectedModule.getId()) + "." + setting.getKey());
    }

    private void drawSettingTextInput(long vg, MemoryStack stack, Rect row, StringSetting setting, boolean hovered, NanoTheme theme, float scale, int font)
    {
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        Rect inputRect = this.settingTextInputRect(row, k);
        boolean active = this.activeTextSetting == setting && this.textInput.isFocused();
        String desc = setting.getDisplayDescription(this.selectedModuleId());
        String placeholder = desc == null || desc.isEmpty() ? this.tr("clickgui.input.text.placeholder", "Input text...") : desc;
        String animKey = "clickgui.setting.text." + (this.selectedModule == null ? "none" : this.selectedModule.getId()) + "." + setting.getKey();
        boolean fieldHovered = inputRect.contains(this.mouseX, this.mouseY);

        if (!active && this.activeTextSetting == setting)
        {
            this.activeTextSetting = null;
        }

        this.textInput.draw(vg, stack, font, theme, inputRect.x, inputRect.y, inputRect.w, inputRect.h, k, scaled(10.2F, k), setting.get(), placeholder, hovered || fieldHovered, active, animKey);
    }

    private void drawNumberValueInput(long vg, MemoryStack stack, Rect valueRect, Rect row, Setting<?> setting, boolean hoveredRow, NanoTheme theme, float scale, int font, String animKey)
    {
        if (valueRect == null || setting == null)
        {
            return;
        }

        boolean fieldHovered = valueRect.contains(this.mouseX, this.mouseY);
        boolean active = this.activeNumberSetting == setting && this.textInput.isFocused();

        if (active)
        {
            this.textInput.draw(vg, stack, font, theme, valueRect.x, valueRect.y, valueRect.w, valueRect.h, scale, scaled(10.2F, scale), this.numberInputBuffer.get(), this.tr("clickgui.input.number.placeholder", "Input..."), hoveredRow || fieldHovered, true, animKey);
            return;
        }

        float focus = UiAnimationBus.animate(animKey + ".idle.focus", fieldHovered ? 1.0F : 0.0F, 0.58F, 0.62F, UiAnimation.Type.EASE_OUT, true);
        int fill = this.mixArgb(theme.cardAltArgb(), theme.controlArgb(), UiMotion.clamp01(0.38F + focus * 0.32F));
        int border = this.mixArgb(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 92), NanoRenderUtils.withAlpha(theme.accentArgb(), 142), UiMotion.clamp01(focus * 0.72F));
        float radius = Math.min(valueRect.h * 0.5F, this.stableControlRadius(scale));
        NanoUi.drawSurface(vg, stack, valueRect.x, valueRect.y, valueRect.w, valueRect.h, radius, fill, border);
        NanoUi.drawRightText(vg, stack, font, valueRect.x2() - scaled(4.0F, scale), row.y + row.h * 0.5F + scaled(0.45F, scale), scaled(10.2F, scale), theme.textMutedArgb(), this.settingValue(setting));
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

        float ratio = ((float)mouseX - track.x) / Math.max(1.0F, track.w);
        ratio = UiMotion.clamp01(ratio);
        this.lastSliderDragNanos = System.nanoTime();

        if (setting instanceof IntSetting)
        {
            IntSetting s = (IntSetting)setting;
            float v = (float)s.getMin() + ratio * (float)(s.getMax() - s.getMin());
            int step = Math.max(1, s.getStep());
            int snapped = Math.round(v / (float)step) * step;
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
            float snapped = Math.round(v / step) * step;
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
            double snapped = Math.round(v / step) * step;
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
            return;
        }

        List<Setting<?>> settings = this.visibleSettings(this.selectedModule);
        int index = settings.indexOf(this.draggingSettingSlider);

        if (index < 0)
        {
            this.draggingSettingSlider = null;
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

        if (this.lastNanoVg != 0L)
        {
            NanoFontBook.ensureLoaded(this.lastNanoVg);
        }

        this.textInput.onMouseDown(mouseX, mouseY, inputRect.x, inputRect.y, inputRect.w, inputRect.h, this.lastNanoVg, NanoFontBook.uiRegular(), scaled(10.2F, UiMotion.clamp(scale, 0.35F, 1.85F)), setting.get());
    }

    private void activateNumberInput(Rect inputRect, float scale, int mouseX, int mouseY, Setting<?> setting)
    {
        if (setting == null || inputRect == null || !this.isSliderSetting(setting))
        {
            return;
        }

        this.commitActiveNumberInput();
        this.activeTextSetting = null;
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
                this.textInput.onMouseDrag(this.mouseX, inputRect.x, inputRect.w, this.lastNanoVg, NanoFontBook.uiRegular(), scaled(10.2F, l.scale), this.activeTextSetting.get());
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
            return value == null ? this.tr("clickgui.value.null", "null") : setting.getDisplayOption(this.selectedModuleId(), value);
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
        if (setting == null || this.selectedModule == null || !"click_gui".equalsIgnoreCase(this.selectedModule.getId()))
        {
            return false;
        }

        String key = setting.getKey();

        if (key == null)
        {
            return false;
        }

        return key.startsWith("ui_anim_") || key.startsWith("ui_control_") || key.startsWith("ui_slider_") || key.startsWith("ui_open_") || key.startsWith("ui_close_") || key.startsWith("ui_switch_") || key.startsWith("ui_back_");
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
            || "accent_override".equals(key);
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

        if (!this.textInput.isFocused() || this.selectedModule == null)
        {
            this.activeTextSetting = null;
            this.textInput.blur();
            return;
        }

        List<Setting<?>> settings = this.selectedModule.getSettings();

        if (!settings.contains(this.activeTextSetting) || !this.activeTextSetting.isVisible() || !this.visibleSettings(this.selectedModule).contains(this.activeTextSetting))
        {
            this.activeTextSetting = null;
            this.textInput.blur();
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

        this.activeTextSetting = null;
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

            if (clickGuiModule && this.isGlobalClientSetting(setting))
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

    private boolean resolveAnimationEnabled(ClickGuiModule clickGui)
    {
        return clickGui == null || clickGui.isGlobalAnimationEnabled();
    }

    private float resolveAnimationSpeed(ClickGuiModule clickGui, UiScaleEditModule uiScale)
    {
        float shared = clickGui == null ? 0.56F : clickGui.getGlobalAnimationSpeed();
        float profile = uiScale == null ? 0.30F : uiScale.getMotionSpeed(UiScaleEditModule.UiTarget.CLICK_GUI);
        float value = UiMotion.clamp(profile * (0.55F + shared), 0.05F, 1.0F);

        if (this.window.isInteracting() || this.draggingSettingSlider != null || this.draggingSv || this.draggingHue || this.draggingAlpha)
        {
            value = UiMotion.clamp(value * 1.22F + 0.06F, 0.05F, 1.0F);
        }

        return value;
    }

    private float resolveControlAnimationSpeed(ClickGuiModule clickGui)
    {
        if (clickGui == null)
        {
            return 0.56F;
        }

        return clickGui.getControlAnimationSpeed();
    }

    private float resolveSliderAnimationSpeed(ClickGuiModule clickGui)
    {
        if (clickGui == null)
        {
            return 0.62F;
        }

        return clickGui.getSliderAnimationSpeed();
    }

    private float resolveAnimationSmooth(ClickGuiModule clickGui)
    {
        return clickGui == null ? 0.62F : clickGui.getGlobalAnimationSmooth();
    }

    private UiAnimation.Type resolveAnimationType(ClickGuiModule clickGui)
    {
        return clickGui == null ? UiAnimation.Type.EASE_OUT : clickGui.getControlAnimationType();
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

    private void openClientSettings()
    {
        this.requestTransition(TransitionMode.SWITCH, new ClientSettingsScreen(this.modules, this));
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
        float smooth = this.resolveAnimationSmooth(clickGui);
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
        float smooth = this.resolveAnimationSmooth(clickGui);
        UiAnimation.Type type = clickGui == null ? UiAnimation.Type.EASE_OUT : clickGui.getGuiSwitchAnimationType();
        float response = UiAnimation.responseFromSpeed(speed, smooth, type, false);
        this.categoryTransition = UiAnimation.step(this.categoryTransition, 1.0F, response, dt, type, smooth);
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
        this.settingScroll = this.clamp(this.settingScroll, settingMax);
        this.categoryScrollVisual = UiMotion.clamp(this.categoryScrollVisual, 0.0F, (float)catMax);
        this.moduleScrollVisual = UiMotion.clamp(this.moduleScrollVisual, 0.0F, (float)moduleMax);
        this.settingScrollVisual = UiMotion.clamp(this.settingScrollVisual, 0.0F, (float)settingMax);
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
        this.settingScroll = this.clamp(this.settingScroll, settingMax);

        if (!this.resolveAnimationEnabled(clickGui))
        {
            this.categoryScrollVisual = (float)this.categoryScroll;
            this.moduleScrollVisual = (float)this.moduleScroll;
            this.settingScrollVisual = (float)this.settingScroll;
            return;
        }

        float speed = UiMotion.clamp(this.resolveControlAnimationSpeed(clickGui) * 0.95F + 0.10F, 0.05F, 1.0F);
        float smooth = this.resolveAnimationSmooth(clickGui);
        UiAnimation.Type type = this.resolveAnimationType(clickGui);
        this.categoryScrollVisual = UiAnimationBus.animate("clickgui.scroll.category", (float)this.categoryScroll, speed, smooth, type, true);
        this.moduleScrollVisual = UiAnimationBus.animate("clickgui.scroll.module", (float)this.moduleScroll, speed, smooth, type, true);
        this.settingScrollVisual = UiAnimationBus.animate("clickgui.scroll.setting", (float)this.settingScroll, speed, smooth, type, true);
    }

    private int clamp(int value, int max)
    {
        return Math.max(0, Math.min(max, value));
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

        float bodyY = windowRect.y + headerHeight + scaled(10.0F, k);
        float bodyH = windowRect.h - headerHeight - outerPad;
        float contentW = windowRect.w - outerPad * 2.0F - gapMajor;
        float leftColumnW = UiMotion.clamp(contentW * 0.24F, scaled(160.0F, k), Math.min(scaled(270.0F, k), contentW * 0.34F));
        float bodyCardH = bodyH - outerPad;
        float leftX = windowRect.x + outerPad;
        Rect sidebar = new Rect(leftX, bodyY, leftColumnW, bodyCardH);
        Rect sidebarRows = new Rect(sidebar.x + scaled(10.0F, k), sidebar.y + scaled(32.0F, k), sidebar.w - scaled(20.0F, k), Math.max(scaled(24.0F, k), sidebar.h - scaled(42.0F, k)));

        float mainX = leftX + leftColumnW + gapMajor;
        float mainW = Math.max(scaled(170.0F, k), windowRect.x2() - mainX - outerPad);
        Rect main = new Rect(mainX, bodyY, mainW, bodyCardH);
        boolean hasPicker = this.activeColor != null;
        float minSettingsHeight = hasPicker ? scaled(286.0F, k) : scaled(170.0F, k);
        float modulesBaseRatio = hasPicker ? 0.40F : 0.50F;
        float maxModulesHeight = Math.max(scaled(154.0F, k), main.h - minSettingsHeight - gapMajor);
        float modulesH = UiMotion.clamp(main.h * modulesBaseRatio, scaled(110.0F, k), maxModulesHeight);
        Rect modulesCard = new Rect(main.x, main.y, main.w, modulesH);
        Rect moduleRows = new Rect(modulesCard.x + scaled(10.0F, k), modulesCard.y + scaled(34.0F, k), modulesCard.w - scaled(20.0F, k), modulesCard.h - scaled(44.0F, k));
        Rect settingsCard = new Rect(main.x, modulesCard.y2() + gapMajor, main.w, Math.max(scaled(110.0F, k), main.y2() - modulesCard.y2() - gapMajor));

        Rect settingsHead = new Rect(settingsCard.x + scaled(12.0F, k), settingsCard.y + scaled(12.0F, k), settingsCard.w - scaled(24.0F, k), scaled(36.0F, k));
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
        float rowsTop = buttonY + scaled(BTN_HEIGHT, k) + scaled(10.0F, k);
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

        return new Layout(windowRect, header, headerDrag, topHudEdit, topUiScale, topClientSettings, sidebar, sidebarRows, main, modulesCard, moduleRows, settingsCard, settingsHead, btnToggle, btnBind, settingsRows, resizeHandle, pickerCard, pickerSv, pickerHue, pickerAlpha, pickerPreview, hasPicker, k);
    }

    private Rect fallbackWindow()
    {
        float width = Math.min(Math.max(MIN_WINDOW_WIDTH, BASE_WINDOW_WIDTH), (float)this.width - 16.0F);
        float height = Math.min(Math.max(MIN_WINDOW_HEIGHT, BASE_WINDOW_HEIGHT), (float)this.height - 16.0F);
        return new Rect(((float)this.width - width) * 0.5F, ((float)this.height - height) * 0.5F, width, height);
    }

    private UiWindowState.ResizeMode resolveResizeMode(Rect windowRect, int mouseX, int mouseY, float edge)
    {
        float e = Math.max(3.0F, edge);
        boolean nearLeft = Math.abs((float)mouseX - windowRect.x) <= e;
        boolean nearRight = Math.abs((float)mouseX - windowRect.x2()) <= e;
        boolean nearTop = Math.abs((float)mouseY - windowRect.y) <= e;
        boolean nearBottom = Math.abs((float)mouseY - windowRect.y2()) <= e;
        boolean insideX = (float)mouseX >= windowRect.x - e && (float)mouseX <= windowRect.x2() + e;
        boolean insideY = (float)mouseY >= windowRect.y - e && (float)mouseY <= windowRect.y2() + e;

        if (!insideX || !insideY)
        {
            return null;
        }

        if (nearLeft && nearTop)
        {
            return UiWindowState.ResizeMode.TOP_LEFT;
        }

        if (nearRight && nearTop)
        {
            return UiWindowState.ResizeMode.TOP_RIGHT;
        }

        if (nearLeft && nearBottom)
        {
            return UiWindowState.ResizeMode.BOTTOM_LEFT;
        }

        if (nearRight && nearBottom)
        {
            return UiWindowState.ResizeMode.BOTTOM_RIGHT;
        }

        if (nearLeft)
        {
            return UiWindowState.ResizeMode.LEFT;
        }

        if (nearRight)
        {
            return UiWindowState.ResizeMode.RIGHT;
        }

        if (nearTop)
        {
            return UiWindowState.ResizeMode.TOP;
        }

        if (nearBottom)
        {
            return UiWindowState.ResizeMode.BOTTOM;
        }

        return null;
    }

    private float stableWindowRadius(float scale)
    {
        return scaled(RADIUS_WINDOW, UiMotion.clamp(scale, 0.35F, 1.85F));
    }

    private float stablePanelRadius(float scale)
    {
        return scaled(RADIUS_PANEL, UiMotion.clamp(scale, 0.35F, 1.85F));
    }

    private float stableRowRadius(float scale)
    {
        return scaled(RADIUS_ROW, UiMotion.clamp(scale, 0.35F, 1.85F));
    }

    private float stableControlRadius(float scale)
    {
        return scaled(RADIUS_CONTROL, UiMotion.clamp(scale, 0.35F, 1.85F));
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

        private Layout(Rect window, Rect header, Rect headerDrag, Rect topHudEdit, Rect topUiScale, Rect topClientSettings, Rect sidebar, Rect sidebarRows, Rect main, Rect modulesCard, Rect moduleRows, Rect settingsCard, Rect settingsHead, Rect btnToggle, Rect btnBind, Rect settingsRows, Rect resizeHandle, Rect pickerCard, Rect pickerSv, Rect pickerHue, Rect pickerAlpha, Rect pickerPreview, boolean hasPicker, float scale)
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
