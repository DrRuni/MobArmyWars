package runi.myddns.mobarmywars.Listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import runi.myddns.mobarmywars.Managers.Event.TimerManager;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.GradientText;
import runi.myddns.mobarmywars.Managers.World.TeleportManager;

public class PlayerJoinListener implements Listener {

    private final MobArmyMain plugin;

    public PlayerJoinListener(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {

            boolean restored = plugin.getEventResume().restorePlayerPosition(player);

            if (!restored) {
                TeleportManager.teleport(player, "world_mobarmylobby");
            }

            plugin.getPlayerEffectManager().applyNightVision(player);

            showWelcomeSequence(player);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                plugin.getTimerManager().addPlayerToBossBar(player);

                String team = plugin.getTeamManager().getPlayerTeam(player);
                if (!team.equalsIgnoreCase("Kein Team")) {
                    plugin.getTeamScoreboardManager().addPlayerToTeam(player, team);
                }

                plugin.getTeamManager().loadTeams();
                plugin.getTeamScoreboardManager().rebuildBoard();
                plugin.getScoreboardSwitcher().switchToTeam(player);

            }, 20L * 7);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.getEventResume().savePlayerLastLocation(player);
        plugin.getScoreboardSwitcher().removePlayer(player);

        TimerManager timer = plugin.getTimerManager();
        if (timer != null) {
            timer.removeBossBarFor(player);
        }
    }

    private void showWelcomeSequence(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.15f);

            Component welcomeTitle = GradientText.gradient(
                    "Willkommen",
                    220, 50, 50,
                    50, 110, 255
            );

            player.showTitle(net.kyori.adventure.title.Title.title(
                    welcomeTitle,
                    Component.empty(),
                    net.kyori.adventure.title.Title.Times.times(
                            java.time.Duration.ofMillis(300),
                            java.time.Duration.ofSeconds(2),
                            java.time.Duration.ofMillis(300)
                    )
            ));

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                Component zuSubtitle = Component.text("zu")
                        .color(net.kyori.adventure.text.format.TextColor.color(210, 210, 210));

                player.showTitle(net.kyori.adventure.title.Title.title(
                        welcomeTitle,
                        zuSubtitle,
                        net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ofMillis(200),
                                java.time.Duration.ofSeconds(1),
                                java.time.Duration.ofMillis(200)
                        )
                ));
            }, 30L);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.1f);

                Component mainTitle = GradientText.gradient(
                        "MobArmyWars",
                        220, 50, 50,
                        50, 110, 255
                );

                Component subtitle = GradientText.gradient(
                        "v1.1",
                        160, 60, 60,
                        60, 120, 220
                );

                player.showTitle(net.kyori.adventure.title.Title.title(
                        mainTitle,
                        subtitle,
                        net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ofMillis(300),
                                java.time.Duration.ofSeconds(2),
                                java.time.Duration.ofMillis(500)
                        )
                ));
            }, 60L);
        }, 20L);
    }
}