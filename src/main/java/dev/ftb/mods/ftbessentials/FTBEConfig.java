package dev.ftb.mods.ftbessentials;

import dev.ftb.mods.ftblibrary.snbt.config.IntValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import net.minecraft.server.level.ServerPlayer;

/**
 * @author LatvianModder
 */
public interface FTBEConfig {
	class TimerConfig {
		public final String name;
		public final IntValue config;
		public final String permissionNode;

		public TimerConfig(SNBTConfig c, String type, String n, int def) {
			name = n;
			config = c.getInt(type, def).range(0, 604800).comment("/" + name + " " + type + " in seconds");
			permissionNode = "ftbessentials." + name + "." + type;
		}

		public int get(ServerPlayer player) {
			if (FTBEssentials.ranksMod) {
				return FTBRanksIntegration.getInt(player, config.get(), permissionNode);
			}

			return config.get();
		}
	}

	SNBTConfig CONFIG = SNBTConfig.create(FTBEssentials.MOD_ID);

	SNBTConfig COMMANDS = CONFIG.getGroup("commands"); // TODO: Implement warmup configs too

	SNBTConfig BACK = COMMANDS.getGroup("back");
	TimerConfig BACK_COOLDOWN = new TimerConfig(BACK, "cooldown", "back", 30);
	IntValue MAX_BACK = BACK.getInt("max", 10).range(0, Integer.MAX_VALUE).comment("Max number of times you can use /back");

	SNBTConfig SPAWN = COMMANDS.getGroup("spawn");
	TimerConfig SPAWN_COOLDOWN = new TimerConfig(SPAWN, "cooldown", "spawn", 10);

	SNBTConfig WARP = COMMANDS.getGroup("warp");
	TimerConfig WARP_COOLDOWN = new TimerConfig(WARP, "cooldown", "warp", 10);

	SNBTConfig HOME = COMMANDS.getGroup("home");
	TimerConfig HOME_COOLDOWN = new TimerConfig(HOME, "cooldown", "home", 10);
	IntValue MAX_HOMES = HOME.getInt("max", 1).range(0, Integer.MAX_VALUE).comment("Max homes");

	SNBTConfig TPA = COMMANDS.getGroup("tpa");
	TimerConfig TPA_COOLDOWN = new TimerConfig(TPA, "cooldown", "tpa", 10);

	SNBTConfig RTP = COMMANDS.getGroup("rtp");
	TimerConfig RTP_COOLDOWN = new TimerConfig(RTP, "cooldown", "rtp", 600);
	IntValue RTP_MAX_TRIES = RTP.getInt("max_tries", 100).range(1, 1000).comment("Number of tries before /rtp gives up");
	IntValue RTP_MIN_DISTANCE = RTP.getInt("min_distance", 1000).range(0, 30000000).comment("/rtp min distance from spawn point");
	IntValue RTP_MAX_DISTANCE = RTP.getInt("max_distance", 100000).range(0, 30000000).comment("/rtp max distance from spawn point");

	static int getMaxBack(ServerPlayer player) {
		if (FTBEssentials.ranksMod) {
			return FTBRanksIntegration.getInt(player, MAX_BACK.get(), "ftbessentials.back.max");
		}

		return MAX_BACK.get();
	}

	static int getMaxHomes(ServerPlayer player) {
		if (FTBEssentials.ranksMod) {
			return FTBRanksIntegration.getInt(player, MAX_HOMES.get(), "ftbessentials.home.max");
		}

		return MAX_HOMES.get();
	}
}
