package irc.server.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Auto-creates all required database tables on startup.
 */
public final class SchemaBootstrap
{
    private final MariaDbPool pool;

    public SchemaBootstrap(MariaDbPool pool) { this.pool = pool; }

    public void ensureSchema() throws SQLException
    {
        Connection conn = this.pool.getConnection();
        try
        {
            Statement st = conn.createStatement();
            try
            {
                st.execute("CREATE TABLE IF NOT EXISTS users ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                        + "nick VARCHAR(64) NOT NULL UNIQUE,"
                        + "display_nick VARCHAR(64),"
                        + "uid VARCHAR(16) NOT NULL UNIQUE,"
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                // Migrate: add uid column if table already exists without it
                try
                {
                    st.execute("ALTER TABLE users ADD COLUMN uid VARCHAR(16) NOT NULL UNIQUE AFTER display_nick");
                }
                catch (SQLException ignored) { /* column already exists */ }

                st.execute("CREATE TABLE IF NOT EXISTS friends ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                        + "user_id BIGINT NOT NULL,"
                        + "friend_id BIGINT NOT NULL,"
                        + "status ENUM('pending','accepted','rejected') DEFAULT 'pending',"
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                        + "UNIQUE KEY uk_pair (user_id, friend_id),"
                        + "FOREIGN KEY (user_id) REFERENCES users(id),"
                        + "FOREIGN KEY (friend_id) REFERENCES users(id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
/* APPEND_SCHEMA_TABLES */
                st.execute("CREATE TABLE IF NOT EXISTS profiles ("
                        + "user_id BIGINT PRIMARY KEY,"
                        + "bio TEXT,"
                        + "avatar_hash VARCHAR(128),"
                        + "theme_color VARCHAR(16) DEFAULT '#5B9BD5',"
                        + "game_stats TEXT,"
                        + "visitor_count INT DEFAULT 0,"
                        + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                        + "FOREIGN KEY (user_id) REFERENCES users(id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                st.execute("CREATE TABLE IF NOT EXISTS profile_visitors ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                        + "profile_user_id BIGINT NOT NULL,"
                        + "visitor_user_id BIGINT NOT NULL,"
                        + "visited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                        + "FOREIGN KEY (profile_user_id) REFERENCES users(id),"
                        + "FOREIGN KEY (visitor_user_id) REFERENCES users(id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                st.execute("CREATE TABLE IF NOT EXISTS mail ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                        + "from_user_id BIGINT NOT NULL,"
                        + "to_user_id BIGINT NOT NULL,"
                        + "subject VARCHAR(256) NOT NULL,"
                        + "body TEXT,"
                        + "is_read TINYINT(1) DEFAULT 0,"
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                        + "FOREIGN KEY (from_user_id) REFERENCES users(id),"
                        + "FOREIGN KEY (to_user_id) REFERENCES users(id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                st.execute("CREATE TABLE IF NOT EXISTS social_posts ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                        + "author_id BIGINT NOT NULL,"
                        + "content TEXT NOT NULL,"
                        + "image_url VARCHAR(512),"
                        + "like_count INT DEFAULT 0,"
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                        + "FOREIGN KEY (author_id) REFERENCES users(id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                st.execute("CREATE TABLE IF NOT EXISTS social_likes ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                        + "post_id BIGINT NOT NULL,"
                        + "user_id BIGINT NOT NULL,"
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                        + "UNIQUE KEY uk_like (post_id, user_id),"
                        + "FOREIGN KEY (post_id) REFERENCES social_posts(id) ON DELETE CASCADE,"
                        + "FOREIGN KEY (user_id) REFERENCES users(id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                st.execute("CREATE TABLE IF NOT EXISTS social_comments ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                        + "post_id BIGINT NOT NULL,"
                        + "author_id BIGINT NOT NULL,"
                        + "content TEXT NOT NULL,"
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                        + "FOREIGN KEY (post_id) REFERENCES social_posts(id) ON DELETE CASCADE,"
                        + "FOREIGN KEY (author_id) REFERENCES users(id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                st.execute("CREATE TABLE IF NOT EXISTS channels ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                        + "name VARCHAR(128) NOT NULL UNIQUE,"
                        + "topic TEXT,"
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                st.execute("CREATE TABLE IF NOT EXISTS channel_members ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                        + "channel_id BIGINT NOT NULL,"
                        + "user_id BIGINT NOT NULL,"
                        + "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                        + "UNIQUE KEY uk_member (channel_id, user_id),"
                        + "FOREIGN KEY (channel_id) REFERENCES channels(id),"
                        + "FOREIGN KEY (user_id) REFERENCES users(id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                st.execute("CREATE TABLE IF NOT EXISTS message_history ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                        + "from_user_id BIGINT NOT NULL,"
                        + "to_user_id BIGINT,"
                        + "channel_name VARCHAR(128),"
                        + "content TEXT NOT NULL,"
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                        + "INDEX idx_channel (channel_name, id),"
                        + "INDEX idx_private (from_user_id, to_user_id, id),"
                        + "FOREIGN KEY (from_user_id) REFERENCES users(id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                System.out.println("[SchemaBootstrap] All tables ensured");
            }
            finally { st.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }
}
