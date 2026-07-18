package com.potionsmp.abilities;

import com.potionsmp.PotionSMP;
import com.potionsmp.utils.PotionType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * §f Feather Potion – Feather Glide
 *
 * PASSIVE ONLY:
 * - No fall damage (ever)
 * - Auto-glide when falling (slows descent, keeps horizontal momentum)
 * - Feather trail particles while gliding
 * - Glide ends on landing
 *
 * ACTIVE: None
 */
public class FeatherAbility implements Ability, Listener {

    private final Set<UUID> gliding = new HashSet<>();

    public void register(PotionSMP plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public PotionType type() { return PotionType.FEATHER; }

    @Override
    public void onEquip(PotionSMP plugin, Player player) {
        startGlideLoop(plugin, player);
        player.sendMessage("§fFeather Glide passive active — no fall damage!");
    }

    @Override
    public void onUnequip(PotionSMP plugin, Player player) {
        gliding.remove(player.getUniqueId());
        plugin.getParticleManager().cancelAura(player.getUniqueId());
    }

    // ── Fall damage cancel ────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onFallDamage(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(e.getEntity() instanceof Player p)) return;
        if (PotionSMP.getInstance().getSlotManager()
                .hasEquipped(p.getUniqueId(), PotionType.FEATHER)) {
            e.setCancelled(true);
        }
    }

    // ── Glide loop ────────────────────────────────────────────────────────
    private void startGlideLoop(PotionSMP plugin, Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() ||
                    !plugin.getSlotManager().hasEquipped(player.getUniqueId(), PotionType.FEATHER)) {
                    gliding.remove(player.getUniqueId());
                    cancel(); return;
                }

                Vector vel = player.getVelocity();

                // Falling (negative Y)
                if (vel.getY() < -0.1 && !player.isOnGround()) {
                    gliding.add(player.getUniqueId());

                    // Slow descent: cap downward velocity
                    double glideY = Math.max(vel.getY(), -0.15);
                    player.setVelocity(new Vector(vel.getX(), glideY, vel.getZ()));

                    // Feather trail
                    Location loc = player.getLocation().add(0, 0.2, 0);
                    loc.getWorld().spawnParticle(Particle.CLOUD,       loc, 2, 0.2,0.1,0.2, 0.02);
                    loc.getWorld().spawnParticle(Particle.END_ROD,     loc, 1, 0.2,0.1,0.2, 0.01);

                } else if (player.isOnGround()) {
                    if (gliding.remove(player.getUniqueId())) {
                        // Landing puff
                        Location loc = player.getLocation();
                        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 8, 0.4,0.1,0.4, 0.05);
                        loc.getWorld().playSound(loc, Sound.BLOCK_WOOL_STEP, 0.8f, 1.2f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ── Active: none ──────────────────────────────────────────────────────
    @Override
    public ActivationResult activate(PotionSMP plugin, Player player) {
        player.sendActionBar(net.kyori.adventure.text.Component.text(
            "§fFeather Potion has no active ability — it's fully passive!"));
        return ActivationResult.SUCCESS;
    }
}
