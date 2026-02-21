package dwgx.ui.ext.menufx;

public final class MenuFxController
{
    private MenuFxController()
    {
    }

    public static void handleWheel(MenuFxState state, MenuFxLayout layout, int wheel, int mouseX, int mouseY)
    {
        if (state == null || layout == null || wheel == 0 || !state.isExpanded() || !layout.rowsClip().contains(mouseX, mouseY))
        {
            return;
        }

        int notches = Math.max(1, Math.abs(wheel) / 120);
        int deltaRows = wheel < 0 ? notches : -notches;
        scrollRows(state, layout, deltaRows);
    }

    public static ClickResult handleClick(MenuFxState state, MenuFxLayout layout, int mouseX, int mouseY, int mouseButton)
    {
        if (state == null || layout == null || mouseButton != 0)
        {
            return ClickResult.none();
        }

        if (layout.toggle().contains(mouseX, mouseY))
        {
            state.toggleExpanded();
            return ClickResult.consumed(-1, false);
        }

        if (!state.isExpanded())
        {
            return ClickResult.none();
        }

        clampScroll(state, layout);
        float scrollOffset = (float)state.getScrollRows() * layout.rowStep();

        if (layout.rowsClip().contains(mouseX, mouseY))
        {
            MenuFxLayout.Rect[] rows = layout.modeRows();

            for (int i = 0; i < rows.length; ++i)
            {
                if (rows[i].offset(0.0F, -scrollOffset).contains(mouseX, mouseY))
                {
                    return ClickResult.consumed(i, false);
                }
            }

            if (layout.rowGameOnly().offset(0.0F, -scrollOffset).contains(mouseX, mouseY))
            {
                return ClickResult.consumed(-1, true);
            }

            return ClickResult.consumed(-1, false);
        }

        if (layout.panel().contains(mouseX, mouseY))
        {
            return ClickResult.consumed(-1, false);
        }

        state.setExpanded(false);
        return ClickResult.consumed(-1, false);
    }

    public static void clampScroll(MenuFxState state, MenuFxLayout layout)
    {
        if (state == null)
        {
            return;
        }

        if (layout == null)
        {
            state.setScrollRows(0);
            state.setScrollVisual(0.0F);
            return;
        }

        int max = maxScroll(layout);
        state.setScrollRows(Math.max(0, Math.min(max, state.getScrollRows())));
    }

    public static int maxScroll(MenuFxLayout layout)
    {
        int totalRows = layout == null ? 0 : layout.backgroundOptions().length + 1;
        int visibleRows = layout == null ? 1 : layout.visibleRows();
        return Math.max(0, totalRows - visibleRows);
    }

    private static void scrollRows(MenuFxState state, MenuFxLayout layout, int deltaRows)
    {
        if (state == null || layout == null || deltaRows == 0)
        {
            return;
        }

        int max = maxScroll(layout);
        state.setScrollRows(Math.max(0, Math.min(max, state.getScrollRows() + deltaRows)));
    }

    public static final class ClickResult
    {
        private static final ClickResult NONE = new ClickResult(false, -1, false);

        private final boolean consumed;
        private final int modeIndex;
        private final boolean toggleGameOnly;

        private ClickResult(boolean consumed, int modeIndex, boolean toggleGameOnly)
        {
            this.consumed = consumed;
            this.modeIndex = modeIndex;
            this.toggleGameOnly = toggleGameOnly;
        }

        public boolean consumed()
        {
            return this.consumed;
        }

        public int modeIndex()
        {
            return this.modeIndex;
        }

        public boolean toggleGameOnly()
        {
            return this.toggleGameOnly;
        }

        private static ClickResult none()
        {
            return NONE;
        }

        private static ClickResult consumed(int modeIndex, boolean toggleGameOnly)
        {
            return new ClickResult(true, modeIndex, toggleGameOnly);
        }
    }
}
