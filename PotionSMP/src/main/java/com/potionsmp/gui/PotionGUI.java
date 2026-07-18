package com.potionsmp.gui;

import com.potionsmp.utils.ItemBuilder;
import com.potionsmp.utils.PotionType;
import com.potionsmp.utils.PotionUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * GUI showing every registered PotionType as a selectable item. Slots are
 * filled left-to-right, skipping the border, so adding new PotionType
 * entries automatically appears here with no GUI code changes needed.
 */
public class PotionGUI {

    private static final int[] CONTENT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25
    };

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36,
                Component.text("✨ PotionSMP — Choose Your Potion"));

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 36; i++) gui.setItem(i, filler);

        PotionType[] types = PotionType.values();
        for (int i = 0; i < types.length && i < CONTENT_SLOTS.length; i++) {
            gui.setItem(CONTENT_SLOTS[i], ItemBuilder.buildPotion(types[i]));
        }

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text(PotionUtils.colorize("&e✨ PotionSMP")));
        infoMeta.lore(List.of(
            Component.text(PotionUtils.colorize("&7Click a potion to receive it!")),
            Component.text(PotionUtils.colorize("&7Drinking always equips to Slot 1.")),
            Component.text(PotionUtils.colorize("&7Use /slot1 or /slot2 to activate.")),
            Component.text(PotionUtils.colorize("&7(Bind these to keys with Bind Command mod)"))
        ));
        info.setItemMeta(infoMeta);
        gui.setItem(4, info);

        player.openInventory(gui);
    }
}
