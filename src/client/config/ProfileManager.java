package client.config;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class ProfileManager
{
    private final Path profilesRoot;
    private String activeProfile = "default";

    public ProfileManager(Path configRoot)
    {
        this.profilesRoot = configRoot.resolve("profiles");
    }

    public Path getProfilesRoot()
    {
        return this.profilesRoot;
    }

    public String getActiveProfile()
    {
        return this.activeProfile;
    }

    public void setActiveProfile(String activeProfile)
    {
        if (activeProfile != null && !activeProfile.trim().isEmpty())
        {
            this.activeProfile = activeProfile.trim();
        }
    }

    public Path getActiveProfileRoot()
    {
        return this.profilesRoot.resolve(this.activeProfile);
    }

    public Path resolveInActiveProfile(String relative)
    {
        return this.getActiveProfileRoot().resolve(Paths.get(relative));
    }
}
