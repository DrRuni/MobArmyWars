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

public class OptionsGUI implements Listener {

    private final MobArmyMain plugin;

    public OptionsGUI(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {

        Inventory inv = Bukkit.createInventory(
                null,
                45,
                lang("options-gui.title")
        );

        inv.setItem(
                11,
                createItem(
                        Material.ENCHANTED_GOLDEN_APPLE,
                        lang("options-gui.start.name"),
                        Component.empty(),
                        lang("options-gui.start.description")
                )
        );

        inv.setItem(
                13,
                createItem(
                        Material.HONEY_BLOCK,
                        lang("options-gui.pause.name"),
                        Component.empty(),
                        lang("options-gui.pause.description")
                )
        );

        inv.setItem(
                15,
                createItem(
                        Material.TOTEM_OF_UNDYING,
                        lang("options-gui.resume.name"),
                        Component.empty(),
                        lang("options-gui.resume.description")
                )
        );

        inv.setItem(
                30,
                createItem(
                        Material.CLOCK,
                        lang("options-gui.timer.name"),
                        Component.empty(),
                        lang("options-gui.timer.description")
                )
        );

        inv.setItem(
                32,
                createItem(
                        Material.COMPARATOR,
                        lang("options-gui.setup.name"),
                        Component.empty(),
                        lang("options-gui.setup.description")
                )
        );

        inv.setItem(
                40,
                createCloseButton()
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

    private ItemStack createCloseButton() {

        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(
                lang("options-gui.close")
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
                lang("options-gui.title")
        )) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null) {
            return;
        }

        if (event.getRawSlot()
                >= event.getView().getTopInventory().getSize()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();

        if (clicked == null
                || clicked.getType() == Material.AIR) {
            return;
        }

        int slot = event.getRawSlot();

        switch (slot) {

            case 11 -> {
                plugin.getEventManager()
                        .enableEventHandling();

                plugin.getEventManager()
                        .startEvent();

                Sounds.playClick(player);
            }

            case 13 -> {

                plugin.getTimerManager()
                        .pauseTimer();

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(
                            lang("options-gui.pause.message")
                    );
                }

                Sounds.playClick(player);
            }

            case 15 -> {

                boolean ok =
                        plugin.getEventResume()
                                .resumeEvent();

                if (ok) {

                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.sendMessage(
                                lang("options-gui.resume.success")
                        );
                    }

                    Sounds.playClick(player);

                } else {

                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.sendMessage(
                                lang("options-gui.resume.failed")
                        );
                    }

                    Sounds.playDanger(player);
                }
            }

            case 30 -> {
                Sounds.playClick(player);
                plugin.getTimerGUI().open(player);
            }

            case 32 -> {
                Sounds.playClick(player);
                plugin.getEventSettingsGUI().open(player);
            }

            case 40 -> {
                Sounds.playClick(player);
                player.closeInventory();
            }
        }
    }

    private Component lang(String path) {
        return plugin.getLanguageManager()
                .getComponent(path);
    }
}