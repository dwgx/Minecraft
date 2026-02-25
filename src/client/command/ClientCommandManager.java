package client.command;

import client.config.ConfigManager;
import client.core.ClientBootstrap;
import client.i18n.I18nManager;
import client.module.Module;
import client.module.ModuleManager;
import client.setting.ColorSetting;
import client.setting.ColorValue;
import client.setting.KeybindSetting;
import client.setting.NumberSetting;
import client.setting.Setting;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import client.runtime.lwjgl.GlfwKeyboard;

/**
 * Local client command:
 * .client <module> [enable|disable|toggle|status|bind|locale]
 */
public final class ClientCommandManager
{
    private static final String ROOT = ".client";
    private static final String SUB_LOCALE = "locale";
    private static final String SUB_STATUS = "status";
    private static final String SUB_LIST = "list";
    private static final String SUB_RELOAD = "reload";
    private static final List<String> ACTION_TOKENS = Arrays.asList(new String[] {"enable", "disable", "toggle", "status", "bind"});
    private static final List<String> BIND_TOKENS = buildBindTokens();
    private final ModuleManager modules;

    public ClientCommandManager(ModuleManager modules)
    {
        this.modules = modules;
    }

    /**
     * Execute local command; return false if command should fall back to vanilla handling.
     */
    public boolean execute(String rawInput)
    {
        CommandInput input = parseCommand(rawInput);

        if (input == null)
        {
            return false;
        }

        if (input.args.isEmpty())
        {
            this.printUsage();
            return true;
        }

        String firstArg = normalize(input.args.get(0));

        if (equalsIgnoreCase(firstArg, SUB_STATUS))
        {
            this.printAllModuleStatus();
            return true;
        }

        if (equalsIgnoreCase(firstArg, SUB_LOCALE))
        {
            this.handleLocaleCommand(input.args);
            return true;
        }

        Module module = this.resolveModule(firstArg);

        if (module == null)
        {
            this.chat(this.tr("command.client.module_not_found", "\u00a7cModule not found: \u00a7f{0}", firstArg));
            this.chat(this.tr("command.client.available_modules", "\u00a77Available modules: \u00a7f{0}", join(this.collectModuleTokens(), ", ")));
            return true;
        }

        Action action = Action.fromToken(input.args.size() >= 2 ? input.args.get(1) : "toggle");

        if (action == null)
        {
            this.chat(this.tr("command.client.invalid_action", "\u00a7cInvalid action: \u00a7f{0}", input.args.get(1)));
            this.chat(this.tr("command.client.valid_actions", "\u00a77Valid actions: \u00a7fenable, disable, toggle, status, bind, locale"));
            return true;
        }

        if (action == Action.BIND)
        {
            this.applyBind(module, input.args);
            return true;
        }

        this.applyAction(module, action);
        return true;
    }

    /**
     * Return null when input is not a local command and should use vanilla completion.
     */
    public List<String> complete(String input, int cursor)
    {
        if (input == null)
        {
            return null;
        }

        int safeCursor = Math.max(0, Math.min(cursor, input.length()));
        String prefix = input.substring(0, safeCursor);
        String trimmed = prefix.trim();

        if (!prefix.startsWith("."))
        {
            return null;
        }

        List<String> tokens = splitTokens(trimmed);
        boolean endsWithSpace = prefix.endsWith(" ");

        if (tokens.isEmpty())
        {
            return Collections.singletonList(ROOT);
        }

        String rootToken = tokens.get(0);

        if (!isRootOrRootPrefix(rootToken))
        {
            return null;
        }

        if (tokens.size() == 1 && !endsWithSpace)
        {
            return filterPrefix(Collections.singletonList(ROOT), rootToken);
        }

        if (tokens.size() == 1)
        {
            return this.collectSecondArgCandidates();
        }

        if (tokens.size() == 2 && !endsWithSpace)
        {
            return filterPrefix(this.collectSecondArgCandidates(), tokens.get(1));
        }

        String second = normalize(tokens.get(1));

        if (tokens.size() == 2)
        {
            if (SUB_STATUS.equals(second))
            {
                return Collections.emptyList();
            }

            if (SUB_LOCALE.equals(second))
            {
                return this.collectLocaleCandidates();
            }

            return new ArrayList<String>(ACTION_TOKENS);
        }

        if (SUB_STATUS.equals(second))
        {
            return Collections.emptyList();
        }

        if (SUB_LOCALE.equals(second))
        {
            if (tokens.size() == 3 && !endsWithSpace)
            {
                return filterPrefix(this.collectLocaleCandidates(), tokens.get(2));
            }

            return Collections.emptyList();
        }

        if (tokens.size() == 3 && !endsWithSpace)
        {
            return filterPrefix(ACTION_TOKENS, tokens.get(2));
        }

        Action action = Action.fromToken(tokens.get(2));

        if (action != Action.BIND)
        {
            return Collections.emptyList();
        }

        if (tokens.size() == 3 && endsWithSpace)
        {
            return new ArrayList<String>(BIND_TOKENS);
        }

        if (tokens.size() == 4 && !endsWithSpace)
        {
            return filterPrefix(BIND_TOKENS, tokens.get(3));
        }

        return Collections.emptyList();
    }

