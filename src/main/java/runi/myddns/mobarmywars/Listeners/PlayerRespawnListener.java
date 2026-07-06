package runi.myddns.mobarmywars.Listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import runi.myddns.mobarmywars.MobArmyMain;

public class PlayerRespawnListener implements Listener {

    private final MobArmyMain plugin;

    public PlayerRespawnListener(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {

        Player player = e.getPlayer();

        Location vanillaRespawn = e.getRespawnLocation();
        if (vanillaRespawn != null
                && vanillaRespawn.getWorld() != null
                && vanillaRespawn.getWorld().getEnvironment() == World.Environment.NETHER) {
            return;
        }

        String team = plugin.getTeamManager().getPlayerTeam(player);
        String teamWorldName = null;

        if ("rot".equalsIgnoreCase(team)) {
            teamWorldName = "world_rot";
        } else if ("blau".equalsIgnoreCase(team)) {
            teamWorldName = "world_blau";
        }

        if (teamWorldName != null) {
            Location bed = player.getBedSpawnLocation();
            if (bed != null
                    && bed.getWorld() != null
                    && bed.getWorld().getName().equalsIgnoreCase(teamWorldName)) {

                e.setRespawnLocation(bed);
                return;
            }
        }

        Location eventSpawn = plugin.getEventResume().getSavedSpawn(player);
        if (eventSpawn != null && eventSpawn.getWorld() != null) {
            e.setRespawnLocation(eventSpawn);
        }

    }
}