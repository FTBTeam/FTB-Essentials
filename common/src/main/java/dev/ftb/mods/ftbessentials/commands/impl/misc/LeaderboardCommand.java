package dev.ftb.mods.ftbessentials.commands.impl.misc;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.mixin.PlayerListAccess;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.Leaderboard;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LeaderboardCommand implements FTBCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderboardCommand.class);

    @Override
    public boolean enabled() {
        return FTBEConfig.LEADERBOARD.isEnabled();
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        return Collections.singletonList(Leaderboard.buildCommand());
    }

    public static <T extends Number> int leaderboard(CommandSourceStack source, Leaderboard<T> leaderboard, boolean reverse) {
        var knownPlayers = FTBEPlayerData.getAllKnownPlayers();
        var playerData = knownPlayers
                .stream()
                .map(uuid -> FTBEPlayerData.getOrCreate(source.getServer(), uuid))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        List<Pair<FTBEPlayerData, T>> list = new ArrayList<>();
        int self = -1;

        Path worldPath = source.getServer().getWorldPath(LevelResource.PLAYER_STATS_DIR);
        if (!Files.exists(worldPath)) {
            return 1;
        }

        playerData.forEach(pd -> {
            ServerStatsCounter stats = getPlayerStats(source.getServer(), pd.getUuid());

            T num = leaderboard.getValue(stats);
            if (leaderboard.test(num)) {
                list.add(Pair.of(pd, num));
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

        source.sendSuccess(() -> Component.literal("== Leaderboard [" + leaderboard.formattedName() + "] ==").withStyle(ChatFormatting.DARK_GREEN), false);

        if (list.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No data!").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        for (int i = 0; i < Math.min(20, list.size()); i++) {
            Pair<FTBEPlayerData, T> pair = list.get(i);
            String num = String.valueOf(i + 1);

            if (i + 1 < 10) {
                num = "0" + num;
            }

            MutableComponent component = Component.literal("");
            component.withStyle(ChatFormatting.GRAY);

            if (i == 0) {
                component.append(Component.literal("#" + num + " ").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
            } else if (i == 1) {
                component.append(Component.literal("#" + num + " ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xE0E0E0))));
            } else if (i == 2) {
                component.append(Component.literal("#" + num + " ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xCD7F32))));
            } else {
                component.append(Component.literal("#" + num + " ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xB4B4B4))));
            }

            var color = TextColor.fromRgb(0xCD7F32);

            component.append(Component.literal(pair.getLeft().getName()).withStyle(i == self ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            component.append(Component.literal(": "));
            component.append(Component.literal(leaderboard.asString(pair.getRight())).withStyle(ChatFormatting.WHITE));
            source.sendSuccess(() -> component, false);
        }

        return 1;
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
}
