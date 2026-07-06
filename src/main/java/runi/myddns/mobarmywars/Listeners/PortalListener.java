package runi.myddns.mobarmywars.Listeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import runi.myddns.mobarmywars.Managers.World.PortalManager;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortalListener implements Listener {

    private final MobArmyMain plugin;
    private final Map<UUID, Long> lastPortalUse = new HashMap<>();
    private static final long VANILLA_COOLDOWN = 3000;

    public PortalListener(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent e) {

        if (e.getCause() != PlayerPortalEvent.TeleportCause.NETHER_PORTAL) return;

        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();

        Long last = lastPortalUse.get(id);
        if (last != null && now - last < VANILLA_COOLDOWN) return;

        World fromWorld = e.getFrom().getWorld();
        if (fromWorld == null) return;

        String team = plugin.getTeamManager().getPlayerTeam(p);
        if (team == null) return;

        PortalManager pm = plugin.getPortalManager();

        String overworldName, netherName;
        if (team.equalsIgnoreCase("rot")) {
            overworldName = "world_rot";
            netherName = "world_rot_nether";
        } else if (team.equalsIgnoreCase("blau")) {
            overworldName = "world_blau";
            netherName = "world_blau_nether";
        } else return;

        Location target = null;

        // 🌍 OVERWORLD → NETHER
        if (fromWorld.getName().equalsIgnoreCase(overworldName)) {

            World nether = Bukkit.getWorld(netherName);
            if (nether == null) return;

            Location portalCenter = pm.findPortalCenter(e.getFrom());
            if (portalCenter == null) portalCenter = e.getFrom().clone();

            pm.registerPortal(portalCenter);

            Location search = portalCenter.clone();
            search.setWorld(nether);
            search.setX(portalCenter.getX() / 8.0);
            search.setZ(portalCenter.getZ() / 8.0);

            target = pm.getNearestPortal(search);
            if (target == null) {
                target = pm.createNetherPortal(nether, portalCenter);
            }
        }

        // 🔥 NETHER → OVERWORLD
        else if (fromWorld.getName().equalsIgnoreCase(netherName)) {

            World overworld = Bukkit.getWorld(overworldName);
            if (overworld == null) return;

            Location portalCenter = pm.findPortalCenter(e.getFrom());
            if (portalCenter == null) portalCenter = e.getFrom().clone();

            pm.registerPortal(portalCenter);

            Location search = portalCenter.clone();
            search.setWorld(overworld);
            search.setX(portalCenter.getX() * 8.0);
            search.setZ(portalCenter.getZ() * 8.0);

            target = pm.getNearestPortal(search);
            if (target == null) {
                target = pm.createOverworldPortal(overworld, portalCenter);
            }
        }

        if (target == null) return;

        e.setCancelled(true);
        e.setCanCreatePortal(false);

        lastPortalUse.put(id, now);
        Location finalTarget = target.clone();

        Bukkit.getScheduler().runTask(plugin, () -> {
            p.teleport(finalTarget, PlayerTeleportEvent.TeleportCause.PLUGIN);
            p.setPortalCooldown(200);
        });
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;

        World to = e.getTo() != null ? e.getTo().getWorld() : null;
        if (to == null) return;

        if (to.getName().equalsIgnoreCase("world_nether")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent e) {

        Block block = e.getBlock();
        World w = block.getWorld();
        if (!w.getName().startsWith("world_")) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Location center = plugin.getPortalManager()
                    .findPortalCenter(block.getLocation());

            if (center != null) {
                plugin.getPortalManager().registerPortal(center);
            }
        }, 2L);
    }

    @EventHandler
    public void onPortalBreak(BlockBreakEvent e) {

        Block b = e.getBlock();
        if (b.getType() != Material.NETHER_PORTAL
                && b.getType() != Material.OBSIDIAN) return;

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {
                    PortalManager pm = plugin.getPortalManager();

                    Location center = pm.findPortalCenter(b.getLocation());
                    if (center != null && center.getBlock().getType() == Material.NETHER_PORTAL) {
                        return;
                    }

                    pm.removeNearestPortal(b.getLocation(), 5.0);
                },
                2L
        );
    }

    @EventHandler
    public void onPortalPhysics(BlockPhysicsEvent e) {
        Block b = e.getBlock();

        if (b.getType() != Material.NETHER_PORTAL) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PortalManager pm = plugin.getPortalManager();

            Location center = pm.findPortalCenter(b.getLocation());

            if (center == null) {
                pm.removeNearestPortal(b.getLocation(), 6.0);
            }
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastPortalUse.remove(e.getPlayer().getUniqueId());
    }
}