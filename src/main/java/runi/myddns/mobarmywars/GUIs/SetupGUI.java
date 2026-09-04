package runi.myddns.mobarmywars.GUIs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import runi.myddns.mobarmywars.Managers.World.WorldManager;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class SetupGUI implements Listener {

    private final MobArmyMain plugin;

    public SetupGUI(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {

        Inventory inv = Bukkit.createInventory(
                null,
                45,
                lang("setup-gui.title")
        );

        inv.setItem(4, createItem(
                Material.ENDER_PEARL,
                lang("setup-gui.teleports.name"),
                Component.empty(),
                lang("setup-gui.teleports.description")
        ));

        inv.setItem(10, createItem(
                Material.GRASS_BLOCK,
                lang("setup-gui.world-settings.name"),
                Component.empty(),
                lang("setup-gui.world-settings.description")
        ));

        inv.setItem(12, createItem(
                Material.PLAYER_HEAD,
                lang("setup-gui.players.name"),
                Component.empty(),
                lang("setup-gui.players.description")
        ));

        inv.setItem(14, createItem(
                Material.SHIELD,
                lang("setup-gui.teams.name"),
                Component.empty(),
                lang("setup-gui.teams.description")
        ));

        inv.setItem(16, createItem(
                Material.IRON_SWORD,
                lang("setup-gui.arena.name"),
                Component.empty(),
                lang("setup-gui.arena.description")
        ));

        inv.setItem(28, createItem(
                Material.WITHER_SKELETON_SKULL,
                lang("setup-gui.reset-game.name"),
                Component.empty(),
                lang("setup-gui.reset-game.description")
        ));

        inv.setItem(30, createItem(
                Material.STRUCTURE_BLOCK,
                lang("setup-gui.reset-teamworlds.name"),
                Component.empty(),
                lang("setup-gui.reset-teamworlds.description")
        ));

        inv.setItem(32, createItem(
                Material.STRUCTURE_BLOCK,
                lang("setup-gui.reset-lobby.name"),
                Component.empty(),
                lang("setup-gui.reset-lobby.description")
        ));

        inv.setItem(34, createItem(
                Material.STRUCTURE_BLOCK,
                lang("setup-gui.reset-arena.name"),
                Component.empty(),
                lang("setup-gui.reset-arena.description")
        ));

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

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!event.getView().title().equals(
                lang("setup-gui.title")
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

        switch (event.getRawSlot()) {

            case 4 -> {
                Sounds.playClick(player);
                plugin.getMobArmySettingsGUI().open(player);
            }

            case 10 -> {
                Sounds.playClick(player);
                plugin.getWorldSettingsGUI().open(player);
            }

            case 12 -> {
                Sounds.playClick(player);
                plugin.getPlayerGUI().open(player);
            }

            case 14 -> {
                Sounds.playClick(player);
                plugin.getTeamSettingsGUI().open(player);
            }

            case 16 -> {
                Sounds.playClick(player);
                plugin.getArenaSettingsGUI().open(player);
            }

            case 28 -> {
                player.closeInventory();
                plugin.getEventManager().resetGame(player);
            }

            case 30 -> resetTeamWorlds(player);

            case 32 -> resetLobbyWorld(player);

            case 34 -> resetArenaWorld(player);

            case 40 -> {
                Sounds.playBack(player);
                plugin.getOptionenGUI().open(player);
            }
        }
    }

    private void resetTeamWorlds(Player player) {

        WorldManager wm = plugin.getWorldManager();

        if (resetBlocked(player, wm)) {
            return;
        }

        showResetTitle(
                player,
                "commands.reset.teamworld.title",
                "commands.reset.teamworld.subtitle"
        );

        Bukkit.getScheduler().runTaskLater(
                plugin,
                wm::resetTeamWorlds,
                80L
        );
    }

    private void resetLobbyWorld(Player player) {

        WorldManager wm = plugin.getWorldManager();

        if (resetBlocked(player, wm)) {
            return;
        }

        showResetTitle(
                player,
                "commands.reset.lobby.title",
                "commands.reset.lobby.subtitle"
        );

        Bukkit.getScheduler().runTaskLater(
                plugin,
                wm::resetLobbyWorld,
                80L
        );
    }

    private void resetArenaWorld(Player player) {

        WorldManager wm = plugin.getWorldManager();

        if (resetBlocked(player, wm)) {
            return;
        }

        showResetTitle(
                player,
                "commands.reset.arena.title",
                "commands.reset.arena.subtitle"
        );

        Bukkit.getScheduler().runTaskLater(
                plugin,
                wm::resetArenaWorld,
                80L
        );
    }

    private boolean resetBlocked(
            Player player,
            WorldManager worldManager
    ) {

        if (worldManager.isWorldResetBlocked()) {

            player.sendMessage(
                    lang("commands.reset.already-running")
            );

            Sounds.playDanger(player);
            return true;
        }

        Sounds.playReset(player);
        player.closeInventory();

        return false;
    }

    private void showResetTitle(
            Player initiator,
            String titlePath,
            String subtitlePath
    ) {

        Component title =
                lang(titlePath);

        Component subtitle =
                plugin.getLanguageManager().getComponent(
                        subtitlePath,
                        "player",
                        initiator.getName()
                );

        Title resetTitle = Title.title(
                title,
                subtitle,
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(1)
                )
        );

        for (Player online : Bukkit.getOnlinePlayers()) {

            online.showTitle(resetTitle);

            online.playSound(
                    online.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_PLING,
                    1.0f,
                    1.2f
            );
        }
    }

    private ItemStack createBackButton() {

        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(
                lang("setup-gui.back")
        );

        meta.lore(List.of());

        item.setItemMeta(meta);

        return item;
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }
}