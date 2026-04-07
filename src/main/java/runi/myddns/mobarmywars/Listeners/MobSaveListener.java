package runi.myddns.mobarmywars.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import runi.myddns.mobarmywars.Managers.Event.MobSaveManager;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MobSaveListener implements Listener {

    private final MobSaveManager mobSaveManager;
    private final MobArmyMain plugin;
    private final Set<UUID> recentlyHandled = new HashSet<>();

    public MobSaveListener(MobArmyMain plugin, MobSaveManager mobSaveManager) {
        this.plugin = plugin;
        this.mobSaveManager = mobSaveManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        LivingEntity mob = event.getEntity();
        UUID uuid = mob.getUniqueId();

        if (recentlyHandled.contains(uuid)) return;
        recentlyHandled.add(uuid);

        Bukkit.getScheduler().runTaskLater(plugin,
                () -> recentlyHandled.remove(uuid), 5L);

        if (!(event.getEntity().getKiller() instanceof Player player)) return;

        mobSaveManager.handleMobKill(player, mob);
    }
}