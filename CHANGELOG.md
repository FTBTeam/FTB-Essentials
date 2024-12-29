# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2101.1.2]

### Fixed
* Fixed a command being usable by non-admin players

## [2101.1.1]

### Added
* Added fr_fr translation (thanks @Nogapra)
* Added tr_tr translation (thanks @RuyaSavascisi)

### Fixed
* Fixed command cooldowns not being sufficiently dynamically calculated
  * E.g. if a player runs a teleport command, and then adds a FTB Ranks node to reduce their cooldown
  * Cooldowns are now recalculated on each command attempt rather than precalculated on a successful run
* Fixed the `/rtp` command sometimes sending players to bad destinations
  * Was particularly an issue in the Nether (and likely other roofed dimensions)

## [2101.1.0]

### Changed
* Minecraft 1.21.1 is now required; this no longer supports Minecraft 1.21

### Added
* Sidebar buttons for this and other FTB mods can now be enabled/disabled/rearranged (new functionality in FTB Library 2101.1.0)

## [2100.1.1]

### Fixed
* Fixed `/enderchest` command opening the Ender Chest GUI on target player's screen instead of executing player

## [2100.1.0]

### Changed
* Ported to Minecraft 1.20.6. Support for Fabric and NeoForge.
  * Forge support may be re-added if/when Architectury adds support for Forge

## [2006.1.0]

### Changed
* Ported to Minecraft 1.20.6. Support for Fabric and NeoForge.
  * Forge support may be re-added if/when Architectury adds support for Forge

## [2004.1.2]

### Changed
* `/tpl` is now `/teleport_last` (Technically this happened last version)
* `/anvil`, `/crafting`, `/smithing`, `/stonecutter` have been moved under the `/open` namespace
  * E.g. `/open anvil` will open an Anvil GUI
* `/listhomes` now allows you to click to teleport to a home (When OP) and has had the output improved.

### Fixed
* `/leaderboard` will now correctly show offline players
* `/leaderboard` will no longer show `#010` :joy:

## [2004.1.1]

### Changed
* Ported to MC 1.20.4. Support for Forge, NeoForge & Fabric.
* Essentials commands can now optionally be registered under the top-level `/ftbessentials` command
  * By default, commands are still registered as their own top-level command, as before
  * See the `register_to_namespace` and `register_alias_as_well_as_namespace` boolean options in the `ftbessentials.snbt` config file
* Server configuration file has moved from `<world>/serverconfig/ftbessentials.snbt` to `<instance>/config/ftbessentials.snbt`
  * This was necessary due to command registration (which happens before server start) needing to know the config

## [2001.2.2]

### Fixed
* Possible fix for a hard-to-reproduce server crash when multiple players are using commands with warmups

## [2001.2.1]

### Added
* The `/rtp` command now makes better use of block and biome tags to control valid RTP destinations
  * `ftbessentials:ignore_rtp` block tag lists blocks which are not valid for the player to land on (leaves and Bedrock by default)
  * `ftbessentials:ignore_rtp` biome tag lists biomes which are not valid for the player to land in (`#minecraft:is_ocean` by default)

### Fixed
* Fixed an event handler running on the client side which shouldn't have been
  * Led to undesirable effects like players flying when they shouldn't

## [2001.2.0]

### Added
* Added Kits!  Kits are configurable collections of items which can be given to players with a single command
  * See https://github.com/FTBTeam/FTB-Essentials/wiki/kits.md for a quick overview of how to manage kits
* Added `/tp_offline` command, allowing admins to change the position of offline players
  * Dimension can also be changed using the standard vanilla `/execute in <dimension> run tp_offline ...` syntax
* Added several commands to open some vanilla work site blocks without needing the block:
  * `/crafting` opens a Crafting Table GUI
  * `/stonecutter` opens a Stonecutter GUI
  * `/smithing` opens a Smithing Table GUI
  * `/anvil` opens an Anvil GUI
* Added a `/jump` command to instantly teleport to the top of the focused block (or block column)
  * E.g. targeting the side of a wall will place you on top of the wall after a `/jump`
* Added a `/speed` command to check or adjust a player's walking speed boost
  * `/speed <boost_pct> [<player>]` can be used to give a player a speed boost modifier, expressed as a percentage
  * e.g. `/speed 50` makes you walk 50% faster
  * Valid ranges are -100 (completely stopped) -> 2000 (stupidly fast)
* Added a `/near` command to list nearby players and their distance from you (or a target player)
* Added a `/feed` command to restore a player's food level (and full saturation)
* Added a `/extinguish` command to extinguish a player who's currently on fire
* Fire a cancellable Architectury event when player is about to teleport due to any Essentials command
  * Can be caught and cancelled by other mods if they want to prevent teleportation under specific circumstances
  * Event is `TeleportEvent.TELEPORT`

### Fixed
* Fixed server crash related to auto-unmuting players who have gone offline
* Fixed pitch and yaw being swapped when teleporting to saved positions (homes, warps)

## [2001.1.2]

### Added
* Updated to MC 1.20.1, based on 1902.3.2 release

## [1902.3.2]

### Fixed
* Fix Trashcan icon missing from sidebar

## [1902.3.1]

### Added
* Fabric support: FTB Essentials is now a cross-platform mod!
* Disabling a command in the config file (`serverconfig/ftbessentials.snbt`) now entirely prevents registration of the command
  * Allows for better compatibility with other mods which may register the same commands
* The /mute command can now take an optional duration, which is a number followed by one of 's','m','h','d' or 'w'
  * E.g. `/mute badguy 10m` or `/mute badguy 1.5h`
  * Mutes with no duration are permanent until reversed with the `/unmute` command, as before

## [1902.3.0]

### Added
* _Skipped_

## [1902.2.0]

### Added
* Added LuckPerms integration, as an alternative to FTB Ranks permission nodes
  * If both FTB Ranks and LuckPerms mods are present, FTB Ranks will be used in preference
* Many teleport commands can now have a configurable warmup time
  * Commands are: `/home`, `/warp`, `/back`, `/spawn`, `/rtp`, `/tpa`
  * Default warmup is 0 seconds, i.e. no warmup as before
  * Warmups can be configured in `ftbessentials.snbt` and/or via FTB Ranks (or LuckPerms) permission nodes
* The player's position is now noted when running `/teleport_last` and the vanilla `/teleport` commands
  * This means `/back` now works with these commands

### Fixed
* Nicknames are now displayed as expected when FTB Ranks is installed
