package com.andriwagner.mc.randomizer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRules;
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

		// Block break event
		PlayerBlockBreakEvents.BEFORE.register((level, player, blockPos , blockState, blockEntity) -> {
			if (level.isClientSide())
				return true; // Pass event

			ServerLevel serverLevel = (ServerLevel)level;

			// Check game rules
			boolean blockDropsGameRule = serverLevel.getGameRules().get(GameRules.BLOCK_DROPS);
			boolean randomizeBlockDropsGameRule = serverLevel.getGameRules().get(Randomizer.RANDOMIZE_BLOCK_DROPS_BOOLEAN_GAMERULE);
			boolean creativeModeDropsGameRule = serverLevel.getGameRules().get(Randomizer.CREATIVE_MODE_DROPS_BOOLEAN_GAMERULE);

			if (!blockDropsGameRule || !randomizeBlockDropsGameRule)
				return true; // Pass event

			if (!creativeModeDropsGameRule && Objects.requireNonNull(player.gameMode()).isCreative())
				return true; // Pass event

			// Destroy block
			level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);

			// Drop item
			ItemStack drop = new ItemStack(blockDrops.getOrDefault(blockState.getBlock(), null));
			ItemEntity itemEntity = new ItemEntity(
					serverLevel,
					blockPos.getX() + 0.5, // X coordinate (center of block)
					blockPos.getY() + 0.5, // Y coordinate (center of block)
					blockPos.getZ() + 0.5, // Z coordinate (center of block)
					drop
			);
			itemEntity.spawnAtLocation(serverLevel, drop);

			// Apply item damage
			ItemStack itemInHand = player.getActiveItem();
			itemInHand.hurtAndBreak(1, (LivingEntity)player, player.getEquipmentSlotForItem(itemInHand));

			return false; // Cancel event
		});

		// TODO: Implement randomized natural block drops

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
