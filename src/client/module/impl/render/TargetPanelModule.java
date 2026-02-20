package client.module.impl.render;

import client.bridge.MinecraftBridge;
import client.core.ClientBootstrap;
import client.module.Category;
import client.module.Module;
import client.module.impl.combat.KillAuraModule;
import client.module.impl.client.ClickGuiModule;
import client.render.DisplayMetrics;
import client.render.RenderContext2D;
import client.setting.BoolSetting;
import client.setting.FloatSetting;
import client.setting.SettingGroup;
import dwgx.foundation.render.ColorMath;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoPalette;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoThemes;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiAnimProfiles;
import client.ui.template.UiAnimationBus;
import client.ui.template.UiMotion;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.system.MemoryStack;
import java.util.Locale;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_MIDDLE;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_RIGHT;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Compact target information panel, aligned with KillAura target state.
 */
public final class TargetPanelModule extends Module
{
    private static final String ANIM_PREFIX = "hud.target_panel.";
    private static final String CHANNEL_VISIBLE = ANIM_PREFIX + "visible";
    private static final String CHANNEL_HEALTH = ANIM_PREFIX + "health";

    private final FloatSetting posX;
    private final FloatSetting posY;
    private final FloatSetting width;
    private final FloatSetting height;
    private final BoolSetting showDistance;
    private final BoolSetting showHealthBar;
    private String cachedTargetName = "target";
    private float cachedHealth = 0.0F;
    private float cachedHealthCap = 20.0F;
    private float cachedDistance = 0.0F;

    public TargetPanelModule()
    {
        super("target_panel", "TargetPanel", Category.HUD);
        SettingGroup group = this.addGroup("general", "General");
        this.posX = this.addSetting(new FloatSetting("x", "X", "Panel X offset from left", 16.0F, 0.0F, 800.0F, 1.0F));
        this.posY = this.addSetting(new FloatSetting("y", "Y", "Panel Y offset from top", 70.0F, 0.0F, 500.0F, 1.0F));
        this.width = this.addSetting(new FloatSetting("width", "Width", "Panel width", 210.0F, 140.0F, 360.0F, 1.0F));
        this.height = this.addSetting(new FloatSetting("height", "Height", "Panel height", 56.0F, 42.0F, 120.0F, 1.0F));
        this.showDistance = this.addSetting(new BoolSetting("show_distance", "Show Distance", "Display target distance", true));
        this.showHealthBar = this.addSetting(new BoolSetting("show_health_bar", "Show Health Bar", "Render animated health progress bar", true));
        group.add(this.posX);
        group.add(this.posY);
        group.add(this.width);
        group.add(this.height);
        group.add(this.showDistance);
        group.add(this.showHealthBar);
    }

    public void onDisable()
    {
        this.cachedTargetName = "target";
        this.cachedHealth = 0.0F;
        this.cachedHealthCap = 20.0F;
        this.cachedDistance = 0.0F;
        UiAnimationBus.clearPrefix(ANIM_PREFIX);
    }

