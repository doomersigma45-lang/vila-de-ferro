package br.com.viladeferro;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(VilaDeFerro.MOD_ID)
public class VilaDeFerro {
    public static final String MOD_ID = "viladeferro";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<EntityType<FighterVillager>> FIGHTER = ENTITIES.register("guerreiro_villager",
            () -> EntityType.Builder.of(FighterVillager::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("guerreiro_villager"));
    // Chefe 20% mais alto que um villager comum
    public static final RegistryObject<EntityType<ChiefVillager>> CHIEF = ENTITIES.register("chefe_villager",
            () -> EntityType.Builder.of(ChiefVillager::new, MobCategory.CREATURE).sized(0.72F, 2.34F).clientTrackingRange(12).build("chefe_villager"));
    public static final RegistryObject<EntityType<ChiefCub>> CHIEF_CUB = ENTITIES.register("filhote_chefe_villager",
            () -> EntityType.Builder.of(ChiefCub::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("filhote_chefe_villager"));

    public static final RegistryObject<Item> FIGHTER_EGG = ITEMS.register("guerreiro_villager_spawn_egg",
            () -> new ForgeSpawnEggItem(FIGHTER, 0x9A6A43, 0x355C43, new Item.Properties()));
    public static final RegistryObject<Item> CHIEF_EGG = ITEMS.register("chefe_villager_spawn_egg",
            () -> new ForgeSpawnEggItem(CHIEF, 0x67432F, 0xC49A3A, new Item.Properties()));

    public VilaDeFerro() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::addCreative);
        bus.addListener(this::registerAttributes);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(FIGHTER.get(), FighterVillager.createFighterAttributes().build());
        event.put(CHIEF.get(), ChiefVillager.createChiefAttributes().build());
        event.put(CHIEF_CUB.get(), ChiefCub.createCubAttributes().build());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(FIGHTER_EGG);
            event.accept(CHIEF_EGG);
        }
    }

    /** Aliados da vila: guerreiros, chefe e filhote do chefe. */
    public static boolean isFriend(Entity e) {
        return e instanceof FighterVillager || e instanceof ChiefCub;
    }

    /** Guerreiros comuns e golems de ferro: nunca atacam jogadores nem o filhote. */
    public static boolean isPeaceful(Entity e) {
        return e instanceof IronGolem || (e instanceof FighterVillager && !(e instanceof ChiefVillager));
    }

    /** Alvos que nenhum membro da vila pode atacar. */
    public static boolean isNeverTarget(Entity e) {
        return isFriend(e) || e instanceof Creeper;
    }

    @Mod.EventBusSubscriber(modid = MOD_ID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onWarriorHurt(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof FighterVillager)) return;
            if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
            var victim = event.getEntity();
            if (victim.level().isClientSide || isNeverTarget(attacker)) return;
            // Bater num guerreiro comum não provoca ninguém; só atacar o Chefe provoca o próprio Chefe
            if (attacker instanceof Player && !(victim instanceof ChiefVillager)) return;
            // ChiefVillager estende FighterVillager, então este loop cobre os dois tipos
            for (var ally : victim.level().getEntitiesOfClass(FighterVillager.class, victim.getBoundingBox().inflate(24))) {
                if (ally != victim && ally.getTarget() == null) ally.setTarget(attacker);
            }
        }

        /** Impede fogo amigável e ataques de guerreiros/golems contra jogadores e o filhote. */
        @SubscribeEvent
        public static void onAttack(LivingAttackEvent event) {
            Entity source = event.getSource().getEntity();
            if (source == null) return;
            Entity victim = event.getEntity();
            if (isFriend(source) && isFriend(victim)) {
                event.setCanceled(true);
            } else if (isPeaceful(source) && (victim instanceof Player || victim instanceof ChiefCub)) {
                event.setCanceled(true);
            }
        }

        /** Guerreiros comuns e golems de ferro nunca escolhem o jogador ou o filhote como alvo. */
        @SubscribeEvent
        public static void onChangeTarget(LivingChangeTargetEvent event) {
            LivingEntity target = event.getNewTarget();
            if (target == null) return;
            if (isPeaceful(event.getEntity()) && (target instanceof Player || target instanceof ChiefCub || isFriend(target))) {
                event.setCanceled(true);
            }
        }

        /** Vilas ganham um Chefe e guerreiros na primeira vez que um jogador chega perto. */
        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
            if (player.tickCount % 100 != 0) return;
            VillageSeeder.trySeed(player.serverLevel(), player);
        }
    }
}
