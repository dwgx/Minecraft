package client.ui.template;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Centralised UI dimension profile. All NanoUI screens read sizes from here.
 * Persisted in client.json under "ui_layout".
 */
public final class UiLayoutProfile
{
    // ClickGui defaults
    private float clickGuiWidth = 1120.0F;
    private float clickGuiHeight = 700.0F;
    private float clickGuiMinWidth = 260.0F;
    private float clickGuiMinHeight = 170.0F;
    private float clickGuiAnchorX = 0.5F;
    private float clickGuiAnchorY = 0.5F;

    // Settings screen defaults
    private float settingsWidth = 900.0F;
    private float settingsHeight = 620.0F;
    private float settingsMinWidth = 520.0F;
    private float settingsMinHeight = 340.0F;
    private float settingsAnchorX = 0.5F;
    private float settingsAnchorY = 0.5F;

    // UIScale screen defaults
    private float uiScaleWidth = 690.0F;
    private float uiScaleHeight = 430.0F;
    private float uiScaleMinWidth = 360.0F;
    private float uiScaleMinHeight = 230.0F;
    private float uiScaleAnchorX = 0.5F;
    private float uiScaleAnchorY = 0.5F;

    // AccountManager defaults
    private float accountWidth = 580.0F;
    private float accountHeight = 460.0F;
    private float accountMinWidth = 340.0F;
    private float accountMinHeight = 260.0F;
    private float accountScale = 1.0F;
    private float accountAnchorX = 0.5F;
    private float accountAnchorY = 0.5F;

    // ClickGui panel ratios
    private float clickGuiSidebarRatio = 0.24F;
    private float clickGuiSidebarMinRatio = 0.15F;
    private float clickGuiSidebarMaxRatio = 0.40F;
    private float clickGuiModulesRatio = 0.31F;
    private float clickGuiModulesMinRatio = 0.18F;
    private float clickGuiModulesMaxRatio = 0.55F;

    // ChatOverlay defaults
    private float chatWidth = 960.0F;
    private float chatHeight = 600.0F;
    private float chatMinWidth = 480.0F;
    private float chatMinHeight = 320.0F;
    private float chatServerListWidth = 56.0F;
    private float chatChannelRatio = 0.20F;
    private float chatTopBarHeight = 42.0F;
    private float chatInputBarHeight = 52.0F;
    private float chatStatusBarHeight = 28.0F;

    // Shared layout constants
    private float screenMargin = 8.0F;
    private float headerHeight = 36.0F;
    private float outerPad = 12.0F;
    private float gapMajor = 14.0F;
    private float rowCategory = 24.0F;
    private float rowModule = 34.0F;
    private float rowSetting = 26.0F;
    private float btnHeight = 20.0F;
    private float radiusWindow = 9.0F;
    private float radiusPanel = 8.0F;
    private float radiusRow = 6.0F;
    private float radiusControl = 6.0F;
    private float valueColWidth = 132.0F;
    private float resetColWidth = 38.0F;

    public UiLayoutProfile()
    {
    }

    // --- ClickGui ---
    public float clickGuiWidth() { return this.clickGuiWidth; }
    public float clickGuiHeight() { return this.clickGuiHeight; }
    public float clickGuiMinWidth() { return this.clickGuiMinWidth; }
    public float clickGuiMinHeight() { return this.clickGuiMinHeight; }
    public float clickGuiAnchorX() { return this.clickGuiAnchorX; }
    public float clickGuiAnchorY() { return this.clickGuiAnchorY; }
    public void setClickGuiWidth(float v) { this.clickGuiWidth = v; }
    public void setClickGuiHeight(float v) { this.clickGuiHeight = v; }
    public void setClickGuiAnchorX(float v) { this.clickGuiAnchorX = v; }
    public void setClickGuiAnchorY(float v) { this.clickGuiAnchorY = v; }

