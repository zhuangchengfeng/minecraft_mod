package com.example.examplemod.client;

import com.example.examplemod.RAVE;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

@EventBusSubscriber(modid = RAVE.MODID, value = Dist.CLIENT)
public class SpeedDisplayOverlay {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        // 获取属性值
        double attackSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
        double movementSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        String attackText = String.format("⚔️ 攻速: %.2f", attackSpeed);
        String speedText = String.format("👟 移速: %.2f", movementSpeed);

        // 计算右上角位置
        var font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int textX = screenWidth - font.width(attackText) - 10;
        int textY = 10;
        int lineHeight = font.lineHeight + 2;

        // 获取 GuiGraphicsExtractor 实例
        var graphics = event.getGuiGraphics();
        // 绘制文字
        graphics.text(font, attackText, textX, textY, 0xFFFFFFFF);
        graphics.text(font, speedText, textX, textY + lineHeight, 0xFFFFFFFF);
    }
}