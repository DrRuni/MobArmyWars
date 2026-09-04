package runi.myddns.mobarmywars.GUIs;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import runi.myddns.mobarmywars.Managers.Event.TeamEquipmentManager.EquipmentType;
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamEquipmentGUI implements Listener {

    private final MobArmyMain plugin;
    private final Map<Integer, SlotAction> slotActions = new HashMap<>();

    public TeamEquipmentGUI(MobArmyMain plugin) {
        this.plugin = plugin;
        registerSlots();
    }

    private void registerSlots() {
        slotActions.put(1, new SlotAction("blue", EquipmentType.HELMET));
        slotActions.put(10, new SlotAction("blue", EquipmentType.CHESTPLATE));
        slotActions.put(19, new SlotAction("blue", EquipmentType.LEGGINGS));
        slotActions.put(28, new SlotAction("blue", EquipmentType.BOOTS));
        slotActions.put(11, new SlotAction("blue", EquipmentType.SWORD));
        slotActions.put(18, new SlotAction("blue", EquipmentType.AXE));
        slotActions.put(20, new SlotAction("blue", EquipmentType.PICKAXE));
        slotActions.put(9, new SlotAction("blue", EquipmentType.BOW));
        slotActions.put(36, new SlotAction("blue", EquipmentType.FOOD));
        slotActions.put(37, new SlotAction("blue", EquipmentType.SHIELD));
        slotActions.put(38, new SlotAction("blue", EquipmentType.OP_FOOD));

        slotActions.put(7, new SlotAction("red", EquipmentType.HELMET));
        slotActions.put(16, new SlotAction("red", EquipmentType.CHESTPLATE));
        slotActions.put(25, new SlotAction("red", EquipmentType.LEGGINGS));
        slotActions.put(34, new SlotAction("red", EquipmentType.BOOTS));
        slotActions.put(15, new SlotAction("red", EquipmentType.SWORD));
        slotActions.put(26, new SlotAction("red", EquipmentType.AXE));
        slotActions.put(24, new SlotAction("red", EquipmentType.PICKAXE));
        slotActions.put(17, new SlotAction("red", EquipmentType.BOW));
        slotActions.put(44, new SlotAction("red", EquipmentType.FOOD));
        slotActions.put(43, new SlotAction("red", EquipmentType.SHIELD));
        slotActions.put(42, new SlotAction("red", EquipmentType.OP_FOOD));
    }

    public void open(Player player) {

        Inventory inv = Bukkit.createInventory(
                null,
                54,
                lang("team-equipment-gui.title")
        );

        fillBackground(inv);

        setTeamItems(inv, "blue");
        setTeamItems(inv, "red");

        inv.setItem(
                49,
                createItem(
                        Material.ARROW,
                        lang("team-equipment-gui.back.name"),
                        Component.empty(),
                        lang("team-equipment-gui.back.description")
                )
        );

        player.openInventory(inv);
    }

    private void setTeamItems(
            Inventory inv,
            String team
    ) {

        for (Map.Entry<Integer, SlotAction> entry : slotActions.entrySet()) {

            SlotAction action = entry.getValue();

            if (!action.team().equalsIgnoreCase(team)) {
                continue;
            }

            int level = plugin.getTeamEquipmentManager()
                    .getLevel(team, action.type());

            inv.setItem(
                    entry.getKey(),
                    createEquipmentItem(
                            team,
                            action.type(),
                            level
                    )
            );
        }
    }

    private ItemStack createEquipmentItem(
            String team,
            EquipmentType type,
            int level
    ) {

        Material material = plugin.getTeamEquipmentManager()
                .getDisplayMaterial(type, level);

        if (material == null) {
            material = Material.BARRIER;
        }

        Component teamName = team.equalsIgnoreCase("blue")
                ? lang("team-equipment-gui.team.blue")
                : lang("team-equipment-gui.team.red");

        Component displayName = teamName
                .append(Component.text(" - "))
                .append(
                        lang("team-equipment-gui.type." + getTypeKey(type))
                );

        Component levelName = plugin.getTeamEquipmentManager()
                .getLevelName(type, level);

        return createItem(
                material,
                displayName,
                Component.empty(),
                lang("team-equipment-gui.current")
                        .append(Component.space())
                        .append(levelName),
                Component.empty(),
                lang("team-equipment-gui.left-click"),
                lang("team-equipment-gui.right-click")
        );
    }

    private String getTypeKey(EquipmentType type) {

        return switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            case BOOTS -> "boots";
            case SWORD -> "sword";
            case AXE -> "axe";
            case PICKAXE -> "pickaxe";
            case BOW -> "bow";
            case FOOD -> "food";
            case SHIELD -> "shield";
            case OP_FOOD -> "op-food";
        };
    }

    private void fillBackground(Inventory inv) {

        ItemStack blue =
                createPane(Material.BLUE_STAINED_GLASS_PANE);

        ItemStack black =
                createPane(Material.BLACK_STAINED_GLASS_PANE);

        ItemStack red =
                createPane(Material.RED_STAINED_GLASS_PANE);

        for (int row = 0; row < 6; row++) {

            int start = row * 9;

            inv.setItem(start, blue);
            inv.setItem(start + 1, blue);
            inv.setItem(start + 2, blue);
            inv.setItem(start + 3, blue);

            inv.setItem(start + 4, black);

            inv.setItem(start + 5, red);
            inv.setItem(start + 6, red);
            inv.setItem(start + 7, red);
            inv.setItem(start + 8, red);
        }
    }

    private ItemStack createPane(Material material) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createItem(
            Material material,
            Component name,
            Component... loreLines
    ) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(name);

        List<Component> lore = new ArrayList<>();

        if (loreLines != null) {
            for (Component line : loreLines) {
                if (line != null) {
                    lore.add(line);
                }
            }
        }

        meta.lore(lore);

        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE
        );

        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!event.getView().title().equals(
                lang("team-equipment-gui.title")
        )) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (slot < 0
                || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        if (slot == 49) {

            Sounds.playBack(player);

            plugin.getTeamSettingsGUI()
                    .open(player);

            return;
        }

        SlotAction action = slotActions.get(slot);

        if (action == null) {
            return;
        }

        if (event.isRightClick()) {

            plugin.getTeamEquipmentManager()
                    .previousLevel(
                            action.team(),
                            action.type()
                    );

        } else {

            plugin.getTeamEquipmentManager()
                    .nextLevel(
                            action.team(),
                            action.type()
                    );
        }

        Sounds.playClick(player);

        Bukkit.getScheduler()
                .runTask(plugin, () -> open(player));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {

        if (event.getView().title().equals(
                lang("team-equipment-gui.title")
        )) {
            event.setCancelled(true);
        }
    }

    private Component lang(String path) {

        return plugin.getLanguageManager()
                .getComponent(path);
    }

    private record SlotAction(
            String team,
            EquipmentType type
    ) {
    }
}