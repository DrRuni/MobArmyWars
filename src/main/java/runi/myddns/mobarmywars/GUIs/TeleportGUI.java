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
import runi.myddns.mobarmywars.Managers.World.ResumeManager;
import runi.myddns.mobarmywars.Managers.World.TeleportManager;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.util.ArrayList;
import java.util.List;

public class TeleportGUI implements Listener {

    private final MobArmyMain plugin;

    public TeleportGUI(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {

        Inventory inv = Bukkit.createInventory(
                null,
                45,
                lang("teleport-gui.title")
        );

        inv.setItem(10, createItem(
                Material.ENDER_PEARL,
                lang("teleport-gui.self.lobby.name"),
                Component.empty(),
                lang("teleport-gui.self.description")
        ));

        inv.setItem(12, createItem(
                Material.ENDER_PEARL,
                lang("teleport-gui.self.teamworld.name"),
                Component.empty(),
                lang("teleport-gui.self.description")
        ));

        inv.setItem(14, createItem(
                Material.ENDER_PEARL,
                lang("teleport-gui.self.wave.name"),
                Component.empty(),
                lang("teleport-gui.self.description")
        ));

        inv.setItem(16, createItem(
                Material.ENDER_PEARL,
                lang("teleport-gui.self.arena.name"),
                Component.empty(),
                lang("teleport-gui.self.description")
        ));

        inv.setItem(28, createItem(
                Material.END_CRYSTAL,
                lang("teleport-gui.all.lobby.name"),
                Component.empty(),
                lang("teleport-gui.all.description"),
                lang("teleport-gui.all.lobby.phase")
        ));

        inv.setItem(30, createItem(
                Material.END_CRYSTAL,
                lang("teleport-gui.all.teamworld.name"),
                Component.empty(),
                lang("teleport-gui.all.description"),
                lang("teleport-gui.all.teamworld.phase")
        ));

        inv.setItem(32, createItem(
                Material.END_CRYSTAL,
                lang("teleport-gui.all.wave.name"),
                Component.empty(),
                lang("teleport-gui.all.description"),
                lang("teleport-gui.all.wave.phase")
        ));

        inv.setItem(34, createItem(
                Material.END_CRYSTAL,
                lang("teleport-gui.all.arena.name"),
                Component.empty(),
                lang("teleport-gui.all.description"),
                lang("teleport-gui.all.arena.phase")
        ));

        inv.setItem(40, createBackButton());

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

        meta.displayName(lang("teleport-gui.back"));
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
                lang("teleport-gui.title")
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

            case 10 -> teleportSelfLobby(player);

            case 12 -> teleportSelfTeamWorld(player);

            case 14 -> teleportSelfWave(player);

            case 16 -> teleportSelfArena(player);

            case 28 -> teleportAllLobby(player);

            case 30 -> teleportAllTeamWorld(player);

            case 32 -> teleportAllWave(player);

            case 34 -> teleportAllArena(player);

            case 40 -> {
                Sounds.playBack(player);
                plugin.getEventSettingsGUI().open(player);
            }
        }
    }

    private void teleportSelfLobby(Player player) {

        TeleportManager.teleport(
                player,
                "world_mobarmy_lobby"
        );

        Sounds.playTeleport(player);
        player.closeInventory();

        player.sendMessage(
                lang("teleport-gui.messages.lobby-self")
        );
    }

    private void teleportAllLobby(Player player) {

        Sounds.playClick(player);

        boolean teleportedAnyone = false;

        for (Player online : Bukkit.getOnlinePlayers()) {

            TeleportManager.teleport(
                    online,
                    "world_mobarmy_lobby"
            );

            Sounds.playTeleport(online);
            teleportedAnyone = true;
        }

        if (!teleportedAnyone) {

            player.sendMessage(
                    lang("teleport-gui.messages.no-player")
            );

            return;
        }

        plugin.getEventResume()
                .savePhase(ResumeManager.PHASE_LOBBY);

        player.closeInventory();

        broadcast(
                lang("teleport-gui.messages.lobby-all")
        );
    }

    private void teleportSelfTeamWorld(Player player) {

        String team =
                plugin.getTeamManager().getPlayerTeam(player);

        if (hasNoTeam(team)) {

            player.sendMessage(
                    lang("teleport-gui.messages.no-team")
            );

            return;
        }

        String worldName =
                team.equalsIgnoreCase("Rot")
                        ? "world_rot"
                        : "world_blau";

        TeleportManager.teleport(
                player,
                worldName
        );

        Sounds.playTeleport(player);

        player.closeInventory();

        player.sendMessage(
                lang("teleport-gui.messages.teamworld-self")
        );
    }

