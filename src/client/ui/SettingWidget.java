package client.ui;

import client.setting.Setting;

public interface SettingWidget
{
    Setting<?> getSetting();

    void render(int mouseX, int mouseY, float partialTicks);
}
