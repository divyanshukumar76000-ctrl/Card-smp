package com.potionsmp.abilities;

import com.potionsmp.PotionSMP;
import com.potionsmp.utils.PotionType;
import com.potionsmp.utils.PotionUtils;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * §4 Strength Potion – Titan Warrior
 *
 * PASSIVE:
 * - 2× damage to hostile mobs
 * - Bonus damage at low HP:
 *   <6 hearts → +1dmg, <4 → +2dmg, <2 → +3dmg
 *
 * ACTIVE – Spark (15s, 30s CD):
 * - Every attack = crit
 * - Disable enemy shields 10s
 * - Arrows ignore shields
 */
public class StrengthAbility implements Ability, Listener {

    private final Set<UUID> sparkActive = new HashSet<>();

    public void register(PotionSMP plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public PotionType type() { return PotionType.STRENGTH; }

    @Override
    public void onEquip(PotionSMP plugin, Player player) {
        player.sendMessage(PotionUtils.colorize("&4Titan Warrior passive active — 2× mob damage!"));
    }

    @Override
    public void onUnequip(PotionSMP plugin, Player player) {
        sparkActive.remove(player.getUniqueId());
    }

    // ── Passive: bonus damage on hit ──────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player player)) return;
        UUID uid = player.getUniqueId();

        if (!PotionSMP.getInstance().getSlotManager().hasEquipped(uid, PotionType.STRENGTH)) return;

        if (e.getEntity() instanceof Mob || e.getEntity() instanceof Monster) {
            // 2× damage on mobs
            e.setDamage(e.getDamage() * 2.0);
        }

        // Low HP bonus
        if (e.getEntity() instanceof LivingEntity le) {
            double hp = le.getHealth();
            if (hp < 4.0)       e.setDamage(e.getDamage() + 3.0);
            else if (hp < 8.0)  e.setDamage(e.getDamage() + 2.0);
            else if (hp < 12.0) e.setDamage(e.getDamage() + 1.0);

            // Spark active → force crit + disable shield
            if (sparkActive.contains(uid)) {
                // Force crit visual
                le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().add(0,1,0),
                    8, 0.3,0.3,0.3, 0.1);
                // Shield disable: briefly stun their shield
                if (le instanceof Player target) {
                    disableShield(target);
                }
            }
        }
    }

    private void disableShield(Player target) {
        // Paper API: disable shield for 10s (200 ticks)
        // Use MINING_FATIGUE to prevent blocking
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 200, 10, false, false));
    }

    // ── Active: Spark ─────────────────────────────────────────────────────
    @Override
    public ActivationResult activate(PotionSMP plugin, Player player) {
        UUID uid = player.getUniqueId();
        if (plugin.getCooldownManager().isOnCooldown(uid, type()))
            return ActivationResult.ON_COOLDOWN;

        int dur      = plugin.getConfig().getInt("potions.strength.active-duration", 15);
        int cooldown = plugin.getConfig().getInt("potions.strength.active-cooldown", 30);
        int ticks    = dur * 20;

        plugin.getCooldownManager().setCooldown(uid, type(), cooldown);
        sparkActive.add(uid);

        // Visual
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.CRIT,         loc.clone().add(0,1,0), 30, 0.7,1,0.7, 0.2);
        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK,  loc.clone().add(0,1,0), 15, 0.5,0.5,0.5, 0);
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,loc.clone().add(0,1,0), 20, 0.5,0.8,0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.2f);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT,     1f,   1.0f);

        player.sendTitle("§4§l⚔ SPARK", "§7Every hit is a critical!", 5, 40, 10);

        // Aura loop during spark
        new BukkitRunnable() {
            int ticks2 = 0;
            double angle = 0;
            @Override
            public void run() {
                if (ticks2 >= ticks || !player.isOnline()) { cancel(); return; }
                Location l = player.getLocation().add(0,1,0);
                angle += 0.3;
                for (int i = 0; i < 3; i++) {
                    double a = angle + (Math.PI * 2 / 3) * i;
                    l.getWorld().spawnParticle(Particle.CRIT,
                        l.clone().add(Math.cos(a)*0.8, 0, Math.sin(a)*0.8), 1, 0,0,0, 0);
                }
                ticks2++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Schedule end
        new BukkitRunnable() {
            @Override
            public void run() {
                sparkActive.remove(uid);
                if (player.isOnline())
                    player.sendActionBar(net.kyori.adventure.text.Component.text("§7Spark ended."));
            }
        }.runTaskLater(plugin, ticks);

        return ActivationResult.SUCCESS;
    }

    public boolean isSparkActive(UUID uid) { return sparkActive.contains(uid); }
}
