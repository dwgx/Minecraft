package client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class ConfigIO
{
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    private final ConfigBackup backup = new ConfigBackup();

    public JsonObject read(Path target) throws IOException
    {
        if (target == null || !Files.isRegularFile(target))
        {
            return new JsonObject();
        }

        BufferedReader reader = Files.newBufferedReader(target, UTF8);

        try
        {
            JsonElement element = (new JsonParser()).parse(reader);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        }
        catch (RuntimeException ignored)
        {
            return new JsonObject();
        }
        finally
        {
            reader.close();
        }
    }

    public void writeAtomic(Path target, JsonObject data) throws IOException
    {
        this.writeAtomicIfChanged(target, data);
    }

    public boolean writeAtomicIfChanged(Path target, JsonObject data) throws IOException
    {
        if (target == null)
        {
            return false;
        }

        JsonObject safeData = data == null ? new JsonObject() : data;
        String serialized = GSON.toJson(safeData);

        if (Files.isRegularFile(target))
        {
            String existing = new String(Files.readAllBytes(target), UTF8);

            if (serialized.equals(existing))
            {
                return false;
            }
        }

        Path parent = target.getParent();

        if (parent != null)
        {
            Files.createDirectories(parent);
        }

        this.backup.backupIfExists(target);

        Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
        BufferedWriter writer = Files.newBufferedWriter(tmp, UTF8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        try
        {
            writer.write(serialized);
            writer.flush();
        }
        finally
        {
            writer.close();
        }

        try
        {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException ignored)
        {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return true;
    }
}
