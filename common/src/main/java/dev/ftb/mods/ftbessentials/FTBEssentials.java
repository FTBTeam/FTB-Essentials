package dev.ftb.mods.ftbessentials;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.integration.FTBRanksIntegration;
import dev.ftb.mods.ftbessentials.integration.LuckPermsIntegration;
import dev.ftb.mods.ftbessentials.net.FTBEssentialsNet;
import dev.ftb.mods.ftblibrary.config.manager.ConfigManager;
import dev.ftb.mods.ftblibrary.integration.permissions.PermissionHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FTBEssentials {
	public static final String MOD_ID = "ftbessentials";
	public static final Logger LOGGER = LogManager.getLogger("FTB Essentials");

	public static final Style RECORDING_STYLE = Style.EMPTY.applyFormat(ChatFormatting.RED);
	public static final Style STREAMING_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x9146FF));

	public static void init() {
		ConfigManager.getInstance().registerServerConfig(FTBEConfig.CONFIG, MOD_ID + "-server", false, FTBEConfig::onChanged);

		FTBEssentialsNet.init();
		FTBEEventHandler.init();

		initPermissions();
	}

	public static Identifier essentialsId(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static void initPermissions() {
		// ftbxmodcompat handles this if it's present
		if (!Platform.isModLoaded("ftbxmodcompat")) {
            if (Platform.isModLoaded("ftbranks")) {
                PermissionHelper.INSTANCE.setProviderImpl(new FTBRanksIntegration());
            } else if (Platform.isModLoaded("luckperms")) {
                PermissionHelper.INSTANCE.setProviderImpl(new LuckPermsIntegration());
            }
        }
    }
}
