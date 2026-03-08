package client.chat.cache;

import java.io.File;
import java.sql.*;

/**
 * SQLite local cache for IRC data. Provides offline access to messages,
 * friends, mail, and profiles.
 */
public final class IRCLocalCache
{
    private static final String DEFAULT_PATH = "config/client/irc_cache.db";

    private Connection connection;
    private final String dbPath;

    public IRCLocalCache() { this(DEFAULT_PATH); }
    public IRCLocalCache(String path) { this.dbPath = path; }

    public void open()
    {
        try
        {
            Class.forName("org.sqlite.JDBC");
            File parent = new File(this.dbPath).getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.dbPath);
            applyPragma("PRAGMA journal_mode=WAL");
            applyPragma("PRAGMA synchronous=NORMAL");
            applyPragma("PRAGMA busy_timeout=5000");
            createTables();
            System.out.println("[IRCLocalCache] Opened: " + this.dbPath);
        }
        catch (Exception e)
        {
            System.err.println("[IRCLocalCache] Failed to open: " + e.getMessage());
            this.connection = null;
        }
    }

    public void close()
    {
        if (this.connection != null)
        {
            try { this.connection.close(); }
            catch (SQLException ignored) {}
            this.connection = null;
            System.out.println("[IRCLocalCache] Closed");
        }
    }

    public Connection getConnection() { return this.connection; }
    public boolean isOpen() { return this.connection != null; }

    private void applyPragma(String sql) throws SQLException
    {
        if (this.connection == null || sql == null || sql.isEmpty()) return;
        Statement st = this.connection.createStatement();
        try
        {
            st.execute(sql);
        }
        finally
        {
            st.close();
        }
    }

    private void createTables()
    {
        if (this.connection == null) return;
        try
        {
            Statement st = this.connection.createStatement();
            try
            {
                st.execute("CREATE TABLE IF NOT EXISTS messages ("
                        + "id TEXT PRIMARY KEY,"
                        + "sender_id TEXT NOT NULL,"
                        + "conversation_id TEXT NOT NULL,"
                        + "type TEXT DEFAULT 'TEXT',"
                        + "text_content TEXT,"
                        + "timestamp_ms INTEGER NOT NULL"
                        + ")");
                st.execute("CREATE INDEX IF NOT EXISTS idx_msg_conv ON messages(conversation_id, timestamp_ms)");

                st.execute("CREATE TABLE IF NOT EXISTS friends ("
                        + "nick TEXT PRIMARY KEY,"
                        + "display_name TEXT,"
                        + "status TEXT DEFAULT 'accepted',"
                        + "updated_ms INTEGER"
                        + ")");

                st.execute("CREATE TABLE IF NOT EXISTS mail ("
                        + "id INTEGER PRIMARY KEY,"
                        + "other_nick TEXT NOT NULL,"
                        + "subject TEXT,"
                        + "body TEXT,"
                        + "is_inbox INTEGER DEFAULT 1,"
                        + "is_read INTEGER DEFAULT 0,"
                        + "created_ms INTEGER"
                        + ")");

                st.execute("CREATE TABLE IF NOT EXISTS profiles ("
                        + "nick TEXT PRIMARY KEY,"
                        + "json_data TEXT,"
                        + "cached_ms INTEGER"
                        + ")");
            }
            finally { st.close(); }
        }
        catch (SQLException e)
        {
            System.err.println("[IRCLocalCache] Failed to create tables: " + e.getMessage());
        }
    }
}