    // --- Settings ---
    public float settingsWidth() { return this.settingsWidth; }
    public float settingsHeight() { return this.settingsHeight; }
    public float settingsMinWidth() { return this.settingsMinWidth; }
    public float settingsMinHeight() { return this.settingsMinHeight; }
    public float settingsAnchorX() { return this.settingsAnchorX; }
    public float settingsAnchorY() { return this.settingsAnchorY; }
    public void setSettingsWidth(float v) { this.settingsWidth = v; }
    public void setSettingsHeight(float v) { this.settingsHeight = v; }
    public void setSettingsAnchorX(float v) { this.settingsAnchorX = v; }
    public void setSettingsAnchorY(float v) { this.settingsAnchorY = v; }

    // --- UIScale ---
    public float uiScaleWidth() { return this.uiScaleWidth; }
    public float uiScaleHeight() { return this.uiScaleHeight; }
    public float uiScaleMinWidth() { return this.uiScaleMinWidth; }
    public float uiScaleMinHeight() { return this.uiScaleMinHeight; }
    public float uiScaleAnchorX() { return this.uiScaleAnchorX; }
    public float uiScaleAnchorY() { return this.uiScaleAnchorY; }
    public void setUiScaleWidth(float v) { this.uiScaleWidth = v; }
    public void setUiScaleHeight(float v) { this.uiScaleHeight = v; }
    public void setUiScaleAnchorX(float v) { this.uiScaleAnchorX = v; }
    public void setUiScaleAnchorY(float v) { this.uiScaleAnchorY = v; }

    // --- AccountManager ---
    public float accountWidth() { return this.accountWidth; }
    public float accountHeight() { return this.accountHeight; }
    public float accountMinWidth() { return this.accountMinWidth; }
    public float accountMinHeight() { return this.accountMinHeight; }
    public float accountScale() { return this.accountScale; }
    public float accountAnchorX() { return this.accountAnchorX; }
    public float accountAnchorY() { return this.accountAnchorY; }

    public void setAccountScale(float value) { this.accountScale = value; }
    public void setAccountAnchorX(float value) { this.accountAnchorX = value; }
    public void setAccountAnchorY(float value) { this.accountAnchorY = value; }
    public void setAccountWidth(float v) { this.accountWidth = v; }
    public void setAccountHeight(float v) { this.accountHeight = v; }

    // --- Shared layout ---
    public float screenMargin() { return this.screenMargin; }
    public float headerHeight() { return this.headerHeight; }
    public float outerPad() { return this.outerPad; }
    public float gapMajor() { return this.gapMajor; }
    public float rowCategory() { return this.rowCategory; }
    public float rowModule() { return this.rowModule; }
    public float rowSetting() { return this.rowSetting; }
    public float btnHeight() { return this.btnHeight; }
    public float radiusWindow() { return this.radiusWindow; }
    public float radiusPanel() { return this.radiusPanel; }
    public float radiusRow() { return this.radiusRow; }
    public float radiusControl() { return this.radiusControl; }
    public float valueColWidth() { return this.valueColWidth; }
    public float resetColWidth() { return this.resetColWidth; }

    // --- ClickGui panel ratios ---
    public float clickGuiSidebarRatio() { return this.clickGuiSidebarRatio; }
    public float clickGuiSidebarMinRatio() { return this.clickGuiSidebarMinRatio; }
    public float clickGuiSidebarMaxRatio() { return this.clickGuiSidebarMaxRatio; }
    public float clickGuiModulesRatio() { return this.clickGuiModulesRatio; }
    public float clickGuiModulesMinRatio() { return this.clickGuiModulesMinRatio; }
    public float clickGuiModulesMaxRatio() { return this.clickGuiModulesMaxRatio; }
    public void setClickGuiSidebarRatio(float v) { this.clickGuiSidebarRatio = v; }
    public void setClickGuiModulesRatio(float v) { this.clickGuiModulesRatio = v; }

