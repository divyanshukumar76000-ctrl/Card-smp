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
import org.bukkit.util.Vector;

import java.util.*;

/**
 * §e Speed Potion – Lightning Fast Warrior
 *
 * PASSIVE:
 * - Base Speed I always active
 * - Every hit → +1 Speed level (max Speed IV)
 * - No hit for 1.5s → reset to Speed I
 * - 25% reduced iframes on hit targets
 *
 * ACTIVE – Lightning Dash:
 * - Dash in look direction
 * - Speed IV during dash
 * - 20% damage reduction
 * - Shockwave at end (4-5 block radius)
 * - Duration: 1.8s | Cooldown: 28s
 */
public class SpeedAbility implements Ability, Listener {

    // Hit stacks per player (1–4)
    private final Map<UUID, Integer> speedStacks   = new HashMap<>();
    // Last hit timestamp
    private final Map<UUID, Long>    lastHitTime   = new HashMap<>();
    // Players currently dashing
    private final Set<UUID>          dashing        = new HashSet<>();

    private static final long STACK_RESET_MS = 1500;
    private static final int  MAX_STACKS     = 4;

    public void register(PotionSMP plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public PotionType type() { return PotionType.SPEED; }

    // ── Equip / Unequip ───────────────────────────────────────────────────

    @Override
    public void onEquip(PotionSMP plugin, Player player) {
        applySpeed(player, 1);
        startStackDecayLoop(plugin, player);
    }

    @Override
    public void onUnequip(PotionSMP plugin, Player player) {
        speedStacks.remove(player.getUniqueId());
        lastHitTime.remove(player.getUniqueId());
        dashing.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.SPEED);
    }

    // ── Stack decay loop: check every 5 ticks ────────────────────────────

    private void startStackDecayLoop(PotionSMP plugin, Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() ||
                    !plugin.getSlotManager().hasEquipped(player.getUniqueId(), PotionType.SPEED)) {
                    cancel(); return;
                }
                UUID uid = player.getUniqueId();
                Long last = lastHitTime.get(uid);
                if (last != null && System.currentTimeMillis() - last > STACK_RESET_MS) {
                    speedStacks.put(uid, 1);
                    lastHitTime.remove(uid);
                    applySpeed(player, 1);
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    // ── Passive hit: stack speed ──────────────────────────────────────────

    @Override
    public void onHit(PotionSMP plugin, Player player, LivingEntity target) {
        UUID uid = player.getUniqueId();
        lastHitTime.put(uid, System.currentTimeMillis());

        int stacks = speedStacks.getOrDefault(uid, 1);
        stacks = Math.min(stacks + 1, MAX_STACKS);
        speedStacks.put(uid, stacks);
        applySpeed(player, stacks);

        // Reduce iframes by 25% (set to 75% of default 20)
        target.setNoDamageTicks(Math.max(0, (int)(target.getNoDamageTicks() * 0.75)));

        // Spark on hit
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
            target.getLocation().add(0,1,0), 4, 0.2,0.2,0.2, 0.05);

        if (stacks == MAX_STACKS) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("§e⚡ MAX SPEED!"));
        }
    }

    private void applySpeed(Player player, int level) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
            Integer.MAX_VALUE, level - 1, false, false));
    }

    // ── Active: Lightning Dash ────────────────────────────────────────────

    @Override
    public ActivationResult activate(PotionSMP plugin, Player player) {
        UUID uid = player.getUniqueId();
        if (plugin.getCooldownManager().isOnCooldown(uid, type()))
            return ActivationResult.ON_COOLDOWN;

        if (dashing.contains(uid)) return ActivationResult.SUCCESS;

        int cooldown = plugin.getConfig().getInt("potions.speed.active-cooldown", 28);
        plugin.getCooldownManager().setCooldown(uid, type(), cooldown);

        executeDash(plugin, player);
        return ActivationResult.SUCCESS;
    }

    private void executeDash(PotionSMP plugin, Player player) {
        UUID uid = player.getUniqueId();
        dashing.add(uid);

        // Speed IV + 20% damage reduction during dash
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,      40, 3, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,  40, 0, false, false));

        // Launch in look direction
        Vector dir = player.getLocation().getDirection().normalize().multiply(1.4);
        dir.setY(Math.max(dir.getY(), 0.1));
        player.setVelocity(dir);

        player.sendTitle("", "§e⚡ Lightning Dash!", 2, 15, 5);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.8f);

        // Dash trail
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 36 || !player.isOnline()) { // 1.8s
                    triggerShockwave(plugin, player);
                    dashing.remove(uid);
                    player.removePotionEffect(PotionEffectType.RESISTANCE);
                    cancel(); return;
                }
                Location loc = player.getLocation();
                loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 5, 0.2,0.2,0.2, 0.05);
                loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK,   loc, 2, 0.3,0.1,0.3, 0);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void triggerShockwave(PotionSMP plugin, Player player) {
        Location loc = player.getLocation();
        World world  = loc.getWorld();
        double radius = plugin.getConfig().getDouble("potions.speed.shockwave-radius", 4.5);
        double dmg    = plugin.getConfig().getDouble("potions.speed.shockwave-damage", 5.0);

        // Ring particles
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            Location p = loc.clone().add(Math.cos(angle)*radius, 0.1, Math.sin(angle)*radius);
            world.spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.1,0.1,0.1, 0.05);
            world.spawnParticle(Particle.SWEEP_ATTACK,   p, 1, 0,0,0, 0);
        }

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.4f);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.5f);

        // Damage + knockback nearby
        for (Entity e : world.getNearbyEntities(loc, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e.getUniqueId().equals(player.getUniqueId())) continue;
            le.damage(dmg, player);
            le.setNoDamageTicks(0);
            Vector kb = le.getLocation().toVector().subtract(loc.toVector()).normalize()
                .multiply(1.5).setY(0.5);
            le.setVelocity(kb);
        }
    }

    // ── Reduce iframes on victim (EntityDamageByEntityEvent) ─────────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!speedStacks.containsKey(p.getUniqueId())) return;
        if (e.getEntity() instanceof LivingEntity le) {
            plugin_ref_hack(p).getServer().getScheduler().runTaskLater(
                plugin_ref_hack(p), () -> le.setNoDamageTicks(
                    (int)(le.getNoDamageTicks() * 0.75)), 1L);
        }
    }
    // Small helper to get plugin ref without storing it (pattern: use getInstance)
    private PotionSMP plugin_ref_hack(Player p) { return PotionSMP.getInstance(); }
}
