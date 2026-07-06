package runi.myddns.mobarmywars.Arena;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaveStorage {

    private final File file;
    private final YamlConfiguration config;
    private final WaveManager waveManager;

    public WaveStorage(File dataFolder, WaveManager waveManager) {
        this.file = new File(dataFolder, "waves.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        this.waveManager = waveManager;
    }

    public void saveWaves() {
        config.set("waves", null);

        for (String team : waveManager.getAllTeams()) {
            List<Map<String, Integer>> waves = waveManager.getAllWaves(team);
            for (int i = 0; i < waves.size(); i++) {
                Map<String, Integer> wave = waves.get(i);
                for (Map.Entry<String, Integer> entry : wave.entrySet()) {
                    config.set("waves." + team + ".wave" + i + "." + entry.getKey(), entry.getValue());
                }
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadWaves() {
        if (!config.contains("waves")) return;

        for (String team : config.getConfigurationSection("waves").getKeys(false)) {
            waveManager.initTeam(team);

            List<Map<String, Integer>> waves = waveManager.getAllWaves(team);

            for (int i = 0; i < waves.size(); i++) {
                String path = "waves." + team + ".wave" + i;
                Map<String, Integer> wave = new HashMap<>();

                if (config.isConfigurationSection(path)) {
                    for (String mob : config.getConfigurationSection(path).getKeys(false)) {
                        wave.put(mob, config.getInt(path + "." + mob));
                    }
                }

                waves.set(i, wave);
            }
        }
    }
}

