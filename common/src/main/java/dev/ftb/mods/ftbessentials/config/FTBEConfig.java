package dev.ftb.mods.ftbessentials.config;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftblibrary.snbt.config.BooleanValue;
import dev.ftb.mods.ftblibrary.snbt.config.IntValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import dev.ftb.mods.ftblibrary.snbt.config.StringListValue;

import java.util.List;

public interface FTBEConfig {
	SNBTConfig CONFIG = SNBTConfig.create(FTBEssentials.MOD_ID).comment("FTB Essentials config file", "If you're a modpack maker, edit defaultconfigs/ftbessentials-server.snbt instead");

	BooleanValue REGISTER_TO_NAMESPACE = CONFIG.addBoolean("register_to_namespace", false)
			.comment("If true, the mod will register its commands to the 'ftbessentials' namespace,",
					"otherwise it will register to the root namespace");

	BooleanValue REGISTER_ALIAS_AS_WELL_AS_NAMESPACE = CONFIG.addBoolean("register_alias_as_well_as_namespace", false)
			.comment("If true, the mod will register its commands to the 'ftbessentials' namespace as well as the root namespace",
					"otherwise it will only register to the root namespace",
					"This setting has no effect if 'register_to_namespace' is false");

	SNBTConfig INTEGRATION = CONFIG.addGroup("integration").comment("Cross-mod integration");

	BooleanValue TEAM_BASES_SPAWN_OVERRIDE = INTEGRATION.addBoolean("team_bases_spawn_override", true)
			.comment("If true, and FTB Team Bases is installed, then the '/spawn' command will instead send players to the lobby");

	SNBTConfig TELEPORTATION = CONFIG.addGroup("teleportation").comment("Teleportation-related settings");

	BooleanValue ADMINS_EXEMPT_DIMENSION_BLACKLISTS = TELEPORTATION.addBoolean("admins_exempt_dimension_blacklists", true)
			.comment("If true, admin-level players (i.e. permission level >= 2) are exempt from",
					"the dimension controls defined in teleportation -> blacklists and",
					"in teleportation -> rtp -> dimension_blacklist/whitelist");

	// back
	TimedCommandConfig BACK = new TimedCommandConfig(TELEPORTATION, "back", 30, 0)
			.comment("Allows users to return to their previous location after teleporting (or dying)");
	PermissionBasedIntValue MAX_BACK = new PermissionBasedIntValue(
			BACK.config.addInt("max", 10)
					.range(0, Integer.MAX_VALUE),
			"ftbessentials.back.max",
			"Max size of the teleport history. This limits how many times you can use /back"
	);
	BooleanValue BACK_ON_DEATH_ONLY = BACK.config.addBoolean("only_on_death", false)
			.comment("Should be the /back command only be used for returning to the last death point?");
	// spawn
	TimedCommandConfig SPAWN = new TimedCommandConfig(TELEPORTATION, "spawn", 10, 0);
	TimedCommandConfig PLAYER_SPAWN = new TimedCommandConfig(TELEPORTATION, "playerspawn", 10, 0);
	// warp
	TimedCommandConfig WARP = new TimedCommandConfig(TELEPORTATION, "warp", 10, 0)
			.comment("Allows admins to create 'warps', which are fixed points in the world that users may teleport to using /warp");
	// home
	TimedCommandConfig HOME = new TimedCommandConfig(TELEPORTATION, "home", 10, 0)
			.comment("Allows users to set 'homes', which they can then freely teleport to by using /home afterwards");
	PermissionBasedIntValue MAX_HOMES = new PermissionBasedIntValue(
			HOME.config.addInt("max", 1)
					.range(0, Integer.MAX_VALUE),
			"ftbessentials.home.max",
			"Max amount of homes a user can have."
	);
	IntValue HOME_MIN_Y = HOME.config.addInt("home_min_y", Integer.MIN_VALUE)
			.comment("The minimum Y value for homes, as set by the /sethome command");
	// tpa
	TimedCommandConfig TPA = new TimedCommandConfig(TELEPORTATION, "tpa", 10, 0)
			.comment("Allows players to create requests to teleport to other users on the server,",
					"as well as requesting other players to teleport to them");
	// rtp
	TimedCommandConfig RTP = new TimedCommandConfig(TELEPORTATION, "rtp", 420, 0)
			.comment("Allows players to teleport to a random point in the Wilderness",
					"Note: This currently does not respect Claimed Chunks yet!");
	IntValue RTP_MAX_TRIES = RTP.config.addInt("max_tries", 100).range(1, 1000).comment("Number of tries before /rtp gives up");
	IntValue RTP_MIN_DISTANCE = RTP.config.addInt("min_distance", 500).range(0, 30000000).comment("/rtp min distance from spawn point");
	IntValue RTP_MAX_DISTANCE = RTP.config.addInt("max_distance", 25000).range(0, 30000000).comment("/rtp max distance from spawn point");
	StringListValue RTP_DIMENSION_WHITELIST = RTP.config.addStringList("dimension_whitelist", List.of())
			.comment("Whitelisted dimension ID's for /rtp (if non-empty, player *must* be in one of these dimensions)",
					"Wildcarded dimensions (e.g. 'somemod:*') are supported");
	StringListValue RTP_DIMENSION_BLACKLIST = RTP.config.addStringList("dimension_blacklist", List.of("minecraft:the_end"))
			.comment("Blacklisted dimension ID's for /rtp (player *must not* be in any of these dimensions)",
					"Wildcarded dimensions (e.g. 'somemod:*') are supported");

