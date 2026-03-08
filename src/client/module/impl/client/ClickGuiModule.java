package client.module.impl.client;

import client.core.ClientBootstrap;
import client.i18n.ClientLocale;
import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.ColorSetting;
import client.setting.ColorValue;
import client.setting.EnumSetting;
import client.setting.FloatSetting;
import client.setting.IntSetting;
import client.setting.SettingGroup;
import client.setting.Visibility;
import client.ui.ClickGuiScreen;
import client.ui.template.UiAnimationBus;
import client.ui.template.UiAnimation;
import dwgx.nano.NanoPalette;
import net.minecraft.client.Minecraft;

public final class ClickGuiModule extends Module
{
    private final EnumSetting<NanoPalette> palette;
    private final EnumSetting<ClientLocale> locale;
    private final FloatSetting cornerRadius;
    private final IntSetting panelAlpha;
    private final BoolSetting backdropEnabled;
    private final IntSetting backdropAlpha;
    private final BoolSetting accentOverrideEnabled;
    private final ColorSetting accentOverride;
    private final BoolSetting globalAnimationEnabled;
    private final FloatSetting globalAnimationSpeed;
    private final FloatSetting globalAnimationSmooth;
    private final FloatSetting controlAnimationSpeed;
    private final BoolSetting sliderAnimationEnabled;
    private final FloatSetting sliderAnimationSpeed;
    private final FloatSetting pageAnimationSpeed;
    private final FloatSetting listAnimationSpeed;
    private final FloatSetting selectionAnimationSpeed;
    private final FloatSetting inputAnimationSpeed;
    private final FloatSetting inputAnimationSmooth;
    private final EnumSetting<UiAnimation.Type> controlAnimationType;
    private final EnumSetting<UiAnimation.Type> inputAnimationType;
    private final EnumSetting<UiAnimation.Type> guiOpenAnimationType;
    private final EnumSetting<UiAnimation.Type> guiCloseAnimationType;
    private final EnumSetting<UiAnimation.Type> guiSwitchAnimationType;
    private final EnumSetting<UiAnimation.Type> guiBackAnimationType;
    private final IntSetting lastCategoryOrdinal;
    private final IntSetting lastModuleIndex;
    private final IntSetting lastCategoryScroll;
    private final IntSetting lastModuleScroll;
    private final IntSetting lastSettingScroll;
    private final IntSetting lastCompatSettingScroll;
    private final FloatSetting settingCenterScale;
    private final FloatSetting settingCenterAnchorX;
    private final FloatSetting settingCenterAnchorY;
    private final FloatSetting settingCenterPickerX;
    private final FloatSetting settingCenterPickerY;

