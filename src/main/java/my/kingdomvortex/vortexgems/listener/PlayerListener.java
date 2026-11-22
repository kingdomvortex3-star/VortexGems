
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

        boolean created = plugin.getGemsManager().createAccount(
                player.getUniqueId(),
                player.getName()
        );

        if (!created) {
            plugin.getLogger().warning(
                    "Failed to create/update Gems account for " + player.getName()
            );
        }

        if (plugin.getConfig().getBoolean(
                "settings.join-message.enabled",
                false
        )) {
            String message = plugin.getConfig().getString(
                    "settings.join-message.message",
                    "<gray>Welcome back, <white>{player}<gray>!"
            );

            player.sendMessage(ColorUtil.text(
                    message
                            .replace("{player}", player.getName())
                            .replace(
                                    "{balance}",
                                    String.valueOf(
                                            plugin.getGemsManager().getBalance(
                                                    player.getUniqueId()
                                            )
                                    )
                            )
            ));
        }
    }
}
