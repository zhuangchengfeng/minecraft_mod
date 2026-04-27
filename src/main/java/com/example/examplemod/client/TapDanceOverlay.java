package com.example.examplemod.client;

import com.example.examplemod.RAVE;
import com.example.examplemod.init.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

@EventBusSubscriber(modid = RAVE.MODID, value = Dist.CLIENT)
public class TapDanceOverlay {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        MobEffectInstance effect = player.getEffect(ModRegistry.TAP_DANCE_EFFECT);
        if (effect == null) return;

        int layer = effect.getAmplifier() + 1;
        String text = Component.translatable("tapdance.layer", layer).getString();
        var font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int textX = screenWidth - font.width(text) - 10;  // 右上角
        int textY = 40;  // 放在攻速移速下方（避免重叠）

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        graphics.text(font, text, textX, textY, 0xFFFFFFFF);

    }
}