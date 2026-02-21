package client.auth.gui;

import client.auth.AccountRepository;
import client.auth.MicrosoftAuthResult;
import client.auth.MicrosoftAuthService;
import client.auth.MicrosoftSessionManager;
import client.core.ClientBootstrap;
import client.module.Module;
import client.module.impl.client.ClickGuiModule;
import client.render.RenderContext2D;
import client.setting.StringSetting;
import client.ui.NanoRenderableScreen;
import client.ui.template.NanoTextInput;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiAnimProfiles;
import client.ui.template.UiAnimationBus;
import client.ui.template.UiControlAnimations;
import client.ui.template.UiMotion;
import client.ui.template.UiWindowState;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoPalette;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoThemes;
import dwgx.nano.NanoUi;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Nano-themed multi-account manager:
 * - Microsoft account device login (auto polling, no redirect URL copy required)
 * - offline account creation
 * - select/login/delete/clear accounts
 */
public final class GuiAccountManagerScreen extends GuiScreen implements NanoRenderableScreen
{
    private static final float BASE_WIDTH = 760.0F;
    private static final float BASE_HEIGHT = 560.0F;
    private static final float MIN_WIDTH = 520.0F;
    private static final float MIN_HEIGHT = 340.0F;
    private static final float SCREEN_MARGIN = 8.0F;
    private static final float HEADER_HEIGHT = 36.0F;
    private static final float OUTER_PAD = 12.0F;
    private static final float ROW_HEIGHT = 30.0F;
    private static final float LIST_STAGGER_STEP = 0.07F;
    private static final float SCROLL_STEP = 0.82F;

    private static final int COLOR_STATUS_INFO = 0xFFE6E6E6;
    private static final int COLOR_STATUS_WARN = 0xFFFFD37F;
    private static final int COLOR_STATUS_OK = 0xFF80FF80;
    private static final int COLOR_STATUS_ERR = 0xFFFF8080;
    private static final int COLOR_STATUS_MS = 0xFF9CFFB0;

    private final GuiScreen parent;
    private final AccountRepository repository;
    private final MicrosoftAuthService authService = new MicrosoftAuthService();
    private final UiWindowState window = new UiWindowState(MIN_WIDTH, MIN_HEIGHT);
    private final StringSetting offlineNameBuffer = new StringSetting("__account_manager_offline_name", "Offline Name", "Offline account name input", "", 16);
    private final NanoTextInput offlineNameInput = new NanoTextInput();
    private final List<AccountRepository.AccountEntry> accounts = new ArrayList<AccountRepository.AccountEntry>();

    private int mouseX;
    private int mouseY;
    private int selectedIndex = -1;
    private int scrollOffset;
    private float scrollVisual;
    private int lastVisibleRows = 1;
    private long lastNanoVg;

    private float windowScale = 1.0F;
    private float windowAnchorX = 0.5F;
    private float windowAnchorY = 0.5F;

    private volatile boolean working;
    private volatile boolean cancelRequested;
    private volatile String status = "";
    private volatile int statusColor = COLOR_STATUS_INFO;
    private volatile String deviceCode = "";
    private volatile String deviceUri = "";
    private volatile long deviceExpiresAt;

    public GuiAccountManagerScreen(GuiScreen parent)
    {
        this.parent = parent;
        this.repository = new AccountRepository(Minecraft.getMinecraft().mcDataDir);
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }

    public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        this.window.endInteraction();
        this.offlineNameInput.blur();
        this.repository.load();
        this.reloadAccounts();
        this.scrollVisual = (float)this.scrollOffset;

