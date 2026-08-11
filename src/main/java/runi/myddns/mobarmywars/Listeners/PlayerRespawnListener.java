package runi.myddns.mobarmywars.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import runi.myddns.mobarmywars.Managers.World.ResumeManager;
import runi.myddns.mobarmywars.MobArmyMain;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerRespawnListener implements Listener {

    private final MobArmyMain plugin;

    public PlayerRespawnListener(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getPlayerEffectManager().applyNightVision(player);
        });

        Location deathLocation = player.getLocation();
        World deathWorld = deathLocation.getWorld();

        if (deathWorld == null) return;

        int phase = plugin.getEventResume().loadPhase();

        // =====================================================
        // PHASE 0 = LOBBY
        // Respawn aus plugins/MobArmyWars/spawns.yml -> lobbyspawn
        // Alte gespeicherte Event-Spawns werden in Phase 0 ignoriert.
        // =====================================================
        if (phase == ResumeManager.PHASE_LOBBY) {
            Location lobbySpawn = getLobbySpawnFromFile();

            if (lobbySpawn != null) {
                e.setRespawnLocation(lobbySpawn);
            }

            return;
        }

        String team = plugin.getTeamManager().getPlayerTeam(player);
        String teamWorldName = getTeamWorldName(team);

        if (phase >= ResumeManager.PHASE_TEAMWELT && phase <= ResumeManager.PHASE_ARENA) {
            if (team == null || team.equalsIgnoreCase("Kein Team")
                    || (!team.equalsIgnoreCase("Rot") && !team.equalsIgnoreCase("Blau"))) {

                Location lobbySpawn = getLobbySpawnFromFile();
                if (lobbySpawn != null) {
                    e.setRespawnLocation(lobbySpawn);
                }
                return;
            }
        }

        // =====================================================
        // PHASE 1 = TEAMWELT
        // Nether: Respawnanker geht vor.
        // Danach Bett in eigener Teamwelt.
        // Danach Spawnpunkt der eigenen Teamwelt.
        // Gespeicherte Event-Spawns werden ignoriert.
        // =====================================================
        if (phase == ResumeManager.PHASE_TEAMWELT) {

            // Wenn der Spieler im Nether stirbt und Minecraft/Bukkit
            // bereits einen gültigen Nether-Respawn gesetzt hat,
            // z.B. Respawnanker, dann lassen wir diesen Respawn unverändert.
            if (deathWorld.getEnvironment() == World.Environment.NETHER) {
                Location vanillaRespawn = e.getRespawnLocation();

                if (vanillaRespawn != null
                        && vanillaRespawn.getWorld() != null
                        && vanillaRespawn.getWorld().getEnvironment() == World.Environment.NETHER) {
                    return;
                }
            }

            Location teamBed = getTeamBedSpawn(player, teamWorldName);

            if (teamBed != null) {
                e.setRespawnLocation(teamBed);
                return;
            }

            Location teamWorldSpawn = getTeamWorldSpawn(teamWorldName);

            if (teamWorldSpawn != null) {
                e.setRespawnLocation(teamWorldSpawn);
                return;
            }

            Location lobbySpawn = getLobbySpawnFromFile();

            if (lobbySpawn != null) {
                e.setRespawnLocation(lobbySpawn);
            }

            return;
        }

        // =====================================================
        // PHASE 2 = WAVE-AUSWAHL
        // Respawn am Team-Spawn der Wave-Auswahl aus spawns.yml.
        // =====================================================
        if (phase == ResumeManager.PHASE_WAVEAUSWAHL) {
            Location waveSpawn = getWaveAuswahlTeamSpawnFromFile(player);

            if (waveSpawn != null) {
                e.setRespawnLocation(waveSpawn);
            } else {
                Location lobbySpawn = getLobbySpawnFromFile();
                if (lobbySpawn != null) {
                    e.setRespawnLocation(lobbySpawn);
                }
            }

            giveArenaCompassAfterRespawn(player);
            return;
        }

        // =====================================================
        // PHASE 3 = ARENA
        // Erst Bett in der Arena-Welt, sonst Teamspawn der Arena.
        // Arena-Kompass wird nach Respawn neu gegeben, wenn aktiv.
        // =====================================================
        if (phase == ResumeManager.PHASE_ARENA) {
            Location arenaBed = getArenaBedSpawn(player);

            if (arenaBed != null) {
                e.setRespawnLocation(arenaBed);
                giveArenaCompassAfterRespawn(player);
                return;
            }

            Location arenaSpawn = getArenaTeamSpawnFromFile(player);

            if (arenaSpawn != null) {
                e.setRespawnLocation(arenaSpawn);
            } else {
                Location lobbySpawn = getLobbySpawnFromFile();
                if (lobbySpawn != null) {
                    e.setRespawnLocation(lobbySpawn);
                }
            }

            giveArenaCompassAfterRespawn(player);
            return;
        }

        // =====================================================
        // UNBEKANNTE PHASE = FALLBACK LOBBY
        // =====================================================
        Location lobbySpawn = getLobbySpawnFromFile();

        if (lobbySpawn != null) {
            e.setRespawnLocation(lobbySpawn);
        }

        return;
    }

    @EventHandler
    public void onTotemPop(EntityResurrectEvent event) {

        if (event.isCancelled()) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            plugin.getPlayerEffectManager().applyNightVision(player);

        }, 1L);
    }

    @EventHandler
    public void onMilkDrink(PlayerItemConsumeEvent event) {

        if (event.getItem().getType() != Material.MILK_BUCKET) {
            return;
        }

        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            plugin.getPlayerEffectManager().applyNightVision(player);

        }, 2L);
    }

    private Location getLobbySpawnFromFile() {
        File file = new File(plugin.getDataFolder(), "spawns.yml");

        if (!file.exists()) {
            plugin.getLogger().warning("spawns.yml wurde nicht gefunden!");
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection("lobbyspawn");
        if (section == null) {
            plugin.getLogger().warning("lobbyspawn wurde in spawns.yml nicht gefunden!");
            return null;
        }

        String worldName = section.getString("world");
        if (worldName == null) {
            plugin.getLogger().warning("lobbyspawn.world fehlt in spawns.yml!");
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Lobby-Welt '" + worldName + "' wurde nicht gefunden!");
            return null;
        }

        ConfigurationSection min = section.getConfigurationSection("min");
        ConfigurationSection max = section.getConfigurationSection("max");

        if (min == null || max == null) {
            plugin.getLogger().warning("lobbyspawn.min oder lobbyspawn.max fehlt in spawns.yml!");
            return null;
        }

        double x = random(min.getDouble("x"), max.getDouble("x"));
        double y = random(min.getDouble("y"), max.getDouble("y"));
        double z = random(min.getDouble("z"), max.getDouble("z"));

        float yaw = (float) section.getDouble("yaw", 0.0);
        float pitch = (float) section.getDouble("pitch", 0.0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    private Location getWaveAuswahlTeamSpawnFromFile(Player player) {
        File file = new File(plugin.getDataFolder(), "spawns.yml");

        if (!file.exists()) {
            plugin.getLogger().warning("spawns.yml wurde nicht gefunden!");
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection("wave-auswahl");
        if (section == null) {
            plugin.getLogger().warning("wave-auswahl wurde in spawns.yml nicht gefunden!");
            return null;
        }

        String worldName = section.getString("world");
        if (worldName == null) {
            plugin.getLogger().warning("wave-auswahl.world fehlt in spawns.yml!");
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Wave-Auswahl-Welt '" + worldName + "' wurde nicht gefunden!");
            return null;
        }

        String team = plugin.getTeamManager().getPlayerTeam(player);
        if (team == null) {
            plugin.getLogger().warning("Spieler " + player.getName() + " hat kein Team für Wave-Auswahl-Respawn.");
            return null;
        }

        team = team.toLowerCase();

        if (!team.equals("rot") && !team.equals("blau")) {
            plugin.getLogger().warning("Ungültiges Team für Wave-Auswahl-Respawn: " + team);
            return null;
        }

        java.util.List<Double> spawn = section.getDoubleList(team + ".teamspawn");

        if (spawn.size() < 5) {
            plugin.getLogger().warning("wave-auswahl." + team + ".teamspawn fehlt oder ist ungültig!");
            return null;
        }

        double x = spawn.get(0);
        double y = spawn.get(1);
        double z = spawn.get(2);
        float yaw = spawn.get(3).floatValue();
        float pitch = spawn.get(4).floatValue();

        return new Location(world, x, y, z, yaw, pitch);
    }

    private Location getArenaTeamSpawnFromFile(Player player) {

        String team = plugin.getTeamManager().getPlayerTeam(player);

        if (team == null) {
            plugin.getLogger().warning(
                    "Spieler " + player.getName()
                            + " hat kein Team für Arena-Respawn."
            );
            return null;
        }

        if (!team.equalsIgnoreCase("rot")
                && !team.equalsIgnoreCase("blau")) {

            plugin.getLogger().warning(
                    "Ungültiges Team für Arena-Respawn: " + team
            );
            return null;
        }

        Location spawn = plugin.getArenaConfig().getTeamSpawn(team);

        if (spawn == null) {
            plugin.getLogger().warning(
                    "Kein Teamspawn für Team " + team
                            + " in der aktiven Arena gefunden!"
            );
            return null;
        }

        return spawn.clone();
    }

    private void giveArenaCompassAfterRespawn(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            if (!plugin.getWorldSettings().isArenaCompassEnabled()) {
                return;
            }

            plugin.getArenaCompassManager().giveMonsterCompass(player);
        }, 2L);
    }

    private double random(double min, double max) {
        if (min == max) return min;

        return ThreadLocalRandom.current().nextDouble(
                Math.min(min, max),
                Math.max(min, max)
        );
    }

    private String getTeamWorldName(String team) {
        if ("rot".equalsIgnoreCase(team)) {
            return "world_rot";
        } else if ("blau".equalsIgnoreCase(team)) {
            return "world_blau";
        }
        return null;
    }

    private Location getTeamBedSpawn(Player player, String teamWorldName) {
        if (teamWorldName == null) return null;

        Location bed = player.getBedSpawnLocation();
        if (bed == null || bed.getWorld() == null) return null;

        if (bed.getWorld().getName().equalsIgnoreCase(teamWorldName)) {
            return bed;
        }

        return null;
    }

    private Location getArenaBedSpawn(Player player) {
        Location bed = player.getBedSpawnLocation();

        if (bed == null || bed.getWorld() == null) {
            return null;
        }

        if (!bed.getWorld().getName().equalsIgnoreCase("world_mobarmy_arena")) {
            return null;
        }

        return bed;
    }

    private Location getTeamWorldSpawn(String teamWorldName) {
        if (teamWorldName == null) return null;

        World teamWorld = Bukkit.getWorld(teamWorldName);
        if (teamWorld == null) return null;

        return teamWorld.getSpawnLocation();
    }
}