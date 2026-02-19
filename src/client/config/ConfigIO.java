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
        return this.readWithResult(target).getData();
    }

    public ReadResult readWithResult(Path target) throws IOException
    {
        if (target == null)
        {
            return new ReadResult(new JsonObject(), true, false, null);
        }

        if (!Files.isRegularFile(target))
        {
            return new ReadResult(new JsonObject(), true, false, null);
        }

        BufferedReader reader = Files.newBufferedReader(target, UTF8);

        try
        {
            JsonElement element = (new JsonParser()).parse(reader);

            if (element != null && element.isJsonObject())
            {
                return new ReadResult(element.getAsJsonObject(), true, true, null);
            }

            return new ReadResult(new JsonObject(), false, true, "Root element is not a JSON object");
        }
        catch (RuntimeException ex)
        {
            String detail = ex.getMessage();
            String message = detail == null || detail.isEmpty() ? ex.getClass().getSimpleName() : ex.getClass().getSimpleName() + ": " + detail;
            return new ReadResult(new JsonObject(), false, true, message);
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

    public static final class ReadResult
    {
        private final JsonObject data;
        private final boolean parsed;
        private final boolean filePresent;
        private final String error;

        private ReadResult(JsonObject data, boolean parsed, boolean filePresent, String error)
        {
            this.data = data == null ? new JsonObject() : data;
            this.parsed = parsed;
            this.filePresent = filePresent;
            this.error = error;
        }

        public JsonObject getData()
        {
            return this.data;
        }

        public boolean isParsed()
        {
            return this.parsed;
        }

        public boolean isFilePresent()
        {
            return this.filePresent;
        }

        public String getError()
        {
            return this.error;
        }
    }
}
