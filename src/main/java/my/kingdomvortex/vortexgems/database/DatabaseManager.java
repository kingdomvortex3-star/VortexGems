
package my.kingdomvortex.vortexgems.database;

import my.kingdomvortex.vortexgems.VortexGems;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {

    private final VortexGems plugin;
    private Connection connection;

    public DatabaseManager(VortexGems plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        try {
            File folder = plugin.getDataFolder();

            if (!folder.exists() && !folder.mkdirs()) {
                throw new SQLException("Could not create plugin directory.");
            }

            File database = new File(folder, "data.db");

            Class.forName("org.sqlite.JDBC");

            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + database.getAbsolutePath()
            );

            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA journal_mode = WAL");
                statement.execute("PRAGMA busy_timeout = 5000");

                statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_gems (
                        uuid TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        balance INTEGER NOT NULL DEFAULT 0,
                        total_earned INTEGER NOT NULL DEFAULT 0,
                        total_spent INTEGER NOT NULL DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);

                statement.execute("""
                    CREATE TABLE IF NOT EXISTS gems_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        type TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        description TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);

                statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_player_gems_balance
                    ON player_gems(balance DESC)
                """);

                statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_gems_transactions_uuid
                    ON gems_transactions(uuid)
                """);
            }

            plugin.getLogger().info("SQLite database connected.");
            return true;

        } catch (Exception exception) {
            plugin.getLogger().severe(
                    "Database connection failed: " + exception.getMessage()
            );
            return false;
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }

        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database is not connected.");
        }

        return connection;
    }

    public synchronized boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException exception) {
            return false;
        }
    }

    public synchronized void close() {
        if (connection == null) {
            return;
        }

        try {
            if (!connection.isClosed()) {
                connection.close();
            }

            plugin.getLogger().info("SQLite database disconnected.");

        } catch (SQLException exception) {
            plugin.getLogger().warning(
                    "Database close failed: " + exception.getMessage()
            );
        }
    }
}
