package com.potionsmp.abilities;

import com.potionsmp.PotionSMP;
import com.potionsmp.utils.PotionType;
import com.potionsmp.utils.PotionUtils;
import org.bukkit.entity.Player;

/**
 * Shield Potion — Divine Barrier
 * No passive.
 * Active: full damage + knockback immunity for 8s, dual spinning particle rings.
 */
public class ShieldAbility implements Ability {

    @Override
    public PotionType type() { return PotionType.SHIELD; }

    @Override
    public ActivationResult activate(PotionSMP plugin, Player player) {
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), type()))
            return ActivationResult.ON_COOLDOWN;

        int duration = plugin.getConfig().getInt("potions.shield.active-duration", 160);
        int cooldown = plugin.getConfig().getInt("potions.shield.active-cooldown", 45);

        plugin.getCooldownManager().setCooldown(player.getUniqueId(), type(), cooldown);
        player.sendMessage(PotionUtils.msg("shield-active"));

        plugin.getShieldManager().activateShield(player, duration);

        return ActivationResult.SUCCESS;
    }
}
