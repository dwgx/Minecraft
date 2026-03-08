package dwgx.nano;

import client.render.NanoFontManager;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    private static int emojiId = -1;

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
        emojiId = -1;

        List<String> roots = resolveSearchRoots();

        uiRegularId = loadFirst(vg, "dwgx-ui-regular", fontCandidates(roots, "SF-Regular.ttf", "SF-Regular.otf", "SF-Bold.ttf", "SF-Bold.otf"));
        uiBoldId = loadFirst(vg, "dwgx-ui-bold", fontCandidates(roots, "SF-Bold.ttf", "SF-Bold.otf", "SF-Regular.ttf", "SF-Regular.otf"));
        monoId = loadFirst(vg, "dwgx-ui-mono", fontCandidates(roots, "SF-Regular.ttf", "SF-Regular.otf"));
        cyrillicId = loadFirst(vg, "dwgx-ui-cyrillic", cyrillicCandidates(roots));
        cjkJpId = loadFirst(vg, "dwgx-ui-cjk-jp", cjkJpCandidates(roots));
        cjkCnId = loadFirst(vg, "dwgx-ui-cjk-cn", cjkCnCandidates(roots));
        emojiId = loadFirst(vg, "dwgx-ui-emoji", emojiCandidates());

        // Hard fallback: if packaged fonts are unavailable, use Windows UI fonts.
        if (uiRegularId < 0) uiRegularId = loadFirst(vg, "dwgx-ui-regular-win", systemUiRegularCandidates());
        if (uiBoldId < 0) uiBoldId = loadFirst(vg, "dwgx-ui-bold-win", systemUiBoldCandidates());
        if (monoId < 0) monoId = loadFirst(vg, "dwgx-ui-mono-win", systemMonoCandidates());

        if (uiBoldId < 0)
        {
            uiBoldId = uiRegularId;
        }

        if (monoId < 0)
        {
            monoId = uiRegularId;
        }

        if (uiRegularId < 0)
        {
            System.err.println("[NanoFontBook] Failed to load any base UI font. Nano text rendering will be unavailable.");
        }

        // Prefer Cyrillic fallback first, then CN for Han glyphs, then JP for kana coverage, then emoji.
        addFallbackChain(vg, uiRegularId, cyrillicId, cjkCnId, cjkJpId, emojiId);
        addFallbackChain(vg, uiBoldId, cyrillicId, cjkCnId, cjkJpId, emojiId);
        addFallbackChain(vg, monoId, cyrillicId, cjkCnId, cjkJpId, emojiId);

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

    private static String[] fontCandidates(List<String> roots, String... fileNames)
    {
        return fontCandidatesFromRoots(roots, fileNames);
    }

    private static String[] cjkJpCandidates(List<String> roots)
    {
        String family = "UDDigiKyokashoN-B" + File.separator;
        return fontCandidatesFromRoots(roots, new String[] {
            family + "UDDigiKyokashoN-R.ttc",
            family + "UDDigiKyokashoN-B.ttc",
            "UDDigiKyokashoN-R.ttc",
            "UDDigiKyokashoN-B.ttc"
        });
    }

    private static String[] cyrillicCandidates(List<String> roots)
    {
        String family = "PT_Sans" + File.separator;
        return fontCandidatesFromRoots(roots, new String[] {
            family + "PTSans-Regular.ttf",
            family + "PTSans-Bold.ttf",
            "PTSans-Regular.ttf",
            "PTSans-Bold.ttf"
        });
    }

    private static String[] cjkCnCandidates(List<String> roots)
    {
        String family = "FangZhengKaiJianTi" + File.separator;
        return fontCandidatesFromRoots(roots, new String[] {
            family + "FangZhengKaiJianTi.ttf",
            family + "FangZhengKaiTiFanTi.ttf",
            "FangZhengKaiJianTi.ttf",
            "FangZhengKaiTiFanTi.ttf"
        });
    }

    private static String[] emojiCandidates()
    {
        // Windows Segoe UI Emoji — the standard color emoji font on Windows 10/11
        String winFonts = System.getenv("WINDIR");
        if (winFonts == null || winFonts.isEmpty()) winFonts = "C:\\Windows";
        return new String[] {
            winFonts + File.separator + "Fonts" + File.separator + "seguiemj.ttf",
            "C:\\Windows\\Fonts\\seguiemj.ttf"
        };
    }

    private static String[] systemUiRegularCandidates()
    {
        String winFonts = System.getenv("WINDIR");
        if (winFonts == null || winFonts.isEmpty()) winFonts = "C:\\Windows";
        String fonts = winFonts + File.separator + "Fonts" + File.separator;
        return new String[] {
            fonts + "segoeui.ttf",
            fonts + "arial.ttf",
            fonts + "tahoma.ttf",
            fonts + "msyh.ttc",
            "C:\\Windows\\Fonts\\segoeui.ttf",
            "C:\\Windows\\Fonts\\arial.ttf",
            "C:\\Windows\\Fonts\\tahoma.ttf",
            "C:\\Windows\\Fonts\\msyh.ttc"
        };
    }

    private static String[] systemUiBoldCandidates()
    {
        String winFonts = System.getenv("WINDIR");
        if (winFonts == null || winFonts.isEmpty()) winFonts = "C:\\Windows";
        String fonts = winFonts + File.separator + "Fonts" + File.separator;
        return new String[] {
            fonts + "segoeuib.ttf",
            fonts + "arialbd.ttf",
            fonts + "tahomabd.ttf",
            fonts + "msyhbd.ttc",
            "C:\\Windows\\Fonts\\segoeuib.ttf",
            "C:\\Windows\\Fonts\\arialbd.ttf",
            "C:\\Windows\\Fonts\\tahomabd.ttf",
            "C:\\Windows\\Fonts\\msyhbd.ttc"
        };
    }

    private static String[] systemMonoCandidates()
    {
        String winFonts = System.getenv("WINDIR");
        if (winFonts == null || winFonts.isEmpty()) winFonts = "C:\\Windows";
        String fonts = winFonts + File.separator + "Fonts" + File.separator;
        return new String[] {
            fonts + "consola.ttf",
            fonts + "cour.ttf",
            fonts + "lucon.ttf",
            "C:\\Windows\\Fonts\\consola.ttf",
            "C:\\Windows\\Fonts\\cour.ttf",
            "C:\\Windows\\Fonts\\lucon.ttf"
        };
    }

    private static String[] fontCandidatesFromRoots(List<String> roots, String... fileNames)
    {
        List<String> out = new ArrayList<String>();
        if (roots != null)
        {
            for (int i = 0; i < roots.size(); ++i)
            {
                addFontCandidates(out, roots.get(i), fileNames);
            }
        }

        return out.toArray(new String[out.size()]);
    }

    private static List<String> resolveSearchRoots()
    {
        LinkedHashSet<String> roots = new LinkedHashSet<String>();

        addRootLineage(roots, canonicalFile(new File(".")), 8);

        String classPath = System.getProperty("java.class.path");
        if (classPath != null && !classPath.isEmpty())
        {
            String[] entries = classPath.split(java.util.regex.Pattern.quote(File.pathSeparator));
            for (int i = 0; i < entries.length; ++i)
            {
                String entry = entries[i];
                if (entry == null || entry.trim().isEmpty()) continue;

                File cp = canonicalFile(new File(entry));
                File dir = cp.isDirectory() ? cp : cp.getParentFile();
                addRootLineage(roots, dir, 4);
            }
        }

        return new ArrayList<String>(roots);
    }

    private static void addRootLineage(Set<String> out, File start, int maxDepth)
    {
        if (out == null || start == null || maxDepth <= 0) return;

        File cursor = start;
        for (int i = 0; i < maxDepth && cursor != null; ++i)
        {
            out.add(cursor.getPath());
            cursor = cursor.getParentFile();
        }
    }

    private static File canonicalFile(File file)
    {
        if (file == null) return null;
        try
        {
            return file.getCanonicalFile();
        }
        catch (Exception ignored)
        {
            return file.getAbsoluteFile();
        }
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
