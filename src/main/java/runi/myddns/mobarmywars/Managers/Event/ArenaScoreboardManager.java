package runi.myddns.mobarmywars.Managers.Event;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Criteria;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.UUID;

public class ArenaScoreboardManager {

    private final MobArmyMain plugin;
    private final ArenaEventManager arenaManager;
    private final Map<String, Integer> killCounts = new HashMap<>();
    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();

    private File scoreboardFile;
    private FileConfiguration scoreboardConfig;

    private static final String EMPTY_TOP = "§0";
    private static final String PANEL_TOP = "\uE020";
    private static final String PANEL_MID = "\uE021";
    private static final String PANEL_BOTTOM = "\uE022";

    private static final String BACK = "\uF805";
    private static final String RIGHT_SHIFT = "\uF806";

    private static final String TEAM_INDENT = "      ";
    private static final String TEXT_INDENT = "       ";

    private static final String TOP_LINE = PANEL_TOP;

    private static final String MIDDLE_EMPTY_LINE = PANEL_MID + "§4";
    private static final String BOTTOM_EMPTY_LINE = PANEL_MID + "§8";

    private static final String BOTTOM_LINE = PANEL_BOTTOM;

    public ArenaScoreboardManager(
            MobArmyMain plugin,
            ArenaEventManager arenaManager
    ) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;

        killCounts.put("Rot", 0);
        killCounts.put("Blau", 0);

        initScoreboardFile();
        loadKills();
    }

    private void initScoreboardFile() {
        scoreboardFile = new File(plugin.getDataFolder(), "scoreboard.yml");
        scoreboardConfig = YamlConfiguration.loadConfiguration(scoreboardFile);
    }

    public void saveKills() {
        scoreboardConfig.set("kills.Rot", killCounts.getOrDefault("Rot", 0));
        scoreboardConfig.set("kills.Blau", killCounts.getOrDefault("Blau", 0));

        try {
            scoreboardConfig.save(scoreboardFile);
        } catch (IOException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to save scoreboard.yml.",
                    e
            );
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
        updateAllArenaPlayers();
    }

    public void setKillCount(String team, int count) {
        killCounts.put(team, count);
        saveKills();
        updateAllArenaPlayers();
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
        updateAllArenaPlayers();
    }

    public int getKillCount(String team) {
        return killCounts.getOrDefault(team, 0);
    }

    private Scoreboard getOrCreateBoard(Player player) {
        Scoreboard board = playerBoards.get(player.getUniqueId());
        if (board != null) {
            return board;
        }

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        board = manager.getNewScoreboard();

        String rotTeamLine = rotTeamLine();
        String rotWaveLine = rotWaveLine();
        String rotKillLine = rotKillLine();

        String blauTeamLine = blauTeamLine();
        String blauWaveLine = blauWaveLine();
        String blauKillLine = blauKillLine();

        Objective obj = board.registerNewObjective(
                "arena",
                Criteria.DUMMY,
                Component.text("\uE010")
        );

        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(NumberFormat.blank());

        obj.getScore(EMPTY_TOP).setScore(12);
        obj.getScore(TOP_LINE).setScore(11);
        obj.getScore(rotTeamLine).setScore(10);
        obj.getScore(rotWaveLine).setScore(9);
        obj.getScore(rotKillLine).setScore(8);
        obj.getScore(MIDDLE_EMPTY_LINE).setScore(7);
        obj.getScore(blauTeamLine).setScore(6);
        obj.getScore(blauWaveLine).setScore(5);
        obj.getScore(blauKillLine).setScore(4);
        obj.getScore(BOTTOM_EMPTY_LINE).setScore(3);
        obj.getScore(BOTTOM_LINE).setScore(2);

        Team rotWave = board.registerNewTeam("rotWave");
        rotWave.addEntry(rotWaveLine);

        Team rotKills = board.registerNewTeam("rotKills");
        rotKills.addEntry(rotKillLine);

        Team blauWave = board.registerNewTeam("blauWave");
        blauWave.addEntry(blauWaveLine);

        Team blauKills = board.registerNewTeam("blauKills");
        blauKills.addEntry(blauKillLine);

        playerBoards.put(player.getUniqueId(), board);
        return board;
    }

    private void updateBoard(Scoreboard board) {
        Team rotWave = board.getTeam("rotWave");
        Team rotKills = board.getTeam("rotKills");
        Team blauWave = board.getTeam("blauWave");
        Team blauKills = board.getTeam("blauKills");

        if (rotWave == null
                || rotKills == null
                || blauWave == null
                || blauKills == null) {
            return;
        }

        rotWave.suffix(
                Component.text(
                        " "
                                + (arenaManager.getCurrentWave("Rot") + 1)
                                + RIGHT_SHIFT,
                        NamedTextColor.GRAY
                )
        );

        rotKills.suffix(
                Component.text(
                        " "
                                + getKillCount("Rot")
                                + RIGHT_SHIFT,
                        NamedTextColor.GRAY
                )
        );

        blauWave.suffix(
                Component.text(
                        " "
                                + (arenaManager.getCurrentWave("Blau") + 1)
                                + RIGHT_SHIFT,
                        NamedTextColor.GRAY
                )
        );

        blauKills.suffix(
                Component.text(
                        " "
                                + getKillCount("Blau")
                                + RIGHT_SHIFT,
                        NamedTextColor.GRAY
                )
        );
    }

    private String rotTeamLine() {
        return PANEL_MID
                + BACK
                + TEAM_INDENT
                + "\uE001 "
                + plugin.getLanguageManager().get(
                "arena-scoreboard.red-team"
        )
                + RIGHT_SHIFT;
    }

    private String rotWaveLine() {
        return PANEL_MID
                + BACK
                + TEXT_INDENT
                + "\uE004 "
                + plugin.getLanguageManager().get(
                "arena-scoreboard.wave"
        );
    }

    private String rotKillLine() {
        return PANEL_MID
                + BACK
                + TEXT_INDENT
                + "\uE005 "
                + plugin.getLanguageManager().get(
                "arena-scoreboard.kills"
        );
    }

    private String blauTeamLine() {
        return PANEL_MID
                + BACK
                + TEAM_INDENT
                + "\uE002 "
                + plugin.getLanguageManager().get(
                "arena-scoreboard.blue-team"
        )
                + RIGHT_SHIFT;
    }

    private String blauWaveLine() {
        return PANEL_MID
                + BACK
                + TEXT_INDENT
                + "\uE004 "
                + plugin.getLanguageManager().get(
                "arena-scoreboard.wave"
        );
    }

    private String blauKillLine() {
        return PANEL_MID
                + BACK
                + TEXT_INDENT
                + "\uE005 "
                + plugin.getLanguageManager().get(
                "arena-scoreboard.kills"
        );
    }

    public void reloadLanguage() {

        var playersWithBoard =
                playerBoards.keySet().stream()
                        .map(Bukkit::getPlayer)
                        .filter(player -> player != null && player.isOnline())
                        .toList();

        playerBoards.clear();

        for (Player player : playersWithBoard) {
            setBoard(player);
        }
    }
}