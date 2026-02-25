package client.ui.component;

import client.ui.template.NanoScreenKit;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiAnimationBus;
import client.ui.template.UiControlAnimations;
import client.ui.template.UiMotion;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoUi;
import org.lwjgl.system.MemoryStack;

/**
 * Dropdown selector for enum values with expand/collapse animation.
 * Replaces duplicated EnumSetting rendering across screens.
 */
public class NanoDropdown extends NanoComponent
{
    public interface SelectionListener
    {
        void onSelected(int index, String value);
    }

    private String[] options = new String[0];
    private int selectedIndex;
    private boolean expanded;
    private float rowHeight = 22.0F;
    private SelectionListener listener;

    public NanoDropdown(String animKeyPrefix)
    {
        super(animKeyPrefix);
    }

    public void setOptions(String[] options, int selectedIndex)
    {
        this.options = options == null ? new String[0] : options;
        this.selectedIndex = Math.max(0, Math.min(selectedIndex, this.options.length - 1));
    }

    public int getSelectedIndex()
    {
        return this.selectedIndex;
    }

    public String getSelectedValue()
    {
        return this.selectedIndex >= 0 && this.selectedIndex < this.options.length
            ? this.options[this.selectedIndex] : "";
    }

    public void setRowHeight(float rowHeight)
    {
        this.rowHeight = rowHeight;
    }

    public void setListener(SelectionListener listener)
    {
        this.listener = listener;
    }

    public boolean isExpanded()
    {
        return this.expanded;
    }

    /**
     * Total height including expanded options.
     */
    public float getTotalHeight()
    {
        if (!this.expanded)
        {
            return this.height;
        }

        return this.height + this.options.length * this.rowHeight;
    }

    public void render(long vg, MemoryStack stack, NanoTheme theme,
                       UiAnimProfile animProfile, float scale, int mouseX, int mouseY)
    {
        if (this.shouldSkipRender())
        {
            return;
        }

        boolean hovered = isInHeader(mouseX, mouseY);
        float focus = UiControlAnimations.focus(this.animKey("focus"), hovered, animProfile);
        float expandVal = UiAnimationBus.animateControl(
            this.animKey("expand"), this.expanded ? 1.0F : 0.0F, animProfile);

        // Header
        float radius = Math.min(this.height * 0.4F, theme.controlRadius());
        int headerFill = NanoScreenKit.mixArgb(theme.controlArgb(), theme.controlActiveArgb(),
            UiMotion.clamp01(focus * 0.4F + expandVal * 0.3F));
        int headerBorder = NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 90);
        NanoUi.drawSurface(vg, stack, this.x, this.y, this.width, this.height, radius, headerFill, headerBorder);

        // Selected value text
        String display = this.getSelectedValue();
        NanoUi.drawCenterText(vg, stack, -1, this.x + this.width * 0.5F, this.y + this.height * 0.5F,
            NanoScreenKit.scaled(11.0F, scale), theme.textArgb(), display);

        // Expanded options
        if (this.expanded && this.options.length > 0)
        {
            float optY = this.y + this.height;

            for (int i = 0; i < this.options.length; ++i)
            {
                boolean optHovered = mouseX >= this.x && mouseX <= this.x + this.width
                    && mouseY >= optY && mouseY <= optY + this.rowHeight;
                float optFocus = UiControlAnimations.focus(
                    this.animKey("opt." + i), optHovered, animProfile);
                boolean isSelected = i == this.selectedIndex;

                int optFill = NanoScreenKit.mixArgb(theme.cardArgb(), theme.rowHoverArgb(),
                    UiMotion.clamp01(optFocus * 0.7F));
                if (isSelected)
                {
                    optFill = NanoScreenKit.mixArgb(optFill, theme.accentSoftArgb(), 0.4F);
                }

                NanoUi.drawSurface(vg, stack, this.x, optY, this.width, this.rowHeight,
                    i == this.options.length - 1 ? radius : 0.0F, optFill, headerBorder);
                NanoUi.drawCenterText(vg, stack, -1, this.x + this.width * 0.5F,
                    optY + this.rowHeight * 0.5F,
                    NanoScreenKit.scaled(10.0F, scale),
                    isSelected ? theme.accentArgb() : theme.textArgb(),
                    this.options[i]);

                optY += this.rowHeight;
            }
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button)
    {
        if (button != 0)
        {
            return false;
        }

        // Click on header toggles expand
        if (isInHeader(mouseX, mouseY))
        {
            this.expanded = !this.expanded;
            return true;
        }

        // Click on expanded option
        if (this.expanded)
        {
            float optY = this.y + this.height;

            for (int i = 0; i < this.options.length; ++i)
            {
                if (mouseX >= this.x && mouseX <= this.x + this.width
                    && mouseY >= optY && mouseY <= optY + this.rowHeight)
                {
                    this.selectedIndex = i;
                    this.expanded = false;

                    if (this.listener != null)
                    {
                        this.listener.onSelected(i, this.options[i]);
                    }

                    return true;
                }

                optY += this.rowHeight;
            }

            // Click outside options collapses
            this.expanded = false;
            return false;
        }

        return false;
    }

    private boolean isInHeader(int mouseX, int mouseY)
    {
        return mouseX >= this.x && mouseX <= this.x + this.width
            && mouseY >= this.y && mouseY <= this.y + this.height;
    }
}
