package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import runi.myddns.mobarmywars.Managers.World.ResumeManager;
import runi.myddns.mobarmywars.Utils.ConsoleColor;
import runi.myddns.mobarmywars.Utils.Sounds;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Managers.World.TeleportManager;

import java.time.Duration;
import java.util.Locale;

public class EventManager {

    private final MobArmyMain plugin;
    private final MobSaveManager mobSaveManager;
    private boolean eventHandlingDisabled = false;
    private BukkitRunnable monitoringTask = null;

    public void enableEventHandling() {
        this.eventHandlingDisabled = false;
    }

    public EventManager(MobArmyMain plugin, MobSaveManager mobSaveManager) {
        this.plugin = plugin;
        this.mobSaveManager = mobSaveManager;
    }

    public void startEvent() {

        if (eventHandlingDisabled) {
            plugin.getLogger().info("⏹️ EventHandling deaktiviert – EventStart unterbunden.");
            return;
        }

        if (plugin.getEventResume().isEventStarted()) return;

        if (plugin.getTimerManager().getTimeInSeconds() <= 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(
                        plugin.getLanguageManager().getComponent(
                                "event-manager.timer-not-set"
                        )
                );
                plugin.getTimerGUI().open(player);
            }
            return;
        }

        boolean allPlayersHaveTeam = true;

        for (Player player : Bukkit.getOnlinePlayers()) {
            String team = plugin.getTeamManager().getPlayerTeam(player);

            if (team == null || team.equalsIgnoreCase("Kein Team")) {
                allPlayersHaveTeam = false;
                player.sendMessage(
                        plugin.getLanguageManager().getComponent(
                                "event-manager.no-team"
                        )
                );
                plugin.getTeamSelectionGUI().openGUI(player);
            }
        }

        if (!allPlayersHaveTeam) {
            plugin.getTeamManager().waitForAllTeamsAndStart();
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            String team = plugin.getTeamManager().getPlayerTeam(player);

            if (team == null || team.equalsIgnoreCase("Kein Team")) continue;

            player.closeInventory();

            player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_PLAYER_LEVELUP,
                    1.0F,
                    1.0F
            );

