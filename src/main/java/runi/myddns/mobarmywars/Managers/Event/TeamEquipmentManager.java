package runi.myddns.mobarmywars.Managers.Event;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class TeamEquipmentManager {

    private final MobArmyMain plugin;
    private final File file;
    private YamlConfiguration config;

    private static final String BASE_PATH = "team-equipment";

    public TeamEquipmentManager(MobArmyMain plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "team-equipment.yml");
        load();
    }

    public void load() {
        this.config = YamlConfiguration.loadConfiguration(file);

        setDefault("blue");
        setDefault("red");

        save();
    }

    private void setDefault(String team) {
        setIfMissing(path(team, "helmet"));
        setIfMissing(path(team, "chestplate"));
        setIfMissing(path(team, "leggings"));
        setIfMissing(path(team, "boots"));

        setIfMissing(path(team, "sword"));
        setIfMissing(path(team, "axe"));
        setIfMissing(path(team, "pickaxe"));

        setIfMissing(path(team, "bow"));
        setIfMissing(path(team, "food"));
        setIfMissing(path(team, "shield"));
        setIfMissing(path(team, "op_food"));
    }

    private String path(String team, String setting) {
        return BASE_PATH + "." + team.toLowerCase(Locale.ROOT) + "." + setting;
    }

    private void setIfMissing(String path) {
        if (!config.contains(path)) {
            config.set(path, "NONE");
        }
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(
                    java.util.logging.Level.SEVERE,
                    "Failed to save team-equipment.yml.",
                    e
            );
        }
    }

    public int getLevel(String team, EquipmentType type) {
        String configPath = path(team, type.getPath());

        if (config.isInt(configPath)) {
            int oldLevel = config.getInt(configPath, 0);

            if (oldLevel < 0) {
                oldLevel = 0;
            }

            if (oldLevel > type.getMaxLevel()) {
                oldLevel = type.getMaxLevel();
            }

            config.set(configPath, getConfigValueForLevel(type, oldLevel));
            save();

            return oldLevel;
        }

        String value = config.getString(configPath, "NONE");

        if (value.equalsIgnoreCase("NONE")) {
            return 0;
        }

        int level = getLevelFromConfigValue(type, value);

        if (level < 0) {
            level = 0;
        }

        if (level > type.getMaxLevel()) {
            level = type.getMaxLevel();
            setLevel(team, type, level);
        }

        return level;
    }

    public void setLevel(String team, EquipmentType type, int level) {
        if (level < 0) {
            level = 0;
        }

        if (level > type.getMaxLevel()) {
            level = type.getMaxLevel();
        }

        config.set(path(team, type.getPath()), getConfigValueForLevel(type, level));
        save();
    }

    public void nextLevel(String team, EquipmentType type) {
        int current = getLevel(team, type);

        if (current >= type.getMaxLevel()) {
            return;
        }

        setLevel(team, type, current + 1);
    }

    public void previousLevel(String team, EquipmentType type) {
        int current = getLevel(team, type);

        if (current <= 0) {
            return;
        }

        setLevel(team, type, current - 1);
    }

    public void resetAllEquipment() {
        for (String team : List.of("blue", "red")) {
            for (EquipmentType type : EquipmentType.values()) {
                config.set(path(team, type.getPath()), "NONE");
            }
        }

        save();
    }

    public void giveEquipment(Player player, String team) {
        String key = team.toLowerCase(Locale.ROOT);

        player.getInventory().setHelmet(createArmorItem(EquipmentType.HELMET, getLevel(key, EquipmentType.HELMET)));
        player.getInventory().setChestplate(createArmorItem(EquipmentType.CHESTPLATE, getLevel(key, EquipmentType.CHESTPLATE)));
        player.getInventory().setLeggings(createArmorItem(EquipmentType.LEGGINGS, getLevel(key, EquipmentType.LEGGINGS)));
        player.getInventory().setBoots(createArmorItem(EquipmentType.BOOTS, getLevel(key, EquipmentType.BOOTS)));

        ItemStack sword = createToolItem(EquipmentType.SWORD, getLevel(key, EquipmentType.SWORD));
        if (sword != null) {
            player.getInventory().setItem(0, sword);
        }

        ItemStack axe = createToolItem(EquipmentType.AXE, getLevel(key, EquipmentType.AXE));
        if (axe != null) {
            player.getInventory().setItem(1, axe);
        }

        if (getLevel(key, EquipmentType.BOW) > 0) {
            player.getInventory().setItem(2, new ItemStack(Material.BOW));
            player.getInventory().setItem(10, new ItemStack(Material.ARROW, 64));
        }

        ItemStack pickaxe = createToolItem(EquipmentType.PICKAXE, getLevel(key, EquipmentType.PICKAXE));
        if (pickaxe != null) {
            player.getInventory().setItem(3, pickaxe);
        }

        int foodLevel = getLevel(key, EquipmentType.FOOD);

        if (foodLevel == 1) {
            player.getInventory().setItem(4, new ItemStack(Material.COOKED_BEEF, 64));
        } else if (foodLevel == 2) {
            player.getInventory().setItem(4, new ItemStack(Material.GOLDEN_CARROT, 64));
        }

        int opFoodLevel = getLevel(key, EquipmentType.OP_FOOD);

        if (opFoodLevel == 1) {
            player.getInventory().setItem(5, new ItemStack(Material.GOLDEN_APPLE, 6));
        } else if (opFoodLevel == 2) {
            player.getInventory().setItem(5, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));
        }

        if (getLevel(key, EquipmentType.SHIELD) > 0) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
        }

        player.updateInventory();

        player.sendMessage(
                lang("team-equipment-gui.received")
        );
    }

    private ItemStack createArmorItem(EquipmentType type, int level) {
        Material material = getArmorMaterial(type, level);

        if (material == null) {
            return null;
        }

        return new ItemStack(material);
    }

    private ItemStack createToolItem(EquipmentType type, int level) {
        Material material = getToolMaterial(type, level);

        if (material == null) {
            return null;
        }

        return new ItemStack(material);
    }

    public Material getDisplayMaterial(EquipmentType type, int level) {
        if (level <= 0) {
            return Material.BARRIER;
        }

        if (type.isArmor()) {
            return getArmorMaterial(type, level);
        }

        if (type.isTool()) {
            return getToolMaterial(type, level);
        }

        return switch (type) {
            case BOW -> Material.BOW;
            case FOOD -> level == 2 ? Material.GOLDEN_CARROT : Material.COOKED_BEEF;
            case SHIELD -> Material.SHIELD;
            case OP_FOOD -> level == 2 ? Material.ENCHANTED_GOLDEN_APPLE : Material.GOLDEN_APPLE;
            default -> Material.BARRIER;
        };
    }

    private String getConfigValueForLevel(EquipmentType type, int level) {
        if (level <= 0) {
            return "NONE";
        }

        Material material;

        if (type.isArmor()) {
            material = getArmorMaterial(type, level);
        } else if (type.isTool()) {
            material = getToolMaterial(type, level);
        } else {
            material = switch (type) {
                case BOW -> Material.BOW;
                case FOOD -> level == 2 ? Material.GOLDEN_CARROT : Material.COOKED_BEEF;
                case SHIELD -> Material.SHIELD;
                case OP_FOOD -> level == 2 ? Material.ENCHANTED_GOLDEN_APPLE : Material.GOLDEN_APPLE;
                default -> Material.BARRIER;
            };
        }

        return material == null ? "NONE" : material.name();
    }

    private int getLevelFromConfigValue(EquipmentType type, String value) {
        String itemName = value.toUpperCase(Locale.ROOT);

        return switch (type) {
            case HELMET -> switch (itemName) {
                case "LEATHER_HELMET" -> 1;
                case "IRON_HELMET" -> 2;
                case "DIAMOND_HELMET" -> 3;
                case "NETHERITE_HELMET" -> 4;
                default -> 0;
            };

            case CHESTPLATE -> switch (itemName) {
                case "LEATHER_CHESTPLATE" -> 1;
                case "IRON_CHESTPLATE" -> 2;
                case "DIAMOND_CHESTPLATE" -> 3;
                case "NETHERITE_CHESTPLATE" -> 4;
                default -> 0;
            };

            case LEGGINGS -> switch (itemName) {
                case "LEATHER_LEGGINGS" -> 1;
                case "IRON_LEGGINGS" -> 2;
                case "DIAMOND_LEGGINGS" -> 3;
                case "NETHERITE_LEGGINGS" -> 4;
                default -> 0;
            };

            case BOOTS -> switch (itemName) {
                case "LEATHER_BOOTS" -> 1;
                case "IRON_BOOTS" -> 2;
                case "DIAMOND_BOOTS" -> 3;
                case "NETHERITE_BOOTS" -> 4;
                default -> 0;
            };

            case SWORD -> switch (itemName) {
                case "STONE_SWORD" -> 1;
                case "IRON_SWORD" -> 2;
                case "DIAMOND_SWORD" -> 3;
                case "NETHERITE_SWORD" -> 4;
                default -> 0;
            };

            case AXE -> switch (itemName) {
                case "STONE_AXE" -> 1;
                case "IRON_AXE" -> 2;
                case "DIAMOND_AXE" -> 3;
                case "NETHERITE_AXE" -> 4;
                default -> 0;
            };

            case PICKAXE -> switch (itemName) {
                case "STONE_PICKAXE" -> 1;
                case "IRON_PICKAXE" -> 2;
                case "DIAMOND_PICKAXE" -> 3;
                case "NETHERITE_PICKAXE" -> 4;
                default -> 0;
            };

            case BOW -> itemName.equals("BOW") ? 1 : 0;

            case FOOD -> switch (itemName) {
                case "COOKED_BEEF" -> 1;
                case "GOLDEN_CARROT" -> 2;
                default -> 0;
            };

            case SHIELD -> itemName.equals("SHIELD") ? 1 : 0;

            case OP_FOOD -> switch (itemName) {
                case "GOLDEN_APPLE" -> 1;
                case "ENCHANTED_GOLDEN_APPLE" -> 2;
                default -> 0;
            };
        };
    }

    private Material getArmorMaterial(EquipmentType type, int level) {
        if (level <= 0) return null;

        String prefix = switch (level) {
            case 1 -> "LEATHER";
            case 2 -> "IRON";
            case 3 -> "DIAMOND";
            case 4 -> "NETHERITE";
            default -> null;
        };

        if (prefix == null) return null;

        String suffix = switch (type) {
            case HELMET -> "HELMET";
            case CHESTPLATE -> "CHESTPLATE";
            case LEGGINGS -> "LEGGINGS";
            case BOOTS -> "BOOTS";
            default -> null;
        };

        if (suffix == null) return null;

        return Material.valueOf(prefix + "_" + suffix);
    }

    private Material getToolMaterial(EquipmentType type, int level) {
        if (level <= 0) return null;

        String prefix = switch (level) {
            case 1 -> "STONE";
            case 2 -> "IRON";
            case 3 -> "DIAMOND";
            case 4 -> "NETHERITE";
            default -> null;
        };

        if (prefix == null) return null;

        String suffix = switch (type) {
            case SWORD -> "SWORD";
            case AXE -> "AXE";
            case PICKAXE -> "PICKAXE";
            default -> null;
        };

        if (suffix == null) return null;

        return Material.valueOf(prefix + "_" + suffix);
    }

    public Component getLevelName(
            EquipmentType type,
            int level
    ) {

        if (level <= 0) {
            return lang("team-equipment-gui.level.none");
        }

        if (type.isArmor()) {
            return switch (level) {
                case 1 -> lang("team-equipment-gui.level.leather");
                case 2 -> lang("team-equipment-gui.level.iron");
                case 3 -> lang("team-equipment-gui.level.diamond");
                case 4 -> lang("team-equipment-gui.level.netherite");
                default -> lang("team-equipment-gui.level.unknown");
            };
        }

        if (type.isTool()) {
            return switch (level) {
                case 1 -> lang("team-equipment-gui.level.stone");
                case 2 -> lang("team-equipment-gui.level.iron");
                case 3 -> lang("team-equipment-gui.level.diamond");
                case 4 -> lang("team-equipment-gui.level.netherite");
                default -> lang("team-equipment-gui.level.unknown");
            };
        }

        return switch (type) {
            case BOW ->
                    lang("team-equipment-gui.level.bow");

            case FOOD ->
                    level == 2
                            ? lang("team-equipment-gui.level.golden-carrot")
                            : lang("team-equipment-gui.level.steak");

            case SHIELD ->
                    lang("team-equipment-gui.level.shield");

            case OP_FOOD ->
                    level == 2
                            ? lang("team-equipment-gui.level.enchanted-golden-apple")
                            : lang("team-equipment-gui.level.golden-apple");

            default ->
                    lang("team-equipment-gui.level.unknown");
        };
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }

    public enum EquipmentType {
        HELMET("helmet", 4),
        CHESTPLATE("chestplate", 4),
        LEGGINGS("leggings", 4),
        BOOTS("boots", 4),

        SWORD("sword", 4),
        AXE("axe", 4),
        PICKAXE("pickaxe", 4),

        BOW("bow", 1),
        FOOD("food", 2),
        SHIELD("shield", 1),
        OP_FOOD("op_food", 2);

        private final String path;
        private final int maxLevel;

        EquipmentType(String path, int maxLevel) {
            this.path = path;
            this.maxLevel = maxLevel;
        }

        public String getPath() {
            return path;
        }

        public int getMaxLevel() {
            return maxLevel;
        }

        public boolean isArmor() {
            return this == HELMET
                    || this == CHESTPLATE
                    || this == LEGGINGS
                    || this == BOOTS;
        }

        public boolean isTool() {
            return this == SWORD
                    || this == AXE
                    || this == PICKAXE;
        }
    }
}