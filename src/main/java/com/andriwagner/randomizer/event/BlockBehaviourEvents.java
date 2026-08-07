package com.andriwagner.randomizer.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.ArrayList;
import java.util.List;

public final class BlockBehaviourEvents {
    public static final Event<GetDrops> GET_DROPS = EventFactory.createArrayBacked(GetDrops.class,
            (listeners) -> (state, params) -> {
                for (GetDrops listener : listeners) {
                    return listener.getDrops(state, params);
                }
                return new ArrayList<>();
            }
    );

    @FunctionalInterface
    public interface GetDrops {
        List<ItemStack> getDrops(final BlockState state, final LootParams.Builder params);
    }
}
