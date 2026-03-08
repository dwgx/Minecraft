package irc.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import irc.server.service.IRCServiceDispatcher;

/**
 * Server-side handler for a single IRC client connection.
 * Implements core IRC commands: NICK, USER, JOIN, PART, PRIVMSG, NOTICE, QUIT, PING, PONG, TOPIC, NAMES, WHO, LIST.
 */
public final class IRCServerHandler extends SimpleChannelInboundHandler<String>
{
    private final IRCServer server;
    private Channel channel;
    private String nick;
    private String username;
    private String realName;
    private boolean registered;
    private final Set<String> joinedChannels = new HashSet<String>();

    public IRCServerHandler(IRCServer server) { this.server = server; }

    public String getNick() { return this.nick; }
    public String getPrefix() { return this.nick + "!" + this.username + "@localhost"; }

    public void sendLine(String line)
    {
        if (this.channel != null && this.channel.isActive())
        {
            this.channel.writeAndFlush(line + "\r\n");
        }
    }

    public void disconnect()
    {
        if (this.channel != null) this.channel.close();
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx)
    {
        this.channel = ctx.channel();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String line)
    {
        if (line == null || line.isEmpty()) return;
        IRCMessage msg = IRCMessage.parse(line);
        if (msg == null) return;

        String cmd = msg.getCommand();

        if ("NICK".equals(cmd)) { handleNick(msg); return; }
        if ("USER".equals(cmd)) { handleUser(msg); return; }
        if ("PING".equals(cmd)) { sendLine(":" + sn() + " PONG " + sn() + " :" + trail(msg)); return; }
        if ("PONG".equals(cmd)) { return; }
        if ("QUIT".equals(cmd)) { handleQuit(msg); return; }

        if (!this.registered) { sendNumeric(451, ":You have not registered"); return; }

        if ("JOIN".equals(cmd)) { handleJoin(msg); }
        else if ("PART".equals(cmd)) { handlePart(msg); }
        else if ("PRIVMSG".equals(cmd)) { handlePrivmsg(msg); }
        else if ("NOTICE".equals(cmd)) { handleNotice(msg); }
        else if ("TOPIC".equals(cmd)) { handleTopic(msg); }
        else if ("NAMES".equals(cmd)) { handleNames(msg); }
        else if ("WHO".equals(cmd)) { handleWho(msg); }
        else if ("LIST".equals(cmd)) { handleList(); }
        else if ("MODE".equals(cmd)) { handleMode(msg); }
        else if ("WHOIS".equals(cmd)) { handleWhois(msg); }
        else { sendNumeric(421, cmd + " :Unknown command"); }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
    {
        if (this.nick != null)
        {
            String quitLine = ":" + getPrefix() + " QUIT :Connection closed";
            for (String chName : this.joinedChannels)
            {
                IRCServerChannel ch = this.server.getChannel(chName);
                if (ch != null) { ch.removeMember(this.nick); }
                this.server.broadcastToChannel(chName, quitLine, this);
            }
            this.server.unregisterClient(this);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        ctx.close();
    }
/* APPEND_HANDLER_COMMANDS */
    // --- Command handlers ---

    private void handleNick(IRCMessage msg)
    {
        String newNick = msg.getTrailing() != null ? msg.getTrailing()
                : (msg.getParams().length > 0 ? msg.getParams()[0] : null);
        if (newNick == null || newNick.isEmpty()) { sendNumeric(431, ":No nickname given"); return; }

        if (this.server.isNickInUse(newNick) && !newNick.equalsIgnoreCase(this.nick))
        {
            sendNumeric(433, newNick + " :Nickname is already in use");
            return;
        }

        String oldNick = this.nick;
        this.nick = newNick;

        if (oldNick != null)
        {
            this.server.renameClient(oldNick, newNick, this);
            String nickLine = ":" + oldNick + "!" + this.username + "@localhost NICK :" + newNick;
            sendLine(nickLine);
            for (String chName : this.joinedChannels)
            {
                this.server.broadcastToChannel(chName, nickLine, this);
            }
        }

        tryRegister();
    }

    private void handleUser(IRCMessage msg)
    {
        if (this.registered) { sendNumeric(462, ":You may not reregister"); return; }
        this.username = msg.getParams().length > 0 ? msg.getParams()[0] : "user";
        this.realName = msg.getTrailing() != null ? msg.getTrailing() : this.username;
        tryRegister();
    }

    private void tryRegister()
    {
        if (this.registered || this.nick == null || this.username == null) return;
        this.registered = true;
        this.server.registerClient(this);
        this.server.getUserStore().ensureUser(this.nick);

        sendNumeric(1, ":Welcome to " + sn() + " " + this.nick);
        sendNumeric(2, ":Your host is " + sn() + ", running MinecraftIRC 1.0");
        sendNumeric(3, ":This server was created today");
        sendNumeric(4, sn() + " MinecraftIRC-1.0 o o");
        sendNumeric(375, ":- " + sn() + " Message of the Day -");
        sendNumeric(372, ":- Welcome to the Minecraft IRC server!");
        sendNumeric(376, ":End of /MOTD command.");

        // Auto-join saved channels from JSON + DB (deduplicated).
        for (String ch : getSavedChannels())
        {
            doJoin(ch);
        }
    }
/* APPEND_HANDLER_JOIN */
    private void handleJoin(IRCMessage msg)
    {
        String target = msg.getParams().length > 0 ? msg.getParams()[0]
                : (msg.getTrailing() != null ? msg.getTrailing() : null);
        if (target == null) { sendNumeric(461, "JOIN :Not enough parameters"); return; }

        for (String chName : target.split(","))
        {
            chName = chName.trim();
            if (!chName.startsWith("#")) chName = "#" + chName;
            doJoin(chName);
        }
    }

    private void doJoin(String chName)
    {
        IRCServerChannel ch = this.server.getOrCreateChannel(chName);
        if (ch.hasMember(this.nick)) return;

        ch.addMember(this.nick);
        this.joinedChannels.add(chName.toLowerCase());
        this.server.getUserStore().addChannel(this.nick, chName);
        persistChannelMembership(chName, true);

        String joinLine = ":" + getPrefix() + " JOIN " + chName;
        this.server.broadcastToChannel(chName, joinLine, null);

        if (!ch.getTopic().isEmpty())
        {
            sendNumeric(332, chName + " :" + ch.getTopic());
        }
        else
        {
            sendNumeric(331, chName + " :No topic is set");
        }

        sendNumeric(353, "= " + chName + " :" + ch.getNamesString());
        sendNumeric(366, chName + " :End of /NAMES list.");
    }

    private void handlePart(IRCMessage msg)
    {
        String target = msg.getParams().length > 0 ? msg.getParams()[0] : null;
        if (target == null) { sendNumeric(461, "PART :Not enough parameters"); return; }
        String reason = msg.getTrailing() != null ? msg.getTrailing() : "";

        for (String chName : target.split(","))
        {
            chName = chName.trim();
            IRCServerChannel ch = this.server.getChannel(chName);
            if (ch == null || !ch.hasMember(this.nick)) { sendNumeric(442, chName + " :You're not on that channel"); continue; }

            String partLine = ":" + getPrefix() + " PART " + chName + (reason.isEmpty() ? "" : " :" + reason);
            this.server.broadcastToChannel(chName, partLine, null);
            ch.removeMember(this.nick);
            this.joinedChannels.remove(chName.toLowerCase());
            this.server.getUserStore().removeChannel(this.nick, chName);
            persistChannelMembership(chName, false);
        }
    }
/* APPEND_HANDLER_MSG */
    private void handlePrivmsg(IRCMessage msg)
    {
        String target = msg.getParams().length > 0 ? msg.getParams()[0] : null;
        String text = msg.getTrailing();
        if (target == null || text == null) { sendNumeric(411, ":No recipient given"); return; }

        // Route service commands to dispatcher
        if (IRCServiceDispatcher.SERVICE_NICK.equals(target))
        {
            IRCServiceDispatcher dispatcher = this.server.getServiceDispatcher();
            if (dispatcher != null)
            {
                dispatcher.dispatch(this, text);
            }
            else
            {
                sendLine(":" + sn() + " NOTICE " + this.nick + " :Service not available (no database)");
            }
            return;
        }

        String line = ":" + getPrefix() + " PRIVMSG " + target + " :" + text;

        if (target.startsWith("#"))
        {
            IRCServerChannel ch = this.server.getChannel(target);
            if (ch == null || !ch.hasMember(this.nick)) { sendNumeric(404, target + " :Cannot send to channel"); return; }
            this.server.broadcastToChannel(target, line, this);
            persistMessage(target, null, text);
        }
        else
        {
            IRCServerHandler recipient = this.server.getClient(target);
            if (recipient == null) { sendNumeric(401, target + " :No such nick"); return; }
            recipient.sendLine(line);
            persistMessage(null, target, text);
        }
    }

    /** Persist message to DB asynchronously (best-effort, don't block send). */
    private void persistMessage(final String channel, final String toNick, final String content)
    {
        IRCServiceDispatcher dispatcher = this.server.getServiceDispatcher();
        if (dispatcher == null) return;
        if (channel != null)
        {
            dispatcher.persistChannelMessageAsync(this.nick, channel, content);
        }
        else if (toNick != null)
        {
            dispatcher.persistPrivateMessageAsync(this.nick, toNick, content);
        }
    }

    private void handleNotice(IRCMessage msg)
    {
        String target = msg.getParams().length > 0 ? msg.getParams()[0] : null;
        String text = msg.getTrailing();
        if (target == null || text == null) return;

        String line = ":" + getPrefix() + " NOTICE " + target + " :" + text;
        if (target.startsWith("#"))
        {
            this.server.broadcastToChannel(target, line, this);
        }
        else
        {
            IRCServerHandler recipient = this.server.getClient(target);
            if (recipient != null) recipient.sendLine(line);
        }
    }

    private void handleTopic(IRCMessage msg)
    {
        String chName = msg.getParams().length > 0 ? msg.getParams()[0] : null;
        if (chName == null) return;
        IRCServerChannel ch = this.server.getChannel(chName);
        if (ch == null) { sendNumeric(403, chName + " :No such channel"); return; }

        if (msg.getTrailing() != null)
        {
            ch.setTopic(msg.getTrailing());
            this.server.broadcastToChannel(chName, ":" + getPrefix() + " TOPIC " + chName + " :" + msg.getTrailing(), null);
        }
        else
        {
            if (ch.getTopic().isEmpty()) sendNumeric(331, chName + " :No topic is set");
            else sendNumeric(332, chName + " :" + ch.getTopic());
        }
    }
/* APPEND_HANDLER_MISC */
    private void handleNames(IRCMessage msg)
    {
        String chName = msg.getParams().length > 0 ? msg.getParams()[0] : null;
        if (chName == null) return;
        IRCServerChannel ch = this.server.getChannel(chName);
        if (ch != null)
        {
            sendNumeric(353, "= " + chName + " :" + ch.getNamesString());
        }
        sendNumeric(366, chName + " :End of /NAMES list.");
    }

    private void handleWho(IRCMessage msg)
    {
        String target = msg.getParams().length > 0 ? msg.getParams()[0] : "*";
        sendNumeric(315, target + " :End of /WHO list.");
    }

    private void handleList()
    {
        sendNumeric(321, "Channel :Users  Name");
        for (IRCServerChannel ch : this.server.getAllChannels())
        {
            sendNumeric(322, ch.getName() + " " + ch.getMemberCount() + " :" + ch.getTopic());
        }
        sendNumeric(323, ":End of /LIST");
    }

    private void handleMode(IRCMessage msg)
    {
        String target = msg.getParams().length > 0 ? msg.getParams()[0] : null;
        if (target == null) return;
        sendNumeric(324, target + " +");
    }

    private void handleWhois(IRCMessage msg)
    {
        String target = msg.getParams().length > 0 ? msg.getParams()[0] : null;
        if (target == null) return;
        IRCServerHandler client = this.server.getClient(target);
        if (client == null) { sendNumeric(401, target + " :No such nick"); return; }
        sendNumeric(311, target + " " + client.username + " localhost * :" + client.realName);
        sendNumeric(318, target + " :End of /WHOIS list.");
    }

    private void handleQuit(IRCMessage msg)
    {
        String reason = msg.getTrailing() != null ? msg.getTrailing() : "Quit";
        String quitLine = ":" + getPrefix() + " QUIT :" + reason;
        for (String chName : this.joinedChannels)
        {
            IRCServerChannel ch = this.server.getChannel(chName);
            if (ch != null) ch.removeMember(this.nick);
            this.server.broadcastToChannel(chName, quitLine, this);
        }
        this.server.unregisterClient(this);
        sendLine("ERROR :Closing Link: " + this.nick + " (" + reason + ")");
        disconnect();
    }

    // --- Helpers ---

    private String sn() { return this.server.getServerName(); }
    private String trail(IRCMessage msg) { return msg.getTrailing() != null ? msg.getTrailing() : ""; }

    private Set<String> getSavedChannels()
    {
        Set<String> saved = new LinkedHashSet<String>();
        saved.addAll(this.server.getUserStore().getChannels(this.nick));
        IRCServiceDispatcher dispatcher = this.server.getServiceDispatcher();
        if (dispatcher != null)
        {
            List<String> dbChannels = dispatcher.getUserChannels(this.nick);
            if (dbChannels != null) saved.addAll(dbChannels);
        }
        return saved;
    }

    private void persistChannelMembership(String chName, boolean joined)
    {
        IRCServiceDispatcher dispatcher = this.server.getServiceDispatcher();
        if (dispatcher == null) return;
        if (joined) dispatcher.persistChannelJoinAsync(chName, this.nick);
        else dispatcher.persistChannelPartAsync(chName, this.nick);
    }

    private void sendNumeric(int code, String text)
    {
        String target = this.nick != null ? this.nick : "*";
        sendLine(":" + sn() + " " + String.format("%03d", code) + " " + target + " " + text);
    }
}
