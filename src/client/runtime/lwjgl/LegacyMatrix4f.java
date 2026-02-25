package client.runtime.lwjgl;

import java.io.Serializable;

/**
 * Minimal 4x4 float matrix.
 * Drop-in replacement for the removed org.lwjgl.util.vector.Matrix4f shim.
 */
public class LegacyMatrix4f implements Serializable
{
    private static final long serialVersionUID = 1L;

    public float m00, m01, m02, m03;
    public float m10, m11, m12, m13;
    public float m20, m21, m22, m23;
    public float m30, m31, m32, m33;

    public LegacyMatrix4f()
    {
        setIdentity();
    }

    public static LegacyMatrix4f setIdentity(LegacyMatrix4f m)
    {
        if (m == null) m = new LegacyMatrix4f();
        m.m00 = 1; m.m01 = 0; m.m02 = 0; m.m03 = 0;
        m.m10 = 0; m.m11 = 1; m.m12 = 0; m.m13 = 0;
        m.m20 = 0; m.m21 = 0; m.m22 = 1; m.m23 = 0;
        m.m30 = 0; m.m31 = 0; m.m32 = 0; m.m33 = 1;
        return m;
    }

    public LegacyMatrix4f setIdentity()
    {
        setIdentity(this);
        return this;
    }

    public LegacyMatrix4f transpose()
    {
        float t;
        t = m01; m01 = m10; m10 = t;
        t = m02; m02 = m20; m20 = t;
        t = m03; m03 = m30; m30 = t;
        t = m12; m12 = m21; m21 = t;
        t = m13; m13 = m31; m31 = t;
        t = m23; m23 = m32; m32 = t;
        return this;
    }

    public LegacyMatrix4f invert()
    {
        float det =
            m00 * m11 * m22 * m33 + m00 * m12 * m23 * m31 + m00 * m13 * m21 * m32
            + m01 * m10 * m23 * m32 + m01 * m12 * m20 * m33 + m01 * m13 * m22 * m30
            + m02 * m10 * m21 * m33 + m02 * m11 * m23 * m30 + m02 * m13 * m20 * m31
            + m03 * m10 * m22 * m31 + m03 * m11 * m20 * m32 + m03 * m12 * m21 * m30
            - m00 * m11 * m23 * m32 - m00 * m12 * m21 * m33 - m00 * m13 * m22 * m31
            - m01 * m10 * m22 * m33 - m01 * m12 * m23 * m30 - m01 * m13 * m20 * m32
            - m02 * m10 * m23 * m31 - m02 * m11 * m20 * m33 - m02 * m13 * m21 * m30
            - m03 * m10 * m21 * m32 - m03 * m11 * m22 * m30 - m03 * m12 * m20 * m31;

        if (det == 0.0f) return this;
        float invDet = 1.0f / det;
        LegacyMatrix4f r = new LegacyMatrix4f();
        r.m00 = invDet * (m11 * m22 * m33 + m12 * m23 * m31 + m13 * m21 * m32 - m11 * m23 * m32 - m12 * m21 * m33 - m13 * m22 * m31);
        r.m01 = invDet * (m01 * m23 * m32 + m02 * m21 * m33 + m03 * m22 * m31 - m01 * m22 * m33 - m02 * m23 * m31 - m03 * m21 * m32);
        r.m02 = invDet * (m01 * m12 * m33 + m02 * m13 * m31 + m03 * m11 * m32 - m01 * m13 * m32 - m02 * m11 * m33 - m03 * m12 * m31);
        r.m03 = invDet * (m01 * m13 * m22 + m02 * m11 * m23 + m03 * m12 * m21 - m01 * m12 * m23 - m02 * m13 * m21 - m03 * m11 * m22);
        r.m10 = invDet * (m10 * m23 * m32 + m12 * m20 * m33 + m13 * m22 * m30 - m10 * m22 * m33 - m12 * m23 * m30 - m13 * m20 * m32);
        r.m11 = invDet * (m00 * m22 * m33 + m02 * m23 * m30 + m03 * m20 * m32 - m00 * m23 * m32 - m02 * m20 * m33 - m03 * m22 * m30);
        r.m12 = invDet * (m00 * m13 * m32 + m02 * m10 * m33 + m03 * m12 * m30 - m00 * m12 * m33 - m02 * m13 * m30 - m03 * m10 * m32);
        r.m13 = invDet * (m00 * m12 * m23 + m02 * m13 * m20 + m03 * m10 * m22 - m00 * m13 * m22 - m02 * m10 * m23 - m03 * m12 * m20);
        r.m20 = invDet * (m10 * m21 * m33 + m11 * m23 * m30 + m13 * m20 * m31 - m10 * m23 * m31 - m11 * m20 * m33 - m13 * m21 * m30);
        r.m21 = invDet * (m00 * m23 * m31 + m01 * m20 * m33 + m03 * m21 * m30 - m00 * m21 * m33 - m01 * m23 * m30 - m03 * m20 * m31);
        r.m22 = invDet * (m00 * m11 * m33 + m01 * m13 * m30 + m03 * m10 * m31 - m00 * m13 * m31 - m01 * m10 * m33 - m03 * m11 * m30);
        r.m23 = invDet * (m00 * m13 * m21 + m01 * m10 * m23 + m03 * m11 * m20 - m00 * m11 * m23 - m01 * m13 * m20 - m03 * m10 * m21);
        r.m30 = invDet * (m10 * m22 * m31 + m11 * m20 * m32 + m12 * m21 * m30 - m10 * m21 * m32 - m11 * m22 * m30 - m12 * m20 * m31);
        r.m31 = invDet * (m00 * m21 * m32 + m01 * m22 * m30 + m02 * m20 * m31 - m00 * m22 * m31 - m01 * m20 * m32 - m02 * m21 * m30);
        r.m32 = invDet * (m00 * m12 * m31 + m01 * m10 * m32 + m02 * m11 * m30 - m00 * m11 * m32 - m01 * m12 * m30 - m02 * m10 * m31);
        r.m33 = invDet * (m00 * m11 * m22 + m01 * m12 * m20 + m02 * m10 * m21 - m00 * m12 * m21 - m01 * m10 * m22 - m02 * m11 * m20);
        this.m00 = r.m00; this.m01 = r.m01; this.m02 = r.m02; this.m03 = r.m03;
        this.m10 = r.m10; this.m11 = r.m11; this.m12 = r.m12; this.m13 = r.m13;
        this.m20 = r.m20; this.m21 = r.m21; this.m22 = r.m22; this.m23 = r.m23;
        this.m30 = r.m30; this.m31 = r.m31; this.m32 = r.m32; this.m33 = r.m33;
        return this;
    }

