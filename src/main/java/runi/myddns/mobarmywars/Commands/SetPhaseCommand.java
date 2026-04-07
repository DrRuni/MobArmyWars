package runi.myddns.mobarmywars.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Managers.World.ResumeManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetPhaseCommand implements CommandExecutor, TabCompleter {

    private final MobArmyMain plugin;

    public SetPhaseCommand(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "❌ Nur OPs dürfen diesen Befehl benutzen.");
            return true;
        }

        if (args.length != 1) {
            sendUsage(sender, label);
            return true;
        }

        int phase = parsePhase(args[0]);

        if (phase == -1) {
            sender.sendMessage(ChatColor.RED + "❌ Ungültige Phase: " + args[0]);
            sendUsage(sender, label);
            return true;
        }

        plugin.getEventResume().savePhase(phase);

        sender.sendMessage(ChatColor.GREEN + "✔ Phase wurde gesetzt auf: " + getPhaseName(phase));
        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Verwendung: /" + label + " <phase>");
        sender.sendMessage(ChatColor.GRAY + "Phasen:");
        sender.sendMessage(ChatColor.GRAY + " - lobby oder 0");
        sender.sendMessage(ChatColor.GRAY + " - teamwelt oder 1");
        sender.sendMessage(ChatColor.GRAY + " - waveauswahl oder 2");
        sender.sendMessage(ChatColor.GRAY + " - arena oder 3");
    }

    private int parsePhase(String input) {
        switch (input.toLowerCase()) {
            case "0":
            case "lobby":
                return ResumeManager.PHASE_LOBBY;

            case "1":
            case "teamwelt":
                return ResumeManager.PHASE_TEAMWELT;

            case "2":
            case "wave":
            case "waveauswahl":
                return ResumeManager.PHASE_WAVEAUSWAHL;

            case "3":
            case "arena":
                return ResumeManager.PHASE_ARENA;

            default:
                return -1;
        }
    }

    private String getPhaseName(int phase) {
        switch (phase) {
            case ResumeManager.PHASE_LOBBY:
                return "Lobby";
            case ResumeManager.PHASE_TEAMWELT:
                return "Teamwelt";
            case ResumeManager.PHASE_WAVEAUSWAHL:
                return "Wave-Auswahl";
            case ResumeManager.PHASE_ARENA:
                return "Arena";
            default:
                return "Unbekannt";
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp()) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> options = Arrays.asList(
                    "lobby",
                    "teamwelt",
                    "waveauswahl",
                    "arena",
                    "0",
                    "1",
                    "2",
                    "3"
            );

            List<String> result = new ArrayList<>();
            String input = args[0].toLowerCase();

            for (String option : options) {
                if (option.toLowerCase().startsWith(input)) {
                    result.add(option);
                }
            }

            return result;
        }

        return new ArrayList<>();
    }
}