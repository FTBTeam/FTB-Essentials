package dev.ftb.mods.ftbessentials.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.integration.PermissionsHelper;
import dev.ftb.mods.ftbessentials.mixin.PlayerListAccess;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData.RecordingStatus;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import dev.ftb.mods.ftbessentials.util.Leaderboard;
import dev.ftb.mods.ftblibrary.util.PlayerDisplayNameUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * @author LatvianModder
 */
public class MiscCommands {
	protected static final int DEFAULT_RADIUS = 200;   // default radius for /near command
	protected static final int MAX_PLAYER_RADIUS = 16; // max radius for non-admin users (adjustable with FTB Ranks)

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		if (FTBEConfig.KICKME.isEnabled()) {
			dispatcher.register(literal("kickme")
					.requires(FTBEConfig.KICKME)
					.executes(context -> kickme(context.getSource().getPlayerOrException()))
			);
		}

		if (FTBEConfig.TRASHCAN.isEnabled()) {
			dispatcher.register(literal("trashcan")
					.requires(FTBEConfig.TRASHCAN)
					.executes(context -> trashcan(context.getSource().getPlayerOrException()))
			);
		}

		if (FTBEConfig.ENDER_CHEST.isEnabled()) {
			dispatcher.register(literal("enderchest")
					.requires(FTBEConfig.ENDER_CHEST)
					.executes(context -> enderChest(context.getSource().getPlayerOrException(), null))
					.then(argument("player", EntityArgument.player())
							.requires(source -> source.hasPermission(2))
							.executes(context -> enderChest(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))
					)
			);
		}

		if (FTBEConfig.ANVIL.isEnabled()) {
			dispatcher.register(literal("anvil")
					.requires(FTBEConfig.ANVIL.enabledAndOp())
					.executes(context -> openWorkSite(context.getSource().getPlayerOrException(), "block.minecraft.anvil", (id, inv, player) -> new VirtualAnvilMenu(id, inv, (ServerPlayer) player)))
			);
		}

		if (FTBEConfig.SMITHING_TABLE.isEnabled()) {
			dispatcher.register(literal("smithing")
					.requires(FTBEConfig.SMITHING_TABLE.enabledAndOp())
					.executes(context -> openWorkSite(context.getSource().getPlayerOrException(), "block.minecraft.smithing_table", (id, inv, player) -> new VirtualSmithingMenu(id, inv, (ServerPlayer) player)))
			);
		}

		if (FTBEConfig.CRAFTING_TABLE.isEnabled()) {
			dispatcher.register(literal("crafting")
					.requires(FTBEConfig.CRAFTING_TABLE.enabledAndOp())
					.executes(context -> openWorkSite(context.getSource().getPlayerOrException(), "block.minecraft.crafting_table", (id, inv, player) -> new VirtualCraftingMenu(id, inv, (ServerPlayer) player)))
			);
		}

		if (FTBEConfig.STONECUTTER.isEnabled()) {
			dispatcher.register(literal("stonecutter")
					.requires(FTBEConfig.STONECUTTER.enabledAndOp())
					.executes(context -> openWorkSite(context.getSource().getPlayerOrException(), "block.minecraft.stonecutter", (id, inv, player) -> new VirtualStoneCutterMenu(id, inv, (ServerPlayer) player)))
			);
		}

		if (FTBEConfig.LEADERBOARD.isEnabled()) {
			dispatcher.register(Leaderboard.buildCommand());
		}

		if (FTBEConfig.REC.isEnabled()) {
			dispatcher.register(literal("recording")
					.requires(FTBEConfig.REC)
					.executes(context -> recording(context.getSource().getPlayerOrException()))
			);
			dispatcher.register(literal("streaming")
					.requires(FTBEConfig.REC)
					.executes(context -> streaming(context.getSource().getPlayerOrException()))
			);
		}

		if (FTBEConfig.HAT.isEnabled()) {
			dispatcher.register(literal("hat")
					.requires(FTBEConfig.HAT.enabledAndOp())
					.executes(context -> hat(context.getSource().getPlayerOrException()))
			);
		}

