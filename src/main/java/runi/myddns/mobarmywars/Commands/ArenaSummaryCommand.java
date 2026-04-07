package runi.myddns.mobarmywars.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import runi.myddns.mobarmywars.MobArmyMain;

public class ArenaSummaryCommand implements CommandExecutor {

    private final MobArmyMain plugin;

    public ArenaSummaryCommand(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Command ist nur für Spieler.");
            return true;
        }

        if (plugin.getArenaManager() == null) {
            player.sendMessage("§cArenaManager nicht verfügbar.");
            return true;
        }

        plugin.getArenaManager().showArenaSummary(player);
        return true;
    }
}
