package dev.ftb.mods.ftbessentials.integration;

import com.google.common.base.Suppliers;
import dev.architectury.platform.Platform;

import java.util.function.Supplier;

public class PermissionsHelper {
    private static final Supplier<PermissionsProvider> INSTANCE = Suppliers.memoize(() -> {
                if (Platform.isModLoaded("ftbranks")) {
                    return new FTBRanksIntegration();
                } else if (Platform.isModLoaded("luckperms")) {
                    return new LuckPermsIntegration();
                } else {
                    return new PermissionsProvider() { };
                }
            }
    );

    public static PermissionsProvider getInstance() {
        return INSTANCE.get();
    }
}
