package dev.ftb.mods.ftbessentials;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.ftb.mods.ftbessentials.net.FTBEssentialsNet;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint.DisplayTest;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
@Mod(FTBEssentials.MOD_ID)
public class FTBEssentials {
	public static final String MOD_ID = "ftbessentials";
	public static final Logger LOGGER = LogManager.getLogger("FTB Essentials");
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().setLenient().create();

	public static FTBEssentialsCommon PROXY;
	public static boolean ranksMod;
	public static boolean luckpermsMod;

	public static List<Pattern> DISALLOWED_DIMENSION_PATTERNS = new ArrayList<>();

	public FTBEssentials() {
		ModLoadingContext.get().registerExtensionPoint(DisplayTest.class, () -> new DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
		FTBEssentialsNet.init();
		PROXY = DistExecutor.safeRunForDist(() -> FTBEssentialsClient::new, () -> FTBEssentialsCommon::new);
		ranksMod = ModList.get().isLoaded("ftbranks");
		luckpermsMod = ModList.get().isLoaded("luckperms");
	}
}