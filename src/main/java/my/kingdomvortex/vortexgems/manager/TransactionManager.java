
package my.kingdomvortex.vortexgems.manager;

import my.kingdomvortex.vortexgems.VortexGems;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TransactionManager {

    private final VortexGems plugin;

    public TransactionManager(VortexGems plugin) {
        this.plugin = plugin;
    }

    public boolean record(
            UUID uuid,
            String type,
            long amount,
            String description
    ) {
        if (uuid == null || type == null || amount <= 0) {
            return false;
        }

        String sql = """
                INSERT INTO gems_transactions
                    (uuid, type, amount, description)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {

            statement.setString(1, uuid.toString());
            statement.setString(2, type.toUpperCase());
            statement.setLong(3, amount);
            statement.setString(4, description == null ? "" : description);

            return statement.executeUpdate() == 1;

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Transaction recording failed: " + exception.getMessage()
            );
            return false;
        }
    }

    public List<Transaction> getHistory(UUID uuid, int limit) {
        List<Transaction> transactions = new ArrayList<>();

        String sql = """
                SELECT id, type, amount, description, created_at
                FROM gems_transactions
                WHERE uuid = ?
                ORDER BY id DESC
                LIMIT ?
                """;

        try (PreparedStatement statement =
                     plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {

            statement.setString(1, uuid.toString());
            statement.setInt(2, Math.max(1, limit));

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    transactions.add(new Transaction(
                            result.getLong("id"),
                            result.getString("type"),
                            result.getLong("amount"),
                            result.getString("description"),
                            result.getString("created_at")
                    ));
                }
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Could not load transaction history: " + exception.getMessage()
            );
        }

        return transactions;
    }

    public static final class Transaction {

        private final long id;
        private final String type;
        private final long amount;
        private final String description;
        private final String date;

        public Transaction(
                long id,
                String type,
                long amount,
                String description,
                String date
        ) {
            this.id = id;
            this.type = type;
            this.amount = amount;
            this.description = description;
            this.date = date;
        }

        public long getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public long getAmount() {
            return amount;
        }

        public String getDescription() {
            return description;
        }

        public String getDate() {
            return date;
        }
    }
}
