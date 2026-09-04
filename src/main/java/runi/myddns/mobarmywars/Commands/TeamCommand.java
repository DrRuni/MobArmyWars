package runi.myddns.mobarmywars.Commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class TeamCommand implements CommandExecutor, TabCompleter {

    private final MobArmyMain plugin;

    public TeamCommand(MobArmyMain plugin) {
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
                    lang("commands.team.player-only")
            );
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "join" -> handleJoin(player, args);

            case "leave" -> handleLeave(player);

            default -> {
                player.sendMessage(
                        lang("commands.team.unknown-subcommand")
                );

                sendUsage(player);
            }
        }

        return true;
    }

    private void handleJoin(
            Player player,
            String[] args
    ) {

        if (args.length < 2) {
            deny(
                    player,
                    "commands.team.join.missing-team"
            );
            return;
        }

        if (isInTeamWorld(player)) {
            deny(
                    player,
                    "commands.team.join.no-change-in-teamworld"
            );
            return;
        }

        if (!isInLobby(player)) {
            deny(
                    player,
                    "commands.team.join.lobby-only"
            );
            return;
        }

        if (plugin.getTeamManager().isInTeam(player)) {
            deny(
                    player,
                    "commands.team.join.already-in-team"
            );
            return;
        }

        switch (args[1].toLowerCase()) {

            case "rot", "red" -> {

                plugin.getTeamManager()
                        .assignTeam(player, "Rot");

                player.sendMessage(Component.empty());

                player.sendMessage(
                        lang("commands.team.join.joined-red")
                );
            }

            case "blau", "blue" -> {

                plugin.getTeamManager()
                        .assignTeam(player, "Blau");

                player.sendMessage(Component.empty());

                player.sendMessage(
                        lang("commands.team.join.joined-blue")
                );
            }

            default -> player.sendMessage(
                    lang("commands.team.join.invalid-team")
            );
        }
    }

    private void handleLeave(Player player) {

        if (isInTeamWorld(player)) {
            deny(
                    player,
                    "commands.team.leave.not-allowed-here"
            );
            return;
        }

        if (!plugin.getTeamManager().isInTeam(player)) {

            player.sendMessage(Component.empty());

            player.sendMessage(
                    lang("commands.team.leave.no-team")
            );

            return;
        }

        plugin.getTeamManager()
                .removePlayerFromTeam(player);

        plugin.getBundleManager()
                .removeTeamBundle(player);

        player.sendMessage(
                lang("commands.team.leave.left")
        );
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command cmd,
            @NotNull String alias,
            @NotNull String @NotNull [] args
    ) {

        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {

            return Stream.of(
                            "join",
                            "leave"
                    )
                    .filter(s ->
                            s.startsWith(
                                    args[0].toLowerCase()
                            )
                    )
                    .toList();
        }

        if (args.length == 2
                && args[0].equalsIgnoreCase("join")) {

            String language =
                    plugin.getConfig()
                            .getString(
                                    "language",
                                    "de"
                            );

            Stream<String> teams;

            if (language.equalsIgnoreCase("en")) {
                teams = Stream.of(
                        "red",
                        "blue"
                );
            } else {
                teams = Stream.of(
                        "rot",
                        "blau"
                );
            }

            return teams
                    .filter(s ->
                            s.startsWith(
                                    args[1].toLowerCase()
                            )
                    )
                    .toList();
        }

        return Collections.emptyList();
    }

    private boolean isInLobby(Player player) {

        return player.getWorld()
                .getName()
                .equalsIgnoreCase(
                        "world_mobarmy_lobby"
                );
    }

    private boolean isInTeamWorld(Player player) {

        String world =
                player.getWorld()
                        .getName()
                        .toLowerCase();

        return world.equals("world_rot")
                || world.equals("world_blau")
                || world.equals("world_rot_nether")
                || world.equals("world_blau_nether")
                || world.equals("world_mobarmy_arena");
    }

    private void deny(
            Player player,
            String path
    ) {

        player.sendMessage(
                lang(path)
        );

        player.playSound(
                player.getLocation(),
                Sound.BLOCK_NOTE_BLOCK_BASS,
                1.0f,
                0.8f
        );
    }

    private void sendUsage(Player player) {
        player.sendMessage(lang("commands.team.usage.title"));
        player.sendMessage(lang("commands.team.usage.join-red"));
        player.sendMessage(lang("commands.team.usage.join-blue"));
        player.sendMessage(lang("commands.team.usage.leave"));
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }
}