    public static LegacyMatrix4f mul(LegacyMatrix4f left, LegacyMatrix4f right, LegacyMatrix4f dest)
    {
        if (dest == null) dest = new LegacyMatrix4f();
        float n00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02 + left.m30 * right.m03;
        float n01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02 + left.m31 * right.m03;
        float n02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02 + left.m32 * right.m03;
        float n03 = left.m03 * right.m00 + left.m13 * right.m01 + left.m23 * right.m02 + left.m33 * right.m03;
        float n10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12 + left.m30 * right.m13;
        float n11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12 + left.m31 * right.m13;
        float n12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12 + left.m32 * right.m13;
        float n13 = left.m03 * right.m10 + left.m13 * right.m11 + left.m23 * right.m12 + left.m33 * right.m13;
        float n20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22 + left.m30 * right.m23;
        float n21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22 + left.m31 * right.m23;
        float n22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22 + left.m32 * right.m23;
        float n23 = left.m03 * right.m20 + left.m13 * right.m21 + left.m23 * right.m22 + left.m33 * right.m23;
        float n30 = left.m00 * right.m30 + left.m10 * right.m31 + left.m20 * right.m32 + left.m30 * right.m33;
        float n31 = left.m01 * right.m30 + left.m11 * right.m31 + left.m21 * right.m32 + left.m31 * right.m33;
        float n32 = left.m02 * right.m30 + left.m12 * right.m31 + left.m22 * right.m32 + left.m32 * right.m33;
        float n33 = left.m03 * right.m30 + left.m13 * right.m31 + left.m23 * right.m32 + left.m33 * right.m33;
        dest.m00 = n00; dest.m01 = n01; dest.m02 = n02; dest.m03 = n03;
        dest.m10 = n10; dest.m11 = n11; dest.m12 = n12; dest.m13 = n13;
        dest.m20 = n20; dest.m21 = n21; dest.m22 = n22; dest.m23 = n23;
        dest.m30 = n30; dest.m31 = n31; dest.m32 = n32; dest.m33 = n33;
        return dest;
    }

    public static LegacyMatrix4f rotate(float angle, LegacyVec3f axis, LegacyMatrix4f src, LegacyMatrix4f dest)
    {
        if (dest == null) dest = new LegacyMatrix4f();
        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);
        float omc = 1.0f - c;
        float x = axis.x, y = axis.y, z = axis.z;
        LegacyMatrix4f rot = new LegacyMatrix4f();
        rot.m00 = c + x * x * omc;
        rot.m01 = x * y * omc + z * s;
        rot.m02 = x * z * omc - y * s;
        rot.m10 = y * x * omc - z * s;
        rot.m11 = c + y * y * omc;
        rot.m12 = y * z * omc + x * s;
        rot.m20 = z * x * omc + y * s;
        rot.m21 = z * y * omc - x * s;
        rot.m22 = c + z * z * omc;
        rot.m03 = 0; rot.m13 = 0; rot.m23 = 0;
        rot.m30 = 0; rot.m31 = 0; rot.m32 = 0; rot.m33 = 1;
        return mul(src, rot, dest);
    }

    public static void transform(LegacyMatrix4f left, LegacyVec4f right, LegacyVec4f dest)
    {
        float x = left.m00 * right.x + left.m10 * right.y + left.m20 * right.z + left.m30 * right.w;
        float y = left.m01 * right.x + left.m11 * right.y + left.m21 * right.z + left.m31 * right.w;
        float z = left.m02 * right.x + left.m12 * right.y + left.m22 * right.z + left.m32 * right.w;
        float w = left.m03 * right.x + left.m13 * right.y + left.m23 * right.z + left.m33 * right.w;
        dest.set(x, y, z, w);
    }
}
