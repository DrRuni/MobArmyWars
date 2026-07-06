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
import java.util.List;
import java.util.stream.Collectors;

public class ArenaMobTargetListener implements Listener {

    private final MobArmyMain plugin;

    public ArenaMobTargetListener(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity mob)) return;

        Byte isArenaMob = mob.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "arenaMob"),
                PersistentDataType.BYTE
        );
        if (isArenaMob == null || isArenaMob != (byte) 1) return;

        String enemyTeam = mob.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "enemyTeam"),
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
                new NamespacedKey(plugin, "arenaMob"),
                PersistentDataType.BYTE
        );
        if (isArenaMob == null || isArenaMob != (byte) 1) return;

        String enemyTeam = creature.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "enemyTeam"),
                PersistentDataType.STRING
        );
        if (enemyTeam == null) return;

        List<Player> enemies = Bukkit.getOnlinePlayers().stream()
                .filter(p -> {
                    String team = plugin.getTeamManager().getPlayerTeam(p);
                    return team != null && team.equalsIgnoreCase(enemyTeam);
                })
                .collect(Collectors.toList());

        if (enemies.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        Player nearest = enemies.stream()
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(creature.getLocation())))
                .orElse(null);

        if (nearest != null) {
            creature.setTarget(nearest);
            event.setTarget(nearest);
        }
    }

    @EventHandler
    public void onMobCombust(EntityCombustEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living)) return;

        Byte isArenaMob = living.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "arenaMob"),
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
                new NamespacedKey(plugin, "arenaMob"),
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
