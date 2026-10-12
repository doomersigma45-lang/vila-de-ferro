package br.com.viladeferro;

import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VilaDeFerro.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VilaDeFerro.FIGHTER.get(), VillagerRenderer::new);
        event.registerEntityRenderer(VilaDeFerro.CHIEF.get(), ChiefRenderer::new);
        event.registerEntityRenderer(VilaDeFerro.CHIEF_CUB.get(), ChiefRenderer::new);
    }
}
