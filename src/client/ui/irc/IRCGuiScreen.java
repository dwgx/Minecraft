package client.ui.irc;

import client.chat.model.*;
import client.chat.service.ChatService;
import client.chat.service.ChatServiceListener;
import client.chat.store.ChatStore;
import client.core.ClientBootstrap;
import client.module.Module;
import client.module.impl.client.IRCGuiModule;
import client.module.impl.client.ClickGuiModule;
import client.render.RenderContext2D;
import client.ui.NanoRenderableScreen;
import client.ui.irc.component.*;
import client.ui.irc.input.IRCMultilineInput;
import client.ui.layout.UiRect;
import client.ui.template.*;
import dwgx.IRCBackend.IRCChatServiceAdapter;
import dwgx.IRCBackend.IRCConfig;
import dwgx.IRCBackend.IRCServiceClient;
import dwgx.IRCBackend.IRCServiceListener;
import dwgx.nano.*;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.nanovg.NanoVG;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Main chat overlay screen. Delegates rendering to extracted components.
 * Coordinates navigation, input handling, data flow, and user interactions.
 *
 * Left-click avatar → user popup (view profile / add friend).
 * Right-click avatar → quick DM.
 * Animation channels use "chat." prefix via UiAnimationBus.
 *
 * Supports IRCChatServiceAdapter connected to the embedded local IRC server (port 1378).
 * Minecraft session username is used as the IRC nickname.
 */
public final class IRCGuiScreen extends GuiScreen implements NanoRenderableScreen
{
    private static final String ANIM_PREFIX = "chat.";

    private final IRCGuiModule module;
    private final ChatService chatService;
    private final ChatStore store;
    private final UiWindowState windowState;
    private final IRCLayout layout;

    private NanoTheme theme;
    private UiAnimProfile animProfile;
    private int lastMouseX;
    private int lastMouseY;
    private long lastVg; // cached for cleanup

    // Navigation state
    private int navMode; // 0=Chat, 1=Friends, 2=Mail, 3=Feed
    private int selectedServerIndex;
    private String activeConversationId;
    private volatile String pendingImagePath;
    private volatile String pendingImageConvId;
    private String myUid = "";

    // Components — Chat mode
    private final IRCTopBar topBar;
    private final IRCServerList serverList;
    private final IRCChannelList channelList;
    private final IRCMessageArea messageArea;
    private final IRCInputBar inputBar;
    private final IRCStatusBar statusBar;
    private final IRCMultilineInput textInput;
    private final IRCUserPopup userPopup;
    private final IRCImagePreview imagePreview;
    private final IRCVoicePlayer voicePlayer;
    private final IRCEmojiPicker emojiPicker;
    private final IRCContextMenu contextMenu;

    // Components — Social modes
    private final IRCFriendList friendList;
    private final IRCFriendRequestPopup friendRequestPopup;
    private final IRCProfilePanel profilePanel;
    private final IRCProfileEditPanel profileEditPanel;
    private final IRCMailInbox mailInbox;
    private final IRCMailCompose mailCompose;
    private final IRCMailDetail mailDetail;
    private final IRCSocialFeed socialFeed;
    private final IRCSocialCompose socialCompose;

    // Service listener for async IRC events
    private final ChatServiceListener serviceListener;
    private IRCServiceListener ircServiceListener;

    /** Singleton adapter — survives screen close/reopen to avoid reconnect lag. */
    private static volatile IRCChatServiceAdapter sharedAdapter;

    public IRCGuiScreen(IRCGuiModule module)
    {
        this.module = module;
        this.chatService = getOrCreateAdapter(module);
        this.store = new ChatStore();
        UiLayoutProfile lp = resolveLayoutProfile();
        this.windowState = new UiWindowState(lp.chatMinWidth(), lp.chatMinHeight());
        this.layout = new IRCLayout();

        this.topBar = new IRCTopBar();
        this.serverList = new IRCServerList();
        this.channelList = new IRCChannelList();
        this.messageArea = new IRCMessageArea();
        this.inputBar = new IRCInputBar();
        this.statusBar = new IRCStatusBar();
        this.textInput = new IRCMultilineInput();
        this.userPopup = new IRCUserPopup();
        this.imagePreview = new IRCImagePreview();
        this.voicePlayer = new IRCVoicePlayer();
        this.emojiPicker = new IRCEmojiPicker();
        this.contextMenu = new IRCContextMenu();

        // Social components
        this.friendList = new IRCFriendList();
        this.friendRequestPopup = new IRCFriendRequestPopup();
        this.profilePanel = new IRCProfilePanel();
        this.profileEditPanel = new IRCProfileEditPanel();
        this.mailInbox = new IRCMailInbox();
        this.mailCompose = new IRCMailCompose();
        this.mailDetail = new IRCMailDetail();
        this.socialFeed = new IRCSocialFeed();
        this.socialCompose = new IRCSocialCompose();

        this.textInput.setSendListener(new IRCMultilineInput.SendListener()
        {
            @Override
            public void onSend(String text) { sendMessage(text); }
        });

        // Service listener bridges async events into the store
        this.serviceListener = new ChatServiceListener()
        {
            @Override public void onMessageReceived(ChatMessage message)
            {
                store.addMessage(message.getConversationId(), message);
                // Auto-scroll to bottom if viewing the same conversation
                if (message.getConversationId() != null
                        && message.getConversationId().equals(activeConversationId))
                {
                    messageArea.scrollToBottom();
                }
            }
            @Override public void onUserStatusChanged(String userId, UserStatus newStatus)
            {
                ChatUser u = store.getUser(userId);
                if (u != null) u.setStatus(newStatus);
            }
            @Override public void onConversationUpdated(ChatConversation conversation)
            {
                // Store already has reference — just refresh
            }
            @Override public void onGroupUpdated(ChatGroup group) { }
            @Override public void onConnectionStateChanged(boolean connected)
            {
                store.setConnected(connected);
                store.setConnectionStatus(connected ? "Connected" : "Reconnecting...");
                if (connected)
                {
                    // Refresh store from service — IRC creates conversations on join
                    store.setConversations(chatService.getConversations());
                    store.setGroups(chatService.getGroups());
                    for (ChatUser u : chatService.getContacts()) store.putUser(u);
                    if (activeConversationId == null)
                    {
                        List<ChatConversation> convs = store.getConversations();
                        if (!convs.isEmpty())
                        {
                            activeConversationId = convs.get(0).getId();
                            store.setActiveConversationId(activeConversationId);
                            messageArea.scrollToBottom();
                        }
                    }
                    // Request history for active conversation from server
                    if (activeConversationId != null)
                    {
                        requestHistoryForConversation(activeConversationId);
                        messageArea.scrollToBottom();
                    }
                    // Load own profile + friend list + UID
                    if (chatService instanceof IRCChatServiceAdapter)
                    {
                        IRCServiceClient sc2 = ((IRCChatServiceAdapter) chatService).getServiceClient();
                        if (sc2 != null && store.getLocalUser() != null)
                        {
                            sc2.queryProfile(store.getLocalUser().getNickname());
                            sc2.listFriends();
                            sc2.requestMyUid();
                        }
                    }
                }
            }
            @Override public void onVoiceCallStateChanged(boolean active, String conversationId) { }
            @Override public void onError(String errorMessage)
            {
                if (errorMessage != null) store.setConnectionStatus("Error: " + errorMessage);
            }
        };
        this.chatService.addListener(this.serviceListener);

        // Populate store
        this.store.setLocalUser(chatService.getLocalUser());
        this.store.setConversations(chatService.getConversations());
        this.store.setGroups(chatService.getGroups());
        for (ChatUser u : chatService.getContacts())
        {
            this.store.putUser(u);
        }

        List<ChatConversation> convs = this.store.getConversations();
        if (!convs.isEmpty())
        {
            this.activeConversationId = convs.get(0).getId();
            this.store.setActiveConversationId(this.activeConversationId);
            this.store.setMessages(this.activeConversationId,
                    chatService.getMessages(this.activeConversationId, 0, 500));
            this.messageArea.scrollToBottom();
        }

        // Connect if IRC backend
        if (this.chatService instanceof IRCChatServiceAdapter)
        {
            final IRCChatServiceAdapter adapter = (IRCChatServiceAdapter) this.chatService;
            if (!adapter.isConnected())
            {
                System.out.println("[IRCGUI] Connecting to IRC server...");
                this.chatService.connect();
            }
            else
            {
                System.out.println("[IRCGUI] Reusing existing IRC connection");
                // Already connected — refresh data immediately
                IRCServiceClient sc2 = adapter.getServiceClient();
                if (sc2 != null)
                {
                    sc2.queryProfile(this.store.getLocalUser().getNickname());
                    sc2.listFriends();
                    sc2.requestMyUid();
                }
            }

            // Wire up service listener for social features
            IRCServiceClient sc = adapter.getServiceClient();
            if (sc != null)
            {
                this.ircServiceListener = new IRCServiceListener()
                {
                    public void onServiceReply(String command, String jsonPayload)
                    {
                        if ("MSG_HISTORY_CHANNEL".equals(command) || "MSG_HISTORY_PRIVATE".equals(command))
                        {
                            handleHistoryReply(command, jsonPayload);
                        }
                        else if ("PROFILE_QUERY".equals(command))
                        {
                            handleProfileReply(jsonPayload);
                        }
                        else if ("FRIEND_LIST".equals(command))
                        {
                            handleFriendListReply(jsonPayload);
                        }
                        else if ("FRIEND_ADD_BY_UID".equals(command))
                        {
                            handleFriendAddByUidReply(jsonPayload);
                        }
                        else if ("GET_MY_UID".equals(command))
                        {
                            handleMyUidReply(jsonPayload);
                        }
                    }
                    public void onFriendRequestNotify(String fromNick)
                    {
                        FriendEntry entry = new FriendEntry(fromNick, fromNick, false, FriendStatus.PENDING_IN);
                        store.addFriend(entry);
                    }
                    public void onFriendAcceptNotify(String nick)
                    {
                        // Update friend status to accepted
                        store.removeFriend(nick);
                        store.addFriend(new FriendEntry(nick, nick, true, FriendStatus.ACCEPTED));
                    }
                    public void onMailNotify(String fromNick, String subject, long mailId)
                    {
                        store.setUnreadMailCount(store.getUnreadMailCount() + 1);
                    }
                };
                sc.addListener(this.ircServiceListener);
            }
        }
    }

