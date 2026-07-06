package runi.myddns.mobarmywars.Utils;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class Sounds {

    private Sounds() {}

    public static void playClick(Player player) {
        play(player, Sound.UI_BUTTON_CLICK, 0.3f , 1f);
    }

    public static void playBack(Player player) {
        play(player, Sound.UI_BUTTON_CLICK, 0.3f , 0.9f);
    }

    public static void playReset(Player player) {play(player, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 0.9f);}

    public static void playDanger(Player player) {
        play(player, Sound.BLOCK_ANVIL_PLACE, 0.6f, 0.6f);
    }

    public static void playCountdown(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
    }

    public static void playGo(Player player) {
        play(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    public static void playTeleport(Player player) {
        if (player == null) return;
        playTeleport(player, player.getLocation());
    }

    public static void playTeleport(Player player, Location loc) {
        if (player == null || loc == null) return;
        player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }

    private static void play(Player player, Sound sound, float vol, float pitch) {
        if (player == null) return;
        player.playSound(player.getLocation(), sound, vol, pitch);
    }
}