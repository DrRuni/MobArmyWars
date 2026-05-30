package runi.myddns.mobarmywars.Managers.World;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class WorldSettings {

    private final MobArmyMain plugin;
    private File file;
    private FileConfiguration config;

    private boolean keepInventory = true;
    private boolean randomizerEnabled = true;
    private boolean nightVisionEnabled = true;
    private boolean mobSpawningEnabled = true;
    private boolean daylightCycleEnabled = true;
    private boolean arenaCompassEnabled = false;

    private String difficulty = "normal";
    private long worldTime = 0L;

    private static final List<String> WORLDS = List.of(
            "world_mobarmy_lobby",
            "world_rot",
            "world_rot_nether",
            "world_blau",
            "world_blau_nether",
            "world_mobarmy_arena"
    );

    public WorldSettings(MobArmyMain plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "worldsettings.yml");

        if (!file.exists()) {
            plugin.saveResource("worldsettings.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);

        boolean changed = false;

        if (!config.contains("keep-inventory")) {
            config.set("keep-inventory", true);
            changed = true;
        }

        if (!config.contains("randomizer-enabled")) {
            config.set("randomizer-enabled", true);
            changed = true;
        }

        if (!config.contains("night-vision-enabled")) {
            config.set("night-vision-enabled", true);
            changed = true;
        }

        if (!config.contains("mob-spawning")) {
            config.set("mob-spawning", true);
            changed = true;
        }

        if (!config.contains("daylight-cycle")) {
            config.set("daylight-cycle", true);
            changed = true;
        }

        if (!config.contains("difficulty")) {
            config.set("difficulty", "normal");
            changed = true;
        }

        if (!config.contains("world-time")) {
            config.set("world-time", 0L);
            changed = true;
        }

        if (!config.contains("arena-compass-enabled")) {
            config.set("arena-compass-enabled", false);
            changed = true;
        }

        if (changed) save();

        keepInventory = config.getBoolean("keep-inventory", true);
        randomizerEnabled = config.getBoolean("randomizer-enabled", true);
        nightVisionEnabled = config.getBoolean("night-vision-enabled", true);
        mobSpawningEnabled = config.getBoolean("mob-spawning", true);
        daylightCycleEnabled = config.getBoolean("daylight-cycle", true);
        difficulty = config.getString("difficulty", "normal");
        worldTime = config.getLong("world-time", 0L);
        arenaCompassEnabled = config.getBoolean("arena-compass-enabled", false);
    }

    public void save() {
        config.set("keep-inventory", keepInventory);
        config.set("randomizer-enabled", randomizerEnabled);
        config.set("night-vision-enabled", nightVisionEnabled);
        config.set("mob-spawning", mobSpawningEnabled);
        config.set("daylight-cycle", daylightCycleEnabled);
        config.set("difficulty", difficulty);
        config.set("world-time", worldTime);
        config.set("arena-compass-enabled", arenaCompassEnabled);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isKeepInventoryEnabled() {
        return keepInventory;
    }

    public boolean isRandomizerEnabled() {
        return randomizerEnabled;
    }

    public boolean isNightVisionEnabled() {
        return nightVisionEnabled;
    }

    public boolean isMobSpawningEnabled() {
        return mobSpawningEnabled;
    }

    public boolean isDaylightCycleEnabled() {
        return daylightCycleEnabled;
    }

    public boolean isArenaCompassEnabled() {
        return arenaCompassEnabled;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public long getWorldTime() {
        return worldTime;
    }

    public long getCurrentWorldTime() {
        for (String worldName : WORLDS) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                return world.getTime();
            }
        }
        return worldTime;
    }

    public void toggleKeepInventory() {
        keepInventory = !keepInventory;
        applyKeepInventory();
        save();
    }

    public void toggleRandomizer() {
        randomizerEnabled = !randomizerEnabled;
        save();
    }

    public void toggleNightVision() {
        nightVisionEnabled = !nightVisionEnabled;
        save();
    }

    public void toggleMobSpawning() {
        mobSpawningEnabled = !mobSpawningEnabled;
        applyMobSpawning();
        save();
    }

    public void toggleDaylightCycle() {
        daylightCycleEnabled = !daylightCycleEnabled;

        if (!daylightCycleEnabled) {
            worldTime = getCurrentWorldTime();
        }

        applyDaylightCycle();
        applyTime();
        save();
    }

    public void toggleArenaCompassEnabled() {
        arenaCompassEnabled = !arenaCompassEnabled;
        save();
    }

    public void setArenaCompassEnabled(boolean enabled) {
        arenaCompassEnabled = enabled;
        save();
    }

    public String cycleDifficulty() {
        difficulty = switch (difficulty.toLowerCase(Locale.ROOT)) {
            case "peaceful" -> "easy";
            case "easy" -> "normal";
            case "normal" -> "hard";
            case "hard" -> "ultra-ultra-hardcore";
            default -> "peaceful";
        };

        applyDifficulty();
        applyNaturalRegeneration();
        save();
        return difficulty;
    }

    public long cycleTime() {
        long current = getCurrentWorldTime();

        if (current < 6000) {
            worldTime = 6000;
        } else if (current < 12000) {
            worldTime = 12000;
        } else if (current < 18000) {
            worldTime = 18000;
        } else {
            worldTime = 0;
        }

        applyTime();
        save();
        return worldTime;
    }

    public void applyAllSettings() {
        applyKeepInventory();
        applyMobSpawning();
        applyDaylightCycle();
        applyDifficulty();
        applyNaturalRegeneration();
        applyTime();
    }

    public void applyToWorld(World world) {
        if (world == null) return;
        if (!WORLDS.contains(world.getName())) return;

        world.setGameRule(GameRules.KEEP_INVENTORY, keepInventory);
        world.setGameRule(GameRules.SPAWN_MOBS, mobSpawningEnabled);
        world.setGameRule(GameRules.ADVANCE_TIME, daylightCycleEnabled);
        world.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, !difficulty.equalsIgnoreCase("ultra-ultra-hardcore"));
        world.setDifficulty(getBukkitDifficulty());
        applyTimeSafely(world);
    }

    public void applyKeepInventory() {
        for (String worldName : WORLDS) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                world.setGameRule(GameRules.KEEP_INVENTORY, keepInventory);
            }
        }
    }

    public void applyMobSpawning() {
        for (String worldName : WORLDS) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                world.setGameRule(GameRules.SPAWN_MOBS, mobSpawningEnabled);
            }
        }
    }

    public void applyDaylightCycle() {
        for (String worldName : WORLDS) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                world.setGameRule(GameRules.ADVANCE_TIME, daylightCycleEnabled);
            }
        }
    }

    public void applyDifficulty() {
        Difficulty bukkitDifficulty = getBukkitDifficulty();

        for (String worldName : WORLDS) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                world.setDifficulty(bukkitDifficulty);
            }
        }
    }

    public void applyNaturalRegeneration() {
        boolean naturalRegen = !difficulty.equalsIgnoreCase("ultra-ultra-hardcore");

        for (String worldName : WORLDS) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                world.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, naturalRegen);
            }
        }
    }

    public void applyTime() {
        for (String worldName : WORLDS) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                applyTimeSafely(world);
            }
        }
    }

    private Difficulty getBukkitDifficulty() {
        return switch (difficulty.toLowerCase(Locale.ROOT)) {
            case "peaceful" -> Difficulty.PEACEFUL;
            case "easy" -> Difficulty.EASY;
            case "normal" -> Difficulty.NORMAL;
            case "hard", "ultra-ultra-hardcore" -> Difficulty.HARD;
            default -> Difficulty.NORMAL;
        };
    }

    private void applyTimeSafely(World world) {
        if (world == null) return;

        if (world.getEnvironment() != World.Environment.NORMAL) {
            return;
        }

        try {
            world.setTime(worldTime);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(
                    "⚠ Zeit konnte für Welt '" + world.getName()
                            + "' nicht gesetzt werden: " + e.getMessage()
            );
        }
    }

}