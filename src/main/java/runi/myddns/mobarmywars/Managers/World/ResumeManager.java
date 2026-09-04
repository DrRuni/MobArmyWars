package runi.myddns.mobarmywars.Managers.World;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import runi.myddns.mobarmywars.Managers.Event.MobSaveManager;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.Locale;
import java.util.logging.Level;
import java.io.File;
import java.io.IOException;

public class ResumeManager {

    private final MobArmyMain plugin;

    private final File file;
    private final FileConfiguration config;
    private boolean suppressSave = false;
    private final PlayerLocationManager locationManager;

    public static final int PHASE_LOBBY = 0;
    public static final int PHASE_TEAMWELT = 1;
    public static final int PHASE_WAVEAUSWAHL = 2;
    public static final int PHASE_ARENA = 3;

    public ResumeManager(MobArmyMain plugin) {
        this.plugin = plugin;
        this.file = new File(
                plugin.getDataFolder(),
                "eventdaten.yml"
        );
        this.config = YamlConfiguration.loadConfiguration(file);
        this.locationManager =
                new PlayerLocationManager(config);
    }

    private void saveConfig() {
        if (suppressSave) return;

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to save eventdaten.yml.",
                    e
            );
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

    public void savePlayerSpawn(Player player, Location loc) {
        locationManager.saveSpawn(player, loc);
        saveConfig();
    }

    public void savePlayerLastLocation(Player player) {
        if (player == null) return;

        locationManager.saveLastLocation(player);
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

        if (!isEventStarted()) {
            broadcast(
                    lang("resume-manager.not-started")
            );
            return false;
        }

        if (!isEventPaused()) {
            broadcast(
                    lang("resume-manager.already-running")
            );
            return false;
        }

        int phase = loadPhase();

        boolean allPlayersHaveTeam = true;

        for (Player player : Bukkit.getOnlinePlayers()) {
            String team = plugin.getTeamManager().getPlayerTeam(player);

            if (team == null || team.equalsIgnoreCase("Kein Team")) {
                allPlayersHaveTeam = false;
                player.sendMessage(
                        lang("resume-manager.no-team")
                );
                plugin.getTeamSelectionGUI().openGUI(player);
            }
        }

        if (!allPlayersHaveTeam) {
            broadcast(
                    lang("resume-manager.missing-teams")
            );
            return false;
        }

        plugin.getTimerManager().stopTimer();
        plugin.getTimerManager().updateBossBar();

        if (phase == PHASE_TEAMWELT) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String team = plugin.getTeamManager().getPlayerTeam(p);

                String worldName = null;
                if ("rot".equalsIgnoreCase(team)) {
                    worldName = "world_rot";
                } else if ("blau".equalsIgnoreCase(team)) {
                    worldName = "world_blau";
                }

                if (worldName == null) {
                    continue;
                }

                World teamWorld = Bukkit.getWorld(worldName);
                if (teamWorld == null) {
                    continue;
                }

                Location last = locationManager.getLastLocation(p);
                Location current = p.getLocation();
                Location target;

                if (isInPlayersTeamWorldGroup(p, current)) {
                    target = current;
                } else if (last != null && isInPlayersTeamWorldGroup(p, last)) {
                    target = last;
                } else {
                    target = teamWorld.getSpawnLocation();
                }

                p.teleport(target);
                p.setGameMode(org.bukkit.GameMode.SURVIVAL);
                p.setInvulnerable(false);
            }

            plugin.getTimerManager().setForward(false);
            plugin.getTimerManager().startTimer();

            plugin.getMobSaveManager().setMobSaveMode(MobSaveManager.MobSaveMode.ENABLED);

            setEventStarted(true);
            setEventPaused(false);

            broadcast(
                    lang("resume-manager.resumed-team-world")
            );
            return true;
        }

        if (phase == PHASE_WAVEAUSWAHL) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                TeleportManager.teleportToWaveSelection(p);
                p.setGameMode(org.bukkit.GameMode.SURVIVAL);
                p.setInvulnerable(false);

                savePlayerSpawn(p, p.getLocation());
            }

            saveTimerState(0, true);
            plugin.getTimerManager().setForward(true);
            plugin.getTimerManager().startTimer();

            plugin.getMobSaveManager().setMobSaveMode(MobSaveManager.MobSaveMode.DISABLED);

            setEventStarted(true);
            setEventPaused(false);

            broadcast(
                    lang("resume-manager.resumed-wave-selection")
            );
            return true;
        }

        if (phase == PHASE_ARENA) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                TeleportManager.teleportToArena(p);
                p.setGameMode(org.bukkit.GameMode.SURVIVAL);
                p.setInvulnerable(false);

                savePlayerSpawn(p, p.getLocation());
            }

            saveTimerState(0, true);
            plugin.getTimerManager().setForward(true);
            plugin.getTimerManager().startTimer();

            plugin.getMobSaveManager().setMobSaveMode(MobSaveManager.MobSaveMode.DISABLED);

            setEventStarted(true);
            setEventPaused(false);

            broadcast(
                    lang("resume-manager.resumed-arena")
            );
            return true;
        }

        broadcast(
                lang("resume-manager.unknown-phase")
        );
        return false;
    }

    private boolean isInPlayersTeamWorldGroup(Player player, Location loc) {
        if (player == null || loc == null || loc.getWorld() == null) return false;

        String team = plugin.getTeamManager().getPlayerTeam(player);
        String worldName =
                loc.getWorld()
                        .getName()
                        .toLowerCase(Locale.ROOT);

        if ("rot".equalsIgnoreCase(team)) {
            return worldName.equals("world_rot") || worldName.equals("world_rot_nether");
        }

        if ("blau".equalsIgnoreCase(team)) {
            return worldName.equals("world_blau") || worldName.equals("world_blau_nether");
        }

        return false;
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

        saveConfig();
    }

    public void beginBatch() {
        suppressSave = true;
    }

    public void endBatch() {
        suppressSave = false;
        saveConfig();
    }

    private Component lang(String path) {
        return plugin.getLanguageManager()
                .getComponent(path);
    }

    private void broadcast(Component message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }
}