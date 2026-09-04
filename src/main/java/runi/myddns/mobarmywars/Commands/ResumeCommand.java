package runi.myddns.mobarmywars.Commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.List;

public class ResumeCommand implements CommandExecutor, TabCompleter {

    private final MobArmyMain plugin;

    public ResumeCommand(MobArmyMain plugin) {
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
                    lang("commands.resume.player-only")
            );
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(
                    lang("commands.resume.op-only")
            );
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("resume")) {

            if (args.length == 1
                    && args[0].equalsIgnoreCase("mobarmy")) {

                plugin.getEventResume().resumeEvent();
                return true;
            }

            player.sendMessage(
                    lang("commands.resume.usage-resume")
            );

            return true;
        }

        if (cmd.getName().equalsIgnoreCase("mobarmy")) {

            if (args.length == 1
                    && args[0].equalsIgnoreCase("resume")) {

                plugin.getEventResume().resumeEvent();
                return true;
            }

            player.sendMessage(
                    lang("commands.resume.usage-mobarmy")
            );

            return true;
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command cmd,
            @NotNull String alias,
            @NotNull String @NotNull [] args
    ) {

        if (cmd.getName().equalsIgnoreCase("resume")) {

            if (args.length == 1) {
                return List.of("mobarmy");
            }
        }

        if (cmd.getName().equalsIgnoreCase("mobarmy")) {

            if (args.length == 1) {
                return List.of("resume");
            }
        }

        return null;
    }

    private Component lang(String path) {
        return plugin.getLanguageManager()
                .getComponent(path);
    }
}