            player.showTitle(
                    Title.title(
                            plugin.getLanguageManager().getComponent(
                                    "event-manager.start.title"
                            ),
                            plugin.getLanguageManager().getComponent(
                                    "event-manager.start.subtitle"
                            ),
                            Title.Times.times(
                                    Duration.ofMillis(500),
                                    Duration.ofSeconds(3),
                                    Duration.ofMillis(500)
                            )
                    )
            );
        }

        new BukkitRunnable() {
            @Override
            public void run() {

                teleportPlayersToTeamWorlds();

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!areTeamPlayersInTeamWorlds()) {
                            return;
                        }

                        cancel();

                        for (Player player : Bukkit.getOnlinePlayers()) {
                            String team = plugin.getTeamManager().getPlayerTeam(player);
                            if (team == null || team.equalsIgnoreCase("Kein Team")) continue;

                            player.showTitle(
                                    Title.title(
                                            plugin.getLanguageManager().getComponent(
                                                    "event-manager.prepare.title"
                                            ),
                                            plugin.getLanguageManager().getComponent(
                                                    "event-manager.prepare.subtitle"
                                            ),
                                            Title.Times.times(
                                                    Duration.ofMillis(500),
                                                    Duration.ofSeconds(2),
                                                    Duration.ofMillis(500)
                                            )
                                    )
                            );
                        }

                        Bukkit.getScheduler().runTaskLater(
                                plugin,
                                () -> StartCountdownEvent(() -> {
                                    plugin.getTimerManager().setForward(false);
                                    plugin.getTimerManager().startTimer();
                                }),
                                80L
                        );
                    }
                }.runTaskTimer(plugin, 5L, 5L);
            }
        }.runTaskLater(plugin, 60L);
    }

    private void StartCountdownEvent(Runnable onFinished) {

        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {

                if (countdown > 0) {

                    NamedTextColor color;

                    if (countdown >= 4) {
                        color = NamedTextColor.RED;
                    } else if (countdown == 3) {
                        color = NamedTextColor.GOLD;
                    } else {
                        color = NamedTextColor.YELLOW;
                    }

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        String team = plugin.getTeamManager().getPlayerTeam(player);
                        if (team == null || team.equalsIgnoreCase("Kein Team")) continue;

                        player.showTitle(
                                Title.title(
                                        Component.text(
                                                countdown,
                                                color
                                        ),
                                        Component.empty(),
                                        Title.Times.times(
                                                Duration.ZERO,
                                                Duration.ofMillis(1250),
                                                Duration.ZERO
                                        )
                                )
                        );

                        Sounds.playCountdown(player);
                    }

                    countdown--;

                } else {

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        String team = plugin.getTeamManager().getPlayerTeam(player);
                        if (team == null || team.equalsIgnoreCase("Kein Team")) continue;

                        player.showTitle(
                                Title.title(
                                        plugin.getLanguageManager().getComponent(
                                                "event-manager.countdown.go"
                                        ),
                                        Component.empty(),
                                        Title.Times.times(
                                                Duration.ZERO,
                                                Duration.ofMillis(1500),
                                                Duration.ofMillis(500)
                                        )
                                )
                        );

                        Sounds.playGo(player);
                    }

                    cancel();

                    if (onFinished != null) {
                        Bukkit.getScheduler().runTaskLater(plugin, onFinished, 15L);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void teleportPlayersToTeamWorlds() {

        plugin.getEventResume().setEventStarted(true);

        mobSaveManager.setMobSaveMode(MobSaveManager.MobSaveMode.ENABLED);

        for (Player player : Bukkit.getOnlinePlayers()) {
            String team = plugin.getTeamManager().getPlayerTeam(player);

            if (team == null || team.equalsIgnoreCase("Kein Team")) {
                player.sendMessage(
                        plugin.getLanguageManager().getComponent(
                                "event-manager.no-team"
                        )
                );
                continue;
            }

            String worldName = team.equalsIgnoreCase("rot") ? "world_rot"
                    : team.equalsIgnoreCase("blau") ? "world_blau" : null;

            if (worldName == null) {
                plugin.getLogger().warning(
                        "Player " + player.getName()
                                + " has an invalid team: " + team
                );
                continue;
            }

            World teamWorld = Bukkit.getWorld(worldName);

            if (teamWorld == null) {
                plugin.getLogger().warning(
                        "World '" + worldName + "' does not exist!"
                );

                player.sendMessage(
                        plugin.getLanguageManager().getComponent(
                                "event-manager.team-world-not-found",
                                "world",
                                worldName
                        )
                );

                continue;
            }

            Location spawnLocation = getSafeSpawn(teamWorld);

            player.teleport(spawnLocation);
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 100, 0.5, 1, 0.5, 0.1);
            Sounds.playTeleport(player);
            resetPlayerState(player);

            if (team.equalsIgnoreCase("blau")) {
                plugin.getTeamEquipmentManager().giveEquipment(player, "blue");
            } else if (team.equalsIgnoreCase("rot")) {
                plugin.getTeamEquipmentManager().giveEquipment(player, "red");
            }

            plugin.getBundleManager().giveTeamBundle(player);

            plugin.getEventResume().savePlayerSpawn(
                    player,
                    player.getLocation()
            );
        }

        plugin.getEventResume().savePhase(ResumeManager.PHASE_TEAMWELT);
    }

    private Location getSafeSpawn(World world) {
        if (world == null) return null;

        Location loc = world.getSpawnLocation().clone().add(0.5, 1, 0.5);

        Material block = loc.getBlock().getType();
        if (block == Material.WATER || block == Material.LAVA) {
            loc.add(0, 2, 0);
        }

        return loc;
    }

    private boolean areTeamPlayersInTeamWorlds() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String team = plugin.getTeamManager().getPlayerTeam(player);
            if (team == null || team.equalsIgnoreCase("Kein Team")) continue;

            String worldName =
                    player.getWorld().getName().toLowerCase(Locale.ROOT);

            if (team.equalsIgnoreCase("rot") && !worldName.equals("world_rot")) {
                return false;
            }

            if (team.equalsIgnoreCase("blau") && !worldName.equals("world_blau")) {
                return false;
            }
        }

        return true;
    }

    private void resetPlayerState(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        AttributeInstance maxHealth =
                player.getAttribute(Attribute.MAX_HEALTH);

        if (maxHealth != null) {
            player.setHealth(maxHealth.getValue());
        }
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setExp(0f);
        player.setLevel(0);
        player.setTotalExperience(0);
        player.setInvulnerable(false);
        player.getInventory().clear();
        player.getInventory().setArmorContents(
                new ItemStack[4]
        );
        player.getActivePotionEffects().forEach(effect ->
                player.removePotionEffect(effect.getType())
        );

        player.addPotionEffect(
                new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 0, false, false)
        );
        player.updateInventory();
    }

    public void handleTimerEnd() {
        mobSaveManager.setMobSaveMode(MobSaveManager.MobSaveMode.DISABLED);

        if (eventHandlingDisabled) {
            plugin.getLogger().info("⏹️ EventHandling ist deaktiviert – handleTimerEnd() wird abgebrochen.");
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            String team = plugin.getTeamManager().getPlayerTeam(player);
            NamedTextColor color =
                    "rot".equalsIgnoreCase(team)
                            ? NamedTextColor.RED
                            : "blau".equalsIgnoreCase(team)
                              ? NamedTextColor.BLUE
                              : NamedTextColor.GRAY;

            player.showTitle(
                    Title.title(
                            plugin.getLanguageManager()
                                    .getComponent("event-manager.timer-end.title")
                                    .color(color),
                            plugin.getLanguageManager()
                                    .getComponent("event-manager.timer-end.subtitle")
                                    .color(color),
                            Title.Times.times(
                                    Duration.ofSeconds(1),
                                    Duration.ofSeconds(6),
                                    Duration.ofSeconds(1)
                            )
                    )
            );

            player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_PLAYER_LEVELUP,
                    1f, 1f
            );
        }

        new BukkitRunnable() {
            @Override
            public void run() {

                for (Player player : Bukkit.getOnlinePlayers()) {
                    String team = plugin.getTeamManager().getPlayerTeam(player);
                    if (team == null) {
                        player.sendMessage(
                                plugin.getLanguageManager().getComponent(
                                        "event-manager.no-team"
                                )
                        );
                        continue;
                    }

                    TeleportManager.teleportToWaveSelection(player);

                    plugin.getArenaCompassManager().giveMonsterCompass(player);

                    player.spawnParticle(
                            Particle.PORTAL,
                            player.getLocation(),
                            100,
                            0.5, 1, 0.5, 0.1
                    );
                    Sounds.playTeleport(player);

                    player.sendMessage(
                            plugin.getLanguageManager().getComponent(
                                    "event-manager.wave-selection.teleported"
                            )
                    );

                    plugin.getEventResume().savePlayerSpawn(
                            player,
                            player.getLocation()
                    );
                }

                for (Player p : Bukkit.getOnlinePlayers()) {
                    String team = plugin.getTeamManager().getPlayerTeam(p);
                    if (team == null || team.equalsIgnoreCase("Kein Team")) continue;

                    p.sendMessage(Component.empty());
                    p.sendMessage(
                            plugin.getLanguageManager().getComponent(
                                    "event-manager.wave-selection.message"
                            )
                    );
                    p.sendMessage(Component.empty());
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    String team = plugin.getTeamManager().getPlayerTeam(player);
                    if (team == null || team.equalsIgnoreCase("Kein Team")) continue;

                    NamedTextColor titleColor =
                            "rot".equalsIgnoreCase(team)
                                    ? NamedTextColor.RED
                                    : "blau".equalsIgnoreCase(team)
                                      ? NamedTextColor.BLUE
                                      : NamedTextColor.GRAY;

                    player.showTitle(
                            Title.title(
                                    plugin.getLanguageManager()
                                            .getComponent("event-manager.wave-selection.title")
                                            .color(titleColor),
                                    plugin.getLanguageManager().getComponent(
                                            "event-manager.wave-selection.subtitle"
                                    ),
                                    Title.Times.times(
                                            Duration.ofSeconds(1),
                                            Duration.ofSeconds(5),
                                            Duration.ofSeconds(1)
                                    )
                            )
                    );
                    player.setGameMode(GameMode.SURVIVAL);
                    AttributeInstance maxHealth =
                            player.getAttribute(Attribute.MAX_HEALTH);

                    if (maxHealth != null) {
                        player.setHealth(maxHealth.getValue());
                    }
                    player.setFoodLevel(20);
                    player.setSaturation(20f);
                }

                plugin.getEventResume().savePhase(ResumeManager.PHASE_WAVEAUSWAHL);
                plugin.getTimerManager().stopTimer();
                plugin.getTimerManager().updateBossBar(null);
                plugin.getTimerManager().setForward(true);
                plugin.getTimerManager().startTimer();
            }
        }.runTaskLater(plugin, 100L);
    }

    public void resetEventState() {
        plugin.getEventResume().setEventStarted(false);
        this.eventHandlingDisabled = false;

        if (monitoringTask != null) {
            monitoringTask.cancel();
            monitoringTask = null;
        }
    }

    public void resetGame(Player player) {
        MobArmyMain plugin = this.plugin;
        ResumeManager resume = plugin.getEventResume();

        resume.beginBatch();

        plugin.getArenaManager().resetArena();
        Bukkit.getBossBars().forEachRemaining(BossBar::removeAll);

        for (Player p : Bukkit.getOnlinePlayers()) {
            resetPlayerState(p);
            p.setRespawnLocation(null, true);
            p.setGameMode(GameMode.SURVIVAL);

            TeleportManager.teleport(p, "world_mobarmy_lobby");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!p.isOnline()) return;

                Sounds.playReset(p);
            }, 3L);
        }

        TimerManager timerManager = plugin.getTimerManager();
        timerManager.stopTimer();
        timerManager.setTime(3600);
        timerManager.setForward(false);

        plugin.getBlockRandomizerManager().resetRandomizer();

        plugin.getTeamManager().resetTeams();
        plugin.getBundleGUI().clearTeamInventories();
        mobSaveManager.clearAllMobData();

        plugin.getWaveManager().getAllTeams()
                .forEach(team -> plugin.getWaveManager().resetWaves(team));

        plugin.getWaveStorage().saveWaves();
        resetEventState();
        plugin.getEventResume().reseteventdaten();

        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.getScoreboardSwitcher().removePlayer(p);
        }

        plugin.getTeamManager().loadTeams();
        plugin.getTeamScoreboardManager().rebuildBoard();
        plugin.getTeamEquipmentManager().resetAllEquipment();

        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.getScoreboardSwitcher().switchToTeam(p);
        }

        timerManager.ensureBossBarExists();
        timerManager.updatePauseState();

        resume.endBatch();

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ConsoleColor.DARK_RED + "   ⚠  MobArmyWars wurde komplett zurückgesetzt!" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage("");

        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());

        player.sendMessage(
                plugin.getLanguageManager().getComponent(
                        "event-manager.reset.complete"
                )
        );

        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
    }
}
