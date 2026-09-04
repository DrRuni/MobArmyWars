package runi.myddns.mobarmywars.Listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.HashMap;
import java.util.Map;

public class PauseListener implements Listener {

    private final MobArmyMain plugin;

    private final Map<Player, Location> freezeLocations = new HashMap<>();

    public PauseListener(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    private boolean isPaused() {
        return plugin.getEventResume().isEventStarted()
                && plugin.getEventResume().isEventPaused();
    }

    private boolean isTeamWorld(Player player) {
        String worldName = player.getWorld().getName().toLowerCase();
        return worldName.contains("rot")
                || worldName.contains("blau");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {

        if (isPaused()
                && event.getEntity() instanceof Player player) {

            player.setInvulnerable(true);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {

        if (isPaused()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {

        if (isPaused()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {

        if (!isPaused()) {
            return;
        }

        if (event.getClickedBlock() != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        if (!isPaused()) {
            return;
        }

        Player player = event.getPlayer();

        freezeLocations.put(
                player,
                player.getLocation()
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        freezeLocations.remove(player);

        if (!plugin.getEventResume().isEventStarted()) {
            return;
        }

        if (plugin.getTimerManager() != null) {
            plugin.getTimerManager().pauseTimer();
        }

        Component message =
                plugin.getLanguageManager().getComponent(
                        "pause-listener.player-left",
                        "player",
                        player.getName()
                );

        plugin.getServer()
                .sendMessage(message);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {

        if (!isPaused()) {
            return;
        }

        Player player = event.getPlayer();

        if (!isTeamWorld(player)) {
            return;
        }

        freezeLocations.putIfAbsent(
                player,
                event.getFrom()
        );

        Location base = freezeLocations.get(player);
        Location to = event.getTo();

        if (base == null) {
            return;
        }

        double dx = to.getX() - base.getX();
        double dz = to.getZ() - base.getZ();

        double max = 2.5;

        if (Math.abs(dx) > max
                || Math.abs(dz) > max) {

            Location corrected = to.clone();

            corrected.setX(
                    base.getX()
                            + Math.clamp(dx, -max, max)
            );

            corrected.setZ(
                    base.getZ()
                            + Math.clamp(dz, -max, max)
            );

            event.setTo(corrected);
        }
    }
}