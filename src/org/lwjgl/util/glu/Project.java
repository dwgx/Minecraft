package org.lwjgl.util.glu;

import org.lwjgl.opengl.GL11;

public final class Project {
    private Project() {}

    public static void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
        float ymax = zNear * (float) Math.tan(Math.toRadians(fovy / 2f));
        float ymin = -ymax;
        float xmin = ymin * aspect;
        float xmax = ymax * aspect;
        GL11.glFrustum(xmin, xmax, ymin, ymax, zNear, zFar);
    }
}
