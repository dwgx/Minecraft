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
        // Edge resize is intentionally disabled.
        // Use explicit resize handle (bottom-right grip) only.
        return null;
    }
}
