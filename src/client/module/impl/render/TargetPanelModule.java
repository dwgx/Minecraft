package client.module.impl.render;

import client.core.ClientBootstrap;
import client.module.Category;
import client.module.Module;
import client.module.impl.combat.KillAuraModule;
import client.render.DisplayMetrics;
import client.render.RenderContext2D;
import client.setting.BoolSetting;
import client.setting.FloatSetting;
import client.setting.SettingGroup;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_MIDDLE;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_RIGHT;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Compact target information panel, aligned with KillAura target state.
 */
public final class TargetPanelModule extends Module
{
    private final FloatSetting posX;
    private final FloatSetting posY;
    private final FloatSetting width;
    private final FloatSetting height;
    private final BoolSetting showDistance;
    private final BoolSetting showHealthBar;
    private float animatedHealth = -1.0F;

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
        this.animatedHealth = -1.0F;
    }

    public void onRender2D(RenderContext2D context)
    {
        if (context == null || context.getNanoVG() == null || !context.getNanoVG().isFrameActive())
        {
            return;
        }

        EntityLivingBase target = this.resolveTarget();

        if (target == null || target.isDead || target.getHealth() <= 0.0F)
        {
            this.animatedHealth = -1.0F;
            return;
        }

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

        long vg = context.getNanoVG().getHandle();
        NanoFontBook.ensureLoaded(vg);
        int regular = NanoFontBook.uiRegular();
        int bold = NanoFontBook.uiBold();
        float health = Math.max(0.0F, target.getHealth() + target.getAbsorptionAmount());
        float healthCap = Math.max(1.0F, target.getMaxHealth() + target.getAbsorptionAmount());
        float ratio = Math.max(0.0F, Math.min(1.0F, health / healthCap));

        if (this.animatedHealth < 0.0F)
        {
            this.animatedHealth = ratio;
        }
        else
        {
            this.animatedHealth += (ratio - this.animatedHealth) * 0.35F;
        }

        try (MemoryStack stack = stackPush())
        {
            NanoRenderUtils.drawPanel(vg, stack, x, y, panelW, panelH, 9.0F, 0xD61A1D24, 0x882E3440, 1.0F, 92);

            String name = target.getName() == null ? "target" : target.getName();
            String hpText = String.format("%.1f / %.1f", Float.valueOf(health), Float.valueOf(healthCap));
            NanoRenderUtils.drawLabel(vg, stack, bold, x + 10.0F, y + 15.0F, 14.0F, name, 238, 241, 248, 235, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            NanoRenderUtils.drawLabel(vg, stack, regular, x + panelW - 10.0F, y + 15.0F, 12.0F, hpText, 170, 184, 202, 230, NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE);

            if (this.showDistance.isEnabled())
            {
                Minecraft mc = Minecraft.getMinecraft();
                float distance = mc == null || mc.thePlayer == null ? 0.0F : mc.thePlayer.getDistanceToEntity(target);
                String distText = String.format("Dist %.2f", Float.valueOf(distance));
                NanoRenderUtils.drawLabel(vg, stack, regular, x + 10.0F, y + panelH - 12.0F, 11.5F, distText, 126, 211, 255, 220, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            }

            if (this.showHealthBar.isEnabled())
            {
                float barX = x + 10.0F;
                float barY = y + panelH - 24.0F;
                float barW = Math.max(14.0F, panelW - 20.0F);
                float barH = 8.0F;
                float filled = barW * Math.max(0.0F, Math.min(1.0F, this.animatedHealth));
                NanoRenderUtils.fillRoundedRect(vg, barX, barY, barW, barH, 4.0F, NanoRenderUtils.argb(stack, 0xA42B313D));

                if (filled > 0.5F)
                {
                    NanoRenderUtils.fillRoundedRectGradient(vg, stack, barX, barY, filled, barH, 4.0F, 0xFF3DB7FF, 0xFF35F2A2, false);
                }
            }
        }
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
