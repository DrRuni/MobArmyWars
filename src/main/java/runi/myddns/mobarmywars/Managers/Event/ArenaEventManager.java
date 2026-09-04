package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import java.time.Duration;
import runi.myddns.mobarmywars.Managers.World.ResumeManager;
import runi.myddns.mobarmywars.Managers.World.TeleportManager;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.util.*;

public class ArenaEventManager implements Listener {

    private final MobArmyMain plugin;
    private final Map<String, Integer> currentWave = new HashMap<>();
    private final Map<String, List<LivingEntity>> activeMobs = new HashMap<>();
    private final Map<String, BossBar> teamBossBars = new HashMap<>();
    private final Map<String, Long> finishTimes = new HashMap<>();
    private final Map<String, Boolean> waveRunning = new HashMap<>();
    private final Set<Chunk> forcedChunks = new HashSet<>();
    private final Set<String> teamsReady = new HashSet<>();
    private final Set<String> finishedTeams = new HashSet<>();
    private final NamespacedKey arenaMobKey;
    private final NamespacedKey enemyTeamKey;
    private final ArenaScoreboardManager scoreboardManager;
    private boolean arenaStarted = false;
    private boolean arenaRunning = false;
    public static final int MAX_WAVES = 3;
    private String winningTeam = null;
    private long winningTime = -1;
    private int aggroTaskId = -1;
    private static final Set<EntityType> FLYING_MOBS = Set.of(
            EntityType.GHAST,
            EntityType.HAPPY_GHAST,
            EntityType.PHANTOM,
            EntityType.WITHER,
            EntityType.ENDER_DRAGON,
            EntityType.BAT,
            EntityType.PARROT,
            EntityType.BEE
    );

    public ArenaEventManager(MobArmyMain plugin) {
        this.plugin = plugin;
        this.scoreboardManager = new ArenaScoreboardManager(plugin, this);
        this.arenaMobKey = new NamespacedKey(plugin, "arenaMob");
        this.enemyTeamKey = new NamespacedKey(plugin, "enemyTeam");

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

            plugin.getArenaCompassManager().giveMonsterCompass(player);

            player.setGameMode(GameMode.SURVIVAL);

            AttributeInstance maxHealthAttribute =
                    player.getAttribute(
                            Attribute.MAX_HEALTH
                    );

            if (maxHealthAttribute != null) {
                player.setHealth(
                        maxHealthAttribute.getValue()
                );
            }

            player.setFoodLevel(20);
            player.setSaturation(20f);
        }

