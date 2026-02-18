package client.command;

import client.module.Module;
import client.module.ModuleManager;
import client.setting.ColorSetting;
import client.setting.ColorValue;
import client.setting.KeybindSetting;
import client.setting.NumberSetting;
import client.setting.Setting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Keyboard;

/**
 * 本地客户端命令：
 * .client <module> [enable|disable|toggle|status|bind]
 */
public final class ClientCommandManager
{
    private static final String ROOT = ".client";
    private static final List<String> ACTION_TOKENS = Arrays.asList(new String[] {"enable", "disable", "toggle", "status", "bind"});
    private static final List<String> BIND_TOKENS = buildBindTokens();
    private final ModuleManager modules;

    public ClientCommandManager(ModuleManager modules)
    {
        this.modules = modules;
    }

    /**
     * 执行本地命令；返回 false 表示并非本地命令，调用方可继续走原版流程。
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

        String firstArg = input.args.get(0);

        if (equalsIgnoreCase(firstArg, "status"))
        {
            this.printAllModuleStatus();
            return true;
        }

        Module module = this.resolveModule(firstArg);

        if (module == null)
        {
            this.chat("§c找不到模块: §f" + firstArg);
            this.chat("§7可用模块: §f" + join(this.collectModuleTokens(), ", "));
            return true;
        }

        Action action = Action.fromToken(input.args.size() >= 2 ? input.args.get(1) : "toggle");

        if (action == null)
        {
            this.chat("§c无效动作: §f" + input.args.get(1));
            this.chat("§7可选动作: §fenable, disable, toggle, status, bind");
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
     * 返回 null 表示不是本地命令，不拦截原版补全流程。
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

        if (tokens.size() == 2)
        {
            if (equalsIgnoreCase(tokens.get(1), "status"))
            {
                return Collections.emptyList();
            }

            return new ArrayList<String>(ACTION_TOKENS);
        }

        if (equalsIgnoreCase(tokens.get(1), "status"))
        {
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

    private void applyAction(Module module, Action action)
    {
        switch (action)
        {
            case ENABLE:
                module.setEnabled(true);
                this.chat("§a已启用 §f" + module.getName());
                return;

            case DISABLE:
                module.setEnabled(false);
                this.chat("§e已禁用 §f" + module.getName());
                return;

            case TOGGLE:
                module.toggle();
                this.chat("§b" + module.getName() + " §7=> §f" + (module.isEnabled() ? "开启" : "关闭"));
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
            this.chat("§7当前绑定: §f" + module.getBind().getDisplayName());
            this.chat("§7用法: §f.client " + normalize(module.getId()) + " bind <key|none>");
            return;
        }

        Integer keyCode = parseKeyCodeToken(args.get(2));

        if (keyCode == null)
        {
            this.chat("§c无法识别按键: §f" + args.get(2));
            this.chat("§7示例: §f.client " + normalize(module.getId()) + " bind rshift");
            this.chat("§7解除绑定: §f.client " + normalize(module.getId()) + " bind none");
            return;
        }

        module.getBind().setKeyCode(keyCode.intValue());
        this.chat("§a" + module.getName() + " §7绑定为 §f" + module.getBind().getDisplayName());
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
        return null;
    }

    private List<String> collectSecondArgCandidates()
    {
        List<String> candidates = new ArrayList<String>(this.collectModuleTokens());
        candidates.add("status");
        return candidates;
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
        }

        return out;
    }

    private void printUsage()
    {
        this.chat("§7用法: §f.client <module> [enable|disable|toggle|status|bind]");
        this.chat("§7示例: §f.client eagle");
        this.chat("§7示例: §f.client eagle bind rshift");
        this.chat("§7示例: §f.client eagle bind none");
    }

    private void printAllModuleStatus()
    {
        List<Module> all = this.modules.getAll();
        this.chat("§6[Client] 模块总览:");

        for (int i = 0; i < all.size(); ++i)
        {
            Module module = all.get(i);
            this.chat(" §7- §f" + module.getName() + " §8(" + module.getId() + ") §7[" + (module.isEnabled() ? "§aON§7" : "§cOFF§7") + "] §8[§f" + module.getBind().getDisplayName() + "§8]");
        }
    }

    private void printModuleStatus(Module module)
    {
        this.chat("§6[Client] §f" + module.getName() + " §7[" + (module.isEnabled() ? "§aON§7" : "§cOFF§7") + "]");
        this.chat(" §7id: §f" + module.getId());
        this.chat(" §7category: §f" + module.getCategory().name());
        this.chat(" §7bind: §f" + module.getBind().getDisplayName());

        List<Setting<?>> settings = module.getSettings();

        if (settings.isEmpty())
        {
            this.chat(" §8(无设置项)");
            return;
        }

        for (int i = 0; i < settings.size(); ++i)
        {
            Setting<?> setting = settings.get(i);
            this.chat(" §7" + setting.getName() + " §8(" + setting.getKey() + ") §7= §f" + this.settingValue(setting));
        }
    }

    private String settingValue(Setting<?> setting)
    {
        if (setting instanceof ColorSetting)
        {
            ColorValue c = ((ColorSetting)setting).get();
            return "rgba(" + c.getR() + "," + c.getG() + "," + c.getB() + "," + c.getA() + "), rainbow=" + c.isRainbow();
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

        Object value = setting.get();
        return value == null ? "null" : String.valueOf(value);
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
            String name = Keyboard.getKeyName(key);

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
            if (normalize(Keyboard.getKeyName(key)).equals(normalizedKeyName))
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
