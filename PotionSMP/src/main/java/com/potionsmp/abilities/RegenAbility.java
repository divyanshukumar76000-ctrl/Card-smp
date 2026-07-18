package com.potionsmp.abilities;

import com.potionsmp.PotionSMP;
import com.potionsmp.managers.ParticleManager;
import com.potionsmp.utils.PotionType;
import com.potionsmp.utils.PotionUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RegenAbility implements Ability {

    @Override
    public PotionType type() { return PotionType.REGEN; }

    @Override
    public void onEquip(PotionSMP plugin, Player player) {
        int amp = plugin.getConfig().getInt("potions.regen.regen-amplifier", 0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,
            Integer.MAX_VALUE, amp, false, false));
    }

    @Override
    public void onUnequip(PotionSMP plugin, Player player) {
        player.removePotionEffect(PotionEffectType.REGENERATION);
        plugin.getRegenAbilityManager().clearCharge(player.getUniqueId());
        plugin.getRegenAbilityManager().disarm(player.getUniqueId());
        plugin.getParticleManager().cancelAura(player.getUniqueId());
    }

    @Override
    public void onHit(PotionSMP plugin, Player player, LivingEntity target) {
        if (!plugin.getRegenAbilityManager().isChargedForDive(player.getUniqueId())) return;
        double dmg = plugin.getConfig().getDouble("potions.regen.mace-dive-damage", 10.0) * 2;
        plugin.getRegenAbilityManager().triggerMaceDive(player, target, dmg);
        player.sendMessage(PotionUtils.msg("mace-dive-hit").replace("%target%", target.getName()));
    }

    @Override
    public ActivationResult activate(PotionSMP plugin, Player player) {
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), type()))
            return ActivationResult.ON_COOLDOWN;

        int cooldown = plugin.getConfig().getInt("potions.regen.active-cooldown", 45);
        plugin.getCooldownManager().setCooldown(player.getUniqueId(), type(), cooldown);
        plugin.getRegenAbilityManager().arm(player.getUniqueId());
        player.sendMessage(PotionUtils.colorize("&a✦ Vitality Surge armed! Double-jump to launch."));
        return ActivationResult.SUCCESS;
    }

    public void triggerVitalitySurge(PotionSMP plugin, Player player) {
        int height = plugin.getConfig().getInt("potions.regen.active-jump-height", 10);
        int duration = plugin.getConfig().getInt("potions.regen.active-duration", 200);
        int amp = plugin.getConfig().getInt("potions.regen.regen-amplifier", 0);

        player.sendMessage(PotionUtils.msg("regen-active"));

        // Start heavy regen aura
        plugin.getParticleManager().startRegenAura(player, duration);

        // Launch + passive particle burst
        ParticleManager.spawnRegenPassiveParticles(player.getLocation().clone().add(0, 1, 0));

        plugin.getRegenAbilityManager().launch(player, height, duration, amp + 1);
    }
}