    public void onRender2D(RenderContext2D context)
    {
        if (context == null || context.getNanoVG() == null || !context.getNanoVG().isFrameActive())
        {
            return;
        }

        EntityLivingBase target = this.resolveTarget();
        boolean activeTarget = target != null && !target.isDead && target.getHealth() > 0.0F;

        if (activeTarget)
        {
            this.cacheTargetState(target);
        }

        ClickGuiModule clickGui = this.resolveClickGuiModule();
        UiAnimProfile animProfile = UiAnimProfiles.hudProfile(clickGui);
        float visibilitySpeed = UiMotion.clamp(animProfile.controlSpeed() * 0.92F + 0.10F, 0.05F, 1.0F);
        float visibility = UiAnimationBus.animateWithSpeed(CHANNEL_VISIBLE, activeTarget ? 1.0F : 0.0F, animProfile, visibilitySpeed);

        if (!activeTarget && visibility <= 0.01F)
        {
            return;
        }

        float healthRatio = this.healthRatio();
        float visualHealth = UiAnimationBus.animateSlider(CHANNEL_HEALTH, healthRatio, animProfile);
        DisplayMetrics metrics = context.getMetrics();
        float screenW = metrics == null ? 0.0F : (float)metrics.getWindowWidth();
        float screenH = metrics == null ? 0.0F : (float)metrics.getWindowHeight();
        float panelW = this.width.get().floatValue();
        float panelH = this.height.get().floatValue();
        float x = this.posX.get().floatValue();
        float y = this.posY.get().floatValue();

        if (screenW > 0.0F)
        {
            x = Math.min(Math.max(0.0F, x), Math.max(0.0F, screenW - panelW));
        }

        if (screenH > 0.0F)
        {
            y = Math.min(Math.max(0.0F, y), Math.max(0.0F, screenH - panelH));
        }

        float visible = UiMotion.clamp01(visibility);
        float alpha = UiMotion.clamp(0.18F + visible * 0.82F, 0.0F, 1.0F);
        float lift = (1.0F - visible) * 8.0F;
        float drawY = y + lift;
        NanoTheme theme = this.resolveTheme(clickGui);
        long vg = context.getNanoVG().getHandle();
        NanoFontBook.ensureLoaded(vg);
        int regular = NanoFontBook.uiRegular();
        int bold = NanoFontBook.uiBold();
        String hpText = String.format(Locale.ROOT, "%.1f / %.1f", Float.valueOf(this.cachedHealth), Float.valueOf(this.cachedHealthCap));
        String hpPercent = String.format(Locale.ROOT, "%.0f%%", Float.valueOf(healthRatio * 100.0F));
        String stateText = activeTarget ? "LOCKED" : "LOST";

        try (MemoryStack stack = stackPush())
        {
            float radius = NanoRenderUtils.stableRadius(10.0F, panelW, panelH);
            int cardTop = NanoRenderUtils.mulAlpha(theme.cardArgb(), alpha);
            int cardBottom = NanoRenderUtils.mulAlpha(theme.cardAltArgb(), alpha);
            int border = NanoRenderUtils.mulAlpha(theme.windowBorderArgb(), alpha);
            NanoRenderUtils.drawPanel(vg, stack, x, drawY, panelW, panelH, radius, cardTop, border, 1.0F, Math.round(96.0F * alpha));
            NanoRenderUtils.fillRoundedRectGradient(vg, stack, x, drawY, panelW, panelH * 0.52F, radius, cardTop, cardBottom, true);
            NanoRenderUtils.fillRoundedRect(vg, x + 6.0F, drawY + 6.0F, 3.0F, panelH - 12.0F, 1.5F, NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(theme.accentArgb(), alpha * 0.92F)));

            NanoRenderUtils.drawLabel(vg, stack, bold, x + 14.0F, drawY + 15.0F, 14.2F, this.cachedTargetName, 236, 241, 248, Math.round(236.0F * alpha), NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            NanoRenderUtils.drawLabel(vg, stack, regular, x + panelW - 10.0F, drawY + 14.8F, 11.8F, hpText, 173, 189, 207, Math.round(228.0F * alpha), NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE);
            NanoRenderUtils.drawLabel(vg, stack, regular, x + 14.0F, drawY + 29.0F, 10.6F, stateText, 133, 152, 173, Math.round(206.0F * alpha), NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            NanoRenderUtils.drawLabel(vg, stack, bold, x + panelW - 10.0F, drawY + 29.0F, 11.2F, hpPercent, 202, 219, 236, Math.round(220.0F * alpha), NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE);

            if (this.showDistance.isEnabled())
            {
                String distText = String.format(Locale.ROOT, "Dist %.2f", Float.valueOf(this.cachedDistance));
                NanoRenderUtils.drawLabel(vg, stack, regular, x + 14.0F, drawY + panelH - 11.5F, 10.8F, distText, 136, 210, 255, Math.round(220.0F * alpha), NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            }

            if (this.showHealthBar.isEnabled())
            {
                float barX = x + 14.0F;
                float barW = Math.max(24.0F, panelW - 28.0F);
                float barY = drawY + panelH - 22.0F;
                float barH = 7.0F;
                float filled = barW * UiMotion.clamp01(visualHealth);
                int barBg = NanoRenderUtils.mulAlpha(theme.controlArgb(), alpha * 0.92F);
                NanoRenderUtils.fillRoundedRect(vg, barX, barY, barW, barH, 3.5F, NanoRenderUtils.argb(stack, barBg));

                if (filled > 0.1F)
                {
                    int healthy = ColorMath.mixArgb(theme.dangerArgb(), theme.successArgb(), UiMotion.clamp01(visualHealth));
                    int head = ColorMath.mixArgb(theme.accentArgb(), healthy, 0.55F);
                    NanoRenderUtils.fillRoundedRectGradient(vg, stack, barX, barY, filled, barH, 3.5F, NanoRenderUtils.mulAlpha(head, alpha), NanoRenderUtils.mulAlpha(healthy, alpha), false);
                }
            }
        }
    }

    private void cacheTargetState(EntityLivingBase target)
    {
        String name = target.getName();
        this.cachedTargetName = name == null || name.trim().isEmpty() ? "target" : name;
        this.cachedHealth = Math.max(0.0F, target.getHealth() + target.getAbsorptionAmount());
        this.cachedHealthCap = Math.max(1.0F, target.getMaxHealth() + target.getAbsorptionAmount());
        this.cachedDistance = MinecraftBridge.shared().hasPlayer() ? MinecraftBridge.shared().localPlayer().getDistanceToEntity(target) : 0.0F;
    }

    private float healthRatio()
    {
        return UiMotion.clamp01(this.cachedHealth / Math.max(1.0F, this.cachedHealthCap));
    }

    private ClickGuiModule resolveClickGuiModule()
    {
        ClientBootstrap bootstrap = ClientBootstrap.instance();

        if (bootstrap == null || bootstrap.getModules() == null)
        {
            return null;
        }

        client.module.Module module = bootstrap.getModules().getById("click_gui");
        return module instanceof ClickGuiModule ? (ClickGuiModule)module : null;
    }

    private NanoTheme resolveTheme(ClickGuiModule clickGui)
    {
        if (clickGui == null)
        {
            return NanoThemes.create(NanoPalette.COBALT, 220, 40, 10.0F, null);
        }

        Integer accent = null;

        if (clickGui.isAccentOverrideEnabled() && clickGui.getAccentOverride() != null)
        {
            accent = Integer.valueOf(clickGui.getAccentOverride().toArgb());
        }

        int backdrop = clickGui.isBackdropEnabled() ? Math.min(64, clickGui.getBackdropAlpha()) : 0;
        return NanoThemes.create(clickGui.getPalette(), clickGui.getPanelAlpha(), backdrop, clickGui.getCornerRadius(), accent);
    }

    private EntityLivingBase resolveTarget()
    {
        ClientBootstrap bootstrap = ClientBootstrap.instance();

        if (bootstrap == null || bootstrap.getModules() == null)
        {
            return null;
        }

        client.module.Module module = bootstrap.getModules().getById("killaura");

        if (!(module instanceof KillAuraModule))
        {
            return null;
        }

        return ((KillAuraModule)module).getCurrentTarget();
    }
}
