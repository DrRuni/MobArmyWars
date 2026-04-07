package runi.myddns.mobarmywars.Listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PauseListener implements Listener {

    private final MobArmyMain plugin;

    private final Set<Player> frozenPlayers = new HashSet<>();
    private final Map<Player, Location> freezeLocations = new HashMap<>();

    public PauseListener(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    private boolean isPaused() {
        return plugin.getEventResume().isEventStarted()
                && plugin.getEventResume().isEventPaused();
    }

    private boolean isTeamWorld(Player p) {
        String w = p.getWorld().getName().toLowerCase();
        return w.contains("rot") || w.contains("blau");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        if (isPaused() && e.getEntity() instanceof Player player) {
            player.setInvulnerable(true);
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        if (isPaused()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (isPaused()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        if (!isPaused()) return;

        if (e.getClickedBlock() != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (isPaused() && e.getPlayer() instanceof Player) {
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (isPaused()) {
            Player p = e.getPlayer();
            frozenPlayers.add(p);
            freezeLocations.put(p, p.getLocation());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        frozenPlayers.remove(p);
        freezeLocations.remove(p);

        if (plugin.getEventResume().isEventStarted()) {
            if (plugin.getTimerManager() != null) {
                plugin.getTimerManager().pauseTimer();
            }

            plugin.getServer().broadcastMessage(
                    ChatColor.RED + "Spieler "
                            + ChatColor.YELLOW + p.getName()
                            + ChatColor.RED + " ist aus dem Event ausgetreten. "
                            + ChatColor.GOLD + "Das Spiel wurde angehalten. "
                            + ChatColor.GRAY + "Weiter mit /resume"
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent e) {
        if (!isPaused()) return;

        Player p = e.getPlayer();
        if (!isTeamWorld(p)) return;

        freezeLocations.putIfAbsent(p, e.getFrom());

        Location base = freezeLocations.get(p);
        Location to = e.getTo();
        if (base == null || to == null) return;

        double dx = to.getX() - base.getX();
        double dz = to.getZ() - base.getZ();

        double max = 2.5;

        if (Math.abs(dx) > max || Math.abs(dz) > max) {
            Location corrected = to.clone();
            corrected.setX(base.getX() + Math.max(-max, Math.min(max, dx)));
            corrected.setZ(base.getZ() + Math.max(-max, Math.min(max, dz)));
            e.setTo(corrected);
        }
    }
}