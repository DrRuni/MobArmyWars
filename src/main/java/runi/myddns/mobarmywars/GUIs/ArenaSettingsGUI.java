package runi.myddns.mobarmywars.GUIs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import runi.myddns.mobarmywars.Managers.Event.ArenaConfig;
import runi.myddns.mobarmywars.Managers.World.ResumeManager;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.util.ArrayList;
import java.util.List;

public class ArenaSettingsGUI implements Listener {

    private final MobArmyMain plugin;
    private static final String TITLE = ChatColor.BLUE + "Arena-Einstellungen";

    public ArenaSettingsGUI(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        inv.setItem(11, createItem(
                Material.IRON_SWORD,
                ChatColor.DARK_RED + "Reset Arena",
                "",
                "Setzt die Arena komplett zurück"
        ));

//        inv.setItem(12, createItem(
//                Material.RESPAWN_ANCHOR,
//                ChatColor.GREEN + "WAVES neu starten",
//                "",
//                "Startet alle Arena-Waves neu"
//        ));

        inv.setItem(13, createItem(
                plugin.getWorldSettings().isArenaCompassEnabled() ? Material.COMPASS : Material.GRAY_DYE,
                ChatColor.AQUA + "Arena-Kompass",
                "",
                "Gibt allen Arena-Spielern einen Spezial-Kompass",
                plugin.getWorldSettings().isArenaCompassEnabled()
                        ? ChatColor.GREEN + "Status: AKTIV"
                        : ChatColor.DARK_RED + "Status: DEAKTIVIERT"
        ));

        inv.setItem(15, createItem(
                Material.COMPASS,
                ChatColor.AQUA + "Arena-Kompass geben",
                "",
                "Gibt allen Arena-Spielern den Arena-Kompass"
        ));

        addArenaButtons(inv);

        inv.setItem(49, createBackButton("Event-Einstellungen"));

        player.openInventory(inv);
    }

    private void addArenaButtons(Inventory inv) {
        List<ArenaConfig.ArenaInfo> arenas = plugin.getArenaConfig().getAvailableArenas();
        String activeArenaId = plugin.getArenaConfig().getActiveArenaId();

        int[] imageSlots = {29, 33};
        int[] buttonSlots = {38, 42};

        for (int i = 0; i < arenas.size() && i < 2; i++) {
            ArenaConfig.ArenaInfo arena = arenas.get(i);
            boolean active = arena.id().equalsIgnoreCase(activeArenaId);

            inv.setItem(imageSlots[i], createArenaPreviewItem(arena, active, i));
            inv.setItem(buttonSlots[i], createArenaSwitchButton(arena, active));
        }
    }

    private ItemStack createArenaPreviewItem(ArenaConfig.ArenaInfo arena, boolean active, int index) {
        Material material = switch (index) {
            case 0 -> Material.CHERRY_SAPLING;
            case 1 -> Material.SCULK_SENSOR;
            default -> Material.MAP;
        };

        return createItem(
                material,
                ChatColor.GOLD + arena.name(),
                "",
                arena.description(),
                "",
                active
                        ? ChatColor.GREEN + "Aktuell ausgewählt"
                        : ChatColor.RED + "Nicht ausgewählt"
        );
    }

    private ItemStack createArenaSwitchButton(ArenaConfig.ArenaInfo arena, boolean active) {
        return createItem(
                active ? Material.LIME_DYE : Material.RED_DYE,
                active
                        ? ChatColor.GREEN + "Aktiv: " + arena.name()
                        : ChatColor.RED + "Aktivieren: " + arena.name(),
                "",
                ChatColor.DARK_GRAY + "ID: " + arena.id(),
                "",
                active
                        ? ChatColor.GREEN + "Diese Arena ist aktuell aktiv."
                        : ChatColor.YELLOW + "Klicken, um diese Arena zu aktivieren."
        );
    }

