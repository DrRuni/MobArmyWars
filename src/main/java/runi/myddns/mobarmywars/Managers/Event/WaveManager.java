package runi.myddns.mobarmywars.Managers.Event;

import java.util.*;

public class WaveManager {

    private final MobSaveManager mobSaveManager;
    private ArenaScoreboardManager scoreboardManager;

    private final Map<String, List<List<WaveEntry>>> teamWaves = new HashMap<>();

    public WaveManager(MobSaveManager mobSaveManager) {
        this.mobSaveManager = mobSaveManager;
    }

    public void setScoreboardManager(ArenaScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }

    public void initTeam(String teamName) {
        if (!teamWaves.containsKey(teamName)) {
            List<List<WaveEntry>> waves = new ArrayList<>();

            for (int i = 0; i < ArenaEventManager.MAX_WAVES; i++) {
                waves.add(new ArrayList<>());
            }

            teamWaves.put(teamName, waves);
        }
    }

    public List<WaveEntry> getWave(String teamName, int waveIndex) {
        initTeam(teamName);
        return teamWaves.get(teamName).get(waveIndex);
    }

    public List<List<WaveEntry>> getAllWaves(String teamName) {
        initTeam(teamName);
        return teamWaves.get(teamName);
    }

    public void clearWaves(String teamName) {
        initTeam(teamName);

        for (List<WaveEntry> wave : teamWaves.get(teamName)) {
            wave.clear();
        }
    }

    public void resetWaves(String teamName) {
        if (scoreboardManager != null) {
            scoreboardManager.resetKills();
        }

        clearWaves(teamName);
    }

    public Set<String> getAllTeams() {
        return teamWaves.keySet();
    }

    public void addMobToWave(String team, int waveIndex, String mobType) {

        List<WaveEntry> wave = getWave(team, waveIndex);

        if (!wave.isEmpty()) {
            WaveEntry last = wave.getLast();

            if (last.getMobType().equals(mobType)) {
                last.addAmount(1);
            } else {
                wave.add(new WaveEntry(mobType, 1));
            }
        } else {
            wave.add(new WaveEntry(mobType, 1));
        }

        mobSaveManager.consumeMob(team, mobType, 1);
    }

    public void removeMobFromWave(
            String team,
            int waveIndex,
            String mobType
    ) {
        List<WaveEntry> wave = getWave(team, waveIndex);

        for (int i = wave.size() - 1; i >= 0; i--) {
            WaveEntry entry = wave.get(i);

            if (!entry.getMobType().equals(mobType)) {
                continue;
            }

            entry.removeAmount(1);

            if (entry.getAmount() <= 0) {
                wave.remove(i);
            }

            mobSaveManager.restoreMob(team, mobType, 1);
            return;
        }
    }

    public int getMobAmountInWave(String team, int waveIndex, String mobType) {
        List<WaveEntry> wave = getWave(team, waveIndex);

        int total = 0;

        for (WaveEntry entry : wave) {
            if (entry.getMobType().equals(mobType)) {
                total += entry.getAmount();
            }
        }

        return total;
    }

    public Map<String, Integer> getWaveAsCountMap(String team, int waveIndex) {
        List<WaveEntry> wave = getWave(team, waveIndex);
        Map<String, Integer> result = new LinkedHashMap<>();

        for (WaveEntry entry : wave) {
            result.merge(entry.getMobType(), entry.getAmount(), Integer::sum);
        }

        return result;
    }

    public void setWave(String team, int waveIndex, List<WaveEntry> entries) {
        initTeam(team);
        teamWaves.get(team).set(waveIndex, entries);
    }

    public static class WaveEntry {

        private final String mobType;
        private int amount;

        public WaveEntry(String mobType, int amount) {
            this.mobType = mobType;
            this.amount = amount;
        }

        public String getMobType() {
            return mobType;
        }

        public int getAmount() {
            return amount;
        }

        public void addAmount(int value) {
            this.amount += value;
        }

        public void removeAmount(int value) {
            this.amount -= value;
        }
    }
}