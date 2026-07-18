package com.potionsmp.managers;

import com.potionsmp.PotionSMP;
import com.potionsmp.utils.PotionUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages Shield Potion's Divine Barrier:
 *  - Complete damage immunity
 *  - Knockback immunity
 *  - Two intersecting particle rings (vertical clockwise + horizontal counter-clockwise)
 *    that follow player yaw/pitch in real time
 *  - Hit effect: white flash + shockwave + glass shard burst
 */
public class ShieldManager {

    private final PotionSMP plugin;
    private final Map<UUID, Integer> shieldTasks = new HashMap<>();
    private final Map<UUID, Boolean> shielded = new HashMap<>();

    public ShieldManager(PotionSMP plugin) {
        this.plugin = plugin;
    }

    public boolean isShielded(UUID uuid) {
        return shielded.getOrDefault(uuid, false);
    }

    public void activateShield(Player player, int durationTicks) {
        UUID uid = player.getUniqueId();

        // Cancel existing
        Integer existing = shieldTasks.remove(uid);
        if (existing != null) plugin.getServer().getScheduler().cancelTask(existing);

        shielded.put(uid, true);
        player.sendMessage(PotionUtils.colorize("&f🛡 Divine Barrier active!"));

        // Activation burst
        Location loc = player.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 80, 1.5, 1.5, 1.5, 0.2);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 60,
            new Particle.DustOptions(Color.WHITE, 1.5f));
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 2f);

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            double vertAngle = 0;
            double horizAngle = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks || !player.isOnline()) {
                    deactivateShield(player);
                    cancel(); return;
                }

                Location center = player.getLocation().add(0, 1, 0);
                float yaw = player.getLocation().getYaw();

                // --- Vertical ring (clockwise, follows player yaw) ---
                int vPoints = 32;
                for (int i = 0; i < vPoints; i++) {
                    double a = Math.toRadians((360.0 / vPoints * i) + vertAngle);
                    double yawRad = Math.toRadians(yaw);

                    double localY = Math.sin(a) * 1.6;
                    double localX = Math.cos(a) * 1.6;

                    // Rotate by yaw
                    double wx = localX * Math.cos(yawRad);
                    double wz = localX * Math.sin(yawRad);

                    center.getWorld().spawnParticle(Particle.END_ROD,
                        center.getX() + wx, center.getY() + localY, center.getZ() + wz,
                        1, 0, 0, 0, 0);
                    if (i % 4 == 0) {
                        center.getWorld().spawnParticle(Particle.DUST,
                            center.getX() + wx, center.getY() + localY, center.getZ() + wz,
                            1, new Particle.DustOptions(Color.WHITE, 1.0f));
                    }
                }

                // --- Horizontal ring (counter-clockwise) ---
                int hPoints = 28;
                for (int i = 0; i < hPoints; i++) {
                    double a = Math.toRadians((360.0 / hPoints * i) + horizAngle);
                    double hx = Math.cos(a) * 1.6;
                    double hz = Math.sin(a) * 1.6;

                    center.getWorld().spawnParticle(Particle.DUST,
                        center.getX() + hx, center.getY(), center.getZ() + hz,
                        1, new Particle.DustOptions(Color.fromRGB(200, 200, 255), 0.9f));
                    if (i % 5 == 0) {
                        center.getWorld().spawnParticle(Particle.END_ROD,
                            center.getX() + hx, center.getY(), center.getZ() + hz,
                            1, 0, 0, 0, 0);
                    }
                }

                // Inner rune pattern (small rotating white dots)
                if (ticks % 3 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.toRadians((45.0 * i) + vertAngle * 2);
                        double rx = Math.cos(a) * 0.6;
                        double rz = Math.sin(a) * 0.6;
                        center.getWorld().spawnParticle(Particle.DUST,
                            center.getX() + rx, center.getY() + 0.2, center.getZ() + rz,
                            1, new Particle.DustOptions(Color.fromRGB(180, 180, 255), 0.6f));
                    }
                }

                // Electric sparks
                if (ticks % 8 == 0) {
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center, 4, 1.5, 1.5, 1.5, 0.1);
                    center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 2f);
                }

                vertAngle += 9;    // clockwise
                horizAngle -= 6;   // counter-clockwise
                ticks++;
            }
        };
        int taskId = task.runTaskTimer(plugin, 0L, 1L).getTaskId();
        shieldTasks.put(uid, taskId);

        // Auto-deactivate after duration
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (isShielded(uid)) {
                deactivateShield(player);
                player.sendMessage(PotionUtils.colorize("&7Divine Barrier has faded."));
            }
        }, durationTicks + 1);
    }

    /** Spawns hit effect when damage is blocked */
    public void spawnHitEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        // White flash
        loc.getWorld().spawnParticle(Particle.DUST, loc, 50,
            new Particle.DustOptions(Color.WHITE, 1.5f));
        // Shockwave ring
        for (int i = 0; i < 24; i++) {
            double a = Math.toRadians(15.0 * i);
            double x = Math.cos(a) * 2.0;
            double z = Math.sin(a) * 2.0;
            loc.getWorld().spawnParticle(Particle.END_ROD,
                loc.getX() + x, loc.getY(), loc.getZ() + z, 1, 0, 0, 0, 0);
        }
        // Glass shard burst
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 12, 0.5, 0.5, 0.5, 0.3);
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 2f);
        loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 2f);
    }

    private void deactivateShield(Player player) {
        UUID uid = player.getUniqueId();
        shielded.remove(uid);
        Integer taskId = shieldTasks.remove(uid);
        if (taskId != null) plugin.getServer().getScheduler().cancelTask(taskId);

        if (player.isOnline()) {
            Location loc = player.getLocation().add(0, 1, 0);
            loc.getWorld().spawnParticle(Particle.END_ROD, loc, 30, 1, 1, 1, 0.1);
            loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1.5f);
        }
    }

    public void cancelAll() {
        shieldTasks.values().forEach(id -> plugin.getServer().getScheduler().cancelTask(id));
        shieldTasks.clear();
        shielded.clear();
    }
}
