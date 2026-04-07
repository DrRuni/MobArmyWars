package runi.myddns.mobarmywars.Managers.World;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import runi.myddns.mobarmywars.MobArmyMain;

public class PlayerEffectManager {

    private final MobArmyMain plugin;

    public PlayerEffectManager(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void applyNightVision(Player player) {
        if (plugin.getWorldSettings().isNightVisionEnabled()) {
            player.addPotionEffect(
                    new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false)
            );
        } else {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }
}
