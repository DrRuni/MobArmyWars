package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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
        this.timerManager = timerManager;

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();

        this.mobDataFile = new File(dataFolder, "mobData.yml");

        try {
            if (!mobDataFile.exists()) mobDataFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("❌ Konnte mobData.yml nicht erstellen!");
            e.printStackTrace();
        }

        this.mobData = YamlConfiguration.loadConfiguration(mobDataFile);
        loadSavedMobs();
    }

    public void setTimerManager(TimerManager timerManager) {
        this.timerManager = timerManager;
    }

    private ChatColor getTeamColor(String team) {
        return switch (team.toLowerCase()) {
            case "rot" -> ChatColor.RED;
            case "blau" -> ChatColor.BLUE;
            default -> ChatColor.GRAY;
        };
    }

    public void handleMobKill(Player player, LivingEntity mob) {

        if (timerManager == null) return;
        if (!timerManager.isRunning()) return;
        if (mobSaveMode != MobSaveMode.ENABLED) return;

        String worldName = player.getWorld().getName().toLowerCase();
        if (!worldName.equals("world_rot") && !worldName.equals("world_blau")) {
            return;
        }

        String teamName = teamManager.getPlayerTeam(player);
        if (teamName == null || teamName.equalsIgnoreCase("Kein Team")) return;


        String mobType = mob.getType().name();
        boolean isBaby = false;

        if (mob instanceof Ageable ageable) {
            isBaby = !ageable.isAdult();
            mobType = (isBaby ? "BABY_" : "ADULT_") + mobType;
        }

        mobKills
                .computeIfAbsent(teamName, k -> new HashMap<>())
                .merge(mobType, 1, Integer::sum);

        saveSavedMobs();

        pendingKills
                .computeIfAbsent(teamName, t -> new HashMap<>())
                .merge(mobType, 1, Integer::sum);

        if (!killFlushTasks.containsKey(teamName)) {

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                flushKillMessage(teamName);
            }, 20L);

            killFlushTasks.put(teamName, task);
        }
    }

    private void flushKillMessage(String teamName) {

        Map<String, Integer> kills = pendingKills.remove(teamName);
        killFlushTasks.remove(teamName);

        if (kills == null || kills.isEmpty()) return;

        ChatColor teamColor = getTeamColor(teamName);

        for (Map.Entry<String, Integer> entry : kills.entrySet()) {

            String mobType = entry.getKey();
            int amount = entry.getValue();

            String mobName = mobType
                    .replace("BABY_", "")
                    .replace("ADULT_", "")
                    .toLowerCase()
                    .replace("_", " ");

            mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1);

            String message =
                    ChatColor.WHITE + "+" + amount + " "
                            + teamColor + mobName + " getötet";

            for (Player p : Bukkit.getOnlinePlayers()) {
                String pTeam = teamManager.getPlayerTeam(p);
                if (teamName.equalsIgnoreCase(pTeam)) {
                    p.sendMessage(message);
                }
            }
        }
    }

    public void spawnSavedMobs(Player player) {

        String teamName = teamManager.getPlayerTeam(player);
        if (!mobKills.containsKey(teamName) || mobKills.get(teamName).isEmpty()) {
            player.sendMessage(ChatColor.RED + "Dein Team hat keine gespeicherten Mobs!");
            return;
        }

        World world = player.getWorld();
        Location base = player.getLocation();
        Random random = ThreadLocalRandom.current();

        Map<String, Integer> saved = new HashMap<>(mobKills.get(teamName));

        for (var entry : saved.entrySet()) {
            String mobTypeString = entry.getKey();
            boolean isBaby = mobTypeString.startsWith("BABY_");

            EntityType type = EntityType.valueOf(
                    mobTypeString.replace("BABY_", "").replace("ADULT_", "")
            );

            for (int i = 0; i < entry.getValue(); i++) {
                Location loc = base.clone().add(
                        random.nextDouble(-10, 10),
                        0,
                        random.nextDouble(-10, 10)
                );
                loc.setY(world.getHighestBlockYAt(loc) + 1);

                LivingEntity entity = (LivingEntity) world.spawnEntity(loc, type);
                if (entity instanceof Ageable ageable) {
                    if (isBaby) ageable.setBaby();
                    else ageable.setAdult();
                }
            }
        }

        mobKills.remove(teamName);
        mobData.set("mobs." + teamName, null);
        saveFile();

        player.sendMessage(ChatColor.GREEN + "Alle gespeicherten Mobs wurden gespawnt!");
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
            e.printStackTrace();
        }
    }

    public void loadSavedMobs() {
        if (!mobDataFile.exists()) return;

        mobData = YamlConfiguration.loadConfiguration(mobDataFile);

        if (!mobData.contains("mobs")) return;

        MemorySection section = (MemorySection) mobData.get("mobs");
        for (String team : section.getKeys(false)) {
            MemorySection teamSec = (MemorySection) section.get(team);
            Map<String, Integer> map = new HashMap<>();

            for (String mob : teamSec.getKeys(false)) {
                map.put(mob, teamSec.getInt(mob));
            }
            mobKills.put(team, map);
        }
    }

    public void setMobSaveMode(MobSaveMode mode) {
        this.mobSaveMode = mode;
    }

    public boolean isMobSaveEnabled() {
        return mobSaveMode == MobSaveMode.ENABLED;
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
                mobKills.computeIfAbsent(team, k -> new HashMap<>());

        int current = teamData.getOrDefault(mobType, 0);
        int newAmount = Math.max(0, current - amount);

        teamData.put(mobType, newAmount);
        saveSavedMobs();
    }

    public void restoreMob(String team, String mobType, int amount) {
        Map<String, Integer> teamData =
                mobKills.computeIfAbsent(team, k -> new HashMap<>());

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