        plugin.getEventResume().savePhase(ResumeManager.PHASE_ARENA);
    }

    public void startArenaEvent() {

        plugin.getTimerManager().stopTimer();

        int teleportTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {

            if (!arenaStarted) {
                return;
            }

            teleportTeamsToArena();

        }, 100L).getTaskId();

        registerTask(teleportTaskId);


        int countdownTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {

            if (!arenaStarted) {
                return;
            }

            startArenaCountdown(() -> {

                if (!arenaStarted) {
                    return;
                }

                plugin.getTimerManager().setForward(true);
                plugin.getTimerManager().startTimer();

                startWaveBattle();
            });

        }, 140L).getTaskId();

        registerTask(countdownTaskId);
    }

    private void startArenaCountdown(
            Runnable onFinished
    ) {

        int taskId = new BukkitRunnable() {

            int countdown = 5;

            @Override
            public void run() {

                if (!arenaStarted) {
                    cancel();
                    return;
                }

                if (countdown > 0) {

                    Component title =
                            Component.text(
                                    countdown,
                                    getCountdownColor(countdown)
                            );

                    Title countdownTitle =
                            Title.title(
                                    title,
                                    Component.empty(),
                                    Title.Times.times(
                                            Duration.ZERO,
                                            Duration.ofMillis(1250),
                                            Duration.ZERO
                                    )
                            );

                    for (Player player :
                            Bukkit.getOnlinePlayers()) {

                        if (!plugin.getTeamManager()
                                .isInTeam(player)) {
                            continue;
                        }

                        player.showTitle(countdownTitle);

                        Sounds.playCountdown(player);
                    }

                    countdown--;
                    return;
                }

                Title goTitle =
                        Title.title(
                                lang(
                                        "arena-event-manager.countdown.go"
                                ),
                                Component.empty(),
                                Title.Times.times(
                                        Duration.ZERO,
                                        Duration.ofMillis(1500),
                                        Duration.ofMillis(500)
                                )
                        );

                for (Player player :
                        Bukkit.getOnlinePlayers()) {

                    if (!plugin.getTeamManager()
                            .isInTeam(player)) {
                        continue;
                    }

                    player.showTitle(goTitle);

                    Sounds.playGo(player);
                }

                cancel();

                if (onFinished != null) {

                    int finishTaskId =
                            Bukkit.getScheduler()
                                    .runTaskLater(
                                            plugin,
                                            onFinished,
                                            15L
                                    )
                                    .getTaskId();

                    registerTask(finishTaskId);
                }
            }
        }.runTaskTimer(
                plugin,
                0L,
                20L
        ).getTaskId();

        registerTask(taskId);
    }

    public void startWaveBattle() {

        if (arenaRunning) {
            return;
        }

        WaveManager waveManager = plugin.getWaveManager();

        if (waveManager == null) {
            plugin.getLogger().warning("[ArenaManager] ❌ WaveManager ist null! Abbruch von startWaveBattle().");
            return;
        }

        ArenaConfig.ArenaData arena = plugin.getArenaConfig().getActiveArena();

        if (arena == null) {
            plugin.getLogger().warning("[ArenaManager] ❌ Keine aktive Arena geladen! Wave abgebrochen.");
            return;
        }

        World arenaWorld = Bukkit.getWorld(arena.world());

        if (arenaWorld == null) {
            plugin.getLogger().warning("[ArenaManager] ❌ Arena-Welt nicht geladen: " + arena.world());
            return;
        }

        arenaRunning = true;

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

        // Arena wurde inzwischen beendet/zurückgesetzt
        if (!arenaRunning) {
            return;
        }

        Integer waveIndexObj = currentWave.get(teamFighting);

        if (waveIndexObj == null) {
            return;
        }

        int waveIndex = waveIndexObj;

        if (!waveRunning.getOrDefault(waveOwner, false) && !finishedTeams.contains(waveOwner)) {
            plugin.getLogger().info("⏹ Wave-Spawning abgebrochen: waveOwner=" + waveOwner + " ist nicht aktiv.");
            return;
        }

        WaveManager waveManager = plugin.getWaveManager();

        List<WaveManager.WaveEntry> wave = waveManager.getWave(waveOwner, waveIndex);
        int finalWave = waveIndex + 1;

        if (wave == null || wave.isEmpty()) {
            handleEmptyWave(teamFighting, waveOwner, spawnPoints, finalWave);
            return;
        }

        announceWaveStart(teamFighting, finalWave);

        List<LivingEntity> spawned = new ArrayList<>();
        activeMobs.put(teamFighting, spawned);

        final int totalMobs = wave.stream()
                .mapToInt(WaveManager.WaveEntry::getAmount)
                .sum();

        BarColor color = teamFighting.equalsIgnoreCase("Rot") ? BarColor.RED : BarColor.BLUE;
        Component bossBarName =
                plugin.getLanguageManager()
                        .getComponent(
                                "arena-event-manager.bossbar.remaining",
                                "wave",
                                finalWave
                        );

        String bossBarText =
                PlainTextComponentSerializer.plainText()
                        .serialize(bossBarName);

        BossBar bar =
                Bukkit.createBossBar(
                        bossBarText,
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
                               List<WaveManager.WaveEntry> wave, List<LivingEntity> spawned,
                               int totalMobs, BossBar bar) {
        int delay = 0;
        Random random = new Random();

        for (WaveManager.WaveEntry entry : wave) {
            String mobType = entry.getMobType();
            int count = entry.getAmount();
            boolean isBaby = mobType.startsWith("BABY_");
            EntityType type = EntityType.valueOf(mobType.replace("BABY_", "").replace("ADULT_", ""));

            for (int i = 0; i < count; i++) {
                Location loc = spawnPoints[random.nextInt(spawnPoints.length)];
                Location spawnLoc = loc.clone().add(random.nextDouble(-1, 1), 0, random.nextDouble(-1, 1));

                int finalDelay = delay;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!arenaRunning) {
                            return;
                        }
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
                                arenaMobKey,
                                PersistentDataType.BYTE,
                                (byte) 1
                        );

                        String enemyTeam = waveOwner.equalsIgnoreCase("Rot") ? "Blau" : "Rot";
                        mob.getPersistentDataContainer().set(
                                enemyTeamKey,
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
                    bar.setProgress( Math.clamp (progress, 0.0, 1.0));
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

        for (Player player : Bukkit.getOnlinePlayers()) {

            String team =
                    plugin.getTeamManager()
                            .getPlayerTeam(player);

            if (team != null
                    && team.equalsIgnoreCase(teamFighting)) {

                player.sendMessage(
                        plugin.getLanguageManager()
                                .getComponent(
                                        "arena-event-manager.wave.empty",
                                        "wave",
                                        finalWave
                                )
                );

                player.playSound(
                        player.getLocation(),
                        Sound.ENTITY_PLAYER_LEVELUP,
                        1.0f,
                        1.0f
                );
            }
        }

        int emptyWaveTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {

            if (!arenaRunning) {
                return;
            }

            Integer current = currentWave.get(teamFighting);
            if (current == null) return;

            int nextWave = current + 1;
            int finishedWave = current + 1;

            if (nextWave < MAX_WAVES) {

                currentWave.put(
                        teamFighting,
                        nextWave
                );

                for (Player p :
                        Bukkit.getOnlinePlayers()) {

                    String team =
                            plugin.getTeamManager()
                                    .getPlayerTeam(p);

                    if (team != null
                            && team.equalsIgnoreCase(teamFighting)) {

                        showWaveCompleted(
                                p,
                                finishedWave
                        );
                    }
                }

                int taskId =
                        Bukkit.getScheduler()
                                .runTaskLater(
                                        plugin,
                                        () -> spawnWave(
                                                teamFighting,
                                                waveOwner,
                                                spawnPoints
                                        ),
                                        80L
                                )
                                .getTaskId();

                registerTask(taskId);

            } else {
                finishTeam(teamFighting);
            }

        }, 40L).getTaskId();

        registerTask(emptyWaveTaskId);
    }

    private void announceWaveStart(String teamFighting, int finalWave) {

        NamedTextColor teamColor =
                teamFighting.equalsIgnoreCase("Rot")
                        ? NamedTextColor.RED
                        : NamedTextColor.BLUE;

        Component title =
                Component.text(
                        "⚔ WAVE " + finalWave,
                        teamColor
                );

        Title waveTitle =
                Title.title(
                        title,
                        plugin.getLanguageManager().getComponent(
                                "arena-event-manager.wave.start-subtitle"
                        ),
                        Title.Times.times(
                                Duration.ofMillis(500),
                                Duration.ofSeconds(2),
                                Duration.ofMillis(500)
                        )
                );

        for (Player player : Bukkit.getOnlinePlayers()) {

            String team =
                    plugin.getTeamManager()
                            .getPlayerTeam(player);

            if (team == null
                    || !team.equalsIgnoreCase(teamFighting)) {
                continue;
            }

            player.showTitle(waveTitle);

            player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_PLAYER_LEVELUP,
                    1.0f,
                    1.2f
            );
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

    private void handleWaveCompletion(
            String teamFighting,
            String waveOwner,
            Location[] spawnPoints
    ) {

        if (!arenaRunning) {
            return;
        }

        Integer current = currentWave.get(teamFighting);

        if (current == null) {
            return;
        }

        int nextWave = current + 1;
        int finishedWave = current + 1;

        if (nextWave < MAX_WAVES) {
            currentWave.put(teamFighting, nextWave);

            for (Player p : Bukkit.getOnlinePlayers()) {
                String team = plugin.getTeamManager().getPlayerTeam(p);
                if (team != null && team.equalsIgnoreCase(teamFighting)) {
                    showWaveCompleted(p, finishedWave);
                }
            }

            int taskId = Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> spawnWave(teamFighting, waveOwner, spawnPoints),
                    80L
            ).getTaskId();

            registerTask(taskId);

        } else {
            finishTeam(teamFighting);
        }
    }

    private void announceWinnerToTeam(String team) {

        List<? extends Player> teamPlayers =
                Bukkit.getOnlinePlayers()
                        .stream()
                        .filter(player ->
                                team.equalsIgnoreCase(
                                        plugin.getTeamManager()
                                                .getPlayerTeam(player)
                                )
                        )
                        .toList();

        for (Player player : teamPlayers) {
            player.setGameMode(GameMode.SPECTATOR);
        }

        Component message =
                plugin.getLanguageManager()
                        .getComponent(
                                "arena-event-manager.winner.first",
                                "team",
                                getTeamDisplayName(team)
                        );

        new BukkitRunnable() {

            int seconds;

            @Override
            public void run() {

                if (seconds++ >= 10) {
                    cancel();
                    return;
                }

                for (Player player : teamPlayers) {

                    if (player.isOnline()) {
                        player.sendActionBar(message);
                    }
                }
            }
        }.runTaskTimer(
                plugin,
                0L,
                20L
        );
    }

    private void announceSecondPlace(String team) {

        long timeUsed =
                finishTimes.get(team)
                        - winningTime;

        String time =
                formatMillis(timeUsed);

        Component message =
                plugin.getLanguageManager()
                        .getComponent(
                                "arena-event-manager.winner.second",
                                Map.of(
                                        "team",
                                        getTeamDisplayName(team),
                                        "winner",
                                        getTeamDisplayName(winningTeam)
                                )
                        )
                        .replaceText(builder ->
                                builder.matchLiteral("%time%")
                                        .replacement(time)
                        );

        List<? extends Player> teamPlayers =
                Bukkit.getOnlinePlayers()
                        .stream()
                        .filter(player ->
                                team.equalsIgnoreCase(
                                        plugin.getTeamManager()
                                                .getPlayerTeam(player)
                                )
                        )
                        .toList();

        for (Player player : teamPlayers) {
            player.setGameMode(GameMode.SPECTATOR);
        }

        new BukkitRunnable() {

            int seconds;

            @Override
            public void run() {

                if (seconds++ >= 10) {
                    cancel();
                    return;
                }

                for (Player player : teamPlayers) {

                    if (player.isOnline()) {
                        player.sendActionBar(message);
                    }
                }
            }
        }.runTaskTimer(
                plugin,
                0L,
                20L
        );
    }

    private void checkWinner() {

        if (finishedTeams.size() < 2) return;

        plugin.getLogger().info("🏁 Arena beendet. Gewinner: " + winningTeam);

        arenaRunning = false;

        sendArenaSummaryButton();

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> plugin.getLogger().info(
                        "🔄 Arena kann jetzt zurückgesetzt werden."
                ),
                200L
        );
    }

    private void sendArenaSummaryButton() {

        Component message =
                lang(
                        "arena-event-manager.summary-button.name"
                )
                        .hoverEvent(
                                lang(
                                        "arena-event-manager.summary-button.hover"
                                )
                        )
                        .clickEvent(
                                ClickEvent.runCommand(
                                        "/arenasummary"
                                )
                        );

        for (Player player :
                Bukkit.getOnlinePlayers()) {

            player.sendMessage(
                    Component.empty()
            );

            player.sendMessage(message);

            player.sendMessage(
                    Component.empty()
            );
        }
    }

    public void markPlayerReady(Player player) {

        if (plugin.getEventResume().loadPhase()
                != ResumeManager.PHASE_WAVEAUSWAHL) {
            return;
        }

        if (arenaStarted) {
            return;
        }

        String team = plugin.getTeamManager().getPlayerTeam(player);

        if (team == null
                || (!team.equalsIgnoreCase("rot")
                && !team.equalsIgnoreCase("blau"))) {
            return;
        }

        String normalizedTeam = team.toLowerCase(Locale.ROOT);

        if (!teamsReady.add(normalizedTeam)) {

            player.sendMessage(
                    lang(
                            "arena-event-manager.ready.already-ready"
                    )
            );

            return;
        }

        Set<String> activeTeams = new HashSet<>();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

            String onlineTeam =
                    plugin.getTeamManager().getPlayerTeam(onlinePlayer);

            if (onlineTeam == null) {
                continue;
            }

            if (onlineTeam.equalsIgnoreCase("rot")
                    || onlineTeam.equalsIgnoreCase("blau")) {

                activeTeams.add(
                        onlineTeam.toLowerCase(Locale.ROOT)
                );
            }
        }

        boolean allTeamsReady =
                !activeTeams.isEmpty()
                        && teamsReady.containsAll(activeTeams);

        if (!allTeamsReady) {
            return;
        }

        arenaStarted = true;

        Title readyTitle =
                Title.title(
                        lang(
                                "arena-event-manager.ready.all-ready-title"
                        ),
                        lang(
                                "arena-event-manager.ready.all-ready-subtitle"
                        ),
                        Title.Times.times(
                                Duration.ofMillis(1500),
                                Duration.ofSeconds(3),
                                Duration.ofMillis(1500)
                        )
                );

        for (Player onlinePlayer :
                Bukkit.getOnlinePlayers()) {

            String world =
                    onlinePlayer.getWorld()
                            .getName()
                            .toLowerCase();

            if (!world.equals(
                    "world_mobarmy_lobby"
            )) {
                continue;
            }

            onlinePlayer.showTitle(
                    readyTitle
            );

            onlinePlayer.playSound(
                    onlinePlayer.getLocation(),
                    Sound.UI_TOAST_CHALLENGE_COMPLETE,
                    1f,
                    1.2f
            );
        }

        startArenaEvent();
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


        for (Chunk c : forcedChunks) {
            c.setForceLoaded(false);
        }
        forcedChunks.clear();

        waveRunning.clear();
        currentWave.clear();
        teamsReady.clear();
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

        ArenaConfig.ArenaData arena = plugin.getArenaConfig().getActiveArena();

        if (arena != null) {
            World world = Bukkit.getWorld(arena.world());

            if (world != null) {
                for (Mob mob : world.getEntitiesByClass(Mob.class)) {
                    mob.remove();
                }
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            TeleportManager.teleportToWaveSelection(player);

            player.setGameMode(GameMode.SURVIVAL);
            player.setInvulnerable(false);

            plugin.getEventResume().savePlayerSpawn(
                    player,
                    player.getLocation()
            );
        }

        plugin.getEventResume().savePhase(
                ResumeManager.PHASE_WAVEAUSWAHL
        );
    }

    private boolean isArenaWorld(World world) {
        if (world == null) return false;

        ArenaConfig.ArenaData arena = plugin.getArenaConfig().getActiveArena();
        if (arena == null) return false;

        return world.getName().equalsIgnoreCase(arena.world());
    }

    public ArenaScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public int getCurrentWave(String team) {
        return currentWave.getOrDefault(team, 0);
    }

    public void showArenaSummary(
            Player player
    ) {

        int killsRot =
                scoreboardManager.getKillCount(
                        "Rot"
                );

        int killsBlau =
                scoreboardManager.getKillCount(
                        "Blau"
                );

        player.sendMessage(
                Component.empty()
        );

        player.sendMessage(
                Component.empty()
        );

        player.sendMessage(
                lang(
                        "arena-event-manager.summary.header"
                )
        );

        player.sendMessage(
                Component.empty()
        );

        player.sendMessage(
                plugin.getLanguageManager()
                        .getComponent(
                                "arena-event-manager.summary.red-kills",
                                "kills",
                                killsRot
                        )
        );

        player.sendMessage(
                plugin.getLanguageManager()
                        .getComponent(
                                "arena-event-manager.summary.blue-kills",
                                "kills",
                                killsBlau
                        )
        );

        player.sendMessage(
                Component.empty()
        );

        player.sendMessage(
                lang(
                        "arena-event-manager.summary.footer"
                )
        );
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

                        String enemyTeam =
                                mob.getPersistentDataContainer().get(
                                        enemyTeamKey,
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

    private void showWaveCompleted(Player player, int wave) {

        Title title = Title.title(
                plugin.getLanguageManager().getComponent(
                        "arena-event-manager.wave.completed-title",
                        "wave",
                        wave
                ),
                plugin.getLanguageManager().getComponent(
                        "arena-event-manager.wave.completed-subtitle"
                ),
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofSeconds(2),
                        Duration.ofMillis(500)
                )
        );

        player.showTitle(title);

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_PLAYER_LEVELUP,
                1.0f,
                1.0f
        );
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }

    private Component getTeamDisplayName(String team) {

        if (team == null) {
            return Component.empty();
        }

        if (team.equalsIgnoreCase("Rot")) {
            return plugin.getLanguageManager().getComponent(
                    "arena-event-manager.team.red"
            );
        }

        if (team.equalsIgnoreCase("Blau")) {
            return plugin.getLanguageManager().getComponent(
                    "arena-event-manager.team.blue"
            );
        }

        return Component.text(team);
    }

    private NamedTextColor getCountdownColor(int countdown) {

        if (countdown >= 4) {
            return NamedTextColor.RED;
        }

        if (countdown == 3) {
            return NamedTextColor.GOLD;
        }

        return NamedTextColor.YELLOW;
    }
}
