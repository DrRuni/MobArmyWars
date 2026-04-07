package runi.myddns.mobarmywars.Managers.World;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class PlayerLocationManager {

    private final FileConfiguration config;

    public PlayerLocationManager(FileConfiguration config) {
        this.config = config;
    }

    public void saveLastLocation(Player player) {
        if (player == null) return;

        Location loc = player.getLocation();
        String base = "players." + player.getName() + ".last";

        config.set(base + ".world", loc.getWorld().getName());
        config.set(base + ".x", loc.getX());
        config.set(base + ".y", loc.getY());
        config.set(base + ".z", loc.getZ());
        config.set(base + ".yaw", loc.getYaw());
        config.set(base + ".pitch", loc.getPitch());
    }

    public Location getLastLocation(Player player) {
        if (player == null) return null;

        String base = "players." + player.getName() + ".last";
        if (!config.contains(base + ".world")) return null;

        World world = Bukkit.getWorld(config.getString(base + ".world"));
        if (world == null) return null;

        double x = config.getDouble(base + ".x");
        double y = config.getDouble(base + ".y");
        double z = config.getDouble(base + ".z");
        float yaw = (float) config.getDouble(base + ".yaw");
        float pitch = (float) config.getDouble(base + ".pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    public void saveSpawn(Player player, Location location) {
        if (player == null || location == null) return;

        String base = "players." + player.getName() + ".spawn";

        config.set(base + ".world", location.getWorld().getName());
        config.set(base + ".x", location.getX());
        config.set(base + ".y", location.getY());
        config.set(base + ".z", location.getZ());
        config.set(base + ".yaw", location.getYaw());
        config.set(base + ".pitch", location.getPitch());
    }

    public Location getSpawn(Player player) {
        if (player == null) return null;

        String base = "players." + player.getName() + ".spawn";
        if (!config.contains(base + ".world")) return null;

        World world = Bukkit.getWorld(config.getString(base + ".world"));
        if (world == null) return null;

        double x = config.getDouble(base + ".x");
        double y = config.getDouble(base + ".y");
        double z = config.getDouble(base + ".z");
        float yaw = (float) config.getDouble(base + ".yaw");
        float pitch = (float) config.getDouble(base + ".pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    public Location getBestRespawn(Player player) {
        if (player == null) return null;

        Location bed = player.getBedSpawnLocation();
        if (bed != null && bed.getWorld() != null) {
            return bed;
        }

        Location spawn = getSpawn(player);
        if (spawn != null && spawn.getWorld() != null) {
            return spawn;
        }

        World world = player.getWorld();
        return world != null ? world.getSpawnLocation() : null;
    }

    public void saveLastPortal(Player player, Location loc) {
        String base = "players." + player.getName() + ".portal";
        config.set(base + ".world", loc.getWorld().getName());
        config.set(base + ".x", loc.getX());
        config.set(base + ".y", loc.getY());
        config.set(base + ".z", loc.getZ());
    }

    public Location getLastPortal(Player player) {
        String base = "players." + player.getName() + ".portal";
        if (!config.contains(base + ".world")) return null;

        World w = Bukkit.getWorld(config.getString(base + ".world"));
        if (w == null) return null;

        return new Location(
                w,
                config.getDouble(base + ".x"),
                config.getDouble(base + ".y"),
                config.getDouble(base + ".z")
        );
    }

    public boolean hasLastLocation(Player player) {
        return player != null &&
                config.contains("players." + player.getName() + ".last.world");
    }

    public boolean hasSpawn(Player player) {
        return player != null &&
                config.contains("players." + player.getName() + ".spawn.world");
    }
}