package runi.myddns.mobarmywars;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import runi.myddns.mobarmywars.Managers.Event.*;
import runi.myddns.mobarmywars.Managers.World.LanguageManager;
import runi.myddns.mobarmywars.Utils.PluginFileManager;
import runi.myddns.mobarmywars.Utils.ConsoleColor;
import runi.myddns.mobarmywars.Commands.*;
import runi.myddns.mobarmywars.GUIs.*;
import runi.myddns.mobarmywars.Listeners.*;
import runi.myddns.mobarmywars.Managers.World.*;

import java.util.logging.Level;

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
    private LanguageSelectionGUI languageSelectionGUI;
    private LanguageManager languageManager;
    private PlayerJoinListener playerJoinListener;

    @Override
    public void onLoad() {

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.COPPER + "  ═══════════════  MobArmyWars  ═══════════════" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.COPPER + "                      V1.7" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.COPPER + "                  L O A D I N G" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage("");

    }


    @Override
    public void onEnable() {
        instance = this;

        getServer().motd(createMotd());

        PluginFileManager pluginFileManager = new PluginFileManager(this);
        pluginFileManager.checkFilesOnStartup();

        languageManager = new LanguageManager(this);

        worldSettings = new WorldSettings(this);
        worldManager = new WorldManager(this);

        worldManager.checkWorldsOnStartup();

        initializeMobArmyWars();

        teamManager.loadTeams();
        teamScoreboardManager.rebuildBoard();

        timerManager.ensureBossBarExists();
        timerManager.updatePauseState();
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

        teamManager = new TeamManager(this);

        arenaBuildProtectionManager = new ArenaBuildProtectionManager(this);
        arenaBuildProtectionManager.loadSpawnProtectionAreas();

        mobSaveManager = new MobSaveManager(this, teamManager);

        waveManager = new WaveManager(mobSaveManager);

        waveStorage = new WaveStorage(this, getDataFolder(), waveManager);

        scoreboardManager = arenaManager.getScoreboardManager();
        waveManager.setScoreboardManager(scoreboardManager);

        eventResume = new ResumeManager(this);
        timerManager = new TimerManager(this);
        mobSaveManager.setTimerManager(timerManager);

        mobSaveListener = new MobSaveListener(this, mobSaveManager);

        bundleManager = new BundleManager(this);
        bundleManager.setTeamManager(teamManager);

        eventManager = new EventManager(this, mobSaveManager);

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
        spawnEggGUI = new RandomizerExclusionGUI(blockRandomizerManager, this);
        arenaSettingsGUI = new ArenaSettingsGUI(this);
        worldSettingsGUI = new WorldSettingsGUI(this, blockRandomizerManager);
        playerGUI = new PlayerGUI(this);
        playerActionGUI = new PlayerActionGUI(this);
        teamSettingsGUI = new TeamSettingsGUI(this);
        teamEquipmentGUI = new TeamEquipmentGUI(this);
        languageSelectionGUI = new LanguageSelectionGUI(this);
        playerJoinListener = new PlayerJoinListener(this);

        // ============================================================
        // Listener registrieren
        // ============================================================
        Bukkit.getPluginManager().registerEvents(new PauseListener(this), this);
        Bukkit.getPluginManager().registerEvents(playerJoinListener, this);
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
        Bukkit.getPluginManager().registerEvents(new BundleListener(this, bundleGUI, teamManager, bundleManager), this);
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
        Bukkit.getPluginManager().registerEvents(languageSelectionGUI, this);

        waveStorage.loadWaves();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                arenaConfig.reload();
                arenaBuildProtectionManager.loadSpawnProtectionAreas();
            } catch (Exception ex) {
                getLogger().log(
                        Level.SEVERE,
                        "Failed to reload build protection.",
                        ex
                );
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

        LanguageCommand languageCommand = new LanguageCommand(this);
        registerCommand("language", languageCommand, null);

        var teamCmd = new TeamCommand(this);
        registerCommand("team", teamCmd, teamCmd);

        MobStatusCommand mobStatusCommand = new MobStatusCommand(this, mobSaveManager, teamManager);
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
                ConsoleColor.DARK_GOLDEN_LIME + "                      V1.7" + ConsoleColor.RESET);
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

    private Component createMotd() {
        return MiniMessage.miniMessage().deserialize(
                "<bold><gradient:#FF3B3B:#8B0000>        Runi´s </gradient>" +
                        "<gradient:#4DA6FF:#004C99>MobArmyWars</gradient> " +
                        "<dark_gray>-</dark_gray> " +
                        "<yellow>V1.7</yellow></bold>" +
                        "\n" +
                        "<red><obfuscated>                  X</obfuscated></red> " +
                        "<bold><gradient:#FF3B3B:#4DA6FF>ROT  ⚔  BLAU</gradient> " +
                        "<blue><obfuscated>X</obfuscated></blue></bold>"
        );
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
    public LanguageSelectionGUI getLanguageSelectionGUI() {return languageSelectionGUI; }
    public LanguageManager getLanguageManager() {return languageManager; }
    public PlayerJoinListener getPlayerJoinListener() {return playerJoinListener; }
}