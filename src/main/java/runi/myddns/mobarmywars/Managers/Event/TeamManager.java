package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
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

    public TeamManager(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void assignTeam(Player player, String team) {


        playerTeams.put(player.getName(), team);
        saveTeamToFile(player.getName(), team);
        plugin.getTeamScoreboardManager().addPlayerToTeam(player, team);
        broadcastTeamChange(player, team);

        ChatColor teamColor = team.equalsIgnoreCase("Blau") ? ChatColor.BLUE : ChatColor.RED;
        player.sendTitle(teamColor + "✅ OK", teamColor + "Du bist jetzt im Team " + team, 10, 70, 20);

        plugin.bundleManager.giveTeamBundle(player);
    }


    public void loadTeams() {

        File teamsFile = new File(plugin.getDataFolder(), "teams.yml");
        if (!teamsFile.exists()) return;

        YamlConfiguration teamsConfig = YamlConfiguration.loadConfiguration(teamsFile);
        playerTeams.clear();

        if (!teamsConfig.contains("teams")) return;

        for (String playerName : teamsConfig.getConfigurationSection("teams").getKeys(false)) {

            String team = teamsConfig.getString("teams." + playerName, "Kein Team");
            playerTeams.put(playerName, team);
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
        plugin.getTeamScoreboardManager().rebuildBoard();
        deleteAllTeamsFromFile();
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
        plugin.getTeamScoreboardManager().removePlayer(player);
        broadcastTeamChange(player, "Kein Team");
    }

    private void saveTeamToFile(String playerName, String team) {
        File teamsFile = new File(plugin.getDataFolder(), "teams.yml");

        if (!teamsFile.exists()) {
            try {
                teamsFile.createNewFile();
            } catch (IOException ignored) {}
        }

        YamlConfiguration teamsConfig = YamlConfiguration.loadConfiguration(teamsFile);
        teamsConfig.set("teams." + playerName, team);

        try {
            teamsConfig.save(teamsFile);
        } catch (IOException ignored) {}
    }

    private void deleteAllTeamsFromFile() {
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