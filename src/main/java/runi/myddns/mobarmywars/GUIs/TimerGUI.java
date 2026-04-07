package runi.myddns.mobarmywars.GUIs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import runi.myddns.mobarmywars.Managers.Event.TimerManager;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.util.Arrays;

public class TimerGUI implements Listener {

    private final MobArmyMain plugin;
    private final TimerManager timerManager;

    public TimerGUI(MobArmyMain plugin, TimerManager timerManager) {
        this.plugin = plugin;
        this.timerManager = timerManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, ChatColor.DARK_AQUA + "Timer");

        inv.setItem(2,  item(Material.SLIME_BLOCK,
                ChatColor.GREEN + "Play",
                ChatColor.GRAY + "Startet den Timer (vorwärts)"
        ));

        inv.setItem(4,  item(Material.HONEY_BLOCK,
                ChatColor.GOLD  + "Pause",
                ChatColor.GRAY + "Pausiert den Timer"
        ));

        inv.setItem(6,  item(Material.REDSTONE_BLOCK,
                ChatColor.RED + "Stop",
                ChatColor.GRAY + "Beendet den Timer",
                ChatColor.GRAY + "Setzt ihn auf 0 zurück"
        ));

        inv.setItem(20, item(Material.GREEN_DYE,
                ChatColor.GREEN + "1 Stunde",
                ChatColor.GRAY + "Linksklick: +1 Stunde",
                ChatColor.GRAY + "Rechtsklick: -1 Stunde"
        ));

        inv.setItem(22, item(Material.ORANGE_DYE,
                ChatColor.GOLD  + "10 Minuten",
                ChatColor.GRAY + "Linksklick: +10 Minuten",
                ChatColor.GRAY + "Rechtsklick: -10 Minuten"
        ));

        inv.setItem(24, item(Material.RED_DYE,
                ChatColor.RED   + "1 Minute",
                ChatColor.GRAY + "Linksklick: +1 Minute",
                ChatColor.GRAY + "Rechtsklick: -1 Minute"
        ));

        inv.setItem(27, item(Material.ARROW,
                ChatColor.DARK_AQUA + "Zurück",
                ChatColor.GRAY + "Zurück zu den Optionen"
        ));

        player.openInventory(inv);
    }

    private ItemStack item(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        it.setItemMeta(meta);
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!ChatColor.stripColor(e.getView().getTitle()).equalsIgnoreCase("Timer")) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        ClickType click = e.getClick();

        Sounds.playClick(player);

        switch (name) {
            case "Play" -> timerManager.startTimer();
            case "Pause" -> timerManager.pauseTimer();
            case "Stop" -> timerManager.stopTimer();
            case "Zurück" -> plugin.getOptionenGUI().open(player);
            case "1 Stunde" -> handleTimeClick(player, click, 3600);
            case "10 Minuten" -> handleTimeClick(player, click, 600);
            case "1 Minute" -> handleTimeClick(player, click, 60);
        }

        timerManager.updateBossBar(null);
    }

    private void handleTimeClick(Player player, ClickType click, int seconds) {
        if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT) {
            timerManager.addTime(seconds);

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendMessage(ChatColor.GREEN + "+" + shortFmt(seconds) + ChatColor.GRAY + " hinzugefügt.");
            }
        } else if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
            timerManager.removeTime(seconds);

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendMessage(ChatColor.RED + "-" + shortFmt(seconds) + ChatColor.GRAY + " entfernt.");
            }
        }
    }

    private String shortFmt(int seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h";
        if (seconds >= 60) return (seconds / 60) + "m";
        return seconds + "s";
    }
}