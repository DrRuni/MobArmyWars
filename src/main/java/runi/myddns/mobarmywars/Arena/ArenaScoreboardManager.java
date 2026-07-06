package runi.myddns.mobarmywars.Arena;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ArenaScoreboardManager {

    private final JavaPlugin plugin;
    private final ArenaManager arenaManager;
    private final Map<String, Integer> killCounts = new HashMap<>();
    private File scoreboardFile;
    private FileConfiguration scoreboardConfig;

    private int taskId = -1;
    private boolean running = false;

    private static final String STATUS_LINE   = ChatColor.WHITE + "Status:";
    private static final String ROT_WAVE_LINE = ChatColor.RED + "Rot Wave:";
    private static final String ROT_KILL_LINE = ChatColor.RED + "Rot Kills:";
    private static final String BLAU_WAVE_LINE = ChatColor.BLUE + "Blau Wave:";
    private static final String BLAU_KILL_LINE = ChatColor.BLUE + "Blau Kills:";
    private static final String EMPTY_1 = ChatColor.BLACK + "";
    private static final String EMPTY_2 = ChatColor.DARK_GRAY + "";

    public ArenaScoreboardManager(JavaPlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;

        killCounts.put("Rot", 0);
        killCounts.put("Blau", 0);

        initScoreboardFile();
        loadKills();
    }

    private void initScoreboardFile() {
        scoreboardFile = new File(plugin.getDataFolder(), "scoreboard.yml");
        if (!scoreboardFile.exists()) {
            try {
                scoreboardFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("❌ Konnte scoreboard.yml nicht erstellen!");
            }
        }
        scoreboardConfig = YamlConfiguration.loadConfiguration(scoreboardFile);
    }

    public void saveKills() {
        scoreboardConfig.set("kills.Rot", killCounts.getOrDefault("Rot", 0));
        scoreboardConfig.set("kills.Blau", killCounts.getOrDefault("Blau", 0));
        try {
            scoreboardConfig.save(scoreboardFile);
        } catch (IOException e) {
            plugin.getLogger().severe("❌ Fehler beim Speichern von scoreboard.yml!");
            e.printStackTrace();
        }
    }

    public void loadKills() {
        int rot = scoreboardConfig.getInt("kills.Rot", 0);
        int blau = scoreboardConfig.getInt("kills.Blau", 0);
        killCounts.put("Rot", rot);
        killCounts.put("Blau", blau);
    }

    public void addKill(String team) {
        killCounts.put(team, killCounts.getOrDefault(team, 0) + 1);
        saveKills();
    }

    public void setKillCount(String team, int count) {
        killCounts.put(team, count);
        saveKills();
    }

    public void startUpdating() {

        if (running) return;
        running = true;

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {

                    String world = player.getWorld().getName().toLowerCase();
                    if (!(world.contains("mobarmylobby") || world.contains("rot") || world.contains("blau"))) {
                        continue;
                    }

                    String team = arenaManager.getPlugin().getTeamManager().getPlayerTeam(player);
                    if (team == null) continue;

                    updatePlayerScoreboard(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L).getTaskId();
    }

    private void updatePlayerScoreboard(Player player) {

        Scoreboard board = player.getScoreboard();
        if (board == null || board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
        }

        Objective obj = board.getObjective("arena");
        if (obj == null) {
            obj = board.registerNewObjective("arena", "dummy", ChatColor.GOLD + "MobArmyWars");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            obj.getScore(STATUS_LINE).setScore(9);
            obj.getScore(EMPTY_1).setScore(8);

            obj.getScore(ROT_WAVE_LINE).setScore(7);
            obj.getScore(ROT_KILL_LINE).setScore(6);

            obj.getScore(EMPTY_2).setScore(5);

            obj.getScore(BLAU_WAVE_LINE).setScore(4);
            obj.getScore(BLAU_KILL_LINE).setScore(3);

            Team statusTeam = board.registerNewTeam("status");
            statusTeam.addEntry(STATUS_LINE);

            Team rotWave = board.registerNewTeam("rotWave");
            rotWave.addEntry(ROT_WAVE_LINE);

            Team rotKills = board.registerNewTeam("rotKills");
            rotKills.addEntry(ROT_KILL_LINE);

            Team blauWave = board.registerNewTeam("blauWave");
            blauWave.addEntry(BLAU_WAVE_LINE);

            Team blauKills = board.registerNewTeam("blauKills");
            blauKills.addEntry(BLAU_KILL_LINE);
        }

        Team statusTeam = board.getTeam("status");
        Team rotWave = board.getTeam("rotWave");
        Team rotKills = board.getTeam("rotKills");
        Team blauWave = board.getTeam("blauWave");
        Team blauKills = board.getTeam("blauKills");

        if (statusTeam == null || rotWave == null || rotKills == null
                || blauWave == null || blauKills == null) {
            return;
        }

        if (!arenaManager.isArenaRunning()) {
            statusTeam.setSuffix(ChatColor.GRAY + " Warten");
        } else if (arenaManager.getWinningTeam() != null) {
            statusTeam.setSuffix(ChatColor.GOLD + " Beendet");
        } else {
            statusTeam.setSuffix(ChatColor.GREEN + " Kampf");
        }

        rotWave.setSuffix(ChatColor.GRAY + " " + (arenaManager.getCurrentWave("Rot") + 1));
        rotKills.setSuffix(ChatColor.GRAY + " " + getKillCount("Rot"));

        blauWave.setSuffix(ChatColor.GRAY + " " + (arenaManager.getCurrentWave("Blau") + 1));
        blauKills.setSuffix(ChatColor.GRAY + " " + getKillCount("Blau"));

        player.setScoreboard(board);
    }

    public void stopUpdating() {
        if (!running) return;

        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            clearScoreboard(p);
        }

        running = false;
    }

    public boolean isArenaBoardActive() {
        return running;
    }

    public void resetKills() {
        killCounts.put("Rot", 0);
        killCounts.put("Blau", 0);
        saveKills();
    }

    public void reset() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }
    public Map<String, Integer> getKillCounts() {
        return killCounts;
    }

    public int getKillCount(String team) {
        return killCounts.getOrDefault(team, 0);
    }

    public int getWave(String team) {
        return arenaManager.getCurrentWave(team);
    }

    public void clearScoreboard(Player player) {
        if (player == null) return;
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }
}