    /**
     * Factory: creates IRC adapter connected to the external IRC server (default localhost:1378).
     * Uses Minecraft session username as the IRC nickname.
     */
    private static ChatService getOrCreateAdapter(IRCGuiModule module)
    {
        if (sharedAdapter != null && sharedAdapter.isConnected())
        {
            // Reuse existing connection
            if (module != null)
            {
                sharedAdapter.setAllowDm(module.isAllowDm());
                sharedAdapter.setMaxHistoryPerConversation(module.getMaxHistorySize());
            }
            return sharedAdapter;
        }
        IRCConfig ircConfig = loadIrcConfig();
        System.out.println("[IRCGUI] IRC config: " + ircConfig.getServerHost() + ":" + ircConfig.getServerPort() + " nick=" + ircConfig.getNickname());
        IRCChatServiceAdapter adapter = new IRCChatServiceAdapter(ircConfig);
        if (module != null)
        {
            adapter.setAllowDm(module.isAllowDm());
            adapter.setMaxHistoryPerConversation(module.getMaxHistorySize());
        }
        sharedAdapter = adapter;
        return adapter;
    }

    private static ChatService createChatService(IRCGuiModule module)
    {
        IRCConfig ircConfig = loadIrcConfig();
        System.out.println("[IRCGUI] IRC config: " + ircConfig.getServerHost() + ":" + ircConfig.getServerPort() + " nick=" + ircConfig.getNickname());
        IRCChatServiceAdapter adapter = new IRCChatServiceAdapter(ircConfig);
        if (module != null)
        {
            adapter.setAllowDm(module.isAllowDm());
            adapter.setMaxHistoryPerConversation(module.getMaxHistorySize());
        }
        return adapter;
    }

    /**
     * Build IRC config. Reads irc.json overrides if present,
     * otherwise defaults to localhost:1378 with the Minecraft session username.
     */
    private static IRCConfig loadIrcConfig()
    {
        IRCConfig cfg = new IRCConfig(); // defaults: 127.0.0.1:1378

        // Pull Minecraft session username
        try
        {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc != null && mc.getSession() != null)
            {
                String mcName = mc.getSession().getUsername();
                if (mcName != null && !mcName.isEmpty())
                {
                    cfg.setNickname(mcName);
                    cfg.setUsername(mcName);
                    cfg.setRealName(mcName);
                }
            }
        }
        catch (Exception ignored) {}

        // Optional overrides from config file
        try
        {
            java.nio.file.Path configFile = java.nio.file.Paths.get("config", "client", "irc.json");
            if (java.nio.file.Files.exists(configFile))
            {
                String json = new String(java.nio.file.Files.readAllBytes(configFile), java.nio.charset.StandardCharsets.UTF_8);
                com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(json).getAsJsonObject();

                if (obj.has("host")) cfg.setServerHost(obj.get("host").getAsString());
                if (obj.has("port")) cfg.setServerPort(obj.get("port").getAsInt());
                if (obj.has("tls")) cfg.setUseTls(obj.get("tls").getAsBoolean());
                if (obj.has("nickname")) cfg.setNickname(obj.get("nickname").getAsString());
                if (obj.has("username")) cfg.setUsername(obj.get("username").getAsString());
                if (obj.has("password")) cfg.setServerPassword(obj.get("password").getAsString());
            }
        }
        catch (Exception ignored) {}

        return cfg;
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui()
    {
        super.initGui();
        UiLayoutProfile lp = resolveLayoutProfile();
        float sw = this.width;
        float sh = this.height;
        float ww = Math.min(lp.chatWidth(), sw - 40.0F);
        float wh = Math.min(lp.chatHeight(), sh - 40.0F);
        float wx = (sw - ww) * 0.5F;
        float wy = (sh - wh) * 0.5F;
        this.windowState.setImmediate(wx, wy, ww, wh);
        this.windowState.setTarget(wx, wy, ww, wh, sw, sh, 8.0F);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
    }

