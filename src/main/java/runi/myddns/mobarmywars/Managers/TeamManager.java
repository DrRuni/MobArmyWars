package runi.myddns.mobarmywars.Managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import runi.myddns.mobarmywars.Arena.ArenaManager;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TeamManager implements Listener {

    private final MobArmyMain plugin;
    private final Map<String, String> playerTeams = new HashMap<>();

    private Scoreboard scoreboard;
    private Team teamRot;
    private Team teamBlau;
    private Objective sidebar;

    public TeamManager(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void assignTeam(Player player, String team) {

        if (plugin.getArenaManager().getScoreboardManager().isArenaBoardActive()) {
            return;
        }

        plugin.getScoreboardController().setBoard(player, ScoreboardController.BoardType.TEAM);

        if (sidebar == null || !scoreboard.getObjectives().contains(sidebar)) {
            enableTeamScoreboard();
        }

        playerTeams.put(player.getName(), team);
        saveTeamToFile(player.getName(), team);
        broadcastTeamChange(player, team);

        ChatColor teamColor = team.equalsIgnoreCase("Blau") ? ChatColor.BLUE : ChatColor.RED;
        player.sendTitle(teamColor + "✅ OK", teamColor + "Du bist jetzt im Team " + team, 10, 70, 20);

        if (team.equalsIgnoreCase("Rot")) {
            teamRot.addEntry(player.getName());
        } else if (team.equalsIgnoreCase("Blau")) {
            teamBlau.addEntry(player.getName());
        }

        if (!plugin.getArenaManager().getScoreboardManager().isArenaBoardActive()) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (isArenaWorld(online)) {
                    online.setScoreboard(scoreboard);
                }
            }
        }

        updateSidebar();
        plugin.bundleManager.giveTeamBundle(player);
    }

    private void updateSidebar() {
        if (sidebar == null) return;

        Scoreboard sb = sidebar.getScoreboard();
        if (sb == null) return;

        for (String entry : new HashSet<>(sb.getEntries())) {
            sb.resetScores(entry);
        }

        int score = 15;
        int empty = 0;

        sidebar.getScore(emptyLine(empty++)).setScore(score--);

        sidebar.getScore(ChatColor.RED + "Team Rot:").setScore(score--);

        for (String name : teamRot.getEntries()) {
            sidebar.getScore(ChatColor.GREEN + "- " + name)
                    .setScore(score--);
        }

        sidebar.getScore(emptyLine(empty++)).setScore(score--);

        sidebar.getScore(ChatColor.BLUE + "Team Blau:").setScore(score--);

        for (String name : teamBlau.getEntries()) {
            sidebar.getScore(ChatColor.GREEN + "- " + name)
                    .setScore(score--);
        }
        sidebar.getScore(emptyLine(empty++)).setScore(score--);
    }

    private String emptyLine(int i) {
        return ChatColor.values()[i].toString();
    }

    private void saveTeamToFile(String playerName, String team) {
        File teamsFile = new File(plugin.getDataFolder(), "teams.yml");

        if (!teamsFile.exists()) {
            try { teamsFile.createNewFile(); } catch (IOException ignored) {}
        }

        YamlConfiguration teamsConfig = YamlConfiguration.loadConfiguration(teamsFile);
        teamsConfig.set("teams." + playerName, team);

        try { teamsConfig.save(teamsFile); } catch (IOException ignored) {}
    }

    public void loadTeams() {

        if (teamRot == null || teamBlau == null || scoreboard == null) {
            enableTeamScoreboard();
        }

        File teamsFile = new File(plugin.getDataFolder(), "teams.yml");
        if (!teamsFile.exists()) return;

        YamlConfiguration teamsConfig = YamlConfiguration.loadConfiguration(teamsFile);
        playerTeams.clear();

        if (!teamsConfig.contains("teams")) return;

        for (String playerName : teamsConfig.getConfigurationSection("teams").getKeys(false)) {

            String team = teamsConfig.getString("teams." + playerName, "Kein Team");
            playerTeams.put(playerName, team);

            if (team.equalsIgnoreCase("Rot")) {
                teamRot.addEntry(playerName);
            } else if (team.equalsIgnoreCase("Blau")) {
                teamBlau.addEntry(playerName);
            }

            Player online = Bukkit.getPlayer(playerName);
            if (online != null && isArenaWorld(online)) {
                online.setScoreboard(scoreboard);
                plugin.getScoreboardController()
                        .setBoard(online, ScoreboardController.BoardType.TEAM);
            }
        }

        if (!plugin.getArenaManager().getScoreboardManager().isArenaBoardActive()) {
            updateSidebar();
        }
    }

    private boolean hasSavedTeams() {
        File f = new File(plugin.getDataFolder(), "teams.yml");
        if (!f.exists()) return false;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        return cfg.contains("teams") && !cfg.getConfigurationSection("teams").getKeys(false).isEmpty();
    }

    public String getPlayerTeam(Player player) {
        return playerTeams.getOrDefault(player.getName(), "Kein Team");
    }

    public String getPlayerTeam(OfflinePlayer offlinePlayer) {
        return playerTeams.getOrDefault(offlinePlayer.getName(), "Kein Team");
    }

    public boolean isInTeam(Player player) {
        return !getPlayerTeam(player).equalsIgnoreCase("Kein Team");
    }

    public void resetTeams() {
        playerTeams.clear();
        saveTeamsToFile();

        if (teamRot != null) {
            for (String entry : new HashSet<>(teamRot.getEntries())) teamRot.removeEntry(entry);
        }
        if (teamBlau != null) {
            for (String entry : new HashSet<>(teamBlau.getEntries())) teamBlau.removeEntry(entry);
        }

        if (!plugin.getArenaManager().getScoreboardManager().isArenaBoardActive()) {
            updateSidebar();
        }
    }

    public void waitForAllTeamsAndStart() {
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean allHaveTeam = Bukkit.getOnlinePlayers().stream()
                        .allMatch(p -> !getPlayerTeam(p).equalsIgnoreCase("Kein Team"));

                if (allHaveTeam) {
                    cancel();
                    MobArmyMain.getInstance().getEventManager().startCountdown();
                }
            }
        }.runTaskTimer(MobArmyMain.getInstance(), 40L, 40L);
    }

    public void removePlayerFromTeam(Player player) {

        plugin.bundleManager.removeTeamBundle(player);

        playerTeams.remove(player.getName());
        saveTeamToFile(player.getName(), "Kein Team");
        broadcastTeamChange(player, "Kein Team");

        teamRot.removeEntry(player.getName());
        teamBlau.removeEntry(player.getName());

        if (!plugin.getArenaManager().getScoreboardManager().isArenaBoardActive()) {
            updateSidebar();
        }
    }

    public void enableTeamScoreboard() {
        ArenaManager arena = plugin.getArenaManager();
        if (arena == null) {
            throw new IllegalStateException("ArenaManager ist noch nicht initialisiert!");
        }

        if (plugin.getArenaManager().getScoreboardManager().isArenaBoardActive()) {
            return;
        }

        if (scoreboard == null) {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            scoreboard = manager.getNewScoreboard();

            teamRot = scoreboard.registerNewTeam("Rot");
            teamRot.setDisplayName(ChatColor.RED + "Team Rot");
            teamRot.setColor(ChatColor.RED);

            teamBlau = scoreboard.registerNewTeam("Blau");
            teamBlau.setDisplayName(ChatColor.BLUE + "Team Blau");
            teamBlau.setColor(ChatColor.BLUE);

            sidebar = scoreboard.registerNewObjective(
                    "teams",
                    "dummy",
                    ChatColor.GOLD + "Team Übersicht"
            );
            sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (isArenaWorld(online)) {
                online.setScoreboard(scoreboard);
            }
        }

        updateSidebar();
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public void disableTeamScoreboard() {
        if (sidebar != null) sidebar.unregister();

        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard empty = Bukkit.getScoreboardManager().getNewScoreboard();
            empty.clearSlot(DisplaySlot.SIDEBAR);
            p.setScoreboard(empty);
        }

        scoreboard = null;
        teamRot = null;
        teamBlau = null;
        sidebar = null;
    }

    private void saveTeamsToFile() {
        File teamsFile = new File(plugin.getDataFolder(), "teams.yml");
        if (!teamsFile.exists()) return;

        YamlConfiguration teamsConfig = YamlConfiguration.loadConfiguration(teamsFile);
        teamsConfig.set("teams", null);
        try { teamsConfig.save(teamsFile); } catch (IOException ignored) {}
    }

    public boolean isArenaWorld(Player player) {
        String name = player.getWorld().getName().toLowerCase();
        return name.equals("world_mobarmylobby")
                || name.equals("world_rot")
                || name.equals("world_blau");
    }

    public Set<String> getAllTeamPlayers() {
        return new HashSet<>(playerTeams.keySet());
    }

    private void broadcastTeamChange(Player player, String team) {
        ChatColor color = team.equalsIgnoreCase("Blau") ? ChatColor.BLUE :
                team.equalsIgnoreCase("Rot")  ? ChatColor.RED  :
                        ChatColor.GRAY;

        Bukkit.getOnlinePlayers().forEach(p -> {
            if (isArenaWorld(p)) {
                p.playSound(
                        p.getLocation(),
                        Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                        1f, 1.4f
                );
            }
        });
    }
}