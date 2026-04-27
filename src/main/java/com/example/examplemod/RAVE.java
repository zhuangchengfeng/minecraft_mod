package com.example.examplemod;

import com.example.examplemod.init.ModRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

import java.util.UUID;

@Mod(RAVE.MODID)
public class RAVE {
    public static final String MODID = "rave";
    public static final Logger LOGGER = LogUtils.getLogger();
    public RAVE(IEventBus modEventBus) {
        ModRegistry.ENCHANTMENTS.register(modEventBus);
        ModRegistry.EFFECTS.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
    }

    private static final Identifier TAP_DANCE_MODIFIER_ID = Identifier.fromNamespaceAndPath("rave", "tap_dance_modifier");
    private static final Identifier SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath("rave", "tap_dance_speed");
    private static final Identifier ATTACK_SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath("rave", "tap_dance_attack"); // 攻速可选
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        ItemStack weapon = player.getMainHandItem();
        int enchantLevel = weapon.getEnchantmentLevel(ModRegistry.TAP_DANCE);
        if (enchantLevel <= 0) return;

        MobEffectInstance current = player.getEffect(ModRegistry.TAP_DANCE_EFFECT);
        int newAmplifier = (current == null ? 0 : current.getAmplifier() + 1);
        // 不再限制最大层数，实现无限叠加
        player.addEffect(new MobEffectInstance(ModRegistry.TAP_DANCE_EFFECT, 100, newAmplifier));
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        MobEffectInstance effect = player.getEffect(ModRegistry.TAP_DANCE_EFFECT);
        AttributeInstance movementSpeedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance attackSpeedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (movementSpeedAttr == null || attackSpeedAttr == null) return;

        // 先移除旧修饰符（移速和攻速）
        movementSpeedAttr.removeModifier(SPEED_MODIFIER_ID);
        attackSpeedAttr.removeModifier(ATTACK_SPEED_MODIFIER_ID);

        if (effect != null) {
            int amplifier = effect.getAmplifier();
            ItemStack weapon = player.getMainHandItem();
            int enchantLevel = weapon.getEnchantmentLevel(ModRegistry.TAP_DANCE);
            if (enchantLevel > 0) {
                // 计算移速加成：每层基础增量X * 附魔等级，总增量 = (amplifier+1) * (0.02 * enchantLevel)
                double baseIncrement = 0.002 * enchantLevel;
                double speedBoost = (amplifier + 1) * baseIncrement;
                // 添加移速修饰符
                movementSpeedAttr.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID, speedBoost, AttributeModifier.Operation.ADD_VALUE));

                // 获取当前移速（已包含刚添加的修饰符）
                double currentMovementSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
                // 攻速加成 = 当前移速 * 0.3
                double attackBoost = currentMovementSpeed * 0.3;
                // 添加攻速修饰符
                attackSpeedAttr.addTransientModifier(new AttributeModifier(ATTACK_SPEED_MODIFIER_ID, attackBoost, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }
}