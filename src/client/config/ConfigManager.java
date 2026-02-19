package client.config;

import client.core.BuildInfo;
import client.core.ClientInfo;
import client.hud.Anchor;
import client.hud.Dock;
import client.hud.HudElement;
import client.hud.HudManager;
import client.hud.HudTransform;
import client.i18n.I18nManager;
import client.module.Module;
import client.module.ModuleManager;
import client.module.ModuleStateListener;
import client.setting.BoolSetting;
import client.setting.ColorSetting;
import client.setting.ColorValue;
import client.setting.EnumSetting;
import client.setting.FloatSetting;
import client.setting.IntSetting;
import client.setting.KeybindSetting;
import client.setting.NumberSetting;
import client.setting.Setting;
import client.setting.StringSetting;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Split configuration manager:
 * - client.json
 * - modules.json
 * - hud.json
 */
public final class ConfigManager implements ModuleStateListener
{
    private static final Logger LOGGER = LogManager.getLogger(ConfigManager.class);
    private static final String FILE_CLIENT = "client.json";
    private static final String FILE_MODULES = "modules.json";
    private static final String FILE_HUD = "hud.json";

    private final ClientInfo clientInfo;
    private final ModuleManager moduleManager;
    private final HudManager hudManager;
    private final I18nManager i18n;
    private final ProfileManager profileManager;
    private final ConfigIO io = new ConfigIO();
    private final ConfigMigrator migrator = new ConfigMigrator();
    private final List<String> loadIssues = new ArrayList<String>();
    private boolean autosaveOnModuleChange = true;
    private boolean dirtyTrackingSuspended;
    private boolean dirtyClient = true;
    private boolean dirtyModules = true;
    private boolean dirtyHud = true;

    public ConfigManager(Path configRoot, ClientInfo clientInfo, ModuleManager moduleManager, HudManager hudManager, I18nManager i18n)
    {
        this.clientInfo = clientInfo;
        this.moduleManager = moduleManager;
        this.hudManager = hudManager;
        this.i18n = i18n;
        this.profileManager = new ProfileManager(configRoot);

        if (this.moduleManager != null)
        {
            this.moduleManager.setStateListener(this);
        }
    }

    public ProfileManager profiles()
    {
        return this.profileManager;
    }

    public synchronized List<String> consumeLoadIssues()
    {
        if (this.loadIssues.isEmpty())
        {
            return Collections.emptyList();
        }

        List<String> out = new ArrayList<String>(this.loadIssues);
        this.loadIssues.clear();
        return out;
    }

    public void setAutosaveOnModuleChange(boolean autosaveOnModuleChange)
    {
        this.autosaveOnModuleChange = autosaveOnModuleChange;
    }

    public void markClientDirty()
    {
        if (!this.dirtyTrackingSuspended)
        {
            this.dirtyClient = true;
        }
    }

    public void markModulesDirty()
    {
        if (!this.dirtyTrackingSuspended)
        {
            this.dirtyModules = true;
        }
    }

    public void markHudDirty()
    {
        if (!this.dirtyTrackingSuspended)
        {
            this.dirtyHud = true;
        }
    }

    public void saveAll() throws IOException
    {
        this.saveClient();
        this.saveModules();
        this.saveHud();
    }

    public void loadAll() throws IOException
    {
        synchronized (this)
        {
            this.loadIssues.clear();
        }

        this.dirtyTrackingSuspended = true;

        try
        {
            this.loadClient();
            this.loadModules();
            this.loadHud();
        }
        finally
        {
            this.dirtyTrackingSuspended = false;
            this.dirtyClient = false;
            this.dirtyModules = false;
            this.dirtyHud = false;
        }
    }

    public void saveClient() throws IOException
    {
        if (!this.dirtyClient)
        {
            return;
        }

        JsonObject root = this.createHeader();
        root.addProperty("clientId", this.clientInfo.getId());
        root.addProperty("clientName", this.clientInfo.getName());
        root.addProperty("clientVersion", this.clientInfo.getVersion());

        if (this.i18n != null)
        {
            root.addProperty("locale", this.i18n.getCurrentLocale());
        }

        this.io.writeAtomicIfChanged(this.path(FILE_CLIENT), root);
        this.dirtyClient = false;
    }

