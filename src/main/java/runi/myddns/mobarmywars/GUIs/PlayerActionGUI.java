package runi.myddns.mobarmywars.GUIs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.util.List;

public class PlayerActionGUI implements Listener {

    private final MobArmyMain plugin;

    public PlayerActionGUI(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    private String getTitle(Player target) {
        return ChatColor.BLUE + "Spieler: " + ChatColor.YELLOW + target.getName();
    }

    public void open(Player viewer, Player target) {
        Inventory inv = Bukkit.createInventory(null, 36, getTitle(target));

        inv.setItem(10, createItem(
                Material.GOLDEN_APPLE,
                ChatColor.GREEN + "Heilen",
                List.of(ChatColor.GRAY + "Heilt den Spieler vollständig",
                        ChatColor.GRAY + "und entfernt alle Effekte")
        ));

        inv.setItem(12, createItem(
                Material.IRON_SWORD,
                ChatColor.RED + "Töten",
                List.of(ChatColor.GRAY + "Tötet den ausgewählten Spieler")
        ));

        inv.setItem(14, createItem(
                Material.BLUE_BANNER,
                ChatColor.BLUE + "Team Blau",
                List.of(ChatColor.GRAY + "Verschiebt den Spieler ins blaue Team")
        ));

        inv.setItem(16, createItem(
                Material.RED_BANNER,
                ChatColor.RED + "Team Rot",
                List.of(ChatColor.GRAY + "Verschiebt den Spieler ins rote Team")
        ));

        inv.setItem(31, createItem(
                Material.ARROW,
                ChatColor.DARK_AQUA + "Zurück",
                List.of(ChatColor.GRAY + "Zurück zur Spielerliste")
        ));

        viewer.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = ChatColor.stripColor(event.getView().getTitle());

        if (!title.startsWith("Spieler: ")) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String itemName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        String targetName = title.replace("Spieler: ", "").trim();

        Player target = Bukkit.getPlayerExact(targetName);

        if (itemName.equalsIgnoreCase("Zurück")) {
            Sounds.playBack(viewer);

            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> plugin.getPlayerGUI().open(viewer),
                    1L
            );
            return;
        }

        if (target == null || !target.isOnline()) {
            viewer.sendMessage(ChatColor.RED + "Der Spieler ist nicht mehr online!");
            Sounds.playDanger(viewer);
            return;
        }

        switch (itemName.toLowerCase()) {
            case "heilen" -> {
                Sounds.playClick(viewer);
                healPlayer(target);

                viewer.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.YELLOW + target.getName()
                        + ChatColor.GRAY + " wurde geheilt und alle Effekte wurden entfernt.");

                target.sendMessage(ChatColor.GREEN + "✔ Du wurdest vollständig geheilt.");

                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            }

            case "töten" -> {
                Sounds.playClick(viewer);
                killPlayer(viewer, target);

                Bukkit.broadcastMessage(ChatColor.RED + "☠ " + ChatColor.YELLOW + target.getName()
                        + ChatColor.GRAY + " wurde von "
                        + ChatColor.GOLD + viewer.getName()
                        + ChatColor.GRAY + " getötet.");
            }

            case "team blau" -> {
                Sounds.playClick(viewer);
                plugin.getTeamManager().assignTeam(target, "Blau");

                viewer.sendMessage(ChatColor.BLUE + "✔ " + ChatColor.YELLOW + target.getName()
                        + ChatColor.GRAY + " wurde ins Team Blau verschoben.");
            }

            case "team rot" -> {
                Sounds.playClick(viewer);
                plugin.getTeamManager().assignTeam(target, "Rot");

                viewer.sendMessage(ChatColor.RED + "✔ " + ChatColor.YELLOW + target.getName()
                        + ChatColor.GRAY + " wurde ins Team Rot verschoben.");
            }
        }
    }

    private void healPlayer(Player target) {
        double maxHealth = 20.0;

        if (target.getAttribute(Attribute.MAX_HEALTH) != null) {
            maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
        }

        target.setHealth(maxHealth);
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.setExhaustion(0f);
        target.setFireTicks(0);
        target.setFreezeTicks(0);
        target.setAbsorptionAmount(0);

        for (PotionEffect effect : target.getActivePotionEffects()) {
            target.removePotionEffect(effect.getType());
        }
    }

    private void killPlayer(Player viewer, Player target) {
        target.setInvulnerable(false);
        target.setAllowFlight(false);
        target.setFlying(false);

        if (target.getGameMode() != GameMode.SURVIVAL) {
            target.setGameMode(GameMode.SURVIVAL);
        }

        target.damage(1000.0, viewer);

        if (!target.isDead()) {
            target.setHealth(0.0);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = ChatColor.stripColor(event.getView().getTitle());

        if (title.startsWith("Spieler: ")) {
            event.setCancelled(true);
        }
    }
}