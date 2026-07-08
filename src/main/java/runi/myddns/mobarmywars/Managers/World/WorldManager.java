package runi.myddns.mobarmywars.Managers.World;

import org.bukkit.*;
import org.bukkit.entity.Player;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.ConsoleColor;

import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import java.io.*;

public class WorldManager {

    private final MobArmyMain plugin;

    private static final String WORLD_LOBBY = "world_mobarmy_lobby";
    private static final String WORLD_ARENA = "world_mobarmy_arena";

    private long teamSeed;
    private boolean isWorldResetRunning = false;

    public WorldManager(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void checkWorldsOnStartup() {

        checkTemplateWorlds();
        checkTeamWorlds();

        loadWorlds(
                WORLD_LOBBY,
                WORLD_ARENA,
                "world_rot",
                "world_blau",
                "world_rot_nether",
                "world_blau_nether"
        );

        plugin.getWorldSettings().applyAllSettings();
    }

    private void checkTemplateWorlds() {

        checkTemplateWorld(WORLD_LOBBY, "Lobby-Welt");
        checkTemplateWorld(WORLD_ARENA, "Arena-Welt");
    }

    public void resetLobbyWorld() {

        List<Player> players = getPlayersInWorld(WORLD_LOBBY);

        for (Player p : players) {
            if (p.isOnline()) {
                TeleportManager.teleport(p, WORLD_ARENA);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (p.isOnline()) {
                        p.sendActionBar(ChatColor.RED + "⚠ Lobby-Welt wird neu geladen...");
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                    }
                }, 10L);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            unloadAndDeleteWorld(WORLD_LOBBY);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {

                extractTemplateWorldFromZip(WORLD_LOBBY);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {

                    loadWorld(WORLD_LOBBY);
                    plugin.getWorldSettings().applyAllSettings();

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {

                        for (Player p : players) {
                            if (p.isOnline()) {
                                TeleportManager.teleport(p, WORLD_LOBBY);
                            }
                        }

                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.sendMessage(ChatColor.GREEN + "✔ LOBBY-Welt wurde neu geladen!");
                            player.sendMessage("");
                            player.sendMessage(ChatColor.DARK_RED + "  Die Spieler- und Eventdaten sind noch geladen,");
                            player.sendMessage(ChatColor.DARK_RED + "  diese können nur über " + ChatColor.GOLD + "Reset Spielfortschritt ");
                            player.sendMessage(ChatColor.DARK_RED + "  zurückgesetzt werden!");
                            player.sendMessage("");
                        }

                        Bukkit.getConsoleSender().sendMessage("");
                        Bukkit.getConsoleSender().sendMessage(
                                ConsoleColor.LIME + "        LOBBY-Welt neu erstellt" + ConsoleColor.RESET);
                        Bukkit.getConsoleSender().sendMessage("");

                        endWorldReset();

                    }, 40L);
                }, 40L);
            }, 40L);
        }, 60L);
    }

    public void resetArenaWorld() {

        List<Player> players = getPlayersInWorld(WORLD_ARENA);

        for (Player p : players) {
            if (p.isOnline()) {
                TeleportManager.teleport(p, WORLD_LOBBY);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (p.isOnline()) {
                        p.sendActionBar(ChatColor.RED + "⚠ Arena-Welt wird neu geladen...");
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                    }
                }, 10L);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            unloadAndDeleteWorld(WORLD_ARENA);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {

                extractTemplateWorldFromZip(WORLD_ARENA);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {

                    loadWorld(WORLD_ARENA);
                    plugin.getWorldSettings().applyAllSettings();

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {


                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.sendMessage("");
                            player.sendMessage(ChatColor.GREEN + "✔ ARENA-Welt wurde neu geladen!");
                            player.sendMessage("");
                            player.sendMessage(ChatColor.DARK_RED + "  Die Spieler- und Eventdaten sind noch geladen,");
                            player.sendMessage(ChatColor.DARK_RED + "  diese können nur über " + ChatColor.GOLD + "Reset Spielfortschritt ");
                            player.sendMessage(ChatColor.DARK_RED + "  zurückgesetzt werden!");
                            player.sendMessage("");
                        }

                        Bukkit.getConsoleSender().sendMessage("");
                        Bukkit.getConsoleSender().sendMessage(
                                ConsoleColor.LIME + "        ARENA-Welt neu erstellt" + ConsoleColor.RESET);
                        Bukkit.getConsoleSender().sendMessage("");

                        endWorldReset();

                    }, 40L); // Abschluss
                }, 40L); // World laden
            }, 40L); // Entpacken
        }, 60L); // Teleports
    }

    private void checkTeamWorlds() {

        boolean rotWorld = worldExists("world_rot");
        boolean rotNether = worldExists("world_rot_nether");
        boolean blauWorld = worldExists("world_blau");
        boolean blauNether = worldExists("world_blau_nether");
        boolean anyWorldExists = rotWorld || rotNether || blauWorld || blauNether;

        if (!anyWorldExists) {
            teamSeed = new Random().nextLong();
        }

        // ROT Overworld
        if (!rotWorld) {
            Bukkit.getConsoleSender().sendMessage(
                    ConsoleColor.RED +
                            "        Teamwelt ROT wird erzeugt!" +
                            ConsoleColor.RESET);
            Bukkit.getConsoleSender().sendMessage("");
            createWorld("world_rot", World.Environment.NORMAL, teamSeed);
        } else {
            Bukkit.getConsoleSender().sendMessage(
                    ConsoleColor.LIME +
                            "        Teamwelt - " +
                            ConsoleColor.RED + "ROT" +
                            ConsoleColor.LIME + " vorhanden!" +
                            ConsoleColor.RESET);
            Bukkit.getConsoleSender().sendMessage("");
        }

        // ROT Nether
        if (!rotNether) {
            Bukkit.getConsoleSender().sendMessage(
                    ConsoleColor.RED +
                            "        Netherwelt ROT wird erzeugt!" +
                            ConsoleColor.RESET);
            Bukkit.getConsoleSender().sendMessage("");
            createWorld("world_rot_nether", World.Environment.NETHER, teamSeed);
        } else {
            Bukkit.getConsoleSender().sendMessage(
                    ConsoleColor.LIME +
                            "        Netherwelt - " +
                            ConsoleColor.RED + "ROT" +
                            ConsoleColor.LIME + " vorhanden!" +
                            ConsoleColor.RESET);
            Bukkit.getConsoleSender().sendMessage("");
        }

        // BLAU Overworld
        if (!blauWorld) {
            Bukkit.getConsoleSender().sendMessage(
                    ConsoleColor.DARK_AQUA +
                            "        Teamwelt BLAU wird erzeugt!" +
                            ConsoleColor.RESET);
            Bukkit.getConsoleSender().sendMessage("");
            createWorld("world_blau", World.Environment.NORMAL, teamSeed);
        } else {
            Bukkit.getConsoleSender().sendMessage(
                    ConsoleColor.LIME +
                            "        Teamwelt - " +
                            ConsoleColor.DARK_AQUA + "BLAU " +
                            ConsoleColor.LIME + "vorhanden!" +
                            ConsoleColor.RESET);
            Bukkit.getConsoleSender().sendMessage("");
        }

        // BLAU Nether
        if (!blauNether) {
            Bukkit.getConsoleSender().sendMessage(
                    ConsoleColor.DARK_AQUA +
                            "        Netherwelt BLAU wird erzeugt!" +
                            ConsoleColor.RESET);
            Bukkit.getConsoleSender().sendMessage("");
            createWorld("world_blau_nether", World.Environment.NETHER, teamSeed);
        } else {
            Bukkit.getConsoleSender().sendMessage(
                    ConsoleColor.LIME +
                            "        Netherwelt - " +
                            ConsoleColor.DARK_AQUA + "BLAU " +
                            ConsoleColor.LIME + "vorhanden!" +
                            ConsoleColor.RESET);
            Bukkit.getConsoleSender().sendMessage("");
        }

        Bukkit.getConsoleSender().sendMessage("");
    }

    private void createTeamWorld(String baseName) {
        createWorld(baseName, World.Environment.NORMAL, teamSeed);
        createWorld(baseName + "_nether", World.Environment.NETHER, teamSeed);
    }

    public void resetTeamWorlds() {

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.BLOOD_ORANGE +
                        "        Team-Welten werden ZURÜCKGESETZT!" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage("");

        List<Player> rotPlayers = new ArrayList<>();
        rotPlayers.addAll(getPlayersInWorld("world_rot"));
        rotPlayers.addAll(getPlayersInWorld("world_rot_nether"));

        List<Player> blauPlayers = new ArrayList<>();
        blauPlayers.addAll(getPlayersInWorld("world_blau"));
        blauPlayers.addAll(getPlayersInWorld("world_blau_nether"));

        for (Player p : merge(rotPlayers, blauPlayers)) {
            if (p.isOnline()) {
                TeleportManager.teleport(p, "world_mobarmy_lobby");

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (p.isOnline()) {
                        p.sendActionBar(ChatColor.RED + "⚠ Team-Welten werden neu generiert...");
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                    }
                }, 10L);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            MobArmyMain.getInstance()
                    .getPortalManager()
                    .clearPortalWorldData();

            unloadAndDeleteWorld("world_rot");
            unloadAndDeleteWorld("world_blau");
            unloadAndDeleteWorld("world_rot_nether");
            unloadAndDeleteWorld("world_blau_nether");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {

                teamSeed = new Random().nextLong();

                createTeamWorld("world_rot");
                createTeamWorld("world_blau");

                Bukkit.getScheduler().runTaskLater(plugin, () -> {

                    loadWorldsWithPreload(
                            "world_rot",
                            "world_blau",
                            "world_rot_nether",
                            "world_blau_nether"
                    );

                    plugin.getWorldSettings().applyToWorld(Bukkit.getWorld("world_rot"));
                    plugin.getWorldSettings().applyToWorld(Bukkit.getWorld("world_blau"));
                    plugin.getWorldSettings().applyToWorld(Bukkit.getWorld("world_rot_nether"));
                    plugin.getWorldSettings().applyToWorld(Bukkit.getWorld("world_blau_nether"));

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {

                        for (Player p : rotPlayers) {
                            if (p.isOnline()) {
                                TeleportManager.teleport(p, "world_rot");
                            }
                        }

                        for (Player p : blauPlayers) {
                            if (p.isOnline()) {
                                TeleportManager.teleport(p, "world_blau");
                            }
                        }

                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.sendMessage("");
                            player.sendMessage(ChatColor.GREEN + "✔ TEAM-Welten wurden neu generiert!");
                        }
                        Bukkit.getConsoleSender().sendMessage("");
                        Bukkit.getConsoleSender().sendMessage(
                                ConsoleColor.LIME + "        TEAM-Welten neu erstellt" + ConsoleColor.RESET);
                        Bukkit.getConsoleSender().sendMessage("");

                        endWorldReset();
                    }, 60L); // Spieler zurück
                }, 60L); // Worlds laden
            }, 100L); // Unload/Delete
        }, 20L); // Teleports
    }

    private void loadWorlds(String... names) {
        for (String name : names) {
            loadWorld(name);
        }
    }

    private void loadWorldsWithPreload(String... names) {
        for (String name : names) {
            World world = loadWorld(name);
            preloadSpawnChunks(world, name.endsWith("_nether") ? 1 : 2);
        }
    }

    private File getDimensionWorldFolder(String name) {
        return new File(Bukkit.getWorldContainer(), "world/dimensions/minecraft/" + name);
    }

    private boolean worldExists(String name) {
        World loaded = Bukkit.getWorld(name);
        if (loaded != null) {
            return true;
        }

        File legacyDir = new File(Bukkit.getWorldContainer(), name);
        if (new File(legacyDir, "level.dat").exists()) {
            return true;
        }

        File dimensionDir = getDimensionWorldFolder(name);

        return dimensionDir.exists() && (
                new File(dimensionDir, "region").exists()
                        || new File(dimensionDir, "entities").exists()
                        || new File(dimensionDir, "poi").exists()
                        || new File(dimensionDir, "paper-world.yml").exists()
        );
    }

    private World loadWorld(String name) {

        World existing = Bukkit.getWorld(name);
        if (existing != null) return existing;

        WorldCreator creator = new WorldCreator(name);

        if (name.endsWith("_nether")) {
            creator.environment(World.Environment.NETHER);
        } else {
            creator.environment(World.Environment.NORMAL);
        }

        return Bukkit.createWorld(creator);
    }

    private void unloadAndDeleteWorld(String name) {
        World w = Bukkit.getWorld(name);

        File actualFolder = null;

        if (w != null) {
            actualFolder = w.getWorldFolder();
            Bukkit.unloadWorld(w, false);
        }

        if (actualFolder != null) {
            deleteDirectoryRecursively(actualFolder);
        }

        File legacyFolder = new File(Bukkit.getWorldContainer(), name);
        deleteDirectoryRecursively(legacyFolder);

        File dimensionFolder = new File(
                Bukkit.getWorldContainer(),
                "world/dimensions/minecraft/" + name
        );
        deleteDirectoryRecursively(dimensionFolder);
    }

    private List<Player> getPlayersInWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        return world != null ? new ArrayList<>(world.getPlayers()) : new ArrayList<>();
    }

    private List<Player> merge(List<Player> a, List<Player> b) {
        List<Player> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    private void extractTemplateWorldFromZip(String worldName) {

        File zipFile = findNewestTemplateZipInDataFolder();

        if (zipFile == null || !zipFile.exists()) {
            plugin.getLogger().severe("❌ Keine Template-ZIP gefunden: world_mobarmy V*.zip");
            return;
        }

        File worldContainer = Bukkit.getWorldContainer();
        File legacyTargetRoot = new File(worldContainer, worldName);

        // Alte Reste vermeiden
        deleteDirectoryRecursively(legacyTargetRoot);
        deleteDirectoryRecursively(new File(worldContainer, "world/dimensions/minecraft/" + worldName));

        boolean found = false;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {

                String prefix = worldName + "/";

                if (!entry.getName().startsWith(prefix)) {
                    zis.closeEntry();
                    continue;
                }

                found = true;

                String relativePath = entry.getName().substring(prefix.length());
                if (relativePath.isEmpty()) {
                    zis.closeEntry();
                    continue;
                }

                File target = new File(legacyTargetRoot, relativePath);

                if (entry.isDirectory()) {
                    target.mkdirs();
                } else {
                    target.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(target)) {
                        zis.transferTo(fos);
                    }
                }

                zis.closeEntry();
            }

        } catch (IOException e) {
            plugin.getLogger().severe("❌ Fehler beim Entpacken der Welt " + worldName + ": " + e.getMessage());
            return;
        }

        if (!found) {
            plugin.getLogger().severe("❌ Welt " + worldName + " wurde in " + zipFile.getName() + " nicht gefunden!");
        }
    }

    private void preloadSpawnChunks(World world, int radius) {

        if (world == null) return;

        Location spawn = world.getSpawnLocation();
        int baseX = spawn.getBlockX() >> 4;
        int baseZ = spawn.getBlockZ() >> 4;

        Queue<int[]> queue = new ArrayDeque<>();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                queue.add(new int[]{baseX + x, baseZ + z});
            }
        }

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {

            int perTick = 2;

            for (int i = 0; i < perTick; i++) {
                int[] c = queue.poll();
                if (c == null) {
                    task.cancel();
                    return;
                }
                if (!world.isChunkLoaded(c[0], c[1])) {
                    world.loadChunk(c[0], c[1], true);
                }
            }

        }, 1L, 1L);
    }

    private void deleteDirectoryRecursively(File dir) {

        if (!dir.exists()) return;

        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectoryRecursively(f);
                } else {
                    if (!f.delete()) {
                        plugin.getLogger().warning("⚠ Konnte Datei nicht löschen: " + f.getPath());
                    }
                }
            }
        }

        if (!dir.delete()) {
            plugin.getLogger().warning("⚠ Konnte Ordner nicht löschen: " + dir.getPath());
        }
    }

    private void createWorld(String name, World.Environment environment, long seed) {

        WorldCreator creator = new WorldCreator(name);
        creator.environment(environment);
        creator.seed(seed);

        World world = Bukkit.createWorld(creator);
        if (world != null) {
            world.getChunkAt(world.getSpawnLocation()); // Spawn initialisieren
            world.save();
        }
    }

    public synchronized boolean tryStartWorldReset() {
        if (isWorldResetRunning) return false;
        isWorldResetRunning = true;
        return true;
    }

    private void endWorldReset() {
        isWorldResetRunning = false;
    }

    private void checkTemplateWorld(String worldName, String displayName) {

        if (!worldExists(worldName)) {
            Bukkit.getConsoleSender().sendMessage(
                    ConsoleColor.BLOOD_ORANGE +
                            "        " + displayName + " wird aus ZIP entpackt" +
                            ConsoleColor.RESET
            );

            Bukkit.getConsoleSender().sendMessage("");

            extractTemplateWorldFromZip(worldName);

        } else {
            Bukkit.getConsoleSender().sendMessage(
                    ConsoleColor.LIME +
                            "        " + displayName + " vorhanden" +
                            ConsoleColor.RESET
            );

            Bukkit.getConsoleSender().sendMessage("");
        }
    }

    private File findNewestTemplateZipInDataFolder() {
        File[] files = plugin.getDataFolder().listFiles((dir, name) ->
                name.startsWith("world_mobarmy V") && name.endsWith(".zip")
        );

        if (files == null || files.length == 0) {
            return null;
        }

        File newestFile = null;
        String newestVersion = null;

        for (File file : files) {
            String version = extractVersionFromZipName(file.getName());
            if (version == null) continue;

            if (newestVersion == null || compareVersions(version, newestVersion) > 0) {
                newestVersion = version;
                newestFile = file;
            }
        }

        return newestFile;
    }

    private String extractVersionFromZipName(String fileName) {
        String prefix = "world_mobarmy V";
        String suffix = ".zip";

        if (!fileName.startsWith(prefix) || !fileName.endsWith(suffix)) {
            return null;
        }

        return fileName.substring(prefix.length(), fileName.length() - suffix.length());
    }

    private int compareVersions(String v1, String v2) {
        v1 = v1.replace("V", "").replace("v", "");
        v2 = v2.replace("V", "").replace("v", "");

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}