package my.kingdomvortex.vortexgems.listener;

import my.kingdomvortex.vortexgems.VortexGems;
import my.kingdomvortex.vortexgems.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerListener implements Listener {

    private final VortexGems plugin;

    public PlayerListener(VortexGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.getGemsManager().createAccount(
                player.getUniqueId(),
                player.getName()
        );

        if (!plugin.getConfig().getBoolean(
                "settings.join-message.enabled", false
        )) {
            return;
        }

        String message = plugin.getConfig().getString(
                "settings.join-message.message",
                "<gray>Welcome back, <white>{player}<gray>!"
        );

        message = ColorUtil.replace(message, "player", player.getName());
        message = ColorUtil.replace(
                message,
                "balance",
                ColorUtil.format(
                        plugin.getGemsManager().getBalance(player.getUniqueId())
                )
        );

        player.sendMessage(ColorUtil.text(message));
    }
}
