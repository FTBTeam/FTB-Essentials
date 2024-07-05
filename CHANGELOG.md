# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1802.2.3]

### Added
* Add the multilanguage support. (`assets/ftbessentials/lang/en_us.json`) and make the majority of
  the languages (use the AI to translate)
  * Now the translator can translate the text according to the `en_us.json` file
    and make more languages support.
  * The added language include 
* Player now can enjoy the Localization of the languages.

## [1802.2.2]

### Added
* Disabling a command in the config file (`serverconfig/ftbessentials.snbt`) now entirely prevents registration of the command
  * Allows for better compatibility with other mods which may register the same commands
* Player pitch and yaw is now also saved along with warps, homes, etc. (thanks @MikePrimm)

## [1802.2.1]

### Fixed
* Corrected the required version of the (optional) luckperms dependency

## [1802.2.0]

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
