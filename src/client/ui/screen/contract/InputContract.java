package client.ui.screen.contract;

/**
 * Unified input contract for screen coordinators.
 */
public interface InputContract
{
    void onMouseInput(int mouseX, int mouseY, int button, boolean pressed);

    void onKeyInput(char typedChar, int keyCode);
}

