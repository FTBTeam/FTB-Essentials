package com.feed_the_beast.mods.ftbessentials.command;

import com.feed_the_beast.mods.ftbessentials.util.FTBEPlayerData;
import com.feed_the_beast.mods.ftbessentials.util.FTBEWorldData;
import com.feed_the_beast.mods.ftbessentials.util.Leaderboard;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.ServerStatisticsManager;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class MiscCommands
{
	public static void register(CommandDispatcher<CommandSource> dispatcher)
	{
		dispatcher.register(Commands.literal("kickme")
				.executes(context -> kickme(context.getSource().asPlayer()))
		);

		dispatcher.register(Commands.literal("trashcan")
				.executes(context -> trashcan(context.getSource().asPlayer()))
		);

		LiteralArgumentBuilder<CommandSource> leaderboardCommand = Commands.literal("leaderboard");

		for (Leaderboard<?> leaderboard : Leaderboard.MAP.values())
		{
			leaderboardCommand = leaderboardCommand.then(Commands.literal(leaderboard.name).executes(context -> leaderboard(context.getSource(), leaderboard, false)));
		}

		dispatcher.register(leaderboardCommand);

		dispatcher.register(Commands.literal("recording")
				.executes(context -> recording(context.getSource().asPlayer()))
		);

		dispatcher.register(Commands.literal("streaming")
				.executes(context -> streaming(context.getSource().asPlayer()))
		);

		dispatcher.register(Commands.literal("hat")
				.executes(context -> hat(context.getSource().asPlayer()))
		);

		dispatcher.register(Commands.literal("nickname")
				.executes(context -> nickname(context.getSource().asPlayer(), ""))
				.then(Commands.argument("nickname", StringArgumentType.greedyString())
						.executes(context -> nickname(context.getSource().asPlayer(), StringArgumentType.getString(context, "nickname")))
				)
		);
	}

	public static int kickme(ServerPlayerEntity player)
	{
		player.connection.disconnect(new StringTextComponent("You kicked yourself!"));
		return 1;
	}

	public static int trashcan(ServerPlayerEntity player)
	{
		player.openContainer(new INamedContainerProvider()
		{
			@Override
			public ITextComponent getDisplayName()
			{
				return new StringTextComponent("Trash Can");
			}

			@Override
			public Container createMenu(int id, PlayerInventory playerInventory, PlayerEntity player)
			{
				return ChestContainer.createGeneric9X4(id, playerInventory);
			}
		});

		return 1;
	}

	public static <T extends Number> int leaderboard(CommandSource source, Leaderboard<T> leaderboard, boolean reverse)
	{
		try
		{
			Files.list(FTBEWorldData.instance.mkdirs("playerdata"))
					.filter(path -> path.toString().endsWith(".json"))
					.map(Path::getFileName)
					.map(path -> new GameProfile(UUID.fromString(path.toString().replace(".json", "")), null))
					.forEach(FTBEPlayerData::get);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		List<Pair<FTBEPlayerData, T>> list = new ArrayList<>();
		int self = -1;

		for (FTBEPlayerData playerData : FTBEPlayerData.MAP.values())
		{
			ServerStatisticsManager stats = source.getServer().getPlayerList().getPlayerStats(FakePlayerFactory.get(source.getWorld(), new GameProfile(playerData.uuid, playerData.name)));

			T num = leaderboard.valueGetter.apply(stats);

			if (leaderboard.filter.test(num))
			{
				list.add(Pair.of(playerData, num));
			}
		}

		if (reverse)
		{
			list.sort(Comparator.comparingDouble(pair -> pair.getRight().doubleValue()));
		}
		else
		{
			list.sort((pair1, pair2) -> Double.compare(pair2.getRight().doubleValue(), pair1.getRight().doubleValue()));
		}

		if (source.getEntity() instanceof ServerPlayerEntity)
		{
			for (int i = 0; i < list.size(); i++)
			{
				if (list.get(i).getLeft().uuid.equals(source.getEntity().getUniqueID()))
				{
					self = list.size();
					break;
				}
			}
		}

		source.sendFeedback(new StringTextComponent("== Leaderboard [" + leaderboard.name + "] ==").mergeStyle(TextFormatting.DARK_GREEN), false);

		if (list.isEmpty())
		{
			source.sendFeedback(new StringTextComponent("No data!").mergeStyle(TextFormatting.GRAY), false);
			return 1;
		}

		for (int i = 0; i < Math.min(20, list.size()); i++)
		{
			Pair<FTBEPlayerData, T> pair = list.get(i);
			String num = String.valueOf(i + 1);

			if (i < 10)
			{
				num = "0" + num;
			}

			StringTextComponent component = new StringTextComponent("");
			component.mergeStyle(TextFormatting.GRAY);

			if (i == 0)
			{
				component.append(new StringTextComponent("#" + num + " ").mergeStyle(Style.EMPTY.setColor(Color.fromInt(0xD4AF37))));
			}
			else if (i == 1)
			{
				component.append(new StringTextComponent("#" + num + " ").mergeStyle(Style.EMPTY.setColor(Color.fromInt(0xC0C0C0))));
			}
			else if (i == 2)
			{
				component.append(new StringTextComponent("#" + num + " ").mergeStyle(Style.EMPTY.setColor(Color.fromInt(0x9F7A34))));
			}
			else
			{
				component.append(new StringTextComponent("#" + num + " "));
			}

			component.append(new StringTextComponent(pair.getLeft().name).mergeStyle(i == self ? TextFormatting.GREEN : TextFormatting.YELLOW));
			component.append(new StringTextComponent(": "));
			component.append(new StringTextComponent(leaderboard.stringGetter.apply(pair.getRight())));
			source.sendFeedback(component, false);
		}

		return 1;
	}

	public static int recording(ServerPlayerEntity player)
	{
		FTBEPlayerData data = FTBEPlayerData.get(player);
		data.recording = data.recording == 1 ? 0 : 1;
		data.save();
		player.refreshDisplayName();

		if (data.recording == 1)
		{
			player.server.getPlayerList().func_232641_a_(new StringTextComponent("").append(player.getDisplayName().deepCopy().mergeStyle(TextFormatting.YELLOW)).appendString(" is now recording!"), ChatType.CHAT, Util.DUMMY_UUID);
		}
		else
		{
			player.server.getPlayerList().func_232641_a_(new StringTextComponent("").append(player.getDisplayName().deepCopy().mergeStyle(TextFormatting.YELLOW)).appendString(" is no longer recording!"), ChatType.CHAT, Util.DUMMY_UUID);
		}

		return 1;
	}

	public static int streaming(ServerPlayerEntity player)
	{
		FTBEPlayerData data = FTBEPlayerData.get(player);
		data.recording = data.recording == 2 ? 0 : 2;
		data.save();
		player.refreshDisplayName();

		if (data.recording == 2)
		{
			player.server.getPlayerList().func_232641_a_(new StringTextComponent("").append(player.getDisplayName().deepCopy().mergeStyle(TextFormatting.YELLOW)).appendString(" is now streaming!"), ChatType.CHAT, Util.DUMMY_UUID);
		}
		else
		{
			player.server.getPlayerList().func_232641_a_(new StringTextComponent("").append(player.getDisplayName().deepCopy().mergeStyle(TextFormatting.YELLOW)).appendString(" is no longer streaming!"), ChatType.CHAT, Util.DUMMY_UUID);
		}

		return 1;
	}

	public static int hat(ServerPlayerEntity player)
	{
		ItemStack hstack = player.getItemStackFromSlot(EquipmentSlotType.HEAD);
		ItemStack istack = player.getItemStackFromSlot(EquipmentSlotType.MAINHAND);
		player.setItemStackToSlot(EquipmentSlotType.HEAD, istack);
		player.setItemStackToSlot(EquipmentSlotType.MAINHAND, hstack);
		player.container.detectAndSendChanges();
		return 1;
	}

	public static int nickname(ServerPlayerEntity player, String nick)
	{
		FTBEPlayerData data = FTBEPlayerData.get(player);
		data.nick = nick.trim();
		data.save();
		player.refreshDisplayName();

		if (data.nick.isEmpty())
		{
			player.sendStatusMessage(new StringTextComponent("Nickname reset!"), false);
		}
		else
		{
			player.sendStatusMessage(new StringTextComponent("Nickname changed to '" + data.nick + "'"), false);
		}

		return 1;
	}
}
