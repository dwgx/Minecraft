package client.runtime.lwjgl;

import java.io.Serializable;

/**
 * Minimal 4-component float vector.
 * Drop-in replacement for the removed org.lwjgl.util.vector.Vector4f shim.
 */
public class LegacyVec4f implements Serializable
{
    private static final long serialVersionUID = 1L;
    public float x, y, z, w;

    public LegacyVec4f()
    {
    }

    public LegacyVec4f(float x, float y, float z, float w)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public LegacyVec4f(LegacyVec4f src)
    {
        this(src.x, src.y, src.z, src.w);
    }

    public LegacyVec4f set(float x, float y, float z, float w)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getW() { return w; }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setZ(float z) { this.z = z; }
    public void setW(float w) { this.w = w; }
}
