package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.HashSet;

public class TeamScoreboardManager {

    private final MobArmyMain plugin;

    private Scoreboard scoreboard;
    private Team teamRot;
    private Team teamBlau;
    private Objective sidebar;

    public TeamScoreboardManager(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void createBoard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        scoreboard = manager.getNewScoreboard();

        teamRot = scoreboard.registerNewTeam("Rot");
        teamRot.setDisplayName(ChatColor.RED + "Team Rot");
        teamRot.setColor(ChatColor.RED);

        teamBlau = scoreboard.registerNewTeam("Blau");
        teamBlau.setDisplayName(ChatColor.BLUE + "Team Blau");
        teamBlau.setColor(ChatColor.BLUE);

        sidebar = scoreboard.registerNewObjective("teams", "dummy", ChatColor.GOLD + "Team Übersicht");
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

        updateBoard();
    }

    public void rebuildBoard() {
        destroyBoard();
        createBoard();

        for (Player player : Bukkit.getOnlinePlayers()) {
            String team = plugin.getTeamManager().getPlayerTeam(player);

            if (team.equalsIgnoreCase("Rot")) {
                teamRot.addEntry(player.getName());
            } else if (team.equalsIgnoreCase("Blau")) {
                teamBlau.addEntry(player.getName());
            }
        }

        updateBoard();
    }

    public void destroyBoard() {
        if (sidebar != null) {
            sidebar.unregister();
            sidebar = null;
        }

        if (teamRot != null) {
            teamRot.unregister();
            teamRot = null;
        }

        if (teamBlau != null) {
            teamBlau.unregister();
            teamBlau = null;
        }

        scoreboard = null;
    }

    public void addPlayerToTeam(Player player, String team) {
        ensureBoard();

        removePlayer(player);

        if (team.equalsIgnoreCase("Rot")) {
            teamRot.addEntry(player.getName());
        } else if (team.equalsIgnoreCase("Blau")) {
            teamBlau.addEntry(player.getName());
        }

        updateBoard();
    }

    public void removePlayer(Player player) {
        if (scoreboard == null) return;

        if (teamRot != null) {
            teamRot.removeEntry(player.getName());
        }

        if (teamBlau != null) {
            teamBlau.removeEntry(player.getName());
        }

        updateBoard();
    }

    public void setBoard(Player player) {
        ensureBoard();
        player.setScoreboard(scoreboard);
    }

    public void setBoardForAll() {
        ensureBoard();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    public void updateBoard() {
        if (sidebar == null || scoreboard == null) return;

        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }

        int score = 15;
        int empty = 0;

        sidebar.getScore(emptyLine(empty++)).setScore(score--);

        sidebar.getScore(ChatColor.RED + "Team Rot:").setScore(score--);
        for (String name : teamRot.getEntries()) {
            sidebar.getScore(ChatColor.GREEN + "- " + name).setScore(score--);
        }

        sidebar.getScore(emptyLine(empty++)).setScore(score--);

        sidebar.getScore(ChatColor.BLUE + "Team Blau:").setScore(score--);
        for (String name : teamBlau.getEntries()) {
            sidebar.getScore(ChatColor.GREEN + "- " + name).setScore(score--);
        }

        sidebar.getScore(emptyLine(empty)).setScore(score);
    }

    public Scoreboard getScoreboard() {
        ensureBoard();
        return scoreboard;
    }

    private void ensureBoard() {
        if (scoreboard == null || teamRot == null || teamBlau == null || sidebar == null) {
            createBoard();
        }
    }

    private String emptyLine(int i) {
        ChatColor[] colors = ChatColor.values();
        return colors[i].toString();
    }
}