    // --- ChatOverlay ---
    public float chatWidth() { return this.chatWidth; }
    public float chatHeight() { return this.chatHeight; }
    public float chatMinWidth() { return this.chatMinWidth; }
    public float chatMinHeight() { return this.chatMinHeight; }
    public float chatServerListWidth() { return this.chatServerListWidth; }
    public float chatChannelRatio() { return this.chatChannelRatio; }
    public float chatTopBarHeight() { return this.chatTopBarHeight; }
    public float chatInputBarHeight() { return this.chatInputBarHeight; }
    public float chatStatusBarHeight() { return this.chatStatusBarHeight; }
    public void setChatWidth(float v) { this.chatWidth = v; }
    public void setChatHeight(float v) { this.chatHeight = v; }
    public void setChatServerListWidth(float v) { this.chatServerListWidth = v; }
    public void setChatChannelRatio(float v) { this.chatChannelRatio = v; }
    public void setChatTopBarHeight(float v) { this.chatTopBarHeight = v; }
    public void setChatInputBarHeight(float v) { this.chatInputBarHeight = v; }
    public void setChatStatusBarHeight(float v) { this.chatStatusBarHeight = v; }

    // --- Shared layout setters ---
    public void setGapMajor(float v) { this.gapMajor = v; }
    public void setRowCategory(float v) { this.rowCategory = v; }
    public void setRowModule(float v) { this.rowModule = v; }
    public void setRowSetting(float v) { this.rowSetting = v; }
    public void setBtnHeight(float v) { this.btnHeight = v; }
    public void setValueColWidth(float v) { this.valueColWidth = v; }

