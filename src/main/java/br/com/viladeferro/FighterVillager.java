package br.com.viladeferro;

import com.mojang.serialization.Dynamic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import javax.annotation.Nullable;
import java.util.EnumSet;

public class FighterVillager extends Villager implements net.minecraft.world.entity.monster.RangedAttackMob {
    private int bowCooldown;
    private int meleeCooldown;

    public FighterVillager(EntityType<? extends Villager> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createFighterAttributes() {
        return Villager.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    // Guerreiros usam goals clássicos; o cérebro de aldeão (pânico, trabalho, sono) foi desativado
    // para não conflitar com o combate.
    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return brainProvider().makeBrain(dynamic);
    }

    @Override
    public void refreshBrain(ServerLevel level) {
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, InteractionHand hand) {
        return net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new CombatGoal(this));
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, e -> !VilaDeFerro.isNeverTarget(e)));
    }

    /** Nunca mira em aliados da vila nem em creepers. */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && VilaDeFerro.isNeverTarget(target)) return;
        super.setTarget(target);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        ItemStack ammo = new ItemStack(Items.ARROW);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammo, velocity);
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double dy = target.getY(0.3333333333333333) - arrow.getY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horizontal * 0.2, dz, 1.6F, 14 - level().getDifficulty().getId() * 4);
        playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(arrow);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable net.minecraft.nbt.CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        var random = getRandom();
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextBoolean() ? Items.IRON_SWORD : Items.BOW));
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(random.nextInt(3) == 0 ? Items.IRON_HELMET : Items.LEATHER_HELMET));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(random.nextInt(3) == 0 ? Items.IRON_CHESTPLATE : Items.LEATHER_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        return result;
    }

    private static class CombatGoal extends Goal {
        private final FighterVillager mob;
        private LivingEntity target;
        CombatGoal(FighterVillager mob) { this.mob = mob; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { target = mob.getTarget(); return target != null && target.isAlive(); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void tick() {
            target = mob.getTarget();
            if (target == null) return;
            mob.getLookControl().setLookAt(target, 30, 30);
            double d2 = mob.distanceToSqr(target);
            boolean ranged = mob.getMainHandItem().is(Items.BOW);
            if (ranged) {
                if (d2 > 100) mob.getNavigation().moveTo(target, 1.0);
                else mob.getNavigation().stop();
                if (mob.bowCooldown-- <= 0 && d2 < 22 * 22) {
                    mob.performRangedAttack(target, 1.0F);
                    mob.bowCooldown = 35;
                }
            } else {
                if (mob.meleeCooldown > 0) mob.meleeCooldown--;
                mob.getNavigation().moveTo(target, 1.15);
                if (d2 < 3.2 * 3.2 && mob.meleeCooldown <= 0) {
                    mob.swing(InteractionHand.MAIN_HAND);
                    mob.doHurtTarget(target);
                    mob.meleeCooldown = 20;
                }
            }
        }
    }
}
