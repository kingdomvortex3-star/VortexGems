package my.kingdomvortex.vortexgems.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import my.kingdomvortex.vortexgems.VortexGems;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

public class GemsExpansion extends PlaceholderExpansion {

    private final VortexGems plugin;

    public GemsExpansion(VortexGems plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "vortexgems";
    }

    @Override
    public @NotNull String getAuthor() {
        return "KingdomVortex";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(
            OfflinePlayer player,
            @NotNull String params
    ) {
        if (player == null || player.getUniqueId() == null) {
            return "0";
        }

        long balance = plugin.getGemsManager().getBalance(player.getUniqueId());

        return switch (params.toLowerCase(Locale.ROOT)) {
            case "balance", "amount" -> String.valueOf(balance);

            case "balance_formatted", "formatted" ->
                    NumberFormat.getNumberInstance(Locale.US).format(balance);

            case "balance_short", "short" -> formatShort(balance);

            default -> null;
        };
    }

    private String formatShort(long value) {
        if (value >= 1_000_000_000L) {
            return String.format(Locale.US, "%.1fB", value / 1_000_000_000.0);
        }

        if (value >= 1_000_000L) {
            return String.format(Locale.US, "%.1fM", value / 1_000_000.0);
        }

        if (value >= 1_000L) {
            return String.format(Locale.US, "%.1fK", value / 1_000.0);
        }

        return String.valueOf(value);
    }
          }
