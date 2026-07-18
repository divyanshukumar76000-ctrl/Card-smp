package com.potionsmp.abilities;

import com.potionsmp.PotionSMP;
import com.potionsmp.utils.PotionType;
import com.potionsmp.utils.PotionUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * §5 Ender Potion – Void Walker
 *
 * PASSIVE:
 * - Every 10th hit → inflict Darkness + Blindness on the target for 2s
 * - Subtle portal particle on each hit
 *
 * ACTIVE – Void Domain:
 * - Activate an Ender Domain (darkness / blindness zone) around the player
 * - Duration: configurable | Cooldown: configurable
 */
public class EnderAbility implements Ability, Listener {

    public void register(PotionSMP plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public PotionType type() { return PotionType.ENDER; }

    @Override
    public void onUnequip(PotionSMP plugin, Player player) {
        plugin.getEnderDomainManager().cancelDomain(player.getUniqueId());
    }

    @Override
    public void onHit(PotionSMP plugin, Player player, LivingEntity target) {
        int newCount = plugin.getHitCounterManager().registerHit(
            player.getUniqueId(), type(), target.getUniqueId(),
            player.getWorld().getFullTime());
        if (newCount == -1) return;

        int threshold = plugin.getConfig().getInt("potions.ender.passive-hits", 10);
        if (newCount >= threshold) {
            plugin.getHitCounterManager().reset(player.getUniqueId(), type());

            // Apply Darkness + Blindness
            target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS,  40, 0, false, false));
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false));

            Location loc = target.getLocation().clone().add(0, 1, 0);
            loc.getWorld().spawnParticle(Particle.PORTAL, loc, 60, 0.5, 1.0, 0.5, 0.5);
            loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 20, 0.3, 0.5, 0.3, 0.1);
            loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
            player.sendMessage(PotionUtils.colorize("&5☽ Void blindness triggered!"));
        } else {
            // Per-hit subtle particle
            target.getWorld().spawnParticle(Particle.PORTAL,
                target.getLocation().add(0, 1, 0), 3, 0.15, 0.15, 0.15, 0.1);
        }
    }

    @Override
    public ActivationResult activate(PotionSMP plugin, Player player) {
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), type()))
            return ActivationResult.ON_COOLDOWN;

        int radius   = plugin.getConfig().getInt("potions.ender.active-radius",   8);
        int duration = plugin.getConfig().getInt("potions.ender.active-duration", 200);
        int cooldown = plugin.getConfig().getInt("potions.ender.active-cooldown",  40);

        plugin.getCooldownManager().setCooldown(player.getUniqueId(), type(), cooldown);
        player.sendMessage(PotionUtils.msg("ender-active"));

        // Burst on activation
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.PORTAL,        loc.clone().add(0,1,0), 150, radius*0.5, 1, radius*0.5, 0.5);
        loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(0,1,0),  60, radius*0.3, 0.5, radius*0.3, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM,    1.2f, 0.7f);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.3f);

        // Apply initial darkness to all nearby enemies
        for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e instanceof LivingEntity le && !e.getUniqueId().equals(player.getUniqueId())) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS,  60, 0, false, false));
                le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0, false, false));
                if (le instanceof Player p)
                    p.sendMessage(PotionUtils.colorize("&5☽ Engulfed by &f" + player.getName() + "&5's Void Domain!"));
            }
        }

        plugin.getEnderDomainManager().activateDomain(player, duration, radius);
        return ActivationResult.SUCCESS;
    }

    /** Cancel the domain if the player logs off via a teleport to another world (optional safety). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) return;
        if (e.getFrom().getWorld() != null && e.getTo() != null
                && !e.getFrom().getWorld().equals(e.getTo().getWorld())) {
            PotionSMP.getInstance().getEnderDomainManager().cancelDomain(e.getPlayer().getUniqueId());
        }
    }
}
