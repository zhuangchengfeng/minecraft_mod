package com.example.examplemod.init;

import com.example.examplemod.RAVE;
import com.example.examplemod.block.entity.ItemReplicatorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Supplier;

public class ModBlockEntities {
    public static DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RAVE.MODID);

    // ======================= 物品资源复制机 =======================
    public static final Supplier<BlockEntityType<@NotNull ItemReplicatorBlockEntity>> ITEM_REPLICATOR =
            BLOCK_ENTITY_TYPES.register("item_replicator", () ->
                    new BlockEntityType<>(ItemReplicatorBlockEntity::new,
                            ModBlocks.ITEM_REPLICATOR_Tier1.get(),
                            ModBlocks.ITEM_REPLICATOR_Tier2.get(),
                            ModBlocks.ITEM_REPLICATOR_Tier3.get(),
                            ModBlocks.ITEM_REPLICATOR_Tier4.get(),
                            ModBlocks.ITEM_REPLICATOR_Tier5.get()));

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}