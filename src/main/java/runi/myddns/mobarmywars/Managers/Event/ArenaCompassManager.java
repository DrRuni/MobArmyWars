package runi.myddns.mobarmywars.Managers.Event;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import runi.myddns.mobarmywars.MobArmyMain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArenaCompassManager implements Listener {

    private final MobArmyMain plugin;
    private final NamespacedKey compassKey;
    private final Map<Player, Long> cooldowns = new HashMap<>();

    private static final int RADIUS = 50;
    private static final int DURATION_SECONDS = 5;
    private static final int COOLDOWN_SECONDS = 20;

    public ArenaCompassManager(MobArmyMain plugin) {
        this.plugin = plugin;
        this.compassKey = new NamespacedKey(plugin, "arena_compass");
    }

    public ItemStack createCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        meta.setDisplayName(ChatColor.AQUA + "Monster-Kompass");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Zeigt für " + DURATION_SECONDS + " Sekunden");
        lore.add(ChatColor.GRAY + "alle Monster im Umkreis von " + RADIUS + " Blöcken.");
        lore.add(ChatColor.GRAY + "Verlangsamt und blendet dich kurz.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Linksklick oder Rechtsklick zum Aktivieren");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isArenaCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        Byte value = meta.getPersistentDataContainer().get(compassKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public void giveMonsterCompass(Player player) {
        if (player == null) return;
        if (!plugin.getWorldSettings().isArenaCompassEnabled()) return;

        addMonsterCompass(player);
    }

    public void addMonsterCompass(Player player) {
        if (player == null) return;

        for (ItemStack content : player.getInventory().getContents()) {
            if (isArenaCompass(content)) {
                return;
            }
        }

        ItemStack compass = createCompass();
        var leftover = player.getInventory().addItem(compass);

        if (!leftover.isEmpty()) {
            leftover.values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item)
            );

            player.sendTitle(
                    ChatColor.DARK_RED + "⚠ Inventar voll!",
                    ChatColor.RED + "Monster-Dedector wurde vor dir gedroppt.",
                    10, 50, 10
            );

            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.7f);
        }
    }

    public void removeArenaCompass(Player player) {
        if (player == null) return;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isArenaCompass(item)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    @EventHandler
    public void onUseCompass(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR &&
                action != Action.RIGHT_CLICK_BLOCK &&
                action != Action.LEFT_CLICK_AIR &&
                action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isArenaCompass(item)) return;

        event.setCancelled(true);

        long now = System.currentTimeMillis();
        long lastUse = cooldowns.getOrDefault(player, 0L);

        long cooldownMillis = COOLDOWN_SECONDS * 1000L;
        long remaining = (lastUse + cooldownMillis) - now;

        if (remaining > 0) {
            long secondsLeft = (long) Math.ceil(remaining / 1000.0);
            player.sendMessage(ChatColor.RED + "❌ Der Dedector ist noch " + secondsLeft + "s im Cooldown.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            return;
        }

        cooldowns.put(player, now);

        player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.6f, 0.8f);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                DURATION_SECONDS * 20,
                1,
                false,
                false,
                true
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS,
                DURATION_SECONDS * 20,
                2,
                false,
                false,
                true
        ));

        int affected = 0;

        for (Entity entity : player.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living instanceof Player) continue;

            Location playerLoc = player.getLocation();
            Location entityLoc = living.getLocation();

            if (!playerLoc.getWorld().equals(entityLoc.getWorld())) continue;
            if (playerLoc.distanceSquared(entityLoc) > (RADIUS * RADIUS)) continue;

            living.setGlowing(true);

            living.getWorld().spawnParticle(
                    Particle.ENCHANT,
                    living.getLocation().add(0, 1, 0),
                    10,
                    0.4, 0.6, 0.4,
                    0.02
            );

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (living.isValid()) {
                    living.setGlowing(false);
                }
            }, DURATION_SECONDS * 20L);

            affected++;
        }

        if (affected == 0) {
            player.sendMessage(ChatColor.GRAY + "Keine Lebewesen im Umkreis von " + RADIUS + " Blöcken gefunden.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
        }
    }
}
