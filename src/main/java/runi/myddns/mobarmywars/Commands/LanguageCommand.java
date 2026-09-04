package runi.myddns.mobarmywars.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import runi.myddns.mobarmywars.MobArmyMain;

public class LanguageCommand implements CommandExecutor {

    private final MobArmyMain plugin;

    public LanguageCommand(MobArmyMain plugin) {
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
                            .getComponent("commands.language.player-only")
            );

            return true;
        }

        plugin.getLanguageSelectionGUI().open(player);

        return true;
    }
}