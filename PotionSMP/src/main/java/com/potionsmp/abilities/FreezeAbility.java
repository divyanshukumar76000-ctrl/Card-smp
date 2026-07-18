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
import org.bukkit.scheduler.BukkitRunnable;

public class FreezeAbility implements Ability {

    @Override
    public PotionType type() { return PotionType.FREEZE; }

    @Override
    public void onUnequip(PotionSMP plugin, Player player) {
        plugin.getParticleManager().cancelAura(player.getUniqueId());
    }

    @Override
    public void onHit(PotionSMP plugin, Player player, LivingEntity target) {
        int newCount = plugin.getHitCounterManager().registerHit(
            player.getUniqueId(), type(), target.getUniqueId(),
            player.getWorld().getFullTime());
        if (newCount == -1) return;

        int threshold = plugin.getConfig().getInt("potions.freeze.passive-hits", 10);
        if (newCount >= threshold) {
            plugin.getHitCounterManager().reset(player.getUniqueId(), type());
            int stunDur = plugin.getConfig().getInt("potions.freeze.passive-stun-duration", 20);
            plugin.getFrozenPlayerManager().freezeEntity(target, stunDur);

            // Passive particle burst
            ParticleManager.spawnFreezePassiveParticles(target.getLocation().clone().add(0, 1, 0));
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.5f);
            player.sendMessage(PotionUtils.colorize("&b❄ Passive freeze triggered!"));
        } else {
            target.getWorld().spawnParticle(Particle.SNOWFLAKE,
                target.getLocation().add(0, 1, 0), 2, 0.15, 0.15, 0.15, 0.01);
        }
    }

    @Override
    public ActivationResult activate(PotionSMP plugin, Player player) {
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), type()))
            return ActivationResult.ON_COOLDOWN;

        int radius = plugin.getConfig().getInt("potions.freeze.active-radius", 10);
        int freezeDur = plugin.getConfig().getInt("potions.freeze.active-freeze-duration", 60);
        int cooldown = plugin.getConfig().getInt("potions.freeze.active-cooldown", 45);

        plugin.getCooldownManager().setCooldown(player.getUniqueId(), type(), cooldown);
        player.sendMessage(PotionUtils.msg("freeze-active"));

        Location center = player.getLocation();

        // Launch burst - massive snowstorm
        center.getWorld().spawnParticle(Particle.SNOWFLAKE, center.clone().add(0,1,0),
            200, radius / 2.0, 2, radius / 2.0, 0.08);
        center.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, center.clone().add(0,1,0),
            100, radius / 2.0, 1.5, radius / 2.0, 0.3);
        center.getWorld().spawnParticle(Particle.SNOWFLAKE, center.clone().add(0,1,0),
            80, 1, 1, 1, 0.5);
        center.getWorld().playSound(center, Sound.ENTITY_GUARDIAN_ELDER_CURSE, 1.5f, 0.5f);
        center.getWorld().playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.5f, 0.5f);

        // Freeze all nearby entities
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (e instanceof LivingEntity le && !e.getUniqueId().equals(player.getUniqueId())) {
                plugin.getFrozenPlayerManager().freezeEntity(le, freezeDur);
                if (le instanceof Player frozen)
                    frozen.sendMessage(PotionUtils.colorize("&b❄ Frozen by &f" + player.getName() + "&b!"));
            }
        }

        // Heavy spinning ice aura during freeze
        plugin.getParticleManager().startFreezeAura(player, freezeDur);

        // Extra spinning crystal rings during freeze duration
        new BukkitRunnable() {
            int ticks = 0; double angle = 0;
            @Override
            public void run() {
                if (ticks >= freezeDur) { cancel(); return; }
                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians(angle + (i * 30));
                    center.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        center.getX() + Math.cos(a) * radius * 0.7,
                        center.getY() + 1,
                        center.getZ() + Math.sin(a) * radius * 0.7,
                        1, 0, 0, 0, 0);
                }
                angle += 12;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return ActivationResult.SUCCESS;
    }
}
