package com.potionsmp.managers;

import com.potionsmp.utils.PotionType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<String, Long> cooldowns = new HashMap<>();

    private String key(UUID uuid, PotionType type) {
        return uuid + ":" + type.getId();
    }

    public boolean isOnCooldown(UUID uuid, PotionType type) {
        String k = key(uuid, type);
        if (!cooldowns.containsKey(k)) return false;
        return System.currentTimeMillis() < cooldowns.get(k);
    }

    public long getRemainingSeconds(UUID uuid, PotionType type) {
        String k = key(uuid, type);
        if (!cooldowns.containsKey(k)) return 0;
        long remaining = cooldowns.get(k) - System.currentTimeMillis();
        return remaining > 0 ? (remaining / 1000) + 1 : 0;
    }

    public void setCooldown(UUID uuid, PotionType type, int seconds) {
        cooldowns.put(key(uuid, type), System.currentTimeMillis() + (seconds * 1000L));
    }

    public void clearCooldown(UUID uuid, PotionType type) {
        cooldowns.remove(key(uuid, type));
    }
}
