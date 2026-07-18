package com.potionsmp.managers;

import com.potionsmp.PotionSMP;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles Regen Potion's active ability (Vitality Surge):
 *
 *  1. Player runs /slot1 or /slot2 (whichever has Regen equipped) -> this
 *     "arms" the ability and starts its cooldown immediately. Nothing else
 *     happens yet - no launch, no regen window.
 *  2. While armed, the player jumps normally once, lands, then jumps again
 *     (a ground-to-ground double-tap, detected via a short time window
 *     between landed jumps) -> THIS is what actually launches them ~10
 *     blocks up and starts the 20s regen+glow window and Mace Dive charge.
 *  3. Being armed has no expiry timer - it stays armed until either the
 *     double-jump launch consumes it, or the player drinks a different
 *     potion (which unequips Regen and clears the arm state).
 */
public class RegenAbilityManager {

    private final PotionSMP plugin;

    // Players who have run /slot1 or /slot2 for Regen and are waiting to double-jump
    private final Set<UUID> armed = new HashSet<>();

    // Tracks last single-jump timestamp (ms) for ground-to-ground double-tap detection
    private final Map<UUID, Long> lastJumpTime = new HashMap<>();
    private static final long DOUBLE_JUMP_WINDOW_MS = 600;

    // Players currently airborne+charged for a bonus Mace Dive hit
    private final Set<UUID> chargedForDive = new HashSet<>();

    public RegenAbilityManager(PotionSMP plugin) {
        this.plugin = plugin;
    }

    public void arm(UUID uuid) {
        armed.add(uuid);
    }

    public boolean isArmed(UUID uuid) {
        return armed.contains(uuid);
    }

    public void disarm(UUID uuid) {
        armed.remove(uuid);
        lastJumpTime.remove(uuid);
    }

    /**
     * Registers a jump. Returns true if this is the second jump of a
     * ground-to-ground double-tap (i.e. the player landed after their
     * first jump and is now jumping again within the window).
     */
    public boolean registerJumpAndCheckDouble(UUID uuid) {
        long now = System.currentTimeMillis();
        Long last = lastJumpTime.get(uuid);
        lastJumpTime.put(uuid, now);
        return last != null && (now - last) <= DOUBLE_JUMP_WINDOW_MS;
    }

    public boolean isChargedForDive(UUID uuid) {
        return chargedForDive.contains(uuid);
    }

    public void clearCharge(UUID uuid) {
        chargedForDive.remove(uuid);
    }

    /**
     * Consumes the armed state and performs the actual launch + regen window.
     * Called once the double-jump is detected.
     */
    public void launch(Player player, int heightBlocks, int durationTicks, int regenAmplifier) {
        UUID uid = player.getUniqueId();

        disarm(uid); // armed state is consumed by this launch

        double velocity = Math.sqrt(heightBlocks) * 0.85;
        player.setVelocity(new Vector(0, velocity, 0));

        chargedForDive.add(uid);

        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durationTicks, regenAmplifier, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationTicks, 0, false, false));

        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0, 1, 0), 12, 0.4, 0.3, 0.4, 0);
        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 0.2, 0), 20, 0.4, 0.1, 0.4, 0.2);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 1f, 1.2f);

        runVisualLoop(player, durationTicks);
        runLandingWatcher(player);
    }

    private void runVisualLoop(Player player, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks || !player.isOnline()) {
                    cancel();
                    return;
                }
                Location loc = player.getLocation();

                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(angle + (i * 120));
                    double x = Math.cos(a) * 0.8;
                    double z = Math.sin(a) * 0.8;
                    loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(x, 1, z), 1, 0, 0, 0, 0);
                }
                angle += 10;

                if (ticks % 10 == 0) {
                    loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 0.2, 0), 4, 0.5, 0.1, 0.5, 0);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void runLandingWatcher(Player player) {
        UUID uid = player.getUniqueId();
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks > 200) {
                    cancel();
                    return;
                }
                if (player.isOnGround() && ticks > 2) {
                    chargedForDive.remove(uid);
                    cancel();
                    return;
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void triggerMaceDive(Player player, LivingEntity target, double diveDamage) {
        UUID uid = player.getUniqueId();
        chargedForDive.remove(uid);

        target.damage(diveDamage, player);

        Location loc = target.getLocation();
        loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0, 1, 0), 25, 0.6, 0.6, 0.6, 0);
        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1, 0), 30, 0.6, 0.4, 0.6, 0.3);

        for (int i = 0; i < 30; i++) {
            double angle = (2 * Math.PI * i) / 30;
            double x = loc.getX() + 1.5 * Math.cos(angle);
            double z = loc.getZ() + 1.5 * Math.sin(angle);
            loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, x, loc.getY() + 0.1, z, 1, 0, 0, 0, 0);
        }

        loc.getWorld().playSound(loc, Sound.ITEM_MACE_SMASH_GROUND, 1.2f, 1f);
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.5f);
    }
}
