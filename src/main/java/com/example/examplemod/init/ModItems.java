package com.example.examplemod.init;

import com.example.examplemod.RAVE;
import com.example.examplemod.effect.TapDanceEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    // 创建附魔和药水效果的注册器，绑定到你的模组 ID "rave"
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(Registries.ENCHANTMENT, RAVE.MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, RAVE.MODID);

    // 注册“踢踏舞”附魔
    // 注意这里使用了 () -> null，因为附魔的具体属性会由后面的 JSON 文件提供
    public static final DeferredHolder<Enchantment, Enchantment> TAP_DANCE =
            ENCHANTMENTS.register("tap_dance", () -> null);

    // 注册“踢踏舞”状态效果，对应的效果类我们下一步就创建
    public static final DeferredHolder<MobEffect, MobEffect> TAP_DANCE_EFFECT =
            MOB_EFFECTS.register("tap_dance_effect", TapDanceEffect::new);
}