    @Override
    public void renderNano(RenderContext2D context)
    {
        if (context == null || context.getNanoVG() == null) return;
        long vg = context.getNanoVG().getHandle();
        if (vg == 0L) return;
        this.lastVg = vg;

        NanoFontBook.ensureLoaded(vg);
        resolveThemeAndAnim();

        // Process pending image from file chooser thread
        String imgPath = this.pendingImagePath;
        String imgConv = this.pendingImageConvId;
        if (imgPath != null && imgConv != null)
        {
            this.pendingImagePath = null;
            this.pendingImageConvId = null;
            sendImage(imgConv, imgPath);
        }

        float sw = this.width;
        float sh = this.height;
        int mx = this.lastMouseX;
        int my = this.lastMouseY;

        // Tick voice player
        this.voicePlayer.tick();

        // Fade-in via UiAnimationBus
        float openT = UiAnimationBus.animateControl(ANIM_PREFIX + "open", 1.0F, this.animProfile);
        float alpha = UiMotion.clamp01(openT);

        this.windowState.tick(this.animProfile);
        this.layout.compute(this.windowState.getX(), this.windowState.getY(),
                this.windowState.getWidth(), this.windowState.getHeight());

        try (org.lwjgl.system.MemoryStack stack = stackPush())
        {
            // Backdrop
            float opacity = this.module != null ? this.module.getOverlayOpacity() : 0.65F;
            int backdropArgb = NanoRenderUtils.withAlpha(0xFF000000, (int)(opacity * alpha * 255.0F));
            NanoRenderUtils.fillRect(vg, 0, 0, sw, sh, NanoRenderUtils.argb(stack, backdropArgb));

            // Window
            NanoUi.drawWindow(vg, stack, this.windowState.getX(), this.windowState.getY(),
                    this.windowState.getWidth(), this.windowState.getHeight(), this.theme);

            try
            {
                this.topBar.render(vg, stack, this.theme, this.layout.topBar,
                        this.store.getLocalUser(), resolveTopBarTitle(), mx, my);
                this.serverList.render(vg, stack, this.theme, this.layout.serverList,
                        this.store.getGroups(), this.selectedServerIndex, this.navMode,
                        this.store.getUnreadMailCount(), countPendingFriends(), mx, my);

                if (this.navMode == IRCServerList.NAV_CHAT)
                {
                    this.channelList.render(vg, stack, this.theme, this.layout.channelList,
                            this.store, this.selectedServerIndex, this.activeConversationId, mx, my);
                    this.messageArea.render(vg, stack, this.theme, this.layout.messageArea,
                            this.store, this.activeConversationId, mx, my);
                    this.inputBar.render(vg, stack, this.theme, this.layout.inputBar,
                            this.textInput, mx, my);
                }
                else if (this.navMode == IRCServerList.NAV_FRIENDS)
                {
                    // Left: friend list, Right: profile panel or edit panel
                    UiRect leftR = this.layout.channelList;
                    UiRect rightR = mergeRect(this.layout.messageArea, this.layout.inputBar);
                    this.friendList.setMyUid(this.myUid);
                    this.friendList.setAnimProfile(this.animProfile);
                    this.friendList.render(vg, stack, this.theme, leftR,
                            this.store.getFriends(), countPendingFriends(), mx, my);
                    if (this.profileEditPanel.isVisible())
                    {
                        this.profileEditPanel.render(vg, stack, this.theme, rightR, mx, my);
                    }
                    else if (this.store.getViewingProfile() != null)
                    {
                        this.profilePanel.show(this.store.getViewingProfile());
                        boolean isSelf = this.store.getLocalUser() != null
                                && this.store.getViewingProfile().getNick().equals(
                                        this.store.getLocalUser().getNickname());
                        this.profilePanel.render(vg, stack, this.theme, rightR, mx, my, isSelf);
                    }
                    else
                    {
                        // No profile selected — show hint
                        NanoRenderUtils.fillRoundedRect(vg, rightR.x, rightR.y, rightR.w, rightR.h,
                                this.theme.surfaceRadius(),
                                NanoRenderUtils.argb(stack, this.theme.mainArgb()));
                        NanoRenderUtils.drawLabel(vg, stack, NanoFontBook.uiRegular(),
                                rightR.x + rightR.w * 0.5F, rightR.y + rightR.h * 0.5F,
                                13.0F, "Select a friend to view their profile",
                                this.theme.textMutedArgb(),
                                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
                    }
                }
                else if (this.navMode == IRCServerList.NAV_MAIL)
                {
                    // Left: mail list, Right: detail or compose
                    UiRect leftR = this.layout.channelList;
                    UiRect rightR = mergeRect(this.layout.messageArea, this.layout.inputBar);
                    this.mailInbox.render(vg, stack, this.theme, leftR,
                            this.store.getMails(), mx, my);
                    if (this.mailCompose.isVisible())
                    {
                        this.mailCompose.render(vg, stack, this.theme, rightR, mx, my);
                    }
                    else
                    {
                        this.mailDetail.render(vg, stack, this.theme, rightR, mx, my);
                    }
                }
                else if (this.navMode == IRCServerList.NAV_FEED)
                {
                    // Left: compose, Right: feed
                    UiRect leftR = this.layout.channelList;
                    UiRect rightR = mergeRect(this.layout.messageArea, this.layout.inputBar);
                    this.socialCompose.render(vg, stack, this.theme, leftR, mx, my);
                    this.socialFeed.render(vg, stack, this.theme, rightR,
                            this.store.getFeed(), mx, my);
                }

                this.statusBar.render(vg, stack, this.theme, this.layout.statusBar, this.store);

                // Overlays (rendered on top, in z-order)
                this.userPopup.render(vg, stack, this.theme, mx, my);
                this.emojiPicker.render(vg, stack, this.theme, mx, my);
                this.contextMenu.render(vg, stack, this.theme, mx, my);
                this.imagePreview.render(vg, stack, this.theme, sw, sh, mx, my);
            }
            catch (Exception ignored)
            {
                // Backdrop already drawn — prevent black screen
            }
        }
    }

    private static UiLayoutProfile resolveLayoutProfile()
    {
        ClientBootstrap boot = ClientBootstrap.instance();
        if (boot != null && boot.getConfigManager() != null)
        {
            return boot.getConfigManager().getUiLayout();
        }
        return new UiLayoutProfile();
    }

    private void resolveThemeAndAnim()
    {
        Module mod = ClientBootstrap.instance().getModules().getById("click_gui");
        ClickGuiModule clickGui = (mod instanceof ClickGuiModule) ? (ClickGuiModule) mod : null;

        if (clickGui != null)
        {
            Integer accent = clickGui.isAccentOverrideEnabled()
                    ? Integer.valueOf(clickGui.getAccentOverride().toArgb()) : null;
            int backdrop = clickGui.isBackdropEnabled() ? clickGui.getBackdropAlpha() : 0;
            this.theme = NanoThemes.create(clickGui.getPalette(), clickGui.getPanelAlpha(),
                    backdrop, clickGui.getCornerRadius(), accent);
            this.animProfile = UiAnimProfiles.fromClickGui(clickGui, 0.56F, 0.62F, 0.62F);
        }
        if (this.theme == null) this.theme = NanoThemes.create(NanoPalette.COBALT, 220, 160, 9.0F, null);
        if (this.animProfile == null) this.animProfile = UiAnimProfile.defaults();

        // Distribute anim profile to components
        this.serverList.setAnimProfile(this.animProfile);
        this.channelList.setAnimProfile(this.animProfile);
        this.friendList.setAnimProfile(this.animProfile);
        this.mailInbox.setAnimProfile(this.animProfile);
        this.socialFeed.setAnimProfile(this.animProfile);
    }

    private String resolveConversationTitle()
    {
        if (this.activeConversationId == null) return null;
        ChatConversation conv = this.store.getConversation(this.activeConversationId);
        if (conv == null) return null;
        if (conv.isDm())
        {
            for (String pid : conv.getParticipantIds())
            {
                if (!"local".equals(pid))
                {
                    ChatUser u = this.store.getUser(pid);
                    if (u != null) return u.getNickname();
                }
            }
            return "DM";
        }
        String chId = conv.getChannelId();
        if (chId != null)
        {
            for (ChatGroup g : this.store.getGroups())
            {
                for (ChatChannel ch : g.getChannels())
                {
                    if (ch.getId().equals(chId)) return "# " + ch.getName();
                }
            }
        }
        return null;
    }

    private String resolveTopBarTitle()
    {
        switch (this.navMode)
        {
            case IRCServerList.NAV_FRIENDS: return "Friends";
            case IRCServerList.NAV_MAIL: return "Mail";
            case IRCServerList.NAV_FEED: return "Feed";
            default: return resolveConversationTitle();
        }
    }

    private int countPendingFriends()
    {
        int count = 0;
        for (FriendEntry f : this.store.getFriends())
        {
            if (f.getFriendStatus() == FriendStatus.PENDING_IN) count++;
        }
        return count;
    }

    private static UiRect mergeRect(UiRect a, UiRect b)
    {
        if (a == null) return b;
        if (b == null) return a;
        float x = Math.min(a.x, b.x);
        float y = Math.min(a.y, b.y);
        float x2 = Math.max(a.x + a.w, b.x + b.w);
        float y2 = Math.max(a.y + a.h, b.y + b.h);
        return new UiRect(x, y, x2 - x, y2 - y);
    }

    // --- Input handling ---

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        // Image preview takes absolute priority
        if (this.imagePreview.isVisible())
        {
            this.imagePreview.handleClick(mouseX, mouseY);
            return;
        }

        // Context menu
        if (this.contextMenu.isVisible())
        {
            String action = this.contextMenu.handleClick(mouseX, mouseY);
            if (action != null)
            {
                handleContextAction(action);
                return;
            }
            return;
        }

        // Emoji picker
        if (this.emojiPicker.isVisible())
        {
            String emoji = this.emojiPicker.handleClick(mouseX, mouseY);
            if (emoji != null)
            {
                if (!emoji.isEmpty())
                {
                    this.textInput.insertText(emoji + " ");
                }
                return;
            }
            return;
        }

        // User popup takes priority
        if (this.userPopup.isVisible())
        {
            int popupResult = this.userPopup.handleClick(mouseX, mouseY);
            if (popupResult == -1)
            {
                this.userPopup.hide();
                return;
            }
            if (popupResult == 1)
            {
                // Send friend request via IRC service
                ChatUser target = this.userPopup.getUser();
                if (target != null && this.chatService instanceof IRCChatServiceAdapter)
                {
                    IRCServiceClient sc = ((IRCChatServiceAdapter) this.chatService).getServiceClient();
                    if (sc != null)
                    {
                        sc.sendFriendRequest(target.getNickname());
                    }
                }
                this.userPopup.hide();
                return;
            }
            if (popupResult == 2)
            {
                // Send DM
                ChatUser target = this.userPopup.getUser();
                if (target != null) openDmWith(target.getId());
                this.userPopup.hide();
                return;
            }
            return;
        }

        // Right-click in message area → view profile or context menu
        if (mouseButton == 1)
        {
            ChatUser clicked = hitTestMessageAvatar(mouseX, mouseY);
            if (clicked != null && !"local".equals(clicked.getId()))
            {
                // Right-click avatar → query and show profile
                if (this.chatService instanceof IRCChatServiceAdapter)
                {
                    IRCServiceClient sc = ((IRCChatServiceAdapter) this.chatService).getServiceClient();
                    if (sc != null) sc.queryProfile(clicked.getNickname());
                }
                this.navMode = IRCServerList.NAV_FRIENDS;
                return;
            }
            // Right-click on message bubble → context menu
            String msgId = hitTestMessageBubble(mouseX, mouseY);
            if (msgId != null)
            {
                ChatMessage msg = findMessageById(msgId);
                boolean isOwn = msg != null && "local".equals(msg.getSenderId());
                this.contextMenu.showForMessage(msgId, mouseX, mouseY, this.width, this.height, isOwn);
                return;
            }
        }

        // Left-click in message area → show user popup
        if (mouseButton == 0)
        {
            ChatUser clicked = hitTestMessageAvatar(mouseX, mouseY);
            if (clicked != null)
            {
                this.userPopup.show(clicked, mouseX, mouseY, this.width, this.height);
                return;
            }
        }

        // Close button
        if (this.topBar.hitClose(this.layout.topBar, mouseX, mouseY))
        {
            this.mc.displayGuiScreen(null);
            return;
        }

        // Server list — navigation mode switch
        int navHit = this.serverList.hitTestNav(this.layout.serverList, mouseX, mouseY);
        if (navHit >= 0)
        {
            this.navMode = navHit;
            // When switching to friends mode, show own profile if nothing else is selected
            if (navHit == IRCServerList.NAV_FRIENDS && this.store.getViewingProfile() == null)
            {
                UserProfile mp = this.store.getMyProfile();
                if (mp != null) this.store.setViewingProfile(mp);
                // Also request fresh profile data
                if (this.chatService instanceof IRCChatServiceAdapter)
                {
                    IRCServiceClient sc = ((IRCChatServiceAdapter) this.chatService).getServiceClient();
                    if (sc != null && this.store.getLocalUser() != null)
                    {
                        sc.queryProfile(this.store.getLocalUser().getNickname());
                    }
                }
            }
            return;
        }

        // Server list — chat mode server/group selection
        if (this.navMode == IRCServerList.NAV_CHAT)
        {
            int serverHit = this.serverList.hitTestServer(this.layout.serverList,
                    this.store.getGroups(), mouseX, mouseY);
            if (serverHit >= 0)
            {
                this.selectedServerIndex = serverHit;
                if (serverHit == 0)
                {
                    selectFirstDm();
                }
                else
                {
                    int groupIdx = serverHit - 1;
                    List<ChatGroup> groups = this.store.getGroups();
                    if (groupIdx >= 0 && groupIdx < groups.size())
                    {
                        selectFirstChannelInGroup(groups.get(groupIdx));
                    }
                }
                return;
            }
        }

        // Mode-specific click handling
        if (this.navMode == IRCServerList.NAV_FRIENDS)
        {
            // Profile edit panel
            if (this.profileEditPanel.isVisible())
            {
                UiRect rightR = mergeRect(this.layout.messageArea, this.layout.inputBar);
                this.profileEditPanel.handleClick(mouseX, mouseY, rightR);
                if (this.profileEditPanel.isSaveHovered())
                {
                    // Save profile to server
                    if (this.chatService instanceof IRCChatServiceAdapter)
                    {
                        IRCServiceClient sc = ((IRCChatServiceAdapter) this.chatService).getServiceClient();
                        if (sc != null)
                        {
                            String json = "{\"displayName\":\"" + escapeJson(this.profileEditPanel.getDisplayName())
                                    + "\",\"bio\":\"" + escapeJson(this.profileEditPanel.getBio())
                                    + "\",\"themeColor\":\"" + escapeJson(this.profileEditPanel.getThemeColor()) + "\"}";
                            sc.updateProfile(json);
                        }
                    }
                    // Update local profile
                    UserProfile mp = this.store.getMyProfile();
                    if (mp == null) { mp = new UserProfile(this.store.getLocalUser() != null ? this.store.getLocalUser().getNickname() : "local"); }
                    mp.setDisplayName(this.profileEditPanel.getDisplayName());
                    mp.setBio(this.profileEditPanel.getBio());
                    mp.setThemeColor(this.profileEditPanel.getThemeColor());
                    this.store.setMyProfile(mp);
                    this.store.setViewingProfile(mp);
                    this.profileEditPanel.hide();
                }
                return;
            }
            // Profile panel action buttons
            if (this.profilePanel.isVisible())
            {
                int profileAction = this.profilePanel.handleClick();
                if (profileAction == 1)
                {
                    // Add friend from profile panel
                    UserProfile vp = this.profilePanel.getProfile();
                    if (vp != null && this.chatService instanceof IRCChatServiceAdapter)
                    {
                        IRCServiceClient sc = ((IRCChatServiceAdapter) this.chatService).getServiceClient();
                        if (sc != null) sc.sendFriendRequest(vp.getNick());
                    }
                    return;
                }
                if (profileAction == 2)
                {
                    // Send mail from profile panel
                    UserProfile vp = this.profilePanel.getProfile();
                    if (vp != null)
                    {
                        this.navMode = IRCServerList.NAV_MAIL;
                        this.mailCompose.show(vp.getNick());
                    }
                    return;
                }
                if (profileAction == 3)
                {
                    // Edit own profile
                    UserProfile mp = this.store.getMyProfile();
                    if (mp == null)
                    {
                        // Fallback: create from viewing profile or local user
                        UserProfile vp = this.store.getViewingProfile();
                        if (vp != null) mp = vp;
                        else
                        {
                            String nick = this.store.getLocalUser() != null
                                    ? this.store.getLocalUser().getNickname() : "local";
                            mp = new UserProfile(nick);
                        }
                        this.store.setMyProfile(mp);
                    }
                    this.profileEditPanel.show(mp);
                    return;
                }
            }
            // Friend request popup
            if (this.friendRequestPopup.isVisible())
            {
                int reqAction = this.friendRequestPopup.handleClick(mouseX, mouseY);
                if (reqAction == 1 || reqAction == 2)
                {
                    String nick = this.friendRequestPopup.getNick();
                    if (nick != null && this.chatService instanceof IRCChatServiceAdapter)
                    {
                        IRCServiceClient sc = ((IRCChatServiceAdapter) this.chatService).getServiceClient();
                        if (sc != null)
                        {
                            if (reqAction == 1) sc.acceptFriendRequest(nick);
                            else sc.rejectFriendRequest(nick);
                            // Refresh friend list after accept/reject
                            sc.listFriends();
                        }
                    }
                    // Remove pending entry from local store
                    this.store.removeFriend(nick);
                    this.friendRequestPopup.hide();
                    return;
                }
                if (reqAction == -1) { this.friendRequestPopup.hide(); return; }
            }
            // UID input field focus + Add button
            UiRect friendLeftR = this.layout.channelList;
            this.friendList.handleClick(mouseX, mouseY, friendLeftR);
            if (this.friendList.isAddBtnHovered() && !this.friendList.getUidInput().isEmpty())
            {
                if (this.chatService instanceof IRCChatServiceAdapter)
                {
                    IRCServiceClient sc = ((IRCChatServiceAdapter) this.chatService).getServiceClient();
                    if (sc != null)
                    {
                        sc.addFriendByUid(this.friendList.getUidInput());
                        this.friendList.clearUidInput();
                    }
                }
                return;
            }
            // Click on friend list item
            int friendIdx = this.friendList.getHoveredIndex();
            if (friendIdx < 0 && friendIdx != -1)
            {
                // Negative index (other than -1) = pending request entry
                int pendingIdx = -(friendIdx + 1);
                List<FriendEntry> allFriends = this.store.getFriends();
                List<FriendEntry> pending = new ArrayList<FriendEntry>();
                for (FriendEntry f : allFriends)
                {
                    if (f.getFriendStatus() == FriendStatus.PENDING_IN) pending.add(f);
                }
                if (pendingIdx >= 0 && pendingIdx < pending.size())
                {
                    this.friendRequestPopup.show(pending.get(pendingIdx).getNick(),
                            mouseX, mouseY, this.width, this.height);
                }
                return;
            }
            if (friendIdx >= 0)
            {
                // Positive index = accepted friend (online then offline)
                List<FriendEntry> allFriends = this.store.getFriends();
                List<FriendEntry> accepted = new ArrayList<FriendEntry>();
                for (FriendEntry f : allFriends)
                {
                    if (f.getFriendStatus() == FriendStatus.ACCEPTED) accepted.add(f);
                }
                if (friendIdx < accepted.size())
                {
                    FriendEntry fe = accepted.get(friendIdx);
                    if (this.chatService instanceof IRCChatServiceAdapter)
                    {
                        IRCServiceClient sc = ((IRCChatServiceAdapter) this.chatService).getServiceClient();
                        if (sc != null) sc.queryProfile(fe.getNick());
                    }
                }
            }
            return;
        }
        if (this.navMode == IRCServerList.NAV_MAIL)
        {
            UiRect leftR = this.layout.channelList;
            if (leftR != null && leftR.contains(mouseX, mouseY))
            {
                this.mailInbox.handleTabClick(mouseX, mouseY, leftR);
            }
            if (this.mailCompose.isVisible())
            {
                UiRect rightR = mergeRect(this.layout.messageArea, this.layout.inputBar);
                this.mailCompose.handleClick(mouseX, mouseY, rightR);
                if (this.mailCompose.isSendHovered())
                {
                    // Send mail via service
                    handleMailSend();
                }
            }
            return;
        }
        if (this.navMode == IRCServerList.NAV_FEED)
        {
            UiRect leftR = this.layout.channelList;
            if (leftR != null && leftR.contains(mouseX, mouseY))
            {
                if (this.socialCompose.isPostHovered())
                {
                    handleSocialPost();
                }
            }
            return;
        }

        // Channel list (Chat mode)
        String convHit = this.channelList.hitTest(this.layout.channelList,
                this.store, this.selectedServerIndex, mouseX, mouseY);
        if (convHit != null)
        {
            switchConversation(convHit);
            return;
        }

        // Input bar
        UiRect ib = this.layout.inputBar;
        if (ib != null && ib.contains(mouseX, mouseY))
        {
            if (this.inputBar.hitSend(ib, mouseX, mouseY))
            {
                String text = this.textInput.getText().trim();
                if (!text.isEmpty())
                {
                    sendMessage(text);
                    this.textInput.clear();
                }
                return;
            }
            // Emoji button hit (left side of input bar)
            if (this.inputBar.hitEmoji(ib, mouseX, mouseY))
            {
                if (this.emojiPicker.isVisible())
                {
                    this.emojiPicker.hide();
                }
                else
                {
                    this.emojiPicker.show(ib.x, ib.y, this.width, this.height);
                }
                return;
            }
            // Image button hit
            if (this.inputBar.hitImage(ib, mouseX, mouseY))
            {
                openImageFileChooser();
                return;
            }
            this.textInput.focus();
            return;
        }
        else
        {
            this.textInput.blur();
        }
    }

