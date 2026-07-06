package runi.myddns.mobarmywars.Managers.World;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import runi.myddns.mobarmywars.Managers.Event.MobSaveManager;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Message;

import java.io.File;
import java.io.IOException;

public class ResumeManager {

    private final MobArmyMain plugin;

    private File file;
    private FileConfiguration config;
    private boolean suppressSave = false;
    private PlayerLocationManager locationManager;

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
                "# Hier werden alle benötigten Daten für das Resume-Event gespeichert\n" +
                        "#\n" +
                        "# event.started = Gibt an, ob das Event bereits gestartet wurde\n" +
                        "# event.paused  = Gibt an, ob das Event aktuell pausiert ist\n" +
                        "#\n" +
                        "# phase = Aktuelle Event-Phase\n" +
                        "# 0 = Lobby\n" +
                        "# 1 = Teamwelt\n" +
                        "# 2 = Wave-Auswahl\n" +
                        "# 3 = Arena\n" +
                        "#\n" +
                        "# timer.time    = Gespeicherte Zeit des Event-Timers\n" +
                        "# timer.forward = true = hochzählen, false = runterzählen\n" +
                        "#\n" +
                        "# players = Hier werden Spielerdaten gespeichert\n" +
                        "# z.B. letzter Standort, Spawnpunkt oder weitere Resume-Daten\n" +
                        "\n" +
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
            Message.sendChatToAll(ChatColor.RED + "✖ Das Event wurde noch nicht gestartet.");
            return false;
        }

        if (!isEventPaused()) {
            Message.sendChatToAll(ChatColor.RED + "✖ Das Event läuft bereits.");
            return false;
        }

        int phase = loadPhase();

        boolean allPlayersHaveTeam = true;

        for (Player player : Bukkit.getOnlinePlayers()) {
            String team = plugin.getTeamManager().getPlayerTeam(player);

            if (team == null || team.equalsIgnoreCase("Kein Team")) {
                allPlayersHaveTeam = false;
                Message.sendToPlayer(player, ChatColor.YELLOW + "⚠ Du hast noch kein Team!");
                plugin.getTeamSelectionGUI().openGUI(player);
            }
        }

        if (!allPlayersHaveTeam) {
            Message.sendChatToAll(ChatColor.RED + "✖ Resume abgebrochen. Nicht alle Online-Spieler haben ein Team.");
            return false;
        }

        plugin.getTimerManager().stopTimer();
        plugin.getTimerManager().updateBossBar(null);

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

            Message.sendChatToAll(ChatColor.GREEN + "▶ Event wird in der Teamwelt fortgesetzt.");
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

            Message.sendChatToAll(ChatColor.GREEN + "▶ Event wird in der Wave-Auswahl fortgesetzt.");
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

            Message.sendChatToAll(ChatColor.GREEN + "▶ Event wird in der Arena fortgesetzt.");
            return true;
        }

        Message.sendChatToAll(ChatColor.DARK_RED + "✖ Unbekannte Phase. Resume abgebrochen.");
        return false;
    }

    private boolean isInPlayersTeamWorldGroup(Player player, Location loc) {
        if (player == null || loc == null || loc.getWorld() == null) return false;

        String team = plugin.getTeamManager().getPlayerTeam(player);
        String worldName = loc.getWorld().getName().toLowerCase();

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