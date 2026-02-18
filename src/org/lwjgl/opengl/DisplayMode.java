package org.lwjgl.opengl;

import java.util.Objects;

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

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof DisplayMode)) {
            return false;
        }

        DisplayMode other = (DisplayMode)obj;
        return this.width == other.width
            && this.height == other.height
            && this.bpp == other.bpp
            && this.freq == other.freq;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.width), Integer.valueOf(this.height), Integer.valueOf(this.bpp), Integer.valueOf(this.freq));
    }

    public String toString() {
        return this.width + "x" + this.height + "@" + this.freq + "Hz/" + this.bpp + "bpp";
    }
}