    public ClickGuiModule()
    {
        super("click_gui", "ClickGUI", Category.CLIENT);
        this.getBind().setKeyCode(54);

        SettingGroup appearance = this.addGroup("appearance", "Appearance");
        this.palette = this.addSetting(new EnumSetting<NanoPalette>("palette", "Palette", "UI palette", NanoPalette.class, NanoPalette.COBALT));
        this.locale = this.addSetting(new EnumSetting<ClientLocale>("locale", "Language", "UI language / 界面语言", ClientLocale.class, ClientLocale.EN_US));
        this.cornerRadius = this.addSetting(new FloatSetting("corner_radius", "Corner Radius", "Window corner radius", 12.0F, 6.0F, 26.0F, 0.5F));
        this.panelAlpha = this.addSetting(new IntSetting("panel_alpha", "Panel Alpha", "Panel opacity", 226, 120, 255, 1));
        this.backdropEnabled = this.addSetting(new BoolSetting("backdrop", "Backdrop", "Enable dimmed backdrop", true));
        this.backdropAlpha = this.addSetting(new IntSetting("backdrop_alpha", "Backdrop Alpha", "Backdrop opacity", 108, 0, 200, 1));
        this.accentOverrideEnabled = this.addSetting(new BoolSetting("accent_override_enabled", "Accent Override", "Use custom accent color", false));
        this.accentOverride = this.addSetting(new ColorSetting("accent_override", "Accent Color", "Custom accent color", new ColorValue(55, 148, 255, 255, false)));
        SettingGroup animation = this.addGroup("animation", "Animation");
        this.globalAnimationEnabled = this.addSetting(new BoolSetting("ui_anim_enabled", "Animation Enable", "Enable global UI animation", true));
        this.globalAnimationSpeed = this.addSetting(new FloatSetting("ui_anim_speed", "Animation Speed", "Global motion speed", 0.56F, 0.05F, 1.0F, 0.01F));
        this.globalAnimationSmooth = this.addSetting(new FloatSetting("ui_anim_smooth", "Animation Smooth", "Global animation smoothness", 0.62F, 0.0F, 1.0F, 0.01F));
        this.controlAnimationSpeed = this.addSetting(new FloatSetting("ui_control_anim_speed", "Control Anim Speed", "Toggle/button transition speed", 0.58F, 0.05F, 1.0F, 0.01F));
        this.sliderAnimationEnabled = this.addSetting(new BoolSetting("ui_slider_anim_enabled", "Slider Anim Enable", "Enable slider interpolation animation", true));
        this.sliderAnimationSpeed = this.addSetting(new FloatSetting("ui_slider_anim_speed", "Slider Anim Speed", "Slider follow transition speed", 0.62F, 0.05F, 1.0F, 0.01F));
        this.pageAnimationSpeed = this.addSetting(new FloatSetting("ui_page_anim_speed", "Page Anim Speed", "Page browsing transition speed", 0.62F, 0.05F, 1.0F, 0.01F));
        this.listAnimationSpeed = this.addSetting(new FloatSetting("ui_list_anim_speed", "List Anim Speed", "List item enter/exit transition speed", 0.66F, 0.05F, 1.0F, 0.01F));
        this.selectionAnimationSpeed = this.addSetting(new FloatSetting("ui_selection_anim_speed", "Selection Anim Speed", "Selection frame transition speed", 0.68F, 0.05F, 1.0F, 0.01F));
        this.inputAnimationSpeed = this.addSetting(new FloatSetting("ui_input_anim_speed", "Input Anim Speed", "Text input transition speed", 0.64F, 0.05F, 1.0F, 0.01F));
        this.inputAnimationSmooth = this.addSetting(new FloatSetting("ui_input_anim_smooth", "Input Anim Smooth", "Text input transition smoothing", 0.70F, 0.0F, 1.0F, 0.01F));
        this.controlAnimationType = this.addSetting(new EnumSetting<UiAnimation.Type>("ui_anim_type", "Control Anim Type", "Toggle/slider animation type", UiAnimation.Type.class, UiAnimation.Type.EASE_OUT));
        this.inputAnimationType = this.addSetting(new EnumSetting<UiAnimation.Type>("ui_input_anim_type", "Input Anim Type", "Text input animation type", UiAnimation.Type.class, UiAnimation.Type.EASE_OUT));
        this.guiOpenAnimationType = this.addSetting(new EnumSetting<UiAnimation.Type>("ui_open_anim_type", "GUI Open Anim", "Animation used when GUI opens", UiAnimation.Type.class, UiAnimation.Type.EASE_OUT));
        this.guiCloseAnimationType = this.addSetting(new EnumSetting<UiAnimation.Type>("ui_close_anim_type", "GUI Close Anim", "Animation used when GUI closes", UiAnimation.Type.class, UiAnimation.Type.EASE_IN_OUT));
        this.guiSwitchAnimationType = this.addSetting(new EnumSetting<UiAnimation.Type>("ui_switch_anim_type", "GUI Switch Anim", "Animation used when switching GUI pages", UiAnimation.Type.class, UiAnimation.Type.EASE_IN_OUT));
        this.guiBackAnimationType = this.addSetting(new EnumSetting<UiAnimation.Type>("ui_back_anim_type", "GUI Back Anim", "Animation used when going back", UiAnimation.Type.class, UiAnimation.Type.EASE_OUT));
        this.lastCategoryOrdinal = this.addSetting(new IntSetting("last_category_ordinal", "Last Category", "Last selected category ordinal", 0, -1, 128, 1));
        this.lastModuleIndex = this.addSetting(new IntSetting("last_module_index", "Last Module Index", "Last selected module index in category", 0, 0, 512, 1));
        this.lastCategoryScroll = this.addSetting(new IntSetting("last_category_scroll", "Last Category Scroll", "Internal: category scroll position", 0, 0, 512, 1));
        this.lastModuleScroll = this.addSetting(new IntSetting("last_module_scroll", "Last Module Scroll", "Internal: module scroll position", 0, 0, 1024, 1));
        this.lastSettingScroll = this.addSetting(new IntSetting("last_setting_scroll", "Last Setting Scroll", "Internal: module setting scroll position", 0, 0, 2048, 1));
        this.lastCompatSettingScroll = this.addSetting(new IntSetting("last_compat_setting_scroll", "Last Compat Setting Scroll", "Internal: Setting page scroll position", 0, 0, 2048, 1));
        this.settingCenterScale = this.addSetting(new FloatSetting("setting_center_scale", "Setting Center Scale", "Internal: Setting Center window scale", 1.0F, 0.70F, 1.40F, 0.01F));
        this.settingCenterAnchorX = this.addSetting(new FloatSetting("setting_center_anchor_x", "Setting Center Anchor X", "Internal: Setting Center anchor X", 0.5F, 0.0F, 1.0F, 0.001F));
        this.settingCenterAnchorY = this.addSetting(new FloatSetting("setting_center_anchor_y", "Setting Center Anchor Y", "Internal: Setting Center anchor Y", 0.5F, 0.0F, 1.0F, 0.001F));
        this.settingCenterPickerX = this.addSetting(new FloatSetting("setting_center_picker_x", "Setting Center Picker X", "Internal: color picker X", 0.0F, 0.0F, 1.0F, 0.001F));
        this.settingCenterPickerY = this.addSetting(new FloatSetting("setting_center_picker_y", "Setting Center Picker Y", "Internal: color picker Y", 0.0F, 0.0F, 1.0F, 0.001F));
        this.lastCategoryOrdinal.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return false;
            }
        });
        this.lastModuleIndex.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return false;
            }
        });
        this.lastCategoryScroll.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return false;
            }
        });
        this.lastModuleScroll.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return false;
            }
        });
        this.lastSettingScroll.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return false;
            }
        });
        this.lastCompatSettingScroll.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return false;
            }
        });
        Visibility hidden = new Visibility()
        {
            public boolean isVisible()
            {
                return false;
            }
        };
        this.settingCenterScale.visibleWhen(hidden);
        this.settingCenterAnchorX.visibleWhen(hidden);
        this.settingCenterAnchorY.visibleWhen(hidden);
        this.settingCenterPickerX.visibleWhen(hidden);
        this.settingCenterPickerY.visibleWhen(hidden);
        this.accentOverride.visibleWhen(new Visibility()
        {
            public boolean isVisible()
            {
                return ClickGuiModule.this.accentOverrideEnabled.isEnabled();
            }
        });

        appearance.add(this.palette);
        appearance.add(this.locale);
        appearance.add(this.cornerRadius);
        appearance.add(this.panelAlpha);
        appearance.add(this.backdropEnabled);
        appearance.add(this.backdropAlpha);
        appearance.add(this.accentOverrideEnabled);
        appearance.add(this.accentOverride);
        animation.add(this.globalAnimationEnabled);
        animation.add(this.globalAnimationSpeed);
        animation.add(this.globalAnimationSmooth);
        animation.add(this.controlAnimationSpeed);
        animation.add(this.sliderAnimationEnabled);
        animation.add(this.sliderAnimationSpeed);
        animation.add(this.pageAnimationSpeed);
        animation.add(this.listAnimationSpeed);
        animation.add(this.selectionAnimationSpeed);
        animation.add(this.inputAnimationSpeed);
        animation.add(this.inputAnimationSmooth);
        animation.add(this.controlAnimationType);
        animation.add(this.inputAnimationType);
        animation.add(this.guiOpenAnimationType);
        animation.add(this.guiCloseAnimationType);
        animation.add(this.guiSwitchAnimationType);
        animation.add(this.guiBackAnimationType);
    }

    public void onEnable()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (!ClientBootstrap.instance().isNanoAvailable())
        {
            ClientBootstrap.instance().notifyUser(ClientBootstrap.instance().getI18n().translateOrDefault("error.nanovg_unavailable.clickgui", "\u00a7cNanoVG unavailable; ClickGUI disabled."));
            return;
        }

        if (mc != null)
        {
            if (mc.currentScreen instanceof ClickGuiScreen)
            {
                mc.displayGuiScreen(null);
            }
            else
            {
                UiAnimationBus.clearPrefix("chat.");
                mc.displayGuiScreen(new ClickGuiScreen(ClientBootstrap.instance().getModules()));
            }
        }
    }

    @Override
    public void onDisable()
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.currentScreen instanceof ClickGuiScreen)
        {
            mc.displayGuiScreen(null);
        }
        UiAnimationBus.clearPrefix("clickgui.");
    }

    public boolean isActionModule()
    {
        return true;
    }

    public NanoPalette getPalette()
    {
        return this.palette.get();
    }

    public ClientLocale getLocale()
    {
        return this.locale.get();
    }

    public void setLocale(ClientLocale locale)
    {
        this.locale.set(locale == null ? ClientLocale.EN_US : locale);
    }

    public float getCornerRadius()
    {
        return this.cornerRadius.get().floatValue();
    }

    public int getPanelAlpha()
    {
        return this.panelAlpha.get().intValue();
    }

    public boolean isBackdropEnabled()
    {
        return this.backdropEnabled.isEnabled();
    }

    public int getBackdropAlpha()
    {
        return this.backdropAlpha.get().intValue();
    }

    public boolean isAccentOverrideEnabled()
    {
        return this.accentOverrideEnabled.isEnabled();
    }

    public ColorValue getAccentOverride()
    {
        return this.accentOverride.get();
    }

    public boolean isGlobalAnimationEnabled()
    {
        return this.globalAnimationEnabled.isEnabled();
    }

    public void setGlobalAnimationEnabled(boolean enabled)
    {
        this.globalAnimationEnabled.setEnabled(enabled);
    }

    public float getGlobalAnimationSpeed()
    {
        return this.globalAnimationSpeed.get().floatValue();
    }

    public void setGlobalAnimationSpeed(float value)
    {
        this.globalAnimationSpeed.set(Float.valueOf(value));
    }

    public float getGlobalAnimationSmooth()
    {
        return this.globalAnimationSmooth.get().floatValue();
    }

    public void setGlobalAnimationSmooth(float value)
    {
        this.globalAnimationSmooth.set(Float.valueOf(value));
    }

    public float getControlAnimationSpeed()
    {
        return this.controlAnimationSpeed.get().floatValue();
    }

    public float getSliderAnimationSpeed()
    {
        return this.sliderAnimationSpeed.get().floatValue();
    }

    public float getPageAnimationSpeed()
    {
        return this.pageAnimationSpeed.get().floatValue();
    }

    public float getListAnimationSpeed()
    {
        return this.listAnimationSpeed.get().floatValue();
    }

    public float getSelectionAnimationSpeed()
    {
        return this.selectionAnimationSpeed.get().floatValue();
    }

    public float getInputAnimationSpeed()
    {
        return this.inputAnimationSpeed.get().floatValue();
    }

    public float getInputAnimationSmooth()
    {
        return this.inputAnimationSmooth.get().floatValue();
    }

    public boolean isSliderAnimationEnabled()
    {
        return this.sliderAnimationEnabled.isEnabled();
    }

    public void setSliderAnimationEnabled(boolean enabled)
    {
        this.sliderAnimationEnabled.setEnabled(enabled);
    }

    public UiAnimation.Type getControlAnimationType()
    {
        return this.controlAnimationType.get();
    }

    public UiAnimation.Type getInputAnimationType()
    {
        return this.inputAnimationType.get();
    }

    public void setControlAnimationType(UiAnimation.Type type)
    {
        this.controlAnimationType.set(type == null ? UiAnimation.Type.EASE_OUT : type);
    }

    public void cycleControlAnimationType(int direction)
    {
        UiAnimation.Type[] values = UiAnimation.Type.values();
        UiAnimation.Type current = this.getControlAnimationType();
        int index = 0;

        for (int i = 0; i < values.length; ++i)
        {
            if (values[i] == current)
            {
                index = i;
                break;
            }
        }

        int next = (index + (direction < 0 ? -1 : 1)) % values.length;

        if (next < 0)
        {
            next += values.length;
        }

        this.setControlAnimationType(values[next]);
    }

    public UiAnimation.Type getGuiOpenAnimationType()
    {
        return this.guiOpenAnimationType.get();
    }

    public UiAnimation.Type getGuiCloseAnimationType()
    {
        return this.guiCloseAnimationType.get();
    }

    public UiAnimation.Type getGuiSwitchAnimationType()
    {
        return this.guiSwitchAnimationType.get();
    }

    public UiAnimation.Type getGuiBackAnimationType()
    {
        return this.guiBackAnimationType.get();
    }

    public float getSettingCenterScale()
    {
        return this.settingCenterScale.get().floatValue();
    }

    public void setSettingCenterScale(float scale)
    {
        this.settingCenterScale.set(Float.valueOf(Math.max(0.70F, Math.min(1.40F, scale))));
    }

    public float getSettingCenterAnchorX()
    {
        return this.settingCenterAnchorX.get().floatValue();
    }

    public float getSettingCenterAnchorY()
    {
        return this.settingCenterAnchorY.get().floatValue();
    }

    public void setSettingCenterAnchor(float x, float y)
    {
        this.settingCenterAnchorX.set(Float.valueOf(Math.max(0.0F, Math.min(1.0F, x))));
        this.settingCenterAnchorY.set(Float.valueOf(Math.max(0.0F, Math.min(1.0F, y))));
    }

    public float getSettingCenterPickerX()
    {
        return this.settingCenterPickerX.get().floatValue();
    }

    public float getSettingCenterPickerY()
    {
        return this.settingCenterPickerY.get().floatValue();
    }

    public void setSettingCenterPicker(float relX, float relY)
    {
        this.settingCenterPickerX.set(Float.valueOf(Math.max(0.0F, Math.min(1.0F, relX))));
        this.settingCenterPickerY.set(Float.valueOf(Math.max(0.0F, Math.min(1.0F, relY))));
    }

    public int getLastCategoryOrdinal()
    {
        return this.lastCategoryOrdinal.get().intValue();
    }

    public void setLastCategoryOrdinal(int ordinal)
    {
        this.lastCategoryOrdinal.set(Integer.valueOf(ordinal));
    }

    public int getLastModuleIndex()
    {
        return this.lastModuleIndex.get().intValue();
    }

    public void setLastModuleIndex(int index)
    {
        this.lastModuleIndex.set(Integer.valueOf(index));
    }

    public int getLastCategoryScroll()
    {
        return this.lastCategoryScroll.get().intValue();
    }

    public void setLastCategoryScroll(int scroll)
    {
        this.lastCategoryScroll.set(Integer.valueOf(Math.max(0, scroll)));
    }

    public int getLastModuleScroll()
    {
        return this.lastModuleScroll.get().intValue();
    }

    public void setLastModuleScroll(int scroll)
    {
        this.lastModuleScroll.set(Integer.valueOf(Math.max(0, scroll)));
    }

    public int getLastSettingScroll()
    {
        return this.lastSettingScroll.get().intValue();
    }

    public void setLastSettingScroll(int scroll)
    {
        this.lastSettingScroll.set(Integer.valueOf(Math.max(0, scroll)));
    }

    public int getLastCompatSettingScroll()
    {
        return this.lastCompatSettingScroll.get().intValue();
    }

    public void setLastCompatSettingScroll(int scroll)
    {
        this.lastCompatSettingScroll.set(Integer.valueOf(Math.max(0, scroll)));
    }
}
