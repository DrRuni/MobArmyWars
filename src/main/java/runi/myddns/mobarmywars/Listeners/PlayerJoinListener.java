package runi.myddns.mobarmywars.Listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import runi.myddns.mobarmywars.Managers.Event.TimerManager;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.GradientText;
import runi.myddns.mobarmywars.Managers.World.TeleportManager;

public class PlayerJoinListener implements Listener {

    private final MobArmyMain plugin;

    public PlayerJoinListener(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    private static final String RESOURCE_PACK_URL =
            "https://github.com/DrRuni/MobArmyWars/releases/download/v1.7/MobArmyWarsRP.zip";

    private static final String RESOURCE_PACK_SHA1 =
            "08509a806eaa15ad2bb206d990b2a3b462532d22";

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            Component prompt = Component.text()
                    .append(
                            Component.text("MobArmyWars\n")
                                    .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
                    )
                    .append(
                            Component.text("Lade das Ressourcenpaket für ")
                                    .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                    )
                    .append(
                            Component.text("Scoreboard-Grafiken und Icons.")
                                    .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
                    )
                    .build();

            player.setResourcePack(
                    RESOURCE_PACK_URL,
                    RESOURCE_PACK_SHA1,
                    false,
                    prompt
            );
        }, 20L);

        Bukkit.getScheduler().runTask(plugin, () -> {

            boolean restored = plugin.getEventResume().restorePlayerPosition(player);

            if (!restored) {
                TeleportManager.teleport(player, "world_mobarmy_lobby");
            }

            plugin.getPlayerEffectManager().applyNightVision(player);

            showWelcomeSequence(player);
            showProjectNotice(player);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                plugin.getTimerManager().ensureBossBarExists();
                plugin.getTimerManager().addPlayerToBossBar(player);
                plugin.getTimerManager().updatePauseState();

                plugin.getTeamScoreboardManager().updateBoard();

                for (Player online : Bukkit.getOnlinePlayers()) {
                    plugin.getScoreboardSwitcher().switchToTeam(online);
                }
            }, 20L * 7);
        });

        String language = plugin.getConfig().getString("language");

        if (player.isOp() && (language == null || language.isBlank())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {

                if (!player.isOnline()) return;

                plugin.getLanguageSelectionGUI().open(player);

            }, 190L);
        }
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
                    plugin.getLanguageManager().get(
                            "player-join-listener.welcome.title"
                    ),
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

                Component zuSubtitle = Component.text(
                        plugin.getLanguageManager().get(
                                "player-join-listener.welcome.to"
                        )
                ).color(
                        net.kyori.adventure.text.format.TextColor.color(
                                210, 210, 210
                        )
                );

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

                Component subtitle = Component.empty();

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

    public void showProjectNotice(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            Component clickText = plugin.getLanguageManager()
                    .getComponent("player-join-listener.notice.line-1")
                    .clickEvent(
                            ClickEvent.callback(audience -> {
                                if (!(audience instanceof Player clickedPlayer)) {
                                    return;
                                }

                                clickedPlayer.sendMessage(Component.empty());

                                clickedPlayer.sendMessage(
                                        plugin.getLanguageManager().getComponent(
                                                "player-join-listener.notice.details-1"
                                        )
                                );

                                clickedPlayer.sendMessage(
                                        plugin.getLanguageManager().getComponent(
                                                "player-join-listener.notice.details-2"
                                        )
                                );

                                clickedPlayer.sendMessage(
                                        plugin.getLanguageManager().getComponent(
                                                "player-join-listener.notice.details-3"
                                        )
                                );

                                clickedPlayer.sendMessage(
                                        plugin.getLanguageManager().getComponent(
                                                "player-join-listener.notice.details-4"
                                        )
                                );

                                clickedPlayer.sendMessage(Component.empty());
                            })
                    )
                    .hoverEvent(
                            HoverEvent.showText(
                                    plugin.getLanguageManager().getComponent(
                                            "player-join-listener.notice.hover"
                                    )
                            )
                    );

            player.sendMessage(Component.empty());
            player.sendMessage(clickText);
            player.sendMessage(Component.empty());

        }, 100L);
    }
}