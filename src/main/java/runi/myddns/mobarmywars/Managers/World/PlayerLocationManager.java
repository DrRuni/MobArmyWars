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

        String worldName = config.getString(base + ".world");
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
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

        String worldName = config.getString(base + ".world");
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x = config.getDouble(base + ".x");
        double y = config.getDouble(base + ".y");
        double z = config.getDouble(base + ".z");
        float yaw = (float) config.getDouble(base + ".yaw");
        float pitch = (float) config.getDouble(base + ".pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }
}