
package my.kingdomvortex.vortexgems.util;

import my.kingdomvortex.vortexgems.VortexGems;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class InputManager implements Listener {

    private final VortexGems plugin;
    private final Map<UUID, Consumer<String>> pendingInputs =
            new ConcurrentHashMap<>();

    public InputManager(VortexGems plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void request(Player player, Consumer<String> callback) {
        pendingInputs.put(player.getUniqueId(), callback);

        player.sendMessage(ColorUtil.text(
                "<yellow>Type your input in chat."
        ));

        player.sendMessage(ColorUtil.text(
                "<gray>Type <red>cancel <gray>to cancel."
        ));
    }

    public boolean isWaiting(UUID uuid) {
        return pendingInputs.containsKey(uuid);
    }

    public void cancel(UUID uuid) {
        pendingInputs.remove(uuid);
    }

    public void cancel(Player player) {
        cancel(player.getUniqueId());
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Consumer<String> callback = pendingInputs.get(uuid);

        if (callback == null) {
            return;
        }

        event.setCancelled(true);

        String input = PlainTextComponentSerializer.plainText()
                .serialize(event.message()).trim();

        if (input.equalsIgnoreCase("cancel")) {
            pendingInputs.remove(uuid);

            player.sendMessage(ColorUtil.text(
                    "<red>Input cancelled."
            ));
            return;
        }

        if (input.isEmpty()) {
            player.sendMessage(ColorUtil.text(
                    "<red>Input cannot be empty."
            ));
            return;
        }

        if (!pendingInputs.remove(uuid, callback)) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            callback.accept(input);
        });
    }
}
