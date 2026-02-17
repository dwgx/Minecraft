package org.lwjgl.opengl;

/**
 * Minimal stub to satisfy existing calls like new PixelFormat().withDepthBits(24).
 * Values are ignored because GLFW chooses a sensible default framebuffer.
 */
public class PixelFormat {
    public PixelFormat withDepthBits(int bits) {
        return this;
    }
}
