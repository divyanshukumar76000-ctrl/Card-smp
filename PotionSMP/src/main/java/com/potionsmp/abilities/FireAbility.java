package com.potionsmp.abilities;

import com.potionsmp.PotionSMP;
import com.potionsmp.managers.ParticleManager;
import com.potionsmp.utils.PotionType;
import com.potionsmp.utils.PotionUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FireAbility implements Ability {

    @Override
    public PotionType type() { return PotionType.FIRE; }

    @Override
    public void onEquip(PotionSMP plugin, Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,
            Integer.MAX_VALUE, 0, false, false));
    }

    @Override
    public void onUnequip(PotionSMP plugin, Player player) {
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        plugin.getParticleManager().cancelAura(player.getUniqueId());
    }

    @Override
    public void onHit(PotionSMP plugin, Player player, LivingEntity target) {
        int newCount = plugin.getHitCounterManager().registerHit(
            player.getUniqueId(), type(), target.getUniqueId(),
            player.getWorld().getFullTime());
        if (newCount == -1) return;

        int threshold = plugin.getConfig().getInt("potions.fire.passive-hits", 10);
        if (newCount >= threshold) {
            plugin.getHitCounterManager().reset(player.getUniqueId(), type());

            Location loc = target.getLocation().clone().add(0, 1, 0);
            target.setFireTicks(100);

            for (Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
                if (e instanceof LivingEntity le && !e.getUniqueId().equals(player.getUniqueId()))
                    le.setFireTicks(80);
            }

            // Passive particles - burst on 10th hit
            ParticleManager.spawnFirePassiveParticles(loc);
            loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f);
            player.sendMessage(PotionUtils.colorize("&c🔥 Passive fire ignition!"));
        } else {
            // Per-hit subtle particle
            target.getWorld().spawnParticle(Particle.FLAME,
                target.getLocation().add(0, 1, 0), 2, 0.15, 0.15, 0.15, 0.01);
        }
    }

    @Override
    public ActivationResult activate(PotionSMP plugin, Player player) {
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), type()))
            return ActivationResult.ON_COOLDOWN;

        int radius = plugin.getConfig().getInt("potions.fire.active-fire-radius", 5);
        int duration = plugin.getConfig().getInt("potions.fire.active-duration", 200);
        int cooldown = plugin.getConfig().getInt("potions.fire.active-cooldown", 30);

        plugin.getCooldownManager().setCooldown(player.getUniqueId(), type(), cooldown);
        player.sendMessage(PotionUtils.msg("fire-active"));

        // Launch burst
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0,1,0), 80, radius, 1, radius, 0.15);
        loc.getWorld().spawnParticle(Particle.LAVA, loc.clone().add(0,1,0), 30, radius, 0.5, radius, 0.1);
        loc.getWorld().spawnParticle(Particle.SMALL_FLAME, loc.clone().add(0,1,0), 50, radius, 0.5, radius, 0.08);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.6f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GHAST_SHOOT, 0.8f, 1.2f);

        // Heavy continuous fire aura
        plugin.getParticleManager().startFireAura(player, duration);

        // Damage zone (from FireAuraManager)
        plugin.getFireAuraManager().activateInfernoAura(player, duration, radius);

        return ActivationResult.SUCCESS;
    }
}
