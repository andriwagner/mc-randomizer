package com.andriwagner.mc.randomizer;

import com.andriwagner.mc.randomizer.event.BlockEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Randomizer implements ModInitializer {
	public static final String MOD_ID = "randomizer";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static Map<Block, Item> blockDrops = new HashMap<>();

	// Game rules
	public static final GameRuleCategory randomizerGameRuleCategory = GameRuleCategory.register(id("randomizer"));
	public static final GameRule<Boolean> RANDOMIZE_BLOCK_DROPS_BOOLEAN_GAMERULE = GameRuleBuilder
			.forBoolean(false) // Default value declaration
			.category(randomizerGameRuleCategory)
			.buildAndRegister(id("block_drops"));
	public static final GameRule<Boolean> CREATIVE_MODE_DROPS_BOOLEAN_GAMERULE = GameRuleBuilder
			.forBoolean(false) // Default value declaration
			.category(randomizerGameRuleCategory)
			.buildAndRegister(id("creative_mode_drops"));

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");

		// Server loaded event
		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
			randomizeBlockDrops(server.overworld().getSeed());
		});

		// Player block break event
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos , state, blockEntity) -> {
			if (level.isClientSide())
				return true; // Pass event

			ServerLevel serverLevel = (ServerLevel)level;
			boolean randomizeBlockDropsGameRule = serverLevel.getGameRules().get(Randomizer.RANDOMIZE_BLOCK_DROPS_BOOLEAN_GAMERULE);
			boolean creativeModeDropsGameRule = serverLevel.getGameRules().get(Randomizer.CREATIVE_MODE_DROPS_BOOLEAN_GAMERULE);

			if (randomizeBlockDropsGameRule && creativeModeDropsGameRule && Objects.requireNonNull(player.gameMode()).isCreative())
				state.getBlock().playerDestroy(level, player, pos, state, blockEntity, player.getUseItem());

			return true; // Pass event
		});

		// Block drop event
		BlockEvents.GET_DROPS.register((state, level, pos, blockEntity, breaker, tool) -> {
			boolean randomizeBlockDropsGameRule = level.getGameRules().get(Randomizer.RANDOMIZE_BLOCK_DROPS_BOOLEAN_GAMERULE);

			if (randomizeBlockDropsGameRule)
				return new ArrayList<>(List.of(new ItemStack(blockDrops.getOrDefault(state.getBlock(), null))));

			return new ArrayList<>();
		});

	}

	public void randomizeBlockDrops(long seed) {
		Random random = new Random(seed);
		blockDrops.clear();

		List<Item> items = new ArrayList<>(BuiltInRegistries.ITEM.stream().toList());

		List<Block> blocks = new ArrayList<>(BuiltInRegistries.BLOCK.stream()
				.filter(this::filterNoneBreakableBlocks)
				.toList());

		Collections.shuffle(items, random);

		for (int i = 0; i < blocks.size(); i++) {
			blockDrops.put(blocks.get(i), items.get(i));
		}
	}

	public boolean filterNoneBreakableBlocks(Block block) {
		BlockState blockState = block.defaultBlockState();
        return !blockState.isAir() && block.defaultDestroyTime() != 100.0 && block.defaultDestroyTime() != -1.0;
    }

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
