# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


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
