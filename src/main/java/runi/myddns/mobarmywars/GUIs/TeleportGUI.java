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
import runi.myddns.mobarmywars.Managers.World.TeleportManager;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.ArrayList;
import java.util.List;

public class TeleportGUI implements Listener {

    private final MobArmyMain plugin;
    private static final String TITLE = ChatColor.BLUE + "Teleports für MobArmyWars";

    public TeleportGUI(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);

        inv.setItem(10, createItem(Material.ENDER_PEARL,
                "Ich → Lobby",
                "",
                "Teleportiert nur dich"));

        inv.setItem(12, createItem(Material.ENDER_PEARL,
                "Ich → Teamwelt",
                "",
                "Teleportiert nur dich"));

        inv.setItem(14, createItem(Material.ENDER_PEARL,
                "Ich → Waveauswahl",
                "",
                "Teleportiert nur dich"));

        inv.setItem(16, createItem(Material.ENDER_PEARL,
                "Ich → Arena",
                "",
                "Teleportiert nur dich"));

        inv.setItem(28, createItem(Material.END_CRYSTAL,
                "Alle → Lobby",
                "",
                "Teleportiert ALLE Spieler &",
                ChatColor.DARK_RED + "Phase Lobby wird gesetzt!"));

        inv.setItem(30, createItem(Material.END_CRYSTAL,
                "Alle → Teamwelt",
                "",
                "Teleportiert ALLE Spieler &",
                ChatColor.DARK_RED + "Phase Teamwelt wird gesetzt!"));

        inv.setItem(32, createItem(Material.END_CRYSTAL,
                "Alle → Waveauswahl",
                "",
                "Teleportiert ALLE Spieler &",
                ChatColor.DARK_RED + "Phase Waveauswahl wird gesetzt!"));

        inv.setItem(34, createItem(Material.END_CRYSTAL,
                "Alle → Arena",
                "",
                "Teleportiert ALLE Spieler &",
                ChatColor.DARK_RED + "Phase Arena wird gesetzt!"));


