package com.potionsmp.managers;

import com.potionsmp.PotionSMP;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the Fire Potion's "Inferno Rage" active ability.
 * Pure aura effect - no blocks are placed or modified. A moving circular zone
 * around the player damages + ignites any player/mob standing inside it,
 * visualized with particles only.
 */
public class FireAuraManager {

    private final PotionSMP plugin;
    private final Map<UUID, Integer> activeTasks = new HashMap<>();

    public FireAuraManager(PotionSMP plugin) {
        this.plugin = plugin;
    }

    public void activateInfernoAura(Player player, int durationTicks, int radius) {
        UUID uid = player.getUniqueId();
        double dps = plugin.getConfig().getDouble("potions.fire.damage-per-second", 2.0);

        Integer existing = activeTasks.remove(uid);
        if (existing != null) plugin.getServer().getScheduler().cancelTask(existing);

        BukkitRunnable auraTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks || !player.isOnline()) {
                    activeTasks.remove(uid);
                    cancel();
                    return;
                }

                Location center = player.getLocation();

                if (ticks % 20 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, radius, 2.5, radius)) {
                        if (e instanceof LivingEntity le && !e.getUniqueId().equals(uid)) {
                            le.damage(dps, player);
                            le.setFireTicks(40);
                        }
                    }
                }

                drawRing(center, radius);
                center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(0, 0.1, 0), 4, 0.2, 0, 0.2, 0.02);

                ticks++;
            }
        };

        int taskId = auraTask.runTaskTimer(plugin, 0L, 1L).getTaskId();
        activeTasks.put(uid, taskId);
    }

    private void drawRing(Location center, int radius) {
        int points = Math.max(24, radius * 6);
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location ringPoint = new Location(center.getWorld(), x, center.getY() + 0.1, z);
            center.getWorld().spawnParticle(Particle.FLAME, ringPoint, 1, 0, 0, 0, 0);
            if (i % 3 == 0) {
                center.getWorld().spawnParticle(Particle.LAVA, ringPoint, 1, 0, 0, 0, 0);
            }
        }
    }

    public void cancelAura(UUID uid) {
        Integer taskId = activeTasks.remove(uid);
        if (taskId != null) plugin.getServer().getScheduler().cancelTask(taskId);
    }

    public void cancelAll() {
        for (int id : activeTasks.values()) {
            plugin.getServer().getScheduler().cancelTask(id);
        }
        activeTasks.clear();
    }
}