	PermissionBasedBooleanValue RTP_MAX_DISTANCE_CUSTOM = new PermissionBasedBooleanValue(
			RTP.config.addBoolean("allow_custom_max_distance", false),
			"ftbessentials.rtp.custom_max",
			"Allow player to specify (only) custom max distance in rtp command"
	);

	PermissionBasedBooleanValue RTP_MIN_DISTANCE_CUSTOM = new PermissionBasedBooleanValue(
			RTP.config.addBoolean("allow_custom_min_max_distance", false),
			"ftbessentials.rtp.custom_min_max",
			"Allow player to specify custom min and max distance in rtp command"
	);
	// tpl
	ToggleableConfig TPL = new ToggleableConfig(TELEPORTATION, "tpl")
			.comment("Allows admins to teleport to the location a user was last seen at");

	ToggleableConfig TPX = new ToggleableConfig(TELEPORTATION, "tpx")
			.comment("Allows admins to teleport to dimension");
	ToggleableConfig JUMP = new ToggleableConfig(TELEPORTATION, "jump")
			.comment("Allows admins to jump (teleport) to the focused block");

	SNBTConfig TELEPORTATION_BLACKLISTS = TELEPORTATION.addGroup("blacklists")
			.comment("Blacklists for all teleport commands",
					"Wildcarded dimensions (e.g. 'somemod:*') are supported");
	StringListValue TELEPORTATION_BLACKLIST_FROM = TELEPORTATION_BLACKLISTS.addStringList("from", List.of())
			.comment("Dimensions players aren't permitted to run teleport commands in.");
	StringListValue TELEPORTATION_BLACKLIST_TO = TELEPORTATION_BLACKLISTS.addStringList("to", List.of())
			.comment("Dimensions players aren't permitted to teleport into.");

	SNBTConfig ADMIN = CONFIG.addGroup("admin").comment("Admin commands for cheating and moderation");
	ToggleableConfig HEAL = new ToggleableConfig(ADMIN, "heal")
			.comment("Allows admins to fully heal (health, food, fire, potion effects) themselves or a player using a command");
	ToggleableConfig FEED = new ToggleableConfig(ADMIN, "feed")
			.comment("Allows admins to fully feed themselves or a player using a command");
	ToggleableConfig EXTINGUISH = new ToggleableConfig(ADMIN, "extinguish")
			.comment("Allows admins to extinguish themselves or a player using a command");
	ToggleableConfig FLY = new ToggleableConfig(ADMIN, "fly")
			.comment("Allows admins to toggle flying status using a command, without having to use Creative Mode");
	ToggleableConfig SPEED = new ToggleableConfig(ADMIN, "speed")
			.comment("Allows admins to change walk speed for themselves or a player");
	ToggleableConfig GOD = new ToggleableConfig(ADMIN, "god")
			.comment("Allows admins to toggle invincibility using a command, without having to use Creative Mode");
	ToggleableConfig INVSEE = new ToggleableConfig(ADMIN, "invsee")
			.comment("Allows admins to view other users' inventories using a command");
	ToggleableConfig MUTE = new ToggleableConfig(ADMIN, "mute")
			.comment("Allows admins to restrict players from chatting by using a command to mute (or unmute) them");
	ToggleableConfig KIT = new ToggleableConfig(ADMIN, "kit")
			.comment("Allows admins to configure kits of items that can be given to players.");
	ToggleableConfig TP_OFFLINE = new ToggleableConfig(ADMIN, "tp_offline")
			.comment("Allows admins to change the location of offline players.");

	SNBTConfig MISC = CONFIG.addGroup("misc").comment("Miscellaneous features and utilities");
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
	ToggleableConfig CRAFTING_TABLE = new ToggleableConfig(MISC, "crafting")
			.comment("Allows users to access a Crafting Table GUI without needing a Crafting Table.");
	ToggleableConfig STONECUTTER = new ToggleableConfig(MISC, "stonecutter")
			.comment("Allows users to access a Stonecutter GUI without needing a Stonecutter.");
	ToggleableConfig ANVIL = new ToggleableConfig(MISC, "anvil")
			.comment("Allows users to access an Anvil GUI without needing an Anvil.");
	ToggleableConfig SMITHING_TABLE = new ToggleableConfig(MISC, "smithing")
			.comment("Allows users to access a Smithing Table GUI without needing a Smithing Table.");
	ToggleableConfig LEADERBOARD = new ToggleableConfig(MISC, "leaderboard")
			.comment("Allows users to view leaderboard stats about everyone on the server.");
	ToggleableConfig NEAR = new ToggleableConfig(MISC, "near")
			.comment("Allows users to list nearby players, sorted by distance");
}

