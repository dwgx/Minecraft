package client.i18n;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Runtime I18N manager for client-side text resources.
 */
public final class I18nManager
{
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private static final String DEFAULT_LOCALE = "en_us";
    private static final String[] CLASSPATH_DEFAULTS = new String[] {"en_us", "zh_cn", "ja_jp"};
    private static final String[] LANG_DIR_RELATIVE = new String[] {
        "run/production/Minecraft/resources/lang",
        "production/Minecraft/resources/lang",
        "run/resources/lang",
        "resources/lang",
        "src/resources/lang"
    };

    private final Map<String, Map<String, String>> bundles = new LinkedHashMap<String, Map<String, String>>();
    private String currentLocale = DEFAULT_LOCALE;

    public synchronized void reload()
    {
        Map<String, Map<String, String>> loaded = new LinkedHashMap<String, Map<String, String>>();
        this.loadClasspathDefaults(loaded);
        this.loadFileSystemBundles(loaded);

        if (!loaded.containsKey(DEFAULT_LOCALE))
        {
            loaded.put(DEFAULT_LOCALE, new LinkedHashMap<String, String>());
        }

        this.bundles.clear();
        this.bundles.putAll(loaded);
        this.currentLocale = this.resolveLocale(this.currentLocale);
    }

    public synchronized String getCurrentLocale()
    {
        return this.currentLocale;
    }

    public synchronized String getDefaultLocale()
    {
        return DEFAULT_LOCALE;
    }

    public synchronized boolean setCurrentLocale(String locale)
    {
        String resolved = this.resolveLocale(locale);

        if (resolved.equals(this.currentLocale))
        {
            return false;
        }

        this.currentLocale = resolved;
        return true;
    }

    public synchronized Set<String> getAvailableLocales()
    {
        return new LinkedHashSet<String>(this.bundles.keySet());
    }

    public String translate(String key, Object... args)
    {
        return this.translateOrDefault(key, key, args);
    }

    public String translateOrDefault(String key, String fallback, Object... args)
    {
        String template;

        synchronized (this)
        {
            template = this.lookup(this.currentLocale, key);
        }

        if (template == null || template.isEmpty())
        {
            template = fallback == null ? key : fallback;
        }

        return this.format(template, args);
    }

    public String translateForLocale(String locale, String key, String fallback, Object... args)
    {
        String template;

        synchronized (this)
        {
            template = this.lookup(this.resolveLocale(locale), key);
        }

        if (template == null || template.isEmpty())
        {
            template = fallback == null ? key : fallback;
        }

        return this.format(template, args);
    }

    public String getLocaleDisplayName(String locale)
    {
        String normalized = normalizeLocale(locale);
        return this.translateForLocale(normalized, "i18n.language_name", normalized);
    }

    private void loadClasspathDefaults(Map<String, Map<String, String>> target)
    {
        ClassLoader loader = I18nManager.class.getClassLoader();

        for (int i = 0; i < CLASSPATH_DEFAULTS.length; ++i)
        {
            String locale = normalizeLocale(CLASSPATH_DEFAULTS[i]);
            InputStream in = loader.getResourceAsStream("lang/" + locale + ".json");

            if (in == null)
            {
                continue;
            }

            try
            {
                this.mergeBundle(target, locale, this.readBundle(in));
            }
            catch (IOException ignored)
            {
            }
            finally
            {
                try
                {
                    in.close();
                }
                catch (IOException ignored)
                {
                }
            }
        }
    }

    private void loadFileSystemBundles(Map<String, Map<String, String>> target)
    {
        List<Path> dirs = this.detectLangDirs();

        for (int i = 0; i < dirs.size(); ++i)
        {
            Path dir = dirs.get(i);

            if (!Files.isDirectory(dir))
            {
                continue;
            }

            DirectoryStream<Path> stream = null;

            try
            {
                stream = Files.newDirectoryStream(dir, "*.json");

                for (Path file : stream)
                {
                    String name = file.getFileName() == null ? "" : file.getFileName().toString();

                    if (!name.endsWith(".json"))
                    {
                        continue;
                    }

                    String locale = normalizeLocale(name.substring(0, name.length() - 5));
                    this.mergeBundle(target, locale, this.readBundle(file));
                }
            }
            catch (IOException ignored)
            {
            }
            finally
            {
                if (stream != null)
                {
                    try
                    {
                        stream.close();
                    }
                    catch (IOException ignored)
                    {
                    }
                }
            }
        }
    }

    private List<Path> detectLangDirs()
    {
        List<Path> out = new ArrayList<Path>();
        Set<String> seen = new LinkedHashSet<String>();
        File gameDir = new File(".").getAbsoluteFile();
        File parentDir = gameDir.getParentFile();
        this.addLangCandidates(out, seen, gameDir);

        if (parentDir != null && !parentDir.equals(gameDir))
        {
            this.addLangCandidates(out, seen, parentDir);
        }

        return out;
    }

