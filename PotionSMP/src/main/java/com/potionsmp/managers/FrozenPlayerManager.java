package com.potionsmp.managers;

import com.potionsmp.PotionSMP;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles freezing entities in place - used both by Freeze's passive
 * (brief 1s "struck"/stun on every 10th hit) and its active Arctic Prison
 * (full 3s area freeze). Locks position every tick and applies heavy
 * Slowness/Mining Fatigue so the entity can't move, attack, mine, or act.
 */
public class FrozenPlayerManager {

    private final PotionSMP plugin;
    private final Map<UUID, Location> frozenEntities = new HashMap<>();
    private final Map<UUID, Integer> frozenTaskIds = new HashMap<>();

    public FrozenPlayerManager(PotionSMP plugin) {
        this.plugin = plugin;
    }

    public void freezeEntity(LivingEntity entity, int durationTicks) {
        UUID uid = entity.getUniqueId();
        Location loc = entity.getLocation().clone();
        frozenEntities.put(uid, loc);

        int dur = durationTicks + 5;
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, dur, 254, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, dur, 254, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, dur, 128, false, false));

        // Cancel any previous freeze task for this entity before starting a new one
        Integer existing = frozenTaskIds.remove(uid);
        if (existing != null) plugin.getServer().getScheduler().cancelTask(existing);

        BukkitRunnable lockTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks || !entity.isValid()) {
                    unfreeze(entity);
                    cancel();
                    return;
                }
                if (entity.getLocation().distanceSquared(loc) > 0.1) {
                    entity.teleport(loc);
                }
                entity.setVelocity(new Vector(0, 0, 0));

                entity.getWorld().spawnParticle(Particle.SNOWFLAKE, entity.getLocation().add(0, 1, 0), 6, 0.3, 0.5, 0.3, 0.01);
                entity.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, entity.getLocation().add(0, 0.5, 0), 3, 0.3, 0.3, 0.3, 0);

                ticks++;
            }
        };
        int taskId = lockTask.runTaskTimer(plugin, 0L, 1L).getTaskId();
        frozenTaskIds.put(uid, taskId);
    }

    public void unfreeze(LivingEntity entity) {
        UUID uid = entity.getUniqueId();
        frozenEntities.remove(uid);
        Integer taskId = frozenTaskIds.remove(uid);
        if (taskId != null) plugin.getServer().getScheduler().cancelTask(taskId);
        entity.removePotionEffect(PotionEffectType.SLOWNESS);
        entity.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        entity.removePotionEffect(PotionEffectType.JUMP_BOOST);
    }

    public boolean isFrozen(UUID uid) {
        return frozenEntities.containsKey(uid);
    }

    public void unfreezeAll() {
        for (int id : frozenTaskIds.values()) {
            plugin.getServer().getScheduler().cancelTask(id);
        }
        frozenEntities.clear();
        frozenTaskIds.clear();
    }
}
