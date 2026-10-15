# Changelog

## 2.0.0

- Rewrote the mod for Minecraft 1.20.1, 1.21.1, 1.21.11 and 26.3, with one jar per version for all its loaders.
- Mobs now drop on an egg-like timer, every 15–25 minutes by default, instead of a buggy per-tick chance.
- New config `config/chickensshed.json` with an in-game editor and per-mob settings; the old config isn't migrated. Invalid entries are skipped and the original file is backed up. Fixed custom mob lists being reset on NeoForge.
- Shedding follows the mob drops game rule.
- Mobs in Supplementaries jars and cages keep shedding and keep their countdown.
- Fabric API and Forge Config API Port are no longer needed.
