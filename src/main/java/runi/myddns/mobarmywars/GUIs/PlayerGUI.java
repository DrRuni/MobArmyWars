package runi.myddns.mobarmywars.GUIs;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

public class PlayerGUI implements Listener {

    private final MobArmyMain plugin;

    private final NamespacedKey playerUuidKey;
    private final NamespacedKey actionKey;

    public PlayerGUI(MobArmyMain plugin) {
        this.plugin = plugin;

        this.playerUuidKey =
                new NamespacedKey(plugin, "player_gui_uuid");

        this.actionKey =
                new NamespacedKey(plugin, "player_gui_action");
    }

    public void open(Player viewer) {

        Inventory inv = Bukkit.createInventory(
                null,
                45,
                lang("player-gui.title")
        );

        int[] startSlots = {10, 19};

        int maxPerRow = 7;
        int index = 0;

        for (Player target : Bukkit.getOnlinePlayers()) {

            int row = index / maxPerRow;

            if (row >= startSlots.length) {
                break;
            }

            int slot =
                    startSlots[row]
                            + (index % maxPerRow);

            ItemStack head =
                    new ItemStack(Material.PLAYER_HEAD);

            SkullMeta meta =
                    (SkullMeta) head.getItemMeta();

            if (meta != null) {

                meta.setOwningPlayer(target);

                meta.displayName(
                        Component.text(target.getName())
                );

                meta.getPersistentDataContainer().set(
                        playerUuidKey,
                        PersistentDataType.STRING,
                        target.getUniqueId().toString()
                );

                head.setItemMeta(meta);
            }

            inv.setItem(slot, head);

            index++;
        }

        inv.setItem(
                40,
                createBackButton()
        );

        viewer.openInventory(inv);
    }

    private ItemStack createBackButton() {

        ItemStack item =
                new ItemStack(Material.ARROW);

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(
                lang("player-gui.back")
        );

        meta.getPersistentDataContainer().set(
                actionKey,
                PersistentDataType.STRING,
                "back"
        );

        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!event.getView().title().equals(
                lang("player-gui.title")
        )) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked()
                instanceof Player viewer)) {
            return;
        }

        if (event.getClickedInventory() == null) {
            return;
        }

        if (event.getRawSlot()
                >= event.getView()
                .getTopInventory()
                .getSize()) {
            return;
        }

        ItemStack clicked =
                event.getCurrentItem();

        if (clicked == null
                || clicked.getType() == Material.AIR
                || !clicked.hasItemMeta()) {
            return;
        }

        ItemMeta meta =
                clicked.getItemMeta();

        if (meta == null) {
            return;
        }

        String action =
                meta.getPersistentDataContainer().get(
                        actionKey,
                        PersistentDataType.STRING
                );

        if ("back".equals(action)) {

            Sounds.playBack(viewer);

            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> plugin
                            .getEventSettingsGUI()
                            .open(viewer),
                    1L
            );

            return;
        }

        String uuidString =
                meta.getPersistentDataContainer().get(
                        playerUuidKey,
                        PersistentDataType.STRING
                );

        if (uuidString == null) {
            return;
        }

        Player target;

        try {

            target = Bukkit.getPlayer(
                    java.util.UUID.fromString(uuidString)
            );

        } catch (IllegalArgumentException ex) {

            return;
        }

        if (target != null
                && target.isOnline()) {

            Sounds.playClick(viewer);

            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> plugin
                            .getPlayerActionGUI()
                            .open(viewer, target),
                    1L
            );

        } else {

            viewer.sendMessage(
                    lang("player-gui.offline")
            );

            Sounds.playDanger(viewer);
        }
    }

    @EventHandler
    public void onInventoryDrag(
            InventoryDragEvent event
    ) {

        if (event.getView().title().equals(
                lang("player-gui.title")
        )) {
            event.setCancelled(true);
        }
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }
}