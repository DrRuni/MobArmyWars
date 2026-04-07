package runi.myddns.mobarmywars.Arena;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import runi.myddns.mobarmywars.Managers.Event.ArenaScoreboardManager;
import runi.myddns.mobarmywars.Managers.Event.BlockRandomizerManager;
import runi.myddns.mobarmywars.Managers.World.ResumeManager;
import runi.myddns.mobarmywars.Managers.World.TeleportManager;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.*;
import java.util.List;

public class ArenaManager implements Listener {

    private final MobArmyMain plugin;
    private BlockRandomizerManager randomizer;

    private final Map<String, Integer> currentWave = new HashMap<>();
    private final Map<String, List<LivingEntity>> activeMobs = new HashMap<>();
    private final Set<String> finishedTeams = new HashSet<>();
    private final Map<String, Long> finishTimes = new HashMap<>();
    private final Map<String, Boolean> waveRunning = new HashMap<>();
    private final Set<Chunk> forcedChunks = new HashSet<>();
    private final Set<UUID> playersReady = new HashSet<>();
    private final Map<String, BossBar> teamBossBars = new HashMap<>();
    private final ArenaScoreboardManager scoreboardManager;
    private boolean arenaStarted = false;
    private boolean arenaRunning = false;
    public static final int MAX_WAVES = 3;
    private String winningTeam = null;
    private long winningTime = -1;
    private int aggroTaskId = -1;
    public int getCurrentWaveForTeam(String team) {return currentWave.getOrDefault(team, 0) + 1;}
    public int getMaxWaves() {return MAX_WAVES;}

    private static final Set<EntityType> FLYING_MOBS = Set.of(
            EntityType.GHAST,
            EntityType.PHANTOM,
            EntityType.WITHER,
            EntityType.ENDER_DRAGON,
            EntityType.BAT,
            EntityType.PARROT,
            EntityType.BEE
    );

