package runi.myddns.mobarmywars.GUIs;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import runi.myddns.mobarmywars.Managers.Event.MobSaveManager;
import runi.myddns.mobarmywars.Arena.WaveManager;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.*;
import java.util.stream.Collectors;

public class UnifiedWaveGUI implements Listener {

    enum Mode {SELECT_WAVE, MOB_SELECTION, OVERVIEW}

    private final Player player;
    private String team;
    private final WaveManager waveManager;
    private final MobSaveManager mobSaveManager;
    private static final NamespacedKey MOB_TYPE_KEY = new NamespacedKey(MobArmyMain.getInstance(), "mob_type");
    Mode currentMode = Mode.SELECT_WAVE;
    int currentWaveIndex = 0;

    public UnifiedWaveGUI(
            WaveManager waveManager,
            MobSaveManager mobSaveManager,
            Player player,
            String team
    ) {
        this(waveManager, mobSaveManager, player, team, Mode.SELECT_WAVE, 0);
    }

    public UnifiedWaveGUI(
            WaveManager waveManager,
            MobSaveManager mobSaveManager,
            Player player,
            String team,
            Mode mode,
            int waveIndex
    ) {
        this.waveManager = waveManager;
        this.mobSaveManager = mobSaveManager;
        this.player = player;
        this.currentMode = mode;
        this.currentWaveIndex = waveIndex;

        if (team == null || team.equalsIgnoreCase("Kein Team")) {
            player.sendMessage(
                    ChatColor.DARK_RED + "✖ " +
                            ChatColor.GRAY + "Du musst einem Team angehören, um Waves zu konfigurieren."
            );
            return;
        }

        this.team = team;

        openGUI();
        Bukkit.getPluginManager().registerEvents(this, MobArmyMain.getInstance());
    }

    private void openGUI() {
        switch (currentMode) {
            case SELECT_WAVE -> openWaveSelection();
            case MOB_SELECTION -> openMobSelection(currentWaveIndex);
        }
    }

    private void openWaveSelection() {
        Inventory gui = Bukkit.createInventory(null, 18, ChatColor.DARK_AQUA + "MobArmy » Wave-Menü");

        gui.setItem(2, createWaveItem(0));
        gui.setItem(4, createWaveItem(1));
        gui.setItem(6, createWaveItem(2));

        gui.setItem(9, createItem(Material.LIME_WOOL, ChatColor.GREEN + "Fertig"));

        gui.setItem(17, createItem(Material.BARRIER, "Waves zurücksetzen"));

        player.openInventory(gui);
    }
    private ItemStack createWaveItem(int waveIndex) {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();

        String title = "Wave " + (waveIndex + 1) + " konfigurieren";
        meta.setDisplayName(ChatColor.YELLOW + title);

        List<Map<String, Integer>> waves = waveManager.getAllWaves(team);
        Map<String, Integer> wave = waveIndex < waves.size() ? waves.get(waveIndex) : new HashMap<>();

        List<String> lore = getSortedMobs(wave).stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> {
                    String type = e.getKey();
                    int count = e.getValue();

                    String base = type.replace("ADULT_", "").replace("BABY_", "").toLowerCase();
                    String suffix = type.startsWith("BABY_") ? " Baby" : "";

                    String name = capitalize(base.replace("_", " ")) + suffix;

                    return ChatColor.GRAY + (count + "x " + name);
                })
                .collect(Collectors.toList());

        if (lore.isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + "Noch keine Mobs ausgewählt");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private List<Map.Entry<String, Integer>> getSortedMobs(Map<String, Integer> mobMap) {
        return mobMap.entrySet().stream()
                .sorted((a, b) -> {
                    String baseA = a.getKey().replace("BABY_", "").replace("ADULT_", "");
                    String baseB = b.getKey().replace("BABY_", "").replace("ADULT_", "");

                    int cmp = baseA.compareTo(baseB);
                    if (cmp != 0) return cmp;

                    if (a.getKey().startsWith("BABY_") && b.getKey().startsWith("ADULT_")) return -1;
                    if (a.getKey().startsWith("ADULT_") && b.getKey().startsWith("BABY_")) return 1;

                    return a.getKey().compareTo(b.getKey());
                })
                .collect(Collectors.toList());
    }

