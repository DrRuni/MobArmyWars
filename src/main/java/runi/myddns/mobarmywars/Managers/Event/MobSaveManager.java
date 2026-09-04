package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MobSaveManager {

    private final MobArmyMain plugin;
    private final TeamManager teamManager;
    private TimerManager timerManager;

    private final Map<String, Map<String, Integer>> mobKills = new HashMap<>();
    private final File mobDataFile;
    private final Map<String, Map<String, Integer>> pendingKills = new HashMap<>();
    private final Map<String, BukkitTask> killFlushTasks = new HashMap<>();
    private YamlConfiguration mobData;

    public enum MobSaveMode {
        DISABLED, ENABLED
    }

    private MobSaveMode mobSaveMode = MobSaveMode.DISABLED;

    public MobSaveManager(MobArmyMain plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;

        this.mobDataFile = new File(plugin.getDataFolder(), "mobData.yml");

        this.mobData = YamlConfiguration.loadConfiguration(mobDataFile);
        loadSavedMobs();
    }

    public void setTimerManager(TimerManager timerManager) {
        this.timerManager = timerManager;
    }

    private NamedTextColor getTeamColor(String team) {
        return switch (team.toLowerCase(Locale.ROOT)) {
            case "rot" -> NamedTextColor.RED;
            case "blau" -> NamedTextColor.BLUE;
            default -> NamedTextColor.GRAY;
        };
    }

    public void handleMobKill(Player player, LivingEntity mob) {

        if (timerManager == null) return;
        if (!timerManager.isRunning()) return;
        if (mobSaveMode != MobSaveMode.ENABLED) return;

        String worldName =
                player.getWorld().getName().toLowerCase(Locale.ROOT);
        if (!worldName.equals("world_rot") && !worldName.equals("world_blau")) {
            return;
        }

        String teamName = teamManager.getPlayerTeam(player);
        if (teamName == null || teamName.equalsIgnoreCase("Kein Team")) return;


        String mobType = mob.getType().name();
        if (mob instanceof Ageable ageable) {
            boolean isBaby = !ageable.isAdult();
            mobType = (isBaby ? "BABY_" : "ADULT_") + mobType;
        }

        mobKills
                .computeIfAbsent(teamName, _ -> new HashMap<>())
                .merge(mobType, 1, Integer::sum);

        saveSavedMobs();

        pendingKills
                .computeIfAbsent(teamName, _ -> new HashMap<>())
                .merge(mobType, 1, Integer::sum);

        if (!killFlushTasks.containsKey(teamName)) {

            BukkitTask task = Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> flushKillMessage(teamName),
                    20L
            );

            killFlushTasks.put(teamName, task);
        }
    }

    private void flushKillMessage(String teamName) {

        Map<String, Integer> kills = pendingKills.remove(teamName);
        killFlushTasks.remove(teamName);

        if (kills == null || kills.isEmpty()) return;

        NamedTextColor teamColor = getTeamColor(teamName);

        for (Map.Entry<String, Integer> entry : kills.entrySet()) {

            String mobType = entry.getKey();
            int amount = entry.getValue();

            String mobName = mobType
                    .replace("BABY_", "")
                    .replace("ADULT_", "")
                    .toLowerCase(Locale.ROOT)
                    .replace("_", " ");

            mobName =
                    mobName.substring(0, 1).toUpperCase(Locale.ROOT)
                            + mobName.substring(1);

            Component message =
                    Component.text("+" + amount + " ", NamedTextColor.WHITE)
                            .append(Component.text(mobName, teamColor))
                            .append(
                                    Component.text(
                                            plugin.getLanguageManager().get(
                                                    "mob-save-manager.kill-suffix"
                                            )
                                    )
                            );

            for (Player p : Bukkit.getOnlinePlayers()) {
                String pTeam = teamManager.getPlayerTeam(p);
                if (teamName.equalsIgnoreCase(pTeam)) {
                    p.sendMessage(message);
                }
            }
        }
    }

    public void saveSavedMobs() {
        mobData.set("mobs", null);
        for (var entry : mobKills.entrySet()) {
            mobData.set("mobs." + entry.getKey(), entry.getValue());
        }
        saveFile();
    }

    private void saveFile() {
        try {
            mobData.save(mobDataFile);
        } catch (IOException e) {
            plugin.getLogger().log(
                    java.util.logging.Level.SEVERE,
                    "Failed to save mobData.yml.",
                    e
            );
        }
    }

    public void loadSavedMobs() {
        if (!mobDataFile.exists()) return;

        mobData =
                YamlConfiguration.loadConfiguration(mobDataFile);

        ConfigurationSection section =
                mobData.getConfigurationSection("mobs");

        if (section == null) {
            return;
        }

        for (String team : section.getKeys(false)) {

            ConfigurationSection teamSec =
                    section.getConfigurationSection(team);

            if (teamSec == null) {
                continue;
            }

            Map<String, Integer> map = new HashMap<>();

            for (String mob : teamSec.getKeys(false)) {
                map.put(
                        mob,
                        teamSec.getInt(mob)
                );
            }

            mobKills.put(team, map);
        }
    }

    public void setMobSaveMode(MobSaveMode mode) {
        this.mobSaveMode = mode;
    }

    public void clearAllMobData() {
        mobKills.clear();
        mobData.set("mobs", null);
        saveFile();
    }

    public int getMobCount(String team, String mobType) {
        return mobKills.getOrDefault(team, Collections.emptyMap())
                .getOrDefault(mobType, 0);
    }

    public Map<String, Integer> getMobKillsForTeam(String teamName) {
        return mobKills.getOrDefault(teamName, Collections.emptyMap());
    }

    public void consumeMob(String team, String mobType, int amount) {
        Map<String, Integer> teamData =
                mobKills.computeIfAbsent(team, _ -> new HashMap<>());

        int current = teamData.getOrDefault(mobType, 0);
        int newAmount = Math.max(0, current - amount);

        teamData.put(mobType, newAmount);
        saveSavedMobs();
    }

    public void restoreMob(String team, String mobType, int amount) {
        Map<String, Integer> teamData =
                mobKills.computeIfAbsent(team, _ -> new HashMap<>());

        teamData.put(
                mobType,
                teamData.getOrDefault(mobType, 0) + amount
        );

        saveSavedMobs();
    }

    public Set<String> getAllKnownMobTypes(String team) {
        return mobKills
                .getOrDefault(team, Collections.emptyMap())
                .keySet();
    }
}
