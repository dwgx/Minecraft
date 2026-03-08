package irc.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple JDBC connection pool for MariaDB.
 */
public final class MariaDbPool
{
    private final DbConfig config;
    private final BlockingQueue<Connection> pool;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public MariaDbPool(DbConfig config)
    {
        this.config = config;
        this.pool = new ArrayBlockingQueue<Connection>(config.getPoolSize());
    }

    public void init() throws SQLException
    {
        try { Class.forName("org.mariadb.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new SQLException("MariaDB driver not found", e); }

        // Auto-create database if it doesn't exist
        ensureDatabase();

        for (int i = 0; i < this.config.getPoolSize(); i++)
        {
            this.pool.offer(createConnection());
        }
        System.out.println("[MariaDbPool] Initialized " + this.config.getPoolSize() + " connections");
    }

    /**
     * Connect without specifying a database and CREATE DATABASE IF NOT EXISTS.
     */
    private void ensureDatabase() throws SQLException
    {
        String baseUrl = "jdbc:mariadb://" + this.config.getHost() + ":" + this.config.getPort()
                + "/?useUnicode=true&characterEncoding=UTF-8";
        Connection conn = DriverManager.getConnection(baseUrl,
                this.config.getUsername(), this.config.getPassword());
        try
        {
            java.sql.Statement st = conn.createStatement();
            try
            {
                st.execute("CREATE DATABASE IF NOT EXISTS `" + this.config.getDatabase()
                        + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                System.out.println("[MariaDbPool] Database '" + this.config.getDatabase() + "' ensured");
            }
            finally { st.close(); }
        }
        finally { conn.close(); }
    }

    public Connection getConnection() throws SQLException
    {
        if (this.closed.get()) throw new SQLException("Pool is closed");
        Connection conn = this.pool.poll();
        if (conn == null || conn.isClosed() || !conn.isValid(2))
        {
            if (conn != null) { try { conn.close(); } catch (SQLException ignored) {} }
            conn = createConnection();
        }
        return conn;
    }

    public void returnConnection(Connection conn)
    {
        if (conn == null) return;
        if (this.closed.get())
        {
            try { conn.close(); } catch (SQLException ignored) {}
            return;
        }
        try
        {
            if (!conn.isClosed() && conn.isValid(1))
            {
                if (!this.pool.offer(conn))
                {
                    conn.close();
                }
            }
            else
            {
                conn.close();
            }
        }
        catch (SQLException ignored) {}
    }
/* APPEND_POOL_CLOSE */
    public void close()
    {
        if (!this.closed.compareAndSet(false, true)) return;
        Connection conn;
        while ((conn = this.pool.poll()) != null)
        {
            try { conn.close(); } catch (SQLException ignored) {}
        }
        System.out.println("[MariaDbPool] Closed");
    }

    private Connection createConnection() throws SQLException
    {
        return DriverManager.getConnection(
                this.config.getJdbcUrl(),
                this.config.getUsername(),
                this.config.getPassword()
        );
    }
}
