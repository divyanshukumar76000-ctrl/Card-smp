package com.potionsmp.abilities;

import com.potionsmp.PotionSMP;
import com.potionsmp.managers.ParticleManager;
import com.potionsmp.utils.PotionType;
import com.potionsmp.utils.PotionUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * Glitch Potion abilities:
 *
 * Passive: Mining Fatigue I (permanent while equipped) + glitch particle flicker
 * Active (System Corruption): on activation OR first direct hit:
 *   - Target drops held item
 *   - Target can't pick up items for 10s
 *   - Micro-teleport stutters (1-2 blocks)
 *   - Visual purple/cyan glitch particles
 *   - Screen scramble effect
 */
public class GlitchAbility implements Ability {

    @Override
    public PotionType type() { return PotionType.GLITCH; }

    @Override
    public void onEquip(PotionSMP plugin, Player player) {
        // Passive: Mining Fatigue I
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.MINING_FATIGUE, Integer.MAX_VALUE, 0, false, false));
        // Start passive glitch flicker aura
        startPassiveAura(plugin, player);
    }

    @Override
    public void onUnequip(PotionSMP plugin, Player player) {
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        plugin.getParticleManager().cancelAura(player.getUniqueId());
        plugin.getGlitchManager().cancelAll();
    }

    private void startPassiveAura(PotionSMP plugin, Player player) {
        plugin.getParticleManager().cancelAura(player.getUniqueId());
        new BukkitRunnable() {
            final Random rand = new Random();
            @Override
            public void run() {
                if (!player.isOnline() ||
                    plugin.getSlotManager().getSlot1(player.getUniqueId()) != PotionType.GLITCH &&
                    plugin.getSlotManager().getSlot2(player.getUniqueId()) != PotionType.GLITCH) {
                    cancel(); return;
                }
                Location loc = player.getLocation().add(0, 1, 0);
                // Subtle flicker - small glitch particles
                if (rand.nextInt(3) == 0) {
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(
                        (rand.nextDouble()-0.5)*0.8, (rand.nextDouble()-0.5)*0.8,
                        (rand.nextDouble()-0.5)*0.8), 1,
                        new Particle.DustOptions(
                            org.bukkit.Color.fromRGB(150, 0, 255), 0.6f));
                    loc.getWorld().spawnParticle(Particle.END_ROD,
                        loc.clone().add((rand.nextDouble()-0.5)*0.5, 0, (rand.nextDouble()-0.5)*0.5),
                        1, 0, 0, 0, 0.02);
                }
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    @Override
    public ActivationResult activate(PotionSMP plugin, Player player) {
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), type()))
            return ActivationResult.ON_COOLDOWN;

        int duration = plugin.getConfig().getInt("potions.glitch.active-duration", 200);
        int cooldown = plugin.getConfig().getInt("potions.glitch.active-cooldown", 35);
        int stutters = plugin.getConfig().getInt("potions.glitch.stutter-count", 4);

        plugin.getCooldownManager().setCooldown(player.getUniqueId(), type(), cooldown);
        player.sendMessage(PotionUtils.msg("glitch-active"));

        // Activation burst
        Location loc = player.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.PORTAL, loc, 60, 0.5, 0.5, 0.5, 0.8);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 30, 0.4, 0.4, 0.4, 0.3);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 40,
            new Particle.DustOptions(org.bukkit.Color.fromRGB(150, 0, 255), 1.2f));
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.4f);
        loc.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1.5f);

        // The ability "arms" — next direct hit or activation applies corruption
        // We store that the player has an active corruption ready to deploy
        // For immediate activation on self/area use, we mark it as armed
        plugin.getGlitchManager().armCorruption(player.getUniqueId(), duration, stutters);

        return ActivationResult.SUCCESS;
    }
}
