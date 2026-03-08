package dwgx.IRCBackend;

/**
 * Parsed IRC protocol message (RFC 2812).
 * Format: [:prefix] command [params...] [:trailing]
 */
public final class IRCMessage
{
    private final String prefix;
    private final String command;
    private final String[] params;
    private final String trailing;
    private final String raw;

    public IRCMessage(String prefix, String command, String[] params, String trailing, String raw)
    {
        this.prefix = prefix;
        this.command = command;
        this.params = params != null ? params : new String[0];
        this.trailing = trailing;
        this.raw = raw;
    }

    public String getPrefix() { return this.prefix; }
    public String getCommand() { return this.command; }
    public String[] getParams() { return this.params; }
    public String getTrailing() { return this.trailing; }
    public String getRaw() { return this.raw; }

    /** Extract nickname from prefix (nick!user@host → nick). */
    public String getNick()
    {
        if (this.prefix == null) return null;
        int bang = this.prefix.indexOf('!');
        return bang > 0 ? this.prefix.substring(0, bang) : this.prefix;
    }

    /** Parse a raw IRC line into an IRCMessage. */
    public static IRCMessage parse(String line)
    {
        if (line == null || line.isEmpty()) return null;
        String raw = line;
        String prefix = null;
        int idx = 0;

        if (line.charAt(0) == ':')
        {
            int space = line.indexOf(' ', 1);
            if (space < 0) return null;
            prefix = line.substring(1, space);
            idx = space + 1;
        }

        String trailing = null;
        int trailIdx = line.indexOf(" :", idx);
        String paramPart;
        if (trailIdx >= 0)
        {
            paramPart = line.substring(idx, trailIdx);
            trailing = line.substring(trailIdx + 2);
        }
        else
        {
            paramPart = line.substring(idx);
        }

        String[] tokens = paramPart.split(" ");
        if (tokens.length == 0) return null;
        String command = tokens[0].toUpperCase();
        String[] params = new String[tokens.length - 1];
        System.arraycopy(tokens, 1, params, 0, params.length);

        return new IRCMessage(prefix, command, params, trailing, raw);
    }
}
