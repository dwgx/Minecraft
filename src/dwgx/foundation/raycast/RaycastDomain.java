package dwgx.foundation.raycast;

/**
 * Shared raycast guard helpers.
 */
public final class RaycastDomain
{
    private RaycastDomain()
    {
    }

    public static boolean withinDistance(double squaredDistance, double maxDistance)
    {
        double limit = Math.max(0.0D, maxDistance);
        return squaredDistance <= limit * limit;
    }
}

