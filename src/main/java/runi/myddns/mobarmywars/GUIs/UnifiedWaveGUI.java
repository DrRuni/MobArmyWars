package runi.myddns.mobarmywars.GUIs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import runi.myddns.mobarmywars.Managers.Event.MobSaveManager;
import runi.myddns.mobarmywars.Managers.Event.WaveManager;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UnifiedWaveGUI implements Listener {

    public enum Mode {
        SELECT_WAVE,
        MOB_SELECTION,
        OVERVIEW
    }

    private final MobArmyMain plugin;
    private final WaveManager waveManager;
    private final MobSaveManager mobSaveManager;
    private final Player player;
    private final NamespacedKey mobTypeKey;

    private String team;
    private final Mode currentMode;
    private final int currentWaveIndex;
    private Inventory currentInventory;

    public UnifiedWaveGUI(
            WaveManager waveManager,
            MobSaveManager mobSaveManager,
            Player player,
            String team
    ) {
        this(
                waveManager,
                mobSaveManager,
                player,
                team,
                Mode.SELECT_WAVE,
                0
        );
    }

    public UnifiedWaveGUI(
            WaveManager waveManager,
            MobSaveManager mobSaveManager,
            Player player,
            String team,
            Mode mode,
            int waveIndex
    ) {
        this.plugin = MobArmyMain.getInstance();
        this.waveManager = waveManager;
        this.mobSaveManager = mobSaveManager;
        this.player = player;
        this.currentMode = mode;
        this.currentWaveIndex = waveIndex;
        this.mobTypeKey = new NamespacedKey(plugin, "mob_type");

        if (team == null
                || team.equalsIgnoreCase("Kein Team")) {

            player.sendMessage(
                    lang("unified-wave-gui.no-team")
            );

            return;
        }

        this.team = team;

        openGUI();

        Bukkit.getPluginManager()
                .registerEvents(this, plugin);
    }

    private void openGUI() {

        switch (currentMode) {

            case SELECT_WAVE ->
                    openWaveSelection();

            case MOB_SELECTION ->
                    openMobSelection(currentWaveIndex);

            case OVERVIEW -> {
            }
        }
    }

    private void openWaveSelection() {

        Inventory gui = Bukkit.createInventory(
                null,
                18,
                lang("unified-wave-gui.selection.title")
        );

        gui.setItem(2, createWaveItem(0));
        gui.setItem(4, createWaveItem(1));
        gui.setItem(6, createWaveItem(2));

        gui.setItem(
                9,
                createItem(
                        Material.LIME_WOOL,
                        lang("unified-wave-gui.selection.finish")
                )
        );

        gui.setItem(
                17,
                createItem(
                        Material.BARRIER,
                        lang("unified-wave-gui.selection.reset")
                )
        );

        currentInventory = gui;
        player.openInventory(gui);
    }

    private ItemStack createWaveItem(int waveIndex) {

        ItemStack item =
                new ItemStack(Material.DIAMOND_SWORD);

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(
                langWave(
                        "unified-wave-gui.selection.wave",
                        waveIndex + 1
                )
        );

        Map<String, Integer> wave =
                waveManager.getWaveAsCountMap(
                        team,
                        waveIndex
                );

        List<Component> lore =
                getSortedMobs(wave)
                        .stream()
                        .filter(entry ->
                                entry.getValue() > 0
                        )
                        .map(entry -> {

                            String type =
                                    entry.getKey();

                            int count =
                                    entry.getValue();

                            String base =
                                    type.replace("ADULT_", "")
                                            .replace("BABY_", "")
                                            .toLowerCase();

                            String suffix =
                                    type.startsWith("BABY_")
                                            ? " Baby"
                                            : "";

                            String name =
                                    capitalize(
                                            base.replace("_", " ")
                                    ) + suffix;

                            return Component.text(
                                    count + "x " + name,
                                    NamedTextColor.GRAY
                            );
                        })
                        .collect(Collectors.toCollection(ArrayList::new));

        if (lore.isEmpty()) {

            lore.add(
                    lang("unified-wave-gui.selection.empty")
            );
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);

        return item;
    }

    private List<Map.Entry<String, Integer>> getSortedMobs(
            Map<String, Integer> mobMap
    ) {

        return mobMap.entrySet()
                .stream()
                .sorted((a, b) -> {

                    String baseA =
                            a.getKey()
                                    .replace("BABY_", "")
                                    .replace("ADULT_", "");

                    String baseB =
                            b.getKey()
                                    .replace("BABY_", "")
                                    .replace("ADULT_", "");

                    int compare =
                            baseA.compareTo(baseB);

                    if (compare != 0) {
                        return compare;
                    }

                    if (a.getKey().startsWith("BABY_")
                            && b.getKey().startsWith("ADULT_")) {

                        return -1;
                    }

                    if (a.getKey().startsWith("ADULT_")
                            && b.getKey().startsWith("BABY_")) {

                        return 1;
                    }

                    return a.getKey()
                            .compareTo(b.getKey());
                })
                .toList();
    }

    private void openMobSelection(int waveIndex) {

        Inventory gui = Bukkit.createInventory(
                null,
                54,
                langWave(
                        "unified-wave-gui.mobs.title",
                        waveIndex + 1
                )
        );

        Set<String> mobTypes =
                mobSaveManager.getAllKnownMobTypes(team);

        Map<String, Integer> mobMap =
                mobTypes.stream()
                        .collect(
                                Collectors.toMap(
                                        type -> type,
                                        type -> mobSaveManager
                                                .getMobCount(team, type)
                                )
                        );

        List<Map.Entry<String, Integer>> sorted =
                getSortedMobs(mobMap);

        int slot = 0;

        for (Map.Entry<String, Integer> entry : sorted) {

            if (slot >= 49) {
                break;
            }

            gui.setItem(
                    slot++,
                    createMobItem(
                            entry.getKey(),
                            entry.getValue()
                    )
            );
        }

        gui.setItem(
                49,
                createItem(
                        Material.EMERALD_BLOCK,
                        lang("unified-wave-gui.mobs.save-back")
                )
        );

        currentInventory = gui;
        player.openInventory(gui);
    }

    private ItemStack createItem(
            Material material,
            Component name
    ) {

        ItemStack item =
                new ItemStack(material);

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(name);

        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createMobItem(
            String mobType,
            int available
    ) {

        String baseType =
                mobType.replace("ADULT_", "")
                        .replace("BABY_", "")
                        .toLowerCase();

        String suffix =
                mobType.startsWith("BABY_")
                        ? " Baby"
                        : "";

        String cleanName =
                formatMobName(baseType) + suffix;

        Material icon;

        try {

            icon = Material.valueOf(
                    baseType.toUpperCase()
                            + "_SPAWN_EGG"
            );

        } catch (IllegalArgumentException exception) {

            icon = Material.SPAWNER;
        }

        int inWave =
                waveManager.getMobAmountInWave(
                        team,
                        currentWaveIndex,
                        mobType
                );

        List<Component> lore =
                new ArrayList<>();

        lore.add(
                langAmount(
                        "unified-wave-gui.mobs.used",
                        inWave
                )
        );

        lore.add(
                langAmount(
                        "unified-wave-gui.mobs.available",
                        available
                )
        );

        lore.add(Component.empty());

        if (available <= 0
                && inWave <= 0) {

            lore.add(
                    lang("unified-wave-gui.mobs.unavailable")
            );

        } else {

            lore.add(
                    lang("unified-wave-gui.mobs.left")
            );

            lore.add(
                    lang("unified-wave-gui.mobs.right")
            );

            lore.add(
                    lang("unified-wave-gui.mobs.shift")
            );
        }

        ItemStack item =
                new ItemStack(icon);

        if (inWave > 0) {

            item.setAmount(
                    Math.min(inWave, 64)
            );
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        if (available <= 0
                && inWave <= 0) {

            meta.displayName(
                    Component.text(
                            cleanName,
                            NamedTextColor.DARK_GRAY
                    )
            );

        } else {

            meta.displayName(
                    Component.text(
                            cleanName,
                            NamedTextColor.YELLOW
                    )
            );
        }

        meta.getPersistentDataContainer()
                .set(
                        mobTypeKey,
                        PersistentDataType.STRING,
                        mobType
                );

        meta.lore(lore);

        item.setItemMeta(meta);

        return item;
    }

    private String formatMobName(
            String baseType
    ) {

        return Arrays.stream(
                        baseType.split("_")
                )
                .map(word ->
                        word.substring(0, 1)
                                .toUpperCase()
                                + word.substring(1)
                                .toLowerCase()
                )
                .collect(Collectors.joining(" "));
    }

    private String capitalize(String value) {

        if (value == null
                || value.isEmpty()) {

            return value;
        }

        return value.substring(0, 1)
                .toUpperCase()
                + value.substring(1);
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!(event.getWhoClicked()
                instanceof Player clickedPlayer)
                || !clickedPlayer.equals(player)) {

            return;
        }

        if (currentInventory == null
                || !event.getView()
                .getTopInventory()
                .equals(currentInventory)) {

            return;
        }

        event.setCancelled(true);

        int slot =
                event.getRawSlot();

        if (slot < 0
                || slot >= currentInventory.getSize()) {

            return;
        }

        if (currentMode == Mode.SELECT_WAVE) {

            switch (slot) {

                case 2 ->
                        openMobSelectionGUI(0);

                case 4 ->
                        openMobSelectionGUI(1);

                case 6 ->
                        openMobSelectionGUI(2);

                case 9 -> {

                    player.closeInventory();

                    player.sendMessage(
                            lang("unified-wave-gui.messages.finished")
                    );
                }

                case 17 ->
                        resetWaves();
            }

            return;
        }

        if (currentMode != Mode.MOB_SELECTION) {
            return;
        }

        if (slot == 49) {

            plugin.getWaveStorage()
                    .saveWaves();

            player.sendMessage(
                    lang("unified-wave-gui.messages.saved")
            );

            new UnifiedWaveGUI(
                    waveManager,
                    mobSaveManager,
                    player,
                    team
            );

            return;
        }

        ItemStack clicked =
                event.getCurrentItem();

        if (clicked == null
                || !clicked.hasItemMeta()) {

            return;
        }

        ItemMeta meta =
                clicked.getItemMeta();

        if (meta == null) {
            return;
        }

        String mobType =
                meta.getPersistentDataContainer()
                        .get(
                                mobTypeKey,
                                PersistentDataType.STRING
                        );

        if (mobType == null
                || mobType.isBlank()) {

            return;
        }

        int amount =
                event.getClick().isShiftClick()
                        ? 10
                        : 1;

        if (event.getClick().isLeftClick()) {

            int available =
                    mobSaveManager.getMobCount(
                            team,
                            mobType
                    );

            if (available <= 0) {
                return;
            }

            int toAdd =
                    Math.min(
                            amount,
                            available
                    );

            for (int i = 0; i < toAdd; i++) {

                waveManager.addMobToWave(
                        team,
                        currentWaveIndex,
                        mobType
                );
            }

        } else if (event.getClick().isRightClick()) {

            int currentAmount =
                    waveManager.getMobAmountInWave(
                            team,
                            currentWaveIndex,
                            mobType
                    );

            if (currentAmount <= 0) {
                return;
            }

            int toRemove =
                    Math.min(
                            amount,
                            currentAmount
                    );

            for (int i = 0; i < toRemove; i++) {

                waveManager.removeMobFromWave(
                        team,
                        currentWaveIndex,
                        mobType
                );
            }

        } else {

            return;
        }

        Bukkit.getScheduler().runTaskLater(
                plugin,
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

    private void openMobSelectionGUI(
            int waveIndex
    ) {

        new UnifiedWaveGUI(
                waveManager,
                mobSaveManager,
                player,
                team,
                Mode.MOB_SELECTION,
                waveIndex
        );
    }

    private void resetWaves() {

        List<List<WaveManager.WaveEntry>> waves =
                waveManager.getAllWaves(team);

        for (List<WaveManager.WaveEntry> wave : waves) {

            for (WaveManager.WaveEntry entry : wave) {

                mobSaveManager.restoreMob(
                        team,
                        entry.getMobType(),
                        entry.getAmount()
                );
            }
        }

        waveManager.resetWaves(team);

        plugin.getWaveStorage()
                .saveWaves();

        player.sendMessage(
                lang("unified-wave-gui.messages.reset")
        );

        new UnifiedWaveGUI(
                waveManager,
                mobSaveManager,
                player,
                team
        );
    }

    @EventHandler
    public void onInventoryClose(
            InventoryCloseEvent event
    ) {

        if (!event.getPlayer().equals(player)) {
            return;
        }

        if (currentInventory == null
                || !event.getInventory()
                .equals(currentInventory)) {

            return;
        }

        InventoryClickEvent.getHandlerList()
                .unregister(this);

        InventoryCloseEvent.getHandlerList()
                .unregister(this);

        InventoryDragEvent.getHandlerList()
                .unregister(this);
    }

    @EventHandler
    public void onInventoryDrag(
            InventoryDragEvent event
    ) {

        if (!event.getWhoClicked().equals(player)) {
            return;
        }

        if (currentInventory == null
                || !event.getView()
                .getTopInventory()
                .equals(currentInventory)) {

            return;
        }

        event.setCancelled(true);
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }

    private Component langWave(
            String path,
            Object wave
    ) {

        return plugin.getLanguageManager()
                .getComponent(
                        path,
                        "wave",
                        wave
                );
    }

    private Component langAmount(
            String path,
            Object amount
    ) {

        return plugin.getLanguageManager()
                .getComponent(
                        path,
                        "amount",
                        amount
                );
    }
}