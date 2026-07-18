package com.potionsmp.managers;

import com.potionsmp.utils.PotionType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Counts combo hits per player per potion type, used for passive abilities
 * (e.g. "every 10 hits"). This includes a debounce window to fix the bug where
 * triggers sometimes fired early at hit 7/8/9: Minecraft can occasionally fire
 * EntityDamageByEntityEvent more than once for what is visually a single swing
 * (e.g. attack + knockback follow-up, or another plugin re-dispatching the
 * event). Hits arriving within the debounce window of the previous counted hit
 * for the SAME target are treated as one combo hit instead of two.
 */
public class HitCounterManager {

    // key = uuid + ":" + potionTypeId  ->  current combo count
    private final Map<String, Integer> counts = new HashMap<>();
    // key = uuid + ":" + potionTypeId  ->  last counted hit's (target uuid + tick)
    private final Map<String, LastHit> lastHits = new HashMap<>();

    private static final long DEBOUNCE_TICKS = 2; // ~0.1s; real separate swings are always further apart

    private record LastHit(UUID targetUuid, long tick) {}

    private String key(UUID uuid, PotionType type) {
        return uuid + ":" + type.getId();
    }

    /**
     * Registers a hit. Returns the new combo count, or -1 if this hit was
     * suppressed as a duplicate (within the debounce window against the same target).
     */
    public int registerHit(UUID uuid, PotionType type, UUID targetUuid, long currentTick) {
        String k = key(uuid, type);
        LastHit last = lastHits.get(k);

        if (last != null && last.targetUuid().equals(targetUuid) && (currentTick - last.tick()) <= DEBOUNCE_TICKS) {
            return -1; // duplicate event for the same swing, ignore
        }

        lastHits.put(k, new LastHit(targetUuid, currentTick));
        int newCount = counts.getOrDefault(k, 0) + 1;
        counts.put(k, newCount);
        return newCount;
    }

    public void reset(UUID uuid, PotionType type) {
        counts.put(key(uuid, type), 0);
    }

    public int get(UUID uuid, PotionType type) {
        return counts.getOrDefault(key(uuid, type), 0);
    }
}