    /**
     * Hit-test message area avatars. Returns the ChatUser if mouse is over an avatar.
     */
    private ChatUser hitTestMessageAvatar(int mx, int my)
    {
        UiRect r = this.layout.messageArea;
        if (r == null || !r.contains(mx, my)) return null;

        List<ChatMessage> msgs = this.store.getMessages(this.activeConversationId);
        if (msgs.isEmpty()) return null;

        String localId = this.store.getLocalUser() != null ? this.store.getLocalUser().getId() : "local";
        float pad = 12.0F;
        float avatarSize = 28.0F;
        float smallFont = 10.0F;
        float bubblePad = 8.0F;
        float fontSize = 13.0F;
        float y = r.y + pad - this.messageArea.getScrollTarget();

        for (int i = 0; i < msgs.size(); i++)
        {
            ChatMessage msg = msgs.get(i);
            if (msg.getType() == MessageType.SYSTEM) { y += 28.0F; continue; }

            boolean isLocal = localId.equals(msg.getSenderId());
            float bubbleH = fontSize + bubblePad * 2.0F;
            float avatarY = y + smallFont + 4.0F;

            float avatarX;
            if (isLocal)
            {
                avatarX = r.x + r.w - pad - avatarSize;
            }
            else
            {
                avatarX = r.x + pad;
            }

            if (mx >= avatarX && mx <= avatarX + avatarSize
                    && my >= avatarY && my <= avatarY + avatarSize)
            {
                return this.store.getUser(msg.getSenderId());
            }

            y += smallFont + 4.0F + Math.max(avatarSize, bubbleH) + 8.0F;
        }
        return null;
    }

