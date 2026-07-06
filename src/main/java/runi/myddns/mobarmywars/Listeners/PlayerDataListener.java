package runi.myddns.mobarmywars.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import runi.myddns.mobarmywars.Managers.TimerManager;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Managers.ScoreboardController;
import runi.myddns.mobarmywars.World.TeleportManager;

public class PlayerDataListener implements Listener {

    private final MobArmyMain plugin;

    public PlayerDataListener(MobArmyMain plugin) {
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

            plugin.getTimerManager().addPlayerToBossBar(player);

            plugin.getTeamManager().enableTeamScoreboard();
            player.setScoreboard(plugin.getTeamManager().getScoreboard());

            plugin.getScoreboardController()
                    .setBoard(player, ScoreboardController.BoardType.TEAM);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        TimerManager timer = plugin.getTimerManager();
        if (timer != null) {
            timer.removeBossBarFor(player);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
    }
}