    private void openMobSelection(int waveIndex) {

        Inventory gui = Bukkit.createInventory(
                null,
                54,
                ChatColor.GOLD + "Wave " + (waveIndex + 1) + " Mobs"
        );

        Set<String> mobTypes = mobSaveManager.getAllKnownMobTypes(team);

        List<Map.Entry<String, Integer>> sorted =
                mobTypes.stream()
                        .map(type -> Map.entry(type, mobSaveManager.getMobCount(team, type)))
                        .collect(Collectors.toList());

        sorted = getSortedMobs(
                sorted.stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );

        int slot = 0;
        for (Map.Entry<String, Integer> entry : sorted) {
            String mobType = entry.getKey();
            int available = entry.getValue();

            gui.setItem(slot++, createMobItem(mobType, available));
        }

        gui.setItem(49, createItem(Material.EMERALD_BLOCK, "Speichern & Zurück"));
        player.openInventory(gui);
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMobItem(String mobType, int available) {

        String baseType = mobType.replace("ADULT_", "").replace("BABY_", "").toLowerCase();
        String suffix = mobType.startsWith("BABY_") ? " Baby" : "";
        String cleanName = formatMobName(baseType) + suffix;
        String displayName = ChatColor.YELLOW + cleanName;

        Material icon;
        try {
            icon = Material.valueOf(baseType.toUpperCase() + "_SPAWN_EGG");
        } catch (IllegalArgumentException e) {
            icon = Material.SPAWNER;
        }

        Map<String, Integer> currentWave =
                waveManager.getAllWaves(team).get(currentWaveIndex);
        int inWave = currentWave.getOrDefault(mobType, 0);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.WHITE + "Eingesetzt: " + inWave);
        lore.add(ChatColor.WHITE + "Verfügbar: " + available);
        lore.add("");

        if (available <= 0) {
            lore.add(ChatColor.RED + "❌ Nicht verfügbar");
        } else {
            lore.add(ChatColor.GREEN + "Linksklick" + ChatColor.GRAY + " zur Wave hinzufügen");
            lore.add(ChatColor.GREEN + "Rechtsklick" + ChatColor.GRAY + " wieder entfernen");
        }

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            if (available <= 0) {
                meta.setDisplayName(ChatColor.DARK_GRAY + cleanName);
            } else {
                meta.setDisplayName(displayName);
            }

            meta.getPersistentDataContainer().set(
                    MOB_TYPE_KEY,
                    org.bukkit.persistence.PersistentDataType.STRING,
                    mobType
            );

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private String formatMobName(String baseType) {
        return Arrays.stream(baseType.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p) || !p.equals(player)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String nameStripped = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String mobType = meta.getPersistentDataContainer().get(MOB_TYPE_KEY, org.bukkit.persistence.PersistentDataType.STRING);
        if (mobType == null) mobType = "";

        String rawName = capitalize(mobType.replace("BABY_", "").replace("ADULT_", "").toLowerCase());

        switch (nameStripped) {
            case "Wave 1 konfigurieren" -> new UnifiedWaveGUI(
                    waveManager,
                    mobSaveManager,
                    player,
                    team,
                    Mode.MOB_SELECTION,
                    0
            );
            case "Wave 2 konfigurieren" -> new UnifiedWaveGUI(
                    waveManager,
                    mobSaveManager,
                    player,
                    team,
                    Mode.MOB_SELECTION,
                    1
            );
            case "Wave 3 konfigurieren" -> new UnifiedWaveGUI(
                    waveManager,
                    mobSaveManager,
                    player,
                    team,
                    Mode.MOB_SELECTION,
                    2
            );
            case "Wellenübersicht anzeigen" -> new UnifiedWaveGUI(
                    waveManager,
                    mobSaveManager,
                    player,
                    team,
                    Mode.OVERVIEW,
                    0);
            case "Speichern & Zurück", "Zurück" -> {
                MobArmyMain.getInstance().getWaveStorage().saveWaves();
                p.sendMessage(ChatColor.BLUE + "✅ Wave gespeichert!");
                new UnifiedWaveGUI(
                        waveManager,
                        mobSaveManager,
                        player,
                        team);
            }
            case "Fertig" -> {
                p.closeInventory();
                p.sendMessage(ChatColor.GREEN + "✔ Wave-Auswahl beendet.");
            }
            case "Waves zurücksetzen" -> {

                List<Map<String, Integer>> waves = waveManager.getAllWaves(team);

                for (Map<String, Integer> wave : waves) {
                    for (Map.Entry<String, Integer> entry : wave.entrySet()) {
                        mobSaveManager.restoreMob(
                                team,
                                entry.getKey(),
                                entry.getValue()
                        );
                    }
                }

                waveManager.resetWaves(team);
                MobArmyMain.getInstance().getWaveStorage().saveWaves();

                p.sendMessage(ChatColor.DARK_RED +
                        "🧨 Alle Waves für dein Team wurden zurückgesetzt und die Mobs wieder freigegeben.");

                new UnifiedWaveGUI(
                        waveManager,
                        mobSaveManager,
                        player,
                        team
                );
            }
            default -> {
                if (currentMode == Mode.MOB_SELECTION && !mobType.isEmpty()) {

                    if (event.getClick().isLeftClick()) {
                        int available = mobSaveManager.getMobCount(team, mobType);
                        if (available <= 0) {
                            p.sendMessage(ChatColor.RED + "❌ Kein " + rawName + " mehr verfügbar!");
                            return;
                        }

                        waveManager.addMobToWave(team, currentWaveIndex, mobType);
                        p.sendMessage(ChatColor.GREEN + "✔ " + rawName + " zur Wave " + (currentWaveIndex + 1) + " hinzugefügt!");
                    } else if (event.getClick().isRightClick()) {
                        Map<String, Integer> currentWave = waveManager.getAllWaves(team).get(currentWaveIndex);
                        int currentAmount = currentWave.getOrDefault(mobType, 0);

                        if (currentAmount <= 0) {
                            p.sendMessage(ChatColor.RED + "🚫 Dieser Mob ist in der aktuellen Wave nicht enthalten.");
                            return;
                        }

                        waveManager.removeMobFromWave(team, currentWaveIndex, mobType);
                        p.sendMessage(ChatColor.YELLOW + "❌ " + rawName + " aus der Wave entfernt.");
                    }

                    Bukkit.getScheduler().runTaskLater(
                            MobArmyMain.getInstance(),
                            () -> new UnifiedWaveGUI(
                                    waveManager,
                                    mobSaveManager,
                                    player,
                                    team,
                                    Mode.MOB_SELECTION,
                                    currentWaveIndex
                            ),
                            1L
                    );
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        if (MobArmyMain.getInstance().getBundleGUI().isTeamInventory(event.getInventory())) return;

        if (!(event.getPlayer() instanceof Player p) || !p.equals(player)) return;

        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
        InventoryDragEvent.getHandlerList().unregister(this);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked().equals(player)) {
            event.setCancelled(true);
        }
    }
}