    /**
     * Hit-test message bubbles. Returns message ID if mouse is over a bubble.
     */
    private String hitTestMessageBubble(int mx, int my)
    {
        UiRect r = this.layout.messageArea;
        if (r == null || !r.contains(mx, my)) return null;

        List<ChatMessage> msgs = this.store.getMessages(this.activeConversationId);
        if (msgs.isEmpty()) return null;

        String localId = this.store.getLocalUser() != null ? this.store.getLocalUser().getId() : "local";
        float pad = 12.0F;
        float avatarSize = 28.0F;
        float smallFont = 10.0F;
        float bubblePad = 8.0F;
        float fontSize = 13.0F;
        float bubbleMaxW = r.w * 0.65F;
        float y = r.y + pad - this.messageArea.getScrollTarget();

        for (int i = 0; i < msgs.size(); i++)
        {
            ChatMessage msg = msgs.get(i);
            if (msg.getType() == MessageType.SYSTEM) { y += 28.0F; continue; }

            boolean isLocal = localId.equals(msg.getSenderId());
            String text = msg.getDisplayText() != null ? msg.getDisplayText() : "";
            if (msg.getType() == MessageType.IMAGE) text = "[Image]";
            if (msg.getType() == MessageType.VOICE) text = "Voice";

            float bubbleH = fontSize + bubblePad * 2.0F;
            float bubbleW = Math.min(bubbleMaxW, 200.0F);
            float avatarY = y + smallFont + 4.0F;

            float bubbleX;
            if (isLocal)
            {
                bubbleX = r.x + r.w - pad - avatarSize - 6.0F - bubbleW;
            }
            else
            {
                bubbleX = r.x + pad + avatarSize + 6.0F;
            }

            if (mx >= bubbleX && mx <= bubbleX + bubbleW
                    && my >= avatarY && my <= avatarY + bubbleH)
            {
                return msg.getId();
            }

            y += smallFont + 4.0F + Math.max(avatarSize, bubbleH) + 8.0F;
        }
        return null;
    }

