package runi.myddns.mobarmywars.Managers.Event;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
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

        String oldTeam = getPlayerTeam(player);

        playerTeams.put(player.getName(), team);
        saveTeamToFile(player.getName(), team);

        plugin.getTeamScoreboardManager()
                .addPlayerToTeam(player, team);

        if (oldTeam != null
                && !oldTeam.equalsIgnoreCase("Kein Team")
                && !oldTeam.equalsIgnoreCase(team)) {

            playTeamLeaveSound(player, oldTeam);
        }

        playTeamJoinSound(player, team);
        showTeamTitle(player, team);

        plugin.getBundleManager()
                .giveTeamBundle(player);
    }

    public void loadTeams() {

        File teamsFile =
                new File(plugin.getDataFolder(), "teams.yml");

        if (!teamsFile.exists()) {
            return;
        }

        YamlConfiguration teamsConfig =
                YamlConfiguration.loadConfiguration(teamsFile);

        playerTeams.clear();

        ConfigurationSection teamsSection =
                teamsConfig.getConfigurationSection("teams");

        if (teamsSection == null) {
            return;
        }

        for (String playerName :
                teamsSection.getKeys(false)) {

            String team = teamsConfig.getString(
                    "teams." + playerName,
                    "Kein Team"
            );

            playerTeams.put(
                    playerName,
                    team
            );
        }
    }

    public String getPlayerTeam(Player player) {

        return playerTeams.getOrDefault(
                player.getName(),
                "Kein Team"
        );
    }

    public String getPlayerTeam(
            OfflinePlayer offlinePlayer
    ) {

        String name = offlinePlayer.getName();

        if (name == null) {
            return "Kein Team";
        }

        return playerTeams.getOrDefault(
                name,
                "Kein Team"
        );
    }

    public boolean isInTeam(Player player) {

        return !getPlayerTeam(player)
                .equalsIgnoreCase("Kein Team");
    }

    public void resetTeams() {

        playerTeams.clear();

        plugin.getTeamScoreboardManager()
                .rebuildBoard();

        deleteAllTeamsFromFile();
    }

    public void waitForAllTeamsAndStart() {

        new BukkitRunnable() {

            @Override
            public void run() {

                boolean allHaveTeam =
                        Bukkit.getOnlinePlayers()
                                .stream()
                                .noneMatch(player ->
                                        getPlayerTeam(player)
                                                .equalsIgnoreCase("Kein Team")
                                );

                if (allHaveTeam) {

                    cancel();

                    plugin.getEventManager()
                            .startEvent();
                }
            }

        }.runTaskTimer(
                plugin,
                40L,
                40L
        );
    }

    public void removePlayerFromTeam(Player player) {

        String oldTeam =
                getPlayerTeam(player);

        plugin.getBundleManager()
                .removeTeamBundle(player);

        playerTeams.remove(
                player.getName()
        );

        saveTeamToFile(
                player.getName(),
                "Kein Team"
        );

        plugin.getTeamScoreboardManager()
                .removePlayer(player);

        if (oldTeam != null
                && !oldTeam.equalsIgnoreCase("Kein Team")) {

            playTeamLeaveSound(
                    player,
                    oldTeam
            );
        }
    }

    private void saveTeamToFile(
            String playerName,
            String team
    ) {

        File teamsFile =
                new File(plugin.getDataFolder(), "teams.yml");

        YamlConfiguration teamsConfig =
                YamlConfiguration.loadConfiguration(teamsFile);

        teamsConfig.set(
                "teams." + playerName,
                team
        );

        try {
            teamsConfig.save(teamsFile);
        } catch (IOException ignored) {
        }
    }

    private void deleteAllTeamsFromFile() {

        File teamsFile =
                new File(plugin.getDataFolder(), "teams.yml");

        if (!teamsFile.exists()) {
            return;
        }

        YamlConfiguration teamsConfig =
                YamlConfiguration.loadConfiguration(teamsFile);

        teamsConfig.set(
                "teams",
                null
        );

        try {
            teamsConfig.save(teamsFile);
        } catch (IOException ignored) {
        }
    }

    private void showTeamTitle(
            Player player,
            String team
    ) {

        boolean blue =
                team.equalsIgnoreCase("Blau");

        Component title = blue
                ? lang("team-manager.joined.title-blue")
                : lang("team-manager.joined.title-red");

        Component subtitle = blue
                ? lang("team-manager.joined.subtitle-blue")
                : lang("team-manager.joined.subtitle-red");

        player.showTitle(
                Title.title(
                        title,
                        subtitle,
                        Title.Times.times(
                                Duration.ofMillis(500),
                                Duration.ofMillis(3500),
                                Duration.ofSeconds(1)
                        )
                )
        );
    }

    private void playTeamJoinSound(
            Player changedPlayer,
            String team
    ) {

        for (Player onlinePlayer :
                Bukkit.getOnlinePlayers()) {

            String onlineTeam =
                    getPlayerTeam(onlinePlayer);

            if (onlinePlayer.equals(changedPlayer)
                    || team.equalsIgnoreCase(onlineTeam)) {

                onlinePlayer.playSound(
                        onlinePlayer.getLocation(),
                        Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                        1f,
                        1.4f
                );
            }
        }
    }

    private void playTeamLeaveSound(
            Player changedPlayer,
            String oldTeam
    ) {

        for (Player onlinePlayer :
                Bukkit.getOnlinePlayers()) {

            String onlineTeam =
                    getPlayerTeam(onlinePlayer);

            if (onlinePlayer.equals(changedPlayer)
                    || oldTeam.equalsIgnoreCase(onlineTeam)) {

                onlinePlayer.playSound(
                        onlinePlayer.getLocation(),
                        Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO,
                        1.0f,
                        0.7f
                );
            }
        }
    }

    public Set<String> getAllTeamPlayers() {

        return new HashSet<>(
                playerTeams.keySet()
        );
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }
}