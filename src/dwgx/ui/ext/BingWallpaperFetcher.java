package dwgx.ui.ext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
 * The goal is to avoid adding menu options: every client start pulls one image
 * into a temp file and the main menu uses STATIC_IMAGE mode to render it.
 */
public final class BingWallpaperFetcher
{
    private static final Logger LOGGER = LogManager.getLogger(BingWallpaperFetcher.class);
    private static final String[] SOURCES = new String[]
    {
        // Prefer 1080p to keep download small while still crisp.
        "https://bing.img.run/rand.php",
        "https://bing.img.run/1920x1080.php",
        "https://bing.img.run/rand_uhd.php"
    };

    private BingWallpaperFetcher()
    {
    }

    public interface Callback
    {
        void onComplete(String localPath);
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
        // First attempt: random pick, later attempts: deterministic fallback.
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

        Path tmp = Files.createTempFile("bing-wallpaper-", ".jpg");
        tmp.toFile().deleteOnExit();

        try (InputStream in = conn.getInputStream())
        {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        finally
        {
            conn.disconnect();
        }

        return tmp.toAbsolutePath().toString();
    }
}
