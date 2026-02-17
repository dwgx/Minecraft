package client.ui;

public final class Theme
{
    private int accentArgb = 0xFF4CAF50;
    private int backgroundArgb = 0xCC1F1F1F;
    private int textArgb = 0xFFFFFFFF;

    public int getAccentArgb()
    {
        return this.accentArgb;
    }

    public void setAccentArgb(int accentArgb)
    {
        this.accentArgb = accentArgb;
    }

    public int getBackgroundArgb()
    {
        return this.backgroundArgb;
    }

    public void setBackgroundArgb(int backgroundArgb)
    {
        this.backgroundArgb = backgroundArgb;
    }

    public int getTextArgb()
    {
        return this.textArgb;
    }

    public void setTextArgb(int textArgb)
    {
        this.textArgb = textArgb;
    }
}
