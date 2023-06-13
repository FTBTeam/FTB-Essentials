# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1902.3.2]

### Added
* Added config setting `only_on_death` in the `back` section
  * When true, the `/back` command works only for returning to your last death point
  * Default is false, as before
* Added `dimension_whitelist` and `dimension_blacklist` settings to the config for the `/rtp` command
  * If whitelist is not empty, players *must* be in one of these dimensions to use `/rtp`
  * If blacklist is not empty, players *must not* be in one of these dimensions to use `/rtp`
  * Wildcards are allowed, e.g. `somemod:*` refers to all dimensions registered by the mod `somemod`
  * Default is an empty whitelist, and `minecraft:the_end` in the blacklist (/rtp in the End can break vanilla progression)

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
