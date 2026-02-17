package org.lwjgl.util.vector;

import java.io.Serializable;

public class Vector4f implements Serializable, ReadableVector4f, WritableVector4f {
    private static final long serialVersionUID = 1L;
    public float x, y, z, w;

    public Vector4f() {
        this(0f,0f,0f,0f);
    }
    public Vector4f(float x, float y, float z, float w) {
        this.x = x; this.y = y; this.z = z; this.w = w;
    }
    public Vector4f(Vector4f src) {
        this(src.x, src.y, src.z, src.w);
    }

    public Vector4f set(float x, float y, float z, float w) {
        this.x = x; this.y = y; this.z = z; this.w = w;
        return this;
    }

    public Vector4f set(ReadableVector4f src) {
        return set(src.getX(), src.getY(), src.getZ(), src.getW());
    }

    @Override public float getX() { return x; }
    @Override public float getY() { return y; }
    @Override public float getZ() { return z; }
    @Override public float getW() { return w; }
    @Override public void setX(float x) { this.x = x; }
    @Override public void setY(float y) { this.y = y; }
    @Override public void setZ(float z) { this.z = z; }
    @Override public void setW(float w) { this.w = w; }
}
