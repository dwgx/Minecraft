package org.lwjgl;

import java.awt.Desktop;
import java.net.URI;

public final class Sys {
    private Sys() {}

    public static String getVersion() {
        try {
            return "LWJGL " + Version.getVersion() + " (compat)";
        } catch (Throwable ignored) {
            return "LWJGL3-compat";
        }
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
