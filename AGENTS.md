# ChickensShed — maintenance notes

Server-side mod: configured mobs (default: chickens) drop an item (default: feather) on an egg-style timer.
Stonecutter multiversion repo: the VCS version is the latest Minecraft version, the others come from Stonecutter comments.

## Working rules
- The maintainer decides: discuss before changing direction, don't add scope, commit only when asked.
- Every user-facing change goes into `CHANGELOG.md` under `## <mod.version>`; publishing uses that section and fails without it.
- Keep dependencies minimal: nothing bundled/jar-in-jar, no config library, no Fabric API. Compile-only deps are fine.
- Must keep working with vanilla clients: client-optional in all loader metadata, client code only in `client/`.
- `README.md` is text only (no logo, no badges).

## Layout
- Versions: `settings.gradle.kts`; per-version properties and dependency versions: `stonecutter.properties.toml`.
- Build: `build.{fabric,neoforge,forge}.gradle.kts` per loader (Forge = ModDevGradle legacy, 1.20.1 only); `stonecutter.gradle.kts` holds `mergedJars`.
- Code (`io.github.dns54.chickensshed`):
  - `config/ShedConfig` — Gson load/save/validation of `config/chickensshed.json`.
  - `Shedding` — server-side logic; `Shedder` — duck interface for the timer.
  - `mixin/LivingEntityMixin` — timer field, tick hook, NBT; `mixin/SupplementariesMixin` — jar compat; `mixin/ItemStackMixin` — client-only, hides glint on config icons; `MixinPlugin` — skips the Supplementaries mixin when the mod is absent.
  - `client/` — `ConfigScreen` (mob list) → `MobEditScreen`; `ShedList` and `ShedScreen` hold the version-specific list/drawing code; loader hooks (Mod Menu, NeoForge, Forge).
- Test: `ShedConfigTest` runs on the Fabric projects only ((Neo)Forge test task disabled: MDG test classpath lacks MC libs).

## Behaviour to keep
- One common code path: a single mixin set on all loaders, no per-loader event code.
- Drops are defined in the config only (no datapacks, loot tables, datagen).
- Timer: egg-style countdown per mob, default 900–1500 s; int `ChickensShedTime` in entity NBT, not written when 0. Mobs removed from the config reset to 0. Config changes don't clamp running countdowns.
- `babies: false` pauses the countdown until the mob grows up.
- Honours the mob-loot game rule. No sound or particles. Drops spawn at the mob's position (also inside Supplementaries jars).
- Config: invalid entries are logged and skipped, never reset to defaults; a file that doesn't fully load is copied to `chickensshed.json.bak` first, because saving from the screen writes only what loaded. The old TOML config is ignored (no migration).
- GUI: built from vanilla widgets inside a vanilla list so each version looks native. No Cancel; every valid change saves instantly; Undo steps back through the visit. Icons draw the item's own model (model-only stack on 26.x, real stacks crash before a world loads).

## Version-specific notes
- Entity save API: `CompoundTag` before 1.21.6, `ValueInput`/`ValueOutput` from 1.21.6.
- Mob-loot game rule: `doMobLoot` before 1.21.11, `gamerules.GameRules.MOB_DROPS` + `get()` from 1.21.11.
- 26.x switches screens via `minecraft.gui.setScreen` and draws via `extractRenderState`.
- Forge 1.20.1 needs the `MixinConfigs` manifest attribute, a refmap injected into the mixins JSON, `mixin { config(...) }` for dev runs and a Forge-only `pack.mcmeta`. "Refmap could not be read" in dev is expected.
- Supplementaries exists for 1.20.1 and 1.21.1 only; the Stonecutter constant `supplementaries` gates the compat body. The mixin targets `containerTick` of `CapturedMobInstance` and `DataCapturedMobInstance` and reaches the mob via `getEntityForRenderer()`.
- Compile deps come from Modrinth Maven by version id, except where a project marks its sources jar as primary (then use the version number).

## Adding a Minecraft version
1. `settings.gradle.kts`: add the version/loaders.
2. `stonecutter.properties.toml`: add its block (deps, `mod.mc_releases`).
3. `.github/workflows/publish.yml`: add it to the matrix (must match `settings.gradle.kts`).
4. Fix the Stonecutter branches that break (save API, game rule, screens, `ShedList`).
5. Build, test server and client, update `CHANGELOG.md`.

## Testing
- `./gradlew build` builds all versions and runs `ShedConfigTest`.
- Server: short interval in `run/config/chickensshed.json`, then check drops, count, game rule off, baby pause, invalid entries skipped, timer saved and loaded. Force-load the 3×3 chunks around the test mob: a mob that walks out of loaded chunks pauses its timer.
- Supplementaries: dev runs with `-PwithSupplementaries`. Fabric unpacks Moonlight's nested jars for dev runs; on `AbstractMethodError`, clear their entries in `.gradle/loom-cache/remapped_mods`.
- Client: open the config screen on each version, from the main menu and in a world.

## Releasing
- `./gradlew build mergedJars` → `build/libs/chickensshed-<version>+<mc>.jar`, one jar per Minecraft version holding all its loaders (Forgix `mergeLoaders`, shrunk by the Machete fork).
- Bump `mod.version`, add its `CHANGELOG.md` section, then run the Publish workflow (Modrinth + CurseForge; needs the `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` secrets). Versions already on Modrinth are skipped.
- The 1.20.1 release is tagged 1.20.1 only (Forge 47).
- Rejected: one jar for all versions (Forgix `mergeVersions` crashes Forge 1.20.1 and NeoForge 1.21.11+, and bundles extra libraries).
