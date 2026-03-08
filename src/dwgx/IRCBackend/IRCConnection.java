package dwgx.IRCBackend;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IRC network connection using Netty (already in lib/).
 * Handles connect/disconnect, message parsing, auto-reconnect, and PING/PONG.
 * Thread-safe: all public methods can be called from any thread.
 */
public final class IRCConnection
{
    private final IRCConfig config;
    private final List<IRCEventListener> listeners = new CopyOnWriteArrayList<IRCEventListener>();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicBoolean shouldReconnect = new AtomicBoolean(true);
    private final AtomicInteger reconnectCount = new AtomicInteger(0);

    private EventLoopGroup group;
    private Channel channel;
    private String currentNick;
    private java.util.concurrent.ScheduledFuture<?> pingTask;

    public IRCConnection(IRCConfig config)
    {
        this.config = config;
        this.currentNick = config.getNickname();
    }

    public void addListener(IRCEventListener listener) { this.listeners.add(listener); }
    public void removeListener(IRCEventListener listener) { this.listeners.remove(listener); }
    public boolean isConnected() { return this.connected.get(); }
    public boolean isConnecting() { return this.connecting.get(); }
    public String getCurrentNick() { return this.currentNick; }

    /** Connect to the IRC server asynchronously. */
    public void connect()
    {
        if (this.connected.get() || !this.connecting.compareAndSet(false, true)) return;
        this.shouldReconnect.set(true);
        this.reconnectCount.set(0);
        System.out.println("[IRCConnection] Connecting to " + this.config.getServerHost() + ":" + this.config.getServerPort() + " as " + this.config.getNickname());
        doConnect();
    }

    private void doConnect()
    {
        this.group = new NioEventLoopGroup(1);
        final SslContext sslCtx;
        if (this.config.isUseTls())
        {
            try
            {
                sslCtx = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
            }
            catch (Exception e)
            {
                this.connecting.set(false);
                fireError("TLS init failed: " + e.getMessage());
                return;
            }
        }
        else
        {
            sslCtx = null;
        }

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(this.group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.config.getTimeoutMs())
                .handler(new ChannelInitializer<SocketChannel>()
                {
                    @Override
                    protected void initChannel(SocketChannel ch)
                    {
                        ChannelPipeline p = ch.pipeline();
                        if (sslCtx != null)
                        {
                            p.addLast("ssl", sslCtx.newHandler(ch.alloc(),
                                    config.getServerHost(), config.getServerPort()));
                        }
                        p.addLast("framer", new LineBasedFrameDecoder(4096));
                        p.addLast("decoder", new StringDecoder(StandardCharsets.UTF_8));
                        p.addLast("encoder", new StringEncoder(StandardCharsets.UTF_8));
                        p.addLast("handler", new IRCChannelHandler());
                    }
                });

        bootstrap.connect(this.config.getServerHost(), this.config.getServerPort())
                .addListener(new ChannelFutureListener()
                {
                    @Override
                    public void operationComplete(ChannelFuture future)
                    {
                        connecting.set(false);
                        if (future.isSuccess())
                        {
                            channel = future.channel();
                            connected.set(true);
                            reconnectCount.set(0);
                            System.out.println("[IRCConnection] TCP connected, registering...");
                            register();
                        }
                        else
                        {
                            connected.set(false);
                            String reason = future.cause() != null ? future.cause().getMessage() : "Unknown";
                            System.err.println("[IRCConnection] Connection failed: " + reason);
                            for (IRCEventListener l : listeners) l.onDisconnected("Connect failed: " + reason);
                            fireError("Cannot connect to " + config.getServerHost() + ":" + config.getServerPort() + " - " + reason);
                            scheduleReconnect();
                        }
                    }
                });
    }

    /** Disconnect gracefully. */
    public void disconnect()
    {
        this.shouldReconnect.set(false);
        sendRaw("QUIT :Leaving");
        closeChannel();
    }

    /** Send a raw IRC line. */
    public void sendRaw(String line)
    {
        if (this.channel != null && this.channel.isActive())
        {
            this.channel.writeAndFlush(line + "\r\n");
        }
    }

    /** Send PRIVMSG to a target (channel or nick). */
    public void sendMessage(String target, String text)
    {
        sendRaw("PRIVMSG " + target + " :" + text);
    }

    /** Join a channel. */
    public void joinChannel(String channel)
    {
        sendRaw("JOIN " + channel);
    }

    /** Part a channel. */
    public void partChannel(String channel, String reason)
    {
        sendRaw("PART " + channel + (reason != null ? " :" + reason : ""));
    }

    /** Change nickname. */
    public void changeNick(String newNick)
    {
        sendRaw("NICK " + newNick);
    }

    private void register()
    {
        if (this.config.getServerPassword() != null)
        {
            sendRaw("PASS " + this.config.getServerPassword());
        }
        sendRaw("NICK " + this.config.getNickname());
        sendRaw("USER " + this.config.getUsername() + " 0 * :" + this.config.getRealName());
        System.out.println("[IRCConnection] Sent NICK/USER registration");
    }