    private ChatMessage findMessageById(String msgId)
    {
        if (msgId == null) return null;
        List<ChatMessage> msgs = this.store.getMessages(this.activeConversationId);
        for (ChatMessage m : msgs)
        {
            if (msgId.equals(m.getId())) return m;
        }
        return null;
    }

    private void handleContextAction(String action)
    {
        if (action == null || action.isEmpty()) return;

        if ("copy".equals(action))
        {
            ChatMessage msg = findMessageById(this.contextMenu.getTargetMessageId());
            if (msg != null && msg.getDisplayText() != null)
            {
                setClipboardString(msg.getDisplayText());
            }
        }
        else if ("dm".equals(action) || "profile".equals(action))
        {
            String userId = this.contextMenu.getTargetUserId();
            if (userId != null)
            {
                if ("dm".equals(action)) openDmWith(userId);
                if ("profile".equals(action))
                {
                    ChatUser u = this.store.getUser(userId);
                    if (u != null) this.userPopup.show(u, this.lastMouseX, this.lastMouseY, this.width, this.height);
                }
            }
        }
    }

    private void sendMessage(String text)
    {
        if (this.activeConversationId == null || text == null || text.isEmpty()) return;
        this.chatService.sendTextMessage(this.activeConversationId, text);
        this.store.setMessages(this.activeConversationId,
                this.chatService.getMessages(this.activeConversationId, 0, 500));
        this.messageArea.scrollToBottom();
    }

