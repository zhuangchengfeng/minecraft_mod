package com.example.examplemod.init;

import com.example.examplemod.RAVE;
import com.example.examplemod.effect.TapDanceEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRegistry {
    // 附魔注册器 (数据驱动，Supplier 返回 null)
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(Registries.ENCHANTMENT, RAVE.MODID);
    // 效果注册器
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, RAVE.MODID);

    // 当您使用 () -> null 时，务必将 DeferredHolder 的类型参数指定为 Enchantment。
    public static final DeferredHolder<Enchantment, Enchantment> TAP_DANCE =
            ENCHANTMENTS.register("tap_dance", () -> null);

    // 踢踏舞效果的引用（Holder 可以直接使用）
    public static final Holder<MobEffect> TAP_DANCE_EFFECT =
            EFFECTS.register("tap_dance_effect", TapDanceEffect::new);
}