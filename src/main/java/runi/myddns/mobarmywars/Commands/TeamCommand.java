package runi.myddns.mobarmywars.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.Collections;
import java.util.List;

public class TeamCommand implements CommandExecutor, TabCompleter {

    private final MobArmyMain plugin;

    public TeamCommand(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("❌ Nur Spieler können Teams betreten!");
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
                player.sendMessage(ChatColor.RED + "❌ Unbekannter Subcommand!");
                sendUsage(player);
            }
        }

        return true;
    }

    private void handleJoin(Player player, String[] args) {

        if (args.length < 2) {
            deny(player, "❌ Bitte gib ein Team an: rot oder blau!");
            return;
        }

        if (isInTeamWorld(player)) {
            deny(player, "❌ Du kannst in der Teamwelt kein Team wechseln!");
            return;
        }

        if (!isInArenaWorld(player)) {
            deny(player, "⚠ Du kannst Teams nur in der MobArmy-Lobby wechseln!");
            return;
        }

        if (plugin.getTeamManager().isInTeam(player)) {
            deny(player, "❌ Du bist bereits in einem Team! Benutze /team leave");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "rot" -> {
                plugin.getTeamManager().assignTeam(player, "Rot");
                player.sendMessage("");
                player.sendMessage(ChatColor.RED + "✔ Du bist nun im Team Rot!");
            }
            case "blau" -> {
                plugin.getTeamManager().assignTeam(player, "Blau");
                player.sendMessage("");
                player.sendMessage(ChatColor.BLUE + "✔ Du bist nun im Team Blau!");
            }
            default -> player.sendMessage(ChatColor.RED + "❌ Ungültiges Team. Benutze: rot oder blau.");
        }
    }

    private void handleLeave(Player player) {

        if (isInTeamWorld(player)) {
            deny(player, "❌ Du kannst dein Team hier nicht verlassen!");
            return;
        }

        if (!plugin.getTeamManager().isInTeam(player)) {
            player.sendMessage("");
            player.sendMessage(ChatColor.GRAY + "Du bist keinem Team zugeordnet.!");
            return;
        }

        plugin.getTeamManager().removePlayerFromTeam(player);
        plugin.getBundleManager().removeTeamBundle(player);

        player.sendMessage(ChatColor.GRAY + "❌ Du hast dein Team verlassen.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return List.of("join", "leave").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            return List.of("rot", "blau").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }

    private boolean isInArenaWorld(Player player) {
        return player.getWorld().getName().equalsIgnoreCase("world_mobarmylobby");
    }

    private boolean isInTeamWorld(Player player) {
        String world = player.getWorld().getName().toLowerCase();
        return world.equals("world_rot") || world.equals("world_blau");
    }

    private void deny(Player player, String message) {
        player.sendMessage(ChatColor.RED + message);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.YELLOW + "⚠ Benutzung:");
        player.sendMessage(ChatColor.GRAY + "/team join rot");
        player.sendMessage(ChatColor.GRAY + "/team join blau");
        player.sendMessage(ChatColor.GRAY + "/team leave");
    }
}