package runi.myddns.mobarmywars;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import runi.myddns.mobarmywars.Managers.Event.*;
import runi.myddns.mobarmywars.Utils.PluginFileManager;
import runi.myddns.mobarmywars.Utils.ConsoleColor;
import runi.myddns.mobarmywars.Commands.*;
import runi.myddns.mobarmywars.GUIs.*;
import runi.myddns.mobarmywars.Listeners.*;
import runi.myddns.mobarmywars.Managers.World.*;

public class MobArmyMain extends JavaPlugin {

    private static MobArmyMain instance;
    public static MobArmyMain getInstance() { return instance; }

    private boolean alreadyInitialized = false;

    // Manager
    private WorldManager worldManager;
    public TimerManager timerManager;
    public TeamManager teamManager;
    public BlockRandomizerManager blockRandomizerManager;
    public WaveManager waveManager;
    private MobSaveManager mobSaveManager;
    public WaveStorage waveStorage;
    public ArenaEventManager arenaManager;
    public EventManager eventManager;
    public ArenaScoreboardManager scoreboardManager;
    public MobSaveListener mobSaveListener;
    public BundleManager bundleManager;
    public ArenaBuildProtectionManager arenaBuildProtectionManager;
    private ResumeManager eventResume;
    private WorldSettings worldSettings;
    private PlayerLocationManager playerLocationManager;
    private PortalManager portalManager;
    private PlayerEffectManager playerEffectManager;
    private ArenaConfig arenaConfig;
    public OptionsGUI optionenGUI;
    public TimerGUI timerGUI;
    public SetupGUI eventSettingsGUI;
    public TeleportGUI mobArmySettingsGUI;
    public TeamSelectionGUI teamSelectionGUI;
    public BundleGUI bundleGUI;
    public RandomizerExclusionGUI spawnEggGUI;
    private ArenaSettingsGUI arenaSettingsGUI;
    private WorldSettingsGUI worldSettingsGUI;
    private PlayerGUI playerGUI;
    private PlayerActionGUI playerActionGUI;
    private TeamSettingsGUI teamSettingsGUI;
    private TeamScoreboardManager teamScoreboardManager;
    private ScoreboardSwitcher scoreboardSwitcher;
    private ArenaCompassManager arenaCompassManager;
    private TeamEquipmentManager teamEquipmentManager;
    private TeamEquipmentGUI teamEquipmentGUI;

    private boolean arenaRunning = false;
    public void setArenaRunning(boolean running) { arenaRunning = running; }

    @Override
    public void onLoad() {

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.COPPER + "  ═══════════════  MobArmyWars  ═══════════════" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.COPPER + "                      V1.5" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.COPPER + "                  L O A D I N G" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage("");

    }


