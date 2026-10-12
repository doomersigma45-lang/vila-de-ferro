package br.com.viladeferro;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

public class ChiefVillager extends FighterVillager {
    private int summonCooldown = 100;
    private boolean hasTotem;
    private boolean totemUsed;

    public ChiefVillager(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        setCustomName(Component.translatable("entity.viladeferro.chefe_villager"));
        setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createChiefAttributes() {
        return createFighterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    // ---------- Aura ----------

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) spawnAura();
    }

    private void spawnAura() {
        boolean enraged = hasEffect(MobEffects.DAMAGE_BOOST);
        double angle = tickCount * 0.35D;
        for (int i = 0; i < 2; i++) {
            double a = angle + Math.PI * i;
            double y = getY() + 0.2D + ((tickCount + i * 10) % 40) / 40.0D * getBbHeight();
            double x = getX() + Math.cos(a) * 0.85D;
            double z = getZ() + Math.sin(a) * 0.85D;
            level().addParticle(enraged ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.END_ROD, x, y, z, 0.0D, 0.02D, 0.0D);
        }
        if (enraged && tickCount % 4 == 0) {
            level().addParticle(ParticleTypes.FLAME, getRandomX(0.6D), getRandomY(), getRandomZ(0.6D), 0.0D, 0.03D, 0.0D);
        }
    }

    // ---------- Totem, buffs e invocação ----------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel serverLevel)) return;
        tickTotem();
        tickSummon(serverLevel);
    }

    private void tickTotem() {
        // O totem é consumido pelo próprio Minecraft ao quase morrer; quando some da mão, aplicamos os buffs.
        if (hasTotem && !getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            hasTotem = false;
            totemUsed = true;
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MobEffectInstance.INFINITE_DURATION, 1, false, true));
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, true));
            addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 2, false, true));
        }
    }

    private void tickSummon(ServerLevel serverLevel) {
        var target = getTarget();
        if (target == null || !target.isAlive()) return;
        if (--summonCooldown > 0) return;
        summonCooldown = 180;

        long nearby = serverLevel.getEntitiesOfClass(FighterVillager.class, getBoundingBox().inflate(24), mob -> mob != this).size();
        int amount = (int) Math.max(0, Math.min(3, 6 - nearby));
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

    // ---------- Equipamento ----------

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_AXE));
        setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        for (EquipmentSlot slot : EquipmentSlot.values()) setDropChance(slot, 0.0F);
        hasTotem = true;
        return result;
    }

    // ---------- Morte ----------

    @Override
    public void die(DamageSource source) {
        boolean alreadyDead = this.dead || this.isRemoved();
        super.die(source);
        if (alreadyDead || !(level() instanceof ServerLevel serverLevel)) return;

        dropGoodLoot();

        ServerPlayer killer = null;
        if (source.getEntity() instanceof ServerPlayer p) killer = p;
        else if (this.lastHurtByPlayer instanceof ServerPlayer p2 && this.lastHurtByPlayerTime > 0) killer = p2;
        if (killer == null) return;

        // Conquista
        var advancement = serverLevel.getServer().getAdvancements()
                .getAdvancement(new ResourceLocation(VilaDeFerro.MOD_ID, "derrotar_chefe"));
        if (advancement != null) killer.getAdvancements().award(advancement, "derrotado");

        // Peitoral de diamante encantado
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
        chest.enchant(Enchantments.UNBREAKING, 3);
        chest.enchant(Enchantments.MENDING, 1);
        spawnAtLocation(chest);

        // Filhote leal ao jogador
        ChiefCub cub = VilaDeFerro.CHIEF_CUB.get().create(serverLevel);
        if (cub != null) {
            cub.moveTo(getX(), getY(), getZ(), random.nextFloat() * 360F, 0F);
            cub.tame(killer);
            cub.becomeBaby();
            serverLevel.addFreshEntity(cub);
        }
    }

    private void dropGoodLoot() {
        spawnAtLocation(new ItemStack(Items.GOLDEN_APPLE, 1 + random.nextInt(3)));
        if (random.nextInt(100) < 15) spawnAtLocation(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        spawnAtLocation(new ItemStack(Items.DIAMOND_BLOCK, 1 + random.nextInt(2)));
        int books = 1 + random.nextInt(2);
        for (int i = 0; i < books; i++) {
            spawnAtLocation(EnchantmentHelper.enchantItem(random, new ItemStack(Items.BOOK), 20 + random.nextInt(11), true));
        }
    }

    // ---------- Save ----------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HasTotem", hasTotem);
        tag.putBoolean("TotemUsed", totemUsed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hasTotem = tag.getBoolean("HasTotem");
        totemUsed = tag.getBoolean("TotemUsed");
    }
}
