package dwgx.nano;

import client.render.NanoFontManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class NanoFontBook
{
    private static final NanoFontManager FONTS = new NanoFontManager();
    private static boolean initialized;
    private static long loadedForVg;
    private static int uiRegularId = -1;
    private static int uiBoldId = -1;
    private static int monoId = -1;
    private static int cjkId = -1;

    private NanoFontBook()
    {
    }

    public static synchronized void ensureLoaded(long vg)
    {
        if (vg == 0L)
        {
            return;
        }

        if (initialized && loadedForVg == vg)
        {
            return;
        }

        initialized = false;
        loadedForVg = 0L;
        uiRegularId = -1;
        uiBoldId = -1;
        monoId = -1;
        cjkId = -1;

        File gameDir = new File(".").getAbsoluteFile();
        File parentDir = gameDir.getParentFile();
        String base = gameDir.getPath();
        String parent = parentDir == null ? base : parentDir.getPath();

        uiRegularId = loadFirst(vg, "dwgx-ui-regular", fontCandidates(base, parent, "SF-Regular.ttf"));
        uiBoldId = loadFirst(vg, "dwgx-ui-bold", fontCandidates(base, parent, "SF-Bold.ttf", "SF-Regular.ttf"));
        monoId = loadFirst(vg, "dwgx-ui-mono", fontCandidates(base, parent, "SF-Regular.ttf"));
        cjkId = loadFirst(vg, "dwgx-ui-cjk", cjkCandidates(base, parent));

        if (uiRegularId < 0)
        {
            System.out.println("[NanoFontBook] Failed to load San-Francisco TTF. cwd=" + base);
        }

        if (uiBoldId < 0)
        {
            uiBoldId = uiRegularId;
        }

        if (monoId < 0)
        {
            monoId = uiRegularId;
        }

        if (cjkId >= 0)
        {
            addFallback(vg, uiRegularId, cjkId);
            addFallback(vg, uiBoldId, cjkId);
            addFallback(vg, monoId, cjkId);
        }

        loadedForVg = vg;
        initialized = true;
    }

    private static int loadFirst(long vg, String alias, String[] candidates)
    {
        for (int i = 0; i < candidates.length; ++i)
        {
            File file = new File(candidates[i]);

            if (!file.isFile())
            {
                continue;
            }

            int id = FONTS.loadFromFile(vg, alias, file.getAbsolutePath());

            if (id >= 0)
            {
                System.out.println("[NanoFontBook] Loaded " + alias + " from " + file.getAbsolutePath());
                return id;
            }
        }

        return -1;
    }

    private static String[] fontCandidates(String base, String parent, String... fileNames)
    {
        List<String> out = new ArrayList<String>();
        addFontCandidates(out, base, fileNames);

        if (parent != null && !parent.equals(base))
        {
            addFontCandidates(out, parent, fileNames);
        }

        return out.toArray(new String[out.size()]);
    }

    private static String[] cjkCandidates(String base, String parent)
    {
        String family = "UDDigiKyokashoN-B" + File.separator;
        List<String> out = new ArrayList<String>();
        addFontCandidates(out, base, new String[] {
            family + "UDDigiKyokashoN-R.ttc",
            family + "UDDigiKyokashoN-B.ttc",
            "UDDigiKyokashoN-R.ttc",
            "UDDigiKyokashoN-B.ttc"
        });

        if (parent != null && !parent.equals(base))
        {
            addFontCandidates(out, parent, new String[] {
                family + "UDDigiKyokashoN-R.ttc",
                family + "UDDigiKyokashoN-B.ttc",
                "UDDigiKyokashoN-R.ttc",
                "UDDigiKyokashoN-B.ttc"
            });
        }

        return out.toArray(new String[out.size()]);
    }

    private static void addFontCandidates(List<String> out, String root, String[] fileNames)
    {
        if (root == null || root.isEmpty())
        {
            return;
        }

        String separator = File.separator;
        String[] dirs = new String[] {
            root + separator + "run" + separator + "production" + separator + "Minecraft" + separator + "resources" + separator + "fonts" + separator + "San-Francisco",
            root + separator + "production" + separator + "Minecraft" + separator + "resources" + separator + "fonts" + separator + "San-Francisco",
            root + separator + "run" + separator + "resources" + separator + "fonts" + separator + "San-Francisco",
            root + separator + "resources" + separator + "fonts" + separator + "San-Francisco",
            root + separator + "src" + separator + "resources" + separator + "fonts" + separator + "San-Francisco",
            root + separator + "run" + separator + "production" + separator + "Minecraft" + separator + "resources" + separator + "fonts",
            root + separator + "production" + separator + "Minecraft" + separator + "resources" + separator + "fonts",
            root + separator + "run" + separator + "resources" + separator + "fonts",
            root + separator + "resources" + separator + "fonts",
            root + separator + "src" + separator + "resources" + separator + "fonts"
        };

        for (int i = 0; i < dirs.length; ++i)
        {
            for (int j = 0; j < fileNames.length; ++j)
            {
                out.add(new File(dirs[i], fileNames[j]).getPath());
            }
        }
    }

    private static void addFallback(long vg, int baseFontId, int fallbackFontId)
    {
        if (vg == 0L || baseFontId < 0 || fallbackFontId < 0 || baseFontId == fallbackFontId)
        {
            return;
        }

        try
        {
            org.lwjgl.nanovg.NanoVG.nvgAddFallbackFontId(vg, baseFontId, fallbackFontId);
        }
        catch (Throwable ignored)
        {
        }
    }

    public static int uiRegular()
    {
        return uiRegularId;
    }

    public static int uiBold()
    {
        return uiBoldId;
    }

    public static int mono()
    {
        return monoId;
    }
}
