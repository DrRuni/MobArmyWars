package runi.myddns.mobarmywars.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.concurrent.ThreadLocalRandom;

public class PlayerRespawnListener implements Listener {

    private final MobArmyMain plugin;

    public PlayerRespawnListener(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getPlayerEffectManager().applyNightVision(player);
        });

        Location deathLocation = player.getLocation();
        World deathWorld = deathLocation.getWorld();

        if (deathWorld == null) return;

        String deathWorldName = deathWorld.getName();

        String team = plugin.getTeamManager().getPlayerTeam(player);
        String teamWorldName = getTeamWorldName(team);

        // 1. Tod in der Lobby:
        if (deathWorldName.equalsIgnoreCase("world_mobarmylobby")) {
            Location eventSpawn = plugin.getEventResume().getSavedSpawn(player);
            if (eventSpawn != null && eventSpawn.getWorld() != null) {
                e.setRespawnLocation(eventSpawn);
                return;
            }

            Location lobbySpawn = getLobbySpawn();
            if (lobbySpawn != null) {
                e.setRespawnLocation(lobbySpawn);
            }
            return;
        }

        // 2. Tod im Nether:
        if (deathWorld.getEnvironment() == World.Environment.NETHER) {
            Location vanillaRespawn = e.getRespawnLocation();

            if (vanillaRespawn != null
                    && vanillaRespawn.getWorld() != null
                    && vanillaRespawn.getWorld().getEnvironment() == World.Environment.NETHER) {
                return;
            }

            Location teamBed = getTeamBedSpawn(player, teamWorldName);
            if (teamBed != null) {
                e.setRespawnLocation(teamBed);
                return;
            }

            Location teamWorldSpawn = getTeamWorldSpawn(teamWorldName);
            if (teamWorldSpawn != null) {
                e.setRespawnLocation(teamWorldSpawn);
                return;
            }
        }

        // 3. Tod in der Teamwelt:
        Location teamBed = getTeamBedSpawn(player, teamWorldName);
        if (teamBed != null) {
            e.setRespawnLocation(teamBed);
            return;
        }

        Location teamWorldSpawn = getTeamWorldSpawn(teamWorldName);
        if (teamWorldSpawn != null) {
            e.setRespawnLocation(teamWorldSpawn);
        }
    }

    private String getTeamWorldName(String team) {
        if ("rot".equalsIgnoreCase(team)) {
            return "world_rot";
        } else if ("blau".equalsIgnoreCase(team)) {
            return "world_blau";
        }
        return null;
    }

    private Location getTeamBedSpawn(Player player, String teamWorldName) {
        if (teamWorldName == null) return null;

        Location bed = player.getBedSpawnLocation();
        if (bed == null || bed.getWorld() == null) return null;

        if (bed.getWorld().getName().equalsIgnoreCase(teamWorldName)) {
            return bed;
        }

        return null;
    }

    private Location getTeamWorldSpawn(String teamWorldName) {
        if (teamWorldName == null) return null;

        World teamWorld = Bukkit.getWorld(teamWorldName);
        if (teamWorld == null) return null;

        return teamWorld.getSpawnLocation();
    }

    private Location getLobbySpawn() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("lobbyspawn");
        if (section == null) return null;

        String worldName = section.getString("world");
        if (worldName == null) return null;

        World lobbyWorld = Bukkit.getWorld(worldName);
        if (lobbyWorld == null) return null;

        ConfigurationSection min = section.getConfigurationSection("min");
        ConfigurationSection max = section.getConfigurationSection("max");
        if (min == null || max == null) return null;

        double x = random(min.getDouble("x"), max.getDouble("x"));
        double y = random(min.getDouble("y"), max.getDouble("y"));
        double z = random(min.getDouble("z"), max.getDouble("z"));

        float yaw = (float) section.getDouble("yaw", 0f);
        float pitch = (float) section.getDouble("pitch", 0f);

        return new Location(lobbyWorld, x, y, z, yaw, pitch);
    }

    private double random(double min, double max) {
        if (min == max) return min;
        return ThreadLocalRandom.current().nextDouble(
                Math.min(min, max),
                Math.max(min, max)
        );
    }
}