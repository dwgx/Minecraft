package org.lwjgl.util.vector;

import java.io.Serializable;

public class Vector3f implements Serializable, ReadableVector3f, WritableVector3f {
    private static final long serialVersionUID = 1L;
    public float x, y, z;

    public Vector3f() {
        this(0f, 0f, 0f);
    }

    public Vector3f(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
    }

    public Vector3f(Vector3f src) {
        this(src.x, src.y, src.z);
    }

    public Vector3f(ReadableVector3f src) {
        this(src.getX(), src.getY(), src.getZ());
    }

    public Vector3f set(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
        return this;
    }

    public Vector3f set(ReadableVector3f src) {
        this.x = src.getX();
        this.y = src.getY();
        this.z = src.getZ();
        return this;
    }

    public static Vector3f add(Vector3f left, Vector3f right, Vector3f dest) {
        if (dest == null) dest = new Vector3f();
        dest.set(left.x + right.x, left.y + right.y, left.z + right.z);
        return dest;
    }

    public static Vector3f sub(Vector3f left, Vector3f right, Vector3f dest) {
        if (dest == null) dest = new Vector3f();
        dest.set(left.x - right.x, left.y - right.y, left.z - right.z);
        return dest;
    }

    public Vector3f scale(float scale) {
        this.x *= scale;
        this.y *= scale;
        this.z *= scale;
        return this;
    }

    public static Vector3f cross(Vector3f left, Vector3f right, Vector3f dest) {
        if (dest == null) dest = new Vector3f();
        dest.set(
                left.y * right.z - left.z * right.y,
                left.z * right.x - left.x * right.z,
                left.x * right.y - left.y * right.x
        );
        return dest;
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public Vector3f normalise(Vector3f dest) {
        float l = length();
        if (l != 0.0f) {
            if (dest == null) dest = this;
            dest.set(x / l, y / l, z / l);
        }
        return dest;
    }

    public static float dot(Vector3f left, Vector3f right) {
        return left.x * right.x + left.y * right.y + left.z * right.z;
    }

    @Override public float getX() { return x; }
    @Override public float getY() { return y; }
    @Override public float getZ() { return z; }
    @Override public void setX(float x) { this.x = x; }
    @Override public void setY(float y) { this.y = y; }
    @Override public void setZ(float z) { this.z = z; }
}
