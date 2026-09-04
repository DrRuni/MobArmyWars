package runi.myddns.mobarmywars.Managers.Event;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import runi.myddns.mobarmywars.MobArmyMain;

public class BundleManager {

    private final MobArmyMain plugin;
    private final NamespacedKey teamBundleKey;

    private TeamManager teamManager;

    public BundleManager(MobArmyMain plugin) {
        this.plugin = plugin;
        this.teamBundleKey = new NamespacedKey(plugin, "team_bundle");
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

        Bukkit.getScheduler().runTaskLater(
                plugin,
                player::updateInventory,
                1L
        );
    }

    public void removeTeamBundle(Player player) {
        ItemStack slot9 = player.getInventory().getItem(9);

        if (isTeamBundle(slot9)) {
            player.getInventory().setItem(9, null);
        }
    }

    private ItemStack createBundle(String team) {
        Material material;
        String languagePath;
        String teamId;
        NamedTextColor color;

        if (team.equalsIgnoreCase("Blau")) {
            material = Material.BLUE_BUNDLE;
            languagePath = "bundle-manager.blue-name";
            teamId = "blue";
            color = NamedTextColor.BLUE;
        } else if (team.equalsIgnoreCase("Rot")) {
            material = Material.RED_BUNDLE;
            languagePath = "bundle-manager.red-name";
            teamId = "red";
            color = NamedTextColor.RED;
        } else {
            return null;
        }

        ItemStack bundle = new ItemStack(material);
        ItemMeta meta = bundle.getItemMeta();

        meta.displayName(
                plugin.getLanguageManager()
                        .getComponent(languagePath)
                        .color(color)
        );

        meta.getPersistentDataContainer().set(
                teamBundleKey,
                PersistentDataType.STRING,
                teamId
        );

        bundle.setItemMeta(meta);

        return bundle;
    }

    public boolean isTeamBundle(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        return item.getItemMeta()
                .getPersistentDataContainer()
                .has(
                        teamBundleKey,
                        PersistentDataType.STRING
                );
    }
}