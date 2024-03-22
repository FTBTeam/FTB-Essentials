package dev.ftb.mods.ftbessentials.commands.impl.misc;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.mixin.PlayerListAccess;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LeaderboardCommand implements FTBCommand {
    @Override
    public boolean enabled() {
        return FTBEConfig.LEADERBOARD.isEnabled();
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        return Collections.singletonList(Leaderboard.buildCommand());
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
