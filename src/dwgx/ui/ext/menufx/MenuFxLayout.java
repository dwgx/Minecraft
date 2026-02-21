package dwgx.ui.ext.menufx;

import client.ui.template.UiMotion;
import dwgx.ui.ext.UiExtensionManager;

public final class MenuFxLayout
{
    private static final float EXT_MARGIN = 8.0F;
    private static final float EXT_TOGGLE_W = 230.0F;
    private static final float EXT_TOGGLE_H = 28.0F;
    private static final float EXT_PANEL_W = 360.0F;
    private static final float EXT_PANEL_MIN_W = 180.0F;
    private static final float EXT_PANEL_MAX_W = 390.0F;
    private static final float EXT_ROW_H = 24.0F;
    private static final float EXT_ROW_GAP = 3.0F;
    private static final float EXT_PANEL_GAP = 7.0F;
    private static final float EXT_TOGGLE_BOTTOM_INSET = 2.0F;
    private static final float EXT_PANEL_MENU_CLEARANCE = 14.0F;
    private static final float EXT_PANEL_HEADER_H = 24.0F;
    private static final float EXT_PANEL_FOOTER_H = 20.0F;
    private static final float EXT_PANEL_ROW_PAD_X = 9.0F;
    private static final float EXT_PANEL_ROW_PAD_TOP = 7.0F;
    private static final float EXT_PANEL_ROW_PAD_BOTTOM = 5.0F;
    private static final float EXT_ANCHOR_Y = 1.0F;
    private static final float EXT_COMPACT_WIDTH = 960.0F;
    private static final float EXT_COMPACT_HEIGHT = 540.0F;
    private static final float EXT_MIN_SCALE = 0.44F;
    private static final float EXT_MAX_SCALE = 1.05F;

    private final Rect toggle;
    private final Rect panel;
    private final UiExtensionManager.MainMenuBackgroundOption[] backgroundOptions;
    private final Rect[] modeRows;
    private final Rect rowGameOnly;
    private final Rect rowsClip;
    private final float rowStep;
    private final int visibleRows;
    private final float scale;

    private MenuFxLayout(
        Rect toggle,
        Rect panel,
        UiExtensionManager.MainMenuBackgroundOption[] backgroundOptions,
        Rect[] modeRows,
        Rect rowGameOnly,
        Rect rowsClip,
        float rowStep,
        int visibleRows,
        float scale
    )
    {
        this.toggle = toggle;
        this.panel = panel;
        this.backgroundOptions = backgroundOptions;
        this.modeRows = modeRows;
        this.rowGameOnly = rowGameOnly;
        this.rowsClip = rowsClip;
        this.rowStep = Math.max(1.0F, rowStep);
        this.visibleRows = Math.max(1, visibleRows);
        this.scale = UiMotion.clamp(scale, EXT_MIN_SCALE, EXT_MAX_SCALE);
    }

    public Rect toggle()
    {
        return this.toggle;
    }

    public Rect panel()
    {
        return this.panel;
    }

    public UiExtensionManager.MainMenuBackgroundOption[] backgroundOptions()
    {
        return this.backgroundOptions;
    }

    public Rect[] modeRows()
    {
        return this.modeRows;
    }

    public Rect rowGameOnly()
    {
        return this.rowGameOnly;
    }

    public Rect rowsClip()
    {
        return this.rowsClip;
    }

    public float rowStep()
    {
        return this.rowStep;
    }

    public int visibleRows()
    {
        return this.visibleRows;
    }

    public float scale()
    {
        return this.scale;
    }

