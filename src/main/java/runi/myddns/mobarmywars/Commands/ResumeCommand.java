package runi.myddns.mobarmywars.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.List;

public class ResumeCommand implements CommandExecutor, TabCompleter {

    private final MobArmyMain plugin;

    public ResumeCommand(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "❌ Nur Spieler können diesen Befehl nutzen!");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "❌ Nur OPs dürfen diesen Befehl ausführen!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("resume")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("mobarmy")) {
                plugin.getEventResume().resumeEvent();
                return true;
            }

            player.sendMessage(ChatColor.RED + "Verwendung: /resume mobarmy");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("mobarmy")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("resume")) {
                plugin.getEventResume().resumeEvent();
                return true;
            }

            player.sendMessage(ChatColor.RED + "Verwendung: /mobarmy resume");
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

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
}