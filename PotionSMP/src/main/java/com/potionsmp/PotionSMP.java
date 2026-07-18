package com.potionsmp;

import com.potionsmp.abilities.*;
import com.potionsmp.commands.*;
import com.potionsmp.hud.HudManager;
import com.potionsmp.listeners.*;
import com.potionsmp.managers.*;
import com.potionsmp.utils.PotionType;
import org.bukkit.plugin.java.JavaPlugin;

public class PotionSMP extends JavaPlugin {

    private static PotionSMP instance;

    private CooldownManager     cooldownManager;
    private HitCounterManager   hitCounterManager;
    private FrozenPlayerManager frozenPlayerManager;
    private FireAuraManager     fireAuraManager;
    private RegenAbilityManager regenAbilityManager;
    private EnderDomainManager  enderDomainManager;
    private ParticleManager     particleManager;
    private GlitchManager       glitchManager;
    private ShieldManager       shieldManager;
    private SlotManager         slotManager;
    private AbilityRegistry     abilityRegistry;
    private HudManager          hudManager;
    private JumpListener        jumpListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        cooldownManager     = new CooldownManager();
        hitCounterManager   = new HitCounterManager();
        frozenPlayerManager = new FrozenPlayerManager(this);
        fireAuraManager     = new FireAuraManager(this);
        regenAbilityManager = new RegenAbilityManager(this);
        enderDomainManager  = new EnderDomainManager(this);
        particleManager     = new ParticleManager(this);
        glitchManager       = new GlitchManager(this);
        shieldManager       = new ShieldManager(this);
        slotManager         = new SlotManager();
        abilityRegistry     = new AbilityRegistry();
        hudManager          = new HudManager(this);
        jumpListener        = new JumpListener(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new DrinkListener(this),  this);
        pm.registerEvents(new CombatListener(this), this);
        pm.registerEvents(jumpListener,              this);
        pm.registerEvents(new GUIListener(this),    this);

        // Register abilities that are also Listeners
        registerAbilityListener(PotionType.REGEN,    RegenAbility.class);
        registerAbilityListener(PotionType.ENDER,    EnderAbility.class);
        registerAbilityListener(PotionType.SPEED,    SpeedAbility.class);
        registerAbilityListener(PotionType.FEATHER,  FeatherAbility.class);
        registerAbilityListener(PotionType.EMERALD,  EmeraldAbility.class);
        registerAbilityListener(PotionType.STRENGTH, StrengthAbility.class);

        // Commands
        PotionCommand potionCmd = new PotionCommand(this);
        getCommand("potionsmp").setExecutor(potionCmd);
        getCommand("potionsmp").setTabCompleter(potionCmd);
        getCommand("slot1").setExecutor(new SlotCommand(this, SlotManager.SLOT_1));
        getCommand("slot2").setExecutor(new SlotCommand(this, SlotManager.SLOT_2));
        getCommand("swap").setExecutor(new SwapCommand(this));

        hudManager.start();

        getLogger().info("PotionSMP enabled! 11 Potions loaded.");
    }

    private void registerAbilityListener(PotionType type, Class<?> clazz) {
        Ability ability = abilityRegistry.get(type);
        if (ability == null) return;
        try {
            var method = clazz.getMethod("register", PotionSMP.class);
            method.invoke(ability, this);
        } catch (Exception e) {
            getLogger().warning("Could not register listener for " + type.name() + ": " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        frozenPlayerManager.unfreezeAll();
        fireAuraManager.cancelAll();
        enderDomainManager.cancelAll();
        particleManager.cancelAll();
        glitchManager.cancelAll();
        shieldManager.cancelAll();
        hudManager.stop();
        getLogger().info("PotionSMP disabled.");
    }

    public static PotionSMP getInstance()                    { return instance; }
    public CooldownManager getCooldownManager()              { return cooldownManager; }
    public HitCounterManager getHitCounterManager()          { return hitCounterManager; }
    public FrozenPlayerManager getFrozenPlayerManager()      { return frozenPlayerManager; }
    public FireAuraManager getFireAuraManager()              { return fireAuraManager; }
    public RegenAbilityManager getRegenAbilityManager()      { return regenAbilityManager; }
    public EnderDomainManager getEnderDomainManager()        { return enderDomainManager; }
    public SlotManager getSlotManager()                      { return slotManager; }
    public AbilityRegistry getAbilityRegistry()              { return abilityRegistry; }
    public HudManager getHudManager()                        { return hudManager; }
    public ParticleManager getParticleManager()              { return particleManager; }
    public GlitchManager getGlitchManager()                  { return glitchManager; }
    public ShieldManager getShieldManager()                  { return shieldManager; }
    public JumpListener getJumpListener()                    { return jumpListener; }
}
