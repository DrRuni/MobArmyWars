package runi.myddns.mobarmywars.Managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class ResumeManager {

    private final MobArmyMain plugin;

    private File file;
    private FileConfiguration config;
    private boolean suppressSave = false;
    private PlayerLocationManager locationManager;
    private BukkitTask autoSaveTask;

    public static final int PHASE_LOBBY = 0;
    public static final int PHASE_TEAMWELT = 1;
    public static final int PHASE_WAVEAUSWAHL = 2;
    public static final int PHASE_ARENA = 3;

    public ResumeManager(MobArmyMain plugin) {
        this.plugin = plugin;
        createFileIfNotExists();
        loadConfig();

        this.locationManager = new PlayerLocationManager(config);
    }

    private void createFileIfNotExists() {
        file = new File(plugin.getDataFolder(), "eventdaten.yml");

        if (!file.exists()) {
            try {
                writeFreshFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeFreshFile() throws IOException {
        String content =
                "event:\n" +
                        "  started: false\n" +
                        "  paused: false\n" +
                        "\n" +
                        "phase: 0\n" +
                        "\n" +
                        "timer:\n" +
                        "  time: 3600\n" +
                        "  forward: false\n" +
                        "\n" +
                        "players: {}\n";

        file.getParentFile().mkdirs();
        java.nio.file.Files.writeString(file.toPath(), content);
    }

    private void loadConfig() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void saveConfig() {
        if (suppressSave) return;

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void savePhase(int phase) {
        config.set("phase", phase);
        saveConfig();
    }

    public int loadPhase() {
        return config.getInt("phase", PHASE_LOBBY);
    }

    public void saveTimerState(int time, boolean forward) {
        config.set("timer.time", time);
        config.set("timer.forward", forward);
        saveConfig();
    }

    public int loadTimerTime() {
        return config.getInt("timer.time", 3600);
    }

    public boolean loadTimerDirection() {
        return config.getBoolean("timer.forward", false);
    }

    public void startAutoSavePositions() {
        if (autoSaveTask != null) return;

        autoSaveTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::saveAllPlayerPositions,
                0L,
                20L * 10
        );
    }

    public void stopAutoSavePositions() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }

    private void saveAllPlayerPositions() {
        Set<String> players = plugin.getTeamManager().getAllTeamPlayers();

        for (String name : players) {
            Player p = Bukkit.getPlayerExact(name);
            if (p == null || !p.isOnline()) continue;

            locationManager.saveLastLocation(p);
        }

        saveConfig();
    }

    public void savePlayerSpawn(Player player, Location loc) {
        locationManager.saveSpawn(player, loc);
        saveConfig();
    }

    public boolean restorePlayerPosition(Player player) {
        if (player == null) return false;

        Location last = locationManager.getLastLocation(player);
        if (last != null) {
            player.teleport(last);
            return true;
        }

        Location spawn = locationManager.getSpawn(player);
        if (spawn != null) {
            player.teleport(spawn);
            return true;
        }

        return false;
    }

    public boolean isEventStarted() {
        return config.getBoolean("event.started", false);
    }

    public boolean isEventPaused() {
        return config.getBoolean("event.paused", false);
    }

    public void setEventStarted(boolean value) {
        config.set("event.started", value);
        saveConfig();
    }

    public void setEventPaused(boolean value) {
        config.set("event.paused", value);
        saveConfig();
    }

    public boolean resumeEvent() {

        for (Player p : Bukkit.getOnlinePlayers()) {

            Location target = locationManager.getLastLocation(p);
            if (target == null) {
                target = locationManager.getSpawn(p);
            }

            if (target != null) {
                p.teleport(target);
            } else {
                p.teleport(p.getWorld().getSpawnLocation());
            }

            p.setGameMode(org.bukkit.GameMode.SURVIVAL);
            p.setInvulnerable(false);
        }

        Bukkit.broadcastMessage(ChatColor.GREEN + "▶ Event erfolgreich fortgesetzt.");

        plugin.getTimerManager().startTimer();

        plugin.getMobSaveManager().setMobSaveMode(
                MobSaveManager.MobSaveMode.ENABLED
        );

        startAutoSavePositions();
        return true;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void reseteventdaten() {
        config.set("event.started", false);
        config.set("event.paused", false);
        config.set("phase", PHASE_LOBBY);
        config.set("players", null);
        config.set("timer.time", 3600);
        config.set("timer.forward", false);

        stopAutoSavePositions();
        saveConfig();
    }

    public Location getSavedSpawn(Player player) {
        return locationManager.getSpawn(player);
    }

    public void beginBatch() {
        suppressSave = true;
    }

    public void endBatch() {
        suppressSave = false;
        saveConfig();
    }
}