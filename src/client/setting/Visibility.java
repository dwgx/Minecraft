package client.setting;

public interface Visibility
{
    Visibility ALWAYS = new Visibility()
    {
        public boolean isVisible()
        {
            return true;
        }
    };

    boolean isVisible();
}
