package net.minecraft.client.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.properties.PropertyMap.Serializer;
import java.io.File;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.Map;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import client.runtime.lwjgl.DisplayDefaults;

public class Main
{
    public static void main(String[] p_main_0_)
    {
        System.setProperty("java.net.preferIPv4Stack", "true");
        OptionParser optionparser = new OptionParser();
        optionparser.allowsUnrecognizedOptions();
        optionparser.accepts("demo");
        optionparser.accepts("fullscreen");
        optionparser.accepts("checkGlErrors");
        optionparser.accepts("aggressiveOptimize");
        OptionSpec<String> optionspec = optionparser.accepts("server").withRequiredArg();
        OptionSpec<Integer> optionspec1 = optionparser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(25565);
        OptionSpec<File> optionspec2 = optionparser.accepts("gameDir").withRequiredArg().ofType(File.class).defaultsTo(new File("."));
        OptionSpec<File> optionspec3 = optionparser.accepts("assetsDir").withRequiredArg().<File>ofType(File.class);
        OptionSpec<File> optionspec4 = optionparser.accepts("resourcePackDir").withRequiredArg().<File>ofType(File.class);
        OptionSpec<String> optionspec5 = optionparser.accepts("proxyHost").withRequiredArg();
        OptionSpec<Integer> optionspec6 = optionparser.accepts("proxyPort").withRequiredArg().ofType(Integer.class).defaultsTo(8080);
        OptionSpec<String> optionspec7 = optionparser.accepts("proxyUser").withRequiredArg();
        OptionSpec<String> optionspec8 = optionparser.accepts("proxyPass").withRequiredArg();
        OptionSpec<String> optionspec9 = optionparser.accepts("username").withRequiredArg().defaultsTo("Player" + Minecraft.getSystemTime() % 1000L);
        OptionSpec<String> optionspec10 = optionparser.accepts("uuid").withRequiredArg();
        OptionSpec<String> optionspec11 = optionparser.accepts("accessToken").withRequiredArg().required();
        OptionSpec<String> optionspec12 = optionparser.accepts("version").withRequiredArg().required();
        OptionSpec<Integer> optionspec13 = optionparser.accepts("width").withRequiredArg().ofType(Integer.class).defaultsTo(DisplayDefaults.WIDTH);
        OptionSpec<Integer> optionspec14 = optionparser.accepts("height").withRequiredArg().ofType(Integer.class).defaultsTo(DisplayDefaults.HEIGHT);
        OptionSpec<String> optionspec15 = optionparser.accepts("userProperties").withRequiredArg().defaultsTo("{}");
        OptionSpec<String> optionspec16 = optionparser.accepts("profileProperties").withRequiredArg().defaultsTo("{}");
        OptionSpec<String> optionspec17 = optionparser.accepts("assetIndex").withRequiredArg();
        OptionSpec<String> optionspec18 = optionparser.accepts("userType").withRequiredArg().defaultsTo("legacy");
        OptionSpec<Integer> optionspec20 = optionparser.accepts("chunkWorkers").withRequiredArg().ofType(Integer.class);
        OptionSpec<String> optionspec19 = optionparser.nonOptions();
        OptionSet optionset = optionparser.parse(p_main_0_);
        List<String> list = optionset.valuesOf(optionspec19);

        if (!list.isEmpty())
        {
            System.out.println("Completely ignored arguments: " + list);
        }

        String s = optionset.valueOf(optionspec5);
        Proxy proxy = Proxy.NO_PROXY;

        if (s != null)
        {
            try
            {
                proxy = new Proxy(Type.SOCKS, new InetSocketAddress(s, optionset.valueOf(optionspec6).intValue()));
            }
            catch (Exception var46)
            {
                ;
            }
        }

        final String s1 = optionset.valueOf(optionspec7);
        final String s2 = optionset.valueOf(optionspec8);

        if (!proxy.equals(Proxy.NO_PROXY) && hasValue(s1) && hasValue(s2))
        {
            Authenticator.setDefault(new Authenticator()
            {
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    return new PasswordAuthentication(s1, s2.toCharArray());
                }
            });
        }

        int i = optionset.valueOf(optionspec13).intValue();
        int j = optionset.valueOf(optionspec14).intValue();
        boolean flag = optionset.has("fullscreen");
        boolean flag1 = optionset.has("checkGlErrors");
        boolean flag2 = optionset.has("demo");
        boolean flag3 = optionset.has("aggressiveOptimize");
        String s3 = optionset.valueOf(optionspec12);

        if (flag3)
        {
            System.setProperty("lwjgl3.aggressiveOptimize", "true");
            System.setProperty("lwjgl3.disableYield", "true");
            System.err.println("[LWJGL3-LAUNCH] Aggressive optimize profile enabled");
        }

        if (optionset.has(optionspec20))
        {
            int k = optionset.valueOf(optionspec20).intValue();

            if (k > 0)
            {
                System.setProperty("lwjgl3.chunkWorkers", Integer.toString(k));
                System.err.println("[LWJGL3-LAUNCH] Chunk worker override: " + k);
            }
        }

