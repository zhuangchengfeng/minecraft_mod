package com.example.examplemod.block;

import com.example.examplemod.block.entity.ItemReplicatorBlockEntity;
import com.example.examplemod.util.ReplicatorFilter;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemReplicatorBlock extends BaseEntityBlock {
    private final int tier;

    public static final MapCodec<ItemReplicatorBlock> CODEC = simpleCodec(ItemReplicatorBlock::new);

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public ItemReplicatorBlock(Properties properties) {
        super(properties);
        this.tier = 1;
    }

    public ItemReplicatorBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        ItemReplicatorBlockEntity blockEntity = new ItemReplicatorBlockEntity(pos, state);
        blockEntity.setTier(tier);
        return blockEntity;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (lvl, pos, st, be) -> {
            if (be instanceof ItemReplicatorBlockEntity) {
                ItemReplicatorBlockEntity.serverTick(lvl, pos, st, (ItemReplicatorBlockEntity) be);
            }
        };
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, @NotNull Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ItemReplicatorBlockEntity replicator) {
            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

            // 潜行取出
            if (player.isShiftKeyDown()) {
                ItemStack extractedItem = replicator.extractItem();
                if (!extractedItem.isEmpty()) {
                    if (heldItem.isEmpty()) {
                        player.setItemInHand(InteractionHand.MAIN_HAND, extractedItem);
                    } else {
                        player.drop(extractedItem, false);
                    }
                    player.sendSystemMessage(Component.translatable("message.item_replicator.extracted",
                            extractedItem.getHoverName()).withStyle(style -> style.withColor(ChatFormatting.YELLOW)));
                } else {
                    player.sendSystemMessage(Component.translatable("message.item_replicator.empty"));
                }
                return InteractionResult.CONSUME;
            }

            // 放入物品
            if (!heldItem.isEmpty()) {
                ReplicatorFilter.FilterResult filterResult = ReplicatorFilter.canInsertItemWithReason(heldItem);
                if (!filterResult.canInsert()) {
                    player.sendSystemMessage(filterResult.getReason());
                    return InteractionResult.CONSUME;
                }

                String itemName = heldItem.getHoverName().getString();

                if (replicator.addItem(heldItem)) {
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                    player.sendSystemMessage(Component.translatable("message.item_replicator.inserted",
                            Component.literal(itemName)).withStyle(style -> style.withColor(ChatFormatting.GOLD)));
                    return InteractionResult.CONSUME;
                }
            }

            // 显示当前物品
            if (replicator.getDisplayedItem().isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.item_replicator.empty"));
            } else {
                player.sendSystemMessage(Component.translatable("message.item_replicator.contains",
                        replicator.getDisplayedItem().getHoverName()).withStyle(style -> style.withColor(ChatFormatting.AQUA)));
            }
        }

        return InteractionResult.SUCCESS;
    }
}