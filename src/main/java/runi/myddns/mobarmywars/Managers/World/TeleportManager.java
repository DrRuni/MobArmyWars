package runi.myddns.mobarmywars.Managers.World;

import org.bukkit.*;
import org.bukkit.entity.Player;
import runi.myddns.mobarmywars.Managers.Event.ArenaConfig;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class TeleportManager {

    public static void teleport(Player player, String worldName) {

        if (player == null || worldName == null) return;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        saveResumeLocation(player, worldName);

        if (worldName.equalsIgnoreCase("world_mobarmy_lobby")) {
            teleportToLobbySpawn(player, world);
        } else {
            player.teleport(world.getSpawnLocation());
        }
    }

    private static void teleportToLobbySpawn(Player player, World world) {

        double minX = 3.0;
        double minY = 69.0;
        double minZ = -2.5;

        double maxX = 6.0;
        double maxY = 69.0;
        double maxZ = 0.5;

        float yaw = -90.0f;
        float pitch = 0.0f;

        double x = random(minX, maxX);
        double y = random(minY, maxY);
        double z = random(minZ, maxZ);

        Location loc = new Location(world, x, y, z, yaw, pitch);
        player.teleport(loc);
    }

    public static void teleportToWaveSelection(Player player) {
        MobArmyMain plugin = MobArmyMain.getInstance();

        String team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null) {
            player.sendMessage(
                    plugin.getLanguageManager().getComponent(
                            "teleport-manager.no-team"
                    )
            );
            return;
        }

        World world = Bukkit.getWorld("world_mobarmy_lobby");
        if (world == null) {
            player.sendMessage(
                    plugin.getLanguageManager().getComponent(
                            "teleport-manager.lobby-world-not-loaded"
                    )
            );
            return;
        }

        Location target;

        if (team.equalsIgnoreCase("rot")) {
            target = new Location(world, 104.5, 75.0, -98.5, 0f, 27f);
        } else if (team.equalsIgnoreCase("blau")) {
            target = new Location(world, 75.5, 75.0, -98.5, 0f, 27f);
        } else {
            player.sendMessage(
                    plugin.getLanguageManager().getComponent(
                            "teleport-manager.invalid-team",
                            "team",
                            team
                    )
            );
            return;
        }

        player.teleport(target);
    }

    public static void teleportToArena(Player player) {

        MobArmyMain plugin = MobArmyMain.getInstance();

        String team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null || team.equalsIgnoreCase("Kein Team")) {
            player.sendMessage(
                    plugin.getLanguageManager().getComponent(
                            "teleport-manager.no-team"
                    )
            );
            return;
        }

        ArenaConfig.ArenaData arena = plugin.getArenaConfig().getActiveArena();

        if (arena == null) {
            player.sendMessage(
                    plugin.getLanguageManager().getComponent(
                            "teleport-manager.no-active-arena"
                    )
            );
            return;
        }

        Location configuredSpawn = plugin.getArenaConfig().getTeamSpawn(team);

        if (configuredSpawn == null) {
            player.sendMessage(
                    plugin.getLanguageManager().getComponent(
                            "teleport-manager.no-arena-spawn",
                            "team",
                            team
                    )
            );
            return;
        }

        World currentWorld = Bukkit.getWorld(arena.world());

        if (currentWorld == null) {
            player.sendMessage(
                    plugin.getLanguageManager().getComponent(
                            "teleport-manager.arena-world-not-loaded"
                    )
            );
            return;
        }

        Location target = new Location(
                currentWorld,
                configuredSpawn.getX(),
                configuredSpawn.getY(),
                configuredSpawn.getZ(),
                configuredSpawn.getYaw(),
                configuredSpawn.getPitch()
        );

        player.teleport(target);
    }

    private static void saveResumeLocation(Player player, String targetWorldName) {

        if (player == null || targetWorldName == null) return;
        if (!targetWorldName.equalsIgnoreCase("world_mobarmy_lobby")) return;

        String currentWorldName =
                player.getWorld()
                        .getName()
                        .toLowerCase(Locale.ROOT);

        boolean isTeamWorld =
                currentWorldName.equals("world_rot") ||
                        currentWorldName.equals("world_blau") ||
                        currentWorldName.equals("world_rot_nether") ||
                        currentWorldName.equals("world_blau_nether");

        if (!isTeamWorld) return;

        MobArmyMain.getInstance().getEventResume().savePlayerLastLocation(player);
    }

    private static double random(double min, double max) {
        if (min == max) return min;
        return ThreadLocalRandom.current().nextDouble(
                Math.min(min, max),
                Math.max(min, max)
        );
    }

}