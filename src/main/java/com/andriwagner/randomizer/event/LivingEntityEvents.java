package com.andriwagner.randomizer.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class LivingEntityEvents {
    public static final Event<DropFromLootTable> DROP_FROM_LOOT_TABLE_EVENTTable = EventFactory.createArrayBacked(DropFromLootTable.class,
            (listeners) -> (entity, level, source, playerKilled) -> {
                for (DropFromLootTable listener : listeners) {
                    boolean result = listener.dropFromLootTable(entity, level, source, playerKilled);

                    if (!result) {
                        return false;
                    }
                }

                return true;
            }
    );

    public interface DropFromLootTable {
        boolean dropFromLootTable(LivingEntity entity, final ServerLevel level, final DamageSource source, final boolean playerKilled);
    }
}