    private void handleLocaleCommand(List<String> args)
    {
        I18nManager i18n = ClientBootstrap.instance().getI18n();

        if (i18n == null)
        {
            return;
        }

        if (args.size() < 2)
        {
            this.chat(this.tr("i18n.locale.current", "\u00a77Current locale: \u00a7f{0}", i18n.getCurrentLocale()));
            this.chat(this.tr("i18n.locale.usage", "\u00a77Usage: \u00a7f.client locale <list|reload|code>"));
            this.printLocaleList(i18n);
            return;
        }

        String token = normalize(args.get(1));

        if (SUB_LIST.equals(token))
        {
            this.printLocaleList(i18n);
            return;
        }

        if (SUB_RELOAD.equals(token))
        {
            i18n.reload();
            this.chat(this.tr("i18n.locale.current", "\u00a77Current locale: \u00a7f{0}", i18n.getCurrentLocale()));
            this.printLocaleList(i18n);
            this.persistLocale();
            return;
        }

        Set<String> available = i18n.getAvailableLocales();

        if (!available.contains(token))
        {
            this.chat(this.tr("i18n.locale.not_found", "\u00a7cUnknown locale: \u00a7f{0}", token));
            this.printLocaleList(i18n);
            return;
        }

        i18n.setCurrentLocale(token);
        String current = i18n.getCurrentLocale();
        this.chat(this.tr("i18n.locale.changed", "\u00a7aLocale switched to \u00a7f{0} \u00a78({1})", current, i18n.getLocaleDisplayName(current)));
        this.persistLocale();
    }

    private void printLocaleList(I18nManager i18n)
    {
        this.chat(this.tr("i18n.locale.available_title", "\u00a76[Client] Available locales:"));
        List<String> locales = new ArrayList<String>(i18n.getAvailableLocales());
        Collections.sort(locales);

        for (int i = 0; i < locales.size(); ++i)
        {
            String code = locales.get(i);
            this.chat(this.tr("i18n.locale.available_item", " \u00a77- \u00a7f{0} \u00a78({1})", i18n.getLocaleDisplayName(code), code));
        }
    }

    private void persistLocale()
    {
        ConfigManager config = ClientBootstrap.instance().getConfigManager();

        if (config == null)
        {
            return;
        }

        try
        {
            config.markClientDirty();
            config.saveClient();
        }
        catch (IOException ignored)
        {
        }
    }

    private void applyAction(Module module, Action action)
    {
        switch (action)
        {
            case ENABLE:
                module.setEnabled(true);
                this.chat(this.tr("command.client.enabled", "\u00a7aEnabled \u00a7f{0}", this.moduleDisplayName(module)));
                return;

            case DISABLE:
                module.setEnabled(false);
                this.chat(this.tr("command.client.disabled", "\u00a7eDisabled \u00a7f{0}", this.moduleDisplayName(module)));
                return;

            case TOGGLE:
                module.toggle();
                this.chat(this.tr("command.client.toggled", "\u00a7b{0} \u00a77=> \u00a7f{1}", this.moduleDisplayName(module), this.moduleStateLabel(module.isEnabled())));
                return;

            case STATUS:
                this.printModuleStatus(module);
                return;

            default:
                return;
        }
    }

