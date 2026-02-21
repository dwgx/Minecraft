package client.module.impl.misc;

import client.auth.SkinCapeOverrideService;
import client.core.ClientBootstrap;
import client.module.Category;
import client.module.Module;
import client.setting.BoolSetting;
import client.setting.SettingGroup;
import client.setting.StringSetting;
import net.minecraft.client.Minecraft;

/**
 * Client-side skin/cape override module.
 *
 * Enter an official Minecraft Java player name, then this module resolves
 * and applies that profile's skin and cape to local rendering.
 */
public final class SkinChangerModule extends Module
{
    private final StringSetting officialName;
    private final BoolSetting clearOnDisable;

    private long nextRequestAtMs;
    private String lastAppliedNotice = "";
    private String lastErrorNotice = "";

    public SkinChangerModule()
    {
        super("skin_changer", "SkinChanger", Category.MISC);
        SettingGroup group = this.addGroup("general", "General");
        this.officialName = this.addSetting(new StringSetting("official_name", "Official Name", "Official Minecraft Java player name to copy skin/cape from", "", 16));
        this.clearOnDisable = this.addSetting(new BoolSetting("clear_on_disable", "Clear On Disable", "Restore default local skin/cape when module is disabled", true));
        group.add(this.officialName);
        group.add(this.clearOnDisable);
    }

    public void onEnable()
    {
        this.nextRequestAtMs = 0L;
        this.lastAppliedNotice = "";
        this.lastErrorNotice = "";
        this.pushRequestIfNeeded(true);
    }

    public void onDisable()
    {
        if (this.clearOnDisable.isEnabled())
        {
            SkinCapeOverrideService.clear();
        }
    }

    public void onTick()
    {
        this.pushRequestIfNeeded(false);
        this.reportStatusIfNeeded();
    }

    private void pushRequestIfNeeded(boolean force)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.theWorld == null)
        {
            return;
        }

        String target = this.normalizeName(this.officialName.get());

        if (target.isEmpty())
        {
            if (force)
            {
                SkinCapeOverrideService.clear();
            }

            return;
        }

        String applied = SkinCapeOverrideService.getAppliedName();

        if (!force && target.equalsIgnoreCase(applied))
        {
            return;
        }

        if (!force && SkinCapeOverrideService.isLoading())
        {
            return;
        }

        long now = System.currentTimeMillis();

        if (!force && now < this.nextRequestAtMs)
        {
            return;
        }

        SkinCapeOverrideService.requestApplyByOfficialName(target);
        this.nextRequestAtMs = now + 4000L;
    }

    private void reportStatusIfNeeded()
    {
        String applied = SkinCapeOverrideService.getAppliedName();

        if (!applied.isEmpty() && !applied.equalsIgnoreCase(this.lastAppliedNotice))
        {
            this.lastAppliedNotice = applied;
            this.lastErrorNotice = "";
            ClientBootstrap.instance().notifyUser("\u00a7aSkin/Cape applied from official profile: " + applied);
        }

        String error = SkinCapeOverrideService.getLastError();

        if (!error.isEmpty() && !error.equals(this.lastErrorNotice))
        {
            this.lastErrorNotice = error;
            ClientBootstrap.instance().notifyUser("\u00a7cSkin/Cape apply failed: " + error);
        }
    }

    private String normalizeName(String raw)
    {
        if (raw == null)
        {
            return "";
        }

        String input = raw.trim();

        if (input.isEmpty())
        {
            return "";
        }

        StringBuilder out = new StringBuilder();

        for (int i = 0; i < input.length() && out.length() < 16; ++i)
        {
            char c = input.charAt(i);

            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')
            {
                out.append(c);
            }
        }

        return out.toString();
    }
}
