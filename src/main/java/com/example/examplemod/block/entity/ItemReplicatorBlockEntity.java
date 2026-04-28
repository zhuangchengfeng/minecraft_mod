package com.example.examplemod.block.entity;

import com.example.examplemod.block.enumTier.ItemReplicatorTier;
import com.example.examplemod.config.ServerConfig;
import com.example.examplemod.init.ModBlockEntities;
import com.example.examplemod.network.ItemReplicatorSyncPacket;
import com.example.examplemod.util.ReplicatorFilter;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ItemReplicatorBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemReplicatorBlockEntity.class);

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT_START = 1;
    private static final int MAX_OUTPUT_SLOTS = 9;
    private static final int TOTAL_SLOTS = 1 + MAX_OUTPUT_SLOTS + 1; // +1 for virtual slot
    private static final int VIRTUAL_SLOT = 10;

    public final ItemStack[] items = new ItemStack[TOTAL_SLOTS];

    private int tickCounter = 0;
    private ItemReplicatorTier tier = ItemReplicatorTier.ITEM_TIER_1;
    private int energyStored;
    private int energyCapacity;
    private int energyConsumption;
    private int currentOutputSlots;
    private int productionRatePerTick;       // 每 tick 虚拟产量

    private boolean virtualSlotActive = false;
    private ItemResource virtualSlotResource = ItemResource.EMPTY;
    private long virtualSlotAmount = Long.MAX_VALUE;
    private int virtualSlotAccumulator = 0;

    // 初始化块
    {
        updateEnergyStats();
        energyStored = 0;
        updateOutputSlots();
        updateProductionRate();
        Arrays.fill(items, ItemStack.EMPTY);
        items[VIRTUAL_SLOT] = ItemStack.EMPTY;
    }

    private void updateEnergyStats() {
        switch (tier) {
            case ITEM_TIER_1 -> {
                energyCapacity = ServerConfig.getItemTier1EnergyCapacity();
                energyConsumption = ServerConfig.getItemTier1EnergyConsumption();
            }
            case ITEM_TIER_2 -> {
                energyCapacity = ServerConfig.getItemTier2EnergyCapacity();
                energyConsumption = ServerConfig.getItemTier2EnergyConsumption();
            }
            case ITEM_TIER_3 -> {
                energyCapacity = ServerConfig.getItemTier3EnergyCapacity();
                energyConsumption = ServerConfig.getItemTier3EnergyConsumption();
            }
            case ITEM_TIER_4 -> {
                energyCapacity = ServerConfig.getItemTier4EnergyCapacity();
                energyConsumption = ServerConfig.getItemTier4EnergyConsumption();
            }
            case ITEM_TIER_5 -> {
                energyCapacity = ServerConfig.getItemTier5EnergyCapacity();
                energyConsumption = ServerConfig.getItemTier5EnergyConsumption();
            }
        }
    }

    private void updateOutputSlots() {
        this.currentOutputSlots = switch (tier) {
            case ITEM_TIER_1 -> ServerConfig.getItemTier1OutputSlots();
            case ITEM_TIER_2 -> ServerConfig.getItemTier2OutputSlots();
            case ITEM_TIER_3 -> ServerConfig.getItemTier3OutputSlots();
            case ITEM_TIER_4 -> ServerConfig.getItemTier4OutputSlots();
            case ITEM_TIER_5 -> ServerConfig.getItemTier5OutputSlots();
        };
    }

    private void updateProductionRate() {
        int outputAmount = tier.getOutputAmount();
        int processSpeed = tier.getProcessSpeed();
        this.productionRatePerTick = (int) Math.floor((double) outputAmount / processSpeed);
        if (this.productionRatePerTick < 1) {
            this.productionRatePerTick = 1;
        }
    }

    // ======================= 物品处理器（新版 ResourceHandler） =======================
    private final ResourceHandler<ItemResource> itemHandler = new ResourceHandler<>() {
        @Override
        public int size() {
            return TOTAL_SLOTS;
        }

        @Override
        public ItemResource getResource(int index) {
            if (index == VIRTUAL_SLOT && virtualSlotActive) {
                return virtualSlotResource;
            }
            if (index >= 0 && index < TOTAL_SLOTS) {
                ItemStack stack = items[index];
                return stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack);
            }
            return ItemResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            if (index == VIRTUAL_SLOT && virtualSlotActive) {
                return virtualSlotAmount;
            }
            if (index >= 0 && index < TOTAL_SLOTS) {
                ItemStack stack = items[index];
                return stack.isEmpty() ? 0L : stack.getCount();
            }
            return 0L;
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            if (index == VIRTUAL_SLOT) {
                return Long.MAX_VALUE;
            }
            return 64L;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            if (index == INPUT_SLOT) {
                ItemStack stack = resource.toStack(1);
                return ReplicatorFilter.canInsertItem(stack);
            }
            return true;
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index == VIRTUAL_SLOT) return 0;
            if (index == INPUT_SLOT) {
                if (resource.isEmpty()) return 0;
                boolean isPipe = !isPlayerInsertion();
                if (isPipe) {
                    if (ServerConfig.isItemReplicatorDestroyEnabled()) {
                        return amount;   // 销毁模式
                    } else {
                        return 0;
                    }
                }
                ItemStack stack = resource.toStack(1);
                ItemStack current = items[index];
                if (current.isEmpty()) {
                    int toInsert = Math.min(amount, stack.getMaxStackSize());
                    items[index] = stack.copyWithCount(toInsert);
                    markUpdated();
                    return toInsert;
                } else if (ItemStack.isSameItemSameComponents(current, stack)) {
                    int canAdd = Math.min(stack.getMaxStackSize() - current.getCount(), amount);
                    if (canAdd > 0) {
                        items[index] = current.copyWithCount(current.getCount() + canAdd);
                        markUpdated();
                    }
                    return canAdd;
                }
                return 0;
            }
            return 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index == VIRTUAL_SLOT && virtualSlotActive) {
                if (!resource.equals(virtualSlotResource)) return 0;
                int maxExtract = tier.getOutputAmount();
                int toExtract = Math.min(amount, maxExtract);
                if (toExtract > 0) {
                    // 虚拟槽位无限，直接返回
                    markUpdated();
                    return toExtract;
                }
                return 0;
            }
            if (index == INPUT_SLOT) return 0;
            if (index >= OUTPUT_SLOT_START && index < OUTPUT_SLOT_START + MAX_OUTPUT_SLOTS) {
                ItemStack current = items[index];
                if (current.isEmpty()) return 0;
                if (!ItemStack.isSameItemSameComponents(current, resource.toStack(1))) return 0;
                int toExtract = Math.min(amount, current.getCount());
                if (toExtract <= 0) return 0;
                ItemStack remaining = current.copyWithCount(current.getCount() - toExtract);
                items[index] = remaining.isEmpty() ? ItemStack.EMPTY : remaining;
                markUpdated();
                return toExtract;
            }
            return 0;
        }
    };

    private boolean isPlayerInsertion() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement e : stackTrace) {
            if (e.getClassName().contains("ItemReplicatorBlock") && e.getMethodName().contains("useWithoutItem"))
                return true;
        }
        return false;
    }

    // ======================= 能量处理器 =======================
    private final EnergyHandler energyHandler = new EnergyHandler() {
        @Override
        public long getAmountAsLong() {
            return energyStored;
        }

        @Override
        public long getCapacityAsLong() {
            return energyCapacity;
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            int canReceive = Math.min(amount, energyCapacity - energyStored);
            if (canReceive <= 0) return 0;
            energyStored += canReceive;
            markUpdated();
            return canReceive;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            int canExtract = Math.min(amount, energyStored);
            if (canExtract <= 0) return 0;
            energyStored -= canExtract;
            markUpdated();
            return canExtract;
        }
    };

    public ItemReplicatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_REPLICATOR.get(), pos, state);
    }

    // ======================= NBT 持久化（新版 ValueOutput/ValueInput） =======================
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag customData = new CompoundTag();
        customData.putInt("tickCounter", tickCounter);
        customData.putInt("tier", tier.getId());
        customData.putInt("energyStored", energyStored);
        customData.putBoolean("virtualSlotActive", virtualSlotActive);
        if (virtualSlotActive && !virtualSlotResource.isEmpty()) {
            ItemStack virtualStack = virtualSlotResource.toStack(1);
            ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, virtualStack)
                    .result()
                    .ifPresent(tag -> customData.put("virtualSlotResourceStack", tag));
        }
        CompoundTag itemsTag = new CompoundTag();
        for (int i = 0; i < items.length; i++) {
            if (!items[i].isEmpty()) {
                int fi = i;
                ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, items[i])
                        .result()
                        .ifPresent(tag -> itemsTag.put("item" + fi, tag));
            }
        }
        customData.put("items", itemsTag);
        output.store("custom_data", CompoundTag.CODEC, customData);
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);

        CompoundTag customData = input.read("custom_data", CompoundTag.CODEC).orElse(new CompoundTag());
        virtualSlotActive = customData.getBoolean("virtualSlotActive").orElse(false);

        if (virtualSlotActive && customData.contains("virtualSlotResourceStack")) {
            customData.getCompound("virtualSlotResourceStack")
                    .flatMap(tag -> ItemStack.CODEC.parse(NbtOps.INSTANCE, tag).result())
                    .ifPresent(stack -> {
                        if (!stack.isEmpty()) {
                            virtualSlotResource = ItemResource.of(stack);
                            virtualSlotAmount = Long.MAX_VALUE;
                        }
                    });
        }

        tickCounter = customData.getInt("tickCounter").orElse(0);
        if (customData.contains("tier")) {
            this.tier = ItemReplicatorTier.fromId(customData.getInt("tier").orElse(0));
        }
        if (customData.contains("energyStored")) {
            this.energyStored = customData.getInt("energyStored").orElse(0);
        }

        updateEnergyStats();
        updateOutputSlots();

        CompoundTag itemsTag = customData.getCompound("items").orElse(new CompoundTag());

        for (int i = 0; i < items.length; i++) {
            String key = "item" + i;
            int finalI = i;

            itemsTag.getCompound(key).ifPresent(itemTag ->
                    ItemStack.CODEC.parse(NbtOps.INSTANCE, itemTag)
                            .result().ifPresent(stack -> items[finalI] = stack)
            );
        }

        setChanged();
    }

    @Override
    public void handleUpdateTag(@NotNull ValueInput input) {
        CompoundTag customData = input.read("custom_data", CompoundTag.CODEC).orElse(new CompoundTag());
        virtualSlotActive = customData.getBoolean("virtualSlotActive").orElse(false);

        if (virtualSlotActive && customData.contains("virtualSlotResourceStack")) {
            customData.getCompound("virtualSlotResourceStack")
                    .flatMap(tag -> ItemStack.CODEC.parse(NbtOps.INSTANCE, tag).result())
                    .ifPresent(stack -> {
                        if (!stack.isEmpty()) {
                            virtualSlotResource = ItemResource.of(stack);
                            virtualSlotAmount = Long.MAX_VALUE;
                        }
                    });
        }

        tickCounter = customData.getInt("tickCounter").orElse(0);
        if (customData.contains("tier")) {
            this.tier = ItemReplicatorTier.fromId(customData.getInt("tier").orElse(0));
        }
        if (customData.contains("energyStored")) {
            this.energyStored = customData.getInt("energyStored").orElse(0);
        }

        updateEnergyStats();
        updateOutputSlots();

        CompoundTag itemsTag = customData.getCompound("items").orElse(new CompoundTag());

        for (int i = 0; i < items.length; i++) {
            String key = "item" + i;
            int finalI = i;

            itemsTag.getCompound(key).ifPresent(itemTag ->
                    ItemStack.CODEC.parse(NbtOps.INSTANCE, itemTag)
                            .result().ifPresent(stack -> items[finalI] = stack)
            );
        }
    }

    @NotNull
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("tickCounter", tickCounter);
        tag.putInt("tier", tier.getId());
        tag.putInt("energyStored", energyStored);
        tag.putBoolean("virtualSlotActive", virtualSlotActive);
        if (virtualSlotActive && !virtualSlotResource.isEmpty()) {
            ItemStack virtualStack = virtualSlotResource.toStack(1);
            ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, virtualStack)
                    .result()
                    .ifPresent(data -> tag.put("virtualSlotResourceStack", data));
        }
        CompoundTag itemsTag = new CompoundTag();
        for (int i = 0; i < items.length; i++) {
            if (!items[i].isEmpty()) {
                int fi = i;
                ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, items[i])
                        .result()
                        .ifPresent(data -> itemsTag.put("item" + fi, data));
            }
        }
        tag.put("items", itemsTag);
        return tag;
    }

    public void handleUpdateTagFromPacket(CompoundTag tag) {
        tickCounter = tag.getInt("tickCounter").orElse(0);
        if (tag.contains("tier")) {
            this.tier = ItemReplicatorTier.fromId(tag.getInt("tier").orElse(0));
        }
        if (tag.contains("energyStored")) {
            this.energyStored = tag.getInt("energyStored").orElse(0);
        }

        virtualSlotActive = tag.getBoolean("virtualSlotActive").orElse(false);
        if (virtualSlotActive && tag.contains("virtualSlotResourceStack")) {
            tag.getCompound("virtualSlotResourceStack")
                    .flatMap(tagData -> ItemStack.CODEC.parse(NbtOps.INSTANCE, tagData).result())
                    .ifPresent(stack -> {
                        if (!stack.isEmpty()) {
                            virtualSlotResource = ItemResource.of(stack);
                            virtualSlotAmount = Long.MAX_VALUE;
                        }
                    });
        } else if (!virtualSlotActive) {
            virtualSlotResource = ItemResource.EMPTY;
            virtualSlotAmount = Long.MAX_VALUE;
            virtualSlotAccumulator = 0;
        }

        updateEnergyStats();
        updateOutputSlots();

        CompoundTag itemsTag = tag.getCompound("items").orElse(new CompoundTag());
        for (int i = 0; i < items.length; i++) {
            String key = "item" + i;
            int finalI = i;

            if (itemsTag.contains(key)) {
                var itemTagOpt = itemsTag.getCompound(key);
                itemTagOpt.flatMap(itemTag -> ItemStack.CODEC.parse(NbtOps.INSTANCE, itemTag)
                        .result()).ifPresent(stack -> items[finalI] = stack);
            } else {
                items[finalI] = ItemStack.EMPTY;
            }
        }
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            CompoundTag tag = getUpdateTag(level.registryAccess());
            var packet = new ItemReplicatorSyncPacket(getBlockPos(), tag);
            if (level instanceof ServerLevel serverLevel) {
                ChunkPos chunkPos = new ChunkPos(getBlockPos().getX() >> 4, getBlockPos().getZ() >> 4);
                serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false)
                        .forEach(player -> PacketDistributor.sendToPlayer(player, packet));
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    public void setTier(int tierId) {
        tier = ItemReplicatorTier.fromId(tierId);
        updateEnergyStats();
        updateOutputSlots();
        updateProductionRate();
    }

    public int getCurrentOutputSlots() {
        return currentOutputSlots;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemReplicatorBlockEntity be) {
        boolean hasItemChanged = false;
        be.tickCounter++;

        ItemStack inputStack = be.items[INPUT_SLOT];

        // 检查是否有真实输出槽里的物品
        boolean hasRealItems = false;
        for (int i = OUTPUT_SLOT_START; i < OUTPUT_SLOT_START + be.currentOutputSlots; i++) {
            if (!be.items[i].isEmpty()) {
                hasRealItems = true;
                break;
            }
        }

        // 如果有真实物品，禁用虚拟槽位
        if (hasRealItems) {
            if (be.virtualSlotActive) {
                be.virtualSlotActive = false;
                be.virtualSlotResource = ItemResource.EMPTY;
                be.virtualSlotAmount = Long.MAX_VALUE;
                be.virtualSlotAccumulator = 0;
                hasItemChanged = true;
            }
        } else {
            // 没有真实物品，处理虚拟生产
            if (!inputStack.isEmpty()) {
                int itemsPerProduction = be.tier.getOutputAmount();
                int ticksPerProduction = be.tier.getProcessSpeed();

                be.virtualSlotAccumulator += itemsPerProduction;

                while (be.virtualSlotAccumulator >= ticksPerProduction) {
                    be.virtualSlotAccumulator -= ticksPerProduction;

                    if (!be.virtualSlotActive) {
                        be.virtualSlotActive = true;
                        be.virtualSlotResource = ItemResource.of(inputStack);
                        be.virtualSlotAmount = Long.MAX_VALUE;
                        hasItemChanged = true;
                    }
                    be.virtualSlotAmount = Long.MAX_VALUE;
                }
            } else {
                if (be.virtualSlotActive) {
                    be.virtualSlotActive = false;
                    be.virtualSlotResource = ItemResource.EMPTY;
                    be.virtualSlotAmount = Long.MAX_VALUE;
                    be.virtualSlotAccumulator = 0;
                    hasItemChanged = true;
                }
            }
        }

        // 未达到生产周期，只更新状态
        if (be.tickCounter < be.tier.getProcessSpeed()) {
            if (hasItemChanged) be.markUpdated();
            return;
        }

        be.tickCounter = 0;

        if (inputStack.isEmpty()) return;

        int requiredEnergy = be.energyConsumption;
        if (be.energyStored < requiredEnergy) return;

        int totalOutput = 0;
        boolean hasProduced = false;

        // 自动输出
        if (ServerConfig.isItemReplicatorAutoOutputEnabled()) {
            Direction outDir = ServerConfig.getItemReplicatorAutoOutputDirection();
            BlockPos neighbor = pos.relative(outDir);
            ResourceHandler<ItemResource> handler = level.getCapability(
                    Capabilities.Item.BLOCK, neighbor, outDir.getOpposite());
            if (handler != null) {
                ItemResource resource = ItemResource.of(inputStack);
                int toInsert = be.tier.getOutputAmount();
                try (Transaction tx = Transaction.openRoot()) {
                    long inserted = handler.insert(resource, toInsert, tx);
                    if (inserted > 0) {
                        totalOutput += (int) inserted;
                        hasProduced = true;
                        tx.commit();
                    }
                }
            }
        }

        // 如果自动输出没有消耗完所有产量（或者没启用自动输出），通过物品处理器插入输出槽
        int remaining = be.tier.getOutputAmount() - totalOutput;
        if (remaining > 0) {
            try (Transaction tx = Transaction.openRoot()) {
                for (int i = OUTPUT_SLOT_START; i < OUTPUT_SLOT_START + be.currentOutputSlots && remaining > 0; i++) {
                    ItemResource resource = ItemResource.of(inputStack);
                    int inserted = be.itemHandler.insert(i, resource, remaining, tx);
                    if (inserted > 0) {
                        totalOutput += inserted;
                        remaining -= inserted;
                        hasProduced = true;
                    }
                }
                if (hasProduced) tx.commit();
            }
        }

        if (hasProduced && totalOutput > 0) {
            int actualEnergy = totalOutput * be.energyConsumption;
            if (be.energyStored >= actualEnergy) {
                be.energyStored -= actualEnergy;
                be.markUpdated();
            }
        }
        be.markUpdated();
    }

    public boolean addItem(ItemStack stack) {
        if (!ReplicatorFilter.canInsertItem(stack)) return false;
        if (items[INPUT_SLOT].isEmpty()) {
            items[INPUT_SLOT] = stack.copyWithCount(1);
            markUpdated();
            return true;
        } else if (ItemStack.isSameItemSameComponents(items[INPUT_SLOT], stack)) {
            int space = items[INPUT_SLOT].getMaxStackSize() - items[INPUT_SLOT].getCount();
            if (space > 0) {
                items[INPUT_SLOT].grow(1);
                markUpdated();
                return true;
            }
        }
        return false;
    }

    public ItemStack extractItem() {
        if (!items[INPUT_SLOT].isEmpty()) {
            // 一次性取出全部（原版作者是这样）
            ItemStack extracted = items[INPUT_SLOT].copy();
            items[INPUT_SLOT] = ItemStack.EMPTY;
            markUpdated();
            return extracted;
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getDisplayedItem() {
        return items[INPUT_SLOT];
    }

    @Nullable
    public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        return itemHandler;
    }

    @Nullable
    public EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return energyHandler;
    }
}