        Gson gson = (new GsonBuilder()).registerTypeAdapter(PropertyMap.class, new Serializer()).create();
        PropertyMap propertymap = gson.fromJson(optionset.valueOf(optionspec15), PropertyMap.class);
        PropertyMap propertymap1 = gson.fromJson(optionset.valueOf(optionspec16), PropertyMap.class);
        File file1 = expandEnvInFile(optionset.valueOf(optionspec2));
        String s5 = optionset.has(optionspec17) ? optionspec17.value(optionset) : null;
        File file2 = resolveAssetsDirectory(file1, optionset.has(optionspec3) ? expandEnvInFile(optionset.valueOf(optionspec3)) : null, s5);
        File file3 = optionset.has(optionspec4) ? expandEnvInFile(optionset.valueOf(optionspec4)) : new File(file1, "resourcepacks/");
        String s4 = optionset.has(optionspec10) ? optionspec10.value(optionset) : optionspec9.value(optionset);
        String s6 = optionset.valueOf(optionspec);
        Integer integer = optionset.valueOf(optionspec1);
        Session session = new Session(optionspec9.value(optionset), s4, optionspec11.value(optionset), optionspec18.value(optionset));
        GameConfiguration gameconfiguration = new GameConfiguration(new GameConfiguration.UserInformation(session, propertymap, propertymap1, proxy), new GameConfiguration.DisplayInformation(i, j, flag, flag1), new GameConfiguration.FolderInformation(file1, file3, file2, s5), new GameConfiguration.GameInformation(flag2, s3), new GameConfiguration.ServerInformation(s6, integer.intValue()));
        Runtime.getRuntime().addShutdownHook(new Thread("Client Shutdown Thread")
        {
            public void run()
            {
                Minecraft.stopIntegratedServer();
            }
        });
        Thread.currentThread().setName("Client thread");
        (new Minecraft(gameconfiguration)).run();
    }

    private static boolean hasValue(String str)
    {
        return str != null && !str.isEmpty();
    }

    private static File expandEnvInFile(File file)
    {
        if (file == null)
        {
            return null;
        }

        String path = file.getPath();
        String expanded = expandWindowsEnv(path);
        return path.equals(expanded) ? file : new File(expanded);
    }

    private static String expandWindowsEnv(String path)
    {
        if (path == null || path.indexOf(37) < 0)
        {
            return path;
        }

        StringBuilder builder = new StringBuilder(path.length());
        int index = 0;

        while (index < path.length())
        {
            int start = path.indexOf(37, index);

            if (start < 0 || start == path.length() - 1)
            {
                builder.append(path.substring(index));
                break;
            }

            int end = path.indexOf(37, start + 1);

            if (end < 0)
            {
                builder.append(path.substring(index));
                break;
            }

            builder.append(path.substring(index, start));
            String key = path.substring(start + 1, end);
            String value = resolveEnvValue(key);
            builder.append(value != null && !value.isEmpty() ? value : path.substring(start, end + 1));
            index = end + 1;
        }

        return builder.toString();
    }

    private static String resolveEnvValue(String key)
    {
        String value = System.getenv(key);

        if (value != null)
        {
            return value;
        }

        for (Map.Entry<String, String> entry : System.getenv().entrySet())
        {
            if (entry.getKey().equalsIgnoreCase(key))
            {
                return entry.getValue();
            }
        }

        return null;
    }

    private static File resolveAssetsDirectory(File gameDir, File requestedAssetsDir, String assetIndex)
    {
        File fallbackAssetsDir = new File(gameDir, "assets/");
        File candidate = requestedAssetsDir != null ? normalizeRequestedAssetsDir(gameDir, requestedAssetsDir, assetIndex) : fallbackAssetsDir;

        if (assetIndex == null || assetIndex.isEmpty() || hasAssetIndex(candidate, assetIndex))
        {
            return candidate;
        }

        if (!candidate.getAbsoluteFile().equals(fallbackAssetsDir.getAbsoluteFile()) && hasAssetIndex(fallbackAssetsDir, assetIndex))
        {
            System.err.println("[LWJGL3-LAUNCH] Requested assetsDir missing index '" + assetIndex + ".json': " + candidate.getAbsolutePath());
            System.err.println("[LWJGL3-LAUNCH] Falling back to gameDir assets: " + fallbackAssetsDir.getAbsolutePath());
            return fallbackAssetsDir;
        }

        return candidate;
    }

    private static File normalizeRequestedAssetsDir(File gameDir, File requestedAssetsDir, String assetIndex)
    {
        if (requestedAssetsDir == null || requestedAssetsDir.isAbsolute() || assetIndex == null || assetIndex.isEmpty())
        {
            return requestedAssetsDir;
        }

        if (hasAssetIndex(requestedAssetsDir, assetIndex))
        {
            return requestedAssetsDir;
        }

        File fromGameDir = new File(gameDir, requestedAssetsDir.getPath());

        if (hasAssetIndex(fromGameDir, assetIndex))
        {
            return fromGameDir;
        }

        File parent = gameDir != null ? gameDir.getAbsoluteFile().getParentFile() : null;

        if (parent != null)
        {
            File fromParent = new File(parent, requestedAssetsDir.getPath());

            if (hasAssetIndex(fromParent, assetIndex))
            {
                return fromParent;
            }
        }

        return requestedAssetsDir;
    }

    private static boolean hasAssetIndex(File assetsDir, String assetIndex)
    {
        if (assetsDir == null || assetIndex == null || assetIndex.isEmpty())
        {
            return false;
        }

        return (new File(new File(assetsDir, "indexes"), assetIndex + ".json")).isFile();
    }
}
