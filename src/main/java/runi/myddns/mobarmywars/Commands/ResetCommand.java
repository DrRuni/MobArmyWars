package runi.myddns.mobarmywars.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import runi.myddns.mobarmywars.Managers.World.WorldManager;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.util.Collections;
import java.util.List;

public class ResetCommand implements CommandExecutor, TabCompleter {

    private final MobArmyMain plugin;

    public ResetCommand(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("❌ Nur Spieler können diesen Befehl ausführen!");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "❌ Du hast keine Berechtigung für diesen Befehl!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "arena" -> resetArena(player);

            case "lobby" -> resetLobby(player);

            case "teamworld" -> resetTeamWorlds(player);

            case "playerdata" -> resetPlayerData(player);

            default -> {
                player.sendMessage(ChatColor.RED + "❌ Unbekannter Reset-Befehl!");
                sendUsage(player);
            }
        }

        return true;
    }

    private void resetTeamWorlds(Player player) {
        WorldManager wm = MobArmyMain.getInstance().getWorldManager();

        if (!wm.tryStartWorldReset()) {
            player.sendMessage(ChatColor.RED + "⚠ Es läuft bereits ein Welt-Reset!");
            Sounds.playDanger(player);
            return;
        }

        Sounds.playReset(player);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendTitle(
                    ChatColor.DARK_RED + "⚠ Reset aktiviert!",
                    ChatColor.GOLD + player.getName() + " hat den Team-Welten-Reset gestartet",
                    10, 100, 20
            );
            online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        }

        Bukkit.getScheduler().runTaskLater(
                MobArmyMain.getInstance(),
                () -> wm.resetTeamWorlds(),
                80L
        );
    }

    private void resetLobby(Player player) {
        WorldManager wm = MobArmyMain.getInstance().getWorldManager();

        if (!wm.tryStartWorldReset()) {
            player.sendMessage(ChatColor.RED + "⚠ Es läuft bereits ein Welt-Reset!");
            Sounds.playDanger(player);
            return;
        }

        Sounds.playReset(player);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendTitle(
                    ChatColor.DARK_RED + "⚠ Lobby-Reset!",
                    ChatColor.GOLD + player.getName() + " hat den Lobby-Welt-Reset gestartet",
                    10, 100, 20
            );
            online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        }

        Bukkit.getScheduler().runTaskLater(
                MobArmyMain.getInstance(),
                () -> wm.resetLobbyWorld(),
                80L
        );
    }

    private void resetArena(Player player) {
        WorldManager wm = MobArmyMain.getInstance().getWorldManager();

        if (!wm.tryStartWorldReset()) {
            player.sendMessage(ChatColor.RED + "⚠ Es läuft bereits ein Welt-Reset!");
            Sounds.playDanger(player);
            return;
        }

        Sounds.playReset(player);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendTitle(
                    ChatColor.DARK_RED + "⚠ Arena-Reset!",
                    ChatColor.GOLD + player.getName() + " hat den Arena-Welt-Reset gestartet",
                    10, 100, 20
            );
            online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        }

        Bukkit.getScheduler().runTaskLater(
                MobArmyMain.getInstance(),
                () -> wm.resetArenaWorld(),
                80L
        );
    }

    private void resetPlayerData(Player player) {
        Sounds.playReset(player);
        plugin.getEventManager().resetGame(player);
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.YELLOW + "⚠ Benutzung:");
        player.sendMessage(ChatColor.GRAY + "/reset arena");
        player.sendMessage(ChatColor.GRAY + "/reset lobby");
        player.sendMessage(ChatColor.GRAY + "/reset teamworld");
        player.sendMessage(ChatColor.GRAY + "/reset playerdata");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (!player.isOp()) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return List.of("arena", "lobby", "teamworld", "playerdata").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }
}