package dev.ftb.mods.ftbessentials.integration;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

public class LuckPermsIntegration {
    public static int getInt(ServerPlayer player, int def, String node) {
        return Math.max(getMetaData(player.getUUID(), node).map(Integer::parseInt).orElse(def), 0);
    }

    private static Optional<String> getMetaData(UUID uuid, String meta) {
        LuckPerms luckperms = LuckPermsProvider.get();
        Optional<String> metaValue = Optional.empty();
        try {
            User user = luckperms.getUserManager().getUser(uuid);
            if (user != null) {
                Optional<QueryOptions> context = luckperms.getContextManager().getQueryOptions(user);
                if (context.isPresent()) {
                    metaValue = Optional.ofNullable(user.getCachedData().getMetaData(context.get()).getMetaValue(meta));
                }
            }
        } catch (IllegalStateException e) {
            System.err.println("Error on fetching user with luckperms");
            System.err.println(e.getMessage());
        }
        return metaValue;
    }
}
