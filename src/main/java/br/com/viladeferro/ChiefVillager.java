package br.com.viladeferro;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import javax.annotation.Nullable;

public class ChiefVillager extends FighterVillager {
    private int summonCooldown = 100;

    public ChiefVillager(EntityType<? extends net.minecraft.world.entity.npc.Villager> type, Level level) {
        super(type, level);
        setCustomName(net.minecraft.network.chat.Component.translatable("entity.viladeferro.chefe_villager"));
        setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createChiefAttributes() {
        return createFighterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel serverLevel)) return;
        var target = getTarget();
        if (target == null || !target.isAlive()) return;
        if (--summonCooldown > 0) return;
        summonCooldown = 180;

        long nearby = serverLevel.getEntitiesOfClass(FighterVillager.class, getBoundingBox().inflate(24), mob -> mob != this).size();
        int amount = (int)Math.max(0, Math.min(3, 6 - nearby));
        for (int i = 0; i < amount; i++) {
            FighterVillager ally = VilaDeFerro.FIGHTER.get().create(serverLevel);
            if (ally == null) continue;
            double angle = (Math.PI * 2 * i / Math.max(1, amount)) + random.nextDouble() * 0.4;
            ally.moveTo(getX() + Math.cos(angle) * 2.5, getY(), getZ() + Math.sin(angle) * 2.5, random.nextFloat() * 360, 0);
            ally.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(ally.blockPosition()), MobSpawnType.EVENT, null, null);
            ally.setTarget(target);
            serverLevel.addFreshEntity(ally);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        return result;
    }
}
