package runi.myddns.mobarmywars.GUIs;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerActionGUI implements Listener {

    private final MobArmyMain plugin;

    public PlayerActionGUI(MobArmyMain plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, Player target) {

        PlayerActionHolder holder = new PlayerActionHolder(
                target.getUniqueId(),
                ViewType.ACTIONS
        );

        Inventory inv = Bukkit.createInventory(
                holder,
                45,
                langPlayer(
                        "player-action-gui.title",
                        target.getName()
                )
        );

        holder.setInventory(inv);

        // Heilen
        inv.setItem(
                10,
                createItem(
                        Material.GOLDEN_APPLE,
                        lang("player-action-gui.heal.name"),
                        List.of(
                                lang("player-action-gui.heal.description-1"),
                                lang("player-action-gui.heal.description-2"),
                                Component.empty(),
                                lang("player-action-gui.heal.action")
                        )
                )
        );

        // Töten
        inv.setItem(
                12,
                createItem(
                        Material.IRON_SWORD,
                        lang("player-action-gui.kill.name"),
                        List.of(
                                lang("player-action-gui.kill.description"),
                                Component.empty(),
                                lang("player-action-gui.kill.action")
                        )
                )
        );

        // Team Blau
        inv.setItem(
                14,
                createItem(
                        Material.BLUE_BANNER,
                        lang("player-action-gui.team-blue.name"),
                        List.of(
                                lang("player-action-gui.team-blue.description-1"),
                                lang("player-action-gui.team-blue.description-2"),
                                Component.empty(),
                                lang("player-action-gui.team-blue.action")
                        )
                )
        );

        // Team Rot
        inv.setItem(
                16,
                createItem(
                        Material.RED_BANNER,
                        lang("player-action-gui.team-red.name"),
                        List.of(
                                lang("player-action-gui.team-red.description-1"),
                                lang("player-action-gui.team-red.description-2"),
                                Component.empty(),
                                lang("player-action-gui.team-red.action")
                        )
                )
        );

        // Inventar ansehen
        inv.setItem(
                22,
                createItem(
                        Material.CHEST,
                        lang("player-action-gui.inventory.name"),
                        List.of(
                                lang("player-action-gui.inventory.description-1"),
                                lang("player-action-gui.inventory.description-2"),
                                Component.empty(),
                                lang("player-action-gui.inventory.action")
                        )
                )
        );

        inv.setItem(
                40,
                createBackButton()
        );

        viewer.openInventory(inv);
    }

    private void openTargetInventory(
            Player viewer,
            Player target
    ) {

        PlayerActionHolder holder = new PlayerActionHolder(
                target.getUniqueId(),
                ViewType.INVENTORY
        );

        Inventory inv = Bukkit.createInventory(
                holder,
                54,
                langPlayer(
                        "player-action-gui.inventory-title",
                        target.getName()
                )
        );

        holder.setInventory(inv);

        ItemStack[] contents =
                target.getInventory().getContents();

        for (int i = 0;
             i < contents.length && i < 41;
             i++) {

            inv.setItem(i, contents[i]);
        }

        inv.setItem(
                49,
                createBackButton()
        );

        viewer.openInventory(inv);
    }

    private ItemStack createItem(
            Material material,
            Component name,
            List<Component> lore
    ) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(name);

        if (lore != null) {
            meta.lore(lore);
        }

        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE
        );

        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createBackButton() {
        return createItem(
                Material.ARROW,
                lang("player-action-gui.back"),
                null
        );
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        Inventory top =
                event.getView().getTopInventory();

        if (!(top.getHolder()
                instanceof PlayerActionHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked()
                instanceof Player viewer)) {
            return;
        }

        if (event.getRawSlot() < 0
                || event.getRawSlot() >= top.getSize()) {
            return;
        }

        int slot = event.getRawSlot();

        UUID targetId = holder.getTargetId();

        Player target =
                Bukkit.getPlayer(targetId);

        if (holder.getViewType() == ViewType.INVENTORY) {

            if (slot != 49) {
                return;
            }

            Sounds.playBack(viewer);

            if (target != null && target.isOnline()) {

                Bukkit.getScheduler().runTaskLater(
                        plugin,
                        () -> open(viewer, target),
                        1L
                );

            } else {

                viewer.sendMessage(
                        lang("player-action-gui.messages.offline")
                );

                Sounds.playDanger(viewer);

                Bukkit.getScheduler().runTaskLater(
                        plugin,
                        () -> plugin.getPlayerGUI().open(viewer),
                        1L
                );
            }

            return;
        }

        if (slot == 40) {

            Sounds.playBack(viewer);

            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> plugin.getPlayerGUI().open(viewer),
                    1L
            );

            return;
        }

        if (target == null || !target.isOnline()) {

            viewer.sendMessage(
                    lang("player-action-gui.messages.offline")
            );

            Sounds.playDanger(viewer);
            return;
        }

        switch (slot) {

            case 10 -> {

                Sounds.playClick(viewer);

                healPlayer(target);

                viewer.sendMessage(
                        langPlayer(
                                "player-action-gui.messages.healed-viewer",
                                target.getName()
                        )
                );

                target.sendMessage(
                        lang("player-action-gui.messages.healed-target")
                );

                target.playSound(
                        target.getLocation(),
                        Sound.ENTITY_PLAYER_LEVELUP,
                        1.0f,
                        1.2f
                );
            }

            case 12 -> {

                Sounds.playClick(viewer);

                killPlayer(
                        viewer,
                        target
                );

                Component message = killedMessage(
                        target,
                        viewer
                );

                for (Player online :
                        Bukkit.getOnlinePlayers()) {

                    online.sendMessage(message);
                }
            }

            case 14 -> {

                Sounds.playClick(viewer);

                plugin.getTeamManager()
                        .assignTeam(target, "Blau");

                viewer.sendMessage(
                        langPlayer(
                                "player-action-gui.messages.team-blue",
                                target.getName()
                        )
                );
            }

            case 16 -> {

                Sounds.playClick(viewer);

                plugin.getTeamManager()
                        .assignTeam(target, "Rot");

                viewer.sendMessage(
                        langPlayer(
                                "player-action-gui.messages.team-red",
                                target.getName()
                        )
                );
            }

            case 22 -> {

                Sounds.playClick(viewer);

                openTargetInventory(
                        viewer,
                        target
                );
            }
        }
    }

    private void healPlayer(Player target) {

        double maxHealth = 20.0;

        AttributeInstance maxHealthAttribute =
                target.getAttribute(Attribute.MAX_HEALTH);

        if (maxHealthAttribute != null) {
            maxHealth = maxHealthAttribute.getValue();
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

    private void killPlayer(
            Player viewer,
            Player target
    ) {

        target.setInvulnerable(false);
        target.setAllowFlight(false);
        target.setFlying(false);

        if (target.getGameMode()
                != GameMode.SURVIVAL) {

            target.setGameMode(
                    GameMode.SURVIVAL
            );
        }

        target.damage(
                1000.0,
                viewer
        );

        if (!target.isDead()) {
            target.setHealth(0.0);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {

        Inventory top =
                event.getView().getTopInventory();

        if (top.getHolder()
                instanceof PlayerActionHolder) {

            event.setCancelled(true);
        }
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }

    private Component langPlayer(
            String path,
            Object value
    ) {

        return plugin.getLanguageManager()
                .getComponent(
                        path,
                        "player",
                        value
                );
    }

    private Component killedMessage(
            Player target,
            Player viewer
    ) {

        return plugin.getLanguageManager()
                .getComponent(
                        "player-action-gui.messages.killed",
                        Map.of(
                                "player", Component.text(target.getName()),
                                "viewer", Component.text(viewer.getName())
                        )
                );
    }

    private enum ViewType {
        ACTIONS,
        INVENTORY
    }

    private static class PlayerActionHolder
            implements InventoryHolder {

        private final UUID targetId;
        private final ViewType viewType;

        private Inventory inventory;

        private PlayerActionHolder(
                UUID targetId,
                ViewType viewType
        ) {
            this.targetId = targetId;
            this.viewType = viewType;
        }

        private UUID getTargetId() {
            return targetId;
        }

        private ViewType getViewType() {
            return viewType;
        }

        private void setInventory(
                Inventory inventory
        ) {
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}