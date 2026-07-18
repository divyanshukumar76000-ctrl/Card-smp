package com.potionsmp.hud;

import com.potionsmp.PotionSMP;
import com.potionsmp.utils.PotionType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.Map;

public class HudManager {

    // Font is minecraft:default (defined in InfusePack assets/minecraft/font/default.json)
    private static final Key ICON_FONT = Key.key("minecraft", "default");

    // Empty slot background (infuse:icons/empty.png -> \uE901)
    private static final String SLOT_BG = "\uE901";

    // Space chars from InfusePack font
    private static final String SPACE_2PX = "\uE904";  // +2px right shift
    private static final String SPACE_6PX = "\uE905";  // +6px right shift

    private record IconInfo(String character, NamedTextColor color) {}

    private static final Map<PotionType, IconInfo> ICONS = new EnumMap<>(PotionType.class);
    static {
        ICONS.put(PotionType.FIRE,     new IconInfo("\uE006", NamedTextColor.RED));
        ICONS.put(PotionType.FREEZE,   new IconInfo("\uE004", NamedTextColor.AQUA));
        ICONS.put(PotionType.REGEN,    new IconInfo("\uE008", NamedTextColor.GREEN));
        ICONS.put(PotionType.GLITCH,   new IconInfo("\uE002", NamedTextColor.LIGHT_PURPLE));
        ICONS.put(PotionType.SHIELD,   new IconInfo("\uE003", NamedTextColor.WHITE));
        ICONS.put(PotionType.ENDER,    new IconInfo("\uE005", NamedTextColor.DARK_PURPLE));
        ICONS.put(PotionType.TELEPORT, new IconInfo("\uE007", NamedTextColor.DARK_AQUA));
        ICONS.put(PotionType.SPEED,    new IconInfo("\uE009", NamedTextColor.YELLOW));
        ICONS.put(PotionType.FEATHER,  new IconInfo("\uE00A", NamedTextColor.WHITE));
        ICONS.put(PotionType.EMERALD,  new IconInfo("\uE00B", NamedTextColor.GREEN));
        ICONS.put(PotionType.STRENGTH, new IconInfo("\uE00C", NamedTextColor.RED));
    }

    private final PotionSMP plugin;
    private BukkitTask task;

    public HudManager(PotionSMP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 10L);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PotionType slot1 = plugin.getSlotManager().getSlot1(player.getUniqueId());
            PotionType slot2 = plugin.getSlotManager().getSlot2(player.getUniqueId());

            // Always show both slot boxes (even if empty) once any potion is ever drunk
            // So we always render both if at least one slot is occupied
            if (slot1 == null && slot2 == null) continue;

            Component bar = Component.empty()
                    .append(buildSlot(player, slot1))
                    .append(Component.text("  ", NamedTextColor.DARK_GRAY))
                    .append(buildSlot(player, slot2));

            player.sendActionBar(bar);
        }
    }

    private Component buildSlot(Player player, PotionType type) {
        if (type == null) {
            // Empty slot: just the slot background icon
            return Component.text(SLOT_BG + SPACE_2PX)
                    .style(Style.style().font(ICON_FONT).build());
        }

        IconInfo info = ICONS.getOrDefault(type, new IconInfo("?", NamedTextColor.WHITE));
        boolean onCooldown = plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), type);
        long remaining = plugin.getCooldownManager().getRemainingSeconds(player.getUniqueId(), type);

        // Potion icon from InfusePack effects.png sheet
        Component icon = Component.text(info.character() + SPACE_2PX)
                .style(Style.style().font(ICON_FONT).build())
                .color(info.color());

        Component status = onCooldown
                ? Component.text(" " + remaining + "s", NamedTextColor.YELLOW)
                : Component.text(" Ready", NamedTextColor.GREEN);

        return Component.text()
                .append(icon)
                .append(status)
                .build();
    }
}
