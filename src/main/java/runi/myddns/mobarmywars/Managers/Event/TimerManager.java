package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import runi.myddns.mobarmywars.Managers.World.ResumeManager;
import runi.myddns.mobarmywars.MobArmyMain;

public class TimerManager implements Listener {
    private final MobArmyMain plugin;
    public boolean isRunning = false;
    public boolean isForward = false;
    private boolean collectPhaseEnded = false;
    private int timeInSeconds = 3600;
    private BossBar bossBar = null;
    private BukkitTask resumeSaveTask;

    public MobArmyMain getPlugin() {
        return plugin;
    }

    public TimerManager(MobArmyMain plugin) {
        this.plugin = plugin;

        loadTime();
        startTimerTask();
    }

    public void setTime(int seconds) {
        this.timeInSeconds = seconds;
        plugin.getEventResume().saveTimerState(seconds, isForward);
        updateBossBar(null);

        if (seconds <= 0) {
            onTimerReachedZero();
        }
    }

    public void addPlayerToBossBar(Player player) {

        if (bossBar == null) {
            bossBar = Bukkit.createBossBar("Timer", BarColor.BLUE, BarStyle.SEGMENTED_6);
        }

        if (!bossBar.getPlayers().contains(player)) {
            bossBar.addPlayer(player);
        }

        updateBossBar(null);
    }

    public void removeBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    private boolean timerTaskRunning = false;

    public void startTimerTask() {
        if (timerTaskRunning) return;
        timerTaskRunning = true;

        Bukkit.getScheduler().runTaskTimer(plugin, this::updateTimer, 20L, 20L);
    }

    public void updateTimer() {
        if (!isRunning) return;

        if (isForward) {
            timeInSeconds++;
            updateBossBar(null);
            return;
        }

        if (timeInSeconds > 0) {
            timeInSeconds--;

            if (timeInSeconds == 0) {
                onTimerReachedZero();
            }
        }

        updateBossBar(null);
    }

    public void updateBossBar(String customMessage) {
        if (bossBar == null) return;

        if (timeInSeconds < 0 && !isForward) {
            bossBar.setTitle(ChatColor.GOLD + "Die Zeit ist abgelaufen!");
        } else {
            int displayTime = Math.max(0, timeInSeconds);
            String time = customMessage != null ? customMessage : formatTime(displayTime);
            bossBar.setTitle(time);
        }
    }

    private String formatTime(int timeInSeconds) {
        int days = timeInSeconds / 86400;
        int hours = (timeInSeconds % 86400) / 3600;
        int minutes = (timeInSeconds % 3600) / 60;
        int seconds = timeInSeconds % 60;

        if (days > 0) {
            String dayText = (days == 1) ? "Tag" : "Tage";
            return String.format(
                    ChatColor.RED + "%d " + ChatColor.BLUE + "%s, " +
                            ChatColor.RED + "%02d" + ChatColor.BLUE + "h " +
                            ChatColor.RED + "%02d" + ChatColor.BLUE + "m " +
                            ChatColor.RED + "%02d" + ChatColor.BLUE + "s",
                    days, dayText, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format(
                    ChatColor.RED + "%d" + ChatColor.BLUE + "h " +
                            ChatColor.RED + "%02d" + ChatColor.BLUE + "m " +
                            ChatColor.RED + "%02d" + ChatColor.BLUE + "s",
                    hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format(
                    ChatColor.RED + "%d" + ChatColor.BLUE + "m " +
                            ChatColor.RED + "%02d" + ChatColor.BLUE + "s",
                    minutes, seconds);
        } else {
            return String.format(
                    ChatColor.RED + "%d" + ChatColor.BLUE + "sek",
                    seconds);
        }
    }

    public void updatePauseState() {

        plugin.getEventResume().setEventPaused(!isRunning);
    }

    public void loadTime() {
        timeInSeconds = plugin.getEventResume().loadTimerTime();
        isForward = plugin.getEventResume().loadTimerDirection();

        isRunning = false;
        updatePauseState();
    }

    public void startTimer() {
        isRunning = true;
        startResumeSaveTask();

        if (!isForward) {
            collectPhaseEnded = false;
        }

        updatePauseState();

        for (Player p : Bukkit.getOnlinePlayers()) {
            String world = p.getWorld().getName().toLowerCase();
            if (world.contains("mobarena") || world.contains("rot") || world.contains("blau")) {
                p.sendMessage(ChatColor.GREEN + "▶ Der Timer wurde gestartet!");
            }
        }

        updatePauseState();
        ensureBossBarExists();
        updateBossBar(null);
    }

    public void pauseTimer() {
        isRunning = false;
        stopResumeSaveTask();
        updatePauseState();

        for (Player p : Bukkit.getOnlinePlayers()) {
            String world = p.getWorld().getName().toLowerCase();
            if (world.contains("mobarena") || world.contains("rot") || world.contains("blau")) {
                p.sendMessage(ChatColor.YELLOW + "⏸  Der Timer wurde pausiert!");
            }
        }
    }

    public void stopTimer() {
        isRunning = false;
        stopResumeSaveTask();

        if (!isForward && timeInSeconds <= 0) {
            collectPhaseEnded = true;
        }

        updatePauseState();
        updateBossBar(null);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isForward() {
        return isForward;
    }

    public int getTimeInSeconds() {
        return timeInSeconds;
    }

    public void addTime(int seconds) {
        timeInSeconds += seconds;
        updateBossBar(null);
    }

    public void removeTime(int seconds) {
        timeInSeconds = Math.max(0, timeInSeconds - seconds);
        updateBossBar(null);

        if (timeInSeconds == 0) {
            onTimerReachedZero();
        }
    }

    public void setForward(boolean forward) {
        this.isForward = forward;

        if (forward && timeInSeconds < 0) {
            timeInSeconds = 0;
        }

        plugin.getEventResume().saveTimerState(timeInSeconds, isForward);

        updateBossBar(null);
    }

    public void ensureBossBarExists() {
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar("Timer", BarColor.BLUE, BarStyle.SEGMENTED_6);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
        }
        updateBossBar(null);
    }

    public void removeBossBarFor(Player player) {
        if (bossBar != null && bossBar.getPlayers().contains(player)) {
            bossBar.removePlayer(player);
        }
    }

    private void onTimerReachedZero() {

        if (collectPhaseEnded) return;
        if (isForward) return;
        if (!isRunning) return;
        if (plugin.getEventResume().loadPhase() != ResumeManager.PHASE_TEAMWELT) return;

        collectPhaseEnded = true;

        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getEventManager().handleTimerEnd();
        });
    }

    private void startResumeSaveTask() {
        if (resumeSaveTask != null) return;

        resumeSaveTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> {
                    if (!isRunning) return;
                    plugin.getEventResume().saveTimerState(timeInSeconds, isForward);
                },
                20L * 30,
                20L * 30
        );
    }

    private void stopResumeSaveTask() {
        if (resumeSaveTask != null) {
            resumeSaveTask.cancel();
            resumeSaveTask = null;
        }
    }
}
