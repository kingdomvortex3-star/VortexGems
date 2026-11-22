
package my.kingdomvortex.vortexgems.manager;

import my.kingdomvortex.vortexgems.VortexGems;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LeaderboardManager {

    private final VortexGems plugin;

    public LeaderboardManager(VortexGems plugin) {
        this.plugin = plugin;
    }

    public List<Entry> getTop(int limit) {
        List<Entry> entries = new ArrayList<>();

        String sql = """
                SELECT uuid, name, balance
                FROM player_gems
                ORDER BY balance DESC, name ASC
                LIMIT ?
                """;

        try (PreparedStatement statement =
                     plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {

            statement.setInt(1, Math.max(1, limit));

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    entries.add(new Entry(
                            UUID.fromString(result.getString("uuid")),
                            result.getString("name"),
                            result.getLong("balance")
                    ));
                }
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Leaderboard error: " + exception.getMessage()
            );
        }

        return entries;
    }

    public int getRank(UUID uuid) {
        String sql = """
                SELECT COUNT(*) + 1
                FROM player_gems
                WHERE balance > (
                    SELECT balance
                    FROM player_gems
                    WHERE uuid = ?
                )
                """;

        try (PreparedStatement statement =
                     plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {

            statement.setString(1, uuid.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Rank lookup error: " + exception.getMessage()
            );
        }

        return -1;
    }

    public Entry getEntry(UUID uuid) {
        String sql = """
                SELECT uuid, name, balance
                FROM player_gems
                WHERE uuid = ?
                """;

        try (PreparedStatement statement =
                     plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {

            statement.setString(1, uuid.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new Entry(
                            UUID.fromString(result.getString("uuid")),
                            result.getString("name"),
                            result.getLong("balance")
                    );
                }
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Player leaderboard lookup error: " + exception.getMessage()
            );
        }

        return null;
    }

    public static final class Entry {

        private final UUID uuid;
        private final String name;
        private final long balance;

        public Entry(UUID uuid, String name, long balance) {
            this.uuid = uuid;
            this.name = name;
            this.balance = balance;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public long getBalance() {
            return balance;
        }
    }
}
