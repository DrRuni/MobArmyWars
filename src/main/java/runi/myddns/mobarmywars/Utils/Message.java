package runi.myddns.mobarmywars.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Message {

    public static void sendChatToAll(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(message);
        }
    }

    public static void sendToPlayer(Player player, String message) {
        if (player == null) {
            return;
        }
        player.sendMessage(message);
    }
}
