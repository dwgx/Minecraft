package client.config;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public final class ConfigMigrator
{
    private final List<MigrationStep> steps = new ArrayList<MigrationStep>();

    public void addStep(MigrationStep step)
    {
        if (step != null)
        {
            this.steps.add(step);
        }
    }

    public JsonObject migrate(JsonObject root, int from, int to)
    {
        if (root == null || from >= to)
        {
            return root;
        }

        JsonObject current = root;
        int version = from;

        while (version < to)
        {
            MigrationStep step = this.findStep(version);

            if (step == null)
            {
                break;
            }

            current = step.migrate(current);
            version = step.toVersion();
        }

        current.addProperty("schemaVersion", Integer.valueOf(to));
        return current;
    }

    private MigrationStep findStep(int fromVersion)
    {
        for (int i = 0; i < this.steps.size(); ++i)
        {
            MigrationStep step = this.steps.get(i);

            if (step.fromVersion() == fromVersion)
            {
                return step;
            }
        }

        return null;
    }
}
