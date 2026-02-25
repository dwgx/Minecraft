package dwgx.nano;

import client.render.NanoFontManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class NanoFontBook
{
    private static final NanoFontManager FONTS = new NanoFontManager();
    private static volatile boolean initialized;
    private static volatile long loadedForVg;
    private static int uiRegularId = -1;
    private static int uiBoldId = -1;
    private static int monoId = -1;
    private static int cyrillicId = -1;
    private static int cjkJpId = -1;
    private static int cjkCnId = -1;

    private NanoFontBook()
    {
    }

    public static void ensureLoaded(long vg)
    {
        if (vg == 0L)
        {
            return;
        }

        // Fast path: already loaded for this context — no synchronization needed
        if (initialized && loadedForVg == vg)
        {
            return;
        }

        ensureLoadedSlow(vg);
    }

    private static synchronized void ensureLoadedSlow(long vg)
    {
        // Double-check under lock
        if (initialized && loadedForVg == vg)
        {
            return;
        }

        initialized = false;
        loadedForVg = 0L;
        uiRegularId = -1;
        uiBoldId = -1;
        monoId = -1;
        cyrillicId = -1;
        cjkJpId = -1;
        cjkCnId = -1;

        File gameDir = new File(".").getAbsoluteFile();
        File parentDir = gameDir.getParentFile();
        String base = gameDir.getPath();
        String parent = parentDir == null ? base : parentDir.getPath();

        uiRegularId = loadFirst(vg, "dwgx-ui-regular", fontCandidates(base, parent, "SF-Regular.ttf", "SF-Regular.otf", "SF-Bold.ttf", "SF-Bold.otf"));
        uiBoldId = loadFirst(vg, "dwgx-ui-bold", fontCandidates(base, parent, "SF-Bold.ttf", "SF-Bold.otf", "SF-Regular.ttf", "SF-Regular.otf"));
        monoId = loadFirst(vg, "dwgx-ui-mono", fontCandidates(base, parent, "SF-Regular.ttf", "SF-Regular.otf"));
        cyrillicId = loadFirst(vg, "dwgx-ui-cyrillic", cyrillicCandidates(base, parent));
        cjkJpId = loadFirst(vg, "dwgx-ui-cjk-jp", cjkJpCandidates(base, parent));
        cjkCnId = loadFirst(vg, "dwgx-ui-cjk-cn", cjkCnCandidates(base, parent));

        if (uiBoldId < 0)
        {
            uiBoldId = uiRegularId;
        }

        if (monoId < 0)
        {
            monoId = uiRegularId;
        }

        // Prefer Cyrillic fallback first, then CN for Han glyphs, then JP for kana coverage.
        addFallbackChain(vg, uiRegularId, cyrillicId, cjkCnId, cjkJpId);
        addFallbackChain(vg, uiBoldId, cyrillicId, cjkCnId, cjkJpId);
        addFallbackChain(vg, monoId, cyrillicId, cjkCnId, cjkJpId);

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
                return id;
            }
        }

        return -1;
    }

    private static String[] fontCandidates(String base, String parent, String... fileNames)
    {
        return fontCandidatesFromRoots(base, parent, fileNames);
    }

    private static String[] cjkJpCandidates(String base, String parent)
    {
        String family = "UDDigiKyokashoN-B" + File.separator;
        return fontCandidatesFromRoots(base, parent, new String[] {
            family + "UDDigiKyokashoN-R.ttc",
            family + "UDDigiKyokashoN-B.ttc",
            "UDDigiKyokashoN-R.ttc",
            "UDDigiKyokashoN-B.ttc"
        });
    }

    private static String[] cyrillicCandidates(String base, String parent)
    {
        String family = "PT_Sans" + File.separator;
        return fontCandidatesFromRoots(base, parent, new String[] {
            family + "PTSans-Regular.ttf",
            family + "PTSans-Bold.ttf",
            "PTSans-Regular.ttf",
            "PTSans-Bold.ttf"
        });
    }

    private static String[] cjkCnCandidates(String base, String parent)
    {
        String family = "FangZhengKaiJianTi" + File.separator;
        return fontCandidatesFromRoots(base, parent, new String[] {
            family + "FangZhengKaiJianTi.ttf",
            family + "FangZhengKaiTiFanTi.ttf",
            "FangZhengKaiJianTi.ttf",
            "FangZhengKaiTiFanTi.ttf"
        });
    }

    private static String[] fontCandidatesFromRoots(String base, String parent, String... fileNames)
    {
        List<String> out = new ArrayList<String>();
        addFontCandidates(out, base, fileNames);

        if (parent != null && !parent.equals(base))
        {
            addFontCandidates(out, parent, fileNames);
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

    private static void addFallbackChain(long vg, int baseFontId, int... fallbackFontIds)
    {
        if (fallbackFontIds == null || fallbackFontIds.length == 0)
        {
            return;
        }

        for (int i = 0; i < fallbackFontIds.length; ++i)
        {
            addFallback(vg, baseFontId, fallbackFontIds[i]);
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
