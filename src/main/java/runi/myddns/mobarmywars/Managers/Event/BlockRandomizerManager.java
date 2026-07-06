package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BlockRandomizerManager implements Listener {

    private final MobArmyMain plugin;
    private final Map<String, Boolean> playerRandomizerStatus = new HashMap<>();
    private final Map<String, Map<Material, Material>> playerBlockDropMap = new HashMap<>();
    private final List<Material> droppableMaterials = new ArrayList<>();
    private final List<Material> blockedSpawnEggs = new ArrayList<>();
    private final Random random = new Random();
    private final File dataFile;
    private FileConfiguration dataConfig;
    private boolean globalRandomizerEnabled;
    private final File disabledEggsFile;
    private FileConfiguration disabledEggsConfig;

    public BlockRandomizerManager(MobArmyMain plugin) {
        this.plugin = plugin;
        this.globalRandomizerEnabled = plugin.getWorldSettings().isRandomizerEnabled();
        this.dataFile = new File(plugin.getDataFolder(), "RandomBlock.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        this.disabledEggsFile = new File(plugin.getDataFolder(), "disabled_spawn_egg.yml");
        if (!disabledEggsFile.exists()) {
            plugin.saveResource("disabled_spawn_egg.yml", false);
        }
        this.disabledEggsConfig = YamlConfiguration.loadConfiguration(disabledEggsFile);

        loadBlockedSpawnEggs();
        loadBlockDrops();
        initializeDroppableMaterials();
    }

    public void disableRandomizer(Player player) {
        playerRandomizerStatus.put(player.getName(), false);

        saveBlockDrops();
    }

    public boolean isRandomizerEnabled(Player player) {
        return playerRandomizerStatus.getOrDefault(player.getName(), false);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        if (!globalRandomizerEnabled) {
            return;
        }

        if (world.getName().equalsIgnoreCase("world_mobarmylobby")) {
            return;
        }

        Material dropMaterial = getRandomBlockDrop(player, event.getBlock().getType());
        event.setDropItems(false);
        if (dropMaterial != null) {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(dropMaterial));
        }
    }

    private Material getRandomBlockDrop(Player player, Material blockType) {
        String name = player.getName();

        playerBlockDropMap.putIfAbsent(name, new HashMap<>());
        Map<Material, Material> map = playerBlockDropMap.get(name);

        if (!map.containsKey(blockType)) {

            Material randomDrop;
            do {
                randomDrop = droppableMaterials.get(random.nextInt(droppableMaterials.size()));
            } while (randomDrop.name().endsWith("_SPAWN_EGG") && isSpawnEggBlocked(randomDrop));

            map.put(blockType, randomDrop);

            saveBlockDrops();
        }

        return map.get(blockType);
    }

    private void initializeDroppableMaterials() {
        FileConfiguration config = plugin.getConfig();
        droppableMaterials.clear();

        if (config.contains("droppable_materials")) {
            List<String> materials = config.getStringList("droppable_materials");
            for (String materialName : materials) {
                Material material = getMaterialFromConfig(materialName);
                if (material != null && material.isItem() && material != Material.AIR) {
                    droppableMaterials.add(material);
                } else {
                    plugin.getLogger().warning("Ungültiges Material in der Konfiguration: " + materialName);
                }
            }
        } else {
            for (Material material : Material.values()) {
                if (material.isItem() && material != Material.AIR) {
                    droppableMaterials.add(material);
                }
            }
        }
    }

    private Material getMaterialFromConfig(String materialName) {
        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) {
            plugin.getLogger().warning("Ungültiges Material in der Konfiguration: " + materialName);
        }
        return material;
    }

    public void loadBlockDrops() {
        if (!dataConfig.contains("block_drops")) return;

        for (String playerName : dataConfig.getConfigurationSection("block_drops").getKeys(false)) {

            Map<Material, Material> blockDropMap = new HashMap<>();

            for (String blockName : dataConfig.getConfigurationSection("block_drops." + playerName).getKeys(false)) {

                Material blockMaterial = Material.getMaterial(blockName);
                Material dropMaterial = Material.getMaterial(
                        dataConfig.getString("block_drops." + playerName + "." + blockName)
                );

                if (blockMaterial != null && dropMaterial != null) {
                    blockDropMap.put(blockMaterial, dropMaterial);
                }
            }

            playerBlockDropMap.put(playerName, blockDropMap);
        }
    }

    public void saveBlockDrops() {

        for (String playerName : playerBlockDropMap.keySet()) {
            for (Map.Entry<Material, Material> entry : playerBlockDropMap.get(playerName).entrySet()) {

                dataConfig.set("block_drops." + playerName + "." +
                        entry.getKey().name(), entry.getValue().name());
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Fehler beim Speichern der RandomBlock.yml: " + e.getMessage());
        }
    }

    public boolean isSpawnEggBlocked(Material spawnEgg) {
        return blockedSpawnEggs.contains(spawnEgg);
    }

    public void toggleSpawnEgg(Material spawnEgg) {
        if (blockedSpawnEggs.contains(spawnEgg)) {
            blockedSpawnEggs.remove(spawnEgg);
        } else {
            blockedSpawnEggs.add(spawnEgg);
        }
        saveBlockedSpawnEggs();
    }

    private void saveBlockedSpawnEggs() {
        List<String> eggNames = new ArrayList<>();
        for (Material material : blockedSpawnEggs) {
            eggNames.add(material.name());
        }

        disabledEggsConfig.set("blocked_spawn_eggs", eggNames);

        try {
            disabledEggsConfig.save(disabledEggsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Fehler beim Speichern der disabled_spawn_egg.yml: " + e.getMessage());
        }
    }

    public void loadBlockedSpawnEggs() {
        List<String> eggNames = disabledEggsConfig.getStringList("blocked_spawn_eggs");
        blockedSpawnEggs.clear();

        for (String name : eggNames) {
            Material material = getMaterialFromConfig(name);
            if (material != null) {
                blockedSpawnEggs.add(material);
            }
        }
    }

    public void resetRandomizer() {

        playerBlockDropMap.clear();

        dataConfig.set("block_drops", null);

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning(
                    "Fehler beim Zurücksetzen der RandomBlock.yml: " + e.getMessage()
            );
        }
    }

    public boolean isGlobalRandomizerEnabled() {
        return globalRandomizerEnabled;
    }

    public void setGlobalRandomizerEnabled(boolean enabled) {
        this.globalRandomizerEnabled = enabled;
    }
}