package com.potionsmp.listeners;

import com.potionsmp.PotionSMP;
import com.potionsmp.utils.ItemBuilder;
import com.potionsmp.utils.PotionType;
import com.potionsmp.utils.PotionUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    private final PotionSMP plugin;

    public GUIListener(PotionSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().title() == null) return;

        String title = event.getView().title().toString();
        if (!title.contains("PotionSMP")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;

        PotionType potionType = PotionUtils.getPotionType(event.getCurrentItem());
        if (potionType == null) return;

        player.closeInventory();
        player.getInventory().addItem(ItemBuilder.buildPotion(potionType));

        player.sendMessage(PotionUtils.colorize("&a✦ You received a ") + potionType.getDisplayName() + PotionUtils.colorize("&a!"));
        player.sendMessage(PotionUtils.colorize("&7Drink it to equip it to Slot 1."));
    }
}