        inv.setItem(40, createBackButton("Event-Einstellungen"));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + name);

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
        meta.setDisplayName(ChatColor.DARK_AQUA + "Zurück");
        meta.setLore(List.of());
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!ChatColor.stripColor(e.getView().getTitle()).equalsIgnoreCase("Teleports für MobArmyWars")) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        switch (name) {

            case "Zurück" -> {
                Sounds.playBack(player);
                plugin.getEventSettingsGUI().open(player);
            }


            case "Ich → Lobby" -> {
                TeleportManager.teleport(player, "world_mobarmy_lobby");
                Sounds.playTeleport(player);

                player.closeInventory();

                player.sendMessage(ChatColor.GREEN + "✔ Du wurdest zur Lobby teleportiert!");
            }

            case "Alle → Lobby" -> {
                Sounds.playClick(player);

                boolean teleportedAnyone = false;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    TeleportManager.teleport(p, "world_mobarmy_lobby");
                    Sounds.playTeleport(p);
                    teleportedAnyone = true;
                }

                if (!teleportedAnyone) {
                    player.sendMessage(ChatColor.RED + "❗ Kein Spieler gefunden!");
                    return;
                }

                plugin.getEventResume().savePhase(ResumeManager.PHASE_LOBBY);

                player.closeInventory();

                Bukkit.broadcastMessage(ChatColor.GREEN + "Alle Spieler wurden zur Lobby teleportiert!");
            }

            case "Ich → Teamwelt" -> {
                String team = plugin.getTeamManager().getPlayerTeam(player);
                if (team == null || team.equalsIgnoreCase("Kein Team")) {
                    player.sendMessage(ChatColor.RED + "❗ Du musst einem Team angehören!");
                    return;
                }

                String worldName = team.equalsIgnoreCase("Rot") ? "world_rot" : "world_blau";
                TeleportManager.teleport(player, worldName);
                Sounds.playTeleport(player);

                player.closeInventory();

                player.sendMessage(ChatColor.GREEN + "🌍 Du wurdest in deine Teamwelt teleportiert!");
            }

            case "Alle → Teamwelt" -> {
                Sounds.playClick(player);

                boolean teleportedAnyone = false;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    String team = plugin.getTeamManager().getPlayerTeam(p);

                    if (team == null || team.equalsIgnoreCase("Kein Team")) {
                        p.sendMessage(ChatColor.DARK_RED + "✖ " + ChatColor.GRAY +
                                "Du wurdest nicht teleportiert, da du keinem Team angehörst.");
                        continue;
                    }

                    if (team.equalsIgnoreCase("Rot")) {
                        TeleportManager.teleport(p, "world_rot");
                    } else if (team.equalsIgnoreCase("Blau")) {
                        TeleportManager.teleport(p, "world_blau");
                    } else {
                        p.sendMessage(ChatColor.RED +
                                "❌ Du konntest nicht teleportiert werden (ungültiges Team).");
                        continue;
                    }

                    Sounds.playTeleport(p);

                    p.sendMessage(ChatColor.GREEN +
                            "🌍 Du wurdest in deine Teamwelt teleportiert!");
                    teleportedAnyone = true;
                }

                if (!teleportedAnyone) {
                    player.sendMessage(ChatColor.RED +
                            "❗ Kein Spieler mit gültigem Team gefunden!");
                    return;
                }

                player.closeInventory();

                plugin.getEventResume().savePhase(ResumeManager.PHASE_TEAMWELT);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage("");
                    p.sendMessage(ChatColor.GOLD + "📢 Der Teamleiter hat alle Team-Spieler teleportiert.");
                }
            }

            case "Ich → Waveauswahl" -> {
                String team = plugin.getTeamManager().getPlayerTeam(player);
                if (team == null || team.equalsIgnoreCase("Kein Team")) {
                    player.sendMessage(ChatColor.RED + "❗ Du musst einem Team angehören!");
                    return;
                }

                TeleportManager.teleportToWaveSelection(player);
                Sounds.playTeleport(player);

                plugin.getTimerManager().stopTimer();
                plugin.getTimerManager().updateBossBar(null);
                plugin.getTimerManager().setForward(true);
                plugin.getTimerManager().startTimer();

                plugin.getEventResume().savePlayerSpawn(player, player.getLocation());
                player.closeInventory();
                plugin.getArenaCompassManager().giveMonsterCompass(player);

                player.sendMessage(ChatColor.GREEN + "🚀 Du wurdest zur Wavekonfiguration teleportiert!");
            }

            case "Alle → Waveauswahl" -> {
                Sounds.playClick(player);

                boolean teleportedAnyone = false;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    String team = plugin.getTeamManager().getPlayerTeam(p);

                    if (team == null || team.equalsIgnoreCase("Kein Team")) {
                        p.sendMessage(ChatColor.DARK_RED + "✖ " + ChatColor.GRAY +
                                "Du wurdest nicht teleportiert, da du keinem Team angehörst.");
                        continue;
                    }

                    TeleportManager.teleportToWaveSelection(p);
                    Sounds.playTeleport(p);

                    plugin.getArenaCompassManager().giveMonsterCompass(p);

                    p.sendMessage(
                            ChatColor.GREEN + "🚀 " +
                                    ChatColor.GRAY + "Du wurdest zur Wavekonfiguration teleportiert!"
                    );

                    plugin.getEventResume().savePlayerSpawn(p, p.getLocation());
                    teleportedAnyone = true;
                }

                if (!teleportedAnyone) {
                    player.sendMessage(ChatColor.RED +
                            "❗ Kein Spieler mit gültigem Team gefunden!");
                    return;
                }

                plugin.getTimerManager().stopTimer();
                plugin.getTimerManager().updateBossBar(null);
                plugin.getTimerManager().setForward(true);
                plugin.getTimerManager().startTimer();

                player.closeInventory();

                plugin.getEventResume().savePhase(ResumeManager.PHASE_WAVEAUSWAHL);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage("");
                    p.sendMessage(ChatColor.GRAY + "📢 Der Teamleiter hat alle Team-Spieler zur Waveauswahl teleportiert.");
                }
            }

            case "Ich → Arena" -> {
                String team = plugin.getTeamManager().getPlayerTeam(player);
                if (team == null || team.equalsIgnoreCase("Kein Team")) {
                    player.sendMessage(ChatColor.RED + "❗ Du musst einem Team angehören!");
                    return;
                }

                TeleportManager.teleportToArena(player);
                Sounds.playTeleport(player);

                plugin.getEventResume().savePlayerSpawn(player, player.getLocation());

                player.sendMessage(ChatColor.GREEN + "🚀 Du wurdest zur Arena teleportiert!");

                player.closeInventory();
                plugin.getArenaCompassManager().giveMonsterCompass(player);
            }

            case "Alle → Arena" -> {
                Sounds.playClick(player);

                boolean teleportedAnyone = false;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    String team = plugin.getTeamManager().getPlayerTeam(p);

                    if (team == null || team.equalsIgnoreCase("Kein Team")) {
                        p.sendMessage(ChatColor.DARK_RED + "✖ " + ChatColor.GRAY +
                                "Du wurdest nicht teleportiert, da du keinem Team angehörst.");
                        continue;
                    }

                    TeleportManager.teleportToArena(p);
                    Sounds.playTeleport(p);

                    plugin.getEventResume().savePlayerSpawn(p, p.getLocation());
                    plugin.getArenaCompassManager().giveMonsterCompass(p);

                    p.sendMessage(ChatColor.GREEN +
                            "🚀 Du wurdest zur Arena teleportiert!");
                    teleportedAnyone = true;
                }

                if (!teleportedAnyone) {
                    player.sendMessage(ChatColor.RED +
                            "❗ Kein Spieler mit gültigem Team gefunden!");
                    return;
                }

                player.closeInventory();

                plugin.getEventResume().savePhase(ResumeManager.PHASE_ARENA);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage("");
                    p.sendMessage(ChatColor.GRAY + "📢 Der Teamleiter hat alle Team-Spieler zur Arena teleportiert.");
                }
            }
        }
    }
}