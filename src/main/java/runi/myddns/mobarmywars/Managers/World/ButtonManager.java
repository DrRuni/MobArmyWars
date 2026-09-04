package runi.myddns.mobarmywars.Managers.World;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.Component;
import runi.myddns.mobarmywars.GUIs.UnifiedWaveGUI;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.time.Duration;
import java.util.Locale;

public class ButtonManager implements Listener {

    private static final String WORLD_LOBBY = "world_mobarmy_lobby";

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
            plugin.getLogger().warning(
                    "❗ " + WORLD_LOBBY + " ist nicht geladen!"
            );
            return;
        }

        Location clicked = block.getLocation();
        Player player = event.getPlayer();

        // Team Auswahl
        if (isButtonAt(clicked, 28, 65, 1)) {

            if (plugin.getEventResume().loadPhase() != ResumeManager.PHASE_LOBBY) {
                player.sendMessage(Component.empty());
                player.sendMessage(lang("button-manager.team-selection-only-lobby"));
                player.sendMessage(lang("button-manager.use-phase-lobby"));
                player.sendMessage(Component.empty());
                event.setCancelled(true);
                return;
            }

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

        // START EVENT
        if (isButtonAt(clicked, 28, 65, -5)) {

            if (!player.isOp()) {
                player.sendMessage(
                        lang("button-manager.operators-only")
                );
                event.setCancelled(true);
                return;
            }

            if (plugin.getEventResume().loadPhase() != ResumeManager.PHASE_LOBBY) {
                player.sendMessage(Component.empty());
                player.sendMessage(lang("button-manager.event-only-lobby"));
                player.sendMessage(lang("button-manager.use-phase-lobby"));
                player.sendMessage(Component.empty());
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
            return;
        }

        // Waveauswahl GUI
        if (isButtonAt(clicked, 104, 75, -95) || isButtonAt(clicked, 75, 75, -95)) {

            if (plugin.getEventResume().loadPhase() != ResumeManager.PHASE_WAVEAUSWAHL) {
                player.sendMessage(Component.empty());
                player.sendMessage(lang("button-manager.wave-selection-only-phase"));
                player.sendMessage(lang("button-manager.use-phase-wave-selection"));
                player.sendMessage(Component.empty());
                event.setCancelled(true);
                return;
            }

            String team = plugin.getTeamManager().getPlayerTeam(player);

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
        if (handleReadyButton(
                player,
                clicked,
                "Rot",
                102
        )) {
            Sounds.playClick(player);
            event.setCancelled(true);
            return;
        }

        // Waveauswahl Ready Blau
        if (handleReadyButton(
                player,
                clicked,
                "Blau",
                73
        )) {
            Sounds.playClick(player);
            event.setCancelled(true);
        }
    }

    private boolean handleReadyButton(
            Player player,
            Location clicked,
            String expectedTeam,
            int x
    ) {

        if (!isButtonAt(
                clicked,
                x,
                75,
                -95
        )) {
            return false;
        }

        if (plugin.getEventResume().loadPhase() != ResumeManager.PHASE_WAVEAUSWAHL) {
            player.sendMessage(Component.empty());
            player.sendMessage(lang("button-manager.ready-only-phase"));
            player.sendMessage(lang("button-manager.use-phase-wave-selection"));
            player.sendMessage(Component.empty());
            return true;
        }

        String playerTeam = plugin.getTeamManager().getPlayerTeam(player);
        if (playerTeam == null) {
            player.sendMessage(
                    lang("button-manager.no-team")
            );
            return true;
        }

        Component teamDisplay =
                expectedTeam.equalsIgnoreCase("Rot")
                        ? plugin.getLanguageManager().getComponent(
                        "arena-event-manager.team.red"
                )
                        : plugin.getLanguageManager().getComponent(
                        "arena-event-manager.team.blue"
                );

        if (!playerTeam.equalsIgnoreCase(expectedTeam)) {
            player.sendMessage(
                    plugin.getLanguageManager().getComponent(
                            "button-manager.wrong-ready-team",
                            "team",
                            teamDisplay
                    )
            );
            return true;
        }

        plugin.getArenaManager().markPlayerReady(player);

        NamedTextColor titleColor =
                expectedTeam.equalsIgnoreCase("Rot")
                        ? NamedTextColor.RED
                        : NamedTextColor.BLUE;

        for (Player p : Bukkit.getOnlinePlayers()) {

            String w =
                    p.getWorld()
                            .getName()
                            .toLowerCase(Locale.ROOT);

            if (!(w.contains("mobarmy_lobby")
                    || w.contains("rot")
                    || w.contains("blau"))) {
                continue;
            }

            p.showTitle(
                    Title.title(
                            plugin.getLanguageManager()
                                    .getComponent(
                                            "button-manager.ready.title",
                                            "team",
                                            teamDisplay
                                    )
                                    .color(titleColor),

                            plugin.getLanguageManager()
                                    .getComponent(
                                            "button-manager.ready.subtitle",
                                            "player",
                                            player.getName()
                                    ),

                            Title.Times.times(
                                    Duration.ofMillis(500),
                                    Duration.ofSeconds(2),
                                    Duration.ofMillis(500)
                            )
                    )
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

    private boolean isButtonAt(Location loc, int x, int y, int z) {
        World world = Bukkit.getWorld(WORLD_LOBBY);

        return world != null
                && loc.getWorld().equals(world)
                && loc.getBlockX() == x
                && loc.getBlockY() == y
                && loc.getBlockZ() == z;
    }

    private Component lang(String path) {
        return plugin.getLanguageManager()
                .getComponent(path);
    }
}