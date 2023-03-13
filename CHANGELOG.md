# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
