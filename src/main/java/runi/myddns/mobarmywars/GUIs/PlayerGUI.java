package runi.myddns.mobarmywars.GUIs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

public class PlayerGUI implements Listener {

    private final MobArmyMain plugin;
    private static final String GUI_TITLE = ChatColor.BLUE + "Online-Spieler";

    public PlayerGUI(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(null, 45, GUI_TITLE);

        int[] startSlots = {10, 19};
        int maxPerRow = 7;
        int index = 0;

        for (Player target : Bukkit.getOnlinePlayers()) {
            int row = index / maxPerRow;
            if (row >= startSlots.length) break;

            int slot = startSlots[row] + (index % maxPerRow);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.setDisplayName(ChatColor.YELLOW + target.getName());
                head.setItemMeta(meta);
            }

            inv.setItem(slot, head);
            index++;
        }

        inv.setItem(40, createBackButton());

        viewer.openInventory(inv);
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_AQUA + "Zurück");
            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ChatColor.stripColor(event.getView().getTitle()).equals("Online-Spieler")) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        if (name.equalsIgnoreCase("zurück")) {
            Sounds.playBack(viewer);

            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> plugin.getEventSettingsGUI().open(viewer),
                    1L
            );
            return;
        }

        Player target = Bukkit.getPlayerExact(name);

        if (target != null && target.isOnline()) {
            Sounds.playClick(viewer);

            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> plugin.getPlayerActionGUI().open(viewer, target),
                    1L
            );
        } else {
            viewer.sendMessage(ChatColor.RED + "Der Spieler ist nicht mehr online!");
            Sounds.playDanger(viewer);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (ChatColor.stripColor(event.getView().getTitle()).equals("Online-Spieler")) {
            event.setCancelled(true);
        }
    }
}
