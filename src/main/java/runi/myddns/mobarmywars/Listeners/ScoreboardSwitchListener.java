package runi.myddns.mobarmywars.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Managers.Event.ScoreboardSwitcher;

public class ScoreboardSwitchListener implements Listener {

    private final MobArmyMain plugin;

    public ScoreboardSwitchListener(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        var player = event.getPlayer();
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
        return worldName.equals("world_mobarmylobby");
    }

    private boolean isTeamBoardWorld(String worldName) {
        return worldName.equals("world_rot")
                || worldName.equals("world_blau")
                || worldName.equals("world_rot_nether")
                || worldName.equals("world_blau_nether");
    }
}