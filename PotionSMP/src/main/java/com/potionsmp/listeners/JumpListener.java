package com.potionsmp.listeners;

import com.potionsmp.PotionSMP;
import com.potionsmp.abilities.RegenAbility;
import com.potionsmp.utils.PotionType;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fixed double-jump detection using a two-phase approach:
 *
 * Phase 1 - PlayerJumpEvent: records "this player just jumped"
 * Phase 2 - PlayerMoveEvent: tracks when the player actually LANDS
 *           (isOnGround() becomes true after being airborne). The landing
 *           sets a "can double jump" flag with a wider time window (800ms).
 *           The NEXT PlayerJumpEvent while that flag is active = double jump.
 *
 * This is more reliable than pure timestamp-only detection because it
 * explicitly requires: jump → land → jump (not just two rapid jumps
 * from the same ground contact).
 */
public class JumpListener implements Listener {

    private final PotionSMP plugin;

    // Tracks players who are airborne (jumped but not yet landed)
    private final Map<UUID, Boolean> airborne = new HashMap<>();
    // Tracks players who landed and are eligible for a double-jump trigger
    private final Map<UUID, Long> landedAt = new HashMap<>();
    private static final long DOUBLE_JUMP_WINDOW_MS = 800;

    public JumpListener(PotionSMP plugin) {
        this.plugin = plugin;
    }

    private boolean regenEquipped(Player player) {
        PotionType s1 = plugin.getSlotManager().getSlot1(player.getUniqueId());
        PotionType s2 = plugin.getSlotManager().getSlot2(player.getUniqueId());
        return s1 == PotionType.REGEN || s2 == PotionType.REGEN;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getRegenAbilityManager().isArmed(player.getUniqueId())) return;
        if (!regenEquipped(player)) return;

        UUID uid = player.getUniqueId();
        Long landed = landedAt.get(uid);

        if (landed != null && (System.currentTimeMillis() - landed) <= DOUBLE_JUMP_WINDOW_MS) {
            // Double jump confirmed: they landed and jumped again within window
            landedAt.remove(uid);
            airborne.put(uid, true);

            RegenAbility ability = (RegenAbility) plugin.getAbilityRegistry().get(PotionType.REGEN);
            ability.triggerVitalitySurge(plugin, player);
        } else {
            // First jump - mark as airborne, clear previous land
            airborne.put(uid, true);
            landedAt.remove(uid);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uid = player.getUniqueId();

        if (!airborne.getOrDefault(uid, false)) return;
        if (!plugin.getRegenAbilityManager().isArmed(uid)) return;

        // Player was airborne and just touched ground
        if (player.isOnGround()) {
            airborne.put(uid, false);
            landedAt.put(uid, System.currentTimeMillis());
        }
    }
}
