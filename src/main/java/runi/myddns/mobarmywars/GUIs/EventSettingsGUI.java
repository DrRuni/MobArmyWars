package runi.myddns.mobarmywars.GUIs;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import runi.myddns.mobarmywars.Managers.World.ResumeManager;
import runi.myddns.mobarmywars.Utils.Sounds;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Managers.Event.BlockRandomizerManager;
import runi.myddns.mobarmywars.Managers.World.WorldManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventSettingsGUI implements Listener {

    private final MobArmyMain plugin;
    private final BlockRandomizerManager blockRandomizerManager;

    private static final String TITLE = ChatColor.BLUE + "Event- & Welten Einstellungen";

    public EventSettingsGUI(MobArmyMain plugin, BlockRandomizerManager blockRandomizerManager) {
        this.plugin = plugin;
        this.blockRandomizerManager = blockRandomizerManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        boolean randomizerOn = blockRandomizerManager.isGlobalRandomizerEnabled();
        boolean keepInvOn = plugin.getWorldSettings().isKeepInventoryEnabled();

        inv.setItem(2, createItem(
                Material.IRON_SWORD,
                ChatColor.GOLD + "Arena-Einstellungen",
                "",
                ChatColor.GRAY + "Derzeit noch deaktiviert",
                ChatColor.YELLOW + "Verfügbar ab Version 1.2"
        ));

        inv.setItem(6, createItem(
                Material.ENDER_PEARL,
                ChatColor.GOLD + "Teleports",
                "",
                ChatColor.GRAY + "Teleport-Menü öffnen"
        ));

        inv.setItem(10, createItem(
                randomizerOn ? Material.LIME_WOOL : Material.RED_WOOL,
                (randomizerOn ? ChatColor.GREEN : ChatColor.RED)
                        + "BlockRandomizer: " + (randomizerOn ? "AN" : "AUS"),
                ChatColor.GRAY + "Globaler Block-Randomizer"
        ));

        inv.setItem(12, createItem(
                keepInvOn ? Material.LIME_WOOL : Material.RED_WOOL,
                (keepInvOn ? ChatColor.GREEN : ChatColor.RED)
                        + "Inventare behalten: " + (keepInvOn ? "AN" : "AUS"),
                ChatColor.GRAY + "KeepInventory in allen Welten"
        ));

        inv.setItem(14, createItem(
                Material.SPAWNER,
                ChatColor.GOLD + "Randomizer-Ausnahmen",
                "",
                ChatColor.GRAY + "Spawneier vom Zufall ausschließen"
        ));

        inv.setItem(16, createItem(
                Material.CREEPER_SPAWN_EGG,
                ChatColor.GOLD + "BlockRandomizer zurücksetzen",
                "",
                ChatColor.GRAY + "Erstellt einen neuen globalen Randomizer"
        ));

        inv.setItem(21, createItem(
                Material.PURPLE_BANNER,
                ChatColor.GOLD + "Team wählen",
                "",
                ChatColor.GRAY + "Weise dich einem Team zu"
        ));

        inv.setItem(31, createItem(
                Material.RESPAWN_ANCHOR,
                ChatColor.GOLD + "WAVES neu starten",
                "",
                ChatColor.GRAY + "Startet alle Arena-Waves neu"
        ));

        inv.setItem(23, createItem(
                Material.WHITE_BANNER,
                ChatColor.DARK_RED + "Teams zurücksetzen",
                "",
                ChatColor.RED + "Alle Spieler werden aus Teams entfernt"
        ));

        inv.setItem(37, createItem(
                Material.WITHER_SKELETON_SKULL,
                ChatColor.DARK_RED + "Reset Spielfortschritt",
                "",
                ChatColor.RED + "Setzt Arena & Spielstatus zurück"
        ));

        inv.setItem(40, createItem(
                Material.STRUCTURE_BLOCK,
                ChatColor.DARK_RED + "Reset Team-Welten",
                "",
                ChatColor.RED + "Welten Rot & Blau werden neu generiert"
        ));

        inv.setItem(43, createItem(
                Material.STRUCTURE_BLOCK,
                ChatColor.DARK_RED + "Reset Lobby-Welt",
                "",
                ChatColor.RED + "Lobby wird komplett zurückgesetzt"
        ));

        inv.setItem(49, createBackButton(ChatColor.DARK_AQUA + "Zurück"));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
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
        meta.setDisplayName("Zurück");
        meta.setLore(Arrays.asList());
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!ChatColor.stripColor(e.getView().getTitle()).equalsIgnoreCase("Event- & Welten Einstellungen")) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String itemName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        switch (itemName) {

//            case "Arena-Einstellungen" -> {
//                Sounds.playClick(player);
//                plugin.getArenaSettingsGUI().open(player);
//            }

            case "BlockRandomizer: AN", "BlockRandomizer: AUS" -> {
                Sounds.playClick(player);

                plugin.getWorldSettings().toggleRandomizer();

                boolean newState = plugin.getWorldSettings().isRandomizerEnabled();
                blockRandomizerManager.setGlobalRandomizerEnabled(newState);

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(
                            newState
                                    ? ChatColor.GREEN + "✅ BlockRandomizer aktiviert!"
                                    : ChatColor.RED + "⛔ BlockRandomizer deaktiviert!"
                    );
                }

                open(player);
            }

            case "Inventare behalten: AN", "Inventare behalten: AUS" -> {
                Sounds.playClick(player);

                plugin.getWorldSettings().toggleKeepInventory();

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(
                            plugin.getWorldSettings().isKeepInventoryEnabled()
                                    ? ChatColor.GREEN + "✅ KeepInventory aktiviert!"
                                    : ChatColor.RED + "⛔ KeepInventory deaktiviert!"
                    );
                }

                open(player);
            }

            case "Randomizer-Ausnahmen" -> {
                Sounds.playClick(player);
                plugin.getSpawnEggGUI().openGUI(player);
            }

            case "Team wählen" -> {
                Sounds.playClick(player);

                Bukkit.getOnlinePlayers().forEach(p -> {
                    plugin.getTeamSelectionGUI().openGUI(p);
                    Sounds.playClick(p);
                });

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

                    onlinePlayer.sendMessage("");
                    onlinePlayer.sendMessage(ChatColor.AQUA + "👥 " + ChatColor.GRAY + "Der Teamleiter hat die Teamauswahl geöffnet.");
                }
            }

            case "Teleports" -> {
                Sounds.playClick(player);
                plugin.getMobArmySettingsGUI().open(player);
            }

            case "WAVES neu starten" -> {
                Sounds.playClick(player);

                int currentPhase = plugin.getEventResume().loadPhase();

                if (currentPhase != ResumeManager.PHASE_ARENA) {
                    player.sendMessage(
                            ChatColor.DARK_RED + "✖ " +
                                    ChatColor.GRAY + "Waves können so nicht ausgelöst werden, " +
                                    "weil du muss erst in die Arena-Phase!"
                    );
                    return;
                }

                List<Player> arenaPlayers = Bukkit.getOnlinePlayers().stream()
                        .map(p -> (Player) p)
                        .filter(p -> List.of("world_mobarmylobby", "world_rot", "world_blau")
                                .contains(p.getWorld().getName().toLowerCase()))
                        .toList();

                boolean hasTeamPlayer = arenaPlayers.stream().anyMatch(p -> {
                    String team = plugin.getTeamManager().getPlayerTeam(p);
                    return team != null && (team.equalsIgnoreCase("rot") || team.equalsIgnoreCase("blau"));
                });

                if (!hasTeamPlayer) {
                    player.sendMessage(ChatColor.RED + "❗ Es muss mindestens ein Spieler in einem Team sein, um Waves zu starten!");
                    Sounds.playClick(player);
                    return;
                }

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(ChatColor.GREEN + "⚔️ Die Waves werden neu gestartet...");
                    onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                }
                player.closeInventory();

                plugin.getTimerManager().stopTimer();
                plugin.getArenaManager().teleportTeamsToArena();
                plugin.getArenaManager().teleportAndStart();
                plugin.getTimerManager().updateBossBar(null);
                plugin.getTimerManager().setForward(true);
                plugin.getTimerManager().startTimer();
            }

            case "Reset Team-Welten" -> {
                WorldManager wm = MobArmyMain.getInstance().getWorldManager();

                if (!wm.tryStartWorldReset()) {
                    player.sendMessage(ChatColor.RED + "⚠ Es läuft bereits ein Welt-Reset!");
                    Sounds.playDanger(player);
                    return;
                }

                Sounds.playReset(player);
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "⏳ Team-Welten werden neu generiert...");

                Bukkit.getScheduler().runTaskLater(
                        MobArmyMain.getInstance(),
                        () -> wm.resetTeamWorlds(),
                        80L
                );
            }

            case "Reset Lobby-Welt" -> {
                WorldManager wm = MobArmyMain.getInstance().getWorldManager();

                if (!wm.tryStartWorldReset()) {
                    player.sendMessage(ChatColor.RED + "⚠ Es läuft bereits ein Welt-Reset!");
                    Sounds.playDanger(player);
                    return;
                }

                Sounds.playReset(player);
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "⏳ Lobby-Welt wird neu geladen...");

                Bukkit.getScheduler().runTaskLater(
                        MobArmyMain.getInstance(),
                        () -> wm.resetLobbyWorld(),
                        80L
                );
            }

            case "BlockRandomizer zurücksetzen" -> {
                Sounds.playReset(player);
                blockRandomizerManager.resetRandomizer();

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(ChatColor.GREEN + "🔁 BlockRandomizer zurückgesetzt!");
                }

                open(player);
            }

            case "Reset Spielfortschritt" -> {
                player.closeInventory();

                plugin.getEventManager().resetGame(player);
            }

//            case "Reset Arena" -> {
//                Sounds.playDanger(player);
//                player.closeInventory();
//                plugin.getArenaManager().resetArena();
//
//                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
//                    onlinePlayer.sendMessage(ChatColor.RED + "⏹ Arena wurde zurückgesetzt!");
//                }
//            }

            case "Teams zurücksetzen" -> {
                Sounds.playReset(player);
                plugin.getTeamManager().resetTeams();

                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> List.of("world_mobarmylobby", "world_rot", "world_blau")
                                .contains(p.getWorld().getName().toLowerCase()))
                        .forEach(p -> plugin.getBundleManager().removeTeamBundle(p));

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(ChatColor.RED + "❌ Alle Teams wurden zurückgesetzt!");
                }

                Sounds.playClick(player);
            }

            case "Zurück" -> {
                Sounds.playBack(player);
                Sounds.playClick(player);
                plugin.getOptionenGUI().open(player);
            }
        }
    }
}