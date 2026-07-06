package runi.myddns.mobarmywars.Managers.World;

import org.bukkit.*;
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

    private static final String WORLD_LOBBY = "world_mobarmy_lobby";
    private static final String WORLD_ARENA = "world_mobarmy_arena";

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

        World pluginWorld = Bukkit.getWorld(WORLD_LOBBY);
        if (pluginWorld == null) {
            Bukkit.getLogger().warning("❗ " + WORLD_LOBBY + " ist nicht geladen!");
            return;
        }

        Location clicked = block.getLocation();
        Player player = event.getPlayer();
        String team = plugin.getTeamManager().getPlayerTeam(player);

        // Waveauswahl Mobs
        if (isButtonAt(clicked, WORLD_LOBBY, 104, 75, -95) || isButtonAt(clicked, WORLD_LOBBY, 75, 75, -95)) {
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
        if (handleReadyButton(player, clicked, "Rot", WORLD_LOBBY, 102, 75, -95, ChatColor.RED)) {
            Sounds.playClick(player);
            event.setCancelled(true);
            return;
        }

        // Waveauswahl Ready Blau
        if (handleReadyButton(player, clicked, "Blau", WORLD_LOBBY, 73, 75, -95, ChatColor.BLUE)) {
            Sounds.playClick(player);
            event.setCancelled(true);
            return;
        }

        // Team Auswahl
        if (isButtonAt(clicked, WORLD_LOBBY, 28, 65, 2)) {

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
        if (isButtonAt(clicked, WORLD_LOBBY, 28, 65, -4)) {

            if (!player.isOp()) {
                Message.sendToPlayer(player, ChatColor.RED + "❌ Nur Operatoren dürfen das Event starten!");
                event.setCancelled(true);
                return;
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getEventManager().enableEventHandling();
                    plugin.getEventManager().startEvent();
                    Sounds.playClick(player);
                }
            }.runTaskLater(plugin, 1L);

            event.setCancelled(true);
        }
    }

    private boolean handleReadyButton(Player player, Location clicked, String expectedTeam,
                                      String worldName, int x, int y, int z, ChatColor color) {

        if (plugin.getEventResume().loadPhase() != ResumeManager.PHASE_WAVEAUSWAHL) {
            player.sendMessage("");
            player.sendMessage(ChatColor.RED + "⚠ Das Event passt nicht zur aktuellen Phase.");
            player.sendMessage(ChatColor.GOLD + "nutze /set phase waveauswahl");
            player.sendMessage("");
            return false;
        }


        String playerTeam = plugin.getTeamManager().getPlayerTeam(player);
        if (playerTeam == null) return false;
        if (!playerTeam.equalsIgnoreCase(expectedTeam)) return false;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return false;

        if (clicked.getWorld().equals(world)
                && clicked.getBlockX() == x
                && clicked.getBlockY() == y
                && clicked.getBlockZ() == z) {

            plugin.getArenaManager().markPlayerReady(player);

            for (Player p : Bukkit.getOnlinePlayers()) {

                String w = p.getWorld().getName().toLowerCase();
                if (!(w.contains("mobarmy_lobby") || w.contains("rot") || w.contains("blau"))) {
                    continue;
                }

                p.sendTitle(
                        color + "✔ Team " + expectedTeam + " bereit!",
                        ChatColor.GRAY + player.getName() + " hat Ready gedrückt",
                        10, 40, 10
                );

                p.playSound(
                        p.getLocation(),
                        Sound.BLOCK_NOTE_BLOCK_PLING,
                        1f,
                        1.4f
                );
            }

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