    private void applyBind(Module module, List<String> args)
    {
        if (args.size() < 3)
        {
            this.chat(this.tr("command.client.current_bind", "\u00a77Current bind: \u00a7f{0}", module.getBind().getDisplayName()));
            this.chat(this.tr("command.client.bind_usage", "\u00a77Usage: \u00a7f.client {0} bind <key|none>", normalize(module.getId())));
            return;
        }

        Integer keyCode = parseKeyCodeToken(args.get(2));

        if (keyCode == null)
        {
            this.chat(this.tr("command.client.unrecognized_key", "\u00a7cUnrecognized key: \u00a7f{0}", args.get(2)));
            this.chat(this.tr("command.client.bind_example", "\u00a77Example: \u00a7f.client {0} bind rshift", normalize(module.getId())));
            this.chat(this.tr("command.client.bind_unbind", "\u00a77Unbind: \u00a7f.client {0} bind none", normalize(module.getId())));
            return;
        }

        module.getBind().setKeyCode(keyCode.intValue());
        this.chat(this.tr("command.client.bind_set", "\u00a7a{0} \u00a77bound to \u00a7f{1}", this.moduleDisplayName(module), module.getBind().getDisplayName()));
    }

    private Module resolveModule(String token)
    {
        if (token == null || token.isEmpty())
        {
            return null;
        }

        String normalized = normalize(token);
        Module byId = this.modules.getById(normalized);

        if (byId != null)
        {
            return byId;
        }

        Module byName = this.modules.getByName(normalized);

        if (byName != null)
        {
            return byName;
        }

        List<Module> all = this.modules.getAll();

        for (int i = 0; i < all.size(); ++i)
        {
            Module module = all.get(i);

            if (normalize(module.getDisplayName()).equals(normalized))
            {
                return module;
            }
        }

        return null;
    }

    private List<String> collectSecondArgCandidates()
    {
        List<String> candidates = new ArrayList<String>(this.collectModuleTokens());
        candidates.add(SUB_STATUS);
        candidates.add(SUB_LOCALE);
        return candidates;
    }

    private List<String> collectLocaleCandidates()
    {
        I18nManager i18n = ClientBootstrap.instance().getI18n();
        List<String> out = new ArrayList<String>();
        out.add(SUB_LIST);
        out.add(SUB_RELOAD);

        if (i18n != null)
        {
            List<String> locales = new ArrayList<String>(i18n.getAvailableLocales());
            Collections.sort(locales);
            out.addAll(locales);
        }

        return out;
    }

    private Set<String> collectModuleTokens()
    {
        Set<String> out = new LinkedHashSet<String>();
        List<Module> all = this.modules.getAll();

        for (int i = 0; i < all.size(); ++i)
        {
            Module module = all.get(i);
            out.add(normalize(module.getId()));
            out.add(normalize(module.getName()));
            out.add(normalize(module.getDisplayName()));
        }

        return out;
    }

    private void printUsage()
    {
        this.chat(this.tr("command.client.usage", "\u00a77Usage: \u00a7f.client <module> [enable|disable|toggle|status|bind|locale]"));
        this.chat(this.tr("command.client.usage_example_1", "\u00a77Example: \u00a7f.client eagle"));
        this.chat(this.tr("command.client.usage_example_2", "\u00a77Example: \u00a7f.client eagle bind rshift"));
        this.chat(this.tr("command.client.usage_example_3", "\u00a77Example: \u00a7f.client locale en_us"));
    }

    private void printAllModuleStatus()
    {
        List<Module> all = this.modules.getAll();
        this.chat(this.tr("command.client.module_overview_title", "\u00a76[Client] Module Overview:"));

        for (int i = 0; i < all.size(); ++i)
        {
            Module module = all.get(i);
            this.chat(this.tr(
                "command.client.module_overview_item",
                " \u00a77- \u00a7f{0} \u00a78({1}) \u00a77[{2}\u00a77] \u00a78[\u00a7f{3}\u00a78]",
                this.moduleDisplayName(module),
                module.getId(),
                module.isEnabled() ? "\u00a7a" + this.moduleStateLabel(true) : "\u00a7c" + this.moduleStateLabel(false),
                module.getBind().getDisplayName()
            ));
        }
    }