    public void loadClient() throws IOException
    {
        Path target = this.path(FILE_CLIENT);
        ConfigIO.ReadResult readResult = this.io.readWithResult(target);

        if (!readResult.isParsed())
        {
            this.recordLoadIssue(FILE_CLIENT, target, readResult.getError());
            this.dirtyClient = false;
            return;
        }

        SchemaResult schema = this.ensureSchema(readResult.getData());
        JsonObject root = schema.root;
        boolean changed = schema.changed;

        if (this.i18n != null)
        {
            String locale = this.readString(root, "locale");

            if (locale != null)
            {
                this.i18n.setCurrentLocale(locale);
            }
            else
            {
                root.addProperty("locale", this.i18n.getCurrentLocale());
                changed = true;
            }
        }

        this.maybeWriteMigrated(FILE_CLIENT, root, changed);
        this.dirtyClient = false;
    }

    public void saveModules() throws IOException
    {
        if (!this.dirtyModules)
        {
            return;
        }

        JsonObject root = this.createHeader();
        JsonObject modules = new JsonObject();
        List<Module> allModules = this.moduleManager.getAll();

        for (int i = 0; i < allModules.size(); ++i)
        {
            Module module = allModules.get(i);
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("enabled", Boolean.valueOf(module.isActionModule() ? false : module.isEnabled()));

            JsonObject bind = new JsonObject();
            bind.addProperty("key", Integer.valueOf(module.getBind().getKeyCode()));
            moduleJson.add("bind", bind);

            JsonObject settings = new JsonObject();
            List<Setting<?>> moduleSettings = module.getSettings();

            for (int j = 0; j < moduleSettings.size(); ++j)
            {
                Setting<?> setting = moduleSettings.get(j);
                settings.add(setting.getKey(), this.serializeSetting(setting));
            }

            moduleJson.add("settings", settings);
            modules.add(module.getId(), moduleJson);
        }

        root.add("modules", modules);
        this.io.writeAtomicIfChanged(this.path(FILE_MODULES), root);
        this.dirtyModules = false;
    }

    public void loadModules() throws IOException
    {
        Path target = this.path(FILE_MODULES);
        ConfigIO.ReadResult readResult = this.io.readWithResult(target);

        if (!readResult.isParsed())
        {
            this.recordLoadIssue(FILE_MODULES, target, readResult.getError());
            this.dirtyModules = false;
            return;
        }

        SchemaResult schema = this.ensureSchema(readResult.getData());
        JsonObject root = schema.root;
        JsonObject modules = this.readObject(root, "modules");
        boolean changed = schema.changed;

        if (modules == null)
        {
            modules = new JsonObject();
        }

        List<Module> allModules = this.moduleManager.getAll();

        for (int i = 0; i < allModules.size(); ++i)
        {
            Module module = allModules.get(i);

            JsonObject moduleJson = this.readObject(modules, module.getId());

            if (moduleJson == null)
            {
                continue;
            }

            Boolean enabled = this.readBoolean(moduleJson, "enabled");

            if (enabled != null && !module.isActionModule())
            {
                module.setEnabled(enabled.booleanValue());
            }

            JsonObject bind = this.readObject(moduleJson, "bind");

            if (bind != null)
            {
                Integer keyCode = this.readInt(bind, "key");

                if (keyCode != null)
                {
                    module.getBind().setKeyCode(keyCode.intValue());
                }
            }

            JsonObject settings = this.readObject(moduleJson, "settings");

            if (settings != null)
            {
                List<Setting<?>> moduleSettings = module.getSettings();

                for (int j = 0; j < moduleSettings.size(); ++j)
                {
                    Setting<?> setting = moduleSettings.get(j);

                    if (settings.has(setting.getKey()))
                    {
                        try
                        {
                            this.applySetting(setting, settings.get(setting.getKey()));
                        }
                        catch (RuntimeException ignored)
                        {
                        }
                    }
                }
            }
        }

        this.maybeWriteMigrated(FILE_MODULES, root, changed);
        this.dirtyModules = false;
    }

