package br.com.viladeferro;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;

@Mod(VilaDeFerro.MOD_ID)
public class VilaDeFerro {
    public static final String MOD_ID = "viladeferro";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<EntityType<FighterVillager>> FIGHTER = ENTITIES.register("guerreiro_villager",
            () -> EntityType.Builder.of(FighterVillager::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("guerreiro_villager"));
    public static final RegistryObject<EntityType<ChiefVillager>> CHIEF = ENTITIES.register("chefe_villager",
            () -> EntityType.Builder.of(ChiefVillager::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(12).build("chefe_villager"));
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
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(FIGHTER_EGG);
            event.accept(CHIEF_EGG);
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onWarriorHurt(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof FighterVillager) && !(event.getEntity() instanceof ChiefVillager)) return;
            if (!(event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker)) return;
            var victim = event.getEntity();
            if (victim.level().isClientSide) return;
            // ChiefVillager estende FighterVillager, então este loop cobre os dois tipos
            for (var ally : victim.level().getEntitiesOfClass(FighterVillager.class, victim.getBoundingBox().inflate(24))) {
                if (ally != victim && ally.getTarget() == null && ally != attacker) ally.setTarget(attacker);
            }
        }
    }
}
