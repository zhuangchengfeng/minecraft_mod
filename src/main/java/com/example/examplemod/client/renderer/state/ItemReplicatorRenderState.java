package com.example.examplemod.client.renderer.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.item.ItemStack;

public class ItemReplicatorRenderState extends BlockEntityRenderState {
    public ItemStack displayedItem = ItemStack.EMPTY;  // 要显示的物品（输入槽的物品）
    public boolean hasItem = false; // 是否有物品的标志
}