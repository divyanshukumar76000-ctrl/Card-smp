package com.potionsmp.listeners;

import com.potionsmp.PotionSMP;
import com.potionsmp.abilities.Ability;
import com.potionsmp.managers.SlotManager;
import com.potionsmp.utils.PotionType;
import com.potionsmp.utils.PotionUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.UUID;

/**
 * Slot assignment logic on drink:
 *  - Slot 1 empty → goes to Slot 1
 *  - Slot 1 filled, Slot 2 empty → goes to Slot 2
 *  - Both filled → Slot 1 is replaced (oldest slot)
 *
 * Vanilla base-effect is stripped 1 tick after drinking (isInfinite() check
 * protects custom permanent passives from being accidentally removed).
 */
public class DrinkListener implements Listener {

    private final PotionSMP plugin;

    public DrinkListener(PotionSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        PotionType type = PotionUtils.getPotionType(event.getItem());
        if (type == null) return;

        Player player = event.getPlayer();
        SlotManager slots = plugin.getSlotManager();
        UUID uid = player.getUniqueId();

        // Strip vanilla base-effect 1 tick after drinking
        PotionEffectType vanillaEffect = vanillaEffectFor(type);
        if (vanillaEffect != null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                PotionEffect active = player.getPotionEffect(vanillaEffect);
                if (active != null && !active.isInfinite()) {
                    player.removePotionEffect(vanillaEffect);
                }
            });
        }

        PotionType slot1 = slots.getSlot1(uid);
        PotionType slot2 = slots.getSlot2(uid);

        int targetSlot;
        PotionType displaced;

        if (slot1 == null) {
            // Slot 1 empty → assign to Slot 1
            targetSlot = SlotManager.SLOT_1;
            displaced = null;
            slots.setSlot1(uid, type);

        } else if (slot2 == null) {
            // Slot 1 filled, Slot 2 empty → assign to Slot 2
            targetSlot = SlotManager.SLOT_2;
            displaced = null;
            slots.setSlot2(uid, type);

        } else {
            // Both filled → replace Slot 1 (oldest)
            targetSlot = SlotManager.SLOT_1;
            displaced = slot1;
            slots.setSlot1(uid, type);
        }

        // Unequip displaced potion's passive if replaced
        if (displaced != null && displaced != type) {
            Ability oldAbility = plugin.getAbilityRegistry().get(displaced);
            if (oldAbility != null) oldAbility.onUnequip(plugin, player);
            player.sendMessage(PotionUtils.colorize("&7Slot " + targetSlot + " replaced: &f"
                + displaced.getDisplayName() + " &7removed."));
        }

        // Equip new potion's passive
        Ability newAbility = plugin.getAbilityRegistry().get(type);
        if (newAbility != null) newAbility.onEquip(plugin, player);

        player.sendMessage(PotionUtils.colorize("&a✦ " + type.getDisplayName()
            + " &aequipped to &eSlot " + targetSlot + "&a!"));
    }

    private PotionEffectType vanillaEffectFor(PotionType type) {
        return switch (type) {
            case FIRE   -> PotionEffectType.FIRE_RESISTANCE;
            case FREEZE -> PotionEffectType.SLOWNESS;
            case REGEN  -> PotionEffectType.REGENERATION;
            case GLITCH -> PotionEffectType.MINING_FATIGUE;
            case SHIELD -> PotionEffectType.SLOWNESS; // Turtle Master gives slowness
            default     -> null;
        };
    }
}
