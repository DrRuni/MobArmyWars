package runi.myddns.mobarmywars.Listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import runi.myddns.mobarmywars.MobArmyMain;

public class UltraHardcoreListener implements Listener {

    private final MobArmyMain plugin;

    public UltraHardcoreListener(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRegainHealth(EntityRegainHealthEvent event) {

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (!plugin.getWorldSettings()
                .getDifficulty()
                .equalsIgnoreCase("ultra-ultra-hardcore")) {
            return;
        }

        if (event.getRegainReason()
                == EntityRegainHealthEvent.RegainReason.SATIATED) {

            event.setCancelled(true);
        }
    }
}
