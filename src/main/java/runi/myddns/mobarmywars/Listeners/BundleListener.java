package runi.myddns.mobarmywars.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import runi.myddns.mobarmywars.GUIs.BundleGUI;
import runi.myddns.mobarmywars.Managers.Event.BundleManager;
import runi.myddns.mobarmywars.Managers.Event.TeamManager;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BundleListener implements Listener {

    private final BundleGUI bundleGUI;
    private final TeamManager teamManager;
    private final BundleManager bundleManager;

    private final Set<UUID> recentlyClosedBundlePlayers = new HashSet<>();
    private final Set<UUID> playersUsingBundle = new HashSet<>();
    private final Set<UUID> playersModifiedBundle = new HashSet<>();
    private final Set<UUID> giveBundleAfterRespawn = new HashSet<>();

    public BundleListener(BundleGUI bundleGUI,
                          TeamManager teamManager,
                          BundleManager bundleManager) {
        this.bundleGUI = bundleGUI;
        this.teamManager = teamManager;
        this.bundleManager = bundleManager;
    }

    private boolean isTeamBundle(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        String name = ChatColor.stripColor(meta.getDisplayName());
        return name.equalsIgnoreCase("Blaues Bundle")
                || name.equalsIgnoreCase("Rotes Bundle");
    }

    private boolean isBundleInventory(Inventory inventory) {
        return inventory.getHolder() == null && inventory.getSize() == 27;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory clickedInv = event.getClickedInventory();
        ItemStack currentItem = event.getCurrentItem();
        String team = teamManager.getPlayerTeam(player);

        if (isTeamBundle(currentItem)) {
            event.setCancelled(true);
            if (team != null) {
                bundleGUI.openTeamInventory(player, team);
            }
            return;
        }

        if (clickedInv != null && clickedInv.equals(player.getOpenInventory().getTopInventory())) {
            switch (event.getAction()) {
                case PICKUP_ALL, PICKUP_HALF, PLACE_ALL, PLACE_SOME, PLACE_ONE,
                     DROP_ONE_SLOT, DROP_ALL_SLOT, MOVE_TO_OTHER_INVENTORY -> {
                    if (team != null && isBundleInventory(clickedInv)) {
                        Bukkit.getScheduler().runTaskLater(
                                MobArmyMain.getInstance(),
                                () -> bundleGUI.saveInventory(team),
                                1L
                        );
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String team = teamManager.getPlayerTeam(player);
        Inventory top = event.getView().getTopInventory();

        if (team != null && isBundleInventory(top)) {
            Bukkit.getScheduler().runTaskLater(
                    MobArmyMain.getInstance(),
                    () -> bundleGUI.saveInventory(team),
                    1L
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();

        if (bundleGUI.isTeamInventory(event.getInventory())) {
            recentlyClosedBundlePlayers.add(uuid);

            Bukkit.getScheduler().runTaskLater(
                    MobArmyMain.getInstance(),
                    () -> recentlyClosedBundlePlayers.remove(uuid),
                    60L
            );
        }

        Bukkit.getScheduler().runTaskLater(
                MobArmyMain.getInstance(),
                () -> {
                    if (player.getItemOnCursor() != null &&
                            !player.getItemOnCursor().getType().isAir()) {
                        player.setItemOnCursor(null);
                    }
                    player.updateInventory();
                },
                2L
        );
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        if (isTeamBundle(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "🚫 Du kannst dein Team-Bundle nicht droppen!");
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            Bukkit.getScheduler().runTaskLater(
                    MobArmyMain.getInstance(),
                    player::updateInventory,
                    2L
            );
        }
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) return;

        if (recentlyClosedBundlePlayers.contains(player.getUniqueId())
                || item.getType().name().endsWith("_SPAWN_EGG")) {

            Bukkit.getScheduler().runTaskLater(
                    MobArmyMain.getInstance(),
                    player::updateInventory,
                    2L
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        event.getDrops().removeIf(this::isTeamBundle);

        giveBundleAfterRespawn.add(player.getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (giveBundleAfterRespawn.remove(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(
                    MobArmyMain.getInstance(),
                    () -> bundleManager.giveTeamBundle(player),
                    1L
            );
        }
    }
}