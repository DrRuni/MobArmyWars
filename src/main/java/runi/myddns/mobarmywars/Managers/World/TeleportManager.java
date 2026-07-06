package runi.myddns.mobarmywars.Managers.World;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
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

            case "world_mobarmylobby" -> {
                teleportToLobbySpawn(player, world);
            }

            default -> player.teleport(world.getSpawnLocation());
        }
    }

    private static void teleportToLobbySpawn(Player player, World world) {

        var config = MobArmyMain.getInstance().getConfig();
        ConfigurationSection section = config.getConfigurationSection("lobbyspawn");

        if (section == null) {
            player.teleport(world.getSpawnLocation());
            return;
        }

        ConfigurationSection min = section.getConfigurationSection("min");
        ConfigurationSection max = section.getConfigurationSection("max");

        if (min == null || max == null) {
            player.teleport(world.getSpawnLocation());
            return;
        }

        double x = random(min.getDouble("x"), max.getDouble("x"));
        double y = random(min.getDouble("y"), max.getDouble("y"));
        double z = random(min.getDouble("z"), max.getDouble("z"));

        float yaw = (float) section.getDouble("yaw", 0f);
        float pitch = (float) section.getDouble("pitch", 0f);

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

        Location target = plugin
                .getArenaConfig()
                .getTeamSpawn("wave-auswahl", team);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "❌ Kein Spawnpunkt für dein Team gefunden!");
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
        if (!targetWorldName.equalsIgnoreCase("world_mobarmylobby")) return;

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