    private void teleportAllTeamWorld(Player player) {

        Sounds.playClick(player);

        boolean teleportedAnyone = false;

        for (Player online : Bukkit.getOnlinePlayers()) {

            String team =
                    plugin.getTeamManager()
                            .getPlayerTeam(online);

            if (hasNoTeam(team)) {

                online.sendMessage(
                        lang("teleport-gui.messages.skipped-no-team")
                );

                continue;
            }

            if (team.equalsIgnoreCase("Rot")) {

                TeleportManager.teleport(
                        online,
                        "world_rot"
                );

            } else if (team.equalsIgnoreCase("Blau")) {

                TeleportManager.teleport(
                        online,
                        "world_blau"
                );

            } else {

                online.sendMessage(
                        lang("teleport-gui.messages.invalid-team")
                );

                continue;
            }

            Sounds.playTeleport(online);

            online.sendMessage(
                    lang("teleport-gui.messages.teamworld-self")
            );

            teleportedAnyone = true;
        }

        if (!teleportedAnyone) {

            player.sendMessage(
                    lang("teleport-gui.messages.no-valid-player")
            );

            return;
        }

        player.closeInventory();

        plugin.getEventResume()
                .savePhase(ResumeManager.PHASE_TEAMWELT);

        broadcastWithBlank(
                lang("teleport-gui.messages.teamworld-all")
        );
    }

    private void teleportSelfWave(Player player) {

        String team =
                plugin.getTeamManager().getPlayerTeam(player);

        if (hasNoTeam(team)) {

            player.sendMessage(
                    lang("teleport-gui.messages.no-team")
            );

            return;
        }

        TeleportManager.teleportToWaveSelection(player);
        Sounds.playTeleport(player);

        restartTimer();

        plugin.getEventResume()
                .savePlayerSpawn(
                        player,
                        player.getLocation()
                );

        player.closeInventory();

        plugin.getArenaCompassManager()
                .giveMonsterCompass(player);

        player.sendMessage(
                lang("teleport-gui.messages.wave-self")
        );
    }

    private void teleportAllWave(Player player) {

        Sounds.playClick(player);

        boolean teleportedAnyone = false;

        for (Player online : Bukkit.getOnlinePlayers()) {

            String team =
                    plugin.getTeamManager()
                            .getPlayerTeam(online);

            if (hasNoTeam(team)) {

                online.sendMessage(
                        lang("teleport-gui.messages.skipped-no-team")
                );

                continue;
            }

            TeleportManager.teleportToWaveSelection(online);
            Sounds.playTeleport(online);

            plugin.getArenaCompassManager()
                    .giveMonsterCompass(online);

            online.sendMessage(
                    lang("teleport-gui.messages.wave-self")
            );

            plugin.getEventResume()
                    .savePlayerSpawn(
                            online,
                            online.getLocation()
                    );

            teleportedAnyone = true;
        }

        if (!teleportedAnyone) {

            player.sendMessage(
                    lang("teleport-gui.messages.no-valid-player")
            );

            return;
        }

        restartTimer();

        player.closeInventory();

        plugin.getEventResume()
                .savePhase(ResumeManager.PHASE_WAVEAUSWAHL);

        broadcastWithBlank(
                lang("teleport-gui.messages.wave-all")
        );
    }

    private void teleportSelfArena(Player player) {

        String team =
                plugin.getTeamManager().getPlayerTeam(player);

        if (hasNoTeam(team)) {

            player.sendMessage(
                    lang("teleport-gui.messages.no-team")
            );

            return;
        }

        TeleportManager.teleportToArena(player);
        Sounds.playTeleport(player);

        plugin.getEventResume()
                .savePlayerSpawn(
                        player,
                        player.getLocation()
                );

        player.sendMessage(
                lang("teleport-gui.messages.arena-self")
        );

        player.closeInventory();

        plugin.getArenaCompassManager()
                .giveMonsterCompass(player);
    }

    private void teleportAllArena(Player player) {

        Sounds.playClick(player);

        boolean teleportedAnyone = false;

        for (Player online : Bukkit.getOnlinePlayers()) {

            String team =
                    plugin.getTeamManager()
                            .getPlayerTeam(online);

            if (hasNoTeam(team)) {

                online.sendMessage(
                        lang("teleport-gui.messages.skipped-no-team")
                );

                continue;
            }

            TeleportManager.teleportToArena(online);
            Sounds.playTeleport(online);

            plugin.getEventResume()
                    .savePlayerSpawn(
                            online,
                            online.getLocation()
                    );

            plugin.getArenaCompassManager()
                    .giveMonsterCompass(online);

            online.sendMessage(
                    lang("teleport-gui.messages.arena-self")
            );

            teleportedAnyone = true;
        }

        if (!teleportedAnyone) {

            player.sendMessage(
                    lang("teleport-gui.messages.no-valid-player")
            );

            return;
        }

        player.closeInventory();

        plugin.getEventResume()
                .savePhase(ResumeManager.PHASE_ARENA);

        broadcastWithBlank(
                lang("teleport-gui.messages.arena-all")
        );
    }

    private void restartTimer() {

        plugin.getTimerManager().stopTimer();
        plugin.getTimerManager().updateBossBar(null);
        plugin.getTimerManager().setForward(true);
        plugin.getTimerManager().startTimer();
    }

    private boolean hasNoTeam(String team) {

        return team == null
                || team.equalsIgnoreCase("Kein Team");
    }

    private void broadcast(Component message) {

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(message);
        }
    }

    private void broadcastWithBlank(Component message) {

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(Component.empty());
            online.sendMessage(message);
        }
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }
}