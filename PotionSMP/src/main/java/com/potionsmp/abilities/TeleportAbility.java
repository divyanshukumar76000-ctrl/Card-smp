package com.potionsmp.abilities;

import com.potionsmp.PotionSMP;
import com.potionsmp.utils.PotionType;
import com.potionsmp.utils.PotionUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * §3 Teleport Potion – Phase Shift
 *
 * PASSIVE: none
 *
 * ACTIVE – Phase Shift:
 * - Instantly teleport up to 20 blocks in the direction you're looking.
 * - Leaves a portal-particle ghost at the origin and bursts at destination.
 * - Cooldown: configurable (default 20s).
 */
public class TeleportAbility implements Ability {

    @Override
    public PotionType type() { return PotionType.TELEPORT; }

    @Override
    public ActivationResult activate(PotionSMP plugin, Player player) {
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), type()))
            return ActivationResult.ON_COOLDOWN;

        int   maxDist = plugin.getConfig().getInt("potions.teleport.active-distance", 20);
        int   cooldown = plugin.getConfig().getInt("potions.teleport.active-cooldown", 20);

        Location origin = player.getLocation();
        Location dest   = getDestination(player, maxDist);

        // Origin burst
        origin.getWorld().spawnParticle(Particle.PORTAL,       origin.clone().add(0,1,0), 80, 0.3,1,0.3, 0.8);
        origin.getWorld().spawnParticle(Particle.DRAGON_BREATH, origin.clone().add(0,1,0), 20, 0.2,0.5,0.2, 0.05);
        origin.getWorld().playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);

        player.teleport(dest);
        plugin.getCooldownManager().setCooldown(player.getUniqueId(), type(), cooldown);

        // Destination burst
        dest.getWorld().spawnParticle(Particle.PORTAL,       dest.clone().add(0,1,0), 80, 0.3,1,0.3, 0.8);
        dest.getWorld().spawnParticle(Particle.DRAGON_BREATH, dest.clone().add(0,1,0), 20, 0.2,0.5,0.2, 0.05);
        dest.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);

        player.sendMessage(PotionUtils.msg("teleport-active"));
        return ActivationResult.SUCCESS;
    }

    /**
     * Ray-trace up to maxDist blocks in the player's look direction,
     * stopping at solid blocks. Returns a safe standing location.
     */
    private Location getDestination(Player player, int maxDist) {
        RayTraceResult result = player.getWorld().rayTraceBlocks(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            maxDist
        );

        if (result != null && result.getHitPosition() != null) {
            Vector hit = result.getHitPosition();
            // Step back slightly so the player lands in front of the block, not inside it
            Location dest = hit.toLocation(player.getWorld());
            dest.subtract(player.getLocation().getDirection().normalize().multiply(0.5));
            dest.setYaw(player.getLocation().getYaw());
            dest.setPitch(player.getLocation().getPitch());
            // Ensure feet are on the ground
            dest.setY(Math.floor(dest.getY()));
            return dest;
        }

        // No obstruction — teleport to the full distance
        Location dest = player.getLocation().clone().add(
            player.getLocation().getDirection().normalize().multiply(maxDist));
        dest.setY(Math.floor(dest.getY()));
        return dest;
    }
}