    public static MenuFxLayout compute(int screenWidth, int screenHeight, int buttonColumnLeft, UiExtensionManager.MainMenuBackgroundOption[] backgroundOptions)
    {
        UiExtensionManager.MainMenuBackgroundOption[] options = backgroundOptions == null ? new UiExtensionManager.MainMenuBackgroundOption[0] : backgroundOptions;
        float totalRows = (float)(options.length + 1);
        float uiScale = resolveScale(screenWidth, screenHeight);
        float margin = Math.max(4.0F, scaled(EXT_MARGIN, uiScale));
        float panelGap = scaled(EXT_PANEL_GAP, uiScale);
        float maxPanelByScreen = Math.max(1.0F, (float)screenWidth - margin * 2.0F);
        float maxPanelByButtons = Math.max(1.0F, (float)buttonColumnLeft - margin - scaled(EXT_PANEL_MENU_CLEARANCE, uiScale));
        float maxPanelW = Math.max(1.0F, Math.min(maxPanelByScreen, maxPanelByButtons));
        float minPanelW = Math.min(maxPanelW, scaled(EXT_PANEL_MIN_W, uiScale));
        float targetPanelW = scaled(EXT_PANEL_W, uiScale);
        float panelW = maxPanelW <= minPanelW ? maxPanelW : UiMotion.clamp(targetPanelW, minPanelW, Math.min(maxPanelW, scaled(EXT_PANEL_MAX_W, uiScale)));
        float toggleW = UiMotion.clamp(scaled(EXT_TOGGLE_W, uiScale), Math.min(panelW, scaled(96.0F, uiScale)), panelW);
        float toggleH = scaled(EXT_TOGGLE_H, uiScale);
        float toggleInset = scaled(EXT_TOGGLE_BOTTOM_INSET, uiScale);
        float toggleX = margin;
        float toggleBottom = (float)screenHeight - margin - toggleH - toggleInset;
        float toggleTop = Math.max(margin, (float)screenHeight * 0.52F);
        float toggleAnchor = (float)screenHeight * EXT_ANCHOR_Y - toggleH;
        float toggleY = UiMotion.clamp(toggleAnchor, toggleTop, Math.max(toggleTop, toggleBottom));
        float rowH = scaled(EXT_ROW_H, uiScale);
        float rowGap = scaled(EXT_ROW_GAP, uiScale);
        float rowStep = rowH + rowGap;
        float headerH = scaled(EXT_PANEL_HEADER_H, uiScale);
        float footerH = scaled(EXT_PANEL_FOOTER_H, uiScale);
        float rowPadX = scaled(EXT_PANEL_ROW_PAD_X, uiScale);
        float rowPadTop = scaled(EXT_PANEL_ROW_PAD_TOP, uiScale);
        float rowPadBottom = scaled(EXT_PANEL_ROW_PAD_BOTTOM, uiScale);
        float rowsHeight = rowH * totalRows + rowGap * Math.max(0.0F, totalRows - 1.0F);
        float panelDesiredHeight = headerH + rowPadTop + rowsHeight + rowPadBottom + footerH;
        float panelTopLimit = Math.max(margin, (float)screenHeight * 0.14F);
        float maxPanelHeightBySpace = Math.max(1.0F, toggleY - panelGap - panelTopLimit);
        float panelRatio = screenHeight <= 540 ? 0.38F : 0.42F;
        float maxPanelHeightByRatio = Math.max(scaled(104.0F, uiScale), (float)screenHeight * panelRatio);
        float maxPanelHeight = Math.max(scaled(120.0F, uiScale), Math.min(maxPanelHeightBySpace, maxPanelHeightByRatio));
        float panelHeight = Math.min(panelDesiredHeight, maxPanelHeight);
        float panelY = toggleY - panelGap - panelHeight;

        if (panelY < panelTopLimit)
        {
            panelY = panelTopLimit;
        }

        float rowAreaHeight = Math.max(1.0F, panelHeight - headerH - rowPadTop - rowPadBottom - footerH);
        int visibleRows = Math.max(1, (int)Math.floor((double)((rowAreaHeight + rowGap) / Math.max(1.0F, rowStep))));
        float rowX = toggleX + rowPadX;
        float rowY = panelY + headerH + rowPadTop;
        float rowWidth = Math.max(1.0F, panelW - rowPadX * 2.0F);
        Rect toggle = new Rect(toggleX, toggleY, toggleW, toggleH);
        Rect panel = new Rect(toggleX, panelY, panelW, panelHeight);
        Rect[] modeRows = new Rect[options.length];
        Rect rowsClip = new Rect(rowX, rowY, rowWidth, rowAreaHeight);

        for (int i = 0; i < options.length; ++i)
        {
            modeRows[i] = new Rect(rowX, rowY + rowStep * (float)i, rowWidth, rowH);
        }

        Rect gameOnlyRow = new Rect(rowX, rowY + rowStep * (float)options.length, rowWidth, rowH);
        return new MenuFxLayout(toggle, panel, options, modeRows, gameOnlyRow, rowsClip, rowStep, visibleRows, uiScale);
    }

    private static float resolveScale(int screenWidth, int screenHeight)
    {
        int guiWidth = Math.max(1, screenWidth);
        int guiHeight = Math.max(1, screenHeight);
        float widthRatio = (float)guiWidth / 1280.0F;
        float heightRatio = (float)guiHeight / 720.0F;
        float base = Math.min(widthRatio, heightRatio);
        float compactRatio = Math.min((float)guiWidth / EXT_COMPACT_WIDTH, (float)guiHeight / EXT_COMPACT_HEIGHT);
        float compactFactor = compactRatio < 1.0F ? UiMotion.clamp(0.80F + compactRatio * 0.20F, 0.72F, 1.0F) : 1.0F;
        return UiMotion.clamp(base * compactFactor, EXT_MIN_SCALE, EXT_MAX_SCALE);
    }

    private static float scaled(float value, float scale)
    {
        return value * scale;
    }

    public static final class Rect
    {
        private final float x;
        private final float y;
        private final float w;
        private final float h;

        public Rect(float x, float y, float w, float h)
        {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public float x()
        {
            return this.x;
        }

        public float y()
        {
            return this.y;
        }

        public float w()
        {
            return this.w;
        }

        public float h()
        {
            return this.h;
        }

        public float x2()
        {
            return this.x + this.w;
        }

        public float y2()
        {
            return this.y + this.h;
        }

        public boolean contains(int mx, int my)
        {
            return (float)mx >= this.x && (float)mx <= this.x2() && (float)my >= this.y && (float)my <= this.y2();
        }

        public Rect offset(float dx, float dy)
        {
            return new Rect(this.x + dx, this.y + dy, this.w, this.h);
        }

        public boolean intersects(Rect other)
        {
            return other != null && this.x2() >= other.x && other.x2() >= this.x && this.y2() >= other.y && other.y2() >= this.y;
        }
    }
}
