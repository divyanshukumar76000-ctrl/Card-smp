package com.potionsmp.managers;

import com.potionsmp.PotionSMP;
import com.potionsmp.utils.PotionType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages continuous active-ability particle auras per player.
 * Each ability's active triggers startAura() with its own particle style.
 * Auras auto-cancel after their duration or when stopAura() is called.
 */
public class ParticleManager {

    private final PotionSMP plugin;
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();

    public ParticleManager(PotionSMP plugin) {
        this.plugin = plugin;
    }

    public void startFireAura(Player player, int durationTicks) {
        cancelAura(player.getUniqueId());
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks || !player.isOnline()) { cancel(); return; }
                Location loc = player.getLocation().add(0, 1, 0);

                // Spinning flame ring - 3 layers
                for (int layer = 0; layer < 3; layer++) {
                    double radius = 1.2 + (layer * 0.6);
                    int points = 12 + (layer * 4);
                    double layerAngle = angle + (layer * 30);
                    for (int i = 0; i < points; i++) {
                        double a = Math.toRadians((360.0 / points * i) + layerAngle);
                        double x = Math.cos(a) * radius;
                        double z = Math.sin(a) * radius;
                        loc.getWorld().spawnParticle(Particle.FLAME,
                            loc.getX() + x, loc.getY() + (layer * 0.3), loc.getZ() + z,
                            1, 0, 0, 0, 0.01);
                        if (ticks % 3 == 0) {
                            loc.getWorld().spawnParticle(Particle.LAVA,
                                loc.getX() + x, loc.getY(), loc.getZ() + z,
                                1, 0.1, 0.1, 0.1, 0);
                        }
                    }
                }

                // Rising embers from feet
                if (ticks % 2 == 0) {
                    loc.getWorld().spawnParticle(Particle.FLAME,
                        loc.getX(), loc.getY() - 0.5, loc.getZ(),
                        5, 0.3, 0, 0.3, 0.05);
                    loc.getWorld().spawnParticle(Particle.SMALL_FLAME,
                        loc.getX(), loc.getY() - 0.5, loc.getZ(),
                        3, 0.2, 0, 0.2, 0.02);
                }

                // Fire trail behind player
                loc.getWorld().spawnParticle(Particle.FLAME,
                    loc.getX(), loc.getY() - 0.9, loc.getZ(),
                    2, 0.1, 0, 0.1, 0.02);

                angle += 8;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        activeTasks.put(player.getUniqueId(), task);
    }

    public void startFreezeAura(Player player, int durationTicks) {
        cancelAura(player.getUniqueId());
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks || !player.isOnline()) { cancel(); return; }
                Location loc = player.getLocation().add(0, 1, 0);

                // Ice crystal orbit - 2 rings spinning opposite directions
                for (int i = 0; i < 16; i++) {
                    double a = Math.toRadians((360.0 / 16 * i) + angle);
                    double x = Math.cos(a) * 1.4;
                    double z = Math.sin(a) * 1.4;
                    loc.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        loc.getX() + x, loc.getY() + 0.2, loc.getZ() + z,
                        1, 0, 0, 0, 0.01);
                }
                for (int i = 0; i < 10; i++) {
                    double a = Math.toRadians((360.0 / 10 * i) - angle * 1.5);
                    double x = Math.cos(a) * 0.8;
                    double z = Math.sin(a) * 0.8;
                    loc.getWorld().spawnParticle(Particle.ITEM_SNOWBALL,
                        loc.getX() + x, loc.getY() + 0.5, loc.getZ() + z,
                        1, 0, 0, 0, 0.01);
                }

                // Frost mist rising from below
                if (ticks % 3 == 0) {
                    loc.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        loc.getX(), loc.getY() - 0.8, loc.getZ(),
                        6, 0.4, 0.1, 0.4, 0.02);
                }

                // Occasional ice burst
                if (ticks % 15 == 0) {
                    loc.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        loc.getX(), loc.getY(), loc.getZ(),
                        20, 0.8, 0.5, 0.8, 0.05);
                    loc.getWorld().spawnParticle(Particle.ITEM_SNOWBALL,
                        loc.getX(), loc.getY(), loc.getZ(),
                        10, 0.5, 0.3, 0.5, 0.1);
                }

                angle += 6;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        activeTasks.put(player.getUniqueId(), task);
    }

    public void startRegenAura(Player player, int durationTicks) {
        cancelAura(player.getUniqueId());
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks || !player.isOnline()) { cancel(); return; }
                Location loc = player.getLocation().add(0, 1, 0);

                // 3 orbiting hearts
                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(angle + (i * 120));
                    double x = Math.cos(a) * 1.0;
                    double z = Math.sin(a) * 1.0;
                    loc.getWorld().spawnParticle(Particle.HEART,
                        loc.getX() + x, loc.getY() + 0.5, loc.getZ() + z,
                        1, 0, 0, 0, 0);
                }

                // Totem sparks rising
                if (ticks % 2 == 0) {
                    loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                        loc.getX(), loc.getY() - 0.5, loc.getZ(),
                        4, 0.3, 0.2, 0.3, 0.1);
                }

                // Green healing mist
                if (ticks % 5 == 0) {
                    loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                        loc.getX(), loc.getY(), loc.getZ(),
                        3, 0.5, 0.3, 0.5, 0);
                }

                // Occasional big heart burst
                if (ticks % 20 == 0) {
                    loc.getWorld().spawnParticle(Particle.HEART,
                        loc.getX(), loc.getY() + 1, loc.getZ(),
                        8, 0.6, 0.4, 0.6, 0.1);
                    loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                        loc.getX(), loc.getY(), loc.getZ(),
                        15, 0.5, 0.5, 0.5, 0.3);
                }

                angle += 10;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        activeTasks.put(player.getUniqueId(), task);
    }

    // Passive hit particles - subtle burst on 10th hit
    public static void spawnFirePassiveParticles(Location loc) {
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 15, 0.4, 0.4, 0.4, 0.1);
        loc.getWorld().spawnParticle(Particle.LAVA, loc, 4, 0.3, 0.2, 0.3, 0);
        loc.getWorld().spawnParticle(Particle.SMALL_FLAME, loc, 8, 0.5, 0.3, 0.5, 0.05);
    }

    public static void spawnFreezePassiveParticles(Location loc) {
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 12, 0.4, 0.4, 0.4, 0.05);
        loc.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, loc, 6, 0.3, 0.3, 0.3, 0.05);
    }

    public static void spawnRegenPassiveParticles(Location loc) {
        loc.getWorld().spawnParticle(Particle.HEART, loc, 5, 0.3, 0.3, 0.3, 0.05);
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 6, 0.4, 0.3, 0.4, 0);
    }

    public void cancelAura(UUID uuid) {
        BukkitTask task = activeTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public void cancelAll() {
        activeTasks.values().forEach(BukkitTask::cancel);
        activeTasks.clear();
    }
}
