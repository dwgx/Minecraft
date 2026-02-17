package org.lwjgl.opengl;

public class DisplayMode {
    private final int width;
    private final int height;
    private final int bpp;
    private final int freq;

    public DisplayMode(int width, int height) {
        this(width, height, 32, 60);
    }

    public DisplayMode(int width, int height, int bpp, int freq) {
        this.width = width;
        this.height = height;
        this.bpp = bpp;
        this.freq = freq;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBitsPerPixel() {
        return bpp;
    }

    public int getFrequency() {
        return freq;
    }
}
