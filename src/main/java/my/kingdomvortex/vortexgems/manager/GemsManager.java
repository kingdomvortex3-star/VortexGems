
package my.kingdomvortex.vortexgems.manager;

import my.kingdomvortex.vortexgems.VortexGems;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class GemsManager {

    private final VortexGems plugin;

    public GemsManager(VortexGems plugin) {
        this.plugin = plugin;
    }

    public synchronized boolean createAccount(UUID uuid, String playerName) {
        String sql = """
                INSERT INTO player_gems (uuid, name, balance)
                VALUES (?, ?, 0)
                ON CONFLICT(uuid) DO UPDATE SET name = excluded.name
                """;

        try (PreparedStatement statement =
                     plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {

            statement.setString(1, uuid.toString());
            statement.setString(2, playerName);
            statement.executeUpdate();
            return true;

        } catch (SQLException exception) {
            logError(exception);
            return false;
        }
    }

    public long getBalance(UUID uuid) {
        String sql = "SELECT balance FROM player_gems WHERE uuid = ?";

        try (PreparedStatement statement =
                     plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {

            statement.setString(1, uuid.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getLong("balance");
                }
            }

        } catch (SQLException exception) {
            logError(exception);
        }

        return 0;
    }

    public synchronized boolean addBalance(
            UUID uuid,
            String playerName,
            long amount
    ) {
        if (amount <= 0) return false;

        String sql = """
                INSERT INTO player_gems
                    (uuid, name, balance, total_earned)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    name = excluded.name,
                    balance = balance + excluded.balance,
                    total_earned = total_earned + excluded.total_earned
                """;

        try (PreparedStatement statement =
                     plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {

            statement.setString(1, uuid.toString());
            statement.setString(2, playerName);
            statement.setLong(3, amount);
            statement.setLong(4, amount);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            logError(exception);
            return false;
        }
    }

    public synchronized boolean removeBalance(UUID uuid, long amount) {
        if (amount <= 0) return false;

        String sql = """
                UPDATE player_gems
                SET balance = balance - ?,
                    total_spent = total_spent + ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE uuid = ? AND balance >= ?
                """;

        try (PreparedStatement statement =
                     plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {

            statement.setLong(1, amount);
            statement.setLong(2, amount);
            statement.setString(3, uuid.toString());
            statement.setLong(4, amount);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            logError(exception);
            return false;
        }
    }

    public synchronized boolean setBalance(
            UUID uuid,
            String playerName,
            long amount
    ) {
        if (amount < 0) return false;

        String sql = """
                INSERT INTO player_gems (uuid, name, balance)
                VALUES (?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    name = excluded.name,
                    balance = excluded.balance,
                    updated_at = CURRENT_TIMESTAMP
                """;

        try (PreparedStatement statement =
                     plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {

            statement.setString(1, uuid.toString());
            statement.setString(2, playerName);
            statement.setLong(3, amount);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            logError(exception);
            return false;
        }
    }

    public synchronized boolean transfer(
            UUID sender,
            String senderName,
            UUID receiver,
            String receiverName,
            long amount
    ) {
        if (amount <= 0 || sender.equals(receiver)) return false;

        Connection connection;

        try {
            connection = plugin.getDatabaseManager().getConnection();
            connection.setAutoCommit(false);

            try {
                createAccountInTransaction(connection, sender, senderName);
                createAccountInTransaction(connection, receiver, receiverName);

                String removeSql = """
                        UPDATE player_gems
                        SET balance = balance - ?,
                            total_spent = total_spent + ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE uuid = ? AND balance >= ?
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(removeSql)) {

                    statement.setLong(1, amount);
                    statement.setLong(2, amount);
                    statement.setString(3, sender.toString());
                    statement.setLong(4, amount);

                    if (statement.executeUpdate() != 1) {
                        connection.rollback();
                        return false;
                    }
                }

                String addSql = """
                        UPDATE player_gems
                        SET balance = balance + ?,
                            total_earned = total_earned + ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE uuid = ?
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(addSql)) {

                    statement.setLong(1, amount);
                    statement.setLong(2, amount);
                    statement.setString(3, receiver.toString());

                    if (statement.executeUpdate() != 1) {
                        connection.rollback();
                        return false;
                    }
                }

                connection.commit();
                return true;

            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                logError(exception);
                return false;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {
            logError(exception);
            return false;
        }
    }

    private void createAccountInTransaction(
            Connection connection,
            UUID uuid,
            String playerName
    ) throws SQLException {

        String sql = """
                INSERT INTO player_gems (uuid, name, balance)
                VALUES (?, ?, 0)
                ON CONFLICT(uuid) DO UPDATE SET name = excluded.name
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, playerName);
            statement.executeUpdate();
        }
    }

    public long getTotalEarned(UUID uuid) {
        return getStatistic(uuid, "total_earned");
    }

    public long getTotalSpent(UUID uuid) {
        return getStatistic(uuid, "total_spent");
    }

    private long getStatistic(UUID uuid, String column) {
        if (!column.equals("total_earned") && !column.equals("total_spent")) {
            return 0;
        }

        String sql = "SELECT " + column + " FROM player_gems WHERE uuid = ?";

        try (PreparedStatement statement =
                     plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {

            statement.setString(1, uuid.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getLong(1);
            }

        } catch (SQLException exception) {
            logError(exception);
        }

        return 0;
    }

    private void logError(Exception exception) {
        plugin.getLogger().severe(
                "Gems database error: " + exception.getMessage()
        );
    }
}
