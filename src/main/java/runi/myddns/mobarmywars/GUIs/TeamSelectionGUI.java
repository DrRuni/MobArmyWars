package runi.myddns.mobarmywars.GUIs;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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

    public TeamSelectionGUI(
            MobArmyMain plugin,
            TeamManager teamManager
    ) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    public void openGUI(Player player) {

        Inventory gui = Bukkit.createInventory(
                null,
                9,
                lang("team-selection-gui.title")
        );

        gui.setItem(
                2,
                createMenuItem(
                        Material.BLUE_WOOL,
                        lang("team-selection-gui.blue")
                )
        );

        gui.setItem(
                4,
                createMenuItem(
                        Material.WHITE_WOOL,
                        lang("team-selection-gui.none")
                )
        );

        gui.setItem(
                6,
                createMenuItem(
                        Material.RED_WOOL,
                        lang("team-selection-gui.red")
                )
        );

        player.openInventory(gui);
    }

    private ItemStack createMenuItem(
            Material material,
            Component name
    ) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(name);
        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!event.getView().title().equals(
                lang("team-selection-gui.title")
        )) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (slot < 0
                || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        switch (slot) {

            case 2 -> {

                teamManager.assignTeam(
                        player,
                        "Blau"
                );

                Component message =
                        langPlayer(
                                "team-selection-gui.messages.blue",
                                player.getName()
                        );

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(message);
                }

                player.closeInventory();
            }

            case 4 -> {

                teamManager.removePlayerFromTeam(player);

                player.sendMessage(Component.empty());

                player.sendMessage(
                        lang("team-selection-gui.messages.none")
                );

                player.closeInventory();
            }

            case 6 -> {

                teamManager.assignTeam(
                        player,
                        "Rot"
                );

                Component message =
                        langPlayer(
                                "team-selection-gui.messages.red",
                                player.getName()
                        );

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(message);
                }

                player.closeInventory();
            }
        }
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }

    private Component langPlayer(
            String path,
            Object player
    ) {

        return plugin.getLanguageManager()
                .getComponent(
                        path,
                        "player",
                        player
                );
    }
}