		if (FTBEConfig.NICK.isEnabled()) {
			dispatcher.register(literal("nickname")
					.requires(FTBEConfig.NICK)
					.executes(context -> nickname(context.getSource().getPlayerOrException(), ""))
					.then(argument("nickname", StringArgumentType.greedyString())
							.executes(context -> nickname(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "nickname")))
					)
			);
		}

		if (FTBEConfig.NEAR.isEnabled()) {
			dispatcher.register(literal("near")
					.requires(FTBEConfig.NEAR.enabledAndOp())
					.executes(context -> showNear(context.getSource(), context.getSource().getPlayerOrException(), DEFAULT_RADIUS))
					.then(argument("radius", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
							.executes(context -> showNear(context.getSource(), context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "radius")))
					)
					.then(argument("player", EntityArgument.player())
							.executes(context -> showNear(context.getSource(), EntityArgument.getPlayer(context, "player"), DEFAULT_RADIUS))
							.then(argument("radius", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
									.executes(context -> showNear(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "radius")))
							)
					)
			);
		}
	}

	private static int openWorkSite(ServerPlayer player, String xlateKey, MenuConstructor ctor) {
		player.openMenu(new SimpleMenuProvider((id, inv, p) -> ctor.createMenu(id, inv, player), Component.translatable(xlateKey)));

		return 1;
	}

	private static int enderChest(ServerPlayer player, @Nullable ServerPlayer target) {
		MutableComponent title = Component.translatable("container.enderchest");
		if (target != null) {
			title.append(" × ").append(target.getDisplayName());
		}

		final ServerPlayer t = target == null ? player : target;

		player.openMenu(new SimpleMenuProvider((i, inv, p) -> ChestMenu.threeRows(i, inv, t.getEnderChestInventory()), title));

		return 1;
	}

	public static int kickme(ServerPlayer player) {
		player.connection.disconnect(Component.literal("You kicked yourself!"));
		return 1;
	}

	public static int trashcan(ServerPlayer player) {
		player.openMenu(new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return Component.literal("Trash Can");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
				return ChestMenu.fourRows(id, playerInventory);
			}
		});

		return 1;
	}

	public static <T extends Number> int leaderboard(CommandSourceStack source, Leaderboard<T> leaderboard, boolean reverse) {
		try (var stream = Files.list(FTBEWorldData.instance.mkdirs("playerdata"))) {
			stream.filter(path -> path.toString().endsWith(".json"))
					.map(Path::getFileName)
					.map(path -> new GameProfile(UUID.fromString(path.toString().replace(".json", "")), null))
					.filter(profile -> !FTBEPlayerData.playerExists(profile.getId()))
					.map(FTBEPlayerData::getOrCreate)
					.filter(Optional::isPresent)
					.forEach(data -> data.get().load());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		List<Pair<FTBEPlayerData, T>> list = new ArrayList<>();
		int self = -1;

		FTBEPlayerData.forEachPlayer(playerData -> {
			ServerStatsCounter stats = getPlayerStats(source.getServer(), playerData.getUuid());

			T num = leaderboard.getValue(stats);
			if (leaderboard.test(num)) {
				list.add(Pair.of(playerData, num));
			}
		});

		if (reverse) {
			list.sort(Comparator.comparingDouble(pair -> pair.getRight().doubleValue()));
		} else {
			list.sort((pair1, pair2) -> Double.compare(pair2.getRight().doubleValue(), pair1.getRight().doubleValue()));
		}

		if (source.getEntity() instanceof ServerPlayer) {
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i).getLeft().getUuid().equals(source.getEntity().getUUID())) {
					self = list.size();
					break;
				}
			}
		}

		source.sendSuccess(() -> Component.literal("== Leaderboard [" + leaderboard.getName() + "] ==").withStyle(ChatFormatting.DARK_GREEN), false);

		if (list.isEmpty()) {
			source.sendSuccess(() -> Component.literal("No data!").withStyle(ChatFormatting.GRAY), false);
			return 1;
		}

		for (int i = 0; i < Math.min(20, list.size()); i++) {
			Pair<FTBEPlayerData, T> pair = list.get(i);
			String num = String.valueOf(i + 1);

			if (i < 10) {
				num = "0" + num;
			}

			MutableComponent component = Component.literal("");
			component.withStyle(ChatFormatting.GRAY);

			if (i == 0) {
				component.append(Component.literal("#" + num + " ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xD4AF37))));
			} else if (i == 1) {
				component.append(Component.literal("#" + num + " ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xC0C0C0))));
			} else if (i == 2) {
				component.append(Component.literal("#" + num + " ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x9F7A34))));
			} else {
				component.append(Component.literal("#" + num + " "));
			}

			component.append(Component.literal(pair.getLeft().getName()).withStyle(i == self ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
			component.append(Component.literal(": "));
			component.append(Component.literal(leaderboard.asString(pair.getRight())));
			source.sendSuccess(() -> component, false);
		}

		return 1;
	}

	public static int recording(ServerPlayer player) {
		return FTBEPlayerData.getOrCreate(player).map(data -> {
			data.setRecording(data.getRecording() == RecordingStatus.RECORDING ? RecordingStatus.NONE : RecordingStatus.RECORDING);
			PlayerDisplayNameUtil.refreshDisplayName(player);

			if (data.getRecording() == RecordingStatus.RECORDING) {
				player.server.getPlayerList().broadcastSystemMessage(player.getDisplayName().copy().withStyle(ChatFormatting.YELLOW).append(" is now recording!"), false);
			} else {
				player.server.getPlayerList().broadcastSystemMessage(player.getDisplayName().copy().withStyle(ChatFormatting.YELLOW).append(" is no longer recording!"), false);
			}

			data.sendTabName(player.server);
			return 1;
		}).orElse(0);
	}

	public static int streaming(ServerPlayer player) {
		return FTBEPlayerData.getOrCreate(player).map(data -> {
			data.setRecording(data.getRecording() == RecordingStatus.STREAMING ? RecordingStatus.NONE : RecordingStatus.STREAMING);
			PlayerDisplayNameUtil.refreshDisplayName(player);

			if (data.getRecording() == RecordingStatus.STREAMING) {
				player.server.getPlayerList().broadcastSystemMessage(player.getDisplayName().copy().withStyle(ChatFormatting.YELLOW).append(" is now streaming!"), false);
			} else {
				player.server.getPlayerList().broadcastSystemMessage(player.getDisplayName().copy().withStyle(ChatFormatting.YELLOW).append(" is no longer streaming!"), false);
			}

			data.sendTabName(player.server);
			return 1;
		}).orElse(0);
	}

	public static int hat(ServerPlayer player) {
		ItemStack hstack = player.getItemBySlot(EquipmentSlot.HEAD);
		ItemStack istack = player.getItemBySlot(EquipmentSlot.MAINHAND);
		player.setItemSlot(EquipmentSlot.HEAD, istack);
		player.setItemSlot(EquipmentSlot.MAINHAND, hstack);
		player.inventoryMenu.broadcastChanges();
		return 1;
	}

	public static int nickname(ServerPlayer player, String nick) {
		if (nick.length() > 30) {
			player.displayClientMessage(Component.literal("Nickname too long!"), false);
			return 0;
		}

		return FTBEPlayerData.getOrCreate(player).map(data -> {
			data.setNick(nick.trim());
			PlayerDisplayNameUtil.refreshDisplayName(player);

			if (data.getNick().isEmpty()) {
				player.displayClientMessage(Component.literal("Nickname reset!"), false);
			} else {
				player.displayClientMessage(Component.literal("Nickname changed to '" + data.getNick() + "'"), false);
			}

			data.sendTabName(player.server);
			return 1;
		}).orElse(0);
	}

	/**
	 * Like {@link net.minecraft.server.players.PlayerList#getPlayerStats(Player)} but doesn't need an online player.
	 * @param server the server
	 * @param playerId UUID of the player
	 * @return the server stats
	 */
	private static ServerStatsCounter getPlayerStats(MinecraftServer server, UUID playerId) {
		Map<UUID, ServerStatsCounter> stats = ((PlayerListAccess) server.getPlayerList()).getStats();
		return stats.computeIfAbsent(playerId, k -> {
			File file1 = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile();
			File file2 = new File(file1, playerId + ".json");
			return new ServerStatsCounter(server, file2);
		});
	}

	private static int showNear(CommandSourceStack source, ServerPlayer target, int radius) {
		if (!source.hasPermission(2) && source.isPlayer()) {
			int max = PermissionsHelper.getInstance().getInt(source.getPlayer(), MAX_PLAYER_RADIUS, "ftbessentials.near.max_radius");
			if (radius > max) {
				source.sendSuccess(() -> Component.literal("Limiting radius to " + max).withStyle(ChatFormatting.GOLD), false);
				radius = max;
			}
		}

		int radius2 = radius * radius;
		List<ServerPlayer> l = target.getServer().getPlayerList().getPlayers().stream()
				.filter(other -> other != target)
				.filter(other -> other.distanceToSqr(target) < radius2)
				.sorted(Comparator.comparingDouble(o -> o.distanceToSqr(target)))
				.toList();

		final int r = radius;
		source.sendSuccess(() -> Component.literal(l.size() + " player(s) within " + r + "m").withStyle(ChatFormatting.YELLOW), false);
		l.forEach(player ->
				source.sendSuccess(() -> Component.literal("• ")
								.append(player.getDisplayName()).withStyle(ChatFormatting.AQUA)
								.append(String.format(" - %5.2fm", player.distanceTo(target))),
						false
				)
		);

		return 1;
	}

	private static class VirtualAnvilMenu extends AnvilMenu {
		public VirtualAnvilMenu(int id, Inventory inv, ServerPlayer player) {
			super(id, inv, ContainerLevelAccess.create(player.level(), player.blockPosition()));
		}

		@Override
		protected boolean isValidBlock(BlockState blockState) {
			return true;
		}
	}

	private static class VirtualSmithingMenu extends SmithingMenu {
		public VirtualSmithingMenu(int id, Inventory inv, ServerPlayer player) {
			super(id, inv, ContainerLevelAccess.create(player.level(), player.blockPosition()));
		}

		@Override
		protected boolean isValidBlock(BlockState blockState) {
			return true;
		}
	}

	private static class VirtualStoneCutterMenu extends StonecutterMenu {
		public VirtualStoneCutterMenu(int id, Inventory inv, ServerPlayer player) {
			super(id, inv, ContainerLevelAccess.create(player.level(), player.blockPosition()));
		}

		@Override
		public boolean stillValid(Player player) {
			return true;
		}
	}

	private static class VirtualCraftingMenu extends CraftingMenu {
		public VirtualCraftingMenu(int id, Inventory inv, ServerPlayer player) {
			super(id, inv, ContainerLevelAccess.create(player.level(), player.blockPosition()));
		}

		@Override
		public boolean stillValid(Player player) {
			return true;
		}
	}
}
