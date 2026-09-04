package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.*;
import org.bukkit.Tag;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.util.*;

public class ArenaBuildProtectionManager implements Listener {

    private final MobArmyMain plugin;
    private final Map<String, Set<BlockPos>> placedBlocks = new HashMap<>();
    private final List<ProtectedArea> opProtectedLobbyAreas = new ArrayList<>();

    private record ProtectedArea(String world, Location corner1, Location corner2) { }
    private record BlockPos(String world, int x, int y, int z) { }

    private BlockPos toPos(Location loc) {
        return new BlockPos(
                loc.getWorld().getName().toLowerCase(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );
    }

    private boolean isLobbyProtectedFor(Player player) {
        return !player.isOp();
    }

    public ArenaBuildProtectionManager(MobArmyMain plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void loadSpawnProtectionAreas() {
        File file = new File(plugin.getDataFolder(), "spawns.yml");

        if (!file.exists()) {
            plugin.getLogger().warning("[MobArmyWars] ⚠ spawns.yml nicht gefunden – Lobby-Schutzbereiche nicht geladen!");
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        opProtectedLobbyAreas.clear();

        loadWaveArea(cfg, "wave-auswahl.rot");
        loadWaveArea(cfg, "wave-auswahl.blau");
    }

    private void loadWaveArea(FileConfiguration cfg, String path) {
        String worldName = cfg.getString("wave-auswahl.world", "world_mobarmy_lobby");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().warning("[MobArmyWars] ⚠ Welt '" + worldName + "' für " + path + " nicht gefunden!");
            return;
        }

        Location c1 = toLoc(world, cfg.getIntegerList(path + ".corner1"));
        Location c2 = toLoc(world, cfg.getIntegerList(path + ".corner2"));

        if (c1 == null || c2 == null) {
            plugin.getLogger().warning("[MobArmyWars] ⚠ Ungültige Corner für " + path);
            return;
        }
        opProtectedLobbyAreas.add(new ProtectedArea(worldName, c1, c2));
    }

    private boolean isInsideOpProtectedLobbyArea(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;

        for (ProtectedArea area : opProtectedLobbyAreas) {
            if (!area.world().equalsIgnoreCase(loc.getWorld().getName())) continue;

            if (isInside(loc, area.corner1(), area.corner2())) {
                return true;
            }
        }
        return false;
    }

    private Location toLoc(World w, List<Integer> list) {
        if (list == null || list.size() < 3) return null;
        return new Location(w, list.get(0), list.get(1), list.get(2));
    }

    private boolean isAllowedNaturalBlock(Material type) {
        if (type == null) return false;

        // Vanilla-Tags
        if (Tag.FLOWERS.isTagged(type)) return true;
        if (Tag.SAPLINGS.isTagged(type)) return true;
        if (Tag.CROPS.isTagged(type)) return true;

        return switch (type) {
            // Gras / Bodenpflanzen
            case SHORT_GRASS,
                 SHORT_DRY_GRASS,
                 TALL_GRASS,
                 TALL_DRY_GRASS,
                 FERN,
                 LARGE_FERN,
                 DEAD_BUSH,

                 // Azalea / kleine Büsche
                 AZALEA,
                 FLOWERING_AZALEA,

                 // Pilze
                 BROWN_MUSHROOM,
                 RED_MUSHROOM,

                 // Beeren / Ranken / Kletterpflanzen
                 SWEET_BERRY_BUSH,
                 VINE,
                 CAVE_VINES,
                 CAVE_VINES_PLANT,
                 TWISTING_VINES,
                 TWISTING_VINES_PLANT,
                 WEEPING_VINES,
                 WEEPING_VINES_PLANT,

                 // Wasserpflanzen
                 LILY_PAD,
                 SEAGRASS,
                 TALL_SEAGRASS,
                 KELP,
                 KELP_PLANT,

                 // Dripleaf
                 SMALL_DRIPLEAF,
                 BIG_DRIPLEAF,
                 BIG_DRIPLEAF_STEM,

                 // Zucker/Bambus
                 SUGAR_CANE,
                 BAMBOO,
                 BAMBOO_SAPLING,

                 // Feld / Garten
                 COCOA,
                 MELON_STEM,
                 ATTACHED_MELON_STEM,
                 PUMPKIN_STEM,
                 ATTACHED_PUMPKIN_STEM,
                 TORCHFLOWER_CROP,
                 PITCHER_CROP,

                 // Nether / spezielles
                 TRIPWIRE,
                 NETHER_WART,
                 CRIMSON_ROOTS,
                 WARPED_ROOTS,
                 NETHER_SPROUTS,
                 CRIMSON_FUNGUS,
                 WARPED_FUNGUS,
                 HANGING_ROOTS,
                 MANGROVE_PROPAGULE,
                 CHORUS_PLANT,
                 CHORUS_FLOWER -> true;

            default -> false;
        };
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        if (plugin.getEventResume().isEventStarted()
                && plugin.getEventResume().isEventPaused()) {
            e.setCancelled(true);
            return;
        }

        Player p = e.getPlayer();
        Location loc = e.getBlock().getLocation();
        Material type = e.getBlock().getType();

        if (loc.getWorld() == null) {
            e.setCancelled(true);
            return;
        }

        String worldName = loc.getWorld().getName().toLowerCase();

        // Lobby: komplett schützen, außer OP
        if (worldName.equals("world_mobarmy_lobby")) {
            if (isInsideOpProtectedLobbyArea(loc)) {
                e.setCancelled(true);
                return;
            }

            if (isLobbyProtectedFor(p)) {
                e.setCancelled(true);
            }
            return;
        }

        ArenaConfig.ArenaData arena =
                plugin.getArenaConfig().getActiveArena();

        if (arena == null) {
            e.setCancelled(true);
            return;
        }

        // Nur aktive Arena-Welt behandeln
        if (!worldName.equalsIgnoreCase(arena.world())) {
            return;
        }

        String team = getTeam(p);

        // Naturblöcke in Arena-Zonen dürfen entfernt werden, aber ohne Drops/XP
        if (isAllowedNaturalBlock(type)) {
            if (!isInsideAnyTeamArea(loc, arena)) {
                e.setCancelled(true);
                return;
            }

            e.setDropItems(false);
            e.setExpToDrop(0);
            return;
        }

        if (team == null) {
            e.setCancelled(true);
            return;
        }

        if (!isInsideAnyTeamArea(loc, arena)) {
            e.setCancelled(true);
            return;
        }

        if (!isInsideTeamArea(loc, team, arena)) {
            e.setCancelled(true);
            return;
        }

        BlockPos pos = toPos(loc);
        Set<BlockPos> placed = placedBlocks.get(team.toLowerCase());

        // Nur selbst gesetzte Blöcke dürfen wieder abgebaut werden
        if (placed == null || !placed.remove(pos)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (plugin.getEventResume().isEventStarted()
                && plugin.getEventResume().isEventPaused()) {
            e.setCancelled(true);
            return;
        }

        Player p = e.getPlayer();
        Location loc = e.getBlock().getLocation();

        if (loc.getWorld() == null) {
            e.setCancelled(true);
            return;
        }

        String worldName = loc.getWorld().getName().toLowerCase();

        // Lobby: komplett schützen, außer OP
        if (worldName.equals("world_mobarmy_lobby")) {
            if (isInsideOpProtectedLobbyArea(loc)) {
                e.setCancelled(true);
                return;
            }

            if (isLobbyProtectedFor(p)) {
                e.setCancelled(true);
            }
            return;
        }

        ArenaConfig.ArenaData arena =
                plugin.getArenaConfig().getActiveArena();

        if (arena == null) {
            e.setCancelled(true);
            return;
        }

        // Nur aktive Arena-Welt behandeln
        if (!worldName.equalsIgnoreCase(arena.world())) {
            return;
        }

        String team = getTeam(p);

        if (team == null) {
            e.setCancelled(true);
            return;
        }

        if (!isInsideAnyTeamArea(loc, arena)) {
            e.setCancelled(true);
            return;
        }

        if (!isInsideTeamArea(loc, team, arena)) {
            e.setCancelled(true);
            return;
        }

        placedBlocks.computeIfAbsent(
                team.toLowerCase(Locale.ROOT),
                _ -> new HashSet<>()
        ).add(toPos(loc));
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent e) {
        World w = e.getLocation().getWorld();

        if (isProtectedMobArmyWorld(w)) {
            e.blockList().clear();
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent e) {
        World w = e.getBlock().getWorld();

        if (isProtectedMobArmyWorld(w)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFireSpread(BlockSpreadEvent e) {
        World w = e.getBlock().getWorld();

        if (isProtectedMobArmyWorld(w) && e.getSource().getType() == Material.FIRE) {
            e.setCancelled(true);
        }
    }

    private boolean isProtectedMobArmyWorld(World world) {
        if (world == null) return false;

        String name = world.getName().toLowerCase();

        if (name.equals("world_mobarmy_lobby")) {
            return true;
        }

        ArenaConfig.ArenaData arena =
                plugin.getArenaConfig().getActiveArena();

        return arena != null
                && name.equalsIgnoreCase(arena.world());
    }

    private boolean isInsideTeamArea(Location loc, String team, ArenaConfig.ArenaData arena) {
        if (!loc.getWorld().getName().equalsIgnoreCase(arena.world())) return false;
        Location c1, c2;

        if (team.equalsIgnoreCase("rot")) {
            c1 = arena.rotCorner1();
            c2 = arena.rotCorner2();
        } else if (team.equalsIgnoreCase("blau")) {
            c1 = arena.blauCorner1();
            c2 = arena.blauCorner2();
        } else return false;

        if (c1 == null || c2 == null) return false;

        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= Math.min(c1.getX(), c2.getX()) && x <= Math.max(c1.getX(), c2.getX()) &&
                y >= Math.min(c1.getY(), c2.getY()) && y <= Math.max(c1.getY(), c2.getY()) &&
                z >= Math.min(c1.getZ(), c2.getZ()) && z <= Math.max(c1.getZ(), c2.getZ());
    }

    private boolean isInsideAnyTeamArea(Location loc, ArenaConfig.ArenaData arena) {
        return isInside(loc, arena.rotCorner1(), arena.rotCorner2())
                || isInside(loc, arena.blauCorner1(), arena.blauCorner2());
    }

    private boolean isInside(Location loc, Location c1, Location c2) {
        if (c1 == null || c2 == null) return false;
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= Math.min(c1.getX(), c2.getX()) && x <= Math.max(c1.getX(), c2.getX()) &&
                y >= Math.min(c1.getY(), c2.getY()) && y <= Math.max(c1.getY(), c2.getY()) &&
                z >= Math.min(c1.getZ(), c2.getZ()) && z <= Math.max(c1.getZ(), c2.getZ());
    }

    private String getTeam(Player player) {

        String team =
                plugin.getTeamManager()
                        .getPlayerTeam(player);

        if (team == null
                || team.equalsIgnoreCase("Kein Team")) {
            return null;
        }

        return team.toLowerCase(Locale.ROOT);
    }

    public void clearTeamData(String team) {
        placedBlocks.remove(team.toLowerCase());
    }
}