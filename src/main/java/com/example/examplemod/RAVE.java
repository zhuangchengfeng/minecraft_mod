package com.example.examplemod;

import com.example.examplemod.block.entity.ItemReplicatorBlockEntity;
import com.example.examplemod.config.ServerConfig;
import com.example.examplemod.init.ModBlocks;
import com.example.examplemod.init.ModBlockEntities;
import com.example.examplemod.init.ModCreativeTabs;
import com.example.examplemod.init.ModItems;
import com.example.examplemod.init.ModRegistry;
import com.example.examplemod.network.ItemReplicatorSyncPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(RAVE.MODID)
public class RAVE {
    public static final String MODID = "rave";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    // 你的自定义物品
    public static final DeferredItem<Item> RUBY = ITEMS.registerSimpleItem("ruby", props -> props.stacksTo(64));

    public RAVE(IEventBus modEventBus, ModContainer modContainer) {
        // 物品复制机注册
        ModCreativeTabs.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.ITEMS_REGISTER.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerPayloadHandlers);  // <-- 关键：注册网络包
        modContainer.registerConfig(ModConfig.Type.COMMON, ServerConfig.CONFIG_SPEC);

        // 你的原有功能
        ModRegistry.ENCHANTMENTS.register(modEventBus);
        ModRegistry.EFFECTS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ITEMS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK,
                ModBlockEntities.ITEM_REPLICATOR.get(),
                ItemReplicatorBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.Energy.BLOCK,
                ModBlockEntities.ITEM_REPLICATOR.get(),
                ItemReplicatorBlockEntity::getEnergyHandler);
    }

    // 注册网络包处理方法
    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                ItemReplicatorSyncPacket.TYPE,
                ItemReplicatorSyncPacket.CODEC,
                ItemReplicatorSyncPacket::handle
        );
    }

    // 以下是你原有的踢踏舞事件（保持不变）
    private static final Identifier SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath(MODID, "tap_dance_speed");
    private static final Identifier ATTACK_SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath(MODID, "tap_dance_attack");

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (event.getNewDamage() <= 0.5F) return;

        ItemStack weapon = player.getMainHandItem();
        int enchantLevel = weapon.getEnchantmentLevel(ModRegistry.TAP_DANCE);
        if (enchantLevel <= 0) return;

        MobEffectInstance current = player.getEffect(ModRegistry.TAP_DANCE_EFFECT);
        int newAmplifier = (current == null ? 0 : current.getAmplifier() + 1);
        newAmplifier = Math.min(newAmplifier, 300);
        int duration = 50 * (enchantLevel + 1);
        player.addEffect(new MobEffectInstance(ModRegistry.TAP_DANCE_EFFECT, duration, newAmplifier));
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        MobEffectInstance effect = player.getEffect(ModRegistry.TAP_DANCE_EFFECT);
        AttributeInstance movementSpeedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance attackSpeedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (movementSpeedAttr == null || attackSpeedAttr == null) return;

        movementSpeedAttr.removeModifier(SPEED_MODIFIER_ID);
        attackSpeedAttr.removeModifier(ATTACK_SPEED_MODIFIER_ID);

        if (effect != null) {
            int amplifier = effect.getAmplifier();
            ItemStack weapon = player.getMainHandItem();
            int enchantLevel = weapon.getEnchantmentLevel(ModRegistry.TAP_DANCE);
            if (enchantLevel > 0) {
                double baseIncrement = 0.002 * enchantLevel;
                double speedBoost = (amplifier + 1) * baseIncrement;
                movementSpeedAttr.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID, speedBoost, AttributeModifier.Operation.ADD_VALUE));

                double currentMovementSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
                double attackBoost = currentMovementSpeed * 0.3;
                attackSpeedAttr.addTransientModifier(new AttributeModifier(ATTACK_SPEED_MODIFIER_ID, attackBoost, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }
}