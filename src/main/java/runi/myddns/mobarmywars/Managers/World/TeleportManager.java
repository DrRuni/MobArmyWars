package runi.myddns.mobarmywars.Managers.World;

import org.bukkit.*;
import org.bukkit.entity.Player;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.concurrent.ThreadLocalRandom;

public class TeleportManager {

    public static void teleport(Player player, String worldName) {

        if (player == null || worldName == null) return;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        saveResumeLocation(player, worldName);

        switch (worldName.toLowerCase()) {

            case "world_rot", "world_blau" -> {
                player.teleport(world.getSpawnLocation());
            }

            case "world_mobarmy_lobby" -> {
                teleportToLobbySpawn(player, world);
            }

            default -> player.teleport(world.getSpawnLocation());
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
            player.sendMessage(ChatColor.RED + "❌ Du hast kein Team!");
            return;
        }

        World world = Bukkit.getWorld("world_mobarmy_lobby");
        if (world == null) {
            player.sendMessage(ChatColor.RED + "❌ Arena-Welt ist nicht geladen!");
            return;
        }

        Location target;

        if (team.equalsIgnoreCase("rot")) {
            target = new Location(world, 104.5, 75.0, -98.5, 0f, 27f);
        } else if (team.equalsIgnoreCase("blau")) {
            target = new Location(world, 75.5, 75.0, -98.5, 0f, 27f);
        } else {
            player.sendMessage(ChatColor.RED + "❌ Ungültiges Team: " + team);
            return;
        }

        player.teleport(target);
    }

    public static void teleportToArena(Player player) {

        MobArmyMain plugin = MobArmyMain.getInstance();
        String team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null) return;

        String arenaKey = "japanisches-dorf";

        Location target = plugin
                .getArenaConfig()
                .getTeamSpawn(arenaKey, team);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "❌ Kein Arena-Spawn für Team " + team);
            return;
        }

        player.teleport(target);
    }


    public static void teleportToOverworld(Player player) {

        if (player == null) return;

        World overworld = Bukkit.getWorld("world");

        if (overworld == null) {
            for (World w : Bukkit.getWorlds()) {
                if (w.getEnvironment() == World.Environment.NORMAL) {
                    overworld = w;
                    break;
                }
            }
        }
        if (overworld == null) {
            player.teleport(player.getWorld().getSpawnLocation());
            return;
        }
        player.teleport(overworld.getSpawnLocation());
    }

    private static void saveResumeLocation(Player player, String targetWorldName) {

        if (player == null || targetWorldName == null) return;
        if (!targetWorldName.equalsIgnoreCase("world_mobarmy_lobby")) return;

        World currentWorld = player.getWorld();
        if (currentWorld == null) return;

        String currentWorldName = currentWorld.getName().toLowerCase();

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