package com.example.examplemod.util;

import com.example.examplemod.config.ServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;  // 改为 Identifier
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ReplicatorFilter {
    // 固定的管理员物品（无法被复制）
    private static final String[] ADMIN_ITEMS = {
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:command_block_minecart",
            "minecraft:jigsaw",
            "minecraft:structure_block",
            "minecraft:structure_void",
            "minecraft:barrier",
            "minecraft:debug_stick",
            "minecraft:light",
            "minecraft:bedrock",
            "rave:*"
    };

    public static boolean canInsertItem(ItemStack stack) {
        return canInsertItemWithReason(stack).canInsert();
    }

    public static FilterResult canInsertItemWithReason(ItemStack stack) {
        if (stack.isEmpty()) {
            return new FilterResult(false, Component.translatable("message.item_replicator.filter.denied"));
        }

        String itemId = getItemId(stack);
        String modId = itemId.split(":")[0];

        for (String adminItem : ADMIN_ITEMS) {
            if (adminItem.endsWith(":*")) {
                String targetModId = adminItem.substring(0, adminItem.length() - 2);
                if (modId.equals(targetModId)) {
                    return new FilterResult(false, Component.translatable("message.item_replicator.filter.denied"));
                }
            } else if (itemId.equals(adminItem)) {
                return new FilterResult(false, Component.translatable("message.item_replicator.filter.denied"));
            }
        }

        boolean isBlacklistMode = ServerConfig.isBlacklistMode();

        if (isBlacklistMode) {
            if (isInList(itemId, modId, stack, ServerConfig.getBlacklistItems())) {
                return new FilterResult(false, Component.translatable("message.item_replicator.filter.denied"));
            }
        } else {
            if (!isInList(itemId, modId, stack, ServerConfig.getWhitelistItems())) {
                return new FilterResult(false, Component.translatable("message.item_replicator.filter.denied"));
            }
        }

        return new FilterResult(true, Component.empty());
    }

    private static boolean isInList(String itemId, String modId, ItemStack stack, List<String> list) {
        for (String entry : list) {
            if (entry.equals(itemId)) {
                return true;
            }
            if (entry.startsWith("@") && modId.equals(entry.substring(1))) {
                return true;
            }
            if (entry.startsWith("#")) {
                try {
                    // 使用 Identifier.parse() 替代 new ResourceLocation()
                    Identifier tagLocation = Identifier.parse(entry.substring(1));
                    TagKey<Item> tag = TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagLocation);
                    if (stack.is(tag)) {
                        return true;
                    }
                } catch (Exception e) {
                    // Tag 解析失败，跳过
                }
            }
        }
        return false;
    }

    private static String getItemId(ItemStack stack) {
        // BuiltInRegistries.ITEM.getKey() 现在返回 Identifier，直接调用 toString()
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static class FilterResult {
        private final boolean canInsert;
        private final Component reason;

        public FilterResult(boolean canInsert, Component reason) {
            this.canInsert = canInsert;
            this.reason = reason;
        }

        public boolean canInsert() {
            return canInsert;
        }

        public Component getReason() {
            return reason;
        }
    }
}