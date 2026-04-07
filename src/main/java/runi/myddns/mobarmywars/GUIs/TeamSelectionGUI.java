package runi.myddns.mobarmywars.GUIs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import runi.myddns.mobarmywars.Managers.Event.TeamManager;
import runi.myddns.mobarmywars.MobArmyMain;

public class TeamSelectionGUI implements Listener {
    private final MobArmyMain plugin;
    private final TeamManager teamManager;
    private final String GUI_TITLE = ChatColor.BLUE + "Team Auswahl";

    public TeamSelectionGUI(MobArmyMain plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    public void openGUI(Player player) {

        Inventory gui = Bukkit.createInventory(null, 9, GUI_TITLE);

        gui.setItem(2, createMenuItem(Material.RED_WOOL, ChatColor.RED + "Team Rot"));
        gui.setItem(4, createMenuItem(Material.WHITE_WOOL, ChatColor.GRAY + "Kein Team"));
        gui.setItem(6, createMenuItem(Material.BLUE_WOOL, ChatColor.BLUE + "Team Blau"));

        player.openInventory(gui);
    }

    private ItemStack createMenuItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(ChatColor.BLUE + "Team Auswahl")) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;

        String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        if ("Kein Team".equalsIgnoreCase(itemName)) {
            teamManager.removePlayerFromTeam(player);
            player.sendMessage("");
            player.sendMessage(ChatColor.GRAY + "Du bist keinem Team zugeordnet.!");
            player.closeInventory();
            return;
        }

        if ("Team Rot".equalsIgnoreCase(itemName)) {
            teamManager.assignTeam(player, "Rot");

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendMessage(
                        ChatColor.YELLOW + player.getName() + ChatColor.RED + " ist nun im Team Rot!"
                );
            }

            player.closeInventory();
            return;
        }

        if ("Team Blau".equalsIgnoreCase(itemName)) {
            teamManager.assignTeam(player, "Blau");

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendMessage(
                        ChatColor.YELLOW + player.getName() + ChatColor.BLUE + " ist nun im Team Blau!"
                );
            }

            player.closeInventory();
        }
    }
}