package com.potionsmp.commands;

import com.potionsmp.PotionSMP;
import com.potionsmp.abilities.Ability;
import com.potionsmp.utils.PotionType;
import com.potionsmp.utils.PotionUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SlotCommand implements CommandExecutor {

    private final PotionSMP plugin;
    private final int slot;

    public SlotCommand(PotionSMP plugin, int slot) {
        this.plugin = plugin;
        this.slot = slot;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PotionUtils.colorize("&cThis command can only be used in-game."));
            return true;
        }

        PotionType type = plugin.getSlotManager().getSlot(player.getUniqueId(), slot);
        if (type == null) {
            player.sendMessage(PotionUtils.msg("no-potion"));
            return true;
        }

        Ability ability = plugin.getAbilityRegistry().get(type);
        if (ability == null) {
            player.sendMessage(PotionUtils.msg("no-potion"));
            return true;
        }

        Ability.ActivationResult result = ability.activate(plugin, player);

        if (result == Ability.ActivationResult.ON_COOLDOWN) {
            long rem = plugin.getCooldownManager().getRemainingSeconds(player.getUniqueId(), type);
            player.sendMessage(PotionUtils.msg("on-cooldown").replace("%seconds%", String.valueOf(rem)));
        } else if (result == Ability.ActivationResult.EMPTY_SLOT) {
            player.sendMessage(PotionUtils.msg("no-potion"));
        }

        return true;
    }
}