    private void openImageFileChooser()
    {
        if (this.activeConversationId == null) return;
        final String convId = this.activeConversationId;
        // Run file chooser on a separate thread to avoid blocking the render loop
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Send Image");
                    chooser.setFileFilter(new FileNameExtensionFilter(
                            "Images (png, jpg, gif, bmp)", "png", "jpg", "jpeg", "gif", "bmp"));
                    chooser.setMultiSelectionEnabled(false);
                    int result = chooser.showOpenDialog(null);
                    if (result == JFileChooser.APPROVE_OPTION)
                    {
                        File selected = chooser.getSelectedFile();
                        if (selected != null && selected.isFile())
                        {
                            pendingImageConvId = convId;
                            pendingImagePath = selected.getAbsolutePath();
                        }
                    }
                }
                catch (Exception ignored)
                {
                }
            }
        });
        thread.setDaemon(true);
        thread.setName("IRC-ImageChooser");
        thread.start();
    }

    private void sendImage(String conversationId, String imagePath)
    {
        if (conversationId == null || imagePath == null || imagePath.isEmpty()) return;
        this.chatService.sendImageMessage(conversationId, imagePath);
        this.store.setMessages(conversationId,
                this.chatService.getMessages(conversationId, 0, 500));
        this.messageArea.scrollToBottom();
    }

    private void handleMailSend()
    {
        String to = this.mailCompose.getToNick();
        String subject = this.mailCompose.getSubject();
        String body = this.mailCompose.getBody();
        if (to.isEmpty() || subject.isEmpty()) return;

        if (this.chatService instanceof IRCChatServiceAdapter)
        {
            IRCServiceClient sc = ((IRCChatServiceAdapter) this.chatService).getServiceClient();
            if (sc != null)
            {
                sc.sendMail(to, subject, body);
                this.mailCompose.hide();
            }
        }
    }

    private void handleSocialPost()
    {
        String content = this.socialCompose.getContent();
        if (content == null || content.trim().isEmpty()) return;

        if (this.chatService instanceof IRCChatServiceAdapter)
        {
            IRCServiceClient sc = ((IRCChatServiceAdapter) this.chatService).getServiceClient();
            if (sc != null)
            {
                sc.createPost(content.trim(), null);
                this.socialCompose.clear();
            }
        }
    }

    private void openDmWith(String userId)
    {
        ChatConversation dm = this.chatService.getOrCreateDm(userId);
        if (dm != null)
        {
            this.store.setConversations(this.chatService.getConversations());
            this.selectedServerIndex = 0;
            switchConversation(dm.getId());
        }
    }

    private void switchConversation(String convId)
    {
        this.activeConversationId = convId;
        this.store.setActiveConversationId(convId);
        this.messageArea.scrollToBottom();

        // Sync active conversation to adapter for unread tracking
        if (this.chatService instanceof IRCChatServiceAdapter)
        {
            ((IRCChatServiceAdapter) this.chatService).setActiveConversationId(convId);
        }

        if (this.store.getMessages(convId).isEmpty())
        {
            this.store.setMessages(convId, chatService.getMessages(convId, 0, 500));
        }

        // Request history from server if store is still empty
        if (this.store.getMessages(convId).isEmpty())
        {
            requestHistoryForConversation(convId);
        }

        ChatConversation conv = this.store.getConversation(convId);
        if (conv != null) conv.clearUnread();
    }

    private void selectFirstDm()
    {
        for (ChatConversation conv : this.store.getConversations())
        {
            if (conv.isDm()) { switchConversation(conv.getId()); return; }
        }
    }

    private void selectFirstChannelInGroup(ChatGroup group)
    {
        for (ChatChannel ch : group.getChannels())
        {
            if (ch.isText()) { switchConversation("conv-" + ch.getId()); return; }
        }
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int scroll = org.lwjgl.input.Mouse.getEventDWheel();
        if (scroll != 0)
        {
            if (this.navMode == IRCServerList.NAV_CHAT)
            {
                UiRect r = this.layout.messageArea;
                if (r != null && r.contains(this.lastMouseX, this.lastMouseY))
                {
                    this.messageArea.scroll(scroll);
                }
            }
            else if (this.navMode == IRCServerList.NAV_FRIENDS)
            {
                UiRect mr = this.layout.messageArea;
                UiRect cr = this.layout.channelList;
                if (mr != null && mr.contains(this.lastMouseX, this.lastMouseY))
                {
                    this.profilePanel.scroll(scroll);
                }
                else if (cr != null && cr.contains(this.lastMouseX, this.lastMouseY))
                {
                    this.friendList.scroll(scroll);
                }
            }
            else if (this.navMode == IRCServerList.NAV_MAIL)
            {
                UiRect r = this.layout.channelList;
                if (r != null && r.contains(this.lastMouseX, this.lastMouseY))
                {
                    this.mailInbox.scroll(scroll);
                }
            }
            else if (this.navMode == IRCServerList.NAV_FEED)
            {
                UiRect r = mergeRect(this.layout.messageArea, this.layout.inputBar);
                if (r != null && r.contains(this.lastMouseX, this.lastMouseY))
                {
                    this.socialFeed.scroll(scroll);
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode)
    {
        if (keyCode == 1)
        {
            if (this.imagePreview.isVisible()) { this.imagePreview.hide(); return; }
            if (this.contextMenu.isVisible()) { this.contextMenu.hide(); return; }
            if (this.emojiPicker.isVisible()) { this.emojiPicker.hide(); return; }
            if (this.userPopup.isVisible()) { this.userPopup.hide(); return; }
            if (this.profileEditPanel.isVisible()) { this.profileEditPanel.hide(); return; }
            if (this.mailCompose.isVisible()) { this.mailCompose.hide(); return; }
            if (this.textInput.isFocused()) { this.textInput.blur(); return; }
            this.mc.displayGuiScreen(null);
            return;
        }

        // Route typing to active mode
        if (this.navMode == IRCServerList.NAV_FRIENDS && this.friendList.isUidFieldFocused())
        {
            if (keyCode == 14) this.friendList.backspace();
            else if (keyCode == 28)
            {
                // Enter key — submit UID add
                if (!this.friendList.getUidInput().isEmpty() && this.chatService instanceof IRCChatServiceAdapter)
                {
                    IRCServiceClient sc = ((IRCChatServiceAdapter) this.chatService).getServiceClient();
                    if (sc != null)
                    {
                        sc.addFriendByUid(this.friendList.getUidInput());
                        this.friendList.clearUidInput();
                    }
                }
            }
            else if (typedChar >= 32) this.friendList.typeChar(typedChar);
            return;
        }
        if (this.navMode == IRCServerList.NAV_FRIENDS && this.profileEditPanel.isVisible())
        {
            if (keyCode == 14) this.profileEditPanel.backspace();
            else if (typedChar >= 32) this.profileEditPanel.typeChar(typedChar);
            return;
        }
        if (this.navMode == IRCServerList.NAV_MAIL && this.mailCompose.isVisible())
        {
            if (keyCode == 14) this.mailCompose.backspace();
            else if (typedChar >= 32) this.mailCompose.typeChar(typedChar);
            return;
        }
        if (this.navMode == IRCServerList.NAV_FEED)
        {
            if (keyCode == 14) this.socialCompose.backspace();
            else if (typedChar >= 32) this.socialCompose.typeChar(typedChar);
            return;
        }
        if (this.textInput.isFocused())
        {
            this.textInput.handleKeyTyped(typedChar, keyCode);
        }
    }

    /** Request chat history from server for a conversation. */
    private void requestHistoryForConversation(String convId)
    {
        if (!(this.chatService instanceof IRCChatServiceAdapter)) return;
        IRCServiceClient sc = ((IRCChatServiceAdapter) this.chatService).getServiceClient();
        if (sc == null) return;

        ChatConversation conv = this.store.getConversation(convId);
        if (conv == null) return;

        if (!conv.isDm())
        {
            String channel = "#" + conv.getGroupId();
            sc.requestChannelHistory(channel, 50, 0);
        }
        else
        {
            // DM conv id is "dm-<nick>"
            String nick = convId.startsWith("dm-") ? convId.substring(3) : convId;
            sc.requestPrivateHistory(nick, 50, 0);
        }
    }

    /** Handle MSG_HISTORY_CHANNEL or MSG_HISTORY_PRIVATE reply from server. */
    private void handleHistoryReply(String command, String jsonPayload)
    {
        try
        {
            JsonObject obj = new JsonParser().parse(jsonPayload).getAsJsonObject();
            if (obj.has("error")) return;

            String messagesStr = obj.has("messagesJson") ? obj.get("messagesJson").getAsString() : "[]";
            JsonArray arr = new JsonParser().parse(messagesStr).getAsJsonArray();
            if (arr.size() == 0) return;

            // Determine conversation ID
            String convId;
            if ("MSG_HISTORY_CHANNEL".equals(command))
            {
                String channel = obj.has("channel") ? obj.get("channel").getAsString() : "#general";
                String chName = channel.startsWith("#") ? channel.substring(1) : channel;
                convId = "conv-" + chName;
            }
            else
            {
                String nick = obj.has("nick") ? obj.get("nick").getAsString() : "";
                convId = "dm-" + nick;
            }

            // Parse messages (they come in DESC order, reverse to chronological)
            String localNick = this.store.getLocalUser() != null
                    ? this.store.getLocalUser().getNickname() : "";
            List<ChatMessage> history = new ArrayList<ChatMessage>();
            for (int i = arr.size() - 1; i >= 0; i--)
            {
                JsonObject m = arr.get(i).getAsJsonObject();
                String from = m.get("from").getAsString();
                String content = m.get("content").getAsString();
                long ts = m.get("ts").getAsLong();
                String senderId = from.equals(localNick) ? "local" : from;

                // Ensure user exists in store
                if (!senderId.equals("local") && this.store.getUser(senderId) == null)
                {
                    this.store.putUser(new ChatUser(senderId, from, null, UserStatus.OFFLINE));
                }

                ChatMessage msg = ChatMessage.text("hist-" + m.get("id").getAsLong(),
                        senderId, convId, content, ts);
                history.add(msg);
            }

            // Only add if conversation is still empty (avoid duplicates)
            if (this.store.getMessages(convId).isEmpty())
            {
                this.store.setMessages(convId, history);
                this.messageArea.scrollToBottom();
            }
        }
        catch (Exception e)
        {
            System.err.println("[IRCGUI] Failed to parse history reply: " + e.getMessage());
        }
    }

    /** Handle PROFILE_QUERY reply — populate viewingProfile in store. */
    private void handleProfileReply(String jsonPayload)
    {
        try
        {
            JsonObject root = new JsonParser().parse(jsonPayload).getAsJsonObject();
            if (root.has("error")) return;

            // Server returns {"ok":true,"profile":{...}} — unwrap the profile object
            JsonObject obj = root.has("profile") ? root.getAsJsonObject("profile") : root;

            String nick = obj.has("nick") ? obj.get("nick").getAsString() : "unknown";
            UserProfile profile = new UserProfile(nick);
            if (obj.has("displayName")) profile.setDisplayName(obj.get("displayName").getAsString());
            if (obj.has("bio")) profile.setBio(obj.get("bio").getAsString());
            if (obj.has("avatarHash")) profile.setAvatarHash(obj.get("avatarHash").getAsString());
            if (obj.has("themeColor")) profile.setThemeColor(obj.get("themeColor").getAsString());
            if (obj.has("gameStats")) profile.setGameStats(obj.get("gameStats").getAsString());
            if (obj.has("visitorCount")) profile.setVisitorCount(obj.get("visitorCount").getAsInt());
            if (obj.has("joinDate")) profile.setJoinDate(obj.get("joinDate").getAsLong());
            if (obj.has("uid")) profile.setUid(obj.get("uid").getAsString());

            this.store.setViewingProfile(profile);

            // Also set as own profile if it matches local user
            String localNick = this.store.getLocalUser() != null
                    ? this.store.getLocalUser().getNickname() : "";
            if (nick.equalsIgnoreCase(localNick))
            {
                this.store.setMyProfile(profile);
            }
        }
        catch (Exception e)
        {
            System.err.println("[IRCGUI] Failed to parse profile reply: " + e.getMessage());
        }
    }

    /** Handle FRIEND_LIST reply — populate friend list in store. */
    private void handleFriendListReply(String jsonPayload)
    {
        try
        {
            JsonObject obj = new JsonParser().parse(jsonPayload).getAsJsonObject();
            if (obj.has("error")) return;

            List<FriendEntry> allFriends = new ArrayList<FriendEntry>();

            // Parse accepted friends
            if (obj.has("friends"))
            {
                JsonArray arr = obj.getAsJsonArray("friends");
                for (JsonElement el : arr)
                {
                    JsonObject f = el.getAsJsonObject();
                    String nick = f.get("nick").getAsString();
                    boolean online = f.has("online") && f.get("online").getAsBoolean();
                    String displayName = f.has("displayName") ? f.get("displayName").getAsString() : nick;
                    allFriends.add(new FriendEntry(nick, displayName, online, FriendStatus.ACCEPTED));
                }
            }

            // Parse pending requests
            if (obj.has("pending"))
            {
                JsonArray arr = obj.getAsJsonArray("pending");
                for (JsonElement el : arr)
                {
                    JsonObject f = el.getAsJsonObject();
                    String nick = f.get("nick").getAsString();
                    allFriends.add(new FriendEntry(nick, nick, false, FriendStatus.PENDING_IN));
                }
            }

            this.store.setFriends(allFriends);
        }
        catch (Exception e)
        {
            System.err.println("[IRCGUI] Failed to parse friend list: " + e.getMessage());
        }
    }

    /** Handle FRIEND_ADD_BY_UID reply. */
    private void handleFriendAddByUidReply(String jsonPayload)
    {
        try
        {
            JsonObject obj = new JsonParser().parse(jsonPayload).getAsJsonObject();
            if (obj.has("error"))
            {
                System.out.println("[IRCGUI] Add by UID failed: " + obj.get("message").getAsString());
                return;
            }
            String targetNick = obj.has("targetNick") ? obj.get("targetNick").getAsString() : "?";
            System.out.println("[IRCGUI] Friend request sent to " + targetNick + " via UID");
            // Refresh friend list
            if (this.chatService instanceof IRCChatServiceAdapter)
            {
                IRCServiceClient sc = ((IRCChatServiceAdapter) this.chatService).getServiceClient();
                if (sc != null) sc.listFriends();
            }
        }
        catch (Exception ignored) {}
    }

    /** Handle GET_MY_UID reply — store UID in own profile. */
    private void handleMyUidReply(String jsonPayload)
    {
        try
        {
            JsonObject obj = new JsonParser().parse(jsonPayload).getAsJsonObject();
            if (obj.has("uid"))
            {
                String uid = obj.get("uid").getAsString();
                UserProfile mp = this.store.getMyProfile();
                if (mp != null) mp.setUid(uid);
                this.myUid = uid;
            }
        }
        catch (Exception ignored) {}
    }

    private static String escapeJson(String s)
    {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    @Override
    public void onGuiClosed()
    {
        UiAnimationBus.clearPrefix(ANIM_PREFIX);
        this.chatService.removeListener(this.serviceListener);
        // Remove IRC service listener to prevent accumulation across reopens
        if (this.ircServiceListener != null && this.chatService instanceof IRCChatServiceAdapter)
        {
            IRCServiceClient sc = ((IRCChatServiceAdapter) this.chatService).getServiceClient();
            if (sc != null) sc.removeListener(this.ircServiceListener);
        }
        if (this.lastVg != 0L) this.messageArea.cleanup(this.lastVg);
        // Do NOT disconnect — shared adapter stays alive for fast reopen
    }
}
