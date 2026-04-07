package runi.myddns.mobarmywars.Arena;

import runi.myddns.mobarmywars.Managers.Event.ArenaScoreboardManager;
import runi.myddns.mobarmywars.Managers.Event.MobSaveManager;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.*;

public class WaveManager {

    private final MobArmyMain plugin;
    private final MobSaveManager mobSaveManager;
    private ArenaScoreboardManager scoreboardManager;

    private final Map<String, List<Map<String, Integer>>> teamWaves = new HashMap<>();

    public WaveManager(MobArmyMain plugin, MobSaveManager mobSaveManager) {
        this.plugin = plugin;
        this.mobSaveManager = mobSaveManager;
    }

    public void setScoreboardManager(ArenaScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }

    public void initTeam(String teamName) {
        if (!teamWaves.containsKey(teamName)) {
            List<Map<String, Integer>> waves = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                waves.add(new HashMap<>());
            }
            teamWaves.put(teamName, waves);
        }
    }

    public Map<String, Integer> getWave(String teamName, int waveIndex) {
        initTeam(teamName);
        return teamWaves.get(teamName).get(waveIndex);
    }

    public List<Map<String, Integer>> getAllWaves(String teamName) {
        initTeam(teamName);
        return teamWaves.get(teamName);
    }

    public void clearWaves(String teamName) {
        initTeam(teamName);
        for (Map<String, Integer> wave : teamWaves.get(teamName)) {
            wave.clear();
        }
    }

    public void resetWaves(String teamName) {
        scoreboardManager.resetKills();
        initTeam(teamName);

        for (Map<String, Integer> wave : teamWaves.get(teamName)) {
            wave.clear();
        }
    }

    public Set<String> getAllTeams() {
        return teamWaves.keySet();
    }

    public void addMobToWave(String team, int waveIndex, String mobType) {
        Objects.requireNonNull(mobSaveManager, "MobSaveManager fehlt im WaveManager");
        List<Map<String, Integer>> waves = getAllWaves(team);
        Map<String, Integer> wave = waves.get(waveIndex);

        wave.merge(mobType, 1, Integer::sum);
        mobSaveManager.consumeMob(team, mobType, 1);
    }

    public boolean removeMobFromWave(String team, int waveIndex, String mobType) {
        List<Map<String, Integer>> waves = getAllWaves(team);
        Map<String, Integer> wave = waves.get(waveIndex);

        int current = wave.getOrDefault(mobType, 0);
        if (current > 0) {
            wave.put(mobType, current - 1);
            if (wave.get(mobType) == 0) {
                wave.remove(mobType);
            }

            mobSaveManager.restoreMob(team, mobType, 1);
            return true;
        }

        return false;
    }
}