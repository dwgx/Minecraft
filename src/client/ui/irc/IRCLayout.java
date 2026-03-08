package client.ui.irc;

import client.core.ClientBootstrap;
import client.ui.layout.UiRect;
import client.ui.template.UiLayoutProfile;

/**
 * Computes all sub-region rectangles for the IRC overlay.
 * Reads dimensions from UiLayoutProfile so they can be adjusted in GuiEdit.
 */
public final class IRCLayout
{
    private static final float PAD = 8.0F;
    private static final float GAP = 4.0F;

    public UiRect window;
    public UiRect topBar;
    public UiRect serverList;
    public UiRect channelList;
    public UiRect messageArea;
    public UiRect inputBar;
    public UiRect statusBar;

    public void compute(float wx, float wy, float ww, float wh)
    {
        UiLayoutProfile lp = resolveLayout();
        float serverListW = lp.chatServerListWidth();
        float channelRatio = lp.chatChannelRatio();
        float topBarH = lp.chatTopBarHeight();
        float statusBarH = lp.chatStatusBarHeight();
        float inputBarH = lp.chatInputBarHeight();

        float ix = wx + PAD;
        float iy = wy + PAD;
        float iw = ww - PAD * 2.0F;
        float ih = wh - PAD * 2.0F;

        this.window = new UiRect(wx, wy, ww, wh);
        this.topBar = new UiRect(ix, iy, iw, topBarH);

        float bodyY = iy + topBarH + GAP;
        float bodyH = ih - topBarH - statusBarH - GAP * 2.0F;

        this.serverList = new UiRect(ix, bodyY, serverListW, bodyH);

        float afterServer = ix + serverListW + GAP;
        float remainW = iw - serverListW - GAP;
        float channelW = Math.max(120.0F, remainW * channelRatio);

        this.channelList = new UiRect(afterServer, bodyY, channelW, bodyH);

        float msgX = afterServer + channelW + GAP;
        float msgW = remainW - channelW - GAP;
        float msgH = bodyH - inputBarH - GAP;

        this.messageArea = new UiRect(msgX, bodyY, msgW, msgH);
        this.inputBar = new UiRect(msgX, bodyY + msgH + GAP, msgW, inputBarH);
        this.statusBar = new UiRect(ix, iy + ih - statusBarH, iw, statusBarH);
    }

    private static UiLayoutProfile resolveLayout()
    {
        ClientBootstrap boot = ClientBootstrap.instance();
        if (boot != null && boot.getConfigManager() != null)
        {
            return boot.getConfigManager().getUiLayout();
        }
        return new UiLayoutProfile();
    }
}
