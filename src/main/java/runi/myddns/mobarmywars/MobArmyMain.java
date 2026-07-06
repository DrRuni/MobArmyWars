package runi.myddns.mobarmywars;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import runi.myddns.mobarmywars.Arena.*;
import runi.myddns.mobarmywars.Utils.ConsoleColor;
import runi.myddns.mobarmywars.Commands.*;
import runi.myddns.mobarmywars.GUIs.*;
import runi.myddns.mobarmywars.Listeners.*;
import runi.myddns.mobarmywars.Managers.*;
import runi.myddns.mobarmywars.World.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class MobArmyMain extends JavaPlugin {

    private static MobArmyMain instance;
    public static MobArmyMain getInstance() { return instance; }

    private boolean alreadyInitialized = false;
    public boolean isInitialized() { return alreadyInitialized; }
    public void setInitialized(boolean value) { alreadyInitialized = value; }

    // Manager
    private WorldManager worldManager;
    public TimerManager timerManager;
    public TeamManager teamManager;
    public BlockRandomizerManager blockRandomizerManager;
    public WaveManager waveManager;
    private MobSaveManager mobSaveManager;
    public WaveStorage waveStorage;
    public ArenaManager arenaManager;
    public EventManager eventManager;
    public ArenaScoreboardManager scoreboardManager;
    public MobSaveListener mobSaveListener;
    public BundleManager bundleManager;
    public ArenaBuildProtectionManager arenaBuildProtectionManager;
    private ScoreboardController scoreboardController;
    private ResumeManager eventResume;
    private WorldSettings worldSettings;
    private PlayerLocationManager playerLocationManager;
    private PortalManager portalManager;

    private ArenaConfig arenaConfig;

    public OptionenGUI optionenGUI;
    public TimerGUI timerGUI;
    public EventSettingsGUI eventSettingsGUI;
    public TeleportGUI mobArmySettingsGUI;
    public TeamSelectionGUI teamSelectionGUI;
    public BundleGUI bundleGUI;
    public SpawnEggGUI spawnEggGUI;

    private boolean arenaRunning = false;
    public boolean isArenaRunning() { return arenaRunning; }
    public void setArenaRunning(boolean running) { arenaRunning = running; }

    @Override
    public void onLoad() {

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.COPPER + "  ═══════════════  MobArmyWars  ═══════════════" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.COPPER + "                  L O A D I N G" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage("");

    }


    @Override
    public void onEnable() {
        instance = this;

        worldManager = new WorldManager(this);
        worldManager.checkWorldsOnStartup();

        extractServerIconIfMissing();
        saveDefaultConfig();
        createArenaCoordFile();

        scoreboardController = new ScoreboardController(this);

        initializeMobArmyWars();
    }

    @Override
    public void onDisable() {

        if (waveStorage != null) waveStorage.saveWaves();

        if (timerManager != null) {
            getEventResume().saveTimerState(
                    timerManager.getTimeInSeconds(),
                    timerManager.isForward()
            );
            timerManager.removeBossBar();
        }
        if (blockRandomizerManager != null) {
            blockRandomizerManager.saveBlockDrops();
        }
    }

    private void initializeMobArmyWars() {

        if (alreadyInitialized) return;
        alreadyInitialized = true;

        worldSettings = new WorldSettings(this);
        blockRandomizerManager = new BlockRandomizerManager(this);

        arenaConfig = new ArenaConfig(this);
        arenaManager = new ArenaManager(this);
        arenaManager.setRandomizer(blockRandomizerManager);

        teamManager = new TeamManager(this);
        teamManager.loadTeams();

        arenaBuildProtectionManager = new ArenaBuildProtectionManager(this);
        arenaBuildProtectionManager.loadFromConfig();

        mobSaveManager = new MobSaveManager(this, teamManager);

        waveManager = new WaveManager(this, mobSaveManager);

        waveStorage = new WaveStorage(getDataFolder(), waveManager);

        scoreboardManager = arenaManager.getScoreboardManager();
        waveManager.setScoreboardManager(scoreboardManager);

        teamManager.enableTeamScoreboard();

        eventResume = new ResumeManager(this);
        if (eventResume.isEventStarted() && !eventResume.isEventPaused()) {
            mobSaveManager.setMobSaveMode(MobSaveManager.MobSaveMode.ENABLED);
        } else {
            mobSaveManager.setMobSaveMode(MobSaveManager.MobSaveMode.DISABLED);
        }

        timerManager = new TimerManager(this, teamManager);
        mobSaveManager.setTimerManager(timerManager);

        mobSaveListener = new MobSaveListener(this, mobSaveManager);

        bundleManager = new BundleManager(this);
        bundleManager.setTeamManager(teamManager);

        eventManager = new EventManager(this, mobSaveManager);
        playerLocationManager = new PlayerLocationManager(getEventResume().getConfig());

        portalManager = new PortalManager(this);
        portalManager.loadAllPortals();

        // ============================================================
        // GUIs
        // ============================================================
        optionenGUI = new OptionenGUI(this);
        timerGUI = new TimerGUI(this, timerManager);
        eventSettingsGUI = new EventSettingsGUI(this, blockRandomizerManager);
        mobArmySettingsGUI = new TeleportGUI(this);
        teamSelectionGUI = new TeamSelectionGUI(this, teamManager);
        bundleGUI = new BundleGUI(this, teamManager);
        spawnEggGUI = new SpawnEggGUI(blockRandomizerManager, this, timerManager);

        teamManager.enableTeamScoreboard();
        // ============================================================
        // Listener registrieren
        // ============================================================
        Bukkit.getPluginManager().registerEvents(
                new PauseListener(this), this);

        Bukkit.getPluginManager().registerEvents(
                new PlayerDataListener(this), this);

        Bukkit.getPluginManager().registerEvents(
                new PlayerRespawnListener(this), this);

        Bukkit.getPluginManager().registerEvents(
                new PortalListener(this), this);

        Bukkit.getPluginManager().registerEvents(blockRandomizerManager, this);
        Bukkit.getPluginManager().registerEvents(mobSaveListener, this);
        Bukkit.getPluginManager().registerEvents(timerManager, this);
        Bukkit.getPluginManager().registerEvents(teamManager, this);

        Bukkit.getPluginManager().registerEvents(optionenGUI, this);
        Bukkit.getPluginManager().registerEvents(timerGUI, this);
        Bukkit.getPluginManager().registerEvents(eventSettingsGUI, this);
        Bukkit.getPluginManager().registerEvents(mobArmySettingsGUI, this);
        Bukkit.getPluginManager().registerEvents(teamSelectionGUI, this);
        Bukkit.getPluginManager().registerEvents(spawnEggGUI, this);



        Bukkit.getPluginManager().registerEvents(new ButtonManager(this), this);
        Bukkit.getPluginManager().registerEvents(new ArenaMobTargetListener(this), this);
        Bukkit.getPluginManager().registerEvents(
                new BundleListener(bundleGUI, teamManager, bundleManager), this
        );

        waveStorage.loadWaves();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                var m = arenaBuildProtectionManager.getClass()
                        .getDeclaredMethod("loadFromConfig");
                m.setAccessible(true);
                m.invoke(arenaBuildProtectionManager);
            } catch (Exception ex) {
                Bukkit.getLogger().severe("[MobArmyWars] Fehler beim Nachladen der BuildProtection!");
                ex.printStackTrace();
            }
        }, 20L);

        // ============================================================
        // Commands
        // ============================================================
        var resumeCmd = new ResumeCommand(this);
        getCommand("resume").setExecutor(resumeCmd);
        getCommand("resume").setTabCompleter(resumeCmd);

        getCommand("mobarmy").setExecutor(resumeCmd);
        getCommand("mobarmy").setTabCompleter(resumeCmd);

        OptionenCommand optionenCommand = new OptionenCommand(this);
        getCommand("optionen").setExecutor(optionenCommand);
        getCommand("optionen").setTabCompleter(optionenCommand);

        var teamCmd = new TeamCommand(this);
        getCommand("team").setExecutor(teamCmd);
        getCommand("team").setTabCompleter(teamCmd);

        MobStatusCommand mobStatusCommand =
                new MobStatusCommand(mobSaveManager, teamManager);

        getCommand("mobstatus").setExecutor(mobStatusCommand);
        getCommand("mobstatus").setTabCompleter(mobStatusCommand);

        getCommand("arenasummary").setExecutor(new ArenaSummaryCommand(this));

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.DARK_GOLDEN_LIME + "  ═══════════════  MobArmyWars  ═══════════════" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.DARK_GOLDEN_LIME + "                    R E A D Y" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage("");
    }

    private void extractServerIconIfMissing() {

        File serverIcon = new File("server-icon.png");

        if (serverIcon.exists()) {
            return;
        }

        if (getResource("server-icon.png") == null) {
            getLogger().warning("⚠️ Keine server-icon.png im Plugin gefunden!");
            return;
        }

        try (InputStream in = getResource("server-icon.png")) {

            if (in == null) {
                getLogger().warning("⚠️ Keine server-icon.png im Plugin gefunden!");
                return;
            }
            Files.copy(
                    in,
                    serverIcon.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

            Bukkit.getConsoleSender().sendMessage("");
            Bukkit.getConsoleSender().sendMessage(ConsoleColor.LIME + "   MobArmyWars-ServerIcon wurde erstellt!" + ConsoleColor.RESET);
            Bukkit.getConsoleSender().sendMessage("");

        } catch (IOException e) {
            getLogger().severe("❌ Fehler beim Kopieren der server-icon.png!");
            e.printStackTrace();
        }
    }

    private void createArenaCoordFile() {
        File f = new File(getDataFolder(), "arena-koordinaten.yml");
        if (!f.exists()) {
            saveResource("arena-koordinaten.yml", false);
            Bukkit.getConsoleSender().sendMessage(ConsoleColor.LIME + "   Datei - " +
                    ConsoleColor.GOLD + "'arena-koordinaten.yml' erstellt." + ConsoleColor.RESET);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("arenasummary")) {

            if (sender instanceof Player player) {

                if (arenaManager != null) {
                    arenaManager.showArenaSummary(player);
                } else {
                    player.sendMessage("Arena ist noch nicht geladen!");
                }

            } else {
                sender.sendMessage("Nur Spieler können die Auswertung sehen.");
            }

            return true;
        }

        return false;
    }

    public void setEventResume(ResumeManager eventResume) {
        this.eventResume = eventResume;
    }

    public TeamManager getTeamManager() { return teamManager; }
    public TimerManager getTimerManager() { return timerManager; }
    public BlockRandomizerManager getBlockRandomizerManager() { return blockRandomizerManager; }
    public WaveManager getWaveManager() { return waveManager; }
    public WaveStorage getWaveStorage() { return waveStorage; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public EventManager getEventManager() { return eventManager; }
    public ArenaScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public MobSaveListener getMobSaveListener() { return mobSaveListener; }
    public BundleManager getBundleManager() { return bundleManager; }
    public ArenaBuildProtectionManager getArenaBuildProtectionManager() { return arenaBuildProtectionManager; }
    public OptionenGUI getOptionenGUI() { return optionenGUI; }
    public TimerGUI getTimerGUI() { return timerGUI; }
    public EventSettingsGUI getEventSettingsGUI() { return eventSettingsGUI; }
    public TeleportGUI getMobArmySettingsGUI() { return mobArmySettingsGUI; }
    public TeamSelectionGUI getTeamSelectionGUI() { return teamSelectionGUI; }
    public BundleGUI getBundleGUI() { return bundleGUI; }
    public SpawnEggGUI getSpawnEggGUI() { return spawnEggGUI; }
    public ScoreboardController getScoreboardController() { return scoreboardController; }
    public ResumeManager getEventResume() { return eventResume; }
    public WorldManager getWorldManager() {
        return worldManager;
    }

    public ArenaConfig getArenaConfig() {
        return arenaConfig;
    }
    public MobSaveManager getMobSaveManager() {
        return mobSaveManager;
    }
    public WorldSettings getWorldSettings() {
        return worldSettings;
    }
    public PlayerLocationManager getPlayerLocationManager() {
        return playerLocationManager;
    }
    public PortalManager getPortalManager() {
        return portalManager;
    }
}