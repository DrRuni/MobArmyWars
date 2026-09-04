package runi.myddns.mobarmywars.GUIs;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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

import java.util.List;

public class TimerGUI implements Listener {

    private final MobArmyMain plugin;
    private final TimerManager timerManager;

    public TimerGUI(
            MobArmyMain plugin,
            TimerManager timerManager
    ) {
        this.plugin = plugin;
        this.timerManager = timerManager;
    }

    public void open(Player player) {

        Inventory inv = Bukkit.createInventory(
                null,
                36,
                lang("timer-gui.title")
        );

        inv.setItem(2, item(
                Material.SLIME_BLOCK,
                lang("timer-gui.play.name"),
                Component.empty(),
                lang("timer-gui.play.description")
        ));

        inv.setItem(4, item(
                Material.HONEY_BLOCK,
                lang("timer-gui.pause.name"),
                Component.empty(),
                lang("timer-gui.pause.description")
        ));

        inv.setItem(6, item(
                Material.REDSTONE_BLOCK,
                lang("timer-gui.stop.name"),
                Component.empty(),
                lang("timer-gui.stop.description-1"),
                lang("timer-gui.stop.description-2")
        ));

        inv.setItem(20, item(
                Material.GREEN_DYE,
                lang("timer-gui.hour.name"),
                Component.empty(),
                lang("timer-gui.hour.left"),
                lang("timer-gui.hour.right")
        ));

        inv.setItem(22, item(
                Material.ORANGE_DYE,
                lang("timer-gui.ten-minutes.name"),
                Component.empty(),
                lang("timer-gui.ten-minutes.left"),
                lang("timer-gui.ten-minutes.right")
        ));

        inv.setItem(24, item(
                Material.RED_DYE,
                lang("timer-gui.minute.name"),
                Component.empty(),
                lang("timer-gui.minute.left"),
                lang("timer-gui.minute.right")
        ));

        inv.setItem(
                31,
                createBackButton()
        );

        player.openInventory(inv);
    }

    private ItemStack item(
            Material material,
            Component name,
            Component... lore
    ) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(name);
        meta.lore(List.of(lore));

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
                lang("timer-gui.back")
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
                lang("timer-gui.title")
        )) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (slot < 0
                || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        ClickType click = event.getClick();

        switch (slot) {

            case 2 -> {
                Sounds.playClick(player);
                timerManager.startTimer();
            }

            case 4 -> {
                Sounds.playClick(player);
                timerManager.pauseTimer();
            }

            case 6 -> {
                Sounds.playClick(player);
                timerManager.stopTimer();
            }

            case 20 -> {
                Sounds.playClick(player);
                handleTimeClick(click, 3600);
            }

            case 22 -> {
                Sounds.playClick(player);
                handleTimeClick(click, 600);
            }

            case 24 -> {
                Sounds.playClick(player);
                handleTimeClick(click, 60);
            }

            case 31 -> {
                Sounds.playBack(player);
                plugin.getOptionenGUI().open(player);
            }
        }

        timerManager.updateBossBar(null);
    }

    private void handleTimeClick(
            ClickType click,
            int seconds
    ) {

        if (click == ClickType.LEFT
                || click == ClickType.SHIFT_LEFT) {

            timerManager.addTime(seconds);

            broadcast(
                    langTime(
                            "timer-gui.messages.added",
                            shortFmt(seconds)
                    )
            );

        } else if (click == ClickType.RIGHT
                || click == ClickType.SHIFT_RIGHT) {

            timerManager.removeTime(seconds);

            broadcast(
                    langTime(
                            "timer-gui.messages.removed",
                            shortFmt(seconds)
                    )
            );
        }
    }

    private String shortFmt(int seconds) {

        if (seconds >= 3600) {
            return (seconds / 3600) + "h";
        }

        if (seconds >= 60) {
            return (seconds / 60) + "m";
        }

        return seconds + "s";
    }

    private void broadcast(Component message) {

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
        }
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }

    private Component langTime(
            String path,
            Object time
    ) {

        return plugin.getLanguageManager()
                .getComponent(
                        path,
                        "time",
                        time
                );
    }
}