package dev.smp.heartrender;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** A read-only inventory that shows the 3x3 crafting recipe. */
public class RecipeGui implements InventoryHolder {

    private static final int[] GRID_SLOTS = {1, 2, 3, 10, 11, 12, 19, 20, 21};

    private final Inventory inventory;

    public RecipeGui(String title, Material[] grid, ItemStack result) {
        this.inventory = Bukkit.createInventory(this, 27, title);

        ItemStack pane = named(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, pane);
        }
        for (int i = 0; i < 9; i++) {
            inventory.setItem(GRID_SLOTS[i], grid[i] == null ? null : new ItemStack(grid[i]));
        }
        inventory.setItem(13, named(Material.ARROW, "\u00a77\u279c"));
        inventory.setItem(15, result);
    }

    private static ItemStack named(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
