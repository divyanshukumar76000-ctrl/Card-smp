package com.potionsmp.listeners;

import com.potionsmp.PotionSMP;
import com.potionsmp.abilities.Ability;
import com.potionsmp.utils.PotionType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class CombatListener implements Listener {

    private final PotionSMP plugin;

    public CombatListener(PotionSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(EntityDamageByEntityEvent event) {
        // --- Shield: block all damage to shielded players ---
        if (event.getEntity() instanceof Player defender) {
            if (plugin.getShieldManager().isShielded(defender.getUniqueId())) {
                event.setCancelled(true);
                plugin.getShieldManager().spawnHitEffect(defender);
                return;
            }
        }

        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // --- Glitch: deploy corruption on next hit if armed ---
        if (plugin.getGlitchManager().isArmed(attacker.getUniqueId())) {
            if (target instanceof Player targetPlayer) {
                plugin.getGlitchManager().deployOnHit(attacker, targetPlayer);
            }
        }

        // --- Block item pickup for corrupted players ---
        // (handled separately via PlayerPickupItemEvent)

        // --- Normal ability onHit dispatch ---
        PotionType slot1 = plugin.getSlotManager().getSlot1(attacker.getUniqueId());
        PotionType slot2 = plugin.getSlotManager().getSlot2(attacker.getUniqueId());

        if (slot1 != null) {
            Ability ability = plugin.getAbilityRegistry().get(slot1);
            if (ability != null) ability.onHit(plugin, attacker, target);
        }
        if (slot2 != null && slot2 != slot1) {
            Ability ability = plugin.getAbilityRegistry().get(slot2);
            if (ability != null) ability.onHit(plugin, attacker, target);
        }
    }

    // Block knockback on shielded players
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.getShieldManager().isShielded(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getShieldManager().spawnHitEffect(player);
        }
    }

    // Block item pickup for glitch-corrupted players
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickup(PlayerPickupItemEvent event) {
        if (plugin.getGlitchManager().hasNoPickup(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
