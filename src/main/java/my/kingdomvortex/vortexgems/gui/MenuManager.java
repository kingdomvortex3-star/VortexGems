
package my.kingdomvortex.vortexgems.gui;

import my.kingdomvortex.vortexgems.VortexGems;
import org.bukkit.entity.Player;

public final class MenuManager {

    private final VortexGems plugin;

    public MenuManager(VortexGems plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        new MainMenu(plugin).open(player);
    }

    public void openBuy(Player player) {
        new BuyMenu(plugin).open(player);
    }

    public void openCategories(Player player) {
        new CategoryMenu(plugin).open(player);
    }

    public void openRewards(Player player, String category) {
        new RewardMenu(plugin).open(player, category);
    }

    public void openConfirm(Player player, String rewardId) {
        new ConfirmMenu(plugin).open(player, rewardId);
    }

    public void openAdmin(Player player) {
        new AdminMenu(plugin).open(player);
    }

    public void openCategoryEditor(Player player) {
        new CategoryEditorMenu(plugin).open(player);
    }

    public void openRewardEditor(Player player) {
        new RewardEditorMenu(plugin).open(player);
    }

    public void openRewardCategory(Player player, String category) {
        new RewardEditorMenu(plugin).openCategory(player, category);
    }

    public void openRewardCreate(Player player, String category) {
        new RewardCreateMenu(plugin).open(player, category);
    }

    public void openSettings(Player player) {
        new SettingsMenu(plugin).open(player);
    }
}
