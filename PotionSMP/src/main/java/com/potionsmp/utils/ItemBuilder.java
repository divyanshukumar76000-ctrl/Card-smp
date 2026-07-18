package com.potionsmp.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.List;

public class ItemBuilder {

    public static ItemStack buildPotion(com.potionsmp.utils.PotionType type) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();

        meta.setCustomModelData(PotionUtils.getModelData(type));
        meta.setDisplayName(type.getDisplayName());

        switch (type) {
            case FIRE -> {
                meta.setBasePotionType(PotionType.FIRE_RESISTANCE);
                meta.setLore(List.of(
                    PotionUtils.colorize("&7Passive: &fEvery 10 hits ignites enemies"),
                    PotionUtils.colorize("&7Passive: &fInfinite Fire Resistance"),
                    PotionUtils.colorize("&7Active: &c&lInferno Rage &7(10x10 aura)"),
                    PotionUtils.colorize("&eCooldown: &f30s"),
                    PotionUtils.colorize("&7Drink to equip Slot 1!")
                ));
            }
            case FREEZE -> {
                meta.setBasePotionType(PotionType.SLOWNESS);
                meta.setLore(List.of(
                    PotionUtils.colorize("&7Passive: &fEvery 10 hits stuns target (1s)"),
                    PotionUtils.colorize("&7Active: &b&lArctic Prison &7(10-block freeze)"),
                    PotionUtils.colorize("&eCooldown: &f45s"),
                    PotionUtils.colorize("&7Drink to equip Slot 1!")
                ));
            }
            case REGEN -> {
                meta.setBasePotionType(PotionType.REGENERATION);
                meta.setLore(List.of(
                    PotionUtils.colorize("&7Passive: &fRegeneration I (infinite)"),
                    PotionUtils.colorize("&7Active: &a&lVitality Surge"),
                    PotionUtils.colorize("&f  Arm with /slot, then double-jump!"),
                    PotionUtils.colorize("&f  Mace Dive: 10 hearts + strong regen 20s"),
                    PotionUtils.colorize("&eCooldown: &f45s"),
                    PotionUtils.colorize("&7Drink to equip Slot 1!")
                ));
            }
            case GLITCH -> {
                meta.setBasePotionType(PotionType.SWIFTNESS);
                meta.setLore(List.of(
                    PotionUtils.colorize("&7Passive: &fMining Fatigue I (infinite)"),
                    PotionUtils.colorize("&7Passive: &fGlitch particles flicker around you"),
                    PotionUtils.colorize("&7Active: &d&lSystem Corruption"),
                    PotionUtils.colorize("&f  Force drop + no-pickup + teleport stutter"),
                    PotionUtils.colorize("&eDuration: &f10s &7| &eCooldown: &f35s"),
                    PotionUtils.colorize("&7Drink to equip Slot 1!")
                ));
            }
            case SHIELD -> {
                meta.setBasePotionType(PotionType.TURTLE_MASTER);
                meta.setLore(List.of(
                    PotionUtils.colorize("&7Active: &f&lDivine Barrier"),
                    PotionUtils.colorize("&f  Full damage + knockback immunity"),
                    PotionUtils.colorize("&f  Dual spinning celestial rings"),
                    PotionUtils.colorize("&eDuration: &f8s &7| &eCooldown: &f45s"),
                    PotionUtils.colorize("&7Drink to equip Slot 1!")
                ));
            }
        }

        PotionUtils.tagPotion(meta, type);
        item.setItemMeta(meta);
        return item;
    }
}
