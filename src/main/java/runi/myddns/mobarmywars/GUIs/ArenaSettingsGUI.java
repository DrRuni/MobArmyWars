package runi.myddns.mobarmywars.GUIs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import runi.myddns.mobarmywars.Managers.Event.ArenaConfig;
import runi.myddns.mobarmywars.Managers.World.ResumeManager;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ArenaSettingsGUI implements Listener {

    private final MobArmyMain plugin;
    private final NamespacedKey arenaIdKey;

    public ArenaSettingsGUI(MobArmyMain plugin) {
        this.plugin = plugin;
        this.arenaIdKey = new NamespacedKey(plugin, "arena_id");
    }

    public void open(Player player) {

        Inventory inv = Bukkit.createInventory(
                null,
                54,
                lang("arena-settings.title")
        );

        inv.setItem(11, createItem(
                Material.IRON_SWORD,
                lang("arena-settings.reset.name"),
                Component.empty(),
                lang("arena-settings.reset.description")
        ));

        inv.setItem(13, createItem(
                plugin.getWorldSettings().isArenaCompassEnabled()
                        ? Material.COMPASS
                        : Material.GRAY_DYE,

                lang("arena-settings.compass.name"),

                Component.empty(),
                lang("arena-settings.compass.description"),

                plugin.getWorldSettings().isArenaCompassEnabled()
                        ? lang("arena-settings.compass.status-active")
                        : lang("arena-settings.compass.status-disabled")
        ));

        inv.setItem(15, createItem(
                Material.COMPASS,
                lang("arena-settings.give-compass.name"),
                Component.empty(),
                lang("arena-settings.give-compass.description")
        ));

        addArenaButtons(inv);

        inv.setItem(49, createBackButton());

        player.openInventory(inv);
    }

    private void addArenaButtons(Inventory inv) {

        List<ArenaConfig.ArenaInfo> arenas =
                plugin.getArenaConfig().getAvailableArenas();

        String activeArenaId =
                plugin.getArenaConfig().getActiveArenaId();

        int[] imageSlots = {29, 33};
        int[] buttonSlots = {38, 42};

        for (int i = 0; i < arenas.size() && i < 2; i++) {

            ArenaConfig.ArenaInfo arena = arenas.get(i);

            boolean active =
                    arena.id().equalsIgnoreCase(activeArenaId);

            inv.setItem(
                    imageSlots[i],
                    createArenaPreviewItem(arena, active, i)
            );

            inv.setItem(
                    buttonSlots[i],
                    createArenaSwitchButton(arena, active)
            );
        }
    }

    private ItemStack createArenaPreviewItem(
            ArenaConfig.ArenaInfo arena,
            boolean active,
            int index
    ) {

        Material material = switch (index) {
            case 0 -> Material.CHERRY_SAPLING;
            case 1 -> Material.SCULK_SENSOR;
            default -> Material.MAP;
        };

        return createItem(
                material,

                Component.text(
                        arena.name(),
                        NamedTextColor.GOLD
                ),

                Component.empty(),

                Component.text(
                        arena.description(),
                        NamedTextColor.GRAY
                ),

                Component.empty(),

                active
                        ? lang("arena-settings.arena.selected")
                        : lang("arena-settings.arena.not-selected")
        );
    }

    private ItemStack createArenaSwitchButton(
            ArenaConfig.ArenaInfo arena,
            boolean active
    ) {

        ItemStack item = createItem(
                active
                        ? Material.LIME_DYE
                        : Material.RED_DYE,

                active
                        ? langArena(
                        "arena-settings.arena.active",
                        arena.name()
                )
                        : langArena(
                        "arena-settings.arena.activate",
                        arena.name()

                ),

                Component.empty(),

                active
                        ? lang("arena-settings.arena.currently-active")
                        : lang("arena-settings.arena.click-to-activate")
        );

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            meta.getPersistentDataContainer().set(
                    arenaIdKey,
                    PersistentDataType.STRING,
                    arena.id()
            );

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createItem(
            Material material,
            Component name,
            Component... loreLines
    ) {

        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        // Moderner Component-Name
        meta.displayName(name);

        // Moderne Component-Lore
        List<Component> lore = new ArrayList<>();

        if (loreLines != null) {

            for (Component line : loreLines) {

                if (line != null) {
                    lore.add(line);
                }
            }
        }

        meta.lore(lore);

        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE
        );

        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createBackButton() {

        ItemStack item = new ItemStack(Material.ARROW);

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(
                lang("arena-settings.back")
        );

        meta.lore(List.of());

        item.setItemMeta(meta);

        return item;
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!event.getView().title().equals(
                lang("arena-settings.title")
        )) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null) {
            return;
        }

        if (event.getRawSlot()
                >= event.getView().getTopInventory().getSize()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();

        if (clicked == null
                || clicked.getType() == Material.AIR
                || !clicked.hasItemMeta()) {
            return;
        }

        int slot = event.getRawSlot();

        switch (slot) {

            // Arena zurücksetzen
            case 11 -> resetArena(player);

            // Arena-Kompass aktivieren / deaktivieren
            case 13 -> toggleArenaCompass(player);

            // Arena-Kompass an Spieler geben
            case 15 -> giveArenaCompass(player);

            // Arena-Auswahl
            case 38, 42 -> handleArenaSwitch(
                    player,
                    clicked
            );

            // Zurück
            case 49 -> {

                Sounds.playBack(player);

                plugin.getEventSettingsGUI()
                        .open(player);
            }
        }
    }

    private void resetArena(Player player) {

        Sounds.playDanger(player);

        plugin.getArenaManager().resetArena();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

            onlinePlayer.sendMessage(
                    lang("arena-settings.reset.message")
            );

            Title title = Title.title(
                    lang("arena-settings.reset.title"),
                    lang("arena-settings.reset.subtitle"),
                    Title.Times.times(
                            Duration.ofMillis(500),
                            Duration.ofSeconds(3),
                            Duration.ofSeconds(1)
                    )
            );

            onlinePlayer.showTitle(title);
        }
    }

    private void toggleArenaCompass(Player player) {

        Sounds.playClick(player);

        plugin.getWorldSettings()
                .toggleArenaCompassEnabled();

        boolean enabled =
                plugin.getWorldSettings()
                        .isArenaCompassEnabled();

        if (enabled) {

            player.sendMessage(
                    lang("arena-settings.compass.enabled")
            );

        } else {

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

                plugin.getArenaCompassManager()
                        .removeArenaCompass(onlinePlayer);
            }

            player.sendMessage(
                    lang("arena-settings.compass.disabled")
            );
        }

        open(player);
    }

    private void giveArenaCompass(Player player) {

        Sounds.playClick(player);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

            plugin.getArenaCompassManager()
                    .addMonsterCompass(onlinePlayer);
        }

        player.sendMessage(
                lang("arena-settings.give-compass.success")
        );

        open(player);
    }

    private void handleArenaSwitch(
            Player player,
            ItemStack clicked
    ) {

        String arenaId = getArenaIdFromItem(clicked);

        if (arenaId == null || arenaId.isBlank()) {

            Sounds.playDanger(player);

            player.sendMessage(
                    lang("arena-settings.arena.id-error")
            );

            return;
        }

        String activeArenaId =
                plugin.getArenaConfig()
                        .getActiveArenaId();

        /*
         * Bereits aktive Arena angeklickt
         */
        if (arenaId.equalsIgnoreCase(activeArenaId)) {

            Sounds.playClick(player);

            player.sendMessage(
                    lang("arena-settings.arena.already-active")
            );

            return;
        }

        int phase =
                plugin.getEventResume()
                        .loadPhase();

        /*
         * Arena-Wechsel nur bis zur Wave-Auswahl.
         */
        if (phase > ResumeManager.PHASE_WAVEAUSWAHL) {

            Sounds.playDanger(player);

            player.sendMessage(
                    lang(
                            "arena-settings.arena.switch-not-allowed"
                    )
            );

            return;
        }

        boolean success =
                plugin.getArenaConfig()
                        .setActiveArena(arenaId);

        if (!success) {

            Sounds.playDanger(player);

            player.sendMessage(
                    langArena(
                            "arena-settings.arena.activate-error",
                            arenaId
                    )
            );

            return;
        }

        Sounds.playClick(player);

        player.sendMessage(
                langArena(
                        "arena-settings.arena.activated",
                        arenaId
                )
        );

        open(player);
    }

    private String getArenaIdFromItem(ItemStack item) {

        if (item == null) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return null;
        }

        return meta.getPersistentDataContainer().get(
                arenaIdKey,
                PersistentDataType.STRING
        );
    }

    /*
     * Sprachtext ohne Platzhalter
     */
    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }

    /*
     * Sprachtext mit einem Platzhalter
     */
    private Component langArena(
            String path,
            Object value
    ) {

        return plugin.getLanguageManager()
                .getComponent(
                        path,
                        "arena",
                        value
                );
    }
}