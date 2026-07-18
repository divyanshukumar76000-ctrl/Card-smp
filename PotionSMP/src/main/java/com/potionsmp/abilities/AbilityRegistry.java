package com.potionsmp.abilities;

import com.potionsmp.utils.PotionType;
import java.util.HashMap;
import java.util.Map;

public class AbilityRegistry {

    private final Map<PotionType, Ability> abilities = new HashMap<>();

    public AbilityRegistry() {
        register(new FireAbility());
        register(new FreezeAbility());
        register(new RegenAbility());
        register(new GlitchAbility());
        register(new ShieldAbility());
        register(new EnderAbility());
        register(new TeleportAbility());
        register(new SpeedAbility());
        register(new FeatherAbility());
        register(new EmeraldAbility());
        register(new StrengthAbility());
    }

    private void register(Ability ability) {
        abilities.put(ability.type(), ability);
    }

    public Ability get(PotionType type) { return abilities.get(type); }
}