    private void handleLine(String line)
    {
        IRCMessage msg = IRCMessage.parse(line);
        if (msg == null) return;

        String cmd = msg.getCommand();

        // PING/PONG keepalive
        if ("PING".equals(cmd))
        {
            sendRaw("PONG :" + (msg.getTrailing() != null ? msg.getTrailing() : ""));
            return;
        }

        // Numeric replies
        try
        {
            int numeric = Integer.parseInt(cmd);
            if (numeric == 1) // RPL_WELCOME
            {
                this.currentNick = msg.getParams().length > 0 ? msg.getParams()[0] : this.config.getNickname();
                startPingKeepAlive();
                for (IRCEventListener l : this.listeners) l.onConnected();
                return;
            }
            if (numeric == 433) // ERR_NICKNAMEINUSE
            {
                this.currentNick = this.currentNick + "_";
                sendRaw("NICK " + this.currentNick);
                System.out.println("[IRCConnection] Nickname in use, trying: " + this.currentNick);
                return;
            }
            if (numeric >= 400 && numeric < 600)
            {
                fireError("Server error " + numeric + ": " + msg.getTrailing());
            }
            for (IRCEventListener l : this.listeners) l.onNumeric(numeric, msg.getParams(), msg.getTrailing());
            return;
        }
        catch (NumberFormatException ignored) {}

        // Command dispatch
        if ("PRIVMSG".equals(cmd))
        {
            String target = msg.getParams().length > 0 ? msg.getParams()[0] : "";
            for (IRCEventListener l : this.listeners) l.onMessage(msg.getNick(), target, msg.getTrailing());
        }
        else if ("NOTICE".equals(cmd))
        {
            String target = msg.getParams().length > 0 ? msg.getParams()[0] : "";
            for (IRCEventListener l : this.listeners) l.onNotice(msg.getNick(), target, msg.getTrailing());
        }
        else if ("JOIN".equals(cmd))
        {
            String ch = msg.getTrailing() != null ? msg.getTrailing()
                    : (msg.getParams().length > 0 ? msg.getParams()[0] : "");
            for (IRCEventListener l : this.listeners) l.onJoin(msg.getNick(), ch);
        }
        else if ("PART".equals(cmd))
        {
            String ch = msg.getParams().length > 0 ? msg.getParams()[0] : "";
            for (IRCEventListener l : this.listeners) l.onPart(msg.getNick(), ch, msg.getTrailing());
        }
        else if ("QUIT".equals(cmd))
        {
            for (IRCEventListener l : this.listeners) l.onQuit(msg.getNick(), msg.getTrailing());
        }
        else if ("NICK".equals(cmd))
        {
            String newNick = msg.getTrailing() != null ? msg.getTrailing()
                    : (msg.getParams().length > 0 ? msg.getParams()[0] : "");
            String oldNick = msg.getNick();
            if (oldNick != null && oldNick.equals(this.currentNick)) this.currentNick = newNick;
            for (IRCEventListener l : this.listeners) l.onNickChange(oldNick, newNick);
        }
        else if ("TOPIC".equals(cmd))
        {
            String ch = msg.getParams().length > 0 ? msg.getParams()[0] : "";
            for (IRCEventListener l : this.listeners) l.onTopic(ch, msg.getTrailing(), msg.getNick());
        }
        else if ("ERROR".equals(cmd))
        {
            fireError(msg.getTrailing());
        }
        else
        {
            for (IRCEventListener l : this.listeners) l.onRaw(msg);
        }
    }

    private void closeChannel()
    {
        stopPingKeepAlive();
        this.connected.set(false);
        this.connecting.set(false);
        if (this.channel != null)
        {
            this.channel.close();
            this.channel = null;
        }
        if (this.group != null)
        {
            this.group.shutdownGracefully();
            this.group = null;
        }
    }

    private void startPingKeepAlive()
    {
        stopPingKeepAlive();
        if (this.group != null && !this.group.isShuttingDown())
        {
            final long intervalMs = this.config.getPingIntervalMs();
            this.pingTask = this.group.next().scheduleAtFixedRate(new Runnable()
            {
                @Override
                public void run()
                {
                    if (connected.get()) sendRaw("PING :" + System.currentTimeMillis());
                }
            }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        }
    }

    private void stopPingKeepAlive()
    {
        if (this.pingTask != null)
        {
            this.pingTask.cancel(false);
            this.pingTask = null;
        }
    }

    private void scheduleReconnect()
    {
        if (!this.shouldReconnect.get()) return;
        int attempt = this.reconnectCount.incrementAndGet();
        if (attempt > this.config.getMaxReconnectAttempts())
        {
            fireError("IRC server offline (" + this.config.getServerHost() + ":" + this.config.getServerPort() + ")");
            return;
        }
        closeChannel();
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(config.getReconnectDelayMs());
                }
                catch (InterruptedException ignored) {}
                if (shouldReconnect.get() && !connected.get() && connecting.compareAndSet(false, true)) doConnect();
            }
        }, "IRC-Reconnect").start();
    }

    private void fireError(String message)
    {
        System.err.println("[IRCConnection] Error: " + message);
        for (IRCEventListener l : this.listeners) l.onError(message);
    }

    /** Netty channel handler for IRC line processing. */
    private class IRCChannelHandler extends SimpleChannelInboundHandler<String>
    {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String line) throws Exception
        {
            handleLine(line);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception
        {
            connected.set(false);
            connecting.set(false);
            for (IRCEventListener l : listeners) l.onDisconnected("Connection closed");
            scheduleReconnect();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
        {
            fireError("Network error: " + cause.getMessage());
            ctx.close();
        }
    }
}
