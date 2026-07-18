package com.potionsmp.abilities;

import com.potionsmp.PotionSMP;
import com.potionsmp.utils.PotionType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Contract for a potion's ability set. Each PotionType (Fire, Freeze, Regen, ...)
 * has exactly one Ability implementation registered in AbilityRegistry.
 *
 * onHit() is called for every melee hit the player lands while this potion is
 * equipped in ANY slot - implementations are responsible for their own combo
 * counting (see HitCounterManager) since not every potion has a hit-based passive
 * (e.g. Regen's passive is just a permanent effect, not hit-triggered).
 *
 * activate() is called when the player triggers this potion's active ability via
 * /slot1 or /slot2 - implementations should check their own cooldown and return
 * the appropriate ActivationResult.
 *
 * onEquip()/onUnequip() let abilities apply/remove passive effects (e.g. Regen's
 * permanent Regeneration I) the moment a potion enters or leaves a slot.
 */
public interface Ability {

    PotionType type();

    /** Called once when this potion is placed into a slot (after drinking). */
    default void onEquip(PotionSMP plugin, Player player) {}

    /** Called once when this potion is removed from a slot (replaced by another). */
    default void onUnequip(PotionSMP plugin, Player player) {}

    /** Called for every hit the player lands while this ability is equipped in some slot. */
    default void onHit(PotionSMP plugin, Player player, LivingEntity target) {}

    /** Triggered via /slot1 or /slot2 when this potion occupies that slot. */
    ActivationResult activate(PotionSMP plugin, Player player);

    enum ActivationResult {
        SUCCESS, ON_COOLDOWN, EMPTY_SLOT
    }
}
