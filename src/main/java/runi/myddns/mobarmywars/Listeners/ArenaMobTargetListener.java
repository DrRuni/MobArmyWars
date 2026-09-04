package runi.myddns.mobarmywars.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.Comparator;

public class ArenaMobTargetListener implements Listener {

    private final MobArmyMain plugin;

    private final NamespacedKey arenaMobKey;
    private final NamespacedKey enemyTeamKey;

    public ArenaMobTargetListener(MobArmyMain plugin) {
        this.plugin = plugin;
        this.arenaMobKey = new NamespacedKey(plugin, "arenaMob");
        this.enemyTeamKey = new NamespacedKey(plugin, "enemyTeam");
    }

    @EventHandler
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity mob)) return;

        Byte isArenaMob = mob.getPersistentDataContainer().get(
                arenaMobKey,
                PersistentDataType.BYTE
        );
        if (isArenaMob == null || isArenaMob != (byte) 1) return;

        String enemyTeam = mob.getPersistentDataContainer().get(
                enemyTeamKey,
                PersistentDataType.STRING
        );

        if (enemyTeam == null) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getTarget() instanceof Player target)) {
            event.setCancelled(true);
            return;
        }

        String team = plugin.getTeamManager().getPlayerTeam(target);
        if (team == null || !team.equalsIgnoreCase(enemyTeam)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTargetCheck(EntityTargetLivingEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Creature creature)) return;

        Byte isArenaMob = creature.getPersistentDataContainer().get(
                arenaMobKey,
                PersistentDataType.BYTE
        );
        if (isArenaMob == null || isArenaMob != (byte) 1) return;

        String enemyTeam = creature.getPersistentDataContainer().get(
                enemyTeamKey,
                PersistentDataType.STRING
        );
        if (enemyTeam == null) return;

        Player nearest = Bukkit.getOnlinePlayers()
                .stream()
                .filter(player -> {
                    String team = plugin.getTeamManager().getPlayerTeam(player);
                    return team != null
                            && team.equalsIgnoreCase(enemyTeam);
                })
                .min(Comparator.comparingDouble(
                        player -> player.getLocation()
                                .distanceSquared(creature.getLocation())
                ))
                .orElse(null);

        if (nearest == null) {
            event.setCancelled(true);
            return;
        }

        creature.setTarget(nearest);
        event.setTarget(nearest);
    }

    @EventHandler
    public void onMobCombust(EntityCombustEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living)) return;

        Byte isArenaMob = living.getPersistentDataContainer().get(
                arenaMobKey,
                PersistentDataType.BYTE
        );

        if (isArenaMob != null && isArenaMob == (byte) 1) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Byte isArenaMob = entity.getPersistentDataContainer().get(
                arenaMobKey,
                PersistentDataType.BYTE
        );
        if (isArenaMob == null || isArenaMob != (byte) 1) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        String team = plugin.getTeamManager().getPlayerTeam(killer);
        if (team != null) {
            plugin.getArenaManager().getScoreboardManager().addKill(team);
        }
    }
}
