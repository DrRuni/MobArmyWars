package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArenaConfig {

    private static final String ARENA_FILE_NAME = "arena-koordinaten.yml";
    private static final String EVENT_FILE_NAME = "eventdaten.yml";

    /**
     * Pfad in eventdaten.yml.
     *
     * Beispiel:
     *
     * arena:
     *   active: "japanisches-dorf"
     */
    private static final String ACTIVE_ARENA_PATH = "arena.active";

    private final MobArmyMain plugin;

    private File arenaFile;
    private File eventFile;

    private FileConfiguration arenaConfig;
    private FileConfiguration eventConfig;

    private String activeArenaId;
    private ArenaData activeArena;

    public ArenaConfig(MobArmyMain plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Kompletter Reload:
     * - arena-koordinaten.yml laden
     * - eventdaten.yml laden
     * - aktive Arena-ID lesen
     * - nur diese aktive Arena komplett laden
     */
    public void reload() {
        loadFiles();
        loadActiveArenaId();
        loadActiveArena();
    }

    private void loadFiles() {
        arenaFile = new File(plugin.getDataFolder(), ARENA_FILE_NAME);
        eventFile = new File(plugin.getDataFolder(), EVENT_FILE_NAME);

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!arenaFile.exists()) {
            plugin.saveResource(ARENA_FILE_NAME, false);
            plugin.getLogger().info("[ArenaConfig] arena-koordinaten.yml wurde erstellt.");
        }

        try {
            if (!eventFile.exists()) {
                eventFile.createNewFile();
                plugin.getLogger().info("[ArenaConfig] eventdaten.yml wurde erstellt.");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("[ArenaConfig] Konnte eventdaten.yml nicht erstellen: " + e.getMessage());
        }

        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
        eventConfig = YamlConfiguration.loadConfiguration(eventFile);
    }

    /**
     * Liest die aktive Arena-ID aus eventdaten.yml.
     * Wenn keine gesetzt ist oder sie ungültig ist, wird die erste Arena aus arena-koordinaten.yml genommen.
     */
    private void loadActiveArenaId() {
        String savedArenaId = eventConfig.getString(ACTIVE_ARENA_PATH);

        if (savedArenaId != null && arenaExists(savedArenaId)) {
            activeArenaId = savedArenaId.toLowerCase();
            return;
        }

        String fallback = getFirstArenaId();

        if (fallback == null) {
            activeArenaId = null;
            plugin.getLogger().severe("[ArenaConfig] Keine Arena in arena-koordinaten.yml gefunden!");
            return;
        }

        activeArenaId = fallback.toLowerCase();
        saveActiveArenaId();

        plugin.getLogger().warning("[ArenaConfig] Keine gültige aktive Arena gefunden. Fallback gesetzt: " + activeArenaId);
    }

    /**
     * Lädt nur die aktuell aktive Arena vollständig in den RAM.
     */
    private void loadActiveArena() {
        if (activeArenaId == null) {
            activeArena = null;
            return;
        }

        activeArena = loadArenaData(activeArenaId);

        if (activeArena == null) {
            plugin.getLogger().severe(
                    "[ArenaConfig] Aktive Arena konnte nicht geladen werden: " + activeArenaId
            );
        }
    }

    /**
     * Setzt eine neue aktive Arena.
     * Wird später vom GUI-Button benutzt.
     */
    public boolean setActiveArena(String arenaId) {
        if (arenaId == null || arenaId.isBlank()) {
            plugin.getLogger().warning("[ArenaConfig] Arena-ID ist leer.");
            return false;
        }

        String id = arenaId.toLowerCase();

        if (!arenaExists(id)) {
            plugin.getLogger().warning("[ArenaConfig] Arena existiert nicht: " + id);
            return false;
        }

        ArenaData newArena = loadArenaData(id);

        if (newArena == null) {
            plugin.getLogger().warning("[ArenaConfig] Arena konnte nicht geladen werden: " + id);
            return false;
        }

        this.activeArenaId = id;
        this.activeArena = newArena;

        saveActiveArenaId();

//        plugin.getLogger().info("[ArenaConfig] Aktive Arena gesetzt: " + id + " / " + newArena.name());
        return true;
    }

    private void saveActiveArenaId() {
        if (eventConfig == null || eventFile == null) return;

        eventConfig.set(ACTIVE_ARENA_PATH, activeArenaId);

        try {
            eventConfig.save(eventFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[ArenaConfig] Konnte aktive Arena nicht in eventdaten.yml speichern: " + e.getMessage());
        }
    }

    /**
     * Lädt eine einzelne Arena aus arena-koordinaten.yml.
     */
    private ArenaData loadArenaData(String arenaId) {
        ConfigurationSection sec = arenaConfig.getConfigurationSection("arenas." + arenaId);

        if (sec == null) {
            plugin.getLogger().warning("[ArenaConfig] Keine Daten für Arena gefunden: " + arenaId);
            return null;
        }

        String name = sec.getString("name", arenaId);
        String description = sec.getString("description", "");
        String worldName = sec.getString("world", "world_mobarmy_arena");

        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().warning("[ArenaConfig] Welt nicht geladen: " + worldName + " für Arena " + arenaId);
            return null;
        }

        Location rotCorner1 = toLoc(world, sec.getList("rot.corner1"));
        Location rotCorner2 = toLoc(world, sec.getList("rot.corner2"));
        Location blauCorner1 = toLoc(world, sec.getList("blau.corner1"));
        Location blauCorner2 = toLoc(world, sec.getList("blau.corner2"));

        Location rotSpawn = toLocWithYaw(world, sec.getList("rot.teamspawn"));
        Location blauSpawn = toLocWithYaw(world, sec.getList("blau.teamspawn"));

        List<Location> rotMobSpawns = toLocList(world, sec.getList("rot.mobSpawns"));
        List<Location> blauMobSpawns = toLocList(world, sec.getList("blau.mobSpawns"));

        if (rotCorner1 == null || rotCorner2 == null || blauCorner1 == null || blauCorner2 == null) {
            plugin.getLogger().warning("[ArenaConfig] Corner-Daten unvollständig für Arena: " + arenaId);
            return null;
        }

        if (rotSpawn == null || blauSpawn == null) {
            plugin.getLogger().warning("[ArenaConfig] Team-Spawns unvollständig für Arena: " + arenaId);
            return null;
        }

        return new ArenaData(
                arenaId,
                name,
                description,
                worldName,
                rotCorner1,
                rotCorner2,
                blauCorner1,
                blauCorner2,
                rotSpawn,
                blauSpawn,
                rotMobSpawns,
                blauMobSpawns
        );
    }

    public boolean arenaExists(String arenaId) {
        if (arenaId == null) return false;
        return arenaConfig.isConfigurationSection("arenas." + arenaId.toLowerCase());
    }

    private String getFirstArenaId() {
        ConfigurationSection arenasSec = arenaConfig.getConfigurationSection("arenas");

        if (arenasSec == null || arenasSec.getKeys(false).isEmpty()) {
            return null;
        }

        return arenasSec.getKeys(false).iterator().next();
    }

    /**
     * Für die GUI.
     * Lädt nur ID, Name und Beschreibung aller Arenen,
     * aber nicht alle Spawn- und Corner-Daten komplett in den RAM.
     */
    public List<ArenaInfo> getAvailableArenas() {
        ConfigurationSection arenasSec = arenaConfig.getConfigurationSection("arenas");

        if (arenasSec == null) {
            return Collections.emptyList();
        }

        List<ArenaInfo> result = new ArrayList<>();

        for (String id : arenasSec.getKeys(false)) {
            ConfigurationSection sec = arenasSec.getConfigurationSection(id);
            if (sec == null) continue;

            String name = sec.getString("name", id);
            String description = sec.getString("description", "");

            result.add(new ArenaInfo(id.toLowerCase(), name, description));
        }

        return result;
    }

    public String getActiveArenaId() {
        return activeArenaId;
    }

    public ArenaData getActiveArena() {
        return activeArena;
    }

    public boolean hasActiveArena() {
        return activeArena != null;
    }

    public World getActiveWorld() {
        if (activeArena == null) return null;
        return Bukkit.getWorld(activeArena.world());
    }

    public Location getTeamSpawn(String team) {
        if (activeArena == null || team == null) return null;

        if (team.equalsIgnoreCase("rot")) {
            return activeArena.rotSpawn();
        }

        if (team.equalsIgnoreCase("blau")) {
            return activeArena.blauSpawn();
        }

        return null;
    }

    public List<Location> getMobSpawns(String team) {
        if (activeArena == null || team == null) return Collections.emptyList();

        if (team.equalsIgnoreCase("rot")) {
            return activeArena.rotMobSpawns();
        }

        if (team.equalsIgnoreCase("blau")) {
            return activeArena.blauMobSpawns();
        }

        return Collections.emptyList();
    }

    public Location getCorner1(String team) {
        if (activeArena == null || team == null) return null;

        if (team.equalsIgnoreCase("rot")) {
            return activeArena.rotCorner1();
        }

        if (team.equalsIgnoreCase("blau")) {
            return activeArena.blauCorner1();
        }

        return null;
    }

    public Location getCorner2(String team) {
        if (activeArena == null || team == null) return null;

        if (team.equalsIgnoreCase("rot")) {
            return activeArena.rotCorner2();
        }

        if (team.equalsIgnoreCase("blau")) {
            return activeArena.blauCorner2();
        }

        return null;
    }

    private Location toLoc(World world, List<?> list) {
        if (world == null || list == null || list.size() < 3) {
            return null;
        }

        try {
            return new Location(
                    world,
                    ((Number) list.get(0)).doubleValue(),
                    ((Number) list.get(1)).doubleValue(),
                    ((Number) list.get(2)).doubleValue()
            );
        } catch (Exception e) {
            return null;
        }
    }

    private Location toLocWithYaw(World world, List<?> list) {
        if (world == null || list == null || list.size() < 3) {
            return null;
        }

        try {
            double x = ((Number) list.get(0)).doubleValue();
            double y = ((Number) list.get(1)).doubleValue();
            double z = ((Number) list.get(2)).doubleValue();

            float yaw = list.size() >= 4 ? ((Number) list.get(3)).floatValue() : 0f;
            float pitch = list.size() >= 5 ? ((Number) list.get(4)).floatValue() : 0f;

            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Location> toLocList(World world, List<?> rawList) {
        List<Location> result = new ArrayList<>();

        if (world == null || rawList == null) {
            return result;
        }

        for (Object object : rawList) {
            if (!(object instanceof List<?> coords)) continue;
            if (coords.size() < 3) continue;

            try {
                result.add(new Location(
                        world,
                        ((Number) coords.get(0)).doubleValue(),
                        ((Number) coords.get(1)).doubleValue(),
                        ((Number) coords.get(2)).doubleValue()
                ));
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    /**
     * Komplett geladene aktive Arena.
     * Davon liegt im RAM immer nur eine aktuell aktive Arena.
     */
    public record ArenaData(
            String id,
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

    /**
     * Kleine Arena-Info für GUI-Buttons.
     * Diese Daten sind leicht und werden nur zur Anzeige benutzt.
     */
    public record ArenaInfo(
            String id,
            String name,
            String description
    ) {}
}