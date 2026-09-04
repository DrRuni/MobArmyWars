package runi.myddns.mobarmywars.Listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import runi.myddns.mobarmywars.GUIs.BundleGUI;
import runi.myddns.mobarmywars.Managers.Event.BundleManager;
import runi.myddns.mobarmywars.Managers.Event.TeamManager;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BundleListener implements Listener {

    private final MobArmyMain plugin;
    private final BundleGUI bundleGUI;
    private final TeamManager teamManager;
    private final BundleManager bundleManager;
    private final NamespacedKey teamBundleKey;

    private final Set<UUID> recentlyClosedBundlePlayers = new HashSet<>();
    private final Set<UUID> giveBundleAfterRespawn = new HashSet<>();

    public BundleListener(
            MobArmyMain plugin,
            BundleGUI bundleGUI,
            TeamManager teamManager,
            BundleManager bundleManager
    ) {
        this.plugin = plugin;
        this.bundleGUI = bundleGUI;
        this.teamManager = teamManager;
        this.bundleManager = bundleManager;
        this.teamBundleKey = new NamespacedKey(plugin, "team_bundle");
    }

    private boolean isTeamBundle(ItemStack item) {

        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return false;
        }

        String bundleTeam =
                meta.getPersistentDataContainer().get(
                        teamBundleKey,
                        PersistentDataType.STRING
                );

        if (bundleTeam != null) {
            return bundleTeam.equalsIgnoreCase("blue")
                    || bundleTeam.equalsIgnoreCase("red");
        }

        Component displayName = meta.displayName();

        if (displayName == null) {
            return false;
        }

        String legacyName =
                PlainTextComponentSerializer.plainText()
                        .serialize(displayName);

        return legacyName.equalsIgnoreCase("Blaues Bundle")
                || legacyName.equalsIgnoreCase("Rotes Bundle");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clickedInventory =
                event.getClickedInventory();

        ItemStack currentItem =
                event.getCurrentItem();

        String team =
                teamManager.getPlayerTeam(player);

        if (isTeamBundle(currentItem)) {

            event.setCancelled(true);

            if (team != null
                    && !team.equalsIgnoreCase("Kein Team")) {

                bundleGUI.openTeamInventory(
                        player,
                        team
                );
            }

            return;
        }

        if (clickedInventory == null
                || !bundleGUI.isTeamInventory(clickedInventory)) {

            return;
        }

        InventoryAction action =
                event.getAction();

        switch (action) {

            case PICKUP_ALL,
                 PICKUP_HALF,
                 PLACE_ALL,
                 PLACE_SOME,
                 PLACE_ONE,
                 DROP_ONE_SLOT,
                 DROP_ALL_SLOT,
                 MOVE_TO_OTHER_INVENTORY -> {

                if (team != null
                        && !team.equalsIgnoreCase("Kein Team")) {

                    Bukkit.getScheduler().runTaskLater(
                            plugin,
                            () -> bundleGUI.saveInventory(team),
                            1L
                    );
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(
            InventoryDragEvent event
    ) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top =
                event.getView().getTopInventory();

        if (!bundleGUI.isTeamInventory(top)) {
            return;
        }

        String team =
                teamManager.getPlayerTeam(player);

        if (team == null
                || team.equalsIgnoreCase("Kein Team")) {

            return;
        }

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> bundleGUI.saveInventory(team),
                1L
        );
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(
            InventoryCloseEvent event
    ) {

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        UUID uuid =
                player.getUniqueId();

        if (bundleGUI.isTeamInventory(
                event.getInventory()
        )) {

            recentlyClosedBundlePlayers.add(uuid);

            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> recentlyClosedBundlePlayers.remove(uuid),
                    60L
            );
        }

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {

                    ItemStack cursor =
                            player.getItemOnCursor();

                    if (!cursor.getType().isAir()) {
                        player.setItemOnCursor(null);
                    }

                    player.updateInventory();
                },
                2L
        );
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(
            PlayerDropItemEvent event
    ) {

        if (!isTeamBundle(
                event.getItemDrop().getItemStack()
        )) {
            return;
        }

        event.setCancelled(true);

        event.getPlayer().sendMessage(
                dropDeniedMessage()
        );
    }

    @EventHandler
    public void onItemPickup(
            EntityPickupItemEvent event
    ) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(
                plugin,
                player::updateInventory,
                2L
        );
    }

    @EventHandler
    public void onPlayerUse(
            PlayerInteractEvent event
    ) {

        Player player =
                event.getPlayer();

        ItemStack item =
                event.getItem();

        if (item == null
                || item.getType() == Material.AIR) {

            return;
        }

        if (recentlyClosedBundlePlayers.contains(
                player.getUniqueId()
        )
                || item.getType()
                .name()
                .endsWith("_SPAWN_EGG")) {

            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    player::updateInventory,
                    2L
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(
            PlayerDeathEvent event
    ) {

        Player player =
                event.getEntity();

        event.getDrops()
                .removeIf(this::isTeamBundle);

        giveBundleAfterRespawn.add(
                player.getUniqueId()
        );
    }

    @EventHandler
    public void onRespawn(
            PlayerRespawnEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!giveBundleAfterRespawn.remove(
                player.getUniqueId()
        )) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> bundleManager.giveTeamBundle(player),
                1L
        );
    }

    private Component dropDeniedMessage() {

        return plugin.getLanguageManager()
                .getComponent(
                        "bundle-listener.drop-denied"
                );
    }
}