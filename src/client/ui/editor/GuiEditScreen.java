package client.ui.editor;

import client.core.ClientBootstrap;
import client.module.Module;
import client.module.impl.client.ClickGuiModule;
import client.module.impl.client.UiScaleEditModule;
import client.render.RenderContext2D;
import client.ui.NanoRenderableScreen;
import client.ui.layout.UiRect;
import client.ui.template.NanoScreenKit;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiAnimProfiles;
import client.ui.template.UiAnimationBus;
import client.ui.template.UiLayoutProfile;
import client.ui.template.UiMotion;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoThemes;
import dwgx.nano.NanoUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Visual GUI layout editor (GuiEdit).
 * Phase 1: selector popup. Phase 2: full-size blueprint editor with draggable edges.
 * Smooth cross-fade transition between phases. Right-click edge to reset.
 * Animation speed synced from ClickGuiModule settings.
 */
public final class GuiEditScreen extends GuiScreen implements NanoRenderableScreen
{
    private static final GuiBlueprint[] BLUEPRINTS = {
        new ClickGuiBlueprint(),
        new SettingsBlueprint(),
        new AccountBlueprint(),
        new ChatOverlayBlueprint()
    };

    private static final String ANIM_PREFIX = "guiedit";
    private static final float SCREEN_MARGIN = 8.0F;

    private enum Phase { SELECTOR, EDITOR }

    private Phase phase = Phase.SELECTOR;
    private Phase targetPhase = Phase.SELECTOR;
    private int selectedIndex = -1;
    private GuiBlueprint activeBlueprint;

    // Phase transition (0=selector, 1=editor)
    private float phaseT = 0.0F;

    private final UiScaleEditModule module;
    private final GuiScreen parentScreen;

    private String hoveredEdgeId;
    private String draggingEdgeId;
    private int mouseX;
    private int mouseY;

    private float selectorFade = 0.0F;
    private int selectorHover = -1;

    // ── Per-frame cache (avoids re-allocation during drag) ──
    private UiAnimProfile frameAnim;
    private UiRect cachedPreviewWin;
    private float cachedScale;
    private List<GuiBlueprint.DragEdge> cachedEdges;
    private GuiBlueprint.DragEdge cachedDragEdge;

    public GuiEditScreen(UiScaleEditModule module)
    {
        this(module, null);
    }

    public GuiEditScreen(UiScaleEditModule module, GuiScreen parentScreen)
    {
        this.module = module;
        this.parentScreen = parentScreen;
    }

    public boolean doesGuiPauseGame() { return false; }

    public void initGui()
    {
        super.initGui();
        this.selectorFade = 0.0F;
        this.phaseT = 0.0F;
        this.phase = Phase.SELECTOR;
        this.targetPhase = Phase.SELECTOR;
    }

