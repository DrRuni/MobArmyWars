package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.configuration.file.YamlConfiguration;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WaveStorage {

    private final MobArmyMain plugin;
    private final File file;
    private final YamlConfiguration config;
    private final WaveManager waveManager;

    public WaveStorage(
            MobArmyMain plugin,
            File dataFolder,
            WaveManager waveManager
    ) {
        this.plugin = plugin;
        this.file = new File(dataFolder, "waves.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        this.waveManager = waveManager;
    }

    public void saveWaves() {
        config.set("waves", null);

        for (String team : waveManager.getAllTeams()) {
            List<List<WaveManager.WaveEntry>> waves = waveManager.getAllWaves(team);

            for (int i = 0; i < waves.size(); i++) {
                List<WaveManager.WaveEntry> wave = waves.get(i);
                List<Map<String, Object>> savedEntries = new ArrayList<>();

                for (WaveManager.WaveEntry entry : wave) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("type", entry.getMobType());
                    map.put("amount", entry.getAmount());
                    savedEntries.add(map);
                }

                config.set("waves." + team + ".wave" + i, savedEntries);
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(
                    java.util.logging.Level.SEVERE,
                    "Failed to save waves.yml.",
                    e
            );
        }
    }

    public void loadWaves() {
        if (!config.contains("waves")) return;
        var wavesSection =
                config.getConfigurationSection("waves");

        if (wavesSection == null) {
            return;
        }

        for (String team : wavesSection.getKeys(false)) {
            waveManager.initTeam(team);

            for (int i = 0; i < ArenaEventManager.MAX_WAVES; i++) {
                String path = "waves." + team + ".wave" + i;
                List<WaveManager.WaveEntry> wave = new ArrayList<>();

                if (config.isList(path)) {
                    List<Map<?, ?>> savedEntries = config.getMapList(path);

                    for (Map<?, ?> map : savedEntries) {
                        Object typeObj = map.get("type");
                        Object amountObj = map.get("amount");

                        if (typeObj == null || amountObj == null) continue;

                        String mobType = String.valueOf(typeObj);
                        int amount;

                        if (amountObj instanceof Number number) {
                            amount = number.intValue();
                        } else {
                            try {
                                amount = Integer.parseInt(String.valueOf(amountObj));
                            } catch (NumberFormatException e) {
                                continue;
                            }
                        }

                        if (amount <= 0) continue;

                        wave.add(new WaveManager.WaveEntry(mobType, amount));
                    }
                }

                waveManager.setWave(team, i, wave);
            }
        }
    }
}