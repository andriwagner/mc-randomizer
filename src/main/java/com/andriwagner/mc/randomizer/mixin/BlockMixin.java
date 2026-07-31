package com.andriwagner.mc.randomizer.mixin;

import com.andriwagner.mc.randomizer.event.BlockEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "getDrops*", at = @At(value = "HEAD"), cancellable = true)
    private static void getDrops(final BlockState state, final ServerLevel level, final BlockPos pos, final @Nullable BlockEntity blockEntity, final @Nullable Entity breaker, final ItemInstance tool, final CallbackInfoReturnable<List<ItemStack>> info) {
        List<ItemStack> result = BlockEvents.GET_DROPS.invoker().getDrops(state, level, pos, blockEntity, breaker, tool);

        if (!result.isEmpty())
            info.setReturnValue(result);
    }
}
