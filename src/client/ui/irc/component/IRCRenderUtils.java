package client.ui.irc.component;

import client.chat.model.UserStatus;

/**
 * Shared rendering utilities for IRC components.
 */
public final class IRCRenderUtils
{
    private IRCRenderUtils() {}

    public static int statusColor(UserStatus status)
    {
        if (status == null) return 0xFF808080;
        switch (status)
        {
            case ONLINE: return 0xFF43B581;
            case AWAY:   return 0xFFFAA61A;
            case DND:    return 0xFFF04747;
            default:     return 0xFF808080;
        }
    }

    public static String formatTime(long timestampMs)
    {
        long diff = System.currentTimeMillis() - timestampMs;
        if (diff < 60000L) return "now";
        if (diff < 3600000L) return (diff / 60000L) + "m";
        if (diff < 86400000L) return (diff / 3600000L) + "h";
        return (diff / 86400000L) + "d";
    }
}