    public ArenaManager(MobArmyMain plugin) {
        this.plugin = plugin;
        this.scoreboardManager = new ArenaScoreboardManager(plugin, this);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private final Set<Integer> activeTasks = new HashSet<>();

    public void registerTask(int taskId) {
        activeTasks.add(taskId);
    }

    public void stopAllArenaTasks() {
        for (int id : activeTasks) {
            Bukkit.getScheduler().cancelTask(id);
        }
        activeTasks.clear();
    }

    public MobArmyMain getPlugin() {
        return plugin;
    }

    public void teleportTeamsToArena() {

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getTeamManager().isInTeam(player)) continue;

            plugin.getEventResume().savePlayerSpawn(player, player.getLocation());

            TeleportManager.teleportToArena(player);
            plugin.getScoreboardSwitcher().switchToArena(player);

            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);
        }

        plugin.getEventResume().savePhase(ResumeManager.PHASE_ARENA);
    }

    public void teleportAndStart() {

        if (randomizer != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                randomizer.disableRandomizer(p);
            }
        }

        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (countdown > 0) {
                    String msg = ChatColor.GOLD + "🏁 MobArmyWars startet in " + ChatColor.RED + countdown;

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(msg);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                    }

                    countdown--;
                } else {
                    cancel();

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(ChatColor.GREEN + "⚔ Das MobArmyWars beginnt!");
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    }

                    startWaveBattle();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void startWaveBattle() {

        if (arenaRunning) {
            return;
        }

        arenaRunning = true;
        plugin.setArenaRunning(true);

        WaveManager waveManager = plugin.getWaveManager();

        if (waveManager == null) {
            Bukkit.getLogger().warning("[ArenaManager] ❌ WaveManager ist null! Abbruch von startWaveBattle().");
            return;
        }

        World arenaWorld = Bukkit.getWorld("world_mobarmylobby");
        if (arenaWorld == null) {
            Bukkit.getLogger().warning("❗ world_mobarmylobby konnte nicht gefunden werden – Wave abgebrochen!");
            return;
        }

        var abpm = plugin.getArenaBuildProtectionManager();
        var arena = abpm.getArena("japanisches-dorf");

        if (arena == null) {
            Bukkit.getLogger().warning("[MobArmyWars] ⚠ Arena 'japanisches-dorf' nicht gefunden!");
            return;
        }

        for (Chunk c : forcedChunks) {
            c.setForceLoaded(false);
        }
        forcedChunks.clear();

        for (Location loc : arena.rotMobSpawns()) {
            Chunk chunk = loc.getChunk();
            if (forcedChunks.add(chunk)) {
                chunk.setForceLoaded(true);
            }
        }
        for (Location loc : arena.blauMobSpawns()) {
            Chunk chunk = loc.getChunk();
            if (forcedChunks.add(chunk)) {
                chunk.setForceLoaded(true);
            }
        }

        scoreboardManager.setKillCount("Rot", 0);
        scoreboardManager.setKillCount("Blau", 0);
        scoreboardManager.updateAllArenaPlayers();

        currentWave.put("Rot", 0);
        currentWave.put("Blau", 0);

        waveRunning.put("Rot", true);
        waveRunning.put("Blau", true);

        spawnWave("Rot", "Blau", arena.rotMobSpawns().toArray(new Location[0]));
        spawnWave("Blau", "Rot", arena.blauMobSpawns().toArray(new Location[0]));

        startAggroTask();
    }

    private void spawnWave(String teamFighting, String waveOwner, Location[] spawnPoints) {
        int waveIndex = currentWave.get(teamFighting);

        if (!waveRunning.getOrDefault(waveOwner, false) && !finishedTeams.contains(waveOwner)) {
            Bukkit.getLogger().info("⏹ Wave-Spawning abgebrochen: waveOwner=" + waveOwner + " ist nicht aktiv.");
            return;
        }

        WaveManager waveManager = plugin.getWaveManager();

        Map<String, Integer> wave = waveManager.getWave(waveOwner, waveIndex);
        int finalWave = waveIndex + 1;

        if (wave == null || wave.isEmpty()) {
            handleEmptyWave(teamFighting, waveOwner, spawnPoints, finalWave);
            return;
        }

        announceWaveStart(teamFighting, finalWave);

        List<LivingEntity> spawned = new ArrayList<>();
        activeMobs.put(teamFighting, spawned);

        final int totalMobs = wave.values().stream().mapToInt(Integer::intValue).sum();

        BarColor color = teamFighting.equalsIgnoreCase("Rot") ? BarColor.RED : BarColor.BLUE;
        BossBar bar = Bukkit.createBossBar(
                "⚔ WAVE " + finalWave + " - Gegner verbleibend",
                color,
                BarStyle.SOLID
        );
        teamBossBars.put(teamFighting, bar);

        for (Player p : Bukkit.getOnlinePlayers()) {
            String team = plugin.getTeamManager().getPlayerTeam(p);
            if (team != null && team.equalsIgnoreCase(teamFighting)) {
                bar.addPlayer(p);
            }
        }

        bar.setProgress(1.0);

        spawnWaveMobs(teamFighting, waveOwner, spawnPoints, wave, spawned, totalMobs, bar);
    }

    private Player findNearestEnemy(LivingEntity mob, String enemyTeam) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> {
                    String team = plugin.getTeamManager().getPlayerTeam(p);
                    return team != null && team.equalsIgnoreCase(enemyTeam);
                })
                .min(Comparator.comparingDouble(
                        p -> p.getLocation().distanceSquared(mob.getLocation())
                ))
                .orElse(null);
    }

    private void spawnWaveMobs(String teamFighting, String waveOwner, Location[] spawnPoints,
                               Map<String, Integer> wave, List<LivingEntity> spawned,
                               int totalMobs, BossBar bar) {
        List<Map.Entry<String, Integer>> mobEntries = new ArrayList<>(wave.entrySet());
        int delay = 0;
        Random random = new Random();

        for (Map.Entry<String, Integer> entry : mobEntries) {
            String mobType = entry.getKey();
            int count = entry.getValue();
            boolean isBaby = mobType.startsWith("BABY_");
            EntityType type = EntityType.valueOf(mobType.replace("BABY_", "").replace("ADULT_", ""));

            for (int i = 0; i < count; i++) {
                Location loc = spawnPoints[random.nextInt(spawnPoints.length)];
                Location spawnLoc = loc.clone().add(random.nextDouble(-1, 1), 0, random.nextDouble(-1, 1));

                int finalDelay = delay;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location actualSpawn = spawnLoc.clone();

                        if (FLYING_MOBS.contains(type)) {
                            actualSpawn.add(0, 10 + random.nextInt(6), 0);
                        }

                        if (!actualSpawn.getBlock().isPassable()) {
                            actualSpawn.add(0, 5, 0);
                        }

                        LivingEntity mob = (LivingEntity) loc.getWorld().spawnEntity(actualSpawn, type);

                        mob.setRemoveWhenFarAway(false);
                        mob.setPersistent(true);
                        mob.setCanPickupItems(false);

                        if (mob instanceof Ageable ageable) {
                            if (isBaby) ageable.setBaby();
                            else ageable.setAdult();
                        }

                        mob.getPersistentDataContainer().set(
                                new NamespacedKey(plugin, "arenaMob"),
                                PersistentDataType.BYTE,
                                (byte) 1
                        );

                        String enemyTeam = waveOwner.equalsIgnoreCase("Rot") ? "Blau" : "Rot";
                        mob.getPersistentDataContainer().set(
                                new NamespacedKey(plugin, "enemyTeam"),
                                PersistentDataType.STRING,
                                enemyTeam
                        );

                        if (mob instanceof Bee bee) {

                            bee.setAnger(999999);
                            bee.setHasNectar(false);

                            Player nearestEnemy = findNearestEnemy(bee, enemyTeam);
                            if (nearestEnemy != null) {
                                bee.setTarget(nearestEnemy);
                            }
                        }

                        spawned.add(mob);
                    }
                }.runTaskLater(plugin, finalDelay);

                delay += 10;
            }
        }

        monitorWaveProgress(teamFighting, waveOwner, spawnPoints, spawned, totalMobs, bar, delay);
    }

    private void monitorWaveProgress(String teamFighting, String waveOwner, Location[] spawnPoints,
                                     List<LivingEntity> spawned, int totalMobs,
                                     BossBar bar, int delay) {
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                spawned.removeIf(e -> e == null || !e.isValid() || e.isDead());

                int alive = spawned.size();
                double progress = totalMobs > 0 ? (double) alive / totalMobs : 0;

                if (bar != null) {
                    bar.setProgress(Math.max(0.0, progress));
                }

                if (alive == 0) {
                    if (bar != null) {
                        bar.removeAll();
                        teamBossBars.remove(teamFighting);
                    }
                    cancel();

                    handleWaveCompletion(teamFighting, waveOwner, spawnPoints);
                }
            }
        }.runTaskTimer(plugin, delay + 20L, 40L).getTaskId();
        plugin.getArenaManager().registerTask(taskId);
    }

    private void handleEmptyWave(String teamFighting, String waveOwner, Location[] spawnPoints, int finalWave) {

        for (Player p : Bukkit.getOnlinePlayers()) {
            String team = plugin.getTeamManager().getPlayerTeam(p);
            if (team != null && team.equalsIgnoreCase(teamFighting)) {
                p.sendMessage(ChatColor.RED + "⚠ Wave " + finalWave + " ist leer.");
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Integer current = currentWave.get(teamFighting);
            if (current == null) return;

            int nextWave = current + 1;
            int finishedWave = current + 1;

            if (nextWave < 3) {

                currentWave.put(teamFighting, nextWave);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    String team = plugin.getTeamManager().getPlayerTeam(p);
                    if (team != null && team.equalsIgnoreCase(teamFighting)) {
                        p.sendTitle(
                                ChatColor.GOLD + "WAVE " + finishedWave + " abgeschlossen",
                                ChatColor.GRAY + "Nächste beginnt gleich...",
                                10, 40, 10
                        );
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    }
                }

                Bukkit.getScheduler().runTaskLater(
                        plugin,
                        () -> spawnWave(teamFighting, waveOwner, spawnPoints),
                        80L
                );

            } else {
                finishTeam(teamFighting);
            }

        }, 40L);
    }

    private void announceWaveStart(String teamFighting, int finalWave) {
        String waveColor = teamFighting.equalsIgnoreCase("Rot") ? ChatColor.RED.toString() : ChatColor.BLUE.toString();

        for (Player p : Bukkit.getOnlinePlayers()) {
            String team = plugin.getTeamManager().getPlayerTeam(p);
            if (team != null && team.equalsIgnoreCase(teamFighting)) {
                p.sendTitle(
                        waveColor + "⚔ WAVE " + finalWave,
                        ChatColor.GRAY + "Bereit machen!",
                        10, 40, 10
                );
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);}
        }
    }

    private void finishTeam(String team) {

        waveRunning.put(team, false);
        finishedTeams.add(team);

        long finishTime = System.currentTimeMillis();
        finishTimes.put(team, finishTime);

        if (winningTeam == null) {
            winningTeam = team;
            winningTime = finishTime;
            announceWinnerToTeam(team);
        } else {
            announceSecondPlace(team);
        }

        if (finishedTeams.size() == 2) {
            checkWinner();
        }
    }

    private void handleWaveCompletion(String teamFighting, String waveOwner, Location[] spawnPoints) {
        int nextWave = currentWave.get(teamFighting) + 1;
        int finishedWave = currentWave.get(teamFighting) + 1;

        if (nextWave < 3) {
            currentWave.put(teamFighting, nextWave);

            for (Player p : Bukkit.getOnlinePlayers()) {
                String team = plugin.getTeamManager().getPlayerTeam(p);
                if (team != null && team.equalsIgnoreCase(teamFighting)) {
                    p.sendTitle(
                            ChatColor.GOLD + "WAVE " + finishedWave + " abgeschlossen",
                            ChatColor.GRAY + "Nächste beginnt gleich...",
                            10, 40, 10
                    );
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }
            }

            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> spawnWave(teamFighting, waveOwner, spawnPoints), 80L);

        } else {
            finishTeam(teamFighting);
        }
    }

    private void announceWinnerToTeam(String team) {

        List<? extends Player> teamPlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> team.equalsIgnoreCase(plugin.getTeamManager().getPlayerTeam(p)))
                .toList();

        for (Player p : teamPlayers) {
            p.setGameMode(GameMode.SPECTATOR);
        }

        String msg = ChatColor.GOLD + "🏆 Team " + team +
                ChatColor.YELLOW + " hat gewonnen!";

        new BukkitRunnable() {
            int seconds = 0;

            @Override
            public void run() {
                if (seconds++ >= 10) {
                    cancel();
                    return;
                }

                for (Player p : teamPlayers) {
                    if (p.isOnline()) {
                        p.sendActionBar(msg);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }


    private void announceSecondPlace(String team) {

        long timeUsed = finishTimes.get(team) - winningTime;
        String time = formatMillis(timeUsed);

        String msg = ChatColor.GRAY + "🥈 Team " + team +
                " ist 2ter Platz | Sieger: " +
                ChatColor.GOLD + winningTeam +
                ChatColor.GRAY + " | Zeit: " + time;

        List<? extends Player> teamPlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> team.equalsIgnoreCase(plugin.getTeamManager().getPlayerTeam(p)))
                .toList();

        for (Player p : teamPlayers) {
            p.setGameMode(GameMode.SPECTATOR);
        }

        new BukkitRunnable() {
            int seconds = 0;

            @Override
            public void run() {
                if (seconds++ >= 10) {
                    cancel();
                    return;
                }

                for (Player p : teamPlayers) {
                    if (p.isOnline()) {
                        p.sendActionBar(msg);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void checkWinner() {

        if (finishedTeams.size() < 2) return;

        Bukkit.getLogger().info("🏁 Arena beendet. Gewinner: " + winningTeam);

        arenaRunning = false;
        plugin.setArenaRunning(false);

        sendArenaSummaryButton();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getLogger().info("🔄 Arena kann jetzt zurückgesetzt werden.");
        }, 200L);
    }

    private void sendArenaSummaryButton() {

        TextComponent msg = new TextComponent("▶ AUSWERTUNG");
        msg.setColor(net.md_5.bungee.api.ChatColor.GOLD);
        msg.setBold(true);

        msg.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§eKlicken für die Arena-Auswertung\n§7(Kills & Mobs je Team)")
                        .create()
        ));

        msg.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/arenasummary"
        ));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("");
            p.spigot().sendMessage(msg);
            p.sendMessage("");
        }
    }

    public void markPlayerReady(Player player) {
        if (plugin.getEventResume().loadPhase() != ResumeManager.PHASE_WAVEAUSWAHL) {
            return;
        }

        if (arenaStarted) return;

        playersReady.add(player.getUniqueId());

        boolean allReady = Bukkit.getOnlinePlayers().stream()
                .filter(p -> plugin.getTeamManager().isInTeam(p))
                .allMatch(p -> playersReady.contains(p.getUniqueId()));

        if (allReady) {
            arenaStarted = true;

            for (Player p : Bukkit.getOnlinePlayers()) {
                String world = p.getWorld().getName().toLowerCase();
                if (world.contains("mobarmylobby") || world.contains("rot") || world.contains("blau")) {

                    p.sendTitle(
                            ChatColor.GREEN + "✅ Alle Teams bereit!",
                            ChatColor.YELLOW + "Das MobArmyWars beginnt gleich...",
                            30, 60, 30
                    );
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
                }
            }
            Bukkit.getScheduler().runTaskLater(plugin, this::teleportTeamsToArena, 100L);
            Bukkit.getScheduler().runTaskLater(plugin, this::teleportAndStart, 140L);
        }

    }

    private void clearMobsInRegion(World world, Location corner1, Location corner2) {
        double minX = Math.min(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        for (Entity entity : world.getEntities()) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                Location loc = entity.getLocation();
                if (loc.getX() >= minX && loc.getX() <= maxX &&
                        loc.getY() >= minY && loc.getY() <= maxY &&
                        loc.getZ() >= minZ && loc.getZ() <= maxZ) {
                    entity.remove();
                }
            }
        }
    }

    public void resetArena() {
        winningTeam = null;
        winningTime = -1;
        arenaStarted = false;
        arenaRunning = false;

        stopAllArenaTasks();

        if (aggroTaskId != -1) {
            Bukkit.getScheduler().cancelTask(aggroTaskId);
            aggroTaskId = -1;
        }

        arenaRunning = false;
        plugin.setArenaRunning(false);
        arenaStarted = false;

        for (Chunk c : forcedChunks) {
            c.setForceLoaded(false);
        }
        forcedChunks.clear();

        waveRunning.clear();
        currentWave.clear();
        playersReady.clear();
        finishedTeams.clear();
        finishTimes.clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isArenaWorld(p.getWorld())) continue;

            if (plugin.getTimerManager() != null) {
                plugin.getTimerManager().removeBossBarFor(p);
            }

            plugin.getArenaBuildProtectionManager().clearTeamData("rot");
            plugin.getArenaBuildProtectionManager().clearTeamData("blau");
        }

        for (List<LivingEntity> mobList : activeMobs.values()) {
            for (LivingEntity mob : mobList) {
                if (mob != null && !mob.isDead()) {
                    mob.remove();
                }
            }
        }
        activeMobs.clear();

        for (BossBar bar : teamBossBars.values()) {
            bar.removeAll();
        }
        teamBossBars.clear();
        scoreboardManager.clearAllBoards();

        World world = Bukkit.getWorld("world_mobarmylobby");
        if (world != null) {
            clearMobsInRegion(world,
                    new Location(world, 238, 96, 381),
                    new Location(world, 160, 57, 271)
            );
            clearMobsInRegion(world,
                    new Location(world, 101, 96, 381),
                    new Location(world, 23, 57, 271)
            );
        }
    }

    private boolean isArenaWorld(World world) {
        if (world == null) return false;
        String name = world.getName().toLowerCase();
        return name.contains("mobarmylobby") || name.contains("rot") || name.contains("blau");
    }

    public ArenaScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public int getCurrentWave(String team) {
        return currentWave.getOrDefault(team, 0);
    }

    public void showArenaSummary(Player player) {
        int killsRot = scoreboardManager.getKillCount("Rot");
        int killsBlau = scoreboardManager.getKillCount("Blau");

        player.sendMessage("");
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "====== 📊 Auswertung Arena ======");
        player.sendMessage("");
        player.sendMessage(ChatColor.RED + "     Team Rot Kills: " + killsRot);
        player.sendMessage(ChatColor.BLUE + "     Team Blau Kills: " + killsBlau);
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "===============================");
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        if (!arenaRunning) return;

        Entity e = event.getEntity();
        if (!(e instanceof LivingEntity living)) return;

        Byte arenaMob = living.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "arenaMob"),
                PersistentDataType.BYTE
        );

        if (arenaMob == null || arenaMob != (byte) 1) return;
    }

    public void setRandomizer(BlockRandomizerManager randomizer) { this.randomizer = randomizer;
    }

    private void forceAggro(LivingEntity mob, Player target) {

        if (mob instanceof Bee bee) {
            bee.setAnger(999999);
            bee.setHasNectar(false);
            bee.setTarget(target);
        }
        else if (mob instanceof Wolf wolf) {
            wolf.setAngry(true);
            wolf.setTarget(target);
        }
        else if (mob instanceof Llama llama) {
            llama.setTarget(target);
        }
        else if (mob instanceof IronGolem golem) {
            golem.setTarget(target);
        }
        else if (mob instanceof Enderman enderman) {
            enderman.setTarget(target);
        }
        else if (mob instanceof Mob hostile) {
            hostile.setTarget(target);
        }
    }

    private void startAggroTask() {

        if (aggroTaskId != -1) return;

        aggroTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!arenaRunning) return;

                for (List<LivingEntity> mobs : activeMobs.values()) {
                    for (LivingEntity mob : mobs) {
                        if (mob == null || mob.isDead() || !mob.isValid()) continue;

                        if (!(mob instanceof Mob aiMob)) continue;

                        String enemyTeam = mob.getPersistentDataContainer().get(
                                new NamespacedKey(plugin, "enemyTeam"),
                                PersistentDataType.STRING
                        );
                        if (enemyTeam == null) continue;

                        if (aiMob.getTarget() instanceof Player currentTarget) {
                            String team = plugin.getTeamManager().getPlayerTeam(currentTarget);
                            if (team != null && team.equalsIgnoreCase(enemyTeam)) {
                                continue;
                            }
                        }

                        Player nearest = findNearestEnemy(mob, enemyTeam);
                        if (nearest == null) continue;

                        if (mob.getLocation().distanceSquared(nearest.getLocation()) > 40 * 40) continue;

                        forceAggro(aiMob, nearest);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    private String formatMillis(long ms) {
        long seconds = ms / 1000;
        long min = seconds / 60;
        long sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    public boolean isArenaRunning() {
        return arenaRunning;
    }

    public String getWinningTeam() {
        return winningTeam;
    }
}
