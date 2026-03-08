package dwgx.ui.ext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lightweight one-shot downloader for Bing daily/random wallpapers.
 *
 * Downloads to a persistent location ({@code config/client/bing-wallpaper.jpg})
 * so the image survives restarts and can be displayed immediately on the next
 * launch while a fresh download runs in the background.
 */
public final class BingWallpaperFetcher
{
    private static final Logger LOGGER = LogManager.getLogger(BingWallpaperFetcher.class);
    private static final String PERSISTENT_FILENAME = "bing-wallpaper.jpg";
    private static final String[] SOURCES = new String[]
    {
        "https://bing.img.run/rand.php",
        "https://bing.img.run/1920x1080.php",
        "https://bing.img.run/rand_uhd.php"
    };

    private static volatile Path persistentDir;

    private BingWallpaperFetcher()
    {
    }

    public interface Callback
    {
        void onComplete(String localPath);
    }

    /**
     * Sets the persistent directory where the wallpaper file is stored.
     * Should be called early in startup (e.g. from config init).
     */
    public static void setPersistentDir(Path dir)
    {
        persistentDir = dir;
    }

    /**
     * Returns the path to the cached wallpaper if it exists on disk, or null.
     */
    public static String getCachedPath()
    {
        Path dir = persistentDir;

        if (dir == null)
        {
            return null;
        }

        File file = dir.resolve(PERSISTENT_FILENAME).toFile();
        return file.isFile() && file.length() > 0 ? file.getAbsolutePath() : null;
    }

    public static String downloadOnce()
    {
        for (int i = 0; i < SOURCES.length; i++)
        {
            String url = pickSource(i);

            try
            {
                String path = download(url);

                if (path != null)
                {
                    return path;
                }
            }
            catch (IOException ex)
            {
                LOGGER.warn("Failed to download Bing wallpaper from {}", url, ex);
            }
        }

        return null;
    }

    public static void downloadOnceAsync(final Callback callback)
    {
        Thread worker = new Thread("bing-wallpaper-fetch")
        {
            public void run()
            {
                String path = null;

                try
                {
                    path = downloadOnce();
                }
                catch (Throwable throwable)
                {
                    LOGGER.warn("Unexpected error while downloading Bing wallpaper.", throwable);
                }

                if (callback != null)
                {
                    callback.onComplete(path);
                }
            }
        };
        worker.setDaemon(true);
        worker.start();
    }

    private static String pickSource(int attempt)
    {
        if (attempt == 0)
        {
            return SOURCES[ThreadLocalRandom.current().nextInt(SOURCES.length)];
        }

        int idx = attempt - 1;
        return SOURCES[idx % SOURCES.length];
    }

    private static String download(String urlStr) throws IOException
    {
        HttpURLConnection conn = (HttpURLConnection)new URL(urlStr).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "Minecraft-MenuFX/1.0");

        int code = conn.getResponseCode();

        if (code >= 300 && code < 400)
        {
            String location = conn.getHeaderField("Location");

            if (location != null && !location.isEmpty())
            {
                conn.disconnect();
                return download(location);
            }
        }

        if (code != HttpURLConnection.HTTP_OK)
        {
            conn.disconnect();
            return null;
        }

        Path target = resolvePersistentPath();

        if (target == null)
        {
            // Fallback to temp if no persistent dir configured
            target = Files.createTempFile("bing-wallpaper-", ".jpg");
            target.toFile().deleteOnExit();
        }

        try (InputStream in = conn.getInputStream())
        {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        finally
        {
            conn.disconnect();
        }

        return target.toAbsolutePath().toString();
    }

    private static Path resolvePersistentPath()
    {
        Path dir = persistentDir;

        if (dir == null)
        {
            return null;
        }

        try
        {
            Files.createDirectories(dir);
            return dir.resolve(PERSISTENT_FILENAME);
        }
        catch (IOException ex)
        {
            LOGGER.warn("Cannot create persistent wallpaper directory: {}", dir, ex);
            return null;
        }
    }
}