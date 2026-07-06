package runi.myddns.mobarmywars.Utils;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class WorldMessageManager {

    private static final String[] ARENA_WORLDS = {
            "world_mobarmylobby",
            "world_rot",
            "world_blau"
    };

    public static void sendToArenaWorlds(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isArenaWorld(p.getWorld().getName())) {
                p.sendMessage(message);
            }
        }
    }

    public static void sendToArenaWorlds(TextComponent component) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isArenaWorld(p.getWorld().getName())) {
                p.spigot().sendMessage(component);
            }
        }
    }

    public static void sendTitleToArenaWorlds(String title, String subtitle,
                                                 int fadeIn, int stay, int fadeOut) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            String world = p.getWorld().getName().toLowerCase();
            if (world.contains("mobarena") || world.contains("rot") || world.contains("blau")) {
                p.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            }
        }
    }

    public static void playSoundInArenaWorlds(Location loc, Sound sound, float volume, float pitch) {
        if (loc == null || loc.getWorld() == null) return;

        String world = loc.getWorld().getName().toLowerCase();

        if (!world.contains("mobarena") && !world.contains("rot") && !world.contains("blau")) {
            return;
        }

        for (Player p : loc.getWorld().getPlayers()) {
            p.playSound(loc, sound, volume, pitch);
        }
    }

    private static boolean isArenaWorld(String worldName) {
        String w = worldName.toLowerCase();
        for (String arenaWorld : ARENA_WORLDS) {
            if (w.equals(arenaWorld)) return true;
        }
        return false;
    }
}
