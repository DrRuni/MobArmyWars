package runi.myddns.mobarmywars.Listeners;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import runi.myddns.mobarmywars.MobArmyMain;

public class ScoreboardAreaListener implements Listener {

    private final MobArmyMain plugin;

    public ScoreboardAreaListener(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        var player = e.getPlayer();
        var buildProt = plugin.getArenaBuildProtectionManager();
        var controller = plugin.getScoreboardController();

        var loc = player.getLocation();

        var arena = buildProt.getArenaByLocation(loc);
        boolean insideArenaArea = (arena != null);

        if (insideArenaArea) {
            controller.setArenaBoard(player);
        } else {
            controller.setTeamBoard(player);
        }
    }
}