    private void printModuleStatus(Module module)
    {
        this.chat(this.tr("command.client.module_status_title", "\u00a76[Client] \u00a7f{0} \u00a77[{1}\u00a77]", this.moduleDisplayName(module), module.isEnabled() ? "\u00a7a" + this.moduleStateLabel(true) : "\u00a7c" + this.moduleStateLabel(false)));
        this.chat(this.tr("command.client.module_status_id", " \u00a77id: \u00a7f{0}", module.getId()));
        this.chat(this.tr("command.client.module_status_category", " \u00a77category: \u00a7f{0}", module.getCategory().getDisplayName()));
        this.chat(this.tr("command.client.module_status_bind", " \u00a77bind: \u00a7f{0}", module.getBind().getDisplayName()));

        List<Setting<?>> settings = module.getSettings();

        if (settings.isEmpty())
        {
            this.chat(this.tr("command.client.module_status_no_settings", " \u00a78(no settings)"));
            return;
        }

        for (int i = 0; i < settings.size(); ++i)
        {
            Setting<?> setting = settings.get(i);
            this.chat(this.tr("command.client.module_status_setting", " \u00a77{0} \u00a78({1}) \u00a77= \u00a7f{2}", setting.getDisplayName(module.getId()), setting.getKey(), this.settingValue(module.getId(), setting)));
        }
    }

    private String settingValue(String moduleId, Setting<?> setting)
    {
        if (setting instanceof ColorSetting)
        {
            ColorValue c = ((ColorSetting)setting).get();
            return this.tr("command.client.setting.color", "rgba({0},{1},{2},{3}), rainbow={4}", Integer.valueOf(c.getR()), Integer.valueOf(c.getG()), Integer.valueOf(c.getB()), Integer.valueOf(c.getA()), c.isRainbow() ? this.tr("command.client.value.true", "true") : this.tr("command.client.value.false", "false"));
        }

        if (setting instanceof KeybindSetting)
        {
            KeybindSetting bind = (KeybindSetting)setting;
            return bind.getDisplayName();
        }

        if (setting instanceof NumberSetting)
        {
            NumberSetting numberSetting = (NumberSetting)setting;
            return numberSetting.format();
        }

        if (setting instanceof client.setting.BoolSetting)
        {
            return ((client.setting.BoolSetting)setting).isEnabled() ? this.moduleStateLabel(true) : this.moduleStateLabel(false);
        }

        if (setting instanceof client.setting.EnumSetting)
        {
            Object value = setting.get();
            return value == null ? this.tr("clickgui.value.null", "null") : setting.getDisplayOption(moduleId, value);
        }

        Object value = setting.get();
        if (value == null)
        {
            return this.tr("clickgui.value.null", "null");
        }

        if (value instanceof Boolean)
        {
            return ((Boolean)value).booleanValue() ? this.tr("command.client.value.true", "true") : this.tr("command.client.value.false", "false");
        }

        return String.valueOf(value);
    }

    private String moduleDisplayName(Module module)
    {
        return module == null ? "" : module.getDisplayName();
    }

    private String moduleStateLabel(boolean enabled)
    {
        return enabled ? this.tr("command.client.state.on", "ON") : this.tr("command.client.state.off", "OFF");
    }

    private String tr(String key, String fallback, Object... args)
    {
        I18nManager i18n = ClientBootstrap.instance().getI18n();
        return i18n == null ? fallback : i18n.translateOrDefault(key, fallback, args);
    }

