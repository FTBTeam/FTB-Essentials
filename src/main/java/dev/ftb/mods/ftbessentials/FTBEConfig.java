package dev.ftb.mods.ftbessentials;

import dev.ftb.mods.ftblibrary.snbt.config.BooleanValue;
import dev.ftb.mods.ftblibrary.snbt.config.IntValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import net.minecraft.server.level.ServerPlayer;

/**
 * @author LatvianModder
 */
public interface FTBEConfig {
	class ToggleableConfig {
		public final String name;
		public final SNBTConfig config;
		public final BooleanValue enabled;

		public ToggleableConfig(SNBTConfig parent, String name) {
			this.name = name;
			config = parent.getGroup(name);
			enabled = config.getBoolean("enabled", true)
					.comment("Controls whether this feature or command is enabled");
		}

		public ToggleableConfig(SNBTConfig parent, String name, String... comment) {
			this(parent, name);
			config.comment(comment);
		}

		public boolean isEnabled() {
			return enabled.get();
		}
	}

	class TimedCommandConfig extends ToggleableConfig {
		public final IntValue cooldown;
		public final String cooldownPermission;
		// TODO: Implement warmup configs too

		public TimedCommandConfig(SNBTConfig parent, String name, int defaultCooldown) {
			super(parent, name);
			cooldownPermission = String.format("ftbessentials.%s.cooldown", name);
			cooldown = config.getInt("cooldown", defaultCooldown)
					.range(0, 604800)
					.comment(
							String.format("Cooldown between /%s commands (in seconds)", name),
							"You can override this with FTB Ranks using " + cooldownPermission
					);

		}

		public int cooldown(ServerPlayer player) {
			if (FTBEssentials.ranksMod) {
				return FTBRanksIntegration.getInt(player, cooldown.get(), cooldownPermission);
			}

			return cooldown.get();
		}
	}

	SNBTConfig CONFIG = SNBTConfig.create(FTBEssentials.MOD_ID);

	SNBTConfig TELEPORTATION = CONFIG.getGroup("teleportation");
	// back
	TimedCommandConfig BACK = new TimedCommandConfig(TELEPORTATION, "back", 30);
	IntValue MAX_BACK = BACK.config.getInt("max", 10).range(0, Integer.MAX_VALUE).comment("Max number of times you can use /back");
	// spawn
	TimedCommandConfig SPAWN = new TimedCommandConfig(TELEPORTATION, "spawn", 10);
	// warp
	TimedCommandConfig WARP = new TimedCommandConfig(TELEPORTATION, "warp", 10);
	// home
	TimedCommandConfig HOME = new TimedCommandConfig(TELEPORTATION, "home", 10);
	IntValue MAX_HOMES = HOME.config.getInt("max", 1).range(0, Integer.MAX_VALUE).comment("Max homes");
	// tpa
	TimedCommandConfig TPA = new TimedCommandConfig(TELEPORTATION, "tpa", 10);
	// rtp
	TimedCommandConfig RTP = new TimedCommandConfig(TELEPORTATION, "rtp", 600);
	IntValue RTP_MAX_TRIES = RTP.config.getInt("max_tries", 100).range(1, 1000).comment("Number of tries before /rtp gives up");
	IntValue RTP_MIN_DISTANCE = RTP.config.getInt("min_distance", 1000).range(0, 30000000).comment("/rtp min distance from spawn point");
	IntValue RTP_MAX_DISTANCE = RTP.config.getInt("max_distance", 100000).range(0, 30000000).comment("/rtp max distance from spawn point");

	SNBTConfig ADMIN = CONFIG.getGroup("admin");
	ToggleableConfig HEAL = new ToggleableConfig(ADMIN, "heal");
	ToggleableConfig FLY = new ToggleableConfig(ADMIN, "fly");
	ToggleableConfig GOD = new ToggleableConfig(ADMIN, "god");
	ToggleableConfig INVSEE = new ToggleableConfig(ADMIN, "invsee");
	ToggleableConfig TPL = new ToggleableConfig(ADMIN, "tpl");
	ToggleableConfig MUTE = new ToggleableConfig(ADMIN, "mute"); // also controls unmute

	SNBTConfig MISC = CONFIG.getGroup("misc");
	ToggleableConfig KICKME = new ToggleableConfig(MISC, "kickme");
	ToggleableConfig TRASHCAN = new ToggleableConfig(MISC, "trashcan");
	ToggleableConfig REC = new ToggleableConfig(MISC, "rec"); // also controls streaming
	ToggleableConfig HAT = new ToggleableConfig(MISC, "hat");
	ToggleableConfig NICK = new ToggleableConfig(MISC, "nick"); // also controls nicknamefor
	// TODO leaderboard config

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
