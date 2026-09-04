package runi.myddns.mobarmywars.GUIs;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import runi.myddns.mobarmywars.Managers.Event.BlockRandomizerManager;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.util.List;

public class RandomizerExclusionGUI implements Listener {

    private final BlockRandomizerManager blockRandomizerManager;
    private final MobArmyMain plugin;

    private static final String COPPER_PATTERN = "*COPPER*";

    private final List<Material> bossSpawnEggs = List.of(
            Material.ENDER_DRAGON_SPAWN_EGG,
            Material.WITHER_SPAWN_EGG,
            Material.WARDEN_SPAWN_EGG,
            Material.ELDER_GUARDIAN_SPAWN_EGG
    );

    private final List<Material> allSpawnEggs = List.of(
            Material.ENDER_DRAGON_SPAWN_EGG,
            Material.ELDER_GUARDIAN_SPAWN_EGG,
            Material.GUARDIAN_SPAWN_EGG,
            Material.WARDEN_SPAWN_EGG,
            Material.WITHER_SPAWN_EGG,
            Material.VEX_SPAWN_EGG,
            Material.BAT_SPAWN_EGG,
            Material.ALLAY_SPAWN_EGG,
            Material.BEE_SPAWN_EGG,
            Material.PARROT_SPAWN_EGG,
            Material.PHANTOM_SPAWN_EGG,
            Material.GHAST_SPAWN_EGG,
            Material.HAPPY_GHAST_SPAWN_EGG,
            Material.BREEZE_SPAWN_EGG,
            Material.BLAZE_SPAWN_EGG,
            Material.EVOKER_SPAWN_EGG,
            Material.SHULKER_SPAWN_EGG,
            Material.WITCH_SPAWN_EGG
    );

    public RandomizerExclusionGUI(
            BlockRandomizerManager blockRandomizerManager,
            MobArmyMain plugin
    ) {
        this.blockRandomizerManager = blockRandomizerManager;
        this.plugin = plugin;
    }

    private List<Material> getNormalSpawnEggs() {
        return allSpawnEggs.stream()
                .filter(egg -> !bossSpawnEggs.contains(egg))
                .toList();
    }

    private ItemStack createEggItem(Material spawnEgg) {

        ItemStack item = new ItemStack(spawnEgg);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        String name = spawnEgg.name()
                .replace("_SPAWN_EGG", "");

        meta.displayName(
                Component.text(name)
        );

        meta.lore(
                List.of(
                        blockRandomizerManager.isSpawnEggBlocked(spawnEgg)
                                ? lang("randomizer-exclusion-gui.spawn-egg.disabled")
                                : lang("randomizer-exclusion-gui.spawn-egg.enabled")
                )
        );

        item.setItemMeta(meta);

        return item;
    }

    public void openGUI(Player player) {

        Inventory gui = Bukkit.createInventory(
                null,
                54,
                lang("randomizer-exclusion-gui.title")
        );

        gui.setItem(
                4,
                createPatternItem(
                        lang("randomizer-exclusion-gui.copper.name"),
                        lang("randomizer-exclusion-gui.copper.description")
                )
        );

        int[] bossSlots = {11, 12, 14, 15};

        for (int i = 0;
             i < bossSpawnEggs.size() && i < bossSlots.length;
             i++) {

            gui.setItem(
                    bossSlots[i],
                    createEggItem(bossSpawnEggs.get(i))
            );
        }

        List<Material> normalEggs = getNormalSpawnEggs();

        int placed = 0;
        int[] rowStarts = {18, 27, 36};

        for (int rowStart : rowStarts) {

            if (placed >= normalEggs.size()) {
                break;
            }

            int remaining =
                    normalEggs.size() - placed;

            int rowCount =
                    Math.min(7, remaining);

            int startOffset =
                    1 + (7 - rowCount) / 2;

            for (int j = 0; j < rowCount; j++) {

                gui.setItem(
                        rowStart + startOffset + j,
                        createEggItem(
                                normalEggs.get(placed++)
                        )
                );
            }
        }

        gui.setItem(
                49,
                createBackButton()
        );

        player.openInventory(gui);
    }

    private ItemStack createPatternItem(
            Component name,
            Component description
    ) {

        ItemStack item = new ItemStack(Material.COPPER_BLOCK);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        boolean blocked =
                blockRandomizerManager
                        .isBlockPatternBlocked(COPPER_PATTERN);

        meta.displayName(name);

        meta.lore(
                List.of(
                        Component.empty(),
                        description,
                        langPattern(COPPER_PATTERN),
                        Component.empty(),
                        blocked
                                ? lang("randomizer-exclusion-gui.spawn-egg.disabled")
                                : lang("randomizer-exclusion-gui.spawn-egg.enabled"),
                        Component.empty(),
                        lang("randomizer-exclusion-gui.click-toggle")
                )
        );

        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createBackButton() {

        ItemStack item =
                new ItemStack(Material.ARROW);

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(
                lang("randomizer-exclusion-gui.back")
        );

        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!event.getView().title().equals(
                lang("randomizer-exclusion-gui.title")
        )) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked()
                instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null) {
            return;
        }

        if (event.getRawSlot()
                >= event.getView()
                .getTopInventory()
                .getSize()) {
            return;
        }

        ItemStack clickedItem =
                event.getCurrentItem();

        if (clickedItem == null
                || clickedItem.getType() == Material.AIR) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot == 49) {

            Sounds.playClick(player);

            plugin.getWorldSettingsGUI()
                    .open(player);

            return;
        }

        if (slot == 4) {

            blockRandomizerManager
                    .toggleBlockedBlockPattern(
                            COPPER_PATTERN
                    );

            Sounds.playClick(player);

            openGUI(player);
            return;
        }

        Material material =
                clickedItem.getType();

        if (!material.name()
                .endsWith("_SPAWN_EGG")) {
            return;
        }

        blockRandomizerManager
                .toggleSpawnEgg(material);

        Sounds.playClick(player);

        openGUI(player);
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }

    private Component langPattern(
            String pattern
    ) {

        return plugin.getLanguageManager()
                .getComponent(
                        "randomizer-exclusion-gui.copper.pattern",
                        "pattern",
                        pattern
                );
    }
}