package client.runtime.lwjgl;

import org.lwjgl.opengl.GL11;

public final class GluMath
{
    private static final ThreadLocal<Scratch> SCRATCH = new ThreadLocal<Scratch>()
    {
        @Override
        protected Scratch initialValue()
        {
            return new Scratch();
        }
    };

    private GluMath() {}

    // ── gluErrorString ──

    public static String gluErrorString(int errorCode)
    {
        switch (errorCode)
        {
            case GL11.GL_NO_ERROR: return "No error";
            case GL11.GL_INVALID_ENUM: return "Invalid enum";
            case GL11.GL_INVALID_VALUE: return "Invalid value";
            case GL11.GL_INVALID_OPERATION: return "Invalid operation";
            case GL11.GL_STACK_OVERFLOW: return "Stack overflow";
            case GL11.GL_STACK_UNDERFLOW: return "Stack underflow";
            case GL11.GL_OUT_OF_MEMORY: return "Out of memory";
            default: return "OpenGL error " + errorCode;
        }
    }

    // ── gluUnProject (array overload) ──

    public static int gluUnProject(float winX, float winY, float winZ,
                                   float[] model, float[] proj, int[] view,
                                   float[] objCoords)
    {
        Scratch scratch = SCRATCH.get();

        multiply(proj, model, scratch.mul);
        if (!invert(scratch.mul, scratch.inv, scratch.invTemp)) return 0;

        scratch.in[0] = (winX - view[0]) / view[2] * 2.0f - 1.0f;
        scratch.in[1] = (winY - view[1]) / view[3] * 2.0f - 1.0f;
        scratch.in[2] = 2.0f * winZ - 1.0f;
        scratch.in[3] = 1.0f;

        multiplyVec(scratch.inv, scratch.in, scratch.out);
        if (scratch.out[3] == 0.0f) return 0;
        scratch.out[3] = 1.0f / scratch.out[3];
        objCoords[0] = scratch.out[0] * scratch.out[3];
        objCoords[1] = scratch.out[1] * scratch.out[3];
        objCoords[2] = scratch.out[2] * scratch.out[3];
        return 1;
    }

    // ── gluUnProject (buffer overload) ──

    public static int gluUnProject(float winX, float winY, float winZ,
                                   java.nio.FloatBuffer model, java.nio.FloatBuffer proj, java.nio.IntBuffer view,
                                   java.nio.FloatBuffer objCoords)
    {
        Scratch scratch = SCRATCH.get();
        model.get(scratch.model).rewind();
        proj.get(scratch.proj).rewind();
        scratch.view[0] = view.get(0);
        scratch.view[1] = view.get(1);
        scratch.view[2] = view.get(2);
        scratch.view[3] = view.get(3);
        int r = gluUnProject(winX, winY, winZ, scratch.model, scratch.proj, scratch.view, scratch.obj);
        objCoords.put(0, scratch.obj[0]);
        objCoords.put(1, scratch.obj[1]);
        objCoords.put(2, scratch.obj[2]);
        return r;
    }

    // ── gluPerspective ──

    public static void gluPerspective(float fovy, float aspect, float zNear, float zFar)
    {
        float ymax = zNear * (float) Math.tan(Math.toRadians(fovy / 2f));
        float ymin = -ymax;
        float xmin = ymin * aspect;
        float xmax = ymax * aspect;
        GL11.glFrustum(xmin, xmax, ymin, ymax, zNear, zFar);
    }

    // ── private helpers ──
    private static void multiply(float[] a, float[] b, float[] r)
    {
        for (int i = 0; i < 4; i++)
        {
            for (int j = 0; j < 4; j++)
            {
                r[i * 4 + j] = a[i * 4] * b[j] + a[i * 4 + 1] * b[4 + j] + a[i * 4 + 2] * b[8 + j] + a[i * 4 + 3] * b[12 + j];
            }
        }
    }

    private static void multiplyVec(float[] m, float[] v, float[] r)
    {
        for (int i = 0; i < 4; i++)
        {
            r[i] = m[i * 4] * v[0] + m[i * 4 + 1] * v[1] + m[i * 4 + 2] * v[2] + m[i * 4 + 3] * v[3];
        }
    }

