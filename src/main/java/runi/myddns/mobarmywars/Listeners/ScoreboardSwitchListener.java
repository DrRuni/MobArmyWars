package runi.myddns.mobarmywars.Listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import runi.myddns.mobarmywars.MobArmyMain;

public class ScoreboardSwitchListener implements Listener {

    private final MobArmyMain plugin;

    public ScoreboardSwitchListener(MobArmyMain plugin) {
        this.plugin = plugin;
    }

//    @EventHandler
//    public void onJoin(PlayerJoinEvent event) {
//        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
//            switchScoreboard(event.getPlayer());
//        }, 1L);
//    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            switchScoreboard(player);
        }, 1L);
    }

    private void switchScoreboard(Player player) {
        String worldName = player.getWorld().getName().toLowerCase();

        if (isArenaBoardWorld(worldName)) {
            plugin.getScoreboardSwitcher().switchToArena(player);
            return;
        }

        if (isTeamBoardWorld(worldName)) {
            plugin.getScoreboardSwitcher().switchToTeam(player);
        }
    }

    private boolean isArenaBoardWorld(String worldName) {
        return worldName.equals("world_mobarmy_arena");
    }

    private boolean isTeamBoardWorld(String worldName) {
        return worldName.equals("world_mobarmy_lobby")
                || worldName.equals("world_rot")
                || worldName.equals("world_blau");
    }
}