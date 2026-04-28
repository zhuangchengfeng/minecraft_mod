package com.example.examplemod.init;

import com.example.examplemod.RAVE;
import com.example.examplemod.block.ItemReplicatorBlock;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCK_REGISTER =
            DeferredRegister.createBlocks(RAVE.MODID);

    // ======================= 物品资源复制机 =======================
    public static final DeferredBlock<ItemReplicatorBlock> ITEM_REPLICATOR_Tier1 =
            registerBlock("item_replicator_tier1",
                    properties -> new ItemReplicatorBlock(properties, 1),
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .sound(SoundType.STONE)
                            .destroyTime(2.5f)
                            .explosionResistance(6.0f)
                            .requiresCorrectToolForDrops(),
                    Rarity.COMMON);
    public static final DeferredBlock<ItemReplicatorBlock> ITEM_REPLICATOR_Tier2 =
            registerBlock("item_replicator_tier2",
                    properties -> new ItemReplicatorBlock(properties, 2),
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .sound(SoundType.STONE)
                            .destroyTime(2.5f)
                            .explosionResistance(6.0f)
                            .requiresCorrectToolForDrops(),
                    Rarity.UNCOMMON);
    public static final DeferredBlock<ItemReplicatorBlock> ITEM_REPLICATOR_Tier3 =
            registerBlock("item_replicator_tier3",
                    properties -> new ItemReplicatorBlock(properties, 3),
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .sound(SoundType.STONE)
                            .destroyTime(2.5f)
                            .explosionResistance(6.0f)
                            .requiresCorrectToolForDrops(),
                    Rarity.RARE);
    public static final DeferredBlock<ItemReplicatorBlock> ITEM_REPLICATOR_Tier4 =
            registerBlock("item_replicator_tier4",
                    properties -> new ItemReplicatorBlock(properties, 4),
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .sound(SoundType.STONE)
                            .destroyTime(2.5f)
                            .explosionResistance(6.0f)
                            .requiresCorrectToolForDrops(),
                    Rarity.EPIC);
    public static final DeferredBlock<ItemReplicatorBlock> ITEM_REPLICATOR_Tier5 =
            registerBlock("item_replicator_tier5",
                    properties -> new ItemReplicatorBlock(properties, 5),
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .sound(SoundType.STONE)
                            .destroyTime(2.5f)
                            .explosionResistance(6.0f)
                            .requiresCorrectToolForDrops(),
                    Rarity.EPIC);

    // 辅助方法：使用 BlockBehaviour.Properties 的 Supplier 来创建方块并注册对应的 BlockItem
    private static <T extends Block> DeferredBlock<T> registerBlock(String name,
                                                                    Function<BlockBehaviour.Properties, T> blockFactory,
                                                                    Supplier<BlockBehaviour.Properties> properties,
                                                                    Rarity rarity) {
        DeferredBlock<T> block = BLOCK_REGISTER.registerBlock(name, blockFactory, properties);
        // 注册对应的 BlockItem，使用 ModItems.ITEMS_REGISTER 的 registerSimpleBlockItem 方法
        ModItems.ITEMS_REGISTER.registerSimpleBlockItem(name, block, p -> p.rarity(rarity));
        return block;
    }

    public static void register(IEventBus eventBus) {
        BLOCK_REGISTER.register(eventBus);
    }
}