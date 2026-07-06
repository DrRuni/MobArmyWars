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
//    private final Map<String, Boolean> playerRandomizerStatus = new HashMap<>();
    private final Map<String, Map<Material, Material>> playerBlockDropMap = new HashMap<>();
    private final List<Material> droppableMaterials = new ArrayList<>();
    private final List<Material> blockedSpawnEggs = new ArrayList<>();
    private final Set<String> blockedMaterialPatterns = new HashSet<>();
    private final Random random = new Random();
    private final File dataFile;
    private FileConfiguration dataConfig;
    private boolean globalRandomizerEnabled;
    private final File worldSettingsFile;
    private FileConfiguration worldSettingsConfig;

    public BlockRandomizerManager(MobArmyMain plugin) {
        this.plugin = plugin;
        this.globalRandomizerEnabled = plugin.getWorldSettings().isRandomizerEnabled();
        this.dataFile = new File(plugin.getDataFolder(), "RandomBlock.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        this.worldSettingsFile = new File(plugin.getDataFolder(), "Worldsettings.yml");
        this.worldSettingsConfig = YamlConfiguration.loadConfiguration(worldSettingsFile);

        loadBlockedSpawnEggs();
        loadBlockedMaterials();
        loadBlockDrops();
        initializeDroppableMaterials();
    }

//    public void disableRandomizer(Player player) {
//        playerRandomizerStatus.put(player.getName(), false);
//
//        saveBlockDrops();
//    }

//    public boolean isRandomizerEnabled(Player player) {
//        return playerRandomizerStatus.getOrDefault(player.getName(), false);
//    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        if (!globalRandomizerEnabled) {
            return;
        }

        String worldName = world.getName().toLowerCase();

        if (!worldName.equals("world_rot")
                && !worldName.equals("world_blau")
                && !worldName.equals("world_rot_nether")
                && !worldName.equals("world_blau_nether")) {
            return;
        }

        Material dropMaterial = getRandomizedMaterial(player, event.getBlock().getType());
        event.setDropItems(false);
        if (dropMaterial != null) {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(dropMaterial));
        }
    }

    public Material getRandomizedMaterial(Player player, Material originalMaterial) {
        String name = player.getName();

        playerBlockDropMap.putIfAbsent(name, new HashMap<>());
        Map<Material, Material> map = playerBlockDropMap.get(name);

        if (!map.containsKey(originalMaterial)) {

            List<Material> allowedMaterials = getAllowedDroppableMaterials();

            if (allowedMaterials.isEmpty()) {
                plugin.getLogger().warning("Keine erlaubten Randomizer-Drops verfügbar!");
                return null;
            }

            Material randomDrop = allowedMaterials.get(random.nextInt(allowedMaterials.size()));
            map.put(originalMaterial, randomDrop);

            saveBlockDrops();
        }

        return map.get(originalMaterial);
    }

    private List<Material> getAllowedDroppableMaterials() {
        return droppableMaterials.stream()
                .filter(material -> !isMaterialBlocked(material))
                .toList();
    }

    public boolean isMaterialBlocked(Material material) {
        return isBlockedByPattern(material.name())
                || material.name().endsWith("_SPAWN_EGG") && isSpawnEggBlocked(material);
    }

    private boolean isBlockedByPattern(String materialName) {
        for (String pattern : blockedMaterialPatterns) {
            if (matchesPattern(materialName, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPattern(String materialName, String pattern) {
        String normalizedPattern = pattern.toUpperCase(Locale.ROOT).trim();

        if (normalizedPattern.equals("*")) {
            return true;
        }

        if (normalizedPattern.startsWith("*") && normalizedPattern.endsWith("*")) {
            String contains = normalizedPattern.substring(1, normalizedPattern.length() - 1);
            return materialName.contains(contains);
        }

        if (normalizedPattern.endsWith("*")) {
            String startsWith = normalizedPattern.substring(0, normalizedPattern.length() - 1);
            return materialName.startsWith(startsWith);
        }

        if (normalizedPattern.startsWith("*")) {
            String endsWith = normalizedPattern.substring(1);
            return materialName.endsWith(endsWith);
        }

        return materialName.equals(normalizedPattern);
    }

    public Material getOriginalMaterial(Player player, Material randomizedMaterial) {
        String name = player.getName();

        Map<Material, Material> map = playerBlockDropMap.get(name);

        if (map == null) {
            return randomizedMaterial;
        }

        for (Map.Entry<Material, Material> entry : map.entrySet()) {
            if (entry.getValue() == randomizedMaterial) {
                return entry.getKey();
            }
        }

        return randomizedMaterial;
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

        worldSettingsConfig.set("randomizer.blocked_spawn_eggs", eggNames);

        try {
            worldSettingsConfig.save(worldSettingsFile);
        } catch (IOException e) {
            plugin.getLogger().warning(
                    "Fehler beim Speichern der weltensettings.yml: " + e.getMessage()
            );
        }
    }

    public void loadBlockedSpawnEggs() {
        worldSettingsConfig = YamlConfiguration.loadConfiguration(worldSettingsFile);

        List<String> eggNames =
                worldSettingsConfig.getStringList("randomizer.blocked_spawn_eggs");

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

    public boolean isBlockPatternBlocked(String pattern) {
        return blockedMaterialPatterns.contains(pattern.toUpperCase(Locale.ROOT));
    }

    public void toggleBlockedBlockPattern(String pattern) {
        String normalizedPattern = pattern.toUpperCase(Locale.ROOT).trim();

        if (blockedMaterialPatterns.contains(normalizedPattern)) {
            blockedMaterialPatterns.remove(normalizedPattern);
        } else {
            blockedMaterialPatterns.add(normalizedPattern);
        }

        saveBlockedMaterialPatterns();
        resetRandomizer();
    }

    private void saveBlockedMaterialPatterns() {
        worldSettingsConfig.set(
                "randomizer.blocked_blocks",
                new ArrayList<>(blockedMaterialPatterns)
        );

        try {
            worldSettingsConfig.save(worldSettingsFile);
        } catch (IOException e) {
            plugin.getLogger().warning(
                    "Fehler beim Speichern der weltensettings.yml: " + e.getMessage()
            );
        }
    }

    public void loadBlockedMaterials() {
        worldSettingsConfig = YamlConfiguration.loadConfiguration(worldSettingsFile);

        List<String> patterns =
                worldSettingsConfig.getStringList("randomizer.blocked_blocks");

        blockedMaterialPatterns.clear();

        for (String pattern : patterns) {
            if (pattern != null && !pattern.trim().isEmpty()) {
                blockedMaterialPatterns.add(pattern.toUpperCase(Locale.ROOT).trim());
            }
        }
    }

    public boolean isGlobalRandomizerEnabled() {
        return globalRandomizerEnabled;
    }

    public void setGlobalRandomizerEnabled(boolean enabled) {
        this.globalRandomizerEnabled = enabled;
    }
}