package client.config;

import client.core.BuildInfo;
import client.core.ClientInfo;
import client.hud.Anchor;
import client.hud.Dock;
import client.hud.HudElement;
import client.hud.HudManager;
import client.hud.HudTransform;
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
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * 分域配置管理器：
 * - client.json
 * - modules.json
 * - hud.json
 */
public final class ConfigManager implements ModuleStateListener
{
    private static final String FILE_CLIENT = "client.json";
    private static final String FILE_MODULES = "modules.json";
    private static final String FILE_HUD = "hud.json";

    private final ClientInfo clientInfo;
    private final ModuleManager moduleManager;
    private final HudManager hudManager;
    private final ProfileManager profileManager;
    private final ConfigIO io = new ConfigIO();
    private final ConfigMigrator migrator = new ConfigMigrator();
    private boolean autosaveOnModuleChange = true;

    public ConfigManager(Path configRoot, ClientInfo clientInfo, ModuleManager moduleManager, HudManager hudManager)
    {
        this.clientInfo = clientInfo;
        this.moduleManager = moduleManager;
        this.hudManager = hudManager;
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

    public void setAutosaveOnModuleChange(boolean autosaveOnModuleChange)
    {
        this.autosaveOnModuleChange = autosaveOnModuleChange;
    }

    public void saveAll() throws IOException
    {
        this.saveClient();
        this.saveModules();
        this.saveHud();
    }

    public void loadAll() throws IOException
    {
        this.loadClient();
        this.loadModules();
        this.loadHud();
    }

    public void saveClient() throws IOException
    {
        JsonObject root = this.createHeader();
        root.addProperty("clientId", this.clientInfo.getId());
        root.addProperty("clientName", this.clientInfo.getName());
        root.addProperty("clientVersion", this.clientInfo.getVersion());
        this.io.writeAtomic(this.path(FILE_CLIENT), root);
    }

    public void loadClient() throws IOException
    {
        JsonObject root = this.ensureSchema(this.io.read(this.path(FILE_CLIENT)));
        this.maybeWriteMigrated(FILE_CLIENT, root);
    }

    public void saveModules() throws IOException
    {
        JsonObject root = this.createHeader();
        JsonObject modules = new JsonObject();
        List<Module> allModules = this.moduleManager.getAll();

        for (int i = 0; i < allModules.size(); ++i)
        {
            Module module = allModules.get(i);
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("enabled", Boolean.valueOf(module.isEnabled()));

            JsonObject bind = new JsonObject();
            bind.addProperty("key", Integer.valueOf(module.getBind().getKeyCode()));
            bind.addProperty("mode", module.getBind().getMode().name());
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
        this.io.writeAtomic(this.path(FILE_MODULES), root);
    }

    public void loadModules() throws IOException
    {
        JsonObject root = this.ensureSchema(this.io.read(this.path(FILE_MODULES)));
        JsonObject modules = root.has("modules") && root.get("modules").isJsonObject() ? root.getAsJsonObject("modules") : new JsonObject();
        List<Module> allModules = this.moduleManager.getAll();

        for (int i = 0; i < allModules.size(); ++i)
        {
            Module module = allModules.get(i);

            if (!modules.has(module.getId()) || !modules.get(module.getId()).isJsonObject())
            {
                continue;
            }

            JsonObject moduleJson = modules.getAsJsonObject(module.getId());

            if (moduleJson.has("enabled"))
            {
                module.setEnabled(moduleJson.get("enabled").getAsBoolean());
            }

            if (moduleJson.has("bind") && moduleJson.get("bind").isJsonObject())
            {
                JsonObject bind = moduleJson.getAsJsonObject("bind");

                if (bind.has("key"))
                {
                    module.getBind().setKeyCode(bind.get("key").getAsInt());
                }

                if (bind.has("mode"))
                {
                    try
                    {
                        module.getBind().setMode(KeybindSetting.BindMode.valueOf(bind.get("mode").getAsString()));
                    }
                    catch (IllegalArgumentException ignored)
                    {
                    }
                }
            }

            if (moduleJson.has("settings") && moduleJson.get("settings").isJsonObject())
            {
                JsonObject settings = moduleJson.getAsJsonObject("settings");
                List<Setting<?>> moduleSettings = module.getSettings();

                for (int j = 0; j < moduleSettings.size(); ++j)
                {
                    Setting<?> setting = moduleSettings.get(j);

                    if (settings.has(setting.getKey()))
                    {
                        this.applySetting(setting, settings.get(setting.getKey()));
                    }
                }
            }
        }

        this.maybeWriteMigrated(FILE_MODULES, root);
    }

    public void saveHud() throws IOException
    {
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
            elements.add(element.getId(), e);
        }

        root.add("elements", elements);
        this.io.writeAtomic(this.path(FILE_HUD), root);
    }

    public void loadHud() throws IOException
    {
        JsonObject root = this.ensureSchema(this.io.read(this.path(FILE_HUD)));
        JsonObject elements = root.has("elements") && root.get("elements").isJsonObject() ? root.getAsJsonObject("elements") : new JsonObject();
        List<HudElement> all = this.hudManager.getElements();

        for (int i = 0; i < all.size(); ++i)
        {
            HudElement element = all.get(i);

            if (!elements.has(element.getId()) || !elements.get(element.getId()).isJsonObject())
            {
                continue;
            }

            JsonObject e = elements.getAsJsonObject(element.getId());
            HudTransform t = element.getTransform();

            if (e.has("enabled"))
            {
                element.setEnabled(e.get("enabled").getAsBoolean());
            }

            if (e.has("anchor"))
            {
                try
                {
                    t.setAnchor(Anchor.valueOf(e.get("anchor").getAsString()));
                }
                catch (IllegalArgumentException ignored)
                {
                }
            }

            if (e.has("dock"))
            {
                try
                {
                    t.setDock(Dock.valueOf(e.get("dock").getAsString()));
                }
                catch (IllegalArgumentException ignored)
                {
                }
            }

            if (e.has("x"))
            {
                t.setOffsetX(e.get("x").getAsFloat());
            }

            if (e.has("y"))
            {
                t.setOffsetY(e.get("y").getAsFloat());
            }

            if (e.has("scale"))
            {
                t.setScale(e.get("scale").getAsFloat());
            }

            if (e.has("snapToGrid"))
            {
                t.setSnapToGrid(e.get("snapToGrid").getAsBoolean());
            }
        }

        this.maybeWriteMigrated(FILE_HUD, root);
    }

    public void onModuleChanged(Module module)
    {
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

    private JsonObject ensureSchema(JsonObject root)
    {
        int version = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : ConfigSchema.CURRENT_VERSION;

        if (version < ConfigSchema.CURRENT_VERSION)
        {
            return this.migrator.migrate(root, version, ConfigSchema.CURRENT_VERSION);
        }

        if (!root.has("schemaVersion"))
        {
            root.addProperty("schemaVersion", Integer.valueOf(ConfigSchema.CURRENT_VERSION));
        }

        return root;
    }

    private void maybeWriteMigrated(String fileName, JsonObject root) throws IOException
    {
        if (root != null && root.has("schemaVersion"))
        {
            this.io.writeAtomic(this.path(fileName), root);
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
            bindJson.addProperty("mode", bind.getMode().name());
            return bindJson;
        }

        Object value = setting.get();
        return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value.toString());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void applySetting(Setting<?> setting, JsonElement element)
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

            if (bind.has("mode"))
            {
                try
                {
                    keybindSetting.setMode(KeybindSetting.BindMode.valueOf(bind.get("mode").getAsString()));
                }
                catch (IllegalArgumentException ignored)
                {
                }
            }
        }
    }
}
