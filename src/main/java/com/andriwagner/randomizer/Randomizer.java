package com.andriwagner.randomizer;

import com.andriwagner.randomizer.event.BlockBehaviourEvents;
import com.andriwagner.randomizer.event.LivingEntityEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.storage.loot.LootTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Randomizer implements ModInitializer {
	public static final String MOD_ID = "randomizer";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Component PREFIX = Component.literal("\uD83C\uDFB2 ").withStyle(Style.EMPTY
			.applyFormats(ChatFormatting.BOLD, ChatFormatting.DARK_RED)
			.withHoverEvent(new HoverEvent.ShowText(Component.literal("Randomizer"))));

	public static final Style MSG_STYLE = Style.EMPTY.applyFormat(ChatFormatting.AQUA);

	public static Map<Block, Item> blockDrops = new HashMap<>();
	public static Map<EntityType<?>, ResourceKey<LootTable>> mobDrops = new HashMap<>();

	// Game rules
	public static final GameRuleCategory randomizerGameRuleCategory = GameRuleCategory.register(id("randomizer"));
	public static final GameRuleCategory miscGameRuleCategory = GameRuleCategory.register(id("randomizer_misc"));

	public static final GameRule<Boolean> RANDOMIZE_BLOCK_DROPS_BOOLEAN_GAMERULE = GameRuleBuilder
			.forBoolean(false)
			.category(randomizerGameRuleCategory)
			.buildAndRegister(id("block_drops"));

	public static final GameRule<Boolean> RANDOMIZE_MOB_DROPS_BOOLEAN_GAMERULE = GameRuleBuilder
			.forBoolean(false)
			.category(randomizerGameRuleCategory)
			.buildAndRegister(id("mob_drops"));

	public static final GameRule<Boolean> CREATIVE_MODE_DROPS_BOOLEAN_GAMERULE = GameRuleBuilder
			.forBoolean(false)
			.category(miscGameRuleCategory)
			.buildAndRegister(id("creative_mode_drops"));

	public static final GameRule<Boolean> IGNORE_CORRECT_TOOL_BOOLEAN_GAMERULE = GameRuleBuilder
			.forBoolean(true)
			.category(miscGameRuleCategory)
			.buildAndRegister(id("ignore_correct_tool"));

	public static final GameRule<Boolean> LOG_TO_CHAT_BOOLEAN_GAMERULE = GameRuleBuilder
			.forBoolean(false)
			.category(miscGameRuleCategory)
			.buildAndRegister(id("log_to_chat"));

	@Override
	public void onInitialize() {
		// Server loaded event
		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
			randomizeBlockDrops(server.overworld().getSeed());
			randomizeMobDrops(server.overworld().getSeed());
		});

		// Player block break event
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos , state, blockEntity) -> {
			if (level.isClientSide())
				return true; // Pass event

			ServerLevel serverLevel = (ServerLevel)level;
			boolean randomizeBlockDropsGameRule = serverLevel.getGameRules().get(Randomizer.RANDOMIZE_BLOCK_DROPS_BOOLEAN_GAMERULE);
			boolean creativeModeDropsGameRule = serverLevel.getGameRules().get(Randomizer.CREATIVE_MODE_DROPS_BOOLEAN_GAMERULE);
			boolean ignoreCorrectToolGameRule = serverLevel.getGameRules().get(Randomizer.IGNORE_CORRECT_TOOL_BOOLEAN_GAMERULE);
			boolean logToChatGameRule = serverLevel.getLevel().getGameRules().get(Randomizer.LOG_TO_CHAT_BOOLEAN_GAMERULE);

			if (randomizeBlockDropsGameRule) {
				if (creativeModeDropsGameRule && Objects.requireNonNull(player.gameMode()).isCreative() || !player.hasCorrectToolForDrops(state) && ignoreCorrectToolGameRule && player.gameMode().isSurvival()) {
					state.getBlock().playerDestroy(level, player, pos, state, blockEntity, player.getUseItem());
				}

				if (logToChatGameRule) {
					ItemStack drop = new ItemStack(blockDrops.getOrDefault(state.getBlock(), null));

					Component msg = Component.empty()
							.append(PREFIX)
							.append("[Block] " + state.getBlock().getName().getString() + " ➡ " + drop.getItemName().getString())
							.withStyle(MSG_STYLE);

					player.sendSystemMessage(msg);
				}
			}

			return true; // Pass event
		});

		// Block get drop event
		BlockBehaviourEvents.GET_DROPS.register((state, params) -> {
			boolean randomizeBlockDropsGameRule = params.getLevel().getGameRules().get(Randomizer.RANDOMIZE_BLOCK_DROPS_BOOLEAN_GAMERULE);

			if (randomizeBlockDropsGameRule)
				return new ArrayList<>(List.of(new ItemStack(blockDrops.getOrDefault(state.getBlock(), null))));

			return new ArrayList<>();
		});

		// Mob drops
		LivingEntityEvents.DROP_FROM_LOOT_TABLE_EVENTTable.register(((entity, level, source, playerKilled) -> {
			boolean randomizeMobDropsGameRule = level.getGameRules().get(Randomizer.RANDOMIZE_MOB_DROPS_BOOLEAN_GAMERULE);

			if (!randomizeMobDropsGameRule)
				return true; // Pass event

			ResourceKey<LootTable> lootTable = mobDrops.getOrDefault(entity.getType(), null);
			entity.dropFromLootTable(level, source, playerKilled, lootTable);

			return false; // Cancel event
		}));

		// Entity kill event
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((level, entity, livingEntity, source) -> {

			if (entity instanceof ServerPlayer player) {
                boolean logToChatGameRule = level.getLevel().getGameRules().get(Randomizer.LOG_TO_CHAT_BOOLEAN_GAMERULE);
				
				if (logToChatGameRule) {
					ResourceKey<LootTable> lootTable = mobDrops.getOrDefault(livingEntity.getType(), null);
					Identifier mobIdentifier = Identifier.parse(lootTable.identifier().toString().replace("entities/", ""));

					Component msg = Component.empty()
							.append(PREFIX)
							.append("[Mob] " + livingEntity.getName().getString() + " ➡ " + BuiltInRegistries.ENTITY_TYPE.getValue(mobIdentifier).getDescription().getString())
							.withStyle(MSG_STYLE);

					player.sendSystemMessage(msg);
				}
			}
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

	public void randomizeMobDrops(long seed) {
		Random random = new Random(seed);
		mobDrops.clear();

		List<EntityType<?>> entityTypes = new ArrayList<>(BuiltInRegistries.ENTITY_TYPE.stream()
				.filter((e) -> e.getCategory() != MobCategory.MISC)
				.toList());

		List<ResourceKey<LootTable>> lootTables = new ArrayList<>();

		for (EntityType<?> entityType : entityTypes) {
			lootTables.add(entityType.getDefaultLootTable().get());
		}

		Collections.shuffle(lootTables, random);

		for (int i = 0; i < entityTypes.size(); i++) {
			mobDrops.put(entityTypes.get(i), lootTables.get(i));
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
