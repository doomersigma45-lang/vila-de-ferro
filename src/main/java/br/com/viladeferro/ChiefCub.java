package br.com.viladeferro;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

/**
 * Filhote do Chefe Villager. Comporta-se como um lobo domado: segue o dono, ataca quem o ataca
 * e quem o dono ataca, e invoca guerreiros e golems de ferro. Ao crescer fica mais forte.
 */
public class ChiefCub extends TamableAnimal {
    private static final int GROW_TICKS = 12000; // 10 minutos
    private int summonCooldown = 100;

    public ChiefCub(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createCubAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.1D, 8.0F, 2.0F, false));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    /** Começa como filhote; cresce sozinho depois de alguns minutos. */
    public void becomeBaby() {
        setAge(-GROW_TICKS);
        applyStage(true);
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        applyStage(true);
    }

    private void applyStage(boolean heal) {
        boolean baby = isBaby();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(baby ? 20.0D : 60.0D);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baby ? 3.0D : 8.0D);
        if (heal) setHealth(getMaxHealth());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        applyStage(false);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        EntityDimensions base = super.getDimensions(pose);
        return isBaby() ? base.scale(0.5F) : base;
    }

    // ---------- Alvos ----------

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && (target == getOwner() || VilaDeFerro.isNeverTarget(target))) return;
        super.setTarget(target);
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (VilaDeFerro.isNeverTarget(target)) return false;
        if (target instanceof TamableAnimal pet && pet.isTame() && pet.getOwner() == owner) return false;
        if (target instanceof Player other && owner instanceof Player ownerPlayer && !ownerPlayer.canHarmPlayer(other)) return false;
        return true;
    }

    // ---------- Invocação ----------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel serverLevel)) return;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        if (--summonCooldown > 0) return;

        boolean baby = isBaby();
        summonCooldown = baby ? 240 : 160;
        int warriorCap = baby ? 3 : 7;
        int golemCap = baby ? 1 : 2;

        long warriors = serverLevel.getEntitiesOfClass(FighterVillager.class, getBoundingBox().inflate(24),
                f -> !(f instanceof ChiefVillager)).size();
        long golems = serverLevel.getEntitiesOfClass(IronGolem.class, getBoundingBox().inflate(24)).size();

        int newWarriors = (int) Math.max(0, Math.min(baby ? 1 : 3, warriorCap - warriors));
        for (int i = 0; i < newWarriors; i++) {
            FighterVillager ally = VilaDeFerro.FIGHTER.get().create(serverLevel);
            if (ally == null) continue;
            place(ally, i, newWarriors);
            ally.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(ally.blockPosition()), MobSpawnType.EVENT, null, null);
            ally.setTarget(target);
            serverLevel.addFreshEntity(ally);
        }
        if (golems < golemCap) {
            IronGolem golem = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(serverLevel);
            if (golem != null) {
                golem.setPlayerCreated(true);
                place(golem, 0, 1);
                golem.setTarget(target);
                serverLevel.addFreshEntity(golem);
            }
        }
    }

    private void place(Mob mob, int index, int total) {
        double angle = (Math.PI * 2 * index / Math.max(1, total)) + random.nextDouble() * 0.5;
        mob.moveTo(getX() + Math.cos(angle) * 2.5, getY(), getZ() + Math.sin(angle) * 2.5, random.nextFloat() * 360F, 0F);
    }

    // ---------- Requisitos de Animal ----------

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }
}
