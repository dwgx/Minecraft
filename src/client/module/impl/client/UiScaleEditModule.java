package client.module.impl.client;

import client.core.ClientBootstrap;
import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.EnumSetting;
import client.setting.FloatSetting;
import client.setting.SettingGroup;
import client.ui.UIScaleEditScreen;
import client.ui.template.UiAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public final class UiScaleEditModule extends Module
{
    public enum UiTarget
    {
        CLICK_GUI,
        HUD_EDIT;

        public String displayName()
        {
            return this == CLICK_GUI ? "ClickGUI" : "HudEdit";
        }
    }

    private final EnumSetting<UiTarget> editTarget;
    private final FloatSetting clickGuiScale;
    private final FloatSetting clickGuiMotion;
    private final FloatSetting clickGuiAnchorX;
    private final FloatSetting clickGuiAnchorY;
    private final FloatSetting hudEditScale;
    private final FloatSetting hudEditMotion;
    private final FloatSetting hudEditAnchorX;
    private final FloatSetting hudEditAnchorY;
    private final BoolSetting animationEnabled;
    private final FloatSetting animationSpeed;
    private final FloatSetting animationSmooth;
    private final EnumSetting<UiAnimation.Type> animationType;

    public UiScaleEditModule()
    {
        super("ui_scale_edit", "UIScaleEdit", Category.CLIENT);

        SettingGroup editor = this.addGroup("editor", "Editor");
        this.editTarget = this.addSetting(new EnumSetting<UiTarget>("edit_target", "Edit Target", "Which UI profile this editor is controlling", UiTarget.class, UiTarget.CLICK_GUI));
        editor.add(this.editTarget);

        SettingGroup clickGui = this.addGroup("click_gui", "ClickGUI");
        this.clickGuiScale = this.addSetting(new FloatSetting("click_gui_scale", "ClickGUI Scale", "ClickGUI scale", 1.0F, 0.20F, 1.85F, 0.01F));
        this.clickGuiMotion = this.addSetting(new FloatSetting("click_gui_motion", "ClickGUI Motion", "ClickGUI interpolation speed", 0.30F, 0.05F, 1.0F, 0.01F));
        this.clickGuiAnchorX = this.addSetting(new FloatSetting("click_gui_anchor_x", "ClickGUI Anchor X", "ClickGUI normalized anchor x", 0.5F, 0.0F, 1.0F, 0.001F));
        this.clickGuiAnchorY = this.addSetting(new FloatSetting("click_gui_anchor_y", "ClickGUI Anchor Y", "ClickGUI normalized anchor y", 0.5F, 0.0F, 1.0F, 0.001F));
        clickGui.add(this.clickGuiScale);
        clickGui.add(this.clickGuiMotion);
        clickGui.add(this.clickGuiAnchorX);
        clickGui.add(this.clickGuiAnchorY);

        SettingGroup hudEdit = this.addGroup("hud_edit", "HudEdit");
        this.hudEditScale = this.addSetting(new FloatSetting("hud_edit_scale", "HudEdit Scale", "HudEdit scale", 1.0F, 0.20F, 1.85F, 0.01F));
        this.hudEditMotion = this.addSetting(new FloatSetting("hud_edit_motion", "HudEdit Motion", "HudEdit interpolation speed", 0.30F, 0.05F, 1.0F, 0.01F));
        this.hudEditAnchorX = this.addSetting(new FloatSetting("hud_edit_anchor_x", "HudEdit Anchor X", "HudEdit normalized anchor x", 0.5F, 0.0F, 1.0F, 0.001F));
        this.hudEditAnchorY = this.addSetting(new FloatSetting("hud_edit_anchor_y", "HudEdit Anchor Y", "HudEdit normalized anchor y", 0.5F, 0.0F, 1.0F, 0.001F));
        hudEdit.add(this.hudEditScale);
        hudEdit.add(this.hudEditMotion);
        hudEdit.add(this.hudEditAnchorX);
        hudEdit.add(this.hudEditAnchorY);

        SettingGroup animation = this.addGroup("animation", "Animation");
        this.animationEnabled = this.addSetting(new BoolSetting("ui_anim_enabled", "UI Animation", "Enable shared UI window animation", true));
        this.animationSpeed = this.addSetting(new FloatSetting("ui_anim_speed", "Animation Speed", "Shared animation speed", 0.56F, 0.05F, 1.0F, 0.01F));
        this.animationSmooth = this.addSetting(new FloatSetting("ui_anim_smooth", "Animation Smooth", "Shared animation smoothing", 0.62F, 0.0F, 1.0F, 0.01F));
        this.animationType = this.addSetting(new EnumSetting<UiAnimation.Type>("ui_anim_type", "Animation Type", "Shared animation type", UiAnimation.Type.class, UiAnimation.Type.EASE_OUT));
        animation.add(this.animationEnabled);
        animation.add(this.animationSpeed);
        animation.add(this.animationSmooth);
        animation.add(this.animationType);
    }

    public void onEnable()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (!ClientBootstrap.instance().isNanoAvailable())
        {
            ClientBootstrap.instance().notifyUser(ClientBootstrap.instance().getI18n().translateOrDefault("error.nanovg_unavailable.uiscaleedit", "\u00a7cNanoVG unavailable; UIScaleEdit disabled."));
            return;
        }

        if (mc != null)
        {
            if (mc.currentScreen instanceof UIScaleEditScreen)
            {
                mc.displayGuiScreen(null);
            }
            else
            {
                GuiScreen parent = mc.currentScreen;
                mc.displayGuiScreen(new UIScaleEditScreen(this, parent));
            }
        }
    }

    public boolean isActionModule()
    {
        return true;
    }

    public UiTarget getEditTarget()
    {
        return this.editTarget.get();
    }

    public void setEditTarget(UiTarget target)
    {
        this.editTarget.set(target == null ? UiTarget.CLICK_GUI : target);
    }

    public float getUiScale(UiTarget target)
    {
        return this.scaleSetting(target).get().floatValue();
    }

    public void setUiScale(UiTarget target, float value)
    {
        this.scaleSetting(target).set(Float.valueOf(value));
    }

    public float getMotionSpeed(UiTarget target)
    {
        return this.motionSetting(target).get().floatValue();
    }

    public void setMotionSpeed(UiTarget target, float value)
    {
        this.motionSetting(target).set(Float.valueOf(value));
    }

    public float getWindowAnchorX(UiTarget target)
    {
        return this.anchorXSetting(target).get().floatValue();
    }

    public float getWindowAnchorY(UiTarget target)
    {
        return this.anchorYSetting(target).get().floatValue();
    }

    public void setWindowAnchor(UiTarget target, float anchorX, float anchorY)
    {
        this.anchorXSetting(target).set(Float.valueOf(anchorX));
        this.anchorYSetting(target).set(Float.valueOf(anchorY));
    }

    public float getUiScaleMin()
    {
        return this.clickGuiScale.getMin();
    }

    public float getUiScaleMax()
    {
        return this.clickGuiScale.getMax();
    }

    public float getMotionSpeedMin()
    {
        return this.clickGuiMotion.getMin();
    }

    public float getMotionSpeedMax()
    {
        return this.clickGuiMotion.getMax();
    }

    public boolean isAnimationEnabled()
    {
        return this.animationEnabled.isEnabled();
    }

    public void setAnimationEnabled(boolean enabled)
    {
        this.animationEnabled.setEnabled(enabled);
    }

    public float getAnimationSpeed()
    {
        return this.animationSpeed.get().floatValue();
    }

    public void setAnimationSpeed(float value)
    {
        this.animationSpeed.set(Float.valueOf(value));
    }

    public float getAnimationSpeedMin()
    {
        return this.animationSpeed.getMin();
    }

    public float getAnimationSpeedMax()
    {
        return this.animationSpeed.getMax();
    }

    public float getAnimationSmooth()
    {
        return this.animationSmooth.get().floatValue();
    }

    public void setAnimationSmooth(float value)
    {
        this.animationSmooth.set(Float.valueOf(value));
    }

    public float getAnimationSmoothMin()
    {
        return this.animationSmooth.getMin();
    }

    public float getAnimationSmoothMax()
    {
        return this.animationSmooth.getMax();
    }

    public UiAnimation.Type getAnimationType()
    {
        return this.animationType.get();
    }

    public void setAnimationType(UiAnimation.Type type)
    {
        this.animationType.set(type == null ? UiAnimation.Type.EASE_OUT : type);
    }

    public void cycleAnimationType(int direction)
    {
        UiAnimation.Type[] values = UiAnimation.Type.values();
        int index = 0;
        UiAnimation.Type current = this.getAnimationType();

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

        this.setAnimationType(values[next]);
    }

    // compatibility helpers for existing call-sites
    public float getUiScale()
    {
        return this.getUiScale(this.getEditTarget());
    }

    public void setUiScale(float value)
    {
        this.setUiScale(this.getEditTarget(), value);
    }

    public float getMotionSpeed()
    {
        return this.getMotionSpeed(this.getEditTarget());
    }

    public void setMotionSpeed(float value)
    {
        this.setMotionSpeed(this.getEditTarget(), value);
    }

    public float getWindowAnchorX()
    {
        return this.getWindowAnchorX(this.getEditTarget());
    }

    public float getWindowAnchorY()
    {
        return this.getWindowAnchorY(this.getEditTarget());
    }

    public void setWindowAnchor(float anchorX, float anchorY)
    {
        this.setWindowAnchor(this.getEditTarget(), anchorX, anchorY);
    }

    private FloatSetting scaleSetting(UiTarget target)
    {
        return target == UiTarget.HUD_EDIT ? this.hudEditScale : this.clickGuiScale;
    }

    private FloatSetting motionSetting(UiTarget target)
    {
        return target == UiTarget.HUD_EDIT ? this.hudEditMotion : this.clickGuiMotion;
    }

    private FloatSetting anchorXSetting(UiTarget target)
    {
        return target == UiTarget.HUD_EDIT ? this.hudEditAnchorX : this.clickGuiAnchorX;
    }

    private FloatSetting anchorYSetting(UiTarget target)
    {
        return target == UiTarget.HUD_EDIT ? this.hudEditAnchorY : this.clickGuiAnchorY;
    }
}
