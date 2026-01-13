package dev.ftb.mods.ftbessentials.integration;

import dev.ftb.mods.ftblibrary.integration.permissions.PermissionProvider;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

public class LuckPermsIntegration implements PermissionProvider {
    @Override
    public int getIntegerPermission(ServerPlayer player, String nodeName, int def) {
        return Math.max(getMetaData(player.getUUID(), nodeName).map(Integer::parseInt).orElse(def), 0);
    }

    @Override
    public boolean getBooleanPermission(ServerPlayer player, String nodeName, boolean def) {
        return getMetaData(player.getUUID(), nodeName).map(Boolean::parseBoolean).orElse(def);
    }

    @Override
    public String getStringPermission(ServerPlayer player, String nodeName, String def) {
        return getMetaData(player.getUUID(), nodeName).orElse(def);
    }

    @Override
    public String getName() {
        return "Luckperms";
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
