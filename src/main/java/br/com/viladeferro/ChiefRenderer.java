package br.com.viladeferro;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;

/** Renderiza o Chefe (e o filhote) com o modelo e a textura do Mercador Andarilho. */
public class ChiefRenderer<T extends Mob> extends MobRenderer<T, VillagerModel<T>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/wandering_trader.png");

    public ChiefRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.WANDERING_TRADER)), 0.5F);
        addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(T entity, PoseStack stack, float partialTick) {
        float scale = 0.9375F;
        if (entity instanceof ChiefVillager) {
            scale *= 1.2F;
        } else if (entity instanceof AgeableMob ageable && ageable.isBaby()) {
            scale *= 0.5F;
        }
        // Sentado: afunda o corpo para parecer agachado
        if (entity instanceof TamableAnimal pet && pet.isInSittingPose()) {
            stack.translate(0.0F, 0.45F, 0.0F);
        }
        stack.scale(scale, scale, scale);
    }
}