    // --- Serialization ---

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        obj.add("clickGuiWidth", new JsonPrimitive(this.clickGuiWidth));
        obj.add("clickGuiHeight", new JsonPrimitive(this.clickGuiHeight));
        obj.add("clickGuiMinWidth", new JsonPrimitive(this.clickGuiMinWidth));
        obj.add("clickGuiMinHeight", new JsonPrimitive(this.clickGuiMinHeight));
        obj.add("clickGuiAnchorX", new JsonPrimitive(this.clickGuiAnchorX));
        obj.add("clickGuiAnchorY", new JsonPrimitive(this.clickGuiAnchorY));
        obj.add("settingsWidth", new JsonPrimitive(this.settingsWidth));
        obj.add("settingsHeight", new JsonPrimitive(this.settingsHeight));
        obj.add("settingsMinWidth", new JsonPrimitive(this.settingsMinWidth));
        obj.add("settingsMinHeight", new JsonPrimitive(this.settingsMinHeight));
        obj.add("settingsAnchorX", new JsonPrimitive(this.settingsAnchorX));
        obj.add("settingsAnchorY", new JsonPrimitive(this.settingsAnchorY));
        obj.add("uiScaleWidth", new JsonPrimitive(this.uiScaleWidth));
        obj.add("uiScaleHeight", new JsonPrimitive(this.uiScaleHeight));
        obj.add("uiScaleMinWidth", new JsonPrimitive(this.uiScaleMinWidth));
        obj.add("uiScaleMinHeight", new JsonPrimitive(this.uiScaleMinHeight));
        obj.add("uiScaleAnchorX", new JsonPrimitive(this.uiScaleAnchorX));
        obj.add("uiScaleAnchorY", new JsonPrimitive(this.uiScaleAnchorY));
        obj.add("accountWidth", new JsonPrimitive(this.accountWidth));
        obj.add("accountHeight", new JsonPrimitive(this.accountHeight));
        obj.add("accountMinWidth", new JsonPrimitive(this.accountMinWidth));
        obj.add("accountMinHeight", new JsonPrimitive(this.accountMinHeight));
        obj.add("accountScale", new JsonPrimitive(this.accountScale));
        obj.add("accountAnchorX", new JsonPrimitive(this.accountAnchorX));
        obj.add("accountAnchorY", new JsonPrimitive(this.accountAnchorY));
        obj.add("screenMargin", new JsonPrimitive(this.screenMargin));
        obj.add("headerHeight", new JsonPrimitive(this.headerHeight));
        obj.add("outerPad", new JsonPrimitive(this.outerPad));
        obj.add("gapMajor", new JsonPrimitive(this.gapMajor));
        obj.add("rowCategory", new JsonPrimitive(this.rowCategory));
        obj.add("rowModule", new JsonPrimitive(this.rowModule));
        obj.add("rowSetting", new JsonPrimitive(this.rowSetting));
        obj.add("btnHeight", new JsonPrimitive(this.btnHeight));
        obj.add("radiusWindow", new JsonPrimitive(this.radiusWindow));
        obj.add("radiusPanel", new JsonPrimitive(this.radiusPanel));
        obj.add("radiusRow", new JsonPrimitive(this.radiusRow));
        obj.add("radiusControl", new JsonPrimitive(this.radiusControl));
        obj.add("valueColWidth", new JsonPrimitive(this.valueColWidth));
        obj.add("resetColWidth", new JsonPrimitive(this.resetColWidth));
        obj.add("clickGuiSidebarRatio", new JsonPrimitive(this.clickGuiSidebarRatio));
        obj.add("clickGuiSidebarMinRatio", new JsonPrimitive(this.clickGuiSidebarMinRatio));
        obj.add("clickGuiSidebarMaxRatio", new JsonPrimitive(this.clickGuiSidebarMaxRatio));
        obj.add("clickGuiModulesRatio", new JsonPrimitive(this.clickGuiModulesRatio));
        obj.add("clickGuiModulesMinRatio", new JsonPrimitive(this.clickGuiModulesMinRatio));
        obj.add("clickGuiModulesMaxRatio", new JsonPrimitive(this.clickGuiModulesMaxRatio));
        obj.add("chatWidth", new JsonPrimitive(this.chatWidth));
        obj.add("chatHeight", new JsonPrimitive(this.chatHeight));
        obj.add("chatMinWidth", new JsonPrimitive(this.chatMinWidth));
        obj.add("chatMinHeight", new JsonPrimitive(this.chatMinHeight));
        obj.add("chatServerListWidth", new JsonPrimitive(this.chatServerListWidth));
        obj.add("chatChannelRatio", new JsonPrimitive(this.chatChannelRatio));
        obj.add("chatTopBarHeight", new JsonPrimitive(this.chatTopBarHeight));
        obj.add("chatInputBarHeight", new JsonPrimitive(this.chatInputBarHeight));
        obj.add("chatStatusBarHeight", new JsonPrimitive(this.chatStatusBarHeight));
        return obj;
    }

    public static UiLayoutProfile fromJson(JsonObject obj)
    {
        UiLayoutProfile p = new UiLayoutProfile();

        if (obj == null)
        {
            return p;
        }

        p.clickGuiWidth = getFloat(obj, "clickGuiWidth", p.clickGuiWidth);
        p.clickGuiHeight = getFloat(obj, "clickGuiHeight", p.clickGuiHeight);
        p.clickGuiMinWidth = getFloat(obj, "clickGuiMinWidth", p.clickGuiMinWidth);
        p.clickGuiMinHeight = getFloat(obj, "clickGuiMinHeight", p.clickGuiMinHeight);
        p.clickGuiAnchorX = getFloat(obj, "clickGuiAnchorX", p.clickGuiAnchorX);
        p.clickGuiAnchorY = getFloat(obj, "clickGuiAnchorY", p.clickGuiAnchorY);
        p.settingsWidth = getFloat(obj, "settingsWidth", p.settingsWidth);
        p.settingsHeight = getFloat(obj, "settingsHeight", p.settingsHeight);
        p.settingsMinWidth = getFloat(obj, "settingsMinWidth", p.settingsMinWidth);
        p.settingsMinHeight = getFloat(obj, "settingsMinHeight", p.settingsMinHeight);
        p.settingsAnchorX = getFloat(obj, "settingsAnchorX", p.settingsAnchorX);
        p.settingsAnchorY = getFloat(obj, "settingsAnchorY", p.settingsAnchorY);
        p.uiScaleWidth = getFloat(obj, "uiScaleWidth", p.uiScaleWidth);
        p.uiScaleHeight = getFloat(obj, "uiScaleHeight", p.uiScaleHeight);
        p.uiScaleMinWidth = getFloat(obj, "uiScaleMinWidth", p.uiScaleMinWidth);
        p.uiScaleMinHeight = getFloat(obj, "uiScaleMinHeight", p.uiScaleMinHeight);
        p.uiScaleAnchorX = getFloat(obj, "uiScaleAnchorX", p.uiScaleAnchorX);
        p.uiScaleAnchorY = getFloat(obj, "uiScaleAnchorY", p.uiScaleAnchorY);
        p.accountWidth = getFloat(obj, "accountWidth", p.accountWidth);
        p.accountHeight = getFloat(obj, "accountHeight", p.accountHeight);
        p.accountMinWidth = getFloat(obj, "accountMinWidth", p.accountMinWidth);
        p.accountMinHeight = getFloat(obj, "accountMinHeight", p.accountMinHeight);
        p.accountScale = getFloat(obj, "accountScale", p.accountScale);
        p.accountAnchorX = getFloat(obj, "accountAnchorX", p.accountAnchorX);
        p.accountAnchorY = getFloat(obj, "accountAnchorY", p.accountAnchorY);
        p.screenMargin = getFloat(obj, "screenMargin", p.screenMargin);
        p.headerHeight = getFloat(obj, "headerHeight", p.headerHeight);
        p.outerPad = getFloat(obj, "outerPad", p.outerPad);
        p.gapMajor = getFloat(obj, "gapMajor", p.gapMajor);
        p.rowCategory = getFloat(obj, "rowCategory", p.rowCategory);
        p.rowModule = getFloat(obj, "rowModule", p.rowModule);
        p.rowSetting = getFloat(obj, "rowSetting", p.rowSetting);
        p.btnHeight = getFloat(obj, "btnHeight", p.btnHeight);
        p.radiusWindow = getFloat(obj, "radiusWindow", p.radiusWindow);
        p.radiusPanel = getFloat(obj, "radiusPanel", p.radiusPanel);
        p.radiusRow = getFloat(obj, "radiusRow", p.radiusRow);
        p.radiusControl = getFloat(obj, "radiusControl", p.radiusControl);
        p.valueColWidth = getFloat(obj, "valueColWidth", p.valueColWidth);
        p.resetColWidth = getFloat(obj, "resetColWidth", p.resetColWidth);
        p.clickGuiSidebarRatio = getFloat(obj, "clickGuiSidebarRatio", p.clickGuiSidebarRatio);
        p.clickGuiSidebarMinRatio = getFloat(obj, "clickGuiSidebarMinRatio", p.clickGuiSidebarMinRatio);
        p.clickGuiSidebarMaxRatio = getFloat(obj, "clickGuiSidebarMaxRatio", p.clickGuiSidebarMaxRatio);
        p.clickGuiModulesRatio = getFloat(obj, "clickGuiModulesRatio", p.clickGuiModulesRatio);
        p.clickGuiModulesMinRatio = getFloat(obj, "clickGuiModulesMinRatio", p.clickGuiModulesMinRatio);
        p.clickGuiModulesMaxRatio = getFloat(obj, "clickGuiModulesMaxRatio", p.clickGuiModulesMaxRatio);
        p.chatWidth = getFloat(obj, "chatWidth", p.chatWidth);
        p.chatHeight = getFloat(obj, "chatHeight", p.chatHeight);
        p.chatMinWidth = getFloat(obj, "chatMinWidth", p.chatMinWidth);
        p.chatMinHeight = getFloat(obj, "chatMinHeight", p.chatMinHeight);
        p.chatServerListWidth = getFloat(obj, "chatServerListWidth", p.chatServerListWidth);
        p.chatChannelRatio = getFloat(obj, "chatChannelRatio", p.chatChannelRatio);
        p.chatTopBarHeight = getFloat(obj, "chatTopBarHeight", p.chatTopBarHeight);
        p.chatInputBarHeight = getFloat(obj, "chatInputBarHeight", p.chatInputBarHeight);
        p.chatStatusBarHeight = getFloat(obj, "chatStatusBarHeight", p.chatStatusBarHeight);
        return p;
    }

    private static float getFloat(JsonObject obj, String key, float fallback)
    {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive())
        {
            return fallback;
        }

        try
        {
            return obj.get(key).getAsFloat();
        }
        catch (NumberFormatException ex)
        {
            return fallback;
        }
    }
}