package com.potionsmp.managers;

import com.potionsmp.PotionSMP;
import com.potionsmp.utils.PotionUtils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class GlitchManager {

    private final PotionSMP plugin;
    // Players who cannot pick up items
    private final Set<UUID> noPickup = new HashSet<>();
    // Active corruption tasks on targets (targetUUID -> taskId)
    private final Map<UUID, Integer> corruptionTasks = new HashMap<>();
    // Armed attackers (attackerUUID -> {duration, stutters}) waiting to deploy on next hit
    private final Map<UUID, int[]> armedAttackers = new HashMap<>();

    public GlitchManager(PotionSMP plugin) {
        this.plugin = plugin;
    }

    public boolean hasNoPickup(UUID uuid) { return noPickup.contains(uuid); }

    public void armCorruption(UUID attackerUuid, int duration, int stutters) {
        armedAttackers.put(attackerUuid, new int[]{duration, stutters});
    }

    public boolean isArmed(UUID attackerUuid) {
        return armedAttackers.containsKey(attackerUuid);
    }

    /** Called from CombatListener when an armed Glitch player hits an entity */
    public void deployOnHit(Player attacker, Player target) {
        int[] params = armedAttackers.remove(attacker.getUniqueId());
        if (params == null) return;
        applySystemCorruption(attacker, target, params[0], params[1]);
    }

    public void applySystemCorruption(Player attacker, Player target, int durationTicks, int stutterCount) {
        UUID uid = target.getUniqueId();

        Integer existing = corruptionTasks.remove(uid);
        if (existing != null) plugin.getServer().getScheduler().cancelTask(existing);

        // Force drop held item
        ItemStack held = target.getInventory().getItemInMainHand();
        if (held != null && !held.getType().isAir()) {
            target.getInventory().setItemInMainHand(null);
            Item dropped = target.getWorld().dropItem(target.getLocation(), held);
            dropped.setPickupDelay(40);
            target.getWorld().spawnParticle(Particle.PORTAL,
                target.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.5);
            target.getWorld().playSound(target.getLocation(),
                Sound.ENTITY_ITEM_BREAK, 1f, 0.5f);
        }

        noPickup.add(uid);

        target.sendTitle(
            PotionUtils.colorize("&d&l⚡ SYSTEM CORRUPTED"),
            PotionUtils.colorize("&5Cannot pick up items!"),
            5, 40, 10
        );
        target.sendMessage(PotionUtils.colorize("&d⚡ System Corruption by &f" + attacker.getName()));
        target.getWorld().playSound(target.getLocation(),
            Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.3f);

        int stutterInterval = Math.max(1, durationTicks / (stutterCount + 1));

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            int stuttersDone = 0;
            final Random rand = new Random();

            @Override
            public void run() {
                if (ticks >= durationTicks || !target.isOnline()) {
                    cleanup(uid);
                    target.sendMessage(PotionUtils.colorize("&7System Corruption faded."));
                    target.getWorld().playSound(target.getLocation(),
                        Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1f, 0.8f);
                    cancel(); return;
                }

                Location loc = target.getLocation();

                // Glitch particles
                if (ticks % 2 == 0) {
                    loc.getWorld().spawnParticle(Particle.DUST,
                        loc.clone().add(rnd(rand), rnd(rand)+1, rnd(rand)), 3,
                        new Particle.DustOptions(Color.fromRGB(150, 0, 255), 0.8f));
                    loc.getWorld().spawnParticle(Particle.DUST,
                        loc.clone().add(rnd(rand), rnd(rand)+1, rnd(rand)), 2,
                        new Particle.DustOptions(Color.fromRGB(0, 255, 220), 0.8f));
                    loc.getWorld().spawnParticle(Particle.END_ROD,
                        loc.clone().add(rnd(rand)*0.5, 1+rnd(rand), rnd(rand)*0.5),
                        1, 0, 0, 0, 0.05);
                }
                if (ticks % 10 == 0) {
                    loc.getWorld().spawnParticle(Particle.PORTAL,
                        loc.clone().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.3);
                    loc.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f, 2f);
                }

                // Micro-teleport stutter
                if (stuttersDone < stutterCount && ticks == stutterInterval * (stuttersDone + 1)) {
                    stuttersDone++;
                    double ox = (rand.nextDouble()*2-1) * (1+rand.nextDouble());
                    double oz = (rand.nextDouble()*2-1) * (1+rand.nextDouble());
                    Location stutter = loc.clone().add(ox, 0, oz);
                    stutter.setYaw(loc.getYaw());
                    stutter.setPitch(loc.getPitch());

                    if (stutter.getBlock().getType().isAir() &&
                        stutter.clone().add(0,1,0).getBlock().getType().isAir()) {
                        target.teleport(stutter);
                    }
                    loc.getWorld().spawnParticle(Particle.PORTAL, stutter, 25, 0.3, 0.5, 0.3, 0.4);
                    loc.getWorld().spawnParticle(Particle.END_ROD, stutter, 10, 0.3, 0.3, 0.3, 0.1);
                    loc.getWorld().playSound(stutter, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1f, 1.5f);
                    target.setVelocity(new Vector(
                        (rand.nextDouble()-0.5)*0.3, 0.1, (rand.nextDouble()-0.5)*0.3));
                }
                ticks++;
            }
        };
        int taskId = task.runTaskTimer(plugin, 0L, 1L).getTaskId();
        corruptionTasks.put(uid, taskId);
    }

    private void cleanup(UUID uid) {
        noPickup.remove(uid);
        corruptionTasks.remove(uid);
    }

    private double rnd(Random r) { return (r.nextDouble()-0.5)*1.2; }

    public void cancelAll() {
        corruptionTasks.values().forEach(id -> plugin.getServer().getScheduler().cancelTask(id));
        corruptionTasks.clear();
        noPickup.clear();
        armedAttackers.clear();
    }
}
