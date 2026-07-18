package com.potionsmp.managers;

import com.potionsmp.utils.PotionType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks which PotionType (if any) occupies Slot 1 and Slot 2 for each player.
 * Drinking a potion always goes into Slot 1, replacing whatever was there
 * (Slot 2 is untouched). A future /swap command can move Slot 1 <-> Slot 2.
 */
public class SlotManager {

    public static final int SLOT_1 = 1;
    public static final int SLOT_2 = 2;

    private final Map<UUID, PotionType> slot1 = new HashMap<>();
    private final Map<UUID, PotionType> slot2 = new HashMap<>();

    public PotionType getSlot1(UUID uuid) {
        return slot1.get(uuid);
    }

    public PotionType getSlot2(UUID uuid) {
        return slot2.get(uuid);
    }

    public PotionType getSlot(UUID uuid, int slot) {
        return slot == SLOT_1 ? getSlot1(uuid) : getSlot2(uuid);
    }

    /**
     * Places a potion into Slot 1, returning whatever was previously there
     * (or null if Slot 1 was empty).
     */
    public PotionType setSlot1(UUID uuid, PotionType type) {
        return slot1.put(uuid, type);
    }

    public PotionType setSlot2(UUID uuid, PotionType type) {
        return slot2.put(uuid, type);
    }

    public void swap(UUID uuid) {
        PotionType a = slot1.get(uuid);
        PotionType b = slot2.get(uuid);
        if (b != null) slot1.put(uuid, b); else slot1.remove(uuid);
        if (a != null) slot2.put(uuid, a); else slot2.remove(uuid);
    }

    /** True if this potion type currently occupies EITHER slot for this player. */
    public boolean hasEquipped(UUID uuid, PotionType type) {
        return type != null && (type.equals(slot1.get(uuid)) || type.equals(slot2.get(uuid)));
    }

    public void clearAll(UUID uuid) {
        slot1.remove(uuid);
        slot2.remove(uuid);
    }
}
