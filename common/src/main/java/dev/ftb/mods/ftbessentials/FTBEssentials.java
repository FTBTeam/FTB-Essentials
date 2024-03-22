package dev.ftb.mods.ftbessentials;

import dev.architectury.platform.Platform;
import dev.architectury.utils.EnvExecutor;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.net.FTBEssentialsNet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public class FTBEssentials {
	public static final String MOD_ID = "ftbessentials";
	public static final Logger LOGGER = LogManager.getLogger("FTB Essentials");

	public static final Style RECORDING_STYLE = Style.EMPTY.applyFormat(ChatFormatting.RED);
	public static final Style STREAMING_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x9146FF));

	private static final String CONFIG_FILE = MOD_ID + ".snbt";
	private static final String[] DEFAULT_CONFIG = {
			"Default config file that will be copied to instance's config/ftbessentials.snbt location",
			"Copy values you wish to override in here",
			"Example:",
			"",
			"{",
			"	misc: {",
			"		enderchest: {",
			"			enabled: false",
			"		}",
			"	}",
			"}",
	};

	public static FTBEssentialsCommon PROXY;

	public static void init() {
		Path configFilePath = Platform.getConfigFolder().resolve(CONFIG_FILE);
		Path defaultConfigFilePath = Platform.getConfigFolder().resolve("../defaultconfigs/ftbessentials-server.snbt");

		FTBEConfig.CONFIG.load(configFilePath, defaultConfigFilePath, () -> DEFAULT_CONFIG);

		FTBEssentialsNet.init();
		FTBEEventHandler.init();

		PROXY = EnvExecutor.getEnvSpecific(() -> FTBEssentialsCommon::new, () -> FTBEssentialsClient::new);
	}
}
