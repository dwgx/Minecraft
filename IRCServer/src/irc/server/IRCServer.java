package irc.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import irc.server.db.DbConfig;
import irc.server.db.MariaDbPool;
import irc.server.db.SchemaBootstrap;
import irc.server.service.IRCServiceDispatcher;

/**
 * Standalone IRC server using Netty.
 * Default port: 1378. User data persisted via IRCUserStore (JSON).
 *
 * Usage: java -cp "lib/*" irc.server.IRCServer [port]
 */
public final class IRCServer
{
    public static final int DEFAULT_PORT = 1378;
    private static final String SERVER_NAME = "MinecraftIRC";

    private final int port;
    private final IRCUserStore userStore;
    private final Map<String, IRCServerHandler> clients = new ConcurrentHashMap<String, IRCServerHandler>();
    private final Map<String, IRCServerChannel> channels = new ConcurrentHashMap<String, IRCServerChannel>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private MariaDbPool dbPool;
    private IRCServiceDispatcher serviceDispatcher;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public IRCServer(int port, IRCUserStore userStore)
    {
        this.port = port;
        this.userStore = userStore;
    }

    public IRCServer() { this(DEFAULT_PORT, new IRCUserStore()); }

    public IRCUserStore getUserStore() { return this.userStore; }
    public boolean isRunning() { return this.running.get(); }
    public String getServerName() { return SERVER_NAME; }
    public IRCServiceDispatcher getServiceDispatcher() { return this.serviceDispatcher; }

    /** Start the server. Blocks until bind completes. */
    public void start()
    {
        if (this.running.getAndSet(true)) return;
        this.userStore.load();

        // Initialize MariaDB
        try
        {
            DbConfig dbConfig = DbConfig.load("config/db.json");
            this.dbPool = new MariaDbPool(dbConfig);
            this.dbPool.init();
            new SchemaBootstrap(this.dbPool).ensureSchema();
            this.serviceDispatcher = new IRCServiceDispatcher(this, this.dbPool);
            System.out.println("[IRCServer] MariaDB connected, service dispatcher ready");
        }
        catch (Exception e)
        {
            System.err.println("[IRCServer] MariaDB init failed (running without DB): " + e.getMessage());
            this.dbPool = null;
            this.serviceDispatcher = null;
        }

        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup(2);

        final IRCServer self = this;
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(this.bossGroup, this.workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>()
                {
                    @Override
                    protected void initChannel(SocketChannel ch)
                    {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast("framer", new LineBasedFrameDecoder(4096));
                        p.addLast("decoder", new StringDecoder(StandardCharsets.UTF_8));
                        p.addLast("encoder", new StringEncoder(StandardCharsets.UTF_8));
                        p.addLast("handler", new IRCServerHandler(self));
                    }
                });

        try
        {
            this.serverChannel = bootstrap.bind(this.port).sync().channel();
            System.out.println("[IRCServer] Listening on port " + this.port);
        }
        catch (Exception e)
        {
            System.err.println("[IRCServer] Failed to bind port " + this.port + ": " + e);
            this.running.set(false);
        }
    }

    /** Stop the server gracefully. */
    public void stop()
    {
        if (!this.running.getAndSet(false)) return;
        System.out.println("[IRCServer] Shutting down...");
        this.userStore.save();

        for (IRCServerHandler client : this.clients.values())
        {
            client.sendLine(":" + SERVER_NAME + " ERROR :Server shutting down");
            client.disconnect();
        }
        this.clients.clear();

        if (this.serverChannel != null) { this.serverChannel.close(); this.serverChannel = null; }
        if (this.workerGroup != null) { this.workerGroup.shutdownGracefully(); this.workerGroup = null; }
        if (this.bossGroup != null) { this.bossGroup.shutdownGracefully(); this.bossGroup = null; }
        if (this.serviceDispatcher != null) { this.serviceDispatcher.shutdown(); this.serviceDispatcher = null; }
        if (this.dbPool != null) { this.dbPool.close(); this.dbPool = null; }
        System.out.println("[IRCServer] Stopped.");
    }
/* APPEND_SERVER_MGMT */
    // --- Client management ---

    void registerClient(IRCServerHandler handler)
    {
        String nick = handler.getNick();
        if (nick != null) this.clients.put(nick.toLowerCase(), handler);
    }

    void unregisterClient(IRCServerHandler handler)
    {
        String nick = handler.getNick();
        if (nick != null) this.clients.remove(nick.toLowerCase());
    }

    public IRCServerHandler getClient(String nick)
    {
        return nick != null ? this.clients.get(nick.toLowerCase()) : null;
    }

    boolean isNickInUse(String nick)
    {
        return nick != null && this.clients.containsKey(nick.toLowerCase());
    }

    void renameClient(String oldNick, String newNick, IRCServerHandler handler)
    {
        if (oldNick != null) this.clients.remove(oldNick.toLowerCase());
        if (newNick != null) this.clients.put(newNick.toLowerCase(), handler);
    }

    // --- Channel management ---

    IRCServerChannel getOrCreateChannel(String name)
    {
        String key = name.toLowerCase();
        IRCServerChannel ch = this.channels.get(key);
        if (ch == null)
        {
            ch = new IRCServerChannel(name);
            this.channels.put(key, ch);
        }
        return ch;
    }

    IRCServerChannel getChannel(String name)
    {
        return name != null ? this.channels.get(name.toLowerCase()) : null;
    }

    Collection<IRCServerChannel> getAllChannels()
    {
        return this.channels.values();
    }

    // --- Broadcast ---

    void broadcastToChannel(String channelName, String line, IRCServerHandler exclude)
    {
        IRCServerChannel ch = getChannel(channelName);
        if (ch == null) return;
        for (String memberNick : ch.getMembers())
        {
            IRCServerHandler client = getClient(memberNick);
            if (client != null && client != exclude)
            {
                client.sendLine(line);
            }
        }
    }

    /** Standalone entry point. */
    public static void main(String[] args)
    {
        int port = DEFAULT_PORT;
        if (args.length > 0)
        {
            try { port = Integer.parseInt(args[0]); }
            catch (NumberFormatException ignored) {}
        }
        final IRCServer server = new IRCServer(port, new IRCUserStore());
        server.start();

        if (!server.isRunning())
        {
            System.err.println("[IRCServer] Failed to start. Exiting.");
            System.exit(1);
        }

        System.out.println("[IRCServer] Press Ctrl+C to stop.");
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
        {
            @Override public void run() { server.stop(); }
        }, "IRCServer-Shutdown"));
    }
}