    private ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        if (loreLines != null && loreLines.length > 0) {
            for (String line : loreLines) {
                lore.add(ChatColor.GRAY + line);
            }
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackButton(String ziel) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatColor.DARK_AQUA + "Zurück");
        meta.setLore(List.of());
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!ChatColor.stripColor(e.getView().getTitle()).equalsIgnoreCase("Arena-Einstellungen")) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        if (clicked.getItemMeta() == null || !clicked.getItemMeta().hasDisplayName()) return;

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        if (name.startsWith("Aktivieren: ")) {
            handleArenaSwitch(player, clicked);
            return;
        }

        if (name.startsWith("Aktiv: ")) {
            Sounds.playClick(player);
            player.sendMessage(ChatColor.YELLOW + "Diese Arena ist bereits aktiv.");
            return;
        }

        switch (name) {
            case "Zurück" -> {
                Sounds.playBack(player);
                plugin.getEventSettingsGUI().open(player);
            }

            case "Reset Arena" -> {
                Sounds.playDanger(player);
                plugin.getArenaManager().resetArena();

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(ChatColor.RED + "⏹ Arena wurde zurückgesetzt!");
                }
            }

//            case "WAVES neu starten" -> {
//                Sounds.playClick(player);
//
//                if (plugin.getEventResume().loadPhase() != ResumeManager.PHASE_ARENA) {
//                    player.sendMessage("");
//                    player.sendMessage(ChatColor.RED + "⚠ Das Event passt nicht zur aktuellen Phase.");
//                    player.sendMessage(ChatColor.GOLD + "Nutze /set phase arena");
//                    player.sendMessage("");
//                    return;
//                }
//
//                boolean hasTeamPlayer = Bukkit.getOnlinePlayers().stream().anyMatch(p -> {
//                    String team = plugin.getTeamManager().getPlayerTeam(p);
//                    return team != null && (team.equalsIgnoreCase("rot") || team.equalsIgnoreCase("blau"));
//                });
//
//                if (!hasTeamPlayer) {
//                    player.sendMessage(ChatColor.RED + "❗ Es muss mindestens ein Spieler in einem Team sein, um Waves zu starten!");
//                    Sounds.playClick(player);
//                    return;
//                }
//
//                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
//                    onlinePlayer.sendMessage(ChatColor.GREEN + "⚔️ Die Waves werden neu gestartet...");
//                    onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
//                }
//
//                player.closeInventory();
//
//                plugin.getTimerManager().stopTimer();
//                plugin.getArenaManager().startArenaEvent();
//                plugin.getTimerManager().updateBossBar(null);
//                plugin.getTimerManager().setForward(true);
//                plugin.getTimerManager().startTimer();
//            }

            case "Arena-Kompass" -> {
                Sounds.playClick(player);

                plugin.getWorldSettings().toggleArenaCompassEnabled();
                boolean enabled = plugin.getWorldSettings().isArenaCompassEnabled();

                if (enabled) {
                    player.sendMessage(ChatColor.GREEN + "✔ Arena-Kompass wurde aktiviert.");
                } else {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        plugin.getArenaCompassManager().removeArenaCompass(onlinePlayer);
                    }

                    player.sendMessage(ChatColor.RED + "✖ Arena-Kompass wurde deaktiviert.");
                }

                open(player);
            }

            case "Arena-Kompass geben" -> {
                Sounds.playClick(player);

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    plugin.getArenaCompassManager().addMonsterCompass(onlinePlayer);
                }

                player.sendMessage(ChatColor.GREEN + "✔ Arena-Kompass wurde vergeben.");
                open(player);
            }
        }
    }

    private void handleArenaSwitch(Player player, ItemStack clicked) {
        int phase = plugin.getEventResume().loadPhase();

        if (phase > ResumeManager.PHASE_WAVEAUSWAHL) {
            Sounds.playDanger(player);
            player.sendMessage(ChatColor.RED + "✖ Arena-Wechsel ist nur bis Phase 2 / Wave-Auswahl möglich.");
            return;
        }

        String arenaId = getArenaIdFromItem(clicked);

        if (arenaId == null || arenaId.isBlank()) {
            Sounds.playDanger(player);
            player.sendMessage(ChatColor.RED + "✖ Arena-ID konnte nicht gelesen werden.");
            return;
        }

        boolean success = plugin.getArenaConfig().setActiveArena(arenaId);

        if (!success) {
            Sounds.playDanger(player);
            player.sendMessage(ChatColor.RED + "✖ Arena konnte nicht aktiviert werden: " + arenaId);
            return;
        }

        Sounds.playClick(player);
        player.sendMessage(ChatColor.GREEN + "✔ Aktive Arena gesetzt: " + ChatColor.GOLD + arenaId);

        open(player);
    }

    private String getArenaIdFromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null || item.getItemMeta().getLore() == null) {
            return null;
        }

        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);

            if (clean.startsWith("ID: ")) {
                return clean.substring("ID: ".length()).trim();
            }
        }

        return null;
    }
}