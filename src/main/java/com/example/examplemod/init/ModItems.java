package com.example.examplemod.init;

import com.example.examplemod.RAVE;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS_REGISTER =
            DeferredRegister.createItems(RAVE.MODID);

    // 向指定事件总线注册所有物品
    public static void register(IEventBus eventBus) {
        ITEMS_REGISTER.register(eventBus);
    }
}

