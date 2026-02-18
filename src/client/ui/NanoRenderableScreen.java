package client.ui;

import client.render.RenderContext2D;

/**
 * Marker for GUI screens that render via the Nano 2D pipeline.
 */
public interface NanoRenderableScreen
{
    void renderNano(RenderContext2D context);
}
