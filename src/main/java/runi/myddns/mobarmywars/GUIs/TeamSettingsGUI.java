package runi.myddns.mobarmywars.GUIs;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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

    public TeamSettingsGUI(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {

        Inventory inv = Bukkit.createInventory(
                null,
                45,
                lang("team-settings-gui.title")
        );

        inv.setItem(
                11,
                createItem(
                        Material.PURPLE_BANNER,
                        lang("team-settings-gui.select.name"),
                        Component.empty(),
                        lang("team-settings-gui.select.description")
                )
        );

        inv.setItem(
                13,
                createItem(
                        Material.WHITE_BANNER,
                        lang("team-settings-gui.reset.name"),
                        Component.empty(),
                        lang("team-settings-gui.reset.description")
                )
        );

        inv.setItem(
                15,
                createItem(
                        Material.ARMOR_STAND,
                        lang("team-settings-gui.equipment.name"),
                        Component.empty(),
                        lang("team-settings-gui.equipment.description")
                )
        );

        inv.setItem(
                40,
                createBackButton()
        );

        player.openInventory(inv);
    }

    private ItemStack createItem(
            Material material,
            Component name,
            Component... loreLines
    ) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(name);

        List<Component> lore = new ArrayList<>();

        if (loreLines != null) {
            for (Component line : loreLines) {
                if (line != null) {
                    lore.add(line);
                }
            }
        }

        meta.lore(lore);

        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE
        );

        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createBackButton() {

        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(
                lang("team-settings-gui.back")
        );

        meta.lore(List.of());

        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!event.getView().title().equals(
                lang("team-settings-gui.title")
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

            case 11 -> {

                Sounds.playClick(player);

                Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                    plugin.getTeamSelectionGUI().openGUI(onlinePlayer);
                    Sounds.playClick(onlinePlayer);
                });

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

                    onlinePlayer.sendMessage(
                            Component.empty()
                    );

                    onlinePlayer.sendMessage(
                            lang("team-settings-gui.select.opened")
                    );
                }
            }

            case 13 -> {

                Sounds.playReset(player);

                plugin.getTeamManager()
                        .resetTeams();

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

                    plugin.getBundleManager()
                            .removeTeamBundle(onlinePlayer);
                }

                plugin.getScoreboardSwitcher()
                        .forceTeamForAll();

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

                    onlinePlayer.sendMessage(
                            lang("team-settings-gui.reset.message")
                    );
                }
            }

            case 15 -> {

                Sounds.playClick(player);

                plugin.getTeamEquipmentGUI()
                        .open(player);
            }

            case 40 -> {

                Sounds.playBack(player);

                plugin.getEventSettingsGUI()
                        .open(player);
            }
        }
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }
}