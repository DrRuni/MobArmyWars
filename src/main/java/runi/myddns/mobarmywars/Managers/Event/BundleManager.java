package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import runi.myddns.mobarmywars.MobArmyMain;

public class BundleManager {

    private final MobArmyMain plugin;
    private TeamManager teamManager;

    public BundleManager(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void setTeamManager(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public void giveTeamBundle(Player player) {
        String team = teamManager.getPlayerTeam(player);
        if (team == null) return;

        ItemStack bundle = createBundle(team);
        if (bundle == null) return;

        Inventory inventory = player.getInventory();

        inventory.setItem(9, bundle);

        Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
    }

    public void removeTeamBundle(Player player) {
        ItemStack slot9 = player.getInventory().getItem(9);
        if (slot9 != null && slot9.getType().toString().endsWith("_BUNDLE")) {
            player.getInventory().setItem(9, null);
        }
    }

    private ItemStack createBundle(String team) {
        ItemStack bundle = null;

        if (team.equalsIgnoreCase("Blau")) {
            bundle = new ItemStack(Material.BLUE_BUNDLE, 1);
            ItemMeta meta = bundle.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.BLUE + "Blaues Bundle");
                bundle.setItemMeta(meta);
            }
        } else if (team.equalsIgnoreCase("Rot")) {
            bundle = new ItemStack(Material.RED_BUNDLE, 1);
            ItemMeta meta = bundle.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "Rotes Bundle");
                bundle.setItemMeta(meta);
            }
        }

        return bundle;
    }

    public boolean hasTeamBundle(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isTeamBundle(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTeamBundle(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        String name = ChatColor.stripColor(meta.getDisplayName());
        return name.equalsIgnoreCase("Blaues Bundle")
                || name.equalsIgnoreCase("Rotes Bundle");
    }
}