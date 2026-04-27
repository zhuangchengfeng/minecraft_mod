package com.example.examplemod.client;

import com.example.examplemod.Config;
import com.example.examplemod.RAVE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

@EventBusSubscriber(modid = RAVE.MODID, value = Dist.CLIENT)
public class SpeedDisplayOverlay {


    private static void getGridCenter(int screenWidth, int screenHeight, int located, int[] out) {
        int gridWidth = screenWidth / 3;
        int gridHeight = screenHeight / 3;
        int col = (located - 1) % 3;
        int row = (located - 1) / 3;
        int centerX = col * gridWidth + gridWidth / 2;
        int centerY = row * gridHeight + gridHeight / 2;
        out[0] = centerX;
        out[1] = centerY;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        // 尝试加载配置（如果尚未加载）

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        double attackSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
        double movementSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        String attackText = String.format("攻速: %.2f", attackSpeed);
        String speedText = String.format("移速: %.2f", movementSpeed);

        var font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int[] center = new int[2];
        int located = Config.LOCATED.get();
        getGridCenter(screenWidth, screenHeight, located, center);

        int lineHeight = font.lineHeight + 2;
        int totalHeight = lineHeight * 2;
        int blockWidth = Math.max(font.width(attackText), font.width(speedText));
        int startX = center[0] - blockWidth / 2;
        int startY = center[1] - totalHeight / 2;

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        graphics.text(font, attackText, startX, startY, 0xFFFFFFFF);
        graphics.text(font, speedText, startX, startY + lineHeight, 0xFFFFFFFF);
    }
}