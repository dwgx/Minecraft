package client.config;

import com.google.gson.JsonObject;

public interface MigrationStep
{
    int fromVersion();

    int toVersion();

    JsonObject migrate(JsonObject root);
}
