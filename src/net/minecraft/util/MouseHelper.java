package net.minecraft.util;

import client.runtime.lwjgl.GlfwMouse;
import client.runtime.lwjgl.GlfwWindow;

public class MouseHelper
{
    /** Mouse delta X this frame */
    public int deltaX;

    /** Mouse delta Y this frame */
    public int deltaY;

    /**
     * Grabs the mouse cursor it doesn't move and isn't seen.
     */
    public void grabMouseCursor()
    {
        GlfwMouse.setGrabbed(true);
        this.deltaX = 0;
        this.deltaY = 0;
    }

    /**
     * Ungrabs the mouse cursor so it can be moved and set it to the center of the screen
     */
    public void ungrabMouseCursor()
    {
        GlfwMouse.setCursorPosition(GlfwWindow.getWidth() / 2, GlfwWindow.getHeight() / 2);
        GlfwMouse.setGrabbed(false);
    }

    public void mouseXYChange()
    {
        this.deltaX = GlfwMouse.getDX();
        this.deltaY = GlfwMouse.getDY();
    }
}
