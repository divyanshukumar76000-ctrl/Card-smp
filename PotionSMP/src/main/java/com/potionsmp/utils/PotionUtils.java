package com.potionsmp.utils;

import com.potionsmp.PotionSMP;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class PotionUtils {

    private static NamespacedKey key() {
        return new NamespacedKey(PotionSMP.getInstance(), "potionsmp_type");
    }

    /**
     * Returns the PotionType of this item, or null if it's not a PotionSMP potion.
     */
    public static PotionType getPotionType(ItemStack item) {
        if (item == null || item.getType() != Material.POTION) return null;
        if (!item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(key(), PersistentDataType.STRING)) return null;
        String id = meta.getPersistentDataContainer().get(key(), PersistentDataType.STRING);
        return PotionType.fromId(id);
    }

    public static void tagPotion(ItemMeta meta, PotionType type) {
        meta.getPersistentDataContainer().set(key(), PersistentDataType.STRING, type.getId());
    }

    public static int getModelData(PotionType type) {
        return PotionSMP.getInstance().getConfig().getInt("potions." + type.getId() + ".custom-model-data", 100);
    }

    public static String msg(String key) {
        String raw = PotionSMP.getInstance().getConfig().getString("messages." + key, "&cMissing message: " + key);
        return colorize(raw);
    }

    public static String colorize(String s) {
        return s.replace("&", "\u00a7");
    }
}
