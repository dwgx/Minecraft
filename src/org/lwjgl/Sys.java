package org.lwjgl;

import java.awt.Desktop;
import java.net.URI;

public final class Sys {
    private Sys() {}

    public static String getVersion() {
        return "LWJGL3-stub";
    }

    public static long getTime() {
        return System.nanoTime() / 1000000L;
    }

    public static long getTimerResolution() {
        return 1000L;
    }

    public static void openURL(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {
        }
    }
}
