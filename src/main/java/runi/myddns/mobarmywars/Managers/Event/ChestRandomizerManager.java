package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChestRandomizerManager implements Listener {

    private final MobArmyMain plugin;
    private final BlockRandomizerManager blockRandomizerManager;

    private final Map<UUID, Inventory> openRandomChests = new HashMap<>();
    private final Map<UUID, Inventory> realChestInventories = new HashMap<>();

    public ChestRandomizerManager(MobArmyMain plugin, BlockRandomizerManager blockRandomizerManager) {
        this.plugin = plugin;
        this.blockRandomizerManager = blockRandomizerManager;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (!plugin.getWorldSettings().isChestRandomizerEnabled()) {
            return;
        }

        Inventory realInventory = event.getInventory();
        InventoryHolder holder = realInventory.getHolder();

        if (!(holder instanceof Container container)) {
            return;
        }

        Block block = container.getBlock();
        World world = block.getWorld();

        String worldName = world.getName().toLowerCase();

        if (!worldName.equals("world_rot")
                && !worldName.equals("world_blau")
                && !worldName.equals("world_rot_nether")
                && !worldName.equals("world_blau_nether")) {
            return;
        }

        Material blockType = block.getType();

        if (blockType != Material.CHEST
                && blockType != Material.TRAPPED_CHEST
                && blockType != Material.BARREL) {
            return;
        }

        event.setCancelled(true);

        Inventory virtualInventory = Bukkit.createInventory(
                null,
                realInventory.getSize(),
                ChatColor.BLUE + "Randomisierte Kiste"
        );

        fillVirtualInventory(player, realInventory, virtualInventory);

        openRandomChests.put(player.getUniqueId(), virtualInventory);
        realChestInventories.put(player.getUniqueId(), realInventory);

        Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(virtualInventory));
    }

    private void fillVirtualInventory(Player player, Inventory realInventory, Inventory virtualInventory) {
        ItemStack[] contents = realInventory.getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack originalItem = contents[slot];

            if (originalItem == null || originalItem.getType() == Material.AIR) {
                continue;
            }

            Material randomMaterial = blockRandomizerManager.getRandomizedMaterial(player, originalItem.getType());

            ItemStack randomItem = new ItemStack(randomMaterial, originalItem.getAmount());
            virtualInventory.setItem(slot, randomItem);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory randomChest = openRandomChests.get(player.getUniqueId());

        if (randomChest == null) {
            return;
        }

        if (!event.getView().getTopInventory().equals(randomChest)) {
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Inventory randomChest = openRandomChests.get(player.getUniqueId());
        Inventory realInventory = realChestInventories.get(player.getUniqueId());

        if (randomChest == null || realInventory == null) {
            return;
        }

        ItemStack[] virtualContents = randomChest.getContents();
        ItemStack[] realContents = new ItemStack[virtualContents.length];

        for (int slot = 0; slot < virtualContents.length; slot++) {
            ItemStack randomItem = virtualContents[slot];

            if (randomItem == null || randomItem.getType() == Material.AIR) {
                realContents[slot] = null;
                continue;
            }

            Material originalMaterial =
                    blockRandomizerManager.getOriginalMaterial(player, randomItem.getType());

            ItemStack originalItem = randomItem.clone();
            originalItem.setType(originalMaterial);

            realContents[slot] = originalItem;
        }

        realInventory.setContents(realContents);

        openRandomChests.remove(player.getUniqueId());
        realChestInventories.remove(player.getUniqueId());
    }
}