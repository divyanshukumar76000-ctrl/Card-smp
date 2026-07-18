package com.potionsmp.utils;

public enum PotionType {

    FIRE     ("fire",     "§cFire Potion"),
    FREEZE   ("freeze",   "§bFreeze Potion"),
    REGEN    ("regen",    "§dRegen Potion"),
    GLITCH   ("glitch",   "§dGlitch Potion"),
    SHIELD   ("shield",   "§9Shield Potion"),
    ENDER    ("ender",    "§5Ender Effect"),
    TELEPORT ("teleport", "§3Teleport Potion"),
    SPEED    ("speed",    "§eSpeed Potion"),
    FEATHER  ("feather",  "§fFeather Potion"),
    EMERALD  ("emerald",  "§aEmerald Potion"),
    STRENGTH ("strength", "§4Strength Potion");

    private final String id;
    private final String displayName;

    PotionType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }

    public static PotionType fromId(String id) {
        if (id == null) return null;
        for (PotionType t : values())
            if (t.id.equalsIgnoreCase(id)) return t;
        return null;
    }
}
