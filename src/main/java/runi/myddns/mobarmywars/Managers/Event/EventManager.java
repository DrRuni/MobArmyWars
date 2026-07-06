package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.*;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import runi.myddns.mobarmywars.Managers.World.ResumeManager;
import runi.myddns.mobarmywars.Utils.ConsoleColor;
import runi.myddns.mobarmywars.Utils.Sounds;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Managers.World.TeleportManager;

public class EventManager {

    private final MobArmyMain plugin;
    private final MobSaveManager mobSaveManager;
    private boolean monitoringTimerEnd = false;
//    private boolean eventStarted = false;
    private boolean eventHandlingDisabled = false;
    private BukkitRunnable monitoringTask = null;

//    public void disableEventHandling() {
//        this.eventHandlingDisabled = true;
//    }

    public void enableEventHandling() {
        this.eventHandlingDisabled = false;
    }

    public EventManager(MobArmyMain plugin, MobSaveManager mobSaveManager) {
        this.plugin = plugin;
        this.mobSaveManager = mobSaveManager;
    }

    public void startEvent() {

        if (eventHandlingDisabled) {
            Bukkit.getLogger().info("⏹️ EventHandling deaktiviert – EventStart unterbunden.");
            return;
        }

        if (plugin.getEventResume().isEventStarted()) return;

        if (plugin.getTimerManager().getTimeInSeconds() <= 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(ChatColor.RED + "❌ Der Timer muss noch eingestellt werden!");
                plugin.getTimerGUI().open(player);
            }
            return;
        }

        boolean allPlayersHaveTeam = true;

        for (Player player : Bukkit.getOnlinePlayers()) {
            String team = plugin.getTeamManager().getPlayerTeam(player);

            if (team == null || team.equalsIgnoreCase("Kein Team")) {
                allPlayersHaveTeam = false;
                player.sendMessage(ChatColor.YELLOW + "⚠ Du hast noch kein Team! Bitte wähle eines aus.");
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

            player.sendTitle(
                    ChatColor.GREEN + "Event beginnt",
                    ChatColor.YELLOW + "Du wirst gleich teleportiert",
                    10, 60, 10
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

                        StartCountdownEvent(() -> {
                            plugin.getTimerManager().setForward(false);
                            plugin.getTimerManager().startTimer();

                            for (Player player : Bukkit.getOnlinePlayers()) {
                                String team = plugin.getTeamManager().getPlayerTeam(player);
                                if (team == null || team.equalsIgnoreCase("Kein Team")) continue;
                            }
                        });
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

                    ChatColor color;

                    if (countdown == 5 || countdown == 4) {
                        color = ChatColor.RED;
                    } else if (countdown == 3) {
                        color = ChatColor.GOLD;
                    } else {
                        color = ChatColor.YELLOW;
                    }

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        String team = plugin.getTeamManager().getPlayerTeam(player);
                        if (team == null || team.equalsIgnoreCase("Kein Team")) continue;

                        player.sendTitle(
                                color + String.valueOf(countdown),
                                "",
                                0, 25, 0
                        );

                        player.playSound(
                                player.getLocation(),
                                Sound.BLOCK_NOTE_BLOCK_PLING,
                                1.0F,
                                1.0F
                        );
                    }

                    countdown--;

                } else {

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        String team = plugin.getTeamManager().getPlayerTeam(player);
                        if (team == null || team.equalsIgnoreCase("Kein Team")) continue;

                        player.sendTitle(
                                ChatColor.GREEN + "Go!",
                                "",
                                0, 30, 10
                        );

                        player.playSound(
                                player.getLocation(),
                                Sound.ENTITY_PLAYER_LEVELUP,
                                1.0F,
                                1.0F
                        );
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
                player.sendMessage(ChatColor.RED + "❌ Du hast kein Team! Bitte wähle zuerst eines.");
                continue;
            }

            String worldName = team.equalsIgnoreCase("rot") ? "world_rot"
                    : team.equalsIgnoreCase("blau") ? "world_blau" : null;

            if (worldName == null) {
                Bukkit.getLogger().warning("⚠ Spieler " + player.getName() + " hat ein ungültiges Team: " + team);
                continue;
            }

            World teamWorld = Bukkit.getWorld(worldName);
            if (teamWorld == null) {
                Bukkit.getLogger().warning("❌ Welt '" + worldName + "' existiert nicht!");
                player.sendMessage(ChatColor.RED + "Fehler: Team-Welt '" + worldName + "' nicht gefunden!");
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

            String worldName = player.getWorld().getName().toLowerCase();

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
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setExp(0f);
        player.setLevel(0);
        player.setTotalExperience(0);
        player.setInvulnerable(false);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
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
            Bukkit.getLogger().info("⏹️ EventHandling ist deaktiviert – handleTimerEnd() wird abgebrochen.");
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            String team = plugin.getTeamManager().getPlayerTeam(player);
            ChatColor color = "rot".equalsIgnoreCase(team) ? ChatColor.RED :
                    "blau".equalsIgnoreCase(team) ? ChatColor.BLUE : ChatColor.GRAY;

            player.sendTitle(
                    color + "⏰ Die Zeit ist abgelaufen!",
                    color + "Bereite dich nun auf den Arenakampf vor.",
                    20, 120, 20
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
                        player.sendMessage(ChatColor.RED +
                                "❌ Du hast kein Team! Bitte wähle zuerst eines.");
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

                    player.sendMessage(ChatColor.GREEN +
                            "🚀 Du wurdest zur Wavekonfiguration teleportiert!");

                    plugin.getEventResume().savePlayerSpawn(
                            player,
                            player.getLocation()
                    );
                }

                for (Player p : Bukkit.getOnlinePlayers()) {
                    String team = plugin.getTeamManager().getPlayerTeam(p);
                    if (team == null || team.equalsIgnoreCase("Kein Team")) continue;

                    p.sendMessage("");
                    p.sendMessage(ChatColor.AQUA +
                            "  ⚔ Bereite nun die Waves für deinen Gegner vor.");
                    p.sendMessage("");
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    String team = plugin.getTeamManager().getPlayerTeam(player);
                    if (team == null || team.equalsIgnoreCase("Kein Team")) continue;

                    ChatColor titleColor = "rot".equalsIgnoreCase(team) ? ChatColor.RED :
                            "blau".equalsIgnoreCase(team) ? ChatColor.BLUE : ChatColor.GRAY;

                    player.sendTitle(
                            titleColor + "Vorbereitung",
                            ChatColor.DARK_AQUA +
                                    "Erstelle die Waves und bereite dich auf den Kampf vor.",
                            20, 100, 20
                    );
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setHealth(player.getMaxHealth());
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
        this.monitoringTimerEnd = false;
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
            p.setBedSpawnLocation(null, true);
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

        player.sendMessage("");
        player.sendMessage("");
        player.sendMessage(ChatColor.DARK_RED + "   MobArmyWars wurde komplett zurückgesetzt!");
        player.sendMessage("");
        player.sendMessage("");
    }
}
