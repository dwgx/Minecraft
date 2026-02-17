package client.render;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.BufferUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static org.lwjgl.nanovg.NanoVG.nvgCreateFont;
import static org.lwjgl.nanovg.NanoVG.nvgCreateFontMem;

public final class NanoFontManager
{
    private final Map<String, Integer> fonts = new HashMap<String, Integer>();

    public int loadFromFile(long vg, String alias, String path)
    {
        if (alias == null || path == null || path.isEmpty())
        {
            return -1;
        }

        int id = nvgCreateFont(vg, alias, path);

        if (id >= 0)
        {
            this.fonts.put(alias, Integer.valueOf(id));
        }

        return id;
    }

    public int loadFromMemory(long vg, String alias, File file) throws IOException
    {
        if (alias == null || file == null || !file.isFile())
        {
            return -1;
        }

        ByteBuffer data = readFileToBuffer(file);
        int id = nvgCreateFontMem(vg, alias, data, 0);

        if (id >= 0)
        {
            this.fonts.put(alias, Integer.valueOf(id));
        }

        return id;
    }

    public int get(String alias)
    {
        Integer id = this.fonts.get(alias);
        return id == null ? -1 : id.intValue();
    }

    public Map<String, Integer> asMap()
    {
        return Collections.unmodifiableMap(this.fonts);
    }

    private static ByteBuffer readFileToBuffer(File file) throws IOException
    {
        FileInputStream stream = new FileInputStream(file);

        try
        {
            FileChannel channel = stream.getChannel();
            ByteBuffer buffer = BufferUtils.createByteBuffer((int)channel.size() + 1);

            while (channel.read(buffer) != -1)
            {
            }

            buffer.flip();
            return buffer;
        }
        finally
        {
            stream.close();
        }
    }
}
