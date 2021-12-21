package dev.ftb.mods.ftbessentials.config;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftblibrary.snbt.config.IntValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;

/**
 * @author LatvianModder
 */
public interface FTBEConfig {
	SNBTConfig CONFIG = SNBTConfig.create(FTBEssentials.MOD_ID).comment("FTB Essentials config file", "If you're a modpack maker, edit defaultconfigs/ftbessentials-server.snbt instead");

	SNBTConfig TELEPORTATION = CONFIG.getGroup("teleportation").comment("Teleportation-related settings");
	// back
	TimedCommandConfig BACK = new TimedCommandConfig(TELEPORTATION, "back", 30)
			.comment("Allows users to return to their previous location after teleporting (or dying)");
	PermissionBasedIntValue MAX_BACK = new PermissionBasedIntValue(
			BACK.config.getInt("max", 10)
					.range(0, Integer.MAX_VALUE),
			"ftbessentials.back.max",
			"Max size of the teleport history. This limits how many times you can use /back"
	);
	// spawn
	TimedCommandConfig SPAWN = new TimedCommandConfig(TELEPORTATION, "spawn", 10);
	// warp
	TimedCommandConfig WARP = new TimedCommandConfig(TELEPORTATION, "warp", 10)
			.comment("Allows admins to create 'warps', which are fixed points in the world that users may teleport to using /warp");
	// home
	TimedCommandConfig HOME = new TimedCommandConfig(TELEPORTATION, "home", 10)
			.comment("Allows users to set 'homes', which they can then freely teleport to by using /home afterwards");
	PermissionBasedIntValue MAX_HOMES = new PermissionBasedIntValue(
			HOME.config.getInt("max", 1)
					.range(0, Integer.MAX_VALUE),
			"ftbessentials.home.max",
			"Max amount of homes a user can have."
	);
	// tpa
	TimedCommandConfig TPA = new TimedCommandConfig(TELEPORTATION, "tpa", 10)
			.comment("Allows players to create requests to teleport to other users on the server,",
					"as well as requesting other players to teleport to them");
	// rtp
	TimedCommandConfig RTP = new TimedCommandConfig(TELEPORTATION, "rtp", 600)
			.comment("Allows players to teleport to a random point in the Wilderness",
					"Note: This currently does not respect Claimed Chunks yet!");
	IntValue RTP_MAX_TRIES = RTP.config.getInt("max_tries", 100).range(1, 1000).comment("Number of tries before /rtp gives up");
	IntValue RTP_MIN_DISTANCE = RTP.config.getInt("min_distance", 1000).range(0, 30000000).comment("/rtp min distance from spawn point");
	IntValue RTP_MAX_DISTANCE = RTP.config.getInt("max_distance", 100000).range(0, 30000000).comment("/rtp max distance from spawn point");
	// tpl
	ToggleableConfig TPL = new ToggleableConfig(TELEPORTATION, "tpl")
			.comment("Allows admins to teleport to the location a user was last seen at");

	ToggleableConfig TPX = new ToggleableConfig(TELEPORTATION, "tpx")
			.comment("Allows admins to teleport to dimension");

	SNBTConfig ADMIN = CONFIG.getGroup("admin").comment("Admin commands for cheating and moderation");
	ToggleableConfig HEAL = new ToggleableConfig(ADMIN, "heal")
			.comment("Allows admins to heal themselves using a command");
	ToggleableConfig FLY = new ToggleableConfig(ADMIN, "fly")
			.comment("Allows admins to toggle flying status using a command, without having to use Creative Mode");
	ToggleableConfig GOD = new ToggleableConfig(ADMIN, "god")
			.comment("Allows admins to toggle invincibility using a command, without having to use Creative Mode");
	ToggleableConfig INVSEE = new ToggleableConfig(ADMIN, "invsee")
			.comment("Allows admins to view other users' inventories using a command");
	ToggleableConfig MUTE = new ToggleableConfig(ADMIN, "mute") // todo: temp mute?
			.comment("Allows admins to restrict players from chatting by using a command to mute (or unmute) them");
	ToggleableConfig HOME_FOR = new ToggleableConfig(ADMIN, "homefor")
			.comment("Allows admins to view and teleport to other users' homes");

	SNBTConfig MISC = CONFIG.getGroup("misc").comment("Miscellaneous features and utilities");
	ToggleableConfig KICKME = new ToggleableConfig(MISC, "kickme")
			.comment("Allows users to kick themselves from the server, for example if they are stuck or desynced");
	ToggleableConfig TRASHCAN = new ToggleableConfig(MISC, "trashcan")
			.comment("Enables usage of a trash can inventory, which can be used to void unneeded items");
	ToggleableConfig REC = new ToggleableConfig(MISC, "rec")
			.comment("Allows users to announce their recording or streaming status to the server by using commands");
	ToggleableConfig HAT = new ToggleableConfig(MISC, "hat")
			.comment("Allows users to set a custom hat as their head item by using a command");
	ToggleableConfig NICK = new ToggleableConfig(MISC, "nick")
			.comment("Allows users to change their display name, as well as admins to change nicknames for other users");
	ToggleableConfig ENDER_CHEST = new ToggleableConfig(MISC, "enderchest")
			.comment("Allows users to access their ender chest, as well as admins to manage other players' ender chests.");
	// TODO leaderboard config

}
