package runi.myddns.mobarmywars.GUIs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import runi.myddns.mobarmywars.Managers.Event.BlockRandomizerManager;
import runi.myddns.mobarmywars.Managers.Event.TimerManager;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.util.Arrays;
import java.util.List;

public class SpawnEggGUI implements Listener {

    private final BlockRandomizerManager blockRandomizerManager;
    private final MobArmyMain plugin;
    private final TimerManager timerManager;

    public SpawnEggGUI(BlockRandomizerManager blockRandomizerManager, MobArmyMain plugin, TimerManager timerManager) {
        this.blockRandomizerManager = blockRandomizerManager;
        this.plugin = plugin;
        this.timerManager = timerManager;
    }

    private final List<Material> bossSpawnEggs = List.of(
            Material.ENDER_DRAGON_SPAWN_EGG,
            Material.WITHER_SPAWN_EGG,
            Material.WARDEN_SPAWN_EGG,
            Material.ELDER_GUARDIAN_SPAWN_EGG
    );

    private final List<Material> allSpawnEggs = Arrays.asList(
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
            Material.BLAZE_SPAWN_EGG,
            Material.EVOKER_SPAWN_EGG,
            Material.SHULKER_SPAWN_EGG,
            Material.WITCH_SPAWN_EGG
    );

    private List<Material> getNormalSpawnEggs() {
        return allSpawnEggs.stream()
                .filter(egg -> !bossSpawnEggs.contains(egg))
                .toList();
    }

    private ItemStack createEggItem(Material spawnEgg) {
        ItemStack item = new ItemStack(spawnEgg);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW +
                    spawnEgg.name().replace("_SPAWN_EGG", ""));

            meta.setLore(List.of(
                    blockRandomizerManager.isSpawnEggBlocked(spawnEgg)
                            ? ChatColor.RED + "Deaktiviert ❌"
                            : ChatColor.GREEN + "Aktiviert ✅"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(
                null,
                54,
                ChatColor.DARK_AQUA + "Spawneier Einstellungen"
        );

        int[] bossSlots = {11, 12, 14, 15};
        int i = 0;

        for (Material egg : bossSpawnEggs) {
            if (i >= bossSlots.length) break;
            gui.setItem(bossSlots[i++], createEggItem(egg));
        }

        List<Material> normalEggs = getNormalSpawnEggs();

        int placed = 0;
        int[] rowStarts = {18, 27, 36};

        for (int rowStart : rowStarts) {
            if (placed >= normalEggs.size()) break;

            int remaining = normalEggs.size() - placed;
            int rowCount = Math.min(7, remaining);

            int startOffset = 1 + (7 - rowCount) / 2;

            for (int j = 0; j < rowCount; j++) {
                gui.setItem(
                        rowStart + startOffset + j,
                        createEggItem(normalEggs.get(placed++))
                );
            }
        }

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta meta = back.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Zurück");
        back.setItemMeta(meta);
        gui.setItem(49, back);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ChatColor.stripColor(event.getView().getTitle())
                .equals("Spawneier Einstellungen")) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        if (clickedItem.getType() == Material.BARRIER) {
            Sounds.playClick(player);
            plugin.getEventSettingsGUI().open(player);
            return;
        }

        Material material = clickedItem.getType();
        if (!material.name().endsWith("_SPAWN_EGG")) return;

        blockRandomizerManager.toggleSpawnEgg(material);
        Sounds.playClick(player);

        openGUI(player);
    }
}