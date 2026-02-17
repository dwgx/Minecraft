package client.render;

import static org.lwjgl.nanovg.NanoVGGL3.NVG_ANTIALIAS;
import static org.lwjgl.nanovg.NanoVGGL3.NVG_DEBUG;
import static org.lwjgl.nanovg.NanoVGGL3.NVG_STENCIL_STROKES;
import static org.lwjgl.nanovg.NanoVGGL3.nvgCreate;
import static org.lwjgl.nanovg.NanoVGGL3.nvgDelete;

public final class NanoRuntime
{
    private NanoRuntime()
    {
    }

    public static long createContext(boolean antialias, boolean stencilStrokes, boolean debug)
    {
        int flags = 0;

        if (antialias)
        {
            flags |= NVG_ANTIALIAS;
        }

        if (stencilStrokes)
        {
            flags |= NVG_STENCIL_STROKES;
        }

        if (debug)
        {
            flags |= NVG_DEBUG;
        }

        return nvgCreate(flags);
    }

    public static void destroyContext(long vg)
    {
        if (vg != 0L)
        {
            nvgDelete(vg);
        }
    }
}
