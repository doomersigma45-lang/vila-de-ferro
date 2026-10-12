package br.com.viladeferro;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;

/** Quem invocou o guerreiro/golem define como ele se comporta com jogadores. */
public enum Tier {
    /** Nascido naturalmente (vila, ovo): neutro, só revida se provocado. */
    NATURAL,
    /** Invocado pelo Chefe: agressivo com jogadores. */
    AGGRESSIVE,
    /** Invocado pelo Filhote: nunca ataca jogadores. */
    PASSIVE;

    private static final String KEY = "viladeferro_tier";

    public static Tier of(Entity e) {
        if (e instanceof FighterVillager f) return f.getTier();
        if (e instanceof IronGolem) {
            String s = e.getPersistentData().getString(KEY);
            for (Tier t : values()) if (t.name().equals(s)) return t;
        }
        return NATURAL;
    }

    public static void set(IronGolem golem, Tier tier) {
        golem.getPersistentData().putString(KEY, tier.name());
    }
}
