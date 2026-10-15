# Changelog

## 2.0.0

- Rewrote the mod for Minecraft 1.20.1, 1.21.1, 1.21.11 and 26.3, with one jar per version for all its loaders.
- Mobs now drop on an egg-like timer, every 15–25 minutes by default, instead of a buggy per-tick chance.
- New config `config/chickensshed.json` with an in-game editor and per-mob settings; the old config isn't migrated. Fixed custom mob lists being reset on NeoForge.
- Shedding follows the mob drops game rule.
- Captured mobs keep their shedding countdown when Supplementaries jars and cages reload or release them.
- Invalid fractional or overflowing config numbers are skipped and the original config is backed up.
- Fixed publishing compatibility metadata for Minecraft 1.21 and 1.21.1.
- Fabric API and Forge Config API Port are no longer needed.
