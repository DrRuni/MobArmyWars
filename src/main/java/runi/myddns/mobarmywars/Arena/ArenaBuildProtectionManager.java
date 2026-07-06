package runi.myddns.mobarmywars.Arena;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
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
import org.bukkit.plugin.java.JavaPlugin;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.util.*;

public class ArenaBuildProtectionManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<String, Set<BlockPos>> placedBlocks = new HashMap<>();
    private final Map<String, ArenaData> arenas = new HashMap<>();

    private record BlockPos(String world, int x, int y, int z) { }

    private BlockPos toPos(Location loc) {
        return new BlockPos(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public record ArenaData(
            String name,
            String description,
            String world,
            Location rotCorner1,
            Location rotCorner2,
            Location blauCorner1,
            Location blauCorner2,
            Location rotSpawn,
            Location blauSpawn,
            List<Location> rotMobSpawns,
            List<Location> blauMobSpawns
    ) {}

    public ArenaBuildProtectionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void loadFromConfig() {
        File file = new File(plugin.getDataFolder(), "arena-koordinaten.yml");

        if (!file.exists()) {
            plugin.saveResource("arena-koordinaten.yml", false);
            Bukkit.getLogger().info(ChatColor.GREEN + "[MobArmyWars] 📄 'arena-koordinaten.yml' neu erstellt (Standardwerte).");
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection arenasSec = cfg.getConfigurationSection("arenas");

        if (arenasSec == null) {
            Bukkit.getLogger().severe(ChatColor.RED + "[MobArmyWars] ❌ Keine Arenen in 'arena-koordinaten.yml' gefunden!");
            return;
        }

        arenas.clear();
        for (String key : arenasSec.getKeys(false)) {
            ConfigurationSection sec = arenasSec.getConfigurationSection(key);
            if (sec == null) continue;

            String name = sec.getString("name", key);
            String description = sec.getString("description", "");
            String worldName = sec.getString("world", "world_mobarmylobby");
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                Bukkit.getLogger().warning("[MobArmyWars] ⚠ Welt '" + worldName + "' nicht gefunden – Arena '" + name + "' übersprungen!");
                continue;
            }

            try {
                Location rotC1 = toLoc(world, sec.getIntegerList("rot.corner1"));
                Location rotC2 = toLoc(world, sec.getIntegerList("rot.corner2"));
                Location blauC1 = toLoc(world, sec.getIntegerList("blau.corner1"));
                Location blauC2 = toLoc(world, sec.getIntegerList("blau.corner2"));

                Location rotSpawn = toLocWithYaw(world, sec.getIntegerList("rot.teamspawn"));
                Location blauSpawn = toLocWithYaw(world, sec.getIntegerList("blau.teamspawn"));

                List<Location> rotMobs = toLocList(world, sec.getList("rot.mobSpawns"));
                List<Location> blauMobs = toLocList(world, sec.getList("blau.mobSpawns"));

                ArenaData arena = new ArenaData(name, description, worldName,
                        rotC1, rotC2, blauC1, blauC2, rotSpawn, blauSpawn, rotMobs, blauMobs);

                arenas.put(key.toLowerCase(), arena);
            } catch (Exception ex) {
                Bukkit.getLogger().severe(ChatColor.RED + "[MobArmyWars] ❌ Fehler beim Laden der Arena '" + key + "': " + ex.getMessage());
            }
        }
    }

    private String fmt(Location l) {
        if (l == null) return "null";
        return "(" + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ() + ")";
    }

    private Location toLoc(World w, List<Integer> list) {
        if (list == null || list.size() < 3) return null;
        return new Location(w, list.get(0), list.get(1), list.get(2));
    }

    private Location toLocWithYaw(World w, List<Integer> list) {
        if (list == null || list.size() < 3) return null;
        float yaw = list.size() > 3 ? list.get(3) : 0;
        float pitch = list.size() > 4 ? list.get(4) : 0;
        return new Location(w, list.get(0), list.get(1), list.get(2), yaw, pitch);
    }

    private List<Location> toLocList(World w, List<?> rawList) {
        List<Location> locs = new ArrayList<>();
        if (rawList == null) return locs;

        for (Object o : rawList) {
            if (o instanceof List<?>) {
                List<?> coords = (List<?>) o;
                if (coords.size() >= 3) {
                    locs.add(new Location(w,
                            ((Number) coords.get(0)).doubleValue(),
                            ((Number) coords.get(1)).doubleValue(),
                            ((Number) coords.get(2)).doubleValue()));
                }
            }
        }
        return locs;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent e) {
        if (plugin instanceof MobArmyMain ma) {
            if (ma.getEventResume().isEventStarted()
                    && ma.getEventResume().isEventPaused()) {
                e.setCancelled(true);
                return;
            }
        }
        Player p = e.getPlayer();
        Location loc = e.getBlock().getLocation();
        Material type = e.getBlock().getType();

        String worldName = loc.getWorld().getName().toLowerCase();
        if (!worldName.contains("mobarmylobby")) {
            return;
        }

        ArenaData arena = getArenaByLocation(loc);
        String team = getTeam(p);

        if (arena == null) {
            e.setCancelled(true);
            return;
        }

        // 🌿 Erlaubte Naturblöcke (dürfen nur IN der Arena-Zone abgebaut werden)
        Set<Material> allowedNaturalBlocks = Set.of(
                // =========================
                // Gras & Unterholz
                // =========================
                Material.SHORT_GRASS,
                Material.SHORT_DRY_GRASS,
                Material.TALL_GRASS,
                Material.FERN,
                Material.LARGE_FERN,
                Material.DEAD_BUSH,

                // =========================
                // Azalea / Büsche
                // =========================
                Material.AZALEA,
                Material.FLOWERING_AZALEA,

                // =========================
                // Kleine Blumen
                // =========================
                Material.DANDELION,
                Material.POPPY,
                Material.BLUE_ORCHID,
                Material.ALLIUM,
                Material.AZURE_BLUET,
                Material.RED_TULIP,
                Material.ORANGE_TULIP,
                Material.WHITE_TULIP,
                Material.PINK_TULIP,
                Material.OXEYE_DAISY,
                Material.CORNFLOWER,
                Material.LILY_OF_THE_VALLEY,

                // =========================
                // Große Blumen (2-Block)
                // =========================
                Material.ROSE_BUSH,
                Material.PEONY,
                Material.LILAC,
                Material.SUNFLOWER,

                // =========================
                // Pilze
                // =========================
                Material.BROWN_MUSHROOM,
                Material.RED_MUSHROOM,

                // =========================
                // Feldfrüchte / Crops
                // =========================
                Material.WHEAT,
                Material.CARROTS,
                Material.POTATOES,
                Material.BEETROOTS,

                // =========================
                // Ranken & Beeren
                // =========================
                Material.SWEET_BERRY_BUSH,
                Material.GLOW_BERRIES,
                Material.CAVE_VINES,
                Material.CAVE_VINES_PLANT,
                Material.TWISTING_VINES,
                Material.TWISTING_VINES_PLANT,
                Material.WEEPING_VINES,
                Material.WEEPING_VINES_PLANT,

                // =========================
                // Zucker & Bambus
                // =========================
                Material.SUGAR_CANE,
                Material.BAMBOO,
                Material.BAMBOO_SAPLING,

                // =========================
                // Nether-Pflanzen (optional, aber „Versteck“-tauglich)
                // =========================
                Material.NETHER_WART,
                Material.CRIMSON_ROOTS,
                Material.WARPED_ROOTS,
                Material.NETHER_SPROUTS
        );

        if (allowedNaturalBlocks.contains(type) && isInsideAnyTeamArea(loc, arena)) {
            return;
        }

        if (allowedNaturalBlocks.contains(type) && !isInsideAnyTeamArea(loc, arena)) {
            e.setCancelled(true);
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

        if (placed == null || !placed.remove(pos)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (plugin instanceof MobArmyMain ma) {
            if (ma.getEventResume().isEventStarted()
                    && ma.getEventResume().isEventPaused()) {
                e.setCancelled(true);
                return;
            }
        }
        Player p = e.getPlayer();
        Location loc = e.getBlock().getLocation();

        String worldName = loc.getWorld().getName().toLowerCase();
        if (getArenaByLocation(loc) == null) {
            return;
        }

        ArenaData arena = getArenaByLocation(loc);
        String team = getTeam(p);

        if (arena == null) {
            e.setCancelled(true);
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

        placedBlocks.computeIfAbsent(team.toLowerCase(), k -> new HashSet<>()).add(toPos(loc));
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent e) {
        World w = e.getLocation().getWorld();
        if (w == null) return;

        if (w.getName().toLowerCase().contains("mobarmy")) {
            e.blockList().clear();
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onFireSpread(BlockSpreadEvent e) {
        if (e.getSource().getType() == Material.FIRE) {
            e.setCancelled(true);
        }
    }

    private boolean isInsideTeamArea(Location loc, String team, ArenaData arena) {
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

    public ArenaData getArenaByLocation(Location loc) {
        for (ArenaData arena : arenas.values()) {
            if (!arena.world().equalsIgnoreCase(loc.getWorld().getName())) continue;

            if (isInside(loc, arena.rotCorner1(), arena.rotCorner2()) ||
                    isInside(loc, arena.blauCorner1(), arena.blauCorner2())) {
                return arena;
            }
        }
        return null;
    }

    private boolean isInsideAnyTeamArea(Location loc, ArenaData arena) {
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

    private String getTeam(Player p) {
        if (plugin instanceof MobArmyMain ma) {
            var tm = ma.getTeamManager();
            if (tm != null) {
                String team = tm.getPlayerTeam(p);
                if (team != null && !team.equalsIgnoreCase("Kein Team"))
                    return team.toLowerCase();
            }
        }
        return null;
    }

    public ArenaData getArena(String key) {
        return arenas.get(key.toLowerCase());
    }

    public void clearTeamData(String team) {
        placedBlocks.remove(team.toLowerCase());
    }
}