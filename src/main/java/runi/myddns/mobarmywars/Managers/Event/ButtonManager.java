package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import runi.myddns.mobarmywars.GUIs.UnifiedWaveGUI;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Message;
import runi.myddns.mobarmywars.Utils.Sounds;

public class ButtonManager implements Listener {

    private final MobArmyMain plugin;

    public ButtonManager(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        if (!event.hasBlock()) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        if (!block.getType().name().contains("BUTTON")) return;

        World pluginWorld = Bukkit.getWorld("world_mobarmylobby");
        if (pluginWorld == null) {
            Bukkit.getLogger().warning("❗ world_mobarmylobby ist nicht geladen!");
            return;
        }

        Location clicked = block.getLocation();
        Player player = event.getPlayer();
        String team = plugin.getTeamManager().getPlayerTeam(player);

        // Waveauswahl Mobs
        if (isButtonAt(clicked, "world_mobarmylobby", 104, 75, -95) || isButtonAt(clicked, "world_mobarmylobby", 75, 75, -95)) {
            Sounds.playClick(player);
            new BukkitRunnable() {
                @Override
                public void run() {
                    new UnifiedWaveGUI(
                            plugin.getWaveManager(),
                            plugin.getMobSaveManager(),
                            player,
                            team
                    );
                }
            }.runTaskLater(plugin, 1L);
            event.setCancelled(true);
            return;
        }

        // Waveauswahl Ready Rot
        if (handleReadyButton(player, clicked, "Rot", "world_mobarmylobby", 102, 75, -95, ChatColor.RED)) {
            Sounds.playClick(player);
            event.setCancelled(true);
            return;
        }

        // Waveauswahl Ready Blau
        if (handleReadyButton(player, clicked, "Blau", "world_mobarmylobby", 73, 75, -95, ChatColor.BLUE)) {
            Sounds.playClick(player);
            event.setCancelled(true);
        }

        // Team Auswahl
        if (isButtonAt(clicked, "world_mobarmylobby", 28, 65, 2)) {

            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getTeamSelectionGUI().openGUI(player);
                    Sounds.playClick(player);
                }
            }.runTaskLater(plugin, 1L);

            event.setCancelled(true);
            return;
        }

        // ▶ START EVENT
        if (isButtonAt(clicked, "world_mobarmylobby", 28, 65, -4)) {

            if (!player.isOp()) {
                Message.sendToPlayer(player,ChatColor.RED + "❌ Nur Operatoren dürfen das Event starten!");
                event.setCancelled(true);
                return;
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getEventManager().enableEventHandling();
                    plugin.getEventManager().startCountdown();
                    Sounds.playClick(player);
                }
            }.runTaskLater(plugin, 1L);

            event.setCancelled(true);
            return;
        }

        // ⏫ TIMER +1 STUNDE
        if (isButtonAt(clicked, "world_mobarmylobby", 0, 67, -3)) {

            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getTimerManager().addTime(3600);
                }
            }.runTaskLater(plugin, 1L);

            event.setCancelled(true);
            return;
        }

        // ⏬ TIMER -1 STUNDE
        if (isButtonAt(clicked, "world_mobarmylobby", 0, 66, -3)) {

            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getTimerManager().removeTime(3600);
                }
            }.runTaskLater(plugin, 1L);

            event.setCancelled(true);
            return;
        }

        // ⏫ TIMER +10 MINUTEN
        if (isButtonAt(clicked, "world_mobarmylobby", 0, 67, 0)) {

            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getTimerManager().addTime(600);
                }
            }.runTaskLater(plugin, 1L);

            event.setCancelled(true);
            return;
        }

        // ⏬ TIMER -10 MINUTEN
        if (isButtonAt(clicked, "world_mobarmylobby", 0, 66, 0)) {

            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getTimerManager().removeTime(600);
                }
            }.runTaskLater(plugin, 1L);

            event.setCancelled(true);
            return;
        }
    }

    private boolean handleReadyButton(Player player, Location clicked, String expectedTeam,
                                      String worldName, int x, int y, int z, ChatColor color) {

        String playerTeam = plugin.getTeamManager().getPlayerTeam(player);
        if (playerTeam == null) return false;
        if (!playerTeam.equalsIgnoreCase(expectedTeam)) return false;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return false;

        if (clicked.getWorld().equals(world)
                && clicked.getBlockX() == x
                && clicked.getBlockY() == y
                && clicked.getBlockZ() == z) {

            for (Player p : Bukkit.getOnlinePlayers()) {

                String team = plugin.getTeamManager().getPlayerTeam(p);

                if (team != null && team.equalsIgnoreCase(expectedTeam)) {

                    String w = p.getWorld().getName().toLowerCase();
                    if (!(w.contains("mobarena") || w.contains("rot") || w.contains("blau")))
                        continue;

                    p.sendMessage(color + "✔ " + player.getName() + " hat sich als BEREIT markiert!");
                }
            }

            plugin.getArenaManager().markPlayerReady(player);
            return true;
        }
        return false;
    }

    private boolean isButtonAt(Location loc, String worldName, int x, int y, int z) {
        World world = Bukkit.getWorld(worldName);
        return world != null && loc.getWorld().equals(world)
                && loc.getBlockX() == x && loc.getBlockY() == y && loc.getBlockZ() == z;
    }
}