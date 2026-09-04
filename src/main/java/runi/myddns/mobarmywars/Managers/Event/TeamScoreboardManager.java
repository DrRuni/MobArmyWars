package runi.myddns.mobarmywars.Managers.Event;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TeamScoreboardManager {

    private final MobArmyMain plugin;

    private Scoreboard scoreboard;
    private Team teamRot;
    private Team teamBlau;
    private Objective sidebar;

    private static final String PANEL_TOP = "\uE020";
    private static final String PANEL_MID = "\uE021";
    private static final String PANEL_BOTTOM = "\uE022";

    private static final String BACK = "\uF805";
    private static final String RIGHT_SHIFT = "\uF806";

    private static final String TEAM_INDENT = "      ";
    private static final String PLAYER_INDENT = "       ";

    private static final String EMPTY_TOP = "§0";
    private static final String EMPTY_BOTTOM = "§8";

    public TeamScoreboardManager(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void createBoard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();

        scoreboard = manager.getNewScoreboard();

        teamRot = scoreboard.registerNewTeam("Rot");
        teamRot.displayName(
                plugin.getLanguageManager()
                        .getComponent("team-scoreboard.red-team")
        );
        teamRot.color(NamedTextColor.RED);

        teamBlau = scoreboard.registerNewTeam("Blau");
        teamBlau.displayName(
                plugin.getLanguageManager()
                        .getComponent("team-scoreboard.blue-team")
        );
        teamBlau.color(NamedTextColor.BLUE);

        sidebar = scoreboard.registerNewObjective(
                "teams",
                Criteria.DUMMY,
                Component.text("\uE010")
        );

        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
        sidebar.numberFormat(NumberFormat.blank());

        updateBoard();
    }

    public void rebuildBoard() {

        Scoreboard oldScoreboard = scoreboard;

        Set<Player> currentViewers = new HashSet<>();

        if (oldScoreboard != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getScoreboard() == oldScoreboard) {
                    currentViewers.add(player);
                }
            }
        }

        destroyBoard();
        createBoard();

        for (Player player : Bukkit.getOnlinePlayers()) {
            String team =
                    plugin.getTeamManager().getPlayerTeam(player);

            if ("Rot".equalsIgnoreCase(team)) {
                teamRot.addEntry(player.getName());
            } else if ("Blau".equalsIgnoreCase(team)) {
                teamBlau.addEntry(player.getName());
            }
        }

        updateBoard();

        for (Player player : currentViewers) {
            player.setScoreboard(scoreboard);
        }
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

        if ("Rot".equalsIgnoreCase(team)) {
            teamRot.addEntry(player.getName());
        } else if ("Blau".equalsIgnoreCase(team)) {
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

    public void updateBoard() {
        if (sidebar == null || scoreboard == null) return;

        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }

        int score = 15;

        Set<String> savedPlayers =
                plugin.getTeamManager().getAllTeamPlayers();

        sidebar.getScore(
                EMPTY_TOP
        ).setScore(score--);

        sidebar.getScore(
                PANEL_TOP
        ).setScore(score--);

        sidebar.getScore(
                PANEL_MID
                        + BACK
                        + TEAM_INDENT
                        + "\uE001 "
                        + "§c"
                        + plugin.getLanguageManager().get(
                        "team-scoreboard.red-label"
                )
                        + RIGHT_SHIFT
        ).setScore(score--);

        for (String name : savedPlayers) {
            String team =
                    plugin.getTeamManager().getPlayerTeam(
                            Bukkit.getOfflinePlayer(name)
                    );

            if ("Rot".equalsIgnoreCase(team)) {
                sidebar.getScore(
                        PANEL_MID
                                + BACK
                                + formatPlayerName(name)
                                + RIGHT_SHIFT
                ).setScore(score--);
            }
        }

        sidebar.getScore(
                PANEL_MID + BACK + "§4"
        ).setScore(score--);

        sidebar.getScore(
                PANEL_MID
                        + BACK
                        + TEAM_INDENT
                        + "\uE002 "
                        + "§9"
                        + plugin.getLanguageManager().get(
                        "team-scoreboard.blue-label"
                )
                        + RIGHT_SHIFT
        ).setScore(score--);

        for (String name : savedPlayers) {
            String team =
                    plugin.getTeamManager().getPlayerTeam(
                            Bukkit.getOfflinePlayer(name)
                    );

            if ("Blau".equalsIgnoreCase(team)) {
                sidebar.getScore(
                        PANEL_MID
                                + BACK
                                + formatPlayerName(name)
                                + RIGHT_SHIFT
                ).setScore(score--);
            }
        }

        List<? extends Player> noTeamPlayers =
                Bukkit.getOnlinePlayers().stream()
                        .filter(player ->
                                "Kein Team".equalsIgnoreCase(
                                        plugin.getTeamManager()
                                                .getPlayerTeam(player)
                                )
                        )
                        .toList();

        if (!noTeamPlayers.isEmpty()) {

            sidebar.getScore(
                    PANEL_MID
            ).setScore(score--);

            sidebar.getScore(
                    PANEL_MID
                            + BACK
                            + TEAM_INDENT
                            + "\uE006 "
                            + "§7"
                            + plugin.getLanguageManager().get(
                            "team-scoreboard.no-team"
                    )
                            + RIGHT_SHIFT
            ).setScore(score--);

            for (Player online : noTeamPlayers) {
                sidebar.getScore(
                        PANEL_MID
                                + BACK
                                + PLAYER_INDENT
                                + "\uE003 "
                                + "§7"
                                + online.getName()
                                + RIGHT_SHIFT
                ).setScore(score--);
            }
        }

        sidebar.getScore(
                EMPTY_BOTTOM
        ).setScore(score--);

        sidebar.getScore(
                PANEL_BOTTOM
        ).setScore(score);
    }

    private String formatPlayerName(String name) {
        Player player = Bukkit.getPlayerExact(name);

        if (player != null && player.isOnline()) {
            return PLAYER_INDENT
                    + "\uE003 "
                    + "§a"
                    + name;
        }

        return PLAYER_INDENT
                + "\uE003 "
                + "§7"
                + name;
    }

    private void ensureBoard() {
        if (scoreboard == null
                || teamRot == null
                || teamBlau == null
                || sidebar == null) {

            createBoard();
        }
    }
}