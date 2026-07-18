package com.potionsmp.abilities;

import com.potionsmp.PotionSMP;
import com.potionsmp.utils.PotionType;
import com.potionsmp.utils.PotionUtils;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * §a Emerald Potion – Lucky Miner
 *
 * PASSIVE:
 * - Permanent Luck
 * - Mob drops = Looting III
 * - 1.5× EXP
 * - 15% chance consumable not consumed
 *
 * ACTIVE – Hero's Blessing (11s, 50s CD):
 * - Hero of the Village
 * - 3× EXP
 * - 30% consumable save chance
 */
public class EmeraldAbility implements Ability, Listener {

    // Players with active Hero's Blessing
    private final Set<UUID> blessingActive = new HashSet<>();

    public void register(PotionSMP plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public PotionType type() { return PotionType.EMERALD; }

    @Override
    public void onEquip(PotionSMP plugin, Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, Integer.MAX_VALUE, 0, false, false));
        player.sendMessage(PotionUtils.colorize("&aLucky Miner passive active!"));
    }

    @Override
    public void onUnequip(PotionSMP plugin, Player player) {
        player.removePotionEffect(PotionEffectType.LUCK);
        player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
        blessingActive.remove(player.getUniqueId());
    }

    // ── Mob death: Looting III + 1.5x EXP ────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onMobDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        if (!PotionSMP.getInstance().getSlotManager()
                .hasEquipped(killer.getUniqueId(), PotionType.EMERALD)) return;

        boolean blessing = blessingActive.contains(killer.getUniqueId());
        double expMult   = blessing ? 3.0 : 1.5;

        // Multiply EXP
        e.setDroppedExp((int)(e.getDroppedExp() * expMult));

        // Extra loot drops (Looting III simulation: up to 3 extra rolls)
        List<org.bukkit.inventory.ItemStack> drops = new ArrayList<>(e.getDrops());
        List<org.bukkit.inventory.ItemStack> extraDrops = new ArrayList<>();
        for (org.bukkit.inventory.ItemStack drop : drops) {
            if (Math.random() < 0.4) { // ~40% extra drop chance per item
                org.bukkit.inventory.ItemStack extra = drop.clone();
                extra.setAmount(Math.min(extra.getAmount() + 1, extra.getMaxStackSize()));
                extraDrops.add(extra);
            }
        }
        e.getDrops().addAll(extraDrops);

        // Particle on kill
        e.getEntity().getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
            e.getEntity().getLocation().add(0,1,0), 8, 0.4,0.5,0.4, 0.05);
    }

    // ── Consumable save 15% (or 30% with blessing) ───────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onConsume(PlayerItemConsumeEvent e) {
        Player player = e.getPlayer();
        if (!PotionSMP.getInstance().getSlotManager()
                .hasEquipped(player.getUniqueId(), PotionType.EMERALD)) return;

        double chance = blessingActive.contains(player.getUniqueId()) ? 0.30 : 0.15;
        if (Math.random() < chance) {
            e.setCancelled(true);
            // Give item back
            plugin(player).getServer().getScheduler().runTaskLater(
                plugin(player), () -> {
                    if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                        player.getInventory().setItemInMainHand(e.getItem());
                    } else {
                        player.getInventory().addItem(e.getItem());
                    }
                }, 1L);
            player.sendActionBar(net.kyori.adventure.text.Component.text("§a✦ Item saved by Lucky Miner!"));
        }
    }

    // ── Active: Hero's Blessing ───────────────────────────────────────────
    @Override
    public ActivationResult activate(PotionSMP plugin, Player player) {
        UUID uid = player.getUniqueId();
        if (plugin.getCooldownManager().isOnCooldown(uid, type()))
            return ActivationResult.ON_COOLDOWN;

        int dur      = plugin.getConfig().getInt("potions.emerald.active-duration", 11);
        int cooldown = plugin.getConfig().getInt("potions.emerald.active-cooldown", 50);
        int ticks    = dur * 20;

        plugin.getCooldownManager().setCooldown(uid, type(), cooldown);
        blessingActive.add(uid);

        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, ticks, 0, false, false));

        // Visual
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0,1,0),
            30, 0.7,1,0.7, 0.05);
        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0,0.5,0),
            25, 0.5,0.8,0.5, 0.2);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.0f);

        player.sendTitle("§a✦ HERO'S BLESSING", "§73× EXP · 30% save · Hero of the Village", 5, 40, 10);

        // Schedule end
        new BukkitRunnable() {
            @Override
            public void run() {
                blessingActive.remove(uid);
                if (player.isOnline()) {
                    player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
                    player.sendActionBar(net.kyori.adventure.text.Component.text("§7Hero's Blessing ended."));
                }
            }
        }.runTaskLater(plugin, ticks);

        return ActivationResult.SUCCESS;
    }

    private PotionSMP plugin(Player p) { return PotionSMP.getInstance(); }
}
