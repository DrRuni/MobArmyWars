package runi.myddns.mobarmywars.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import runi.myddns.mobarmywars.MobArmyMain;

public class ArenaSummaryCommand implements CommandExecutor {

    private final MobArmyMain plugin;

    public ArenaSummaryCommand(MobArmyMain plugin) {
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
                            .getComponent("commands.arena-summary.player-only")
            );
            return true;
        }

        if (plugin.getArenaManager() == null) {
            player.sendMessage(
                    plugin.getLanguageManager()
                            .getComponent("commands.arena-summary.manager-unavailable")
            );
            return true;
        }

        plugin.getArenaManager().showArenaSummary(player);
        return true;
    }
}
