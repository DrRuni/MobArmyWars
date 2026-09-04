package runi.myddns.mobarmywars.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.Collections;
import java.util.List;

public class OptionenCommand implements CommandExecutor, TabCompleter {

    private final MobArmyMain plugin;

    public OptionenCommand(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    plugin.getLanguageManager()
                            .getComponent("commands.options.player-only")
            );
            return true;
        }

        plugin.getOptionenGUI().open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String @NotNull [] args
    ) {
        return Collections.emptyList();
    }
}