    public void onGuiClosed()
    {
        super.onGuiClosed();
        UiAnimationBus.clearPrefix(ANIM_PREFIX);
        markLayoutDirty();
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == 1)
        {
            if (this.targetPhase == Phase.EDITOR)
            {
                this.targetPhase = Phase.SELECTOR;
                this.draggingEdgeId = null;
                this.hoveredEdgeId = null;
                this.selectorFade = 0.0F;
                return;
            }
            this.goBack();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        // Ignore during transition
        if (this.phaseT > 0.05F && this.phaseT < 0.95F) return;

        if (mouseButton == 1)
        {
            handleRightClick(mouseX, mouseY);
            return;
        }

        if (mouseButton != 0) return;

        if (this.phase == Phase.SELECTOR)
        {
            handleSelectorClick(mouseX, mouseY);
        }
        else
        {
            handleEditorClick(mouseX, mouseY);
        }
    }

    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick)
    {
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        if (this.phase == Phase.EDITOR && this.draggingEdgeId != null && this.activeBlueprint != null)
        {
            // Use cached edge from render frame — avoids recomputing edges every mouse move
            GuiBlueprint.DragEdge edge = this.cachedDragEdge;
            if (edge != null && edge.id.equals(this.draggingEdgeId))
            {
                UiLayoutProfile lp = liveLayout();
                UiRect pw = this.cachedPreviewWin;
                float k = this.cachedScale;
                if (pw != null)
                {
                    float pos = edge.vertical ? mouseX : mouseY;
                    edge.handler.apply(lp, pos, pw, k);
                    markLayoutDirty();
                }
            }
        }
    }

    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        this.draggingEdgeId = null;
        this.cachedDragEdge = null;
    }

    // ── Right-click to reset hovered edge ──

    private void handleRightClick(int mouseX, int mouseY)
    {
        if (this.phase != Phase.EDITOR || this.activeBlueprint == null) return;

        // Use cached edges from last render frame
        List<GuiBlueprint.DragEdge> edges = this.cachedEdges;
        if (edges == null)
        {
            UiRect pw = editorPreviewWindow();
            edges = this.activeBlueprint.computeEdges(pw, liveLayout(), editorScale(pw));
        }
        String hitId = GuiPreviewRenderer.hitTestEdge(edges, mouseX, mouseY);
        if (hitId == null) return;

        resetEdgeToDefault(hitId, liveLayout());
        markLayoutDirty();
    }

    private void resetEdgeToDefault(String edgeId, UiLayoutProfile lp)
    {
        switch (edgeId)
        {
            case "sidebar_w":   lp.setClickGuiSidebarRatio(0.24F); break;
            case "modules_h":   lp.setClickGuiModulesRatio(0.31F); break;
            case "value_col":   lp.setValueColWidth(132.0F); break;
            case "gap":         lp.setGapMajor(14.0F); break;
            case "row_cat":     lp.setRowCategory(24.0F); break;
            case "row_mod":     lp.setRowModule(34.0F); break;
            case "row_set":     lp.setRowSetting(26.0F); break;
            case "win_w":
                if (this.activeBlueprint != null)
                {
                    if ("settings".equals(this.activeBlueprint.key())) lp.setSettingsWidth(900.0F);
                    else if ("account".equals(this.activeBlueprint.key())) lp.setAccountWidth(580.0F);
                }
                break;
            case "win_h":
                if (this.activeBlueprint != null)
                {
                    if ("settings".equals(this.activeBlueprint.key())) lp.setSettingsHeight(620.0F);
                    else if ("account".equals(this.activeBlueprint.key())) lp.setAccountHeight(460.0F);
                }
                break;
            default: break;
            case "chat_server_w": lp.setChatServerListWidth(56.0F); break;
            case "chat_channel_r": lp.setChatChannelRatio(0.20F); break;
            case "chat_input_h": lp.setChatInputBarHeight(52.0F); break;
        }
    }

    // ── Selector click — uses animated positions ──

    private void handleSelectorClick(int mouseX, int mouseY)
    {
        float fade = this.selectorFade;
        if (fade < 0.5F) return;

        float centerX = this.width * 0.5F;
        float centerY = this.height * 0.5F;
        float cardW = Math.min(420.0F, this.width - 40.0F);
        float cardH = Math.min(320.0F, this.height - 40.0F);
        float animScale = 0.92F + 0.08F * fade;
        float animLift = (1.0F - fade) * 12.0F;
        float aw = cardW * animScale;
        float ah = cardH * animScale;
        float ax = centerX - aw * 0.5F;
        float ay = centerY - ah * 0.5F + animLift;

        // Back button
        float btnH = 28.0F;
        float backY = ay + ah - 14.0F - btnH;
        if (new UiRect(ax + 16.0F, backY, 80.0F, btnH).contains(mouseX, mouseY))
        {
            this.goBack();
            return;
        }

        // Blueprint options
        float optionY = ay + 68.0F;
        float optionH = 48.0F;
        float optionGap = 6.0F;
        float optionW = aw - 32.0F;

        for (int i = 0; i < BLUEPRINTS.length; i++)
        {
            float reveal = UiMotion.clamp01((fade - i * 0.08F) * 2.0F);
            if (reveal < 0.5F) continue;
            float itemLift = (1.0F - reveal) * 6.0F;
            float y = optionY + i * (optionH + optionGap) + itemLift;
            if (new UiRect(ax + 16.0F, y, optionW, optionH).contains(mouseX, mouseY))
            {
                this.selectedIndex = i;
                this.activeBlueprint = BLUEPRINTS[i];
                this.targetPhase = Phase.EDITOR;
                this.draggingEdgeId = null;
                this.hoveredEdgeId = null;
                return;
            }
        }
    }

    private void handleEditorClick(int mouseX, int mouseY)
    {
        UiRect backBtn = editorBackButton();
        if (backBtn.contains(mouseX, mouseY))
        {
            this.targetPhase = Phase.SELECTOR;
            this.draggingEdgeId = null;
            this.selectorFade = 0.0F;
            return;
        }

        UiRect resetBtn = editorResetButton();
        if (resetBtn.contains(mouseX, mouseY))
        {
            resetLayoutDefaults();
            return;
        }

        if (this.activeBlueprint != null)
        {
            // Use cached edges from last render frame
            List<GuiBlueprint.DragEdge> edges = this.cachedEdges;
            if (edges == null)
            {
                UiRect pw = editorPreviewWindow();
                edges = this.activeBlueprint.computeEdges(pw, liveLayout(), editorScale(pw));
            }
            String hitId = GuiPreviewRenderer.hitTestEdge(edges, mouseX, mouseY);
            if (hitId != null) this.draggingEdgeId = hitId;
        }
    }

    // ── Rendering ──

    public void renderNano(RenderContext2D context)
    {
        if (context == null || context.getNanoVG() == null) return;
        long vg = context.getNanoVG().getHandle();
        if (vg == 0L) return;

        ClickGuiModule clickGui = resolveClickGuiModule();
        NanoTheme theme = resolveTheme(clickGui);
        this.frameAnim = resolveAnimProfile(clickGui);
        NanoFontBook.ensureLoaded(vg);

        // Phase transition
        float phaseTarget = (this.targetPhase == Phase.EDITOR) ? 1.0F : 0.0F;
        this.phaseT = UiAnimationBus.animateControl(ANIM_PREFIX + ".phase", phaseTarget, this.frameAnim);

        if (this.phaseT > 0.98F && this.targetPhase == Phase.EDITOR)
        {
            this.phase = Phase.EDITOR;
        }
        else if (this.phaseT < 0.02F && this.targetPhase == Phase.SELECTOR)
        {
            this.phase = Phase.SELECTOR;
            if (this.phaseT < 0.01F)
            {
                this.activeBlueprint = null;
                this.selectedIndex = -1;
                this.cachedEdges = null;
                this.cachedDragEdge = null;
                this.cachedPreviewWin = null;
            }
        }

        try (MemoryStack stack = stackPush())
        {
            // Always draw backdrop first — guarantees no black frame even if
            // selector/editor rendering fails
            if (clickGui != null && clickGui.isBackdropEnabled())
            {
                NanoUi.drawBackdrop(vg, stack, this.width, this.height, theme);
            }
            else
            {
                NanoRenderUtils.fillRect(vg, 0, 0, this.width, this.height,
                    NanoRenderUtils.argb(stack, 0x88000000));
            }

            float selectorAlpha = 1.0F - this.phaseT;
            float editorAlpha = this.phaseT;

            try
            {
                if (selectorAlpha > 0.01F)
                {
                    renderSelector(vg, stack, theme, selectorAlpha);
                }
                if (editorAlpha > 0.01F && this.activeBlueprint != null)
                {
                    renderEditor(vg, stack, theme, editorAlpha);
                }
            }
            catch (Throwable ignored)
            {
                // Backdrop is already drawn — swallow to prevent black frame
            }
        }
    }

    // ── Selector rendering ──

    private void renderSelector(long vg, MemoryStack stack, NanoTheme theme, float phaseAlpha)
    {
        int regular = NanoFontBook.uiRegular();
        int bold = NanoFontBook.uiBold();
        UiAnimProfile anim = this.frameAnim;

        this.selectorFade = UiMotion.clamp01(this.selectorFade + 0.06F);
        float fade = this.selectorFade * phaseAlpha;
        if (fade < 0.01F) return;

        float cx = this.width * 0.5F;
        float cy = this.height * 0.5F;
        float cardW = Math.min(420.0F, this.width - 40.0F);
        float cardH = Math.min(320.0F, this.height - 40.0F);

        float animScale = 0.92F + 0.08F * this.selectorFade;
        float animLift = (1.0F - this.selectorFade) * 12.0F;
        float phaseOutScale = 1.0F - (1.0F - phaseAlpha) * 0.06F;
        float phaseOutLift = (1.0F - phaseAlpha) * -20.0F;
        float aw = cardW * animScale * phaseOutScale;
        float ah = cardH * animScale * phaseOutScale;
        float ax = cx - aw * 0.5F;
        float ay = cy - ah * 0.5F + animLift + phaseOutLift;

        // Card shadow
        NanoRenderUtils.fillRoundedRect(vg, ax + 3, ay + 5, aw, ah,
            theme.windowRadius() + 2,
            NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(0xFF000000, (int)(fade * 40))));

        // Card background
        NanoRenderUtils.fillRoundedRectGradient(vg, stack, ax, ay, aw, ah,
            theme.windowRadius(),
            NanoRenderUtils.mulAlpha(theme.windowTopArgb(), fade),
            NanoRenderUtils.mulAlpha(theme.windowBottomArgb(), fade), true);
        NanoRenderUtils.strokeRoundedRect(vg, ax, ay, aw, ah,
            theme.windowRadius(), 1.0F,
            NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(theme.windowBorderArgb(), fade)));

        // Title
        NanoUi.drawCenterText(vg, stack, bold, cx, ay + 30.0F,
            16.0F, NanoRenderUtils.mulAlpha(theme.textArgb(), fade),
            tr("guiedit.selector.title", "GUI Editor"));

        // Accent underline
        NanoRenderUtils.fillRoundedRect(vg, cx - 20.0F, ay + 38.0F, 40.0F, 2.0F, 1.0F,
            NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(theme.accentArgb(), fade * 0.6F)));

        // Subtitle
        NanoUi.drawCenterText(vg, stack, regular, cx, ay + 52.0F,
            11.0F, NanoRenderUtils.mulAlpha(theme.textMutedArgb(), fade),
            tr("guiedit.selector.subtitle", "Select a GUI to customize"));

        // Blueprint options
        float optionY = ay + 68.0F;
        float optionH = 48.0F;
        float optionGap = 6.0F;
        float optionW = aw - 32.0F;
        this.selectorHover = -1;

        for (int i = 0; i < BLUEPRINTS.length; i++)
        {
            float reveal = UiMotion.clamp01((this.selectorFade - i * 0.08F) * 2.0F) * phaseAlpha;
            float itemLift = (1.0F - reveal) * 6.0F;
            float y = optionY + i * (optionH + optionGap) + itemLift;
            UiRect drawRect = new UiRect(ax + 16.0F, y, optionW, optionH);

            boolean hovered = drawRect.contains(this.mouseX, this.mouseY) && phaseAlpha > 0.8F;
            if (hovered) this.selectorHover = i;

            float hoverAnim = UiAnimationBus.animateControl(
                ANIM_PREFIX + ".sel." + i, hovered ? 1.0F : 0.0F, anim);

            int fill = NanoScreenKit.mixArgb(theme.controlArgb(), theme.controlHoverArgb(),
                UiMotion.clamp01(hoverAnim * 0.7F));
            fill = NanoRenderUtils.mulAlpha(fill, reveal);
            int border = NanoScreenKit.mixArgb(
                NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 60),
                NanoRenderUtils.withAlpha(theme.accentArgb(), 140),
                UiMotion.clamp01(hoverAnim * 0.6F));
            border = NanoRenderUtils.mulAlpha(border, reveal);

            NanoRenderUtils.fillRoundedRect(vg, drawRect.x, drawRect.y, drawRect.w, drawRect.h,
                theme.controlRadius(), NanoRenderUtils.argb(stack, fill));
            NanoRenderUtils.strokeRoundedRect(vg, drawRect.x, drawRect.y, drawRect.w, drawRect.h,
                theme.controlRadius(), 1.0F, NanoRenderUtils.argb(stack, border));

            // Accent flag
            if (hoverAnim > 0.05F)
            {
                float flagH = drawRect.h * 0.5F * hoverAnim;
                NanoRenderUtils.fillRoundedRect(vg,
                    drawRect.x + 4.0F, drawRect.y + (drawRect.h - flagH) * 0.5F,
                    3.0F, flagH, 1.5F,
                    NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(theme.accentArgb(), reveal * hoverAnim)));
            }

            // Dot
            float dotSize = 8.0F;
            NanoRenderUtils.fillRoundedRect(vg,
                drawRect.x + 16.0F, drawRect.y + drawRect.h * 0.5F - dotSize * 0.5F,
                dotSize, dotSize, dotSize * 0.5F,
                NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(blueprintColor(i, theme), reveal)));

            // Name + description
            NanoUi.drawLeftText(vg, stack, bold,
                drawRect.x + 30.0F, drawRect.y + drawRect.h * 0.35F,
                13.0F, NanoRenderUtils.mulAlpha(theme.textArgb(), reveal),
                BLUEPRINTS[i].displayName());
            NanoUi.drawLeftText(vg, stack, regular,
                drawRect.x + 30.0F, drawRect.y + drawRect.h * 0.70F,
                10.0F, NanoRenderUtils.mulAlpha(theme.textWeakArgb(), reveal),
                blueprintDescription(i));

            // Arrow
            float arrowShift = hoverAnim * 4.0F;
            int arrowColor = NanoScreenKit.mixArgb(theme.textMutedArgb(), theme.accentArgb(), hoverAnim);
            NanoUi.drawRightText(vg, stack, regular,
                drawRect.x2() - 12.0F + arrowShift, drawRect.y + drawRect.h * 0.5F,
                14.0F, NanoRenderUtils.mulAlpha(arrowColor, reveal), ">");
        }

        // Back button
        float btnH = 28.0F;
        float backY = ay + ah - 14.0F - btnH;
        UiRect backBtn = new UiRect(ax + 16.0F, backY, 80.0F, btnH);
        boolean backHovered = backBtn.contains(this.mouseX, this.mouseY) && phaseAlpha > 0.8F;
        float backHoverT = UiAnimationBus.animateControl(ANIM_PREFIX + ".sel.back", backHovered ? 1.0F : 0.0F, anim);
        int backFill = NanoRenderUtils.mulAlpha(
            NanoScreenKit.mixArgb(theme.controlArgb(), theme.controlHoverArgb(), backHoverT), fade);
        int backBorder = NanoRenderUtils.mulAlpha(
            NanoScreenKit.mixArgb(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 50),
                NanoRenderUtils.withAlpha(theme.accentArgb(), 100), backHoverT), fade);

        NanoRenderUtils.fillRoundedRect(vg, backBtn.x, backBtn.y, backBtn.w, backBtn.h,
            theme.controlRadius(), NanoRenderUtils.argb(stack, backFill));
        NanoRenderUtils.strokeRoundedRect(vg, backBtn.x, backBtn.y, backBtn.w, backBtn.h,
            theme.controlRadius(), 0.5F, NanoRenderUtils.argb(stack, backBorder));
        NanoUi.drawCenterText(vg, stack, regular,
            backBtn.x + backBtn.w * 0.5F, backBtn.y + backBtn.h * 0.5F,
            11.0F, NanoRenderUtils.mulAlpha(theme.textArgb(), fade),
            tr("guiedit.back", "< Back"));
    }

    // ── Editor rendering ──

    private void renderEditor(long vg, MemoryStack stack, NanoTheme theme, float phaseAlpha)
    {
        int regular = NanoFontBook.uiRegular();
        int bold = NanoFontBook.uiBold();
        UiAnimProfile anim = this.frameAnim;

        if (this.activeBlueprint == null) return;

        UiLayoutProfile lp = liveLayout();
        UiRect previewWin = editorPreviewWindow();
        float k = editorScale(previewWin);

        // Cache for mouseClickMove — avoids recomputing during drag
        this.cachedPreviewWin = previewWin;
        this.cachedScale = k;

        List<GuiBlueprint.Zone> zones = this.activeBlueprint.computeZones(previewWin, lp, k);
        List<GuiBlueprint.DragEdge> edges = this.activeBlueprint.computeEdges(previewWin, lp, k);
        this.cachedEdges = edges;

        // Update hover
        this.hoveredEdgeId = this.draggingEdgeId != null
            ? this.draggingEdgeId
            : GuiPreviewRenderer.hitTestEdge(edges, this.mouseX, this.mouseY);

        // Cache the active drag edge for mouseClickMove
        if (this.draggingEdgeId != null)
        {
            this.cachedDragEdge = GuiPreviewRenderer.findEdge(edges, this.draggingEdgeId);
        }

        // Editor fade-in animation
        float edScale = 0.96F + 0.04F * phaseAlpha;
        float edLift = (1.0F - phaseAlpha) * 15.0F;
        float pwX = previewWin.x + previewWin.w * (1.0F - edScale) * 0.5F;
        float pwY = previewWin.y + previewWin.h * (1.0F - edScale) * 0.5F + edLift;
        float pwW = previewWin.w * edScale;
        float pwH = previewWin.h * edScale;

        // Preview window background
        NanoRenderUtils.fillRoundedRect(vg, pwX, pwY, pwW, pwH,
            theme.windowRadius() * 0.6F,
            NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(
                NanoRenderUtils.withAlpha(theme.windowTopArgb(), 220), phaseAlpha)));
        NanoRenderUtils.strokeRoundedRect(vg, pwX, pwY, pwW, pwH,
            theme.windowRadius() * 0.6F, 1.0F,
            NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(theme.windowBorderArgb(), phaseAlpha)));

        GuiPreviewRenderer.renderZones(vg, stack, zones, theme, k, anim,
            this.draggingEdgeId != null);
        GuiPreviewRenderer.renderEdges(vg, stack, edges, this.hoveredEdgeId, this.draggingEdgeId,
            theme, k, anim, lp, previewWin);

        // ── Top bar ──
        float barH = 32.0F;
        float barW = Math.min(500.0F, this.width - 40.0F);
        float barX = (this.width - barW) * 0.5F;
        float barLift = (1.0F - phaseAlpha) * -barH;
        float barY = SCREEN_MARGIN + barLift;

        NanoRenderUtils.fillRoundedRect(vg, barX, barY, barW, barH, 8.0F,
            NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(
                NanoRenderUtils.withAlpha(theme.windowTopArgb(), 230), phaseAlpha)));
        NanoRenderUtils.strokeRoundedRect(vg, barX, barY, barW, barH, 8.0F, 1.0F,
            NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(
                NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 120), phaseAlpha)));

        NanoUi.drawLeftText(vg, stack, bold,
            barX + 14.0F, barY + barH * 0.5F,
            13.0F, NanoRenderUtils.mulAlpha(theme.textArgb(), phaseAlpha),
            tr("guiedit.editing", "Editing: {0}", this.activeBlueprint.displayName()));

        // Hint
        String hint;
        if (this.draggingEdgeId != null)
        {
            hint = tr("guiedit.hint.dragging", "Drag to adjust, release to apply");
        }
        else if (this.hoveredEdgeId != null)
        {
            GuiBlueprint.DragEdge hovEdge = GuiPreviewRenderer.findEdge(edges, this.hoveredEdgeId);
            String edgeName = hovEdge != null ? hovEdge.tooltip : "";
            hint = tr("guiedit.hint.edge", "{0} | Right-click to reset", edgeName);
        }
        else
        {
            hint = tr("guiedit.hint.idle", "Hover handles to adjust layout");
        }
        NanoUi.drawRightText(vg, stack, regular,
            barX + barW - 14.0F, barY + barH * 0.5F,
            10.0F, NanoRenderUtils.mulAlpha(theme.textMutedArgb(), phaseAlpha), hint);

        // ── Bottom bar ──
        float bottomH = 36.0F;
        float bottomW = Math.min(700.0F, this.width - 40.0F);
        float bottomX = (this.width - bottomW) * 0.5F;
        float bottomLift = (1.0F - phaseAlpha) * bottomH;
        float bottomY = this.height - SCREEN_MARGIN - bottomH + bottomLift;

        NanoRenderUtils.fillRoundedRect(vg, bottomX, bottomY, bottomW, bottomH, 8.0F,
            NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(
                NanoRenderUtils.withAlpha(theme.windowTopArgb(), 230), phaseAlpha)));
        NanoRenderUtils.strokeRoundedRect(vg, bottomX, bottomY, bottomW, bottomH, 8.0F, 1.0F,
            NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(
                NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 120), phaseAlpha)));

        // Back button
        UiRect backBtn = new UiRect(bottomX + 8.0F, bottomY + 6.0F, 72.0F, 24.0F);
        drawBarButton(vg, stack, theme, anim, backBtn, ANIM_PREFIX + ".btn.back",
            tr("guiedit.back", "< Back"), phaseAlpha);

        // Reset button
        UiRect resetBtn = new UiRect(backBtn.x2() + 8.0F, backBtn.y, 72.0F, 24.0F);
        drawBarButton(vg, stack, theme, anim, resetBtn, ANIM_PREFIX + ".btn.reset",
            tr("guiedit.reset_all", "Reset"), phaseAlpha);

        // Current values
        drawCurrentValues(vg, stack, bottomX, bottomY, bottomW, bottomH, regular, theme, lp, phaseAlpha);
    }

    private void drawBarButton(long vg, MemoryStack stack, NanoTheme theme, UiAnimProfile anim,
                                UiRect btn, String animKey, String label, float alpha)
    {
        int regular = NanoFontBook.uiRegular();
        boolean hovered = btn.contains(this.mouseX, this.mouseY) && alpha > 0.9F;
        float hoverT = UiAnimationBus.animateControl(animKey, hovered ? 1.0F : 0.0F, anim);
        int fill = NanoScreenKit.mixArgb(theme.controlArgb(), theme.controlHoverArgb(), hoverT);
        int border = NanoScreenKit.mixArgb(
            NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 60),
            NanoRenderUtils.withAlpha(theme.accentArgb(), 120), hoverT);
        NanoRenderUtils.fillRoundedRect(vg, btn.x, btn.y, btn.w, btn.h,
            theme.controlRadius(), NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(fill, alpha)));
        NanoRenderUtils.strokeRoundedRect(vg, btn.x, btn.y, btn.w, btn.h,
            theme.controlRadius(), 0.5F, NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(border, alpha)));
        NanoUi.drawCenterText(vg, stack, regular,
            btn.x + btn.w * 0.5F, btn.y + btn.h * 0.5F,
            10.0F, NanoRenderUtils.mulAlpha(theme.textArgb(), alpha), label);
    }

    private void drawCurrentValues(long vg, MemoryStack stack, float barX, float barY,
                                   float barW, float barH, int regular, NanoTheme theme,
                                   UiLayoutProfile lp, float alpha)
    {
        if (this.activeBlueprint == null) return;
        String key = this.activeBlueprint.key();
        String info;

        // Use simple concatenation instead of String.format to avoid GC pressure during drag
        if ("clickgui".equals(key))
        {
            info = "Side:" + Math.round(lp.clickGuiSidebarRatio() * 100) + "%  "
                 + "Mod:" + Math.round(lp.clickGuiModulesRatio() * 100) + "%  "
                 + "Gap:" + Math.round(lp.gapMajor()) + "  "
                 + "Val:" + Math.round(lp.valueColWidth()) + "  "
                 + "Cat:" + Math.round(lp.rowCategory()) + "  "
                 + "Mod:" + Math.round(lp.rowModule()) + "  "
                 + "Set:" + Math.round(lp.rowSetting());
        }
        else if ("settings".equals(key))
        {
            info = "W:" + Math.round(lp.settingsWidth()) + "  "
                 + "H:" + Math.round(lp.settingsHeight()) + "  "
                 + "Set:" + Math.round(lp.rowSetting());
        }
        else if ("account".equals(key))
        {
            info = "W:" + Math.round(lp.accountWidth()) + "  "
                 + "H:" + Math.round(lp.accountHeight());
        }
        else if ("chatoverlay".equals(key))
        {
            info = "Srv:" + Math.round(lp.chatServerListWidth()) + "  "
                 + "Ch:" + Math.round(lp.chatChannelRatio() * 100) + "%  "
                 + "In:" + Math.round(lp.chatInputBarHeight());
        }
        else
        {
            info = "";
        }

        NanoUi.drawLeftText(vg, stack, regular,
            barX + 170.0F, barY + barH * 0.5F,
            9.0F, NanoRenderUtils.mulAlpha(theme.textWeakArgb(), alpha), info);
    }

    // ── Layout helpers ──

    private UiRect editorPreviewWindow()
    {
        float topBar = SCREEN_MARGIN + 36.0F;
        float bottomBar = SCREEN_MARGIN + 40.0F;
        float pad = 20.0F;
        float x = pad;
        float y = topBar + 4.0F;
        float w = this.width - pad * 2.0F;
        float h = this.height - y - bottomBar - 4.0F;
        return new UiRect(x, y, Math.max(200.0F, w), Math.max(120.0F, h));
    }

    private float editorScale(UiRect win)
    {
        if (this.activeBlueprint != null)
        {
            String key = this.activeBlueprint.key();
            if ("clickgui".equals(key)) return UiMotion.clamp(win.w / 1120.0F, 0.3F, 1.5F);
            if ("settings".equals(key)) return UiMotion.clamp(win.w / 900.0F, 0.3F, 1.5F);
            if ("account".equals(key)) return UiMotion.clamp(win.w / 760.0F, 0.3F, 1.5F);
            if ("chatoverlay".equals(key)) return UiMotion.clamp(win.w / 960.0F, 0.3F, 1.5F);
        }
        return UiMotion.clamp(win.w / 1000.0F, 0.3F, 1.5F);
    }

    private UiRect editorBackButton()
    {
        float bottomH = 36.0F;
        float bottomY = this.height - SCREEN_MARGIN - bottomH;
        float bottomW = Math.min(700.0F, this.width - 40.0F);
        float bottomX = (this.width - bottomW) * 0.5F;
        return new UiRect(bottomX + 8.0F, bottomY + 6.0F, 72.0F, 24.0F);
    }

    private UiRect editorResetButton()
    {
        UiRect back = editorBackButton();
        return new UiRect(back.x2() + 8.0F, back.y, 72.0F, 24.0F);
    }

    // ── Navigation ──

    private void goBack()
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) mc.displayGuiScreen(this.parentScreen);
    }

    private void resetLayoutDefaults()
    {
        UiLayoutProfile lp = liveLayout();
        if (this.activeBlueprint == null) return;
        String key = this.activeBlueprint.key();

        if ("clickgui".equals(key))
        {
            lp.setClickGuiSidebarRatio(0.24F);
            lp.setClickGuiModulesRatio(0.31F);
            lp.setRowCategory(24.0F);
            lp.setRowModule(34.0F);
            lp.setRowSetting(26.0F);
            lp.setBtnHeight(20.0F);
            lp.setGapMajor(14.0F);
            lp.setValueColWidth(132.0F);
        }
        else if ("settings".equals(key))
        {
            lp.setSettingsWidth(900.0F);
            lp.setSettingsHeight(620.0F);
            lp.setRowSetting(26.0F);
        }
        else if ("account".equals(key))
        {
            lp.setAccountWidth(580.0F);
            lp.setAccountHeight(460.0F);
        }
        else if ("chatoverlay".equals(key))
        {
            lp.setChatServerListWidth(56.0F);
            lp.setChatChannelRatio(0.20F);
            lp.setChatInputBarHeight(52.0F);
        }
        markLayoutDirty();
    }

    // ── Utilities ──

    private static UiLayoutProfile liveLayout()
    {
        ClientBootstrap boot = ClientBootstrap.instance();
        if (boot != null && boot.getConfigManager() != null)
            return boot.getConfigManager().getUiLayout();
        return new UiLayoutProfile();
    }

    private static void markLayoutDirty()
    {
        ClientBootstrap boot = ClientBootstrap.instance();
        if (boot != null && boot.getConfigManager() != null)
            boot.getConfigManager().markClientDirty();
    }

    private static ClickGuiModule resolveClickGuiModule()
    {
        ClientBootstrap boot = ClientBootstrap.instance();
        if (boot == null || boot.getModules() == null) return null;
        Module m = boot.getModules().getById("click_gui");
        return m instanceof ClickGuiModule ? (ClickGuiModule) m : null;
    }

    private static NanoTheme resolveTheme(ClickGuiModule clickGui)
    {
        if (clickGui == null)
            return NanoThemes.create(dwgx.nano.NanoPalette.COBALT, 226, 108, 12.0F, null);
        Integer accentOverride = clickGui.isAccentOverrideEnabled()
            ? Integer.valueOf(clickGui.getAccentOverride().toArgb()) : null;
        int backdropAlpha = clickGui.isBackdropEnabled() ? clickGui.getBackdropAlpha() : 0;
        return NanoThemes.create(clickGui.getPalette(), clickGui.getPanelAlpha(), backdropAlpha,
            clickGui.getCornerRadius(), accentOverride);
    }

    /**
     * Resolve animation profile from ClickGuiModule settings — syncs speed/smooth/type.
     */
    private static UiAnimProfile resolveAnimProfile(ClickGuiModule clickGui)
    {
        return UiAnimProfiles.settingsProfile(clickGui);
    }

    private String tr(String key, String fallback, Object... args)
    {
        ClientBootstrap boot = ClientBootstrap.instance();
        if (boot != null && boot.getI18n() != null)
            return boot.getI18n().translateOrDefault(key, fallback, args);
        if (args.length > 0)
        {
            String result = fallback;
            for (int i = 0; i < args.length; i++)
                result = result.replace("{" + i + "}", String.valueOf(args[i]));
            return result;
        }
        return fallback;
    }

    private String blueprintDescription(int index)
    {
        switch (index)
        {
            case 0: return tr("guiedit.desc.clickgui", "Sidebar, modules, settings panel layout");
            case 1: return tr("guiedit.desc.settings", "Theme and animation settings layout");
            case 2: return tr("guiedit.desc.account", "Account list and controls layout");
            case 3: return tr("guiedit.desc.chatoverlay", "Chat overlay panel layout");
            default: return "";
        }
    }

    private static int blueprintColor(int index, NanoTheme theme)
    {
        switch (index)
        {
            case 0: return theme.accentArgb();
            case 1: return theme.successArgb();
            case 2: return NanoRenderUtils.withAlpha(theme.controlActiveArgb(), 200);
            case 3: return NanoRenderUtils.withAlpha(theme.accentSoftArgb(), 200);
            default: return theme.textMutedArgb();
        }
    }
}
