package runi.myddns.mobarmywars.Arena;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ArenaConfig {

    private final MobArmyMain plugin;
    private YamlConfiguration config;

    public ArenaConfig(MobArmyMain plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "arena-koordinaten.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public Location getTeamSpawn(String arenaId, String team) {

        String base = "arenas." + arenaId + "." + team.toLowerCase();
        List<?> list = config.getList(base + ".teamspawn");

        if (list == null || list.size() < 3) {
            plugin.getLogger().warning("[ArenaConfig] ❌ Kein teamspawn für " + arenaId + " / " + team);
            return null;
        }

        World world = getArenaWorld(arenaId);
        if (world == null) return null;

        return new Location(
                world,
                ((Number) list.get(0)).doubleValue(),
                ((Number) list.get(1)).doubleValue(),
                ((Number) list.get(2)).doubleValue(),
                list.size() >= 4 ? ((Number) list.get(3)).floatValue() : 0f,
                list.size() >= 5 ? ((Number) list.get(4)).floatValue() : 0f
        );
    }

    public List<Location> getMobSpawns(String arenaId, String team) {

        String path = "arenas." + arenaId + "." + team.toLowerCase() + ".mobSpawns";
        List<Location> result = new ArrayList<>();

        List<?> rawList = config.getList(path);
        if (rawList == null) return result;

        World world = getArenaWorld(arenaId);
        if (world == null) return result;

        for (Object o : rawList) {
            if (o instanceof List<?> coords && coords.size() >= 3) {
                result.add(new Location(
                        world,
                        ((Number) coords.get(0)).doubleValue(),
                        ((Number) coords.get(1)).doubleValue(),
                        ((Number) coords.get(2)).doubleValue()
                ));
            }
        }
        return result;
    }

    private World getArenaWorld(String arenaId) {
        String worldName = config.getString(
                "arenas." + arenaId + ".world",
                "world_mobarmylobby"
        );

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("[ArenaConfig] ❌ Welt nicht geladen: " + worldName);
        }
        return world;
    }
}