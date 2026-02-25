package client.runtime.lwjgl;

public class GlfwInitException extends Exception
{
    public GlfwInitException(String message)
    {
        super(message);
    }

    public GlfwInitException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
