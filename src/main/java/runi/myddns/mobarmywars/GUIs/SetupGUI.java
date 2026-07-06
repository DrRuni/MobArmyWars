package runi.myddns.mobarmywars.GUIs;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import runi.myddns.mobarmywars.Utils.Sounds;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Managers.World.WorldManager;

import java.util.ArrayList;
import java.util.List;

public class SetupGUI implements Listener {

    private final MobArmyMain plugin;

    private static final String TITLE = ChatColor.BLUE + "Event- & Welten Einstellungen";

    public SetupGUI(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);

        inv.setItem(4, createItem(
                Material.ENDER_PEARL,
                ChatColor.GOLD + "Teleports",
                "",
                "Teleport-Menü öffnen"
        ));

        inv.setItem(10, createItem(
                Material.GRASS_BLOCK,
                ChatColor.GOLD + "Welteinstellungen",
                "",
                "Öffne das Welt-Einstellungs-Menü"
        ));

        inv.setItem(12, createItem(
                Material.PLAYER_HEAD,
                ChatColor.GOLD + "Spieler-Verwaltung",
                "",
                "Öffnet Spieler-Einstellungen"
        ));

        inv.setItem(14, createItem(
                Material.SHIELD,
                ChatColor.GOLD + "Team Einstellungen",
                "",
                "Öffnet die Team-Einstellungen"
        ));

        inv.setItem(16, createItem(
                Material.IRON_SWORD,
                ChatColor.GOLD + "Arena-Einstellungen",
                "",
                "Öffnet die Arena-Einstellungen"
        ));

        inv.setItem(28, createItem(
                Material.WITHER_SKELETON_SKULL,
                ChatColor.DARK_RED + "Reset Spielfortschritt",
                "",
                ChatColor.RED + "Setzt Arena & Spielstatus zurück"
        ));

        inv.setItem(30, createItem(
                Material.STRUCTURE_BLOCK,
                ChatColor.DARK_RED + "Reset Team-Welten",
                "",
                ChatColor.RED + "Welten Rot & Blau werden neu generiert"
        ));

        inv.setItem(32, createItem(
                Material.STRUCTURE_BLOCK,
                ChatColor.DARK_RED + "Reset Lobby-Welt",
                "",
                ChatColor.RED + "Lobby wird komplett zurückgesetzt"
        ));

        inv.setItem(34, createItem(
                Material.STRUCTURE_BLOCK,
                ChatColor.DARK_RED + "Reset Arena-Welt",
                "",
                ChatColor.RED + "Arena wird komplett zurückgesetzt"
        ));

        inv.setItem(40, createItem(
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
        if (!ChatColor.stripColor(e.getView().getTitle()).equalsIgnoreCase("Event- & Welten Einstellungen")) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String itemName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        switch (itemName) {
            case "Arena-Einstellungen" -> {
                Sounds.playClick(player);
                plugin.getArenaSettingsGUI().open(player);
            }

            case "Welteinstellungen" -> {
                Sounds.playClick(player);
                plugin.getWorldSettingsGUI().open(player);
            }

            case "Spieler-Verwaltung" -> {
                Sounds.playClick(player);
                plugin.getPlayerGUI().open(player);
            }

            case "Team Einstellungen" -> {
                Sounds.playClick(player);
                plugin.getTeamSettingsGUI().open(player);
            }

            case "Teleports" -> {
                Sounds.playClick(player);
                plugin.getMobArmySettingsGUI().open(player);
            }

            case "Reset Team-Welten" -> {
                WorldManager wm = MobArmyMain.getInstance().getWorldManager();

                if (!wm.tryStartWorldReset()) {
                    player.sendMessage(ChatColor.RED + "⚠ Es läuft bereits ein Welt-Reset!");
                    Sounds.playDanger(player);
                    return;
                }

                Sounds.playReset(player);
                player.closeInventory();

                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendTitle(
                            ChatColor.DARK_RED + "⚠ Reset aktiviert!",
                            ChatColor.GOLD + player.getName() + " hat den Team-Welten-Reset gestartet",
                            10, 100, 20
                    );
                    online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
                }

                Bukkit.getScheduler().runTaskLater(
                        MobArmyMain.getInstance(),
                        () -> wm.resetTeamWorlds(),
                        80L
                );
            }

            case "Reset Lobby-Welt" -> {
                WorldManager wm = MobArmyMain.getInstance().getWorldManager();

                if (!wm.tryStartWorldReset()) {
                    player.sendMessage(ChatColor.RED + "⚠ Es läuft bereits ein Welt-Reset!");
                    Sounds.playDanger(player);
                    return;
                }

                Sounds.playReset(player);
                player.closeInventory();

                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendTitle(
                            ChatColor.DARK_RED + "⚠ Lobby-Reset!",
                            ChatColor.GOLD + player.getName() + " hat den Lobby-Welt-Reset gestartet",
                            10, 100, 20
                    );
                    online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
                }

                Bukkit.getScheduler().runTaskLater(
                        MobArmyMain.getInstance(),
                        () -> wm.resetLobbyWorld(),
                        80L
                );
            }

            case "Reset Arena-Welt" -> {
                WorldManager wm = MobArmyMain.getInstance().getWorldManager();

                if (!wm.tryStartWorldReset()) {
                    player.sendMessage(ChatColor.RED + "⚠ Es läuft bereits ein Welt-Reset!");
                    Sounds.playDanger(player);
                    return;
                }

                Sounds.playReset(player);
                player.closeInventory();

                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendTitle(
                            ChatColor.DARK_RED + "⚠ Arena-Reset!",
                            ChatColor.GOLD + player.getName() + " hat den Arena-Welt-Reset gestartet",
                            10, 100, 20
                    );
                    online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
                }

                Bukkit.getScheduler().runTaskLater(
                        MobArmyMain.getInstance(),
                        () -> wm.resetArenaWorld(),
                        80L
                );
            }

            case "Reset Spielfortschritt" -> {
                player.closeInventory();
                plugin.getEventManager().resetGame(player);
            }

            case "Zurück" -> {
                Sounds.playBack(player);
                plugin.getOptionenGUI().open(player);
            }
        }
    }
}