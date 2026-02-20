package client.ui.state;

import client.ui.template.UiWindowState;

/**
 * Shared edge hit-testing for resizable Nano windows.
 */
public final class WindowResizeResolver
{
    private WindowResizeResolver()
    {
    }

    public static UiWindowState.ResizeMode resolve(float x, float y, float width, float height, int mouseX, int mouseY, float edge)
    {
        float e = Math.max(3.0F, edge);
        float x2 = x + Math.max(0.0F, width);
        float y2 = y + Math.max(0.0F, height);
        boolean nearLeft = Math.abs((float)mouseX - x) <= e;
        boolean nearRight = Math.abs((float)mouseX - x2) <= e;
        boolean nearTop = Math.abs((float)mouseY - y) <= e;
        boolean nearBottom = Math.abs((float)mouseY - y2) <= e;
        boolean insideX = (float)mouseX >= x - e && (float)mouseX <= x2 + e;
        boolean insideY = (float)mouseY >= y - e && (float)mouseY <= y2 + e;

        if (!insideX || !insideY)
        {
            return null;
        }

        if (nearLeft && nearTop)
        {
            return UiWindowState.ResizeMode.TOP_LEFT;
        }

        if (nearRight && nearTop)
        {
            return UiWindowState.ResizeMode.TOP_RIGHT;
        }

        if (nearLeft && nearBottom)
        {
            return UiWindowState.ResizeMode.BOTTOM_LEFT;
        }

        if (nearRight && nearBottom)
        {
            return UiWindowState.ResizeMode.BOTTOM_RIGHT;
        }

        if (nearLeft)
        {
            return UiWindowState.ResizeMode.LEFT;
        }

        if (nearRight)
        {
            return UiWindowState.ResizeMode.RIGHT;
        }

        if (nearTop)
        {
            return UiWindowState.ResizeMode.TOP;
        }

        return nearBottom ? UiWindowState.ResizeMode.BOTTOM : null;
    }
}

