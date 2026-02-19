package dwgx.modulekit;

import java.util.Random;

/**
 * Timing and click cadence helpers shared by combat and inventory modules.
 */
public final class PulseMath
{
    private PulseMath()
    {
    }

    public static int randomBetween(Random random, int min, int max)
    {
        int lo = Math.max(0, min);
        int hi = Math.max(0, max);

        if (hi < lo)
        {
            int swap = lo;
            lo = hi;
            hi = swap;
        }

        if (hi <= lo)
        {
            return lo;
        }

        Random source = random == null ? new Random() : random;
        return lo + source.nextInt(hi - lo + 1);
    }

    public static int randomDelayMs(Random random, int minMs, int maxMs)
    {
        return randomBetween(random, minMs, maxMs);
    }

    public static long cpsDelayMs(Random random, int minCps, int maxCps, boolean randomize)
    {
        int min = Math.max(1, minCps);
        int max = Math.max(1, maxCps);

        if (max < min)
        {
            int swap = min;
            min = max;
            max = swap;
        }

        int cps = randomize ? randomBetween(random, min, max) : max;
        return Math.max(10L, Math.round(1000.0D / (double)Math.max(1, cps)));
    }
}

