package client.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigBackup
{
    public void backupIfExists(Path target) throws IOException
    {
        if (target != null && Files.isRegularFile(target))
        {
            Path backup = target.resolveSibling(target.getFileName().toString() + ".bak");
            Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
