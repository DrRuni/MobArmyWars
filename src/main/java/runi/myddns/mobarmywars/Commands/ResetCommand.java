package runi.myddns.mobarmywars.Commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import runi.myddns.mobarmywars.Managers.World.WorldManager;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ResetCommand implements CommandExecutor, TabCompleter {

    private final MobArmyMain plugin;

    public ResetCommand(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command cmd,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    lang("commands.reset.player-only")
            );
            return true;
        }

        if (!player.isOp()) {

            player.sendMessage(
                    lang("commands.reset.no-permission")
            );

            player.playSound(
                    player.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_BASS,
                    1.0f,
                    0.8f
            );

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
                player.sendMessage(
                        lang("commands.reset.unknown")
                );

                sendUsage(player);
            }
        }

        return true;
    }

    private void resetTeamWorlds(Player player) {

        WorldManager wm = plugin.getWorldManager();

        if (wm.isWorldResetBlocked()) {

            player.sendMessage(
                    lang("commands.reset.already-running")
            );

            Sounds.playDanger(player);
            return;
        }

        Sounds.playReset(player);

        for (Player online : Bukkit.getOnlinePlayers()) {

            showResetTitle(
                    online,
                    "commands.reset.teamworld.title",
                    "commands.reset.teamworld.subtitle",
                    player.getName()
            );

            online.playSound(
                    online.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_PLING,
                    1.0f,
                    1.2f
            );
        }

        Bukkit.getScheduler().runTaskLater(
                plugin,
                wm::resetTeamWorlds,
                80L
        );
    }

    private void resetLobby(Player player) {

        WorldManager wm = plugin.getWorldManager();

        if (wm.isWorldResetBlocked()) {

            player.sendMessage(
                    lang("commands.reset.already-running")
            );

            Sounds.playDanger(player);
            return;
        }

        Sounds.playReset(player);

        for (Player online : Bukkit.getOnlinePlayers()) {

            showResetTitle(
                    online,
                    "commands.reset.lobby.title",
                    "commands.reset.lobby.subtitle",
                    player.getName()
            );

            online.playSound(
                    online.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_PLING,
                    1.0f,
                    1.2f
            );
        }

        Bukkit.getScheduler().runTaskLater(
                plugin,
                wm::resetLobbyWorld,
                80L
        );
    }

    private void resetArena(Player player) {

        WorldManager wm = plugin.getWorldManager();

        if (wm.isWorldResetBlocked()) {

            player.sendMessage(
                    lang("commands.reset.already-running")
            );

            Sounds.playDanger(player);
            return;
        }

        Sounds.playReset(player);

        for (Player online : Bukkit.getOnlinePlayers()) {

            showResetTitle(
                    online,
                    "commands.reset.arena.title",
                    "commands.reset.arena.subtitle",
                    player.getName()
            );

            online.playSound(
                    online.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_PLING,
                    1.0f,
                    1.2f
            );
        }

        Bukkit.getScheduler().runTaskLater(
                plugin,
                wm::resetArenaWorld,
                80L
        );
    }

    private void resetPlayerData(Player player) {

        Sounds.playReset(player);

        plugin.getEventManager()
                .resetGame(player);
    }

    private void sendUsage(Player player) {

        player.sendMessage(
                lang("commands.reset.usage.title")
        );

        player.sendMessage(
                lang("commands.reset.usage.arena")
        );

        player.sendMessage(
                lang("commands.reset.usage.lobby")
        );

        player.sendMessage(
                lang("commands.reset.usage.teamworld")
        );

        player.sendMessage(
                lang("commands.reset.usage.playerdata")
        );
    }

    private void showResetTitle(
            Player player,
            String titlePath,
            String subtitlePath,
            String playerName
    ) {

        Title title = Title.title(
                lang(titlePath),
                plugin.getLanguageManager().getComponent(
                        subtitlePath,
                        "player",
                        playerName
                ),
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(1)
                )
        );

        player.showTitle(title);
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command cmd,
            @NotNull String alias,
            @NotNull String @NotNull [] args
    ) {

        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (!player.isOp()) {
            return Collections.emptyList();
        }

        if (args.length == 1) {

            return Stream.of(
                            "arena",
                            "lobby",
                            "teamworld",
                            "playerdata"
                    )
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }
}