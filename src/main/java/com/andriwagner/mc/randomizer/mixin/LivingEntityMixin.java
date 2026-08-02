package com.andriwagner.mc.randomizer.mixin;

import com.andriwagner.mc.randomizer.event.LivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V", at = @At(value = "HEAD"), cancellable = true)
    private void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled, CallbackInfo info) {
        boolean result = LivingEntityEvents.DROP_FROM_LOOT_TABLE_EVENTTable.invoker().dropFromLootTable((LivingEntity) (Object) this, level, source, playerKilled);

        if (!result)
            info.cancel();
    }
}
