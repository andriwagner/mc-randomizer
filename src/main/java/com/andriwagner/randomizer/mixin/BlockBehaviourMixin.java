package com.andriwagner.randomizer.mixin;

import com.andriwagner.randomizer.event.BlockBehaviourEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {
    @Inject(method = "getDrops", at = @At(value = "HEAD"), cancellable = true)
    private static void getDrops(final BlockState state, final LootParams.Builder params, final CallbackInfoReturnable<List<ItemStack>> info) {
        List<ItemStack> result = BlockBehaviourEvents.GET_DROPS.invoker().getDrops(state, params);

        if (!result.isEmpty())
            info.setReturnValue(result);
    }
}
