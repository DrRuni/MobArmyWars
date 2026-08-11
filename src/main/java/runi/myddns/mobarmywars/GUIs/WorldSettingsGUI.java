package runi.myddns.mobarmywars.GUIs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import runi.myddns.mobarmywars.Managers.Event.BlockRandomizerManager;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorldSettingsGUI implements Listener {

    private final MobArmyMain plugin;
    private final BlockRandomizerManager blockRandomizerManager;

    private static final String TITLE = ChatColor.BLUE + "Welteinstellungen";

    public WorldSettingsGUI(MobArmyMain plugin, BlockRandomizerManager blockRandomizerManager) {
        this.plugin = plugin;
        this.blockRandomizerManager = blockRandomizerManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);

        boolean randomizerOn = blockRandomizerManager.isGlobalRandomizerEnabled();
        boolean keepInvOn = plugin.getWorldSettings().isKeepInventoryEnabled();
        boolean mobSpawningOn = plugin.getWorldSettings().isMobSpawningEnabled();
        boolean daylightCycleOn = plugin.getWorldSettings().isDaylightCycleEnabled();
        boolean nightVisionOn = plugin.getWorldSettings().isNightVisionEnabled();
        boolean chestRandomizerOn = plugin.getWorldSettings().isChestRandomizerEnabled();
        String difficulty = plugin.getWorldSettings().getDifficulty();
        long currentTime = plugin.getWorldSettings().getCurrentWorldTime();

        inv.setItem(10, createItem(
                randomizerOn ? Material.LIME_WOOL : Material.RED_WOOL,
                (randomizerOn ? ChatColor.GREEN : ChatColor.RED)
                        + "BlockRandomizer: " + (randomizerOn ? "AN" : "AUS"),
                "",
                "Zufällige Blöcke beim Abbauen",
                "global ein- oder ausschalten"
        ));

        inv.setItem(12, createToggleItem(
                "Kisten-Randomizer",
                chestRandomizerOn
        ));

        inv.setItem(14, createItem(
                Material.SPAWNER,
                ChatColor.GOLD + "BlockRandomizer-Ausnahmen",
                "",
                "Blöcke, Gegenstände und Spawn-Eier",
                "vom Zufall ausschließen"
        ));

        inv.setItem(16, createItem(
                Material.TNT,
                ChatColor.DARK_RED + "BlockRandomizer zurücksetzen",
                "",
                "Erstellt eine neue zufällige",
                "Zuordnung aller Blöcke"
        ));

        inv.setItem(20, createDifficultyItem(difficulty));

        inv.setItem(22, createToggleItem(
                "Mob-Spawning",
                mobSpawningOn
        ));

        inv.setItem(24, createItem(
                keepInvOn ? Material.LIME_WOOL : Material.RED_WOOL,
                (keepInvOn ? ChatColor.GREEN : ChatColor.RED)
                        + "Inventar behalten: " + (keepInvOn ? "AN" : "AUS"),
                "",
                "Spieler behalten nach dem Tod",
                "Inventar und Erfahrung"
        ));

        inv.setItem(29, createToggleItem(
                "Nachtsicht",
                nightVisionOn,
                "Gibt allen Spielern dauerhaft Nachtsicht",
                "Nachtsicht ist deaktiviert"
        ));

        inv.setItem(31, createTimeItem(currentTime));

        inv.setItem(33, createToggleItem(
                "Tageslichtzyklus",
                daylightCycleOn,
                "Tag und Nacht wechseln automatisch",
                "Die aktuelle Tageszeit bleibt stehen"
        ));

        inv.setItem(40, createItem(
                Material.ARROW,
                ChatColor.DARK_AQUA + "Zurück",
                "",
                "Zurück zum vorherigen Menü"
        ));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        if (loreLines != null && loreLines.length > 0) {
            for (String line : loreLines) {
                lore.add(ChatColor.GRAY + line);
            }
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createToggleItem(String name, boolean state) {
        return createToggleItem(name, state, null, null);
    }

    private ItemStack createToggleItem(
            String name,
            boolean state,
            String enabledDescription,
            String disabledDescription
    ) {
        Material icon;

        if (name.equalsIgnoreCase("Mob-Spawning")) {
            icon = state ? Material.ZOMBIE_HEAD : Material.PLAYER_HEAD;

        } else if (name.equalsIgnoreCase("Tageslichtzyklus")) {
            icon = state ? Material.CLOCK : Material.DAYLIGHT_DETECTOR;

        } else if (name.equalsIgnoreCase("Nachtsicht")) {
            icon = state ? Material.LIGHT : Material.GRAY_CANDLE;

        } else if (name.equalsIgnoreCase("Kisten-Randomizer")) {
            icon = state ? Material.CHEST : Material.BARRIER;

        } else {
            icon = state ? Material.LIME_WOOL : Material.RED_WOOL;
        }

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(
                (state ? ChatColor.GREEN : ChatColor.RED)
                        + name
                        + ": "
                        + (state ? "AN" : "AUS")
        );

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Status: "
                + (state ? ChatColor.GREEN + "AN" : ChatColor.RED + "AUS"));

        String description = state ? enabledDescription : disabledDescription;

        if (description != null && !description.isBlank()) {
            lore.add("");
            lore.add(ChatColor.GRAY + description);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createDifficultyItem(String difficulty) {
        Material mat;
        ChatColor color;
        String displayName;

        switch (difficulty.toLowerCase(Locale.ROOT)) {
            case "peaceful" -> {
                mat = Material.WHITE_WOOL;
                color = ChatColor.WHITE;
                displayName = "Friedlich";
            }
            case "easy" -> {
                mat = Material.LIME_WOOL;
                color = ChatColor.GREEN;
                displayName = "Leicht";
            }
            case "normal" -> {
                mat = Material.ORANGE_WOOL;
                color = ChatColor.GOLD;
                displayName = "Normal";
            }
            case "hard" -> {
                mat = Material.RED_WOOL;
                color = ChatColor.RED;
                displayName = "Schwer";
            }
            case "ultra-ultra-hardcore" -> {
                mat = Material.PURPLE_WOOL;
                color = ChatColor.DARK_PURPLE;
                displayName = "Ultra-Ultra-Hardcore";
            }
            default -> {
                mat = Material.GRAY_WOOL;
                color = ChatColor.GRAY;
                displayName = "Unbekannt";
            }
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(color + "Schwierigkeit: " + displayName);
        meta.setLore(List.of(
                "",
                ChatColor.GRAY + "Klicke zum Ändern"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTimeItem(long currentTime) {
        String phase;
        ChatColor color;
        Material mat;

        if (currentTime < 6000) {
            phase = "Früh";
            color = ChatColor.GOLD;
            mat = Material.ORANGE_WOOL;
        } else if (currentTime < 12000) {
            phase = "Mittag";
            color = ChatColor.YELLOW;
            mat = Material.YELLOW_WOOL;
        } else if (currentTime < 18000) {
            phase = "Abend";
            color = ChatColor.RED;
            mat = Material.RED_WOOL;
        } else {
            phase = "Nacht";
            color = ChatColor.BLUE;
            mat = Material.BLUE_WOOL;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(color + "Tageszeit: §f" + phase);
        meta.setLore(List.of(
                "",
                ChatColor.GRAY + "Klicke, um zur nächsten Tageszeit zu springen",
                ChatColor.DARK_GRAY + "(" +
                ChatColor.GOLD + "Früh " +
                ChatColor.DARK_GRAY + "→ " +
                ChatColor.YELLOW + "Mittag " +
                ChatColor.DARK_GRAY + "→ " +
                ChatColor.RED + "Abend " +
                ChatColor.DARK_GRAY + "→ " +
                ChatColor.BLUE + "Nacht " +
                ChatColor.DARK_GRAY + "→ " +
                ChatColor.GOLD + "Früh" +
                ChatColor.DARK_GRAY +  ")"
        ));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!ChatColor.stripColor(e.getView().getTitle()).equalsIgnoreCase("Welteinstellungen")) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        if (clicked.getItemMeta() == null || !clicked.getItemMeta().hasDisplayName()) return;

        String itemName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT);
        boolean reopen = true;

        switch (itemName) {
            case "zurück" -> {
                Sounds.playBack(player);
                plugin.getEventSettingsGUI().open(player);
                reopen = false;
            }

            case "blockrandomizer: an", "blockrandomizer: aus" -> {
                Sounds.playClick(player);

                plugin.getWorldSettings().toggleRandomizer();

                boolean newState = plugin.getWorldSettings().isRandomizerEnabled();
                blockRandomizerManager.setGlobalRandomizerEnabled(newState);

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(
                            newState
                                    ? ChatColor.GREEN + "✅ BlockRandomizer aktiviert!"
                                    : ChatColor.RED + "⛔ BlockRandomizer deaktiviert!"
                    );
                }
            }

            case "kisten-randomizer: an", "kisten-randomizer: aus" -> {
                Sounds.playClick(player);

                plugin.getWorldSettings().toggleChestRandomizer();

                boolean newState = plugin.getWorldSettings().isChestRandomizerEnabled();

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(
                            newState
                                    ? ChatColor.GREEN + "✅ Kisten-Randomizer aktiviert!"
                                    : ChatColor.RED + "⛔ Kisten-Randomizer deaktiviert!"
                    );
                }
            }

            case "inventar behalten: an", "inventar behalten: aus" -> {
                Sounds.playClick(player);

                plugin.getWorldSettings().toggleKeepInventory();

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(
                            plugin.getWorldSettings().isKeepInventoryEnabled()
                                    ? ChatColor.GREEN + "✅ KeepInventory aktiviert!"
                                    : ChatColor.RED + "⛔ KeepInventory deaktiviert!"
                    );
                }
            }

            case "mob-spawning: an", "mob-spawning: aus" -> {
                Sounds.playClick(player);
                plugin.getWorldSettings().toggleMobSpawning();

                boolean newState = plugin.getWorldSettings().isMobSpawningEnabled();
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(
                            newState
                                    ? ChatColor.GREEN + "✅ Mob-Spawning aktiviert!"
                                    : ChatColor.RED + "⛔ Mob-Spawning deaktiviert!"
                    );
                }
            }

            case "tageslichtzyklus: an", "tageslichtzyklus: aus" -> {
                Sounds.playClick(player);

                plugin.getWorldSettings().toggleDaylightCycle();

                boolean newState = plugin.getWorldSettings().isDaylightCycleEnabled();

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(
                            newState
                                    ? ChatColor.GREEN + "✅ Tageslichtzyklus aktiviert!"
                                    : ChatColor.RED + "⛔ Tageslichtzyklus deaktiviert!"
                    );
                }
            }

            case "blockrandomizer-ausnahmen" -> {
                Sounds.playClick(player);
                plugin.getSpawnEggGUI().openGUI(player);
                reopen = false;
            }

            case "blockrandomizer zurücksetzen" -> {
                Sounds.playReset(player);
                blockRandomizerManager.resetRandomizer();

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(ChatColor.GREEN + "🔁 BlockRandomizer zurückgesetzt!");
                }
            }

            case "nachtsicht: an", "nachtsicht: aus" -> {
                Sounds.playClick(player);

                plugin.getWorldSettings().toggleNightVision();
                plugin.getPlayerEffectManager().applyNightVisionToAll();

                boolean newState = plugin.getWorldSettings().isNightVisionEnabled();

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(
                            newState
                                    ? ChatColor.GREEN + "✅ Nachtsicht aktiviert!"
                                    : ChatColor.RED + "⛔ Nachtsicht deaktiviert!"
                    );
                }
            }

            default -> {
                if (itemName.startsWith("schwierigkeit:")) {
                    Sounds.playClick(player);
                    handleDifficultyClick();
                } else if (itemName.startsWith("tageszeit:")) {
                    Sounds.playClick(player);
                    handleTimeClick();
                }
            }
        }

        if (reopen) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 2L);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (ChatColor.stripColor(e.getView().getTitle()).equalsIgnoreCase("Welteinstellungen")) {
            e.setCancelled(true);
        }
    }

    private void handleDifficultyClick() {
        String next = plugin.getWorldSettings().cycleDifficulty();

        ChatColor diffColor = switch (next.toLowerCase(Locale.ROOT)) {
            case "peaceful" -> ChatColor.WHITE;
            case "easy" -> ChatColor.GREEN;
            case "normal" -> ChatColor.GOLD;
            case "hard" -> ChatColor.RED;
            case "ultra-ultra-hardcore" -> ChatColor.DARK_PURPLE;
            default -> ChatColor.GRAY;
        };

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ChatColor.AQUA + "⚔ "
                    + ChatColor.BLUE + "Schwierigkeit geändert: "
                    + diffColor + next.toUpperCase(Locale.ROOT));
        }
    }

    private void handleTimeClick() {
        long next = plugin.getWorldSettings().cycleTime();

        String phaseName;
        ChatColor phaseColor;

        if (next < 6000) {
            phaseName = "Früh";
            phaseColor = ChatColor.GOLD;
        } else if (next < 12000) {
            phaseName = "Mittag";
            phaseColor = ChatColor.YELLOW;
        } else if (next < 18000) {
            phaseName = "Abend";
            phaseColor = ChatColor.RED;
        } else {
            phaseName = "Nacht";
            phaseColor = ChatColor.BLUE;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ChatColor.AQUA + "🕒 "
                    + ChatColor.BLUE + "Tageszeit geändert: "
                    + phaseColor + phaseName);
        }
    }
}