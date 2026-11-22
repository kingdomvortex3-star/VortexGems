
package my.kingdomvortex.vortexgems.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MenuHolder implements InventoryHolder {

    private final String menu;
    private final String data;
    private Inventory inventory;

    public MenuHolder(String menu) {
        this(menu, null);
    }

    public MenuHolder(String menu, String data) {
        this.menu = menu;
        this.data = data;
    }

    public String getMenu() {
        return menu;
    }

    public String getData() {
        return data;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
