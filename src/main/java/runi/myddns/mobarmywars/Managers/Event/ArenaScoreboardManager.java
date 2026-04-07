package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import runi.myddns.mobarmywars.Arena.ArenaManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArenaScoreboardManager {

    private final JavaPlugin plugin;
    private final ArenaManager arenaManager;
    private final Map<String, Integer> killCounts = new HashMap<>();
    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();

    private File scoreboardFile;
    private FileConfiguration scoreboardConfig;

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
                e.printStackTrace();
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

    public void setBoard(Player player) {
        if (player == null) return;

        Scoreboard board = getOrCreateBoard(player);
        updateBoard(board);
        player.setScoreboard(board);
    }

    public void updateBoard(Player player) {
        if (player == null) return;

        Scoreboard board = getOrCreateBoard(player);
        updateBoard(board);

        if (player.isOnline()) {
            player.setScoreboard(board);
        }
    }

    public void updateAllArenaPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasBoard(player)) {
                updateBoard(player);
            }
        }
    }

    public boolean hasBoard(Player player) {
        if (player == null) return false;
        return playerBoards.containsKey(player.getUniqueId());
    }

    public void removeBoard(Player player) {
        if (player == null) return;
        playerBoards.remove(player.getUniqueId());
    }

    public void clearAllBoards() {
        playerBoards.clear();
    }

    public void resetKills() {
        killCounts.put("Rot", 0);
        killCounts.put("Blau", 0);
        saveKills();
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

    private Scoreboard getOrCreateBoard(Player player) {
        Scoreboard board = playerBoards.get(player.getUniqueId());
        if (board != null) {
            return board;
        }

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            throw new IllegalStateException("ScoreboardManager ist null");
        }

        board = manager.getNewScoreboard();

        Objective obj = board.registerNewObjective("arena", "dummy", ChatColor.GOLD + "MobArmyWars");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.getScore(EMPTY_1).setScore(8);
        obj.getScore(ROT_WAVE_LINE).setScore(7);
        obj.getScore(ROT_KILL_LINE).setScore(6);
        obj.getScore(EMPTY_2).setScore(5);
        obj.getScore(BLAU_WAVE_LINE).setScore(4);
        obj.getScore(BLAU_KILL_LINE).setScore(3);

        Team rotWave = board.registerNewTeam("rotWave");
        rotWave.addEntry(ROT_WAVE_LINE);

        Team rotKills = board.registerNewTeam("rotKills");
        rotKills.addEntry(ROT_KILL_LINE);

        Team blauWave = board.registerNewTeam("blauWave");
        blauWave.addEntry(BLAU_WAVE_LINE);

        Team blauKills = board.registerNewTeam("blauKills");
        blauKills.addEntry(BLAU_KILL_LINE);

        playerBoards.put(player.getUniqueId(), board);
        return board;
    }

    private void updateBoard(Scoreboard board) {
        Team rotWave = board.getTeam("rotWave");
        Team rotKills = board.getTeam("rotKills");
        Team blauWave = board.getTeam("blauWave");
        Team blauKills = board.getTeam("blauKills");

        if (rotWave == null || rotKills == null || blauWave == null || blauKills == null) {
            return;
        }

        rotWave.setSuffix(ChatColor.GRAY + " " + (arenaManager.getCurrentWave("Rot") + 1));
        rotKills.setSuffix(ChatColor.GRAY + " " + getKillCount("Rot"));

        blauWave.setSuffix(ChatColor.GRAY + " " + (arenaManager.getCurrentWave("Blau") + 1));
        blauKills.setSuffix(ChatColor.GRAY + " " + getKillCount("Blau"));
    }
}