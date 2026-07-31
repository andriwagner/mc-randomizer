package com.andriwagner.mc.randomizer.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public interface BlockEvents {
    Event<BlockEvents> GET_DROPS = EventFactory.createArrayBacked(BlockEvents.class,
            (listeners) -> (state, level, pos, blockEntity, breaker, tool) -> {
                for (BlockEvents listener : listeners) {
                    return listener.getDrops(state, level, pos, blockEntity, breaker, tool);
                }
                return new ArrayList<>();
            }
    );
    List<ItemStack> getDrops(final BlockState state, final ServerLevel level, final BlockPos pos, final @Nullable BlockEntity blockEntity, final @Nullable Entity breaker, final ItemInstance tool);
}