    @Override
    public void onEnable() {
        instance = this;

        PluginFileManager pluginFileManager = new PluginFileManager(this);
        pluginFileManager.checkFilesOnStartup();

        worldSettings = new WorldSettings(this);
        worldManager = new WorldManager(this);

        worldManager.checkWorldsOnStartup();

        initializeMobArmyWars();

        teamManager.loadTeams();
        teamScoreboardManager.rebuildBoard();
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

        playerEffectManager = new PlayerEffectManager(this);
        blockRandomizerManager = new BlockRandomizerManager(this);

        arenaConfig = new ArenaConfig(this);
        arenaManager = new ArenaEventManager(this);
//        arenaManager.setRandomizer(blockRandomizerManager);

        teamManager = new TeamManager(this);

        arenaBuildProtectionManager = new ArenaBuildProtectionManager(this);
        arenaBuildProtectionManager.loadFromConfig();
        arenaBuildProtectionManager.loadSpawnProtectionAreas();

        mobSaveManager = new MobSaveManager(this, teamManager);

        waveManager = new WaveManager(this, mobSaveManager);

        waveStorage = new WaveStorage(getDataFolder(), waveManager);

        scoreboardManager = arenaManager.getScoreboardManager();
        waveManager.setScoreboardManager(scoreboardManager);

        eventResume = new ResumeManager(this);
        timerManager = new TimerManager(this);
        mobSaveManager.setTimerManager(timerManager);

        mobSaveListener = new MobSaveListener(this, mobSaveManager);

        bundleManager = new BundleManager(this);
        bundleManager.setTeamManager(teamManager);

        eventManager = new EventManager(this, mobSaveManager);
        playerLocationManager = new PlayerLocationManager(getEventResume().getConfig());

        portalManager = new PortalManager(this);
        portalManager.loadAllPortals();

        teamScoreboardManager = new TeamScoreboardManager(this);
        scoreboardSwitcher = new ScoreboardSwitcher(this, teamScoreboardManager, scoreboardManager);
        arenaCompassManager = new ArenaCompassManager(this);
        teamEquipmentManager = new TeamEquipmentManager(this);
        ChestRandomizerManager chestRandomizerManager = new ChestRandomizerManager(this, blockRandomizerManager);

        // ============================================================
        // GUIs
        // ============================================================
        optionenGUI = new OptionsGUI(this);
        timerGUI = new TimerGUI(this, timerManager);
        eventSettingsGUI = new SetupGUI(this);
        mobArmySettingsGUI = new TeleportGUI(this);
        teamSelectionGUI = new TeamSelectionGUI(this, teamManager);
        bundleGUI = new BundleGUI(this, teamManager);
        spawnEggGUI = new RandomizerExclusionGUI(blockRandomizerManager, this, timerManager);
        arenaSettingsGUI = new ArenaSettingsGUI(this);
        worldSettingsGUI = new WorldSettingsGUI(this, blockRandomizerManager);
        playerGUI = new PlayerGUI(this);
        playerActionGUI = new PlayerActionGUI(this);
        teamSettingsGUI = new TeamSettingsGUI(this);
        teamEquipmentGUI = new TeamEquipmentGUI(this);

        // ============================================================
        // Listener registrieren
        // ============================================================
        Bukkit.getPluginManager().registerEvents(new PauseListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PortalListener(this), this);
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
        Bukkit.getPluginManager().registerEvents(new BundleListener(bundleGUI, teamManager, bundleManager), this);
        Bukkit.getPluginManager().registerEvents(arenaSettingsGUI, this);
        Bukkit.getPluginManager().registerEvents(worldSettingsGUI, this);
        Bukkit.getPluginManager().registerEvents(new UltraHardcoreListener(this), this);
        Bukkit.getPluginManager().registerEvents(arenaCompassManager, this);
        Bukkit.getPluginManager().registerEvents(playerGUI, this);
        Bukkit.getPluginManager().registerEvents(playerActionGUI, this);
        Bukkit.getPluginManager().registerEvents(teamSettingsGUI, this);
        Bukkit.getPluginManager().registerEvents(teamEquipmentGUI, this);
        Bukkit.getPluginManager().registerEvents(new ScoreboardSwitchListener(this), this);
        Bukkit.getPluginManager().registerEvents(chestRandomizerManager, this);

        waveStorage.loadWaves();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                arenaBuildProtectionManager.loadFromConfig();
                arenaBuildProtectionManager.loadSpawnProtectionAreas();
            } catch (Exception ex) {
                Bukkit.getLogger().severe("[MobArmyWars] Fehler beim Nachladen der BuildProtection!");
                ex.printStackTrace();
            }
        }, 20L);

        // ============================================================
        // Commands
        // ============================================================
        var resumeCmd = new ResumeCommand(this);
        registerCommand("resume", resumeCmd, resumeCmd);
        registerCommand("mobarmy", resumeCmd, resumeCmd);

        OptionenCommand optionenCommand = new OptionenCommand(this);
        registerCommand("optionen", optionenCommand, optionenCommand);

        var teamCmd = new TeamCommand(this);
        registerCommand("team", teamCmd, teamCmd);

        MobStatusCommand mobStatusCommand = new MobStatusCommand(mobSaveManager, teamManager);
        registerCommand("mobstatus", mobStatusCommand, mobStatusCommand);

        registerCommand("arenasummary", new ArenaSummaryCommand(this), null);

        SetPhaseCommand setPhaseCommand = new SetPhaseCommand(this);
        registerCommand("setphase", setPhaseCommand, setPhaseCommand);

        ResetCommand resetCommand = new ResetCommand(this);
        registerCommand("reset", resetCommand, resetCommand);

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.DARK_GOLDEN_LIME + "  ═══════════════  MobArmyWars  ═══════════════" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.DARK_GOLDEN_LIME + "                      V1.5" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.DARK_GOLDEN_LIME + "                    R E A D Y" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage("");
    }

    private void registerCommand(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = getCommand(name);

        if (command == null) {
            getLogger().warning("Command '" + name + "' fehlt in der plugin.yml!");
            return;
        }

        command.setExecutor(executor);

        if (tabCompleter != null) {
            command.setTabCompleter(tabCompleter);
        }
    }

    public TeamManager getTeamManager() { return teamManager; }
    public TimerManager getTimerManager() { return timerManager; }
    public BlockRandomizerManager getBlockRandomizerManager() { return blockRandomizerManager; }
    public WaveManager getWaveManager() { return waveManager; }
    public WaveStorage getWaveStorage() { return waveStorage; }
    public ArenaEventManager getArenaManager() { return arenaManager; }
    public EventManager getEventManager() { return eventManager; }
    public BundleManager getBundleManager() { return bundleManager; }
    public ArenaBuildProtectionManager getArenaBuildProtectionManager() { return arenaBuildProtectionManager; }
    public OptionsGUI getOptionenGUI() { return optionenGUI; }
    public TimerGUI getTimerGUI() { return timerGUI; }
    public SetupGUI getEventSettingsGUI() { return eventSettingsGUI; }
    public TeleportGUI getMobArmySettingsGUI() { return mobArmySettingsGUI; }
    public TeamSelectionGUI getTeamSelectionGUI() { return teamSelectionGUI; }
    public BundleGUI getBundleGUI() { return bundleGUI; }
    public RandomizerExclusionGUI getSpawnEggGUI() { return spawnEggGUI; }
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
    public PortalManager getPortalManager() {
        return portalManager;
    }
    public PlayerEffectManager getPlayerEffectManager() { return playerEffectManager; }
    public TeamScoreboardManager getTeamScoreboardManager() { return teamScoreboardManager; }
    public ScoreboardSwitcher getScoreboardSwitcher() { return scoreboardSwitcher; }
    public ArenaSettingsGUI getArenaSettingsGUI() { return arenaSettingsGUI; }
    public WorldSettingsGUI getWorldSettingsGUI() { return worldSettingsGUI; }
    public ArenaCompassManager getArenaCompassManager() { return arenaCompassManager; }
    public PlayerGUI getPlayerGUI() { return playerGUI; }
    public PlayerActionGUI getPlayerActionGUI() { return playerActionGUI; }
    public TeamSettingsGUI getTeamSettingsGUI() { return teamSettingsGUI; }
    public TeamEquipmentManager getTeamEquipmentManager() { return teamEquipmentManager; }
    public TeamEquipmentGUI getTeamEquipmentGUI() { return teamEquipmentGUI; }
}