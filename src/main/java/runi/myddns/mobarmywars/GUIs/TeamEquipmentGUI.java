package runi.myddns.mobarmywars.GUIs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import runi.myddns.mobarmywars.MobArmyMain;
import runi.myddns.mobarmywars.Managers.Event.TeamEquipmentManager.EquipmentType;
import runi.myddns.mobarmywars.Utils.Sounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamEquipmentGUI implements Listener {

    private final MobArmyMain plugin;

    private static final String TITLE = ChatColor.BLUE + "Team Equipment Einstellungen";

    private final Map<Integer, SlotAction> slotActions = new HashMap<>();

    public TeamEquipmentGUI(MobArmyMain plugin) {
        this.plugin = plugin;
        registerSlots();
    }

    private void registerSlots() {
        // Blau links - Rüstung weiter links
        slotActions.put(1, new SlotAction("blue", EquipmentType.HELMET));
        slotActions.put(10, new SlotAction("blue", EquipmentType.CHESTPLATE));
        slotActions.put(19, new SlotAction("blue", EquipmentType.LEGGINGS));
        slotActions.put(28, new SlotAction("blue", EquipmentType.BOOTS));

        // Blau links - Waffen/Werkzeuge weiter links
        slotActions.put(11, new SlotAction("blue", EquipmentType.SWORD));
        slotActions.put(18, new SlotAction("blue", EquipmentType.AXE));
        slotActions.put(20, new SlotAction("blue", EquipmentType.PICKAXE));

        // Blau links - Extras unten weiter links
        slotActions.put(9, new SlotAction("blue", EquipmentType.BOW));
        slotActions.put(36, new SlotAction("blue", EquipmentType.FOOD));
        slotActions.put(37, new SlotAction("blue", EquipmentType.SHIELD));
        slotActions.put(38, new SlotAction("blue", EquipmentType.OP_FOOD));


        // Rot rechts - Rüstung weiter rechts
        slotActions.put(7, new SlotAction("red", EquipmentType.HELMET));
        slotActions.put(16, new SlotAction("red", EquipmentType.CHESTPLATE));
        slotActions.put(25, new SlotAction("red", EquipmentType.LEGGINGS));
        slotActions.put(34, new SlotAction("red", EquipmentType.BOOTS));

        // Rot rechts - Waffen/Werkzeuge weiter rechts
        slotActions.put(15, new SlotAction("red", EquipmentType.SWORD));
        slotActions.put(26, new SlotAction("red", EquipmentType.AXE));
        slotActions.put(24, new SlotAction("red", EquipmentType.PICKAXE));

        // Rot rechts - Extras unten weiter rechts
        slotActions.put(17, new SlotAction("red", EquipmentType.BOW));
        slotActions.put(44, new SlotAction("red", EquipmentType.FOOD));
        slotActions.put(43, new SlotAction("red", EquipmentType.SHIELD));
        slotActions.put(42, new SlotAction("red", EquipmentType.OP_FOOD));
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        fillBackground(inv);

        setTeamItems(inv, "blue");
        setTeamItems(inv, "red");

        inv.setItem(49, createSimpleItem(
                Material.ARROW,
                ChatColor.DARK_AQUA + "Zurück",
                "",
                ChatColor.GRAY + "Zurück zu den Team Einstellungen"
        ));

        player.openInventory(inv);
    }

    private void setTeamItems(Inventory inv, String team) {
        for (Map.Entry<Integer, SlotAction> entry : slotActions.entrySet()) {
            int slot = entry.getKey();
            SlotAction action = entry.getValue();

            if (!action.team().equalsIgnoreCase(team)) continue;

            EquipmentType type = action.type();
            int level = plugin.getTeamEquipmentManager().getLevel(team, type);

            inv.setItem(slot, createEquipmentItem(team, type, level));
        }
    }

    private ItemStack createEquipmentItem(String team, EquipmentType type, int level) {
        Material material = plugin.getTeamEquipmentManager().getDisplayMaterial(type, level);

        if (material == null) {
            material = Material.BARRIER;
        }

        ChatColor teamColor = team.equalsIgnoreCase("blue") ? ChatColor.BLUE : ChatColor.RED;
        String teamName = team.equalsIgnoreCase("blue") ? "Blau" : "Rot";

        String displayType = getDisplayType(type);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(teamColor + teamName + ChatColor.GRAY + " - " + ChatColor.GOLD + displayType);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Aktuelle Stufe: " + ChatColor.YELLOW + level);
        lore.add(ChatColor.GRAY + "Aktuell: " + plugin.getTeamEquipmentManager().getLevelName(type, level));
        lore.add("");
        lore.add(ChatColor.GREEN + "Klicken: nächste Stufe");
        lore.add(ChatColor.DARK_GRAY + "Nach jedem Klick wird gespeichert.");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);

        item.setItemMeta(meta);
        return item;
    }

    private String getDisplayType(EquipmentType type) {
        return switch (type) {
            case HELMET -> "Helm";
            case CHESTPLATE -> "Brustplatte";
            case LEGGINGS -> "Hose";
            case BOOTS -> "Schuhe";
            case SWORD -> "Schwert";
            case AXE -> "Axt";
            case PICKAXE -> "Spitzhacke";
            case BOW -> "Bogen";
            case FOOD -> "Essen";
            case SHIELD -> "Schild";
            case OP_FOOD -> "OP Essen";
        };
    }

    private void fillBackground(Inventory inv) {
        ItemStack blue = createPane(Material.BLUE_STAINED_GLASS_PANE, " ");
        ItemStack black = createPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack red = createPane(Material.RED_STAINED_GLASS_PANE, " ");

        for (int row = 0; row < 6; row++) {
            int start = row * 9;

            // Linke Seite Blau: Spalten 0-3
            inv.setItem(start, blue);
            inv.setItem(start + 1, blue);
            inv.setItem(start + 2, blue);
            inv.setItem(start + 3, blue);

            // Mitte Schwarz: Spalte 4
            inv.setItem(start + 4, black);

            // Rechte Seite Rot: Spalten 5-8
            inv.setItem(start + 5, red);
            inv.setItem(start + 6, red);
            inv.setItem(start + 7, red);
            inv.setItem(start + 8, red);
        }
    }

    private ItemStack createPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createSimpleItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        if (loreLines != null) {
            for (String line : loreLines) {
                lore.add(line);
            }
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!ChatColor.stripColor(e.getView().getTitle()).equalsIgnoreCase("Team Equipment Einstellungen")) return;

        e.setCancelled(true);

        int slot = e.getRawSlot();

        if (slot >= e.getView().getTopInventory().getSize()) {
            return;
        }

        if (slot == 49) {
            Sounds.playBack(player);
            plugin.getTeamSettingsGUI().open(player);
            return;
        }

        SlotAction action = slotActions.get(slot);

        if (action == null) {
            return;
        }

        plugin.getTeamEquipmentManager().nextLevel(action.team(), action.type());

        Sounds.playClick(player);

        Bukkit.getScheduler().runTask(plugin, () -> open(player));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!ChatColor.stripColor(e.getView().getTitle()).equalsIgnoreCase("Team Equipment Einstellungen")) return;

        e.setCancelled(true);
    }

    private record SlotAction(String team, EquipmentType type) {
    }
}