    private void addLangCandidates(List<Path> out, Set<String> seen, File root)
    {
        if (root == null)
        {
            return;
        }

        for (int i = 0; i < LANG_DIR_RELATIVE.length; ++i)
        {
            Path path = new File(root, LANG_DIR_RELATIVE[i].replace('/', File.separatorChar)).toPath().toAbsolutePath().normalize();
            String key = path.toString();

            if (seen.add(key))
            {
                out.add(path);
            }
        }
    }

    private Map<String, String> readBundle(Path file) throws IOException
    {
        BufferedReader reader = Files.newBufferedReader(file, UTF8);

        try
        {
            return this.readBundle(reader);
        }
        finally
        {
            reader.close();
        }
    }

    private Map<String, String> readBundle(InputStream in) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, UTF8));
        return this.readBundle(reader);
    }

    private Map<String, String> readBundle(BufferedReader reader) throws IOException
    {
        Map<String, String> out = new LinkedHashMap<String, String>();
        JsonElement parsed;

        try
        {
            parsed = new JsonParser().parse(reader);
        }
        catch (RuntimeException ignored)
        {
            return out;
        }

        if (parsed == null || !parsed.isJsonObject())
        {
            return out;
        }

        this.flattenJson("", parsed.getAsJsonObject(), out);
        return out;
    }

    private void flattenJson(String prefix, JsonObject object, Map<String, String> out)
    {
        if (object == null)
        {
            return;
        }

        for (Map.Entry<String, JsonElement> entry : object.entrySet())
        {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            JsonElement value = entry.getValue();

            if (value == null || value.isJsonNull())
            {
                continue;
            }

            if (value.isJsonObject())
            {
                this.flattenJson(key, value.getAsJsonObject(), out);
            }
            else if (value.isJsonPrimitive())
            {
                out.put(key, value.getAsString());
            }
        }
    }

    private void mergeBundle(Map<String, Map<String, String>> target, String locale, Map<String, String> incoming)
    {
        if (incoming == null || incoming.isEmpty())
        {
            return;
        }

        Map<String, String> bundle = target.get(locale);

        if (bundle == null)
        {
            bundle = new LinkedHashMap<String, String>();
            target.put(locale, bundle);
        }

        bundle.putAll(incoming);
    }

    private synchronized String resolveLocale(String locale)
    {
        if (this.bundles.isEmpty())
        {
            return DEFAULT_LOCALE;
        }

        String normalized = normalizeLocale(locale);

        if (this.bundles.containsKey(normalized))
        {
            return normalized;
        }

        int split = normalized.indexOf('_');

        if (split > 0)
        {
            String languageOnly = normalized.substring(0, split);

            if (this.bundles.containsKey(languageOnly))
            {
                return languageOnly;
            }
        }

        if (this.bundles.containsKey(DEFAULT_LOCALE))
        {
            return DEFAULT_LOCALE;
        }

        return this.bundles.keySet().iterator().next();
    }

    private synchronized String lookup(String locale, String key)
    {
        if (key == null || key.isEmpty())
        {
            return null;
        }

        Map<String, String> bundle = this.bundles.get(locale);

        if (bundle != null && bundle.containsKey(key))
        {
            return bundle.get(key);
        }

        int split = locale == null ? -1 : locale.indexOf('_');

        if (split > 0)
        {
            Map<String, String> languageBundle = this.bundles.get(locale.substring(0, split));

            if (languageBundle != null && languageBundle.containsKey(key))
            {
                return languageBundle.get(key);
            }
        }

        if (!DEFAULT_LOCALE.equals(locale))
        {
            Map<String, String> fallback = this.bundles.get(DEFAULT_LOCALE);

            if (fallback != null && fallback.containsKey(key))
            {
                return fallback.get(key);
            }
        }

        return null;
    }

    private String format(String template, Object... args)
    {
        if (template == null)
        {
            return "";
        }

        if (args == null || args.length == 0)
        {
            return template;
        }

        try
        {
            return (new MessageFormat(template, Locale.ROOT)).format(args);
        }
        catch (IllegalArgumentException ignored)
        {
            return template;
        }
    }

    private static String normalizeLocale(String locale)
    {
        if (locale == null)
        {
            return DEFAULT_LOCALE;
        }

        String value = locale.trim().toLowerCase(Locale.ROOT).replace('-', '_');

        if (value.isEmpty())
        {
            return DEFAULT_LOCALE;
        }

        StringBuilder safe = new StringBuilder();

        for (int i = 0; i < value.length(); ++i)
        {
            char c = value.charAt(i);

            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')
            {
                safe.append(c);
            }
        }

        return safe.length() == 0 ? DEFAULT_LOCALE : safe.toString();
    }
}
