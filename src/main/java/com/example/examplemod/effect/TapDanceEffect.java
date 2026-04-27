package com.example.examplemod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class TapDanceEffect extends MobEffect {
    public TapDanceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00AA00);
        // 不再添加任何属性修饰符，移速加成完全由事件控制
    }
}