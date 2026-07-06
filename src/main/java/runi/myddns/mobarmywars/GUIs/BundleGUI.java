package runi.myddns.mobarmywars.GUIs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import runi.myddns.mobarmywars.Managers.Event.TeamManager;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BundleGUI {

    private final TeamManager teamManager;
    private final Map<String, Inventory> teamInventories = new HashMap<>();
    private final JavaPlugin plugin;
    private final File dataFile;
    private FileConfiguration config;

    public BundleGUI(JavaPlugin plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.dataFile = new File(plugin.getDataFolder(), "team_inventories.yml");
        loadInventories();
    }

    public void saveInventory(String team) {
        Inventory inv = teamInventories.get(team);
        if (inv == null) return;

        if (config == null) {
            config = YamlConfiguration.loadConfiguration(dataFile);
        }

        boolean changed = false;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack current = inv.getItem(i);
            ItemStack saved = config.getItemStack(team + ".slot" + i);
            if ((current != null && !current.equals(saved)) || (current == null && saved != null)) {
                config.set(team + ".slot" + i, current);
                changed = true;
            }
        }

        if (changed) {
            try {
                config.save(dataFile);
                System.out.println("[BundleGUI] Team " + team + " wurde gespeichert.");
            } catch (IOException e) {
                System.out.println("Fehler beim Speichern des Inventars: " + e.getMessage());
            }
        }
    }

    private void loadInventories() {
        config = YamlConfiguration.loadConfiguration(dataFile);
        Inventory blueInventory = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Blau Inventar");
        Inventory redInventory = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Rot Inventar");

        for (int i = 0; i < 27; i++) {
            blueInventory.setItem(i, config.getItemStack("Blau.slot" + i));
            redInventory.setItem(i, config.getItemStack("Rot.slot" + i));
        }

        teamInventories.put("Blau", blueInventory);
        teamInventories.put("Rot", redInventory);
    }

    public void clearTeamInventories() {
        for (Inventory inv : teamInventories.values()) {
            inv.clear();
        }
        saveOnShutdown();
    }

    public void openTeamInventory(Player player, String team) {
        Inventory teamInventory = teamInventories.get(team);
        if (teamInventory == null) {
            player.sendMessage(ChatColor.RED + "Kein Inventar für dieses Team gefunden!");
            return;
        }
        player.openInventory(teamInventory);
    }

    public void saveOnShutdown() {
        if (config == null) {
            config = YamlConfiguration.loadConfiguration(dataFile);
        }
        for (String team : teamInventories.keySet()) {
            saveInventory(team);
        }
    }
    public boolean isTeamInventory(Inventory inv) {
        return teamInventories.containsValue(inv);
    }
}