        if (this.accounts.isEmpty())
        {
            this.setStatus("No accounts yet. Add offline or use MS Auto Login.", COLOR_STATUS_WARN);
        }
        else
        {
            this.setStatus("Select an account then click Login Selected.", COLOR_STATUS_INFO);
        }
    }

    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
        this.window.endInteraction();
        this.offlineNameInput.onMouseUp();
        this.offlineNameInput.blur();
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.offlineNameInput.isFocused())
        {
            if (this.offlineNameInput.handleKeyTyped(typedChar, keyCode, this.offlineNameBuffer))
            {
                return;
            }
        }

        if (keyCode == 1)
        {
            this.closeToParent();
            return;
        }

        if (keyCode == 28 || keyCode == 156)
        {
            if (this.offlineNameInput.isFocused())
            {
                this.addOffline();
                return;
            }

            if (!this.offlineNameBuffer.get().trim().isEmpty())
            {
                this.addOffline();
                return;
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        Layout l = this.layout();

        if (mouseButton == 0)
        {
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

            if (l.offlineInput.contains(mouseX, mouseY))
            {
                this.activateOfflineNameInput(l, mouseX, mouseY);
            }
            else
            {
                this.offlineNameInput.blur();
            }

            if (l.listRows.contains(mouseX, mouseY))
            {
                int index = this.rowIndexFromMouse(l, mouseY);

                if (index >= 0 && index < this.accountCount())
                {
                    this.selectedIndex = index;
                    this.persistSelectedId();
                }
            }

            if (l.scrollUp.contains(mouseX, mouseY) && this.canScrollUp())
            {
                this.scrollBy(-1, l.visibleRows);
                return;
            }

            if (l.scrollDown.contains(mouseX, mouseY) && this.canScrollDown(l.visibleRows))
            {
                this.scrollBy(1, l.visibleRows);
                return;
            }

            if (l.addOfflineButton.contains(mouseX, mouseY) && this.canAddOffline())
            {
                this.addOffline();
                return;
            }

            if (l.msAutoButton.contains(mouseX, mouseY) && this.canStartMicrosoftLogin())
            {
                this.startMicrosoftDeviceLogin();
                return;
            }

            if (l.loginSelectedButton.contains(mouseX, mouseY) && this.canLoginSelected())
            {
                this.loginSelected();
                return;
            }

            if (l.deleteSelectedButton.contains(mouseX, mouseY) && this.canDeleteSelected())
            {
                this.deleteSelected();
                return;
            }

            if (l.clearAllButton.contains(mouseX, mouseY) && this.canClearAll())
            {
                this.clearAll();
                return;
            }

            if (l.cancelWaitButton.contains(mouseX, mouseY) && this.canCancelWait())
            {
                this.cancelRequested = true;
                this.setStatus("Cancelling...", COLOR_STATUS_WARN);
                return;
            }

            if (l.backButton.contains(mouseX, mouseY) && this.canBack())
            {
                this.closeToParent();
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick)
    {
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        if (clickedMouseButton != 0)
        {
            return;
        }

        if (this.offlineNameInput.isFocused())
        {
            Layout l = this.layout();
            this.offlineNameInput.onMouseDrag(mouseX, l.offlineInput.x, l.offlineInput.w, this.lastNanoVg, NanoFontBook.uiRegular(), scaled(10.2F, l.scale), this.offlineNameBuffer.get());
        }
    }

    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.window.endInteraction();
        this.offlineNameInput.onMouseUp();
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

        Layout l = this.layout();

        if (l.listCard.contains(this.mouseX, this.mouseY) || l.listRows.contains(this.mouseX, this.mouseY))
        {
            int step = this.resolveScrollStep(wheel);
            this.scrollBy(wheel < 0 ? step : -step, l.visibleRows);
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

        this.lastNanoVg = vg;
        this.refreshLiveMousePosition();
        ClickGuiModule clickGui = this.resolveClickGuiModule();
        this.syncWindowTarget();

        if (this.window.isInteracting())
        {
            this.window.updateInteraction(this.liveMouseX(), this.liveMouseY(), (float)this.width, (float)this.height, SCREEN_MARGIN);
            this.syncWindowProfileFromWindow();
        }

        UiAnimProfile windowAnim = this.resolveWindowAnimationProfile(clickGui);
        this.window.tick(windowAnim);

        Layout l = this.layout();
        this.lastVisibleRows = l.visibleRows;
        this.clampScroll(l.visibleRows);
        this.updateScrollAnimation(clickGui);
        NanoTheme theme = this.resolveTheme(clickGui);
        UiAnimProfile animProfile = this.resolveAnimationProfile(clickGui);
        UiAnimProfile inputProfile = this.resolveInputAnimationProfile(clickGui, animProfile);
        NanoFontBook.ensureLoaded(vg);
        int regular = NanoFontBook.uiRegular();
        int bold = NanoFontBook.uiBold();

        try (MemoryStack stack = stackPush())
        {
            if ((theme.backdropArgb() >>> 24) > 0)
            {
                NanoUi.drawBackdrop(vg, stack, (float)this.width, (float)this.height, theme);
            }

            NanoUi.drawWindow(vg, stack, l.window.x, l.window.y, l.window.w, l.window.h, theme);
            NanoUi.drawLeftText(vg, stack, bold, l.header.x + scaled(12.0F, l.scale), l.header.y + l.header.h * 0.5F, scaled(16.0F, l.scale), theme.textArgb(), this.tr("account.manager.title", "Account Manager"));
            NanoUi.drawRightText(vg, stack, regular, l.header.x2() - scaled(12.0F, l.scale), l.header.y + l.header.h * 0.5F, scaled(10.5F, l.scale), theme.textWeakArgb(), this.tr("account.manager.subtitle", "Module-based multi-account login"));
            NanoUi.drawSurface(vg, stack, l.body.x, l.body.y, l.body.w, l.body.h, theme.surfaceRadius(), theme.mainArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 55));
            this.drawAccountList(vg, stack, regular, bold, theme, l, clickGui, animProfile);
            this.drawControls(vg, stack, regular, theme, l, inputProfile, animProfile);
            this.drawInfoCard(vg, stack, regular, bold, theme, l);
            this.drawResizeHandle(vg, stack, theme, l.resizeHandle);
            context.getNanoVG().resetScissor();
        }
    }

    private void drawAccountList(long vg, MemoryStack stack, int regular, int bold, NanoTheme theme, Layout l, ClickGuiModule clickGui, UiAnimProfile animProfile)
    {
        NanoUi.drawPanel(vg, stack, l.listCard.x, l.listCard.y, l.listCard.w, l.listCard.h, theme);
        NanoUi.drawLeftText(vg, stack, bold, l.listCard.x + scaled(10.0F, l.scale), l.listCard.y + scaled(13.0F, l.scale), scaled(11.2F, l.scale), theme.textArgb(), this.tr("account.manager.list.title", "Saved Accounts"));
        NanoUi.drawRightText(vg, stack, regular, l.listCard.x2() - scaled(34.0F, l.scale), l.listCard.y + scaled(13.0F, l.scale), scaled(9.5F, l.scale), theme.textWeakArgb(), Integer.toString(this.accountCount()));

        this.drawActionButton(vg, stack, regular, theme, l.scrollUp, "^", this.canScrollUp(), false, false, "account.manager.scroll.up", animProfile);
        this.drawActionButton(vg, stack, regular, theme, l.scrollDown, "v", this.canScrollDown(l.visibleRows), false, false, "account.manager.scroll.down", animProfile);

        List<AccountRepository.AccountEntry> snapshot = this.snapshotAccounts();
        int selected = this.selectedIndexSnapshot();

        if (snapshot.isEmpty())
        {
            NanoUi.drawCenterText(vg, stack, regular, l.listRows.x + l.listRows.w * 0.5F, l.listRows.y + l.listRows.h * 0.5F, scaled(11.2F, l.scale), theme.textWeakArgb(), this.tr("account.manager.list.empty", "No account"));
            return;
        }

        float listSpeed = this.resolveListAnimationSpeed(clickGui, animProfile);
        int base = (int)Math.floor((double)this.scrollVisual);
        float rowOffset = (this.scrollVisual - (float)base) * l.rowHeight;
        Rect selectedRect = null;
        float selectedVisibility = 1.0F;
        NanoUi.beginClip(vg, l.listRows.x, l.listRows.y, l.listRows.w, l.listRows.h);
        int drawRows = l.visibleRows + 2;

        for (int i = 0; i < drawRows; i++)
        {
            int index = base + i;

            if (index < 0 || index >= snapshot.size())
            {
                continue;
            }

            float rowY = l.listRows.y + l.rowHeight * (float)i - rowOffset;
            Rect row = new Rect(l.listRows.x, rowY, l.listRows.w, Math.max(scaled(18.0F, l.scale), l.rowHeight - scaled(2.0F, l.scale)));

            if (row.y2() < l.listRows.y || row.y > l.listRows.y2())
            {
                continue;
            }

            AccountRepository.AccountEntry entry = snapshot.get(index);
            String rowKey = "account.manager.row." + entry.getId();
            float reveal = UiControlAnimations.stagger("account.manager.list.reveal", 1.0F, i, LIST_STAGGER_STEP, animProfile, listSpeed);

            if (reveal <= 0.001F)
            {
                continue;
            }

            float lift = (1.0F - reveal) * scaled(5.0F, l.scale);
            Rect visualRow = new Rect(row.x, row.y + lift, row.w, row.h);
            boolean hovered = visualRow.contains(this.mouseX, this.mouseY);
            boolean isSelected = index == selected;
            float hoverRatio = UiControlAnimations.hover(rowKey + ".hover", hovered, animProfile);
            float selectRatio = UiAnimationBus.animateControl(rowKey + ".select", isSelected ? 1.0F : 0.0F, animProfile);
            int fill = this.mixArgb(theme.rowArgb(), theme.rowHoverArgb(), UiMotion.clamp01(hoverRatio * 0.80F));
            fill = this.mixArgb(fill, theme.rowSelectedArgb(), UiMotion.clamp01(selectRatio * 0.88F));
            int border = this.mixArgb(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 72), NanoRenderUtils.withAlpha(theme.accentArgb(), 146), UiMotion.clamp01(selectRatio * 0.72F + hoverRatio * 0.20F));
            fill = NanoRenderUtils.mulAlpha(fill, reveal);
            border = NanoRenderUtils.mulAlpha(border, reveal);
            NanoUi.drawSurface(vg, stack, visualRow.x, visualRow.y, visualRow.w, visualRow.h, Math.min(theme.controlRadius(), visualRow.h * 0.36F), fill, border);

            Rect typeChip = new Rect(visualRow.x + scaled(7.0F, l.scale), visualRow.y + (visualRow.h - scaled(14.0F, l.scale)) * 0.5F, scaled(32.0F, l.scale), scaled(14.0F, l.scale));
            boolean microsoft = entry.isMicrosoft();
            int typeFill = microsoft
                ? this.mixArgb(theme.controlArgb(), theme.accentArgb(), 0.62F)
                : this.mixArgb(theme.controlArgb(), theme.successArgb(), 0.42F);
            typeFill = NanoRenderUtils.mulAlpha(typeFill, reveal);
            NanoUi.drawSurface(vg, stack, typeChip.x, typeChip.y, typeChip.w, typeChip.h, Math.min(typeChip.h * 0.5F, theme.controlRadius()), typeFill, 0);
            NanoUi.drawCenterText(vg, stack, regular, typeChip.x + typeChip.w * 0.5F, typeChip.y + typeChip.h * 0.5F, scaled(9.0F, l.scale), NanoRenderUtils.mulAlpha(theme.textArgb(), reveal), microsoft ? "MS" : "OFF");

            float textX = typeChip.x2() + scaled(8.0F, l.scale);
            float textW = visualRow.x2() - textX - scaled(8.0F, l.scale);
            String name = this.ellipsize(vg, bold, scaled(10.8F, l.scale), entry.getName(), textW);
            String detail = microsoft
                ? this.tr("account.manager.type.microsoft", "Microsoft Account")
                : this.tr("account.manager.type.offline", "Offline Account");
            NanoUi.drawLeftText(vg, stack, bold, textX, visualRow.y + visualRow.h * 0.38F, scaled(10.8F, l.scale), NanoRenderUtils.mulAlpha(theme.textArgb(), reveal), name);
            NanoUi.drawLeftText(vg, stack, regular, textX, visualRow.y + visualRow.h * 0.72F, scaled(9.0F, l.scale), NanoRenderUtils.mulAlpha(theme.textWeakArgb(), reveal), detail);

            if (isSelected)
            {
                selectedRect = visualRow;
                selectedVisibility = reveal;
            }
        }

        NanoUi.endClip(vg);
        this.drawAnimatedSelectionBox(vg, stack, "account.manager.selection", selectedRect, theme, l.scale, animProfile, clickGui, selectedVisibility);
    }

    private void drawControls(long vg, MemoryStack stack, int regular, NanoTheme theme, Layout l, UiAnimProfile inputProfile, UiAnimProfile animProfile)
    {
        boolean inputHovered = l.offlineInput.contains(this.mouseX, this.mouseY);
        this.offlineNameInput.draw(
            vg,
            stack,
            regular,
            theme,
            l.offlineInput.x,
            l.offlineInput.y,
            l.offlineInput.w,
            l.offlineInput.h,
            l.scale,
            scaled(10.2F, l.scale),
            this.offlineNameBuffer.get(),
            this.tr("account.manager.offline.placeholder", "Offline name (A-Z a-z 0-9 _)"),
            inputHovered,
            true,
            "account.manager.input.offline",
            inputProfile
        );

        this.drawActionButton(vg, stack, regular, theme, l.addOfflineButton, this.tr("account.manager.action.add_offline", "Add Offline"), this.canAddOffline(), true, false, "account.manager.btn.add_offline", animProfile);
        this.drawActionButton(vg, stack, regular, theme, l.msAutoButton, this.tr("account.manager.action.ms_auto", "MS Auto Login"), this.canStartMicrosoftLogin(), true, false, "account.manager.btn.ms_auto", animProfile);
        this.drawActionButton(vg, stack, regular, theme, l.loginSelectedButton, this.tr("account.manager.action.login_selected", "Login Selected"), this.canLoginSelected(), true, false, "account.manager.btn.login_selected", animProfile);
        this.drawActionButton(vg, stack, regular, theme, l.deleteSelectedButton, this.tr("account.manager.action.delete_selected", "Delete Selected"), this.canDeleteSelected(), false, true, "account.manager.btn.delete_selected", animProfile);
        this.drawActionButton(vg, stack, regular, theme, l.clearAllButton, this.tr("account.manager.action.clear_all", "Clear All"), this.canClearAll(), false, true, "account.manager.btn.clear_all", animProfile);
        this.drawActionButton(vg, stack, regular, theme, l.cancelWaitButton, this.tr("account.manager.action.cancel_wait", "Cancel Wait"), this.canCancelWait(), false, false, "account.manager.btn.cancel_wait", animProfile);
        this.drawActionButton(vg, stack, regular, theme, l.backButton, this.tr("ui.back", "Back"), this.canBack(), false, false, "account.manager.btn.back", animProfile);
    }

    private void drawInfoCard(long vg, MemoryStack stack, int regular, int bold, NanoTheme theme, Layout l)
    {
        NanoUi.drawPanel(vg, stack, l.infoCard.x, l.infoCard.y, l.infoCard.w, l.infoCard.h, theme);
        NanoUi.drawLeftText(vg, stack, bold, l.infoCard.x + scaled(10.0F, l.scale), l.infoCard.y + scaled(13.0F, l.scale), scaled(10.8F, l.scale), theme.textArgb(), this.tr("account.manager.info.title", "Session & Status"));
        Rect content = new Rect(l.infoCard.x + scaled(10.0F, l.scale), l.infoCard.y + scaled(22.0F, l.scale), l.infoCard.w - scaled(20.0F, l.scale), l.infoCard.h - scaled(28.0F, l.scale));
        float lineH = scaled(11.6F, l.scale);
        float lineY = content.y + scaled(3.0F, l.scale);
        NanoUi.beginClip(vg, content.x, content.y, content.w, content.h);
        NanoUi.drawLeftText(vg, stack, regular, content.x, lineY, scaled(10.1F, l.scale), theme.textWeakArgb(), this.ellipsize(vg, regular, scaled(10.1F, l.scale), this.currentSessionLine(), content.w));
        lineY += lineH;
        NanoUi.drawLeftText(vg, stack, regular, content.x, lineY, scaled(10.1F, l.scale), this.statusColor, this.ellipsize(vg, regular, scaled(10.1F, l.scale), this.status, content.w));

        if (!this.deviceCode.isEmpty())
        {
            long leftMs = Math.max(0L, this.deviceExpiresAt - System.currentTimeMillis());
            int leftSec = (int)(leftMs / 1000L);
            lineY += lineH;
            String codeLine = "MS Code: " + this.deviceCode + " (" + leftSec + "s)";
            NanoUi.drawLeftText(vg, stack, regular, content.x, lineY, scaled(10.1F, l.scale), COLOR_STATUS_MS, this.ellipsize(vg, regular, scaled(10.1F, l.scale), codeLine, content.w));
            lineY += lineH;
            String copiedLine = "Verification code copied to clipboard automatically.";
            NanoUi.drawLeftText(vg, stack, regular, content.x, lineY, scaled(10.1F, l.scale), COLOR_STATUS_MS, this.ellipsize(vg, regular, scaled(10.1F, l.scale), copiedLine, content.w));
            lineY += lineH;
            String uriLine = "Open: " + this.deviceUri;
            NanoUi.drawLeftText(vg, stack, regular, content.x, lineY, scaled(10.1F, l.scale), COLOR_STATUS_MS, this.ellipsize(vg, regular, scaled(10.1F, l.scale), uriLine, content.w));
        }

        NanoUi.endClip(vg);
    }

    private void drawActionButton(long vg, MemoryStack stack, int regular, NanoTheme theme, Rect rect, String label, boolean enabled, boolean primary, boolean danger, String animKey, UiAnimProfile animProfile)
    {
        boolean hovered = enabled && rect.contains(this.mouseX, this.mouseY);
        float hover = UiControlAnimations.hover(animKey + ".hover", hovered, animProfile);
        float focus = UiControlAnimations.focus(animKey + ".focus", hovered, animProfile);
        int fill;
        int border;
        int textColor;

        if (!enabled)
        {
            fill = this.mixArgb(theme.cardAltArgb(), theme.controlArgb(), 0.40F);
            border = NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 58);
            textColor = NanoRenderUtils.withAlpha(theme.textWeakArgb(), 136);
        }
        else if (danger)
        {
            fill = this.mixArgb(theme.controlArgb(), theme.dangerArgb(), UiMotion.clamp01(0.26F + hover * 0.54F));
            border = this.mixArgb(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 82), NanoRenderUtils.withAlpha(theme.dangerArgb(), 168), UiMotion.clamp01(hover * 0.80F + focus * 0.20F));
            textColor = theme.textArgb();
        }
        else if (primary)
        {
            fill = this.mixArgb(theme.controlArgb(), theme.controlActiveArgb(), UiMotion.clamp01(0.52F + hover * 0.38F));
            border = this.mixArgb(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 86), NanoRenderUtils.withAlpha(theme.accentArgb(), 166), UiMotion.clamp01(0.36F + hover * 0.46F + focus * 0.18F));
            textColor = theme.textArgb();
        }
        else
        {
            fill = this.mixArgb(theme.controlArgb(), theme.controlHoverArgb(), UiMotion.clamp01(0.30F + hover * 0.56F));
            border = this.mixArgb(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 86), NanoRenderUtils.withAlpha(theme.accentArgb(), 132), UiMotion.clamp01(hover * 0.64F + focus * 0.24F));
            textColor = theme.textArgb();
        }

        NanoUi.drawSurface(vg, stack, rect.x, rect.y, rect.w, rect.h, Math.min(theme.controlRadius(), rect.h * 0.5F), fill, border);
        NanoUi.drawCenterText(vg, stack, regular, rect.x + rect.w * 0.5F, rect.y + rect.h * 0.5F, scaled(10.4F, UiMotion.clamp(rect.h / 22.0F, 0.35F, 1.85F)), textColor, label);
    }

    private void drawResizeHandle(long vg, MemoryStack stack, NanoTheme theme, Rect handle)
    {
        float k = UiMotion.clamp(handle.w / 10.0F, 0.35F, 1.85F);
        NanoUi.drawSurface(vg, stack, handle.x, handle.y, handle.w, handle.h, scaled(4.0F, k), NanoRenderUtils.withAlpha(theme.controlActiveArgb(), 188), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 90));
    }

    private void drawAnimatedSelectionBox(long vg, MemoryStack stack, String key, Rect target, NanoTheme theme, float scale, UiAnimProfile profile, ClickGuiModule clickGui, float visibility)
    {
        float speed = this.resolveSelectionAnimationSpeed(clickGui, profile);
        float visible = UiControlAnimations.presence(key, target != null && visibility > 0.01F, profile, speed);
        float alpha = UiMotion.clamp01(visible * UiMotion.clamp01(visibility));

        if (alpha <= 0.001F || target == null)
        {
            return;
        }

        float x = UiAnimationBus.animateWithSpeed(key + ".x", target.x, profile, speed);
        float y = UiAnimationBus.animateWithSpeed(key + ".y", target.y, profile, speed);
        float w = UiAnimationBus.animateWithSpeed(key + ".w", target.w, profile, speed);
        float h = UiAnimationBus.animateWithSpeed(key + ".h", target.h, profile, speed);
        float radius = Math.min(h * 0.5F, this.stableRowRadius(scale));
        int fill = NanoRenderUtils.mulAlpha(theme.accentSoftArgb(), alpha * 0.36F);
        int border = NanoRenderUtils.mulAlpha(theme.accentArgb(), alpha * 0.72F);
        NanoUi.drawSurface(vg, stack, x, y, w, h, radius, fill, border);
    }

    private void activateOfflineNameInput(Layout l, int mouseX, int mouseY)
    {
        if (this.lastNanoVg != 0L)
        {
            NanoFontBook.ensureLoaded(this.lastNanoVg);
        }

        this.offlineNameInput.onMouseDown(mouseX, mouseY, l.offlineInput.x, l.offlineInput.y, l.offlineInput.w, l.offlineInput.h, this.lastNanoVg, NanoFontBook.uiRegular(), scaled(10.2F, l.scale), this.offlineNameBuffer.get());
    }

    private void closeToParent()
    {
        if (this.mc != null)
        {
            this.mc.displayGuiScreen(this.parent);
        }
    }

    private void syncWindowTarget()
    {
        float targetWidth = UiMotion.clamp(BASE_WIDTH * this.windowScale, MIN_WIDTH, (float)this.width - SCREEN_MARGIN * 2.0F);
        float targetHeight = UiMotion.clamp(BASE_HEIGHT * this.windowScale, MIN_HEIGHT, (float)this.height - SCREEN_MARGIN * 2.0F);
        float targetX = this.windowAnchorX * (float)this.width - targetWidth * 0.5F;
        float targetY = this.windowAnchorY * (float)this.height - targetHeight * 0.5F;

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

    private void syncWindowProfileFromWindow()
    {
        float minScale = MIN_WIDTH / BASE_WIDTH;
        float widthRatio = this.window.getTargetWidth() / BASE_WIDTH;
        float heightRatio = this.window.getTargetHeight() / BASE_HEIGHT;
        this.windowScale = UiMotion.clamp(Math.min(widthRatio, heightRatio), minScale, 1.85F);
        float cx = this.window.getTargetX() + this.window.getTargetWidth() * 0.5F;
        float cy = this.window.getTargetY() + this.window.getTargetHeight() * 0.5F;
        this.windowAnchorX = UiMotion.clamp01(cx / Math.max(1.0F, (float)this.width));
        this.windowAnchorY = UiMotion.clamp01(cy / Math.max(1.0F, (float)this.height));
    }

    private void addOffline()
    {
        if (this.working)
        {
            return;
        }

        try
        {
            this.repository.addOffline(this.offlineNameBuffer.get());
            this.repository.save();
            this.reloadAccounts();
            this.offlineNameBuffer.set("");
            this.setStatus("Offline account added.", COLOR_STATUS_OK);
        }
        catch (Exception ex)
        {
            this.setStatus("Add offline failed: " + ex.getMessage(), COLOR_STATUS_ERR);
        }
    }

    private void loginSelected()
    {
        if (this.working)
        {
            return;
        }

        final AccountRepository.AccountEntry selected = this.getSelectedEntry();

        if (selected == null)
        {
            this.setStatus("Select an account first.", COLOR_STATUS_WARN);
            return;
        }

        this.working = true;
        this.cancelRequested = false;
        this.setStatus("Logging in...", COLOR_STATUS_WARN);

        Thread worker = new Thread("Account-Login-Selected")
        {
            public void run()
            {
                try
                {
                    if (selected.isMicrosoft())
                    {
                        if (selected.getRefreshToken() == null || selected.getRefreshToken().trim().isEmpty())
                        {
                            throw new MicrosoftAuthService.AuthException("Missing refresh token. Re-login this account.");
                        }

                        MicrosoftAuthResult result = GuiAccountManagerScreen.this.authService.loginWithRefreshToken(selected.getRefreshToken());
                        MicrosoftSessionManager.applyMicrosoftSession(GuiAccountManagerScreen.this.mc, result);
                        AccountRepository.AccountEntry updated = GuiAccountManagerScreen.this.repository.upsertMicrosoft(result);
                        GuiAccountManagerScreen.this.repository.setSelectedId(updated.getId());
                    }
                    else
                    {
                        MicrosoftSessionManager.applyOfflineSession(GuiAccountManagerScreen.this.mc, selected.getName());
                        GuiAccountManagerScreen.this.repository.setSelectedId(selected.getId());
                    }

                    GuiAccountManagerScreen.this.repository.save();
                    GuiAccountManagerScreen.this.reloadAccountsThreadSafe();
                    GuiAccountManagerScreen.this.setStatus("Login success: " + selected.getName(), COLOR_STATUS_OK);
                }
                catch (Exception ex)
                {
                    GuiAccountManagerScreen.this.setStatus("Login failed: " + ex.getMessage(), COLOR_STATUS_ERR);
                }
                finally
                {
                    GuiAccountManagerScreen.this.working = false;
                }
            }
        };
        worker.setDaemon(true);
        worker.start();
    }

    private void deleteSelected()
    {
        if (this.working)
        {
            return;
        }

        AccountRepository.AccountEntry selected = this.getSelectedEntry();

        if (selected == null)
        {
            this.setStatus("Select an account first.", COLOR_STATUS_WARN);
            return;
        }

        if (this.repository.removeById(selected.getId()))
        {
            this.repository.save();
            this.reloadAccounts();
            this.setStatus("Deleted account: " + selected.getName(), COLOR_STATUS_OK);
        }
        else
        {
            this.setStatus("Delete failed.", COLOR_STATUS_ERR);
        }
    }

    private void clearAll()
    {
        if (this.working)
        {
            return;
        }

        this.repository.clear();
        this.repository.save();
        this.reloadAccounts();
        this.setStatus("All saved accounts cleared.", COLOR_STATUS_OK);
    }

    private void startMicrosoftDeviceLogin()
    {
        if (this.working)
        {
            return;
        }

        this.working = true;
        this.cancelRequested = false;
        this.deviceCode = "";
        this.deviceUri = "";
        this.deviceExpiresAt = 0L;
        this.setStatus("Starting Microsoft device login...", COLOR_STATUS_WARN);

        Thread worker = new Thread("Account-MS-Device")
        {
            public void run()
            {
                try
                {
                    MicrosoftAuthService.DeviceAuthStart start = GuiAccountManagerScreen.this.authService.startDeviceLogin();
                    GuiAccountManagerScreen.this.deviceCode = start.getUserCode();
                    GuiAccountManagerScreen.this.deviceUri = start.getVerificationUri();
                    GuiAccountManagerScreen.this.deviceExpiresAt = System.currentTimeMillis() + start.getExpiresInSeconds() * 1000L;
                    setClipboardString(start.getUserCode());
                    openBrowser(start.getVerificationUri());
                    GuiAccountManagerScreen.this.setStatus("Browser opened. Code copied: " + start.getUserCode(), COLOR_STATUS_MS);

                    MicrosoftAuthResult result = GuiAccountManagerScreen.this.authService.awaitDeviceLogin(
                        start,
                        new MicrosoftAuthService.CancelCheck()
                        {
                            public boolean isCancelled()
                            {
                                return GuiAccountManagerScreen.this.cancelRequested;
                            }
                        },
                        new MicrosoftAuthService.DeviceProgressListener()
                        {
                            public void onProgress(String message)
                            {
                                if (message != null && !message.isEmpty())
                                {
                                    GuiAccountManagerScreen.this.setStatus(message, COLOR_STATUS_WARN);
                                }
                            }
                        }
                    );

                    MicrosoftSessionManager.applyMicrosoftSession(GuiAccountManagerScreen.this.mc, result);
                    AccountRepository.AccountEntry entry = GuiAccountManagerScreen.this.repository.upsertMicrosoft(result);
                    GuiAccountManagerScreen.this.repository.setSelectedId(entry.getId());
                    GuiAccountManagerScreen.this.repository.save();
                    GuiAccountManagerScreen.this.reloadAccountsThreadSafe();
                    GuiAccountManagerScreen.this.setStatus("Microsoft login success: " + result.getPlayerName(), COLOR_STATUS_OK);
                }
                catch (Exception ex)
                {
                    GuiAccountManagerScreen.this.setStatus("Microsoft login failed: " + ex.getMessage(), COLOR_STATUS_ERR);
                }
                finally
                {
                    GuiAccountManagerScreen.this.working = false;
                    GuiAccountManagerScreen.this.cancelRequested = false;
                }
            }
        };
        worker.setDaemon(true);
        worker.start();
    }

    private synchronized void reloadAccounts()
    {
        this.accounts.clear();
        this.accounts.addAll(this.repository.list());
        this.selectedIndex = this.findIndexById(this.repository.getSelectedId());

        if (this.selectedIndex < 0 && !this.accounts.isEmpty())
        {
            this.selectedIndex = 0;
        }

        this.clampScroll(this.lastVisibleRows);
        this.ensureSelectionVisible(this.lastVisibleRows);
        this.scrollVisual = (float)this.scrollOffset;
    }

    private void reloadAccountsThreadSafe()
    {
        this.reloadAccounts();
    }

    private synchronized List<AccountRepository.AccountEntry> snapshotAccounts()
    {
        return new ArrayList<AccountRepository.AccountEntry>(this.accounts);
    }

    private synchronized int accountCount()
    {
        return this.accounts.size();
    }

    private synchronized int selectedIndexSnapshot()
    {
        return this.selectedIndex;
    }

    private synchronized AccountRepository.AccountEntry getSelectedEntry()
    {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.accounts.size())
        {
            return null;
        }

        return this.accounts.get(this.selectedIndex);
    }

    private synchronized int findIndexById(String id)
    {
        if (id == null || id.trim().isEmpty())
        {
            return -1;
        }

        for (int i = 0; i < this.accounts.size(); i++)
        {
            if (id.equals(this.accounts.get(i).getId()))
            {
                return i;
            }
        }

        return -1;
    }

    private void persistSelectedId()
    {
        AccountRepository.AccountEntry selected = this.getSelectedEntry();

        if (selected != null)
        {
            this.repository.setSelectedId(selected.getId());
            this.repository.save();
        }
    }

    private void ensureSelectionVisible(int visibleRows)
    {
        int rows = Math.max(1, visibleRows);
        int selected = this.selectedIndexSnapshot();

        if (selected < 0)
        {
            return;
        }

        if (selected < this.scrollOffset)
        {
            this.scrollOffset = selected;
        }
        else if (selected >= this.scrollOffset + rows)
        {
            this.scrollOffset = selected - rows + 1;
        }

        this.clampScroll(rows);
    }

    private void scrollBy(int delta, int visibleRows)
    {
        this.scrollOffset += delta;
        this.clampScroll(visibleRows);
    }

    private void clampScroll(int visibleRows)
    {
        int max = this.maxScroll(visibleRows);

        if (this.scrollOffset < 0)
        {
            this.scrollOffset = 0;
        }

        if (this.scrollOffset > max)
        {
            this.scrollOffset = max;
        }
    }

    private synchronized int maxScroll(int visibleRows)
    {
        return Math.max(0, this.accounts.size() - Math.max(1, visibleRows));
    }

    private int resolveScrollStep(int wheelDelta)
    {
        float notches = Math.max(1.0F, Math.abs((float)wheelDelta) / 120.0F);
        return Math.max(1, Math.round(SCROLL_STEP * notches));
    }

    private int rowIndexFromMouse(Layout l, int mouseY)
    {
        int base = (int)Math.floor((double)this.scrollVisual);
        float offset = (this.scrollVisual - (float)base) * l.rowHeight;
        int row = (int)(((float)mouseY - l.listRows.y + offset) / Math.max(1.0F, l.rowHeight));

        if (row < 0)
        {
            return -1;
        }

        return base + row;
    }

    private void updateScrollAnimation(ClickGuiModule clickGui)
    {
        UiAnimProfile profile = this.resolveAnimationProfile(clickGui);
        float target = (float)this.scrollOffset;

        if (!profile.isEnabled())
        {
            this.scrollVisual = target;
            return;
        }

        float speed = UiAnimProfiles.scrollSpeed(profile);
        this.scrollVisual = UiAnimationBus.animateWithSpeed("account.manager.scroll", target, profile, speed);
    }

    private boolean canLoginSelected()
    {
        return !this.working && this.getSelectedEntry() != null;
    }

    private boolean canDeleteSelected()
    {
        return !this.working && this.getSelectedEntry() != null;
    }

    private boolean canClearAll()
    {
        return !this.working && this.accountCount() > 0;
    }

    private boolean canAddOffline()
    {
        return !this.working;
    }

    private boolean canStartMicrosoftLogin()
    {
        return !this.working;
    }

    private boolean canCancelWait()
    {
        return this.working;
    }

    private boolean canBack()
    {
        return !this.working;
    }

    private boolean canScrollUp()
    {
        return this.scrollOffset > 0;
    }

    private boolean canScrollDown(int visibleRows)
    {
        return this.scrollOffset < this.maxScroll(visibleRows);
    }

    private String currentSessionLine()
    {
        Session session = this.mc == null ? null : this.mc.getSession();

        if (session == null)
        {
            return "Current Session: <none>";
        }

        String type = session.getSessionType() == Session.Type.MOJANG ? "mojang" : "legacy";
        String username = session.getUsername();
        return "Current Session: " + username + " (" + type + ")";
    }

    private void setStatus(String text, int color)
    {
        this.status = text == null ? "" : text;
        this.statusColor = color;
    }

    private ClickGuiModule resolveClickGuiModule()
    {
        Module module = ClientBootstrap.instance().getModules().getById("click_gui");
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
        boolean interacting = this.window.isInteracting();
        return UiAnimProfiles.settingsWindowProfile(clickGui, interacting);
    }

    private float resolveListAnimationSpeed(ClickGuiModule clickGui, UiAnimProfile profile)
    {
        return UiAnimProfiles.listSpeed(clickGui, profile);
    }

    private float resolveSelectionAnimationSpeed(ClickGuiModule clickGui, UiAnimProfile profile)
    {
        return UiAnimProfiles.selectionSpeed(clickGui, profile);
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

    private String tr(String key, String fallback, Object... args)
    {
        client.i18n.I18nManager i18n = ClientBootstrap.instance().getI18n();
        return i18n == null ? fallback : i18n.translateOrDefault(key, fallback, args);
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

    private String ellipsize(long vg, int fontId, float size, String text, float maxWidth)
    {
        String value = text == null ? "" : text;

        if (maxWidth <= 0.0F || value.isEmpty())
        {
            return "";
        }

        if (NanoRenderUtils.textWidth(vg, fontId, size, value) <= maxWidth)
        {
            return value;
        }

        String suffix = "...";
        float suffixWidth = NanoRenderUtils.textWidth(vg, fontId, size, suffix);
        String out = value;

        while (!out.isEmpty() && NanoRenderUtils.textWidth(vg, fontId, size, out) + suffixWidth > maxWidth)
        {
            out = out.substring(0, out.length() - 1);
        }

        if (out.isEmpty())
        {
            return suffixWidth <= maxWidth ? suffix : "";
        }

        return out + suffix;
    }

    private Rect fallbackWindow()
    {
        float width = Math.min(Math.max(MIN_WIDTH, BASE_WIDTH), (float)this.width - SCREEN_MARGIN * 2.0F);
        float height = Math.min(Math.max(MIN_HEIGHT, BASE_HEIGHT), (float)this.height - SCREEN_MARGIN * 2.0F);
        return new Rect(((float)this.width - width) * 0.5F, ((float)this.height - height) * 0.5F, width, height);
    }

    private Layout layout()
    {
        Rect windowRect = this.window.isInitialized()
            ? new Rect(this.window.getX(), this.window.getY(), this.window.getWidth(), this.window.getHeight())
            : this.fallbackWindow();
        float k = UiMotion.clamp(windowRect.w / BASE_WIDTH, 0.35F, 1.85F);
        float headerHeight = scaled(HEADER_HEIGHT, k);
        float outerPad = scaled(OUTER_PAD, k);
        Rect header = new Rect(windowRect.x + scaled(1.0F, k), windowRect.y + scaled(1.0F, k), windowRect.w - scaled(2.0F, k), headerHeight - scaled(2.0F, k));
        Rect headerDrag = new Rect(header.x + scaled(2.0F, k), header.y + scaled(2.0F, k), header.w - scaled(4.0F, k), header.h - scaled(4.0F, k));
        Rect body = new Rect(windowRect.x + outerPad, windowRect.y + headerHeight + scaled(10.0F, k), windowRect.w - outerPad * 2.0F, windowRect.h - headerHeight - outerPad * 2.0F);
        float controlH = scaled(22.0F, k);
        float gap = scaled(6.0F, k);
        float infoMin = scaled(58.0F, k);
        float listMin = scaled(86.0F, k);
        float listMax = Math.max(listMin, body.h - (controlH * 3.0F + gap * 4.0F + infoMin));
        float listH = UiMotion.clamp(body.h * 0.46F, listMin, listMax);
        Rect listCard = new Rect(body.x, body.y, body.w, listH);
        float scrollW = scaled(24.0F, k);
        Rect listRows = new Rect(listCard.x + scaled(8.0F, k), listCard.y + scaled(22.0F, k), listCard.w - scaled(16.0F, k) - scrollW - scaled(4.0F, k), listCard.h - scaled(30.0F, k));
        Rect scrollUp = new Rect(listRows.x2() + scaled(4.0F, k), listRows.y, scrollW, scaled(18.0F, k));
        Rect scrollDown = new Rect(scrollUp.x, scrollUp.y + scaled(22.0F, k), scrollW, scaled(18.0F, k));

        float controlsY = listCard.y2() + gap;
        float offlineW = UiMotion.clamp(body.w * 0.44F, scaled(140.0F, k), body.w - scaled(240.0F, k));
        float halfRemain = (body.w - offlineW - gap * 2.0F) * 0.5F;
        Rect offlineInput = new Rect(body.x, controlsY, offlineW, controlH);
        Rect addOfflineButton = new Rect(offlineInput.x2() + gap, controlsY, halfRemain, controlH);
        Rect msAutoButton = new Rect(addOfflineButton.x2() + gap, controlsY, halfRemain, controlH);

        float row2Y = controlsY + controlH + gap;
        float third = (body.w - gap * 2.0F) / 3.0F;
        Rect loginSelectedButton = new Rect(body.x, row2Y, third, controlH);
        Rect deleteSelectedButton = new Rect(loginSelectedButton.x2() + gap, row2Y, third, controlH);
        Rect clearAllButton = new Rect(deleteSelectedButton.x2() + gap, row2Y, third, controlH);

        float row3Y = row2Y + controlH + gap;
        float cancelW = UiMotion.clamp(body.w * 0.34F, scaled(120.0F, k), body.w - scaled(140.0F, k));
        Rect cancelWaitButton = new Rect(body.x, row3Y, cancelW, controlH);
        Rect backButton = new Rect(cancelWaitButton.x2() + gap, row3Y, body.w - cancelW - gap, controlH);

        float infoY = row3Y + controlH + gap;
        Rect infoCard = new Rect(body.x, infoY, body.w, Math.max(scaled(50.0F, k), body.y2() - infoY));
        Rect resizeHandle = new Rect(windowRect.x2() - scaled(14.0F, k), windowRect.y2() - scaled(14.0F, k), scaled(10.0F, k), scaled(10.0F, k));
        float rowH = scaled(ROW_HEIGHT, k);
        int visibleRows = Math.max(1, (int)Math.floor((double)(listRows.h / Math.max(1.0F, rowH))));
        return new Layout(windowRect, header, headerDrag, body, listCard, listRows, scrollUp, scrollDown, offlineInput, addOfflineButton, msAutoButton, loginSelectedButton, deleteSelectedButton, clearAllButton, cancelWaitButton, backButton, infoCard, resizeHandle, rowH, visibleRows, k);
    }

    private float stableRowRadius(float scale)
    {
        return scaled(6.0F * this.cornerRadiusScale(), UiMotion.clamp(scale, 0.35F, 1.85F));
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

    private static float scaled(float value, float scale)
    {
        return value * scale;
    }

    private static boolean openBrowser(String url)
    {
        try
        {
            if (url == null || url.isEmpty() || GraphicsEnvironment.isHeadless() || !Desktop.isDesktopSupported())
            {
                return false;
            }

            Desktop.getDesktop().browse(new URI(url));
            return true;
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    private static final class Layout
    {
        private final Rect window;
        private final Rect header;
        private final Rect headerDrag;
        private final Rect body;
        private final Rect listCard;
        private final Rect listRows;
        private final Rect scrollUp;
        private final Rect scrollDown;
        private final Rect offlineInput;
        private final Rect addOfflineButton;
        private final Rect msAutoButton;
        private final Rect loginSelectedButton;
        private final Rect deleteSelectedButton;
        private final Rect clearAllButton;
        private final Rect cancelWaitButton;
        private final Rect backButton;
        private final Rect infoCard;
        private final Rect resizeHandle;
        private final float rowHeight;
        private final int visibleRows;
        private final float scale;

        private Layout(Rect window, Rect header, Rect headerDrag, Rect body, Rect listCard, Rect listRows, Rect scrollUp, Rect scrollDown, Rect offlineInput, Rect addOfflineButton, Rect msAutoButton, Rect loginSelectedButton, Rect deleteSelectedButton, Rect clearAllButton, Rect cancelWaitButton, Rect backButton, Rect infoCard, Rect resizeHandle, float rowHeight, int visibleRows, float scale)
        {
            this.window = window;
            this.header = header;
            this.headerDrag = headerDrag;
            this.body = body;
            this.listCard = listCard;
            this.listRows = listRows;
            this.scrollUp = scrollUp;
            this.scrollDown = scrollDown;
            this.offlineInput = offlineInput;
            this.addOfflineButton = addOfflineButton;
            this.msAutoButton = msAutoButton;
            this.loginSelectedButton = loginSelectedButton;
            this.deleteSelectedButton = deleteSelectedButton;
            this.clearAllButton = clearAllButton;
            this.cancelWaitButton = cancelWaitButton;
            this.backButton = backButton;
            this.infoCard = infoCard;
            this.resizeHandle = resizeHandle;
            this.rowHeight = rowHeight;
            this.visibleRows = visibleRows;
            this.scale = UiMotion.clamp(scale, 0.35F, 1.85F);
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
