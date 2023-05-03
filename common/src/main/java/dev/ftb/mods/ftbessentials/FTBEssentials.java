package dev.ftb.mods.ftbessentials;

import dev.architectury.platform.Platform;
import dev.architectury.utils.EnvExecutor;
import dev.ftb.mods.ftbessentials.net.FTBEssentialsNet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author LatvianModder
 */
public class FTBEssentials {
	public static final String MOD_ID = "ftbessentials";
	public static final Logger LOGGER = LogManager.getLogger("FTB Essentials");

	public static final Style RECORDING_STYLE = Style.EMPTY.applyFormat(ChatFormatting.RED);
	public static final Style STREAMING_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x9146FF));

	public static FTBEssentialsCommon PROXY;
	public static boolean ranksMod;
	public static boolean luckpermsMod;

	public static void init() {
		FTBEssentialsNet.init();
		FTBEEventHandler.init();

		PROXY = EnvExecutor.getEnvSpecific(() -> FTBEssentialsCommon::new, () -> FTBEssentialsClient::new);
		ranksMod = Platform.isModLoaded("ftbranks");
		luckpermsMod = Platform.isModLoaded("luckperms");
	}

}