    public void saveHud() throws IOException
    {
        if (!this.dirtyHud)
        {
            return;
        }

        JsonObject root = this.createHeader();
        JsonObject elements = new JsonObject();
        List<HudElement> all = this.hudManager.getElements();

        for (int i = 0; i < all.size(); ++i)
        {
            HudElement element = all.get(i);
            HudTransform t = element.getTransform();
            JsonObject e = new JsonObject();
            e.addProperty("enabled", Boolean.valueOf(element.isEnabled()));
            e.addProperty("anchor", t.getAnchor().name());
            e.addProperty("dock", t.getDock().name());
            e.addProperty("x", Float.valueOf(t.getOffsetX()));
            e.addProperty("y", Float.valueOf(t.getOffsetY()));
            e.addProperty("scale", Float.valueOf(t.getScale()));
            e.addProperty("snapToGrid", Boolean.valueOf(t.isSnapToGrid()));

            JsonObject settings = new JsonObject();
            List<Setting<?>> elementSettings = element.getSettings();

            for (int j = 0; j < elementSettings.size(); ++j)
            {
                Setting<?> setting = elementSettings.get(j);
                settings.add(setting.getKey(), this.serializeSetting(setting));
            }

            e.add("settings", settings);
            elements.add(element.getId(), e);
        }

        root.add("elements", elements);
        this.io.writeAtomicIfChanged(this.path(FILE_HUD), root);
        this.dirtyHud = false;
    }

    public void loadHud() throws IOException
    {
        Path target = this.path(FILE_HUD);
        ConfigIO.ReadResult readResult = this.io.readWithResult(target);

        if (!readResult.isParsed())
        {
            this.recordLoadIssue(FILE_HUD, target, readResult.getError());
            this.dirtyHud = false;
            return;
        }

        SchemaResult schema = this.ensureSchema(readResult.getData());
        JsonObject root = schema.root;
        JsonObject elements = this.readObject(root, "elements");
        boolean changed = schema.changed;

        if (elements == null)
        {
            elements = new JsonObject();
        }

        List<HudElement> all = this.hudManager.getElements();

        for (int i = 0; i < all.size(); ++i)
        {
            HudElement element = all.get(i);

            JsonObject e = this.readObject(elements, element.getId());

            if (e == null)
            {
                continue;
            }

            HudTransform t = element.getTransform();

            Boolean enabled = this.readBoolean(e, "enabled");

            if (enabled != null)
            {
                element.setEnabled(enabled.booleanValue());
            }

            String anchorValue = this.readString(e, "anchor");

            if (anchorValue != null)
            {
                try
                {
                    t.setAnchor(Anchor.valueOf(anchorValue));
                }
                catch (IllegalArgumentException ignored)
                {
                }
            }

            String dockValue = this.readString(e, "dock");

            if (dockValue != null)
            {
                try
                {
                    t.setDock(Dock.valueOf(dockValue));
                }
                catch (IllegalArgumentException ignored)
                {
                }
            }

            Float offsetX = this.readFloat(e, "x");

            if (offsetX != null)
            {
                t.setOffsetX(offsetX.floatValue());
            }

            Float offsetY = this.readFloat(e, "y");

            if (offsetY != null)
            {
                t.setOffsetY(offsetY.floatValue());
            }

            Float scale = this.readFloat(e, "scale");

            if (scale != null)
            {
                t.setScale(scale.floatValue());
            }

            Boolean snapToGrid = this.readBoolean(e, "snapToGrid");

            if (snapToGrid != null)
            {
                t.setSnapToGrid(snapToGrid.booleanValue());
            }

            JsonObject settings = this.readObject(e, "settings");

            if (settings != null)
            {
                List<Setting<?>> elementSettings = element.getSettings();

                for (int j = 0; j < elementSettings.size(); ++j)
                {
                    Setting<?> setting = elementSettings.get(j);

                    if (settings.has(setting.getKey()))
                    {
                        try
                        {
                            this.applySetting(setting, settings.get(setting.getKey()));
                        }
                        catch (RuntimeException ignored)
                        {
                        }
                    }
                }
            }
        }

        this.maybeWriteMigrated(FILE_HUD, root, changed);
        this.dirtyHud = false;
    }

    public void onModuleChanged(Module module)
    {
        if (this.dirtyTrackingSuspended)
        {
            return;
        }

        this.markModulesDirty();

        if (!this.autosaveOnModuleChange)
        {
            return;
        }

        try
        {
            this.saveModules();
        }
        catch (IOException ignored)
        {
        }
    }

    private JsonObject createHeader()
    {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", Integer.valueOf(ConfigSchema.CURRENT_VERSION));
        root.addProperty("clientVersion", this.clientInfo.getVersion());
        root.addProperty("buildVersion", BuildInfo.CLIENT_VERSION);
        root.addProperty("timestamp", Long.valueOf(Instant.now().toEpochMilli()));
        return root;
    }

    private Path path(String fileName)
    {
        return this.profileManager.resolveInActiveProfile(fileName);
    }

    private SchemaResult ensureSchema(JsonObject root)
    {
        JsonObject out = root == null ? new JsonObject() : root;
        Integer versionValue = this.readInt(out, "schemaVersion");
        int version = versionValue == null ? ConfigSchema.CURRENT_VERSION : versionValue.intValue();
        boolean changed = false;

        if (version < ConfigSchema.CURRENT_VERSION)
        {
            JsonObject migrated = this.migrator.migrate(out, version, ConfigSchema.CURRENT_VERSION);
            out = migrated == null ? new JsonObject() : migrated;
            Integer migratedVersion = this.readInt(out, "schemaVersion");
            changed = migratedVersion != null && migratedVersion.intValue() == ConfigSchema.CURRENT_VERSION;
        }
        else if (versionValue == null)
        {
            out.addProperty("schemaVersion", Integer.valueOf(ConfigSchema.CURRENT_VERSION));
            changed = true;
        }

        return new SchemaResult(out, changed);
    }

    private void maybeWriteMigrated(String fileName, JsonObject root, boolean changed) throws IOException
    {
        if (root != null && changed)
        {
            this.io.writeAtomicIfChanged(this.path(fileName), root);
        }
    }

    private synchronized void recordLoadIssue(String fileName, Path path, String error)
    {
        String detail = error == null || error.isEmpty() ? "unknown parse error" : error;
        String location = path == null ? "(unknown path)" : path.toAbsolutePath().toString();
        String message = "Failed to parse config '" + fileName + "' at " + location + ": " + detail;
        this.loadIssues.add(message);
        LOGGER.warn(message);
    }

    private JsonObject readObject(JsonObject root, String key)
    {
        if (root == null || key == null || key.isEmpty() || !root.has(key))
        {
            return null;
        }

        JsonElement element = root.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private String readString(JsonObject root, String key)
    {
        if (root == null || key == null || key.isEmpty() || !root.has(key))
        {
            return null;
        }

        JsonElement element = root.get(key);

        if (element == null || !element.isJsonPrimitive())
        {
            return null;
        }

        try
        {
            return element.getAsString();
        }
        catch (RuntimeException ignored)
        {
            return null;
        }
    }

    private Boolean readBoolean(JsonObject root, String key)
    {
        if (root == null || key == null || key.isEmpty() || !root.has(key))
        {
            return null;
        }

        JsonElement element = root.get(key);

        if (element == null || !element.isJsonPrimitive())
        {
            return null;
        }

        try
        {
            return Boolean.valueOf(element.getAsBoolean());
        }
        catch (RuntimeException ignored)
        {
            return null;
        }
    }

    private Integer readInt(JsonObject root, String key)
    {
        if (root == null || key == null || key.isEmpty() || !root.has(key))
        {
            return null;
        }

        JsonElement element = root.get(key);

        if (element == null || !element.isJsonPrimitive())
        {
            return null;
        }

        try
        {
            return Integer.valueOf(element.getAsInt());
        }
        catch (RuntimeException ignored)
        {
            return null;
        }
    }

    private Float readFloat(JsonObject root, String key)
    {
        if (root == null || key == null || key.isEmpty() || !root.has(key))
        {
            return null;
        }

        JsonElement element = root.get(key);

        if (element == null || !element.isJsonPrimitive())
        {
            return null;
        }

        try
        {
            return Float.valueOf(element.getAsFloat());
        }
        catch (RuntimeException ignored)
        {
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JsonElement serializeSetting(Setting<?> setting)
    {
        if (setting instanceof BoolSetting)
        {
            return new JsonPrimitive(((BoolSetting)setting).isEnabled());
        }
        else if (setting instanceof IntSetting)
        {
            return new JsonPrimitive(((IntSetting)setting).get().intValue());
        }
        else if (setting instanceof FloatSetting)
        {
            return new JsonPrimitive(((FloatSetting)setting).get().floatValue());
        }
        else if (setting instanceof NumberSetting)
        {
            return new JsonPrimitive(((NumberSetting)setting).get().doubleValue());
        }
        else if (setting instanceof StringSetting)
        {
            return new JsonPrimitive(((StringSetting)setting).get());
        }
        else if (setting instanceof EnumSetting)
        {
            Enum<?> value = (Enum<?>)((EnumSetting)setting).get();
            return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value.name());
        }
        else if (setting instanceof ColorSetting)
        {
            ColorValue c = ((ColorSetting)setting).get();
            JsonObject color = new JsonObject();
            color.addProperty("r", Integer.valueOf(c.getR()));
            color.addProperty("g", Integer.valueOf(c.getG()));
            color.addProperty("b", Integer.valueOf(c.getB()));
            color.addProperty("a", Integer.valueOf(c.getA()));
            color.addProperty("rainbow", Boolean.valueOf(c.isRainbow()));
            return color;
        }
        else if (setting instanceof KeybindSetting)
        {
            KeybindSetting bind = (KeybindSetting)setting;
            JsonObject bindJson = new JsonObject();
            bindJson.addProperty("key", Integer.valueOf(bind.getKeyCode()));
            return bindJson;
        }

        Object value = setting.get();
        return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value.toString());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void applySetting(Setting<?> setting, JsonElement element)
    {
        try
        {
            if (element == null || element.isJsonNull())
            {
                return;
            }

            if (setting instanceof BoolSetting && element.isJsonPrimitive())
            {
                ((BoolSetting)setting).setEnabled(element.getAsBoolean());
            }
            else if (setting instanceof IntSetting && element.isJsonPrimitive())
            {
                ((IntSetting)setting).set(Integer.valueOf(element.getAsInt()));
            }
            else if (setting instanceof FloatSetting && element.isJsonPrimitive())
            {
                ((FloatSetting)setting).set(Float.valueOf(element.getAsFloat()));
            }
            else if (setting instanceof NumberSetting && element.isJsonPrimitive())
            {
                ((NumberSetting)setting).set(Double.valueOf(element.getAsDouble()));
            }
            else if (setting instanceof StringSetting && element.isJsonPrimitive())
            {
                ((StringSetting)setting).set(element.getAsString());
            }
            else if (setting instanceof EnumSetting && element.isJsonPrimitive())
            {
                ((EnumSetting)setting).setByName(element.getAsString());
            }
            else if (setting instanceof ColorSetting && element.isJsonObject())
            {
                JsonObject c = element.getAsJsonObject();
                int r = c.has("r") ? c.get("r").getAsInt() : 255;
                int g = c.has("g") ? c.get("g").getAsInt() : 255;
                int b = c.has("b") ? c.get("b").getAsInt() : 255;
                int a = c.has("a") ? c.get("a").getAsInt() : 255;
                boolean rainbow = c.has("rainbow") && c.get("rainbow").getAsBoolean();
                ((ColorSetting)setting).set(new ColorValue(r, g, b, a, rainbow));
            }
            else if (setting instanceof KeybindSetting && element.isJsonObject())
            {
                JsonObject bind = element.getAsJsonObject();
                KeybindSetting keybindSetting = (KeybindSetting)setting;

                if (bind.has("key"))
                {
                    keybindSetting.setKeyCode(bind.get("key").getAsInt());
                }
            }
            else if (setting instanceof KeybindSetting && element.isJsonPrimitive())
            {
                ((KeybindSetting)setting).setKeyCode(element.getAsInt());
            }
        }
        catch (RuntimeException ignored)
        {
        }
    }

    private static final class SchemaResult
    {
        private final JsonObject root;
        private final boolean changed;

        private SchemaResult(JsonObject root, boolean changed)
        {
            this.root = root;
            this.changed = changed;
        }
    }

}
