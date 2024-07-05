package dev.ftb.mods.ftbessentials.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import dev.ftb.mods.ftbessentials.util.Leaderboard;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author LatvianModder
 */
public class MiscCommands {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		if (FTBEConfig.KICKME.isEnabled()) {
			dispatcher.register(Commands.literal("kickme")
					.requires(FTBEConfig.KICKME)
					.executes(context -> kickme(context.getSource().getPlayerOrException()))
			);
		}

		if (FTBEConfig.TRASHCAN.isEnabled()) {
			dispatcher.register(Commands.literal("trashcan")
					.requires(FTBEConfig.TRASHCAN)
					.executes(context -> trashcan(context.getSource().getPlayerOrException()))
			);
		}

		if (FTBEConfig.ENDER_CHEST.isEnabled()) {
			dispatcher.register(Commands.literal("enderchest")
					.requires(FTBEConfig.ENDER_CHEST)
					.executes(context -> enderChest(context.getSource().getPlayerOrException(), null))
					.then(Commands.argument("player", EntityArgument.player())
							.requires(source -> source.hasPermission(2))
							.executes(context -> enderChest(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))
					)
			);
		}

		if (FTBEConfig.LEADERBOARD.isEnabled()) {
			LiteralArgumentBuilder<CommandSourceStack> leaderboardCommand = Commands.literal("leaderboard");

			for (Leaderboard<?> leaderboard : Leaderboard.MAP.values()) {
				leaderboardCommand = leaderboardCommand.then(Commands.literal(leaderboard.name).executes(context -> leaderboard(context.getSource(), leaderboard, false)));
			}

			dispatcher.register(leaderboardCommand);
		}


		if (FTBEConfig.REC.isEnabled()) {
			dispatcher.register(Commands.literal("recording")
					.requires(FTBEConfig.REC)
					.executes(context -> recording(context.getSource().getPlayerOrException()))
			);

			dispatcher.register(Commands.literal("streaming")
					.requires(FTBEConfig.REC)
					.executes(context -> streaming(context.getSource().getPlayerOrException()))
			);
		}

		if (FTBEConfig.HAT.isEnabled()) {
			dispatcher.register(Commands.literal("hat")
					.requires(FTBEConfig.HAT.enabledAndOp())
					.executes(context -> hat(context.getSource().getPlayerOrException()))
			);
		}

