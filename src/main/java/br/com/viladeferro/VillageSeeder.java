package br.com.viladeferro;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Coloca o Chefe e os guerreiros nas vilas (uma única vez por vila). */
public final class VillageSeeder {
    private VillageSeeder() {}

    public static void trySeed(ServerLevel level, ServerPlayer player) {
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(64),
                v -> v.getType() == EntityType.VILLAGER && v.isAlive() && !v.isBaby());
        if (villagers.size() < 3) return;

        Villager anchor = villagers.get(0);
        BlockPos center = anchor.blockPosition();
        if (!level.isVillage(center)) return;

        VillageSeedData data = VillageSeedData.get(level);
        if (data.isSeeded(center)) return;
        data.add(center);

        boolean hasChief = !level.getEntitiesOfClass(ChiefVillager.class, new AABB(center).inflate(128)).isEmpty();
        if (!hasChief) spawn(level, VilaDeFerro.CHIEF.get(), anchor);

        int fighters = Math.min(4, Math.max(2, villagers.size() / 3));
        for (int i = 0; i < fighters; i++) {
            Villager near = villagers.get(level.random.nextInt(villagers.size()));
            spawn(level, VilaDeFerro.FIGHTER.get(), near);
        }
    }

    private static void spawn(ServerLevel level, EntityType<? extends FighterVillager> type, Villager near) {
        FighterVillager mob = type.create(level);
        if (mob == null) return;
        mob.moveTo(near.getX(), near.getY(), near.getZ(), level.random.nextFloat() * 360F, 0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.STRUCTURE, null, null);
        level.addFreshEntity(mob);
    }
}
