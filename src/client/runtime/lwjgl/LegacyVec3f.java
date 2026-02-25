package client.runtime.lwjgl;

import java.io.Serializable;

/**
 * Minimal 3-component float vector.
 * Drop-in replacement for the removed org.lwjgl.util.vector.Vector3f shim.
 */
public class LegacyVec3f implements Serializable
{
    private static final long serialVersionUID = 1L;
    public float x, y, z;

    public LegacyVec3f()
    {
    }

    public LegacyVec3f(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public LegacyVec3f(LegacyVec3f src)
    {
        this(src.x, src.y, src.z);
    }

    public LegacyVec3f set(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public static LegacyVec3f add(LegacyVec3f left, LegacyVec3f right, LegacyVec3f dest)
    {
        if (dest == null) dest = new LegacyVec3f();
        dest.set(left.x + right.x, left.y + right.y, left.z + right.z);
        return dest;
    }

    public static LegacyVec3f sub(LegacyVec3f left, LegacyVec3f right, LegacyVec3f dest)
    {
        if (dest == null) dest = new LegacyVec3f();
        dest.set(left.x - right.x, left.y - right.y, left.z - right.z);
        return dest;
    }

    public LegacyVec3f scale(float scale)
    {
        this.x *= scale;
        this.y *= scale;
        this.z *= scale;
        return this;
    }

    public static LegacyVec3f cross(LegacyVec3f left, LegacyVec3f right, LegacyVec3f dest)
    {
        if (dest == null) dest = new LegacyVec3f();
        dest.set(
            left.y * right.z - left.z * right.y,
            left.z * right.x - left.x * right.z,
            left.x * right.y - left.y * right.x
        );
        return dest;
    }

    public float length()
    {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public LegacyVec3f normalise(LegacyVec3f dest)
    {
        float l = length();

        if (l != 0.0f)
        {
            if (dest == null) dest = this;
            dest.set(x / l, y / l, z / l);
        }

        return dest;
    }

    public static float dot(LegacyVec3f left, LegacyVec3f right)
    {
        return left.x * right.x + left.y * right.y + left.z * right.z;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setZ(float z) { this.z = z; }
}