    private void chat(String message)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc != null && mc.ingameGUI != null && mc.ingameGUI.getChatGUI() != null)
        {
            mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message));
        }
    }

    private static CommandInput parseCommand(String rawInput)
    {
        if (rawInput == null)
        {
            return null;
        }

        String trimmed = rawInput.trim();

        if (!trimmed.startsWith("."))
        {
            return null;
        }

        List<String> tokens = splitTokens(trimmed);

        if (tokens.isEmpty() || !equalsIgnoreCase(tokens.get(0), ROOT))
        {
            return null;
        }

        List<String> args = tokens.size() > 1 ? tokens.subList(1, tokens.size()) : Collections.<String>emptyList();
        return new CommandInput(args);
    }

    private static List<String> splitTokens(String text)
    {
        if (text == null || text.isEmpty())
        {
            return Collections.emptyList();
        }

        String[] arr = text.split("\\s+");
        List<String> out = new ArrayList<String>();

        for (int i = 0; i < arr.length; ++i)
        {
            if (!arr[i].isEmpty())
            {
                out.add(arr[i]);
            }
        }

        return out;
    }

    private static List<String> filterPrefix(List<String> candidates, String current)
    {
        List<String> out = new ArrayList<String>();
        String normalized = normalize(current);

        for (int i = 0; i < candidates.size(); ++i)
        {
            String candidate = candidates.get(i);

            if (normalize(candidate).startsWith(normalized))
            {
                out.add(candidate);
            }
        }

        return out;
    }

    private static boolean isRootOrRootPrefix(String rootToken)
    {
        String normalized = normalize(rootToken);
        return ROOT.startsWith(normalized) || ROOT.equals(normalized);
    }

    private static String join(Set<String> values, String sep)
    {
        StringBuilder sb = new StringBuilder();
        int i = 0;

        for (String value : values)
        {
            if (i++ > 0)
            {
                sb.append(sep);
            }

            sb.append(value);
        }

        return sb.toString();
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static boolean equalsIgnoreCase(String left, String right)
    {
        return normalize(left).equals(normalize(right));
    }

    private static Integer parseKeyCodeToken(String token)
    {
        String normalized = normalize(token).replace('-', '_');

        if (normalized.isEmpty())
        {
            return null;
        }

        if ("none".equals(normalized) || "clear".equals(normalized) || "unbind".equals(normalized) || "null".equals(normalized))
        {
            return Integer.valueOf(KeybindSetting.NONE_KEY_CODE);
        }

        int key = findKeyCodeByName(normalized);

        if (key > 0)
        {
            return Integer.valueOf(key);
        }

        try
        {
            int numeric = Integer.parseInt(normalized);
            return Integer.valueOf(numeric <= 0 ? KeybindSetting.NONE_KEY_CODE : numeric);
        }
        catch (NumberFormatException ignored)
        {
            return null;
        }
    }

    private static List<String> buildBindTokens()
    {
        Set<String> out = new LinkedHashSet<String>();
        out.add("none");
        out.add("esc");
        out.add("escape");
        out.add("rshift");
        out.add("lshift");
        out.add("rcontrol");
        out.add("lcontrol");
        out.add("rmenu");
        out.add("lmenu");

        for (int key = 1; key < 256; ++key)
        {
            String name = GlfwKeyboard.getKeyName(key);

            if (name != null && !name.trim().isEmpty())
            {
                out.add(normalize(name));
            }
        }

        return new ArrayList<String>(out);
    }

    private static int findKeyCodeByName(String normalizedKeyName)
    {
        if ("esc".equals(normalizedKeyName) || "escape".equals(normalizedKeyName))
        {
            return 1;
        }

        for (int key = 1; key < 256; ++key)
        {
            if (normalize(GlfwKeyboard.getKeyName(key)).equals(normalizedKeyName))
            {
                return key;
            }
        }

        return -1;
    }

    private static final class CommandInput
    {
        private final List<String> args;

        private CommandInput(List<String> args)
        {
            this.args = args;
        }
    }

    private enum Action
    {
        ENABLE("enable"),
        DISABLE("disable"),
        TOGGLE("toggle"),
        STATUS("status"),
        BIND("bind");

        private final String token;

        Action(String token)
        {
            this.token = token;
        }

        private static Action fromToken(String token)
        {
            String normalized = ClientCommandManager.normalize(token);

            for (Action action : values())
            {
                if (action.token.equals(normalized))
                {
                    return action;
                }
            }

            return null;
        }
    }
}
