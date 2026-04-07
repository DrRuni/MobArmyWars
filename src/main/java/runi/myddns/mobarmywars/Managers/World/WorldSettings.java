package runi.myddns.mobarmywars.Managers.World;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class WorldSettings {

    private final MobArmyMain plugin;
    private File file;
    private FileConfiguration config;


    private boolean keepInventory = true;
    private boolean randomizerEnabled = true;
    private boolean nightVisionEnabled = true;


    private static final List<String> WORLDS = List.of(
            "world_mobarmylobby",
            "world_rot",
            "world_rot_nether",
            "world_blau",
            "world_blau_nether",
            "world_arena"
    );

    public WorldSettings(MobArmyMain plugin) {
        this.plugin = plugin;
        load();
        applyKeepInventory();
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

        if (changed) save();

        keepInventory = config.getBoolean("keep-inventory", true);
        randomizerEnabled = config.getBoolean("randomizer-enabled", true);
        nightVisionEnabled = config.getBoolean("night-vision-enabled", true);
    }

    public void save() {
        config.set("keep-inventory", keepInventory);
        config.set("randomizer-enabled", randomizerEnabled);
        config.set("night-vision-enabled", nightVisionEnabled);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isKeepInventoryEnabled() {return keepInventory;}
    public boolean isRandomizerEnabled() {return randomizerEnabled;}
    public boolean isNightVisionEnabled() { return nightVisionEnabled; }

    public void toggleKeepInventory() {keepInventory = !keepInventory;applyKeepInventory();save();}
    public void toggleRandomizer() {randomizerEnabled = !randomizerEnabled;save();}
    public void toggleNightVision() {nightVisionEnabled = !nightVisionEnabled;save();}

    public void applyKeepInventory() {
        for (String worldName : WORLDS) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                world.setGameRule(GameRule.KEEP_INVENTORY, keepInventory);
            }
        }
    }
}
