package client.ui.irc.component;

import client.chat.model.ChatUser;
import client.chat.store.ChatStore;
import client.ui.layout.UiRect;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

/**
 * Bottom status bar: colored connection indicator dot + online count + connection status.
 */
public final class IRCStatusBar
{
    private static final float DOT_RADIUS = 4.0F;
    private static final float DOT_MARGIN = 12.0F;

    public void render(long vg, MemoryStack stack, NanoTheme theme, UiRect r, ChatStore store)
    {
        if (r == null) return;
        NanoRenderUtils.fillRoundedRect(vg, r.x, r.y, r.w, r.h, theme.surfaceRadius(),
                NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.sidebarArgb(), 180)));

        int onlineCount = 0;
        for (ChatUser u : store.getAllUsers())
        {
            if (u.isOnline()) onlineCount++;
        }

        boolean connected = store.isConnected();
        String connStatus = store.getConnectionStatus();
        if (connStatus == null) connStatus = connected ? "Connected" : "Disconnected";

        // Connection indicator dot
        float dotX = r.x + DOT_MARGIN;
        float dotY = r.y + r.h * 0.5F;
        int dotColor = connected ? 0xFF43B581 : 0xFFF04747;
        if (connStatus.startsWith("Reconnecting") || connStatus.startsWith("Connecting"))
        {
            dotColor = 0xFFFAA61A; // amber for connecting/reconnecting
        }
        NanoRenderUtils.fillRoundedRect(vg, dotX - DOT_RADIUS, dotY - DOT_RADIUS,
                DOT_RADIUS * 2.0F, DOT_RADIUS * 2.0F, DOT_RADIUS,
                NanoRenderUtils.argb(stack, dotColor));

        // Status text
        String status = onlineCount + " online | " + connStatus;
        float textX = dotX + DOT_RADIUS + 8.0F;
        NanoRenderUtils.drawLabel(vg, stack, NanoFontBook.uiRegular(),
                textX, dotY, 11.0F, status,
                theme.textMutedArgb(), NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
    }
}
