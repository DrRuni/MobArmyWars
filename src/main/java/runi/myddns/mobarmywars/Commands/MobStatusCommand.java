package runi.myddns.mobarmywars.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import runi.myddns.mobarmywars.Managers.Event.MobSaveManager;
import runi.myddns.mobarmywars.Managers.Event.TeamManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MobStatusCommand implements CommandExecutor, TabCompleter {

    private final MobSaveManager mobSaveManager;
    private final TeamManager teamManager;

    public MobStatusCommand(MobSaveManager mobSaveManager, TeamManager teamManager) {
        this.mobSaveManager = mobSaveManager;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können diesen Befehl ausführen.");
            return true;
        }

        if (!teamManager.isArenaWorld(player)) {
            player.sendMessage(ChatColor.RED +
                    "❌ Dieser Befehl kann nur in den MobArmy-Welten verwendet werden!");
            return true;
        }

        if (args.length == 1 && player.hasPermission("mobarmy.mobstatus.admin")) {
            String team = normalizeTeam(args[0]);

            if (team == null) {
                player.sendMessage(ChatColor.RED +
                        "❌ Ungültiges Team. Benutze: rot oder blau");
                return true;
            }

            showMobStatus(player, team);
            return true;
        }

        String team = teamManager.getPlayerTeam(player);

        if (team.equalsIgnoreCase("Kein Team")) {
            player.sendMessage(ChatColor.RED + "❌ Du bist in keinem Team!");
            return true;
        }

        showMobStatus(player, team);
        return true;
    }

    private void showMobStatus(Player player, String team) {

        Map<String, Integer> mobs =
                mobSaveManager.getMobKillsForTeam(team);

        if (mobs == null || mobs.isEmpty()) {
            player.sendMessage(ChatColor.RED +
                    "Team " + team + " hat keine gespeicherten Mobs.");
            return;
        }

        ChatColor color =
                team.equalsIgnoreCase("Rot") ? ChatColor.RED : ChatColor.BLUE;

        player.sendMessage(color +
                "[MobArmyWars] Team " + team + " – gespeicherte Mobs:");

        for (Map.Entry<String, Integer> entry : mobs.entrySet()) {
            player.sendMessage(ChatColor.GRAY +
                    "- " + entry.getValue() + "x " + entry.getKey());
        }
    }

    private String normalizeTeam(String input) {
        return switch (input.toLowerCase()) {
            case "rot" -> "Rot";
            case "blau" -> "Blau";
            default -> null;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {

        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (!player.hasPermission("mobarmy.mobstatus.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return List.of("rot", "blau");
        }

        return Collections.emptyList();
    }
}