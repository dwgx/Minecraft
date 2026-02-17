package org.lwjgl.opengl;

import org.lwjgl.opengl.GL;

/**
 * Minimal stub bridging to LWJGL3 capabilities.
 */
public final class GLContext {
    private static ContextCapabilities caps;
    private static GLCapabilities glCaps;

    private GLContext() {}

    public static ContextCapabilities getCapabilities() {
        GLCapabilities currentCaps = GL.getCapabilities();
        if (currentCaps == null) {
            currentCaps = GL.createCapabilities();
        }

        if (caps == null || glCaps != currentCaps) {
            caps = new ContextCapabilities();
            glCaps = currentCaps;
        }

        return caps;
    }
}