		if (FTBEConfig.NICK.isEnabled()) {
			dispatcher.register(Commands.literal("nickname")
					.requires(FTBEConfig.NICK)
					.executes(context -> nickname(context.getSource().getPlayerOrException(), ""))
					.then(Commands.argument("nickname", StringArgumentType.greedyString())
							.executes(context -> nickname(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "nickname")))
					)
			);
		}
	}

	private static int enderChest(ServerPlayer player, @Nullable ServerPlayer target) {

		MutableComponent title = new TranslatableComponent("container.enderchest");
		if (target != null) {
			title.append(" × ").append(target.getDisplayName());
		}

		final ServerPlayer t = target == null ? player : target;

		player.openMenu(new SimpleMenuProvider((i, inv, p) -> ChestMenu.threeRows(i, inv, t.getEnderChestInventory()), title));

		return 1;
	}

	public static int kickme(ServerPlayer player) {
		player.connection.disconnect(new TranslatableComponent("misc_command_message.ftbessentials.kickme"));
		return 1;
	}

	public static int trashcan(ServerPlayer player) {
		player.openMenu(new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return new TranslatableComponent("misc_command_name.ftbessentials.trash_can");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
				return ChestMenu.fourRows(id, playerInventory);
			}
		});

		return 1;
	}

	public static <T extends Number> int leaderboard(CommandSourceStack source, Leaderboard<T> leaderboard, boolean reverse) {
		try {
			Files.list(FTBEWorldData.instance.mkdirs("playerdata"))
					.filter(path -> path.toString().endsWith(".snbt"))
					.map(Path::getFileName)
					.map(path -> profileWithCachedName(UUID.fromString(path.toString().replace(".snbt", ""))))
					.map(FTBEPlayerData::get)
					.filter(Objects::nonNull)
					.forEach(FTBEPlayerData::load);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		List<Pair<FTBEPlayerData, T>> list = new ArrayList<>();
		int self = -1;

		for (FTBEPlayerData playerData : FTBEPlayerData.MAP.values()) {
			ServerStatsCounter stats = source.getServer().getPlayerList().getPlayerStats(FakePlayerFactory.get(source.getLevel(), new GameProfile(playerData.uuid, playerData.name)));

			T num = leaderboard.valueGetter.apply(stats);

			if (leaderboard.filter.test(num)) {
				list.add(Pair.of(playerData, num));
			}
		}

		if (reverse) {
			list.sort(Comparator.comparingDouble(pair -> pair.getRight().doubleValue()));
		} else {
			list.sort((pair1, pair2) -> Double.compare(pair2.getRight().doubleValue(), pair1.getRight().doubleValue()));
		}

		if (source.getEntity() instanceof ServerPlayer) {
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i).getLeft().uuid.equals(source.getEntity().getUUID())) {
					self = list.size();
					break;
				}
			}
		}

		source.sendSuccess(new TranslatableComponent("misc_command_message.ftbessentials.leaderboard_title").append(new TextComponent(" ==[" + leaderboard.name + "]== ")).withStyle(ChatFormatting.DARK_GREEN), false);

		if (list.isEmpty()) {
			source.sendSuccess(new TranslatableComponent("misc_command_message.ftbessentials.leaderboard_nodata").withStyle(ChatFormatting.GRAY), false);
			return 1;
		}

		for (int i = 0; i < Math.min(20, list.size()); i++) {
			Pair<FTBEPlayerData, T> pair = list.get(i);
			String num = String.valueOf(i + 1);

			if (i < 10) {
				num = "0" + num;
			}

			TextComponent component = new TextComponent("");
			component.withStyle(ChatFormatting.GRAY);

			if (i == 0) {
				component.append(new TextComponent("#" + num + " ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xD4AF37))));
			} else if (i == 1) {
				component.append(new TextComponent("#" + num + " ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xC0C0C0))));
			} else if (i == 2) {
				component.append(new TextComponent("#" + num + " ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x9F7A34))));
			} else {
				component.append(new TextComponent("#" + num + " "));
			}

			component.append(new TextComponent(pair.getLeft().name).withStyle(i == self ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
			component.append(new TextComponent(": "));
			component.append(new TextComponent(leaderboard.stringGetter.apply(pair.getRight())));
			source.sendSuccess(component, false);
		}

		return 1;
	}

	private static GameProfile profileWithCachedName(UUID id) {
		return new GameProfile(id, UsernameCache.getLastKnownUsername(id));
	}

	public static int recording(ServerPlayer player) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		data.recording = data.recording == 1 ? 0 : 1;
		data.save();
		player.refreshDisplayName();

		if (data.recording == 1) {
			player.server.getPlayerList().broadcastMessage(new TextComponent("").append(player.getDisplayName().copy().withStyle(ChatFormatting.YELLOW)).append(new TranslatableComponent("misc_command_message.ftbessentials.recording_on")), ChatType.CHAT, Util.NIL_UUID);
		} else {
			player.server.getPlayerList().broadcastMessage(new TextComponent("").append(player.getDisplayName().copy().withStyle(ChatFormatting.YELLOW)).append(new TranslatableComponent("misc_command_message.ftbessentials.recording_off")), ChatType.CHAT, Util.NIL_UUID);
		}

		data.sendTabName(player.server);
		return 1;
	}

	public static int streaming(ServerPlayer player) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		data.recording = data.recording == 2 ? 0 : 2;
		data.save();
		player.refreshDisplayName();

		if (data.recording == 2) {
			player.server.getPlayerList().broadcastMessage(new TextComponent("").append(player.getDisplayName().copy().withStyle(ChatFormatting.YELLOW)).append(new TranslatableComponent("misc_command_message.ftbessentials.streaming_on")), ChatType.CHAT, Util.NIL_UUID);
		} else {
			player.server.getPlayerList().broadcastMessage(new TextComponent("").append(player.getDisplayName().copy().withStyle(ChatFormatting.YELLOW)).append(new TranslatableComponent("misc_command_message.ftbessentials.streaming_off")), ChatType.CHAT, Util.NIL_UUID);
		}

		data.sendTabName(player.server);
		return 1;
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
			player.displayClientMessage(new TranslatableComponent("misc_command_message.ftbessentials.nickname_toolong"), false);
			return 0;
		}

		FTBEPlayerData data = FTBEPlayerData.get(player);
		data.nick = nick.trim();

		data.save();
		player.refreshDisplayName();

		if (data.nick.isEmpty()) {
			player.displayClientMessage(new TranslatableComponent("misc_command_message.ftbessentials.nickname_reset"), false);
		} else {
			player.displayClientMessage(new TranslatableComponent("misc_command_message.ftbessentials.nickname_changedto").append(new TextComponent(data.nick + "'")), false);
		}

		data.sendTabName(player.server);
		return 1;
	}
}
