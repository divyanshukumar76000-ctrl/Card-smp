package com.potionsmp.commands;

import com.potionsmp.PotionSMP;
import com.potionsmp.gui.PotionGUI;
import com.potionsmp.utils.ItemBuilder;
import com.potionsmp.utils.PotionType;
import com.potionsmp.utils.PotionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class PotionCommand implements CommandExecutor, TabCompleter {

    private final PotionSMP plugin;

    public PotionCommand(PotionSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Console must specify a player: /potionSMP give <type> [player]");
                return true;
            }
            PotionGUI.open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("potionsmp.give")) {
                sender.sendMessage(PotionUtils.colorize("&cYou don't have permission."));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(PotionUtils.colorize("&cUsage: /potionSMP give <type> [player]"));
                return true;
            }
            PotionType type = PotionType.fromId(args[1]);
            if (type == null) {
                sender.sendMessage(PotionUtils.colorize("&cUnknown potion type. Options: " + availableTypes()));
                return true;
            }

            Player target;
            if (args.length >= 3) {
                target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(PotionUtils.colorize("&cPlayer not found: " + args[2]));
                    return true;
                }
            } else {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Specify a player name.");
                    return true;
                }
                target = (Player) sender;
            }

            target.getInventory().addItem(ItemBuilder.buildPotion(type));
            sender.sendMessage(PotionUtils.msg("gave-potion")
                    .replace("%potion%", type.getDisplayName())
                    .replace("%player%", target.getName()));
            return true;
        }

        if (args[0].equalsIgnoreCase("swap")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            plugin.getSlotManager().swap(player.getUniqueId());
            player.sendMessage(PotionUtils.colorize("&a✦ Swapped Slot 1 and Slot 2!"));
            return true;
        }

        sender.sendMessage(PotionUtils.colorize("&eUsage: /potionSMP [give <type> [player] | swap]"));
        return true;
    }

    private String availableTypes() {
        return Arrays.stream(PotionType.values()).map(PotionType::getId).reduce((a, b) -> a + ", " + b).orElse("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("give", "swap");
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Arrays.stream(PotionType.values()).map(PotionType::getId).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
