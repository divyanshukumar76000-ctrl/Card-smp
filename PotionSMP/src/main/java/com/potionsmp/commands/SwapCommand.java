package com.potionsmp.commands;

import com.potionsmp.PotionSMP;
import com.potionsmp.utils.PotionType;
import com.potionsmp.utils.PotionUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SwapCommand implements CommandExecutor {

    private final PotionSMP plugin;

    public SwapCommand(PotionSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PotionUtils.colorize("&cThis command can only be used in-game."));
            return true;
        }

        PotionType slot1 = plugin.getSlotManager().getSlot1(player.getUniqueId());
        PotionType slot2 = plugin.getSlotManager().getSlot2(player.getUniqueId());

        if (slot1 == null && slot2 == null) {
            player.sendMessage(PotionUtils.colorize("&cBoth slots are empty — nothing to swap."));
            return true;
        }

        plugin.getSlotManager().swap(player.getUniqueId());

        String s1 = slot1 != null ? slot1.getDisplayName() : "&7Empty";
        String s2 = slot2 != null ? slot2.getDisplayName() : "&7Empty";
        player.sendMessage(PotionUtils.colorize(
            "&7Swapped slots: &fSlot 1 &7→ " + s2 + " &7| &fSlot 2 &7→ " + s1));
        return true;
    }
}