    private static boolean invert(float[] m, float[] invOut, float[] inv)
    {
        inv[0] = m[5]  * m[10] * m[15] -
                 m[5]  * m[11] * m[14] -
                 m[9]  * m[6]  * m[15] +
                 m[9]  * m[7]  * m[14] +
                 m[13] * m[6]  * m[11] -
                 m[13] * m[7]  * m[10];

        inv[4] = -m[4]  * m[10] * m[15] +
                  m[4]  * m[11] * m[14] +
                  m[8]  * m[6]  * m[15] -
                  m[8]  * m[7]  * m[14] -
                  m[12] * m[6]  * m[11] +
                  m[12] * m[7]  * m[10];

        inv[8] = m[4]  * m[9] * m[15] -
                 m[4]  * m[11] * m[13] -
                 m[8]  * m[5] * m[15] +
                 m[8]  * m[7] * m[13] +
                 m[12] * m[5] * m[11] -
                 m[12] * m[7] * m[9];

        inv[12] = -m[4]  * m[9] * m[14] +
                   m[4]  * m[10] * m[13] +
                   m[8]  * m[5] * m[14] -
                   m[8]  * m[6] * m[13] -
                   m[12] * m[5] * m[10] +
                   m[12] * m[6] * m[9];

        inv[1] = -m[1]  * m[10] * m[15] +
                  m[1]  * m[11] * m[14] +
                  m[9]  * m[2] * m[15] -
                  m[9]  * m[3] * m[14] -
                  m[13] * m[2] * m[11] +
                  m[13] * m[3] * m[10];

        inv[5] = m[0]  * m[10] * m[15] -
                 m[0]  * m[11] * m[14] -
                 m[8]  * m[2] * m[15] +
                 m[8]  * m[3] * m[14] +
                 m[12] * m[2] * m[11] -
                 m[12] * m[3] * m[10];

        inv[9] = -m[0]  * m[9] * m[15] +
                  m[0]  * m[11] * m[13] +
                  m[8]  * m[1] * m[15] -
                  m[8]  * m[3] * m[13] -
                  m[12] * m[1] * m[11] +
                  m[12] * m[3] * m[9];
        inv[13] = m[0]  * m[9] * m[14] -
                  m[0]  * m[10] * m[13] -
                  m[8]  * m[1] * m[14] +
                  m[8]  * m[2] * m[13] +
                  m[12] * m[1] * m[10] -
                  m[12] * m[2] * m[9];

        inv[2] = m[1]  * m[6] * m[15] -
                 m[1]  * m[7] * m[14] -
                 m[5]  * m[2] * m[15] +
                 m[5]  * m[3] * m[14] +
                 m[13] * m[2] * m[7] -
                 m[13] * m[3] * m[6];

        inv[6] = -m[0]  * m[6] * m[15] +
                  m[0]  * m[7] * m[14] +
                  m[4]  * m[2] * m[15] -
                  m[4]  * m[3] * m[14] -
                  m[12] * m[2] * m[7] +
                  m[12] * m[3] * m[6];

        inv[10] = m[0]  * m[5] * m[15] -
                  m[0]  * m[7] * m[13] -
                  m[4]  * m[1] * m[15] +
                  m[4]  * m[3] * m[13] +
                  m[12] * m[1] * m[7] -
                  m[12] * m[3] * m[5];

        inv[14] = -m[0]  * m[5] * m[14] +
                   m[0]  * m[6] * m[13] +
                   m[4]  * m[1] * m[14] -
                   m[4]  * m[2] * m[13] -
                   m[12] * m[1] * m[6] +
                   m[12] * m[2] * m[5];

        inv[3] = -m[1] * m[6] * m[11] +
                  m[1] * m[7] * m[10] +
                  m[5] * m[2] * m[11] -
                  m[5] * m[3] * m[10] -
                  m[9] * m[2] * m[7] +
                  m[9] * m[3] * m[6];

        inv[7] = m[0] * m[6] * m[11] -
                 m[0] * m[7] * m[10] -
                 m[4] * m[2] * m[11] +
                 m[4] * m[3] * m[10] +
                 m[8] * m[2] * m[7] -
                 m[8] * m[3] * m[6];
        inv[11] = -m[0] * m[5] * m[11] +
                   m[0] * m[7] * m[9] +
                   m[4] * m[1] * m[11] -
                   m[4] * m[3] * m[9] -
                   m[8] * m[1] * m[7] +
                   m[8] * m[3] * m[5];

        inv[15] = m[0] * m[5] * m[10] -
                  m[0] * m[6] * m[9] -
                  m[4] * m[1] * m[10] +
                  m[4] * m[2] * m[9] +
                  m[8] * m[1] * m[6] -
                  m[8] * m[2] * m[5];

        float det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];
        if (det == 0) return false;

        det = 1.0f / det;
        for (int i = 0; i < 16; i++)
        {
            invOut[i] = inv[i] * det;
        }
        return true;
    }

    private static final class Scratch
    {
        final float[] mul = new float[16];
        final float[] inv = new float[16];
        final float[] invTemp = new float[16];
        final float[] in = new float[4];
        final float[] out = new float[4];
        final float[] model = new float[16];
        final float[] proj = new float[16];
        final int[] view = new int[4];
        final float[] obj = new float[3];
    }
}