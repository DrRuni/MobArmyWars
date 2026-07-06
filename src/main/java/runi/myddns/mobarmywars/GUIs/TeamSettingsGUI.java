package runi.myddns.mobarmywars.GUIs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.util.ArrayList;
import java.util.List;

public class TeamSettingsGUI implements Listener {

    private final MobArmyMain plugin;

    private static final String TITLE = ChatColor.BLUE + "Team Einstellungen";

    public TeamSettingsGUI(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        inv.setItem(11, createItem(
                Material.PURPLE_BANNER,
                ChatColor.GOLD + "Team wählen",
                "",
                "Öffnet die Teamauswahl für alle Spieler"
        ));

        inv.setItem(13, createItem(
                Material.WHITE_BANNER,
                ChatColor.DARK_RED + "Teams zurücksetzen",
                "",
                ChatColor.RED + "Alle Spieler werden aus Teams entfernt"
        ));

        inv.setItem(15, createItem(
                Material.ARMOR_STAND,
                ChatColor.GOLD + "Team Equipment",
                "",
                "Equipment für Rot und Blau einstellen"
        ));

        inv.setItem(22, createItem(
                Material.ARROW,
                ChatColor.DARK_AQUA + "Zurück"
        ));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        if (loreLines != null && loreLines.length > 0) {
            for (String line : loreLines) {
                lore.add(ChatColor.GRAY + line);
            }
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!ChatColor.stripColor(e.getView().getTitle()).equalsIgnoreCase("Team Einstellungen")) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String itemName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        switch (itemName) {
            case "Team wählen" -> {
                Sounds.playClick(player);

                Bukkit.getOnlinePlayers().forEach(p -> {
                    plugin.getTeamSelectionGUI().openGUI(p);
                    Sounds.playClick(p);
                });

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage("");
                    onlinePlayer.sendMessage(ChatColor.AQUA + "👥 " + ChatColor.GRAY + "Der Teamleiter hat die Teamauswahl geöffnet.");
                }
            }

            case "Teams zurücksetzen" -> {
                Sounds.playReset(player);
                plugin.getTeamManager().resetTeams();

                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> List.of("world_mobarmylobby", "world_rot", "world_blau")
                                .contains(p.getWorld().getName().toLowerCase()))
                        .forEach(p -> plugin.getBundleManager().removeTeamBundle(p));

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(ChatColor.RED + "❌ Alle Teams wurden zurückgesetzt!");
                }

                Sounds.playClick(player);
            }

            case "Team Equipment" -> {
                Sounds.playClick(player);
                plugin.getTeamEquipmentGUI().open(player);
            }

            case "Zurück" -> {
                Sounds.playBack(player);
                plugin.getEventSettingsGUI().open(player);
            }
        }
    }
}
