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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OptionenGUI implements Listener {

    private final MobArmyMain plugin;
    private static final String TITLE = ChatColor.BLUE + "MobArmyWars Optionen";

    public OptionenGUI(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE);

        inv.setItem(2, createItem(Material.ENCHANTED_GOLDEN_APPLE, "Start MobArmyWars",
                "Startet den Countdown, falls Zeit > 0"));
        inv.setItem(4, createItem(Material.HONEY_BLOCK, "Event Pause",
                "Pausiert den Timer und friert Spieler ein"));
        inv.setItem(6, createItem(Material.TOTEM_OF_UNDYING, "Event fortsetzen (Resume)",
                "Setzt Timer und Event fort"));
        inv.setItem(21, createItem(Material.CLOCK, "Timer",
                "Öffne das Timer-Menü"));
        inv.setItem(23, createItem(Material.COMPARATOR, "Event Einstellungen",
                "Teleport & Reset-Optionen"));

        inv.setItem(31, createBackButton("Menü schließen"));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + name);

        List<String> lore = new ArrayList<>();
        if (loreLines != null && loreLines.length > 0) {
            for (String line : loreLines) lore.add(ChatColor.GRAY + line);
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackButton(String ziel) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_AQUA + ziel);
        meta.setLore(Arrays.asList());
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!ChatColor.stripColor(e.getView().getTitle()).equalsIgnoreCase("MobArmyWars Optionen")) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        switch (name) {
            case "Start MobArmyWars" -> {
                plugin.getEventManager().enableEventHandling();
                plugin.getEventManager().startCountdown();
                Sounds.playClick(player);
            }

            case "Event Pause" -> {
                plugin.getTimerManager().pauseTimer();

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(ChatColor.YELLOW + "⏸ Event pausiert!");
                }

                Sounds.playClick(player);
            }

            case "Event fortsetzen (Resume)" -> {
                boolean ok = plugin.getEventResume().resumeEvent();

                if (ok) {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.sendMessage(ChatColor.GREEN + "▶ Event fortgesetzt!");
                    }
                    Sounds.playClick(player);
                } else {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.sendMessage(ChatColor.RED + "❌ Event konnte nicht fortgesetzt werden!");
                    }
                    Sounds.playDanger(player);
                }
            }

            case "Timer" -> {
                Sounds.playClick(player);
                plugin.getTimerGUI().open(player);
            }

            case "Event Einstellungen" -> {
                Sounds.playClick(player);
                plugin.getEventSettingsGUI().open(player);
            }

            case "Menü schließen" -> {
                Sounds.playClick(player);
                player.closeInventory();
            }
        }
    }
}