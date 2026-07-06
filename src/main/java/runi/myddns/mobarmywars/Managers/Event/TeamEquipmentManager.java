package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TeamEquipmentManager {

    private final MobArmyMain plugin;
    private final File file;
    private YamlConfiguration config;

    public TeamEquipmentManager(MobArmyMain plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "team_equipment.yml");
        load();
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);

        setDefault("blue");
        setDefault("red");

        save();
    }

    private void setDefault(String team) {
        setIfMissing(team + ".helmet", 0);
        setIfMissing(team + ".chestplate", 0);
        setIfMissing(team + ".leggings", 0);
        setIfMissing(team + ".boots", 0);

        setIfMissing(team + ".sword", 0);
        setIfMissing(team + ".axe", 0);
        setIfMissing(team + ".pickaxe", 0);

        setIfMissing(team + ".bow", 0);
        setIfMissing(team + ".food", 0);
        setIfMissing(team + ".shield", 0);
        setIfMissing(team + ".op_food", 0);
    }

    private void setIfMissing(String path, int value) {
        if (!config.contains(path)) {
            config.set(path, value);
        }
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getLevel(String team, EquipmentType type) {
        int level = config.getInt(team.toLowerCase() + "." + type.getPath(), 0);

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
        config.set(team.toLowerCase() + "." + type.getPath(), level);
        save();
    }

    public void nextLevel(String team, EquipmentType type) {
        int current = getLevel(team, type);
        int next = current + 1;

        if (next > type.getMaxLevel()) {
            next = 0;
        }

        setLevel(team, type, next);
    }

    public void giveEquipment(Player player, String team) {
        String key = team.toLowerCase();

        // Rüstung direkt anziehen
        player.getInventory().setHelmet(createArmorItem(EquipmentType.HELMET, getLevel(key, EquipmentType.HELMET)));
        player.getInventory().setChestplate(createArmorItem(EquipmentType.CHESTPLATE, getLevel(key, EquipmentType.CHESTPLATE)));
        player.getInventory().setLeggings(createArmorItem(EquipmentType.LEGGINGS, getLevel(key, EquipmentType.LEGGINGS)));
        player.getInventory().setBoots(createArmorItem(EquipmentType.BOOTS, getLevel(key, EquipmentType.BOOTS)));

        // Hotbar Slot 1 = Index 0
        ItemStack sword = createToolItem(EquipmentType.SWORD, getLevel(key, EquipmentType.SWORD));
        if (sword != null) {
            player.getInventory().setItem(0, sword);
        }

        // Hotbar Slot 2 = Index 1
        ItemStack axe = createToolItem(EquipmentType.AXE, getLevel(key, EquipmentType.AXE));
        if (axe != null) {
            player.getInventory().setItem(1, axe);
        }

        // Hotbar Slot 3 = Index 2
        if (getLevel(key, EquipmentType.BOW) > 0) {
            player.getInventory().setItem(2, new ItemStack(Material.BOW));

            // 1 Stack Pfeile ins Inventar
            player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
        }

        // Hotbar Slot 4 = Index 3
        ItemStack pickaxe = createToolItem(EquipmentType.PICKAXE, getLevel(key, EquipmentType.PICKAXE));
        if (pickaxe != null) {
            player.getInventory().setItem(3, pickaxe);
        }

        // Hotbar Slot 5 = Index 4
        int foodLevel = getLevel(key, EquipmentType.FOOD);

        if (foodLevel == 1) {
            player.getInventory().setItem(4, new ItemStack(Material.COOKED_BEEF, 16));
        } else if (foodLevel == 2) {
            player.getInventory().setItem(4, new ItemStack(Material.GOLDEN_CARROT, 16));
        }

        // Hotbar Slot 6 = Index 5
        int opFoodLevel = getLevel(key, EquipmentType.OP_FOOD);

        if (opFoodLevel == 1) {
            player.getInventory().setItem(5, new ItemStack(Material.GOLDEN_APPLE, 2));
        } else if (opFoodLevel == 2) {
            player.getInventory().setItem(5, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
        }

        // Schild in die Offhand
        if (getLevel(key, EquipmentType.SHIELD) > 0) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
        }

        player.updateInventory();
        player.sendMessage(ChatColor.GREEN + "✔ Du hast dein Team-Equipment erhalten.");
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

    public String getLevelName(EquipmentType type, int level) {
        if (level <= 0) {
            return ChatColor.RED + "Kein Equipment";
        }

        if (type.isArmor()) {
            return switch (level) {
                case 1 -> ChatColor.GRAY + "Leder";
                case 2 -> ChatColor.WHITE + "Eisen";
                case 3 -> ChatColor.AQUA + "Diamant";
                case 4 -> ChatColor.DARK_PURPLE + "Netherite";
                default -> ChatColor.RED + "Unbekannt";
            };
        }

        if (type.isTool()) {
            return switch (level) {
                case 1 -> ChatColor.DARK_GRAY + "Stein";
                case 2 -> ChatColor.WHITE + "Eisen";
                case 3 -> ChatColor.AQUA + "Diamant";
                case 4 -> ChatColor.DARK_PURPLE + "Netherite";
                default -> ChatColor.RED + "Unbekannt";
            };
        }

        return switch (type) {
            case BOW -> ChatColor.GREEN + "Bogen + Pfeile";
            case FOOD -> level == 2
                    ? ChatColor.GOLD + "Goldene Karotten"
                    : ChatColor.GREEN + "Steak";
            case SHIELD -> ChatColor.GREEN + "Schild";
            case OP_FOOD -> level == 2
                    ? ChatColor.GOLD + "Verzauberter Goldapfel"
                    : ChatColor.GOLD + "Goldapfel";
            default -> ChatColor.RED + "Unbekannt";
        };
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

    public void resetAllEquipment() {
        for (String team : List.of("blue", "red")) {
            for (EquipmentType type : EquipmentType.values()) {
                config.set(team + "." + type.getPath(), 0);
            }
        }

        save();
    }
}