package com.potionsmp.managers;

import com.potionsmp.PotionSMP;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the Ender Potion's "Void Domain" active ability.
 * Creates a moving zone of darkness and disorientation around the player —
 * nearby enemies periodically receive Blindness and Darkness while
 * portal and dragon-breath particles mark the boundary.
 */
public class EnderDomainManager {

    private final PotionSMP plugin;
    private final Map<UUID, Integer> activeTasks = new HashMap<>();

    public EnderDomainManager(PotionSMP plugin) {
        this.plugin = plugin;
    }

    public void activateDomain(Player player, int durationTicks, int radius) {
        UUID uid = player.getUniqueId();

        Integer existing = activeTasks.remove(uid);
        if (existing != null) plugin.getServer().getScheduler().cancelTask(existing);

        BukkitRunnable domainTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks || !player.isOnline()) {
                    activeTasks.remove(uid);
                    cancel();
                    return;
                }

                Location center = player.getLocation();

                // Apply Darkness + Blindness to nearby enemies every 30 ticks
                if (ticks % 30 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                        if (e instanceof LivingEntity le && !e.getUniqueId().equals(uid)) {
                            le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS,  40, 0, false, false));
                            le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false));
                        }
                    }
                }

                drawDomainRing(center, radius);
                center.getWorld().spawnParticle(Particle.PORTAL,
                    center.clone().add(0, 1, 0), 6, 0.3, 0.5, 0.3, 0.5);

                ticks++;
            }
        };

        int taskId = domainTask.runTaskTimer(plugin, 0L, 1L).getTaskId();
        activeTasks.put(uid, taskId);
    }

    private void drawDomainRing(Location center, int radius) {
        int points = Math.max(24, radius * 6);
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location ringPoint = new Location(center.getWorld(), x, center.getY() + 0.1, z);
            center.getWorld().spawnParticle(Particle.PORTAL, ringPoint, 1, 0, 0.5, 0, 0.1);
            if (i % 4 == 0) {
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH, ringPoint, 1, 0, 0.2, 0, 0);
            }
        }
    }

    public void cancelDomain(UUID uid) {
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
