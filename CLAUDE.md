# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

WorldManager is a **Paper-only** server plugin (Java 21) for creating, loading, unloading, deleting, backing
up, restoring, teleporting between, and pre-generating Minecraft worlds, with a native Dialog-based in-game
menu and two optional per-world-group features: shared inventories and linked Nether/End portals. Distributed
on Hangar, Modrinth, CurseForge, and SpigotMC (SpigotMC/legacy Spigot builds are no longer supported as of
3.0.0 — the GUI is built on Paper's Dialog API, which has no Spigot/CraftBukkit equivalent).

## Build & run

- Build a shaded plugin jar: `./gradlew shadowJar` (or `./gradlew build`). This uses `build.gradle`, which
  targets Paper API `1.21.7-R0.1-SNAPSHOT` (deliberately the *oldest* version with the Dialog API, not the
  latest — Paper's Bukkit-facing API is backward-additive, so compiling against the floor gives the widest
  real compatibility range, advertised as 1.21.7 → 26.2) and, as a `doLast` step, **copies the built jar into
  `server/plugins/`** — a local Paper server checkout used for manual testing (gitignored).
- `server/` is a full local Paper server install (jar, configs, world data, `plugins/`). Start it manually to
  test the plugin in-game; there is no scripted launch task.
- CI/releases use a *separate* build file, `alt.gradle` (`./gradlew -b alt.gradle shadowJar`), which outputs to
  `./out` instead of copying into `server/plugins`. Keep `build.gradle` and `alt.gradle` dependency/version
  blocks in sync when changing dependencies — they are not shared.
- No test suite exists in this repository.
- Plugin version lives in `build.gradle` (`version = '...'`) and is interpolated into
  `src/main/resources/plugin.yml` via `processResources` (`${version}` placeholder). Bump it there.
- GitHub Actions (`.github/workflows/gradle.yml`) builds with `alt.gradle` on push/PR to `master` and cuts a
  GitHub release tagged with the extracted version.

## Architecture

**Entry point**: `fr.mathildeuh.worldmanager.WorldManager` (`JavaPlugin`). `onEnable()` wires everything up in
order: default config → PlaceholderAPI hook → lang file → bStats metrics → Adventure `BukkitAudiences` → the
`worldmanager` command → event listeners → backup/worlds config files → database → linked-worlds config →
update checker → GUI cache warm-up. Plugin-wide singletons (`langConfig`, `backupConfig`, `worldsConfig`,
`configFile`, `adventure`) are exposed as **static fields on `WorldManager`** and referenced directly from
anywhere in the codebase instead of being dependency-injected — follow this pattern for new global state.

**Command dispatch**: A single `worldmanager` command (alias `wm`) is registered in `plugin.yml`. All routing
happens in `commands/WorldManagerCommand.java` via a manual `switch` on `args[0]` (including short aliases like
`c`/`del`/`l`/`u`/`tp`), with a per-subcommand permission check (`worldmanager.<name>`) before dispatch. Each
subcommand is its own class in `commands/subcommands/` (`Create`, `Delete`, `Load`, `Unload`, `Backup`,
`Restore`, `Teleport`, `Lists`, `Gui`), instantiated with the `CommandSender` and invoked via an `execute(...)`
method. Tab completion is hand-rolled in the same class, branching on `args.length` and the subcommand name.
World pre-generation is its own nested sub-area under `commands/subcommands/pregenerator/` (`Pregen`,
`ChunkGenerator`), since it's stateful/async (start/stop/pause/resume) rather than a one-shot action.

**Configuration is split across several YAML files**, each with its own manager class under `configs/`:
- `config.yml` — main plugin config (Bukkit's standard `getConfig()`), holds `lang`, `database.*`,
  `enable-linked-inventory`, and `linked-worlds-inventory.<group>: [worldNames]`.
- `worlds.yml` (`WorldsConfig`) — persisted metadata per created/loaded world: `type`, `environment`,
  `generator`, `createdBy`, and per-world `gameRules`; worlds are recreated from this file on every startup.
- `backups.yml` under `backups/WorldManager/` (`BackupConfig`) — backup registry, separate from `worlds.yml`.
- `lang/<locale>.yml` (`LangConfig`) — one file per supported locale (`en`, `fr`, `es`, `de`, `pl`, `ru`);
  messages use MiniMessage markup with positional placeholders (`{0}`, `{1}`, ...) substituted by
  `LangConfig.formatMessage`. Add new user-facing strings to **every** lang file, not just `en.yml`.

**Messaging**: never build/send `Component`s ad hoc. Look up text via `WorldManager.langConfig.sendError/
sendSuccess/sendWaiting(sender, "path.to.key", args...)`, which resolves the MiniMessage string from the
active lang file, wraps it with a status icon + "World Manager" prefix (`messages/MessageUtils.java`), and
sends it through Adventure (`WorldManager.adventure()`), which supports both players and console.

**Database layer** (`database/`) backs the *linked-worlds inventory* feature only (not world data itself).
`DatabaseConnection` is an abstract base with `SQLiteConnection` and `MySQLConnection` implementations;
`DatabaseFactory.createConnection()` picks one based on `config.yml`'s `database.type`. `DatabaseManager`
serializes player inventories (main/armor/offhand + health/food/saturation) to a YAML blob stored as a BLOB
column, keyed by `(player_uuid, group_name)`.

**Linked-worlds inventory feature**: `LinkedWorldsManager` loads `linked-worlds-inventory` groups from
`config.yml` into in-memory maps (world→group). `events/WorldChangeListener` reacts to
`PlayerChangedWorldEvent`/`PlayerQuitEvent`: if source and destination worlds share a group, the inventory is
left untouched; if **neither** world is in a configured group, the inventory is *also* left untouched (this is
load-bearing — a bug here in 2.2.0–2.2.1 wiped inventories on every world change between two ungrouped worlds,
regardless of whether the feature was even enabled; see `AUDIT.md`); otherwise the outgoing group's inventory
is saved and the incoming group's inventory is restored (or the player is reset to a clean slate) via
`configs/PlayerInventoryManager`, backed by `DatabaseManager`. The `enable-linked-inventory` config check
**must default to `false`** everywhere it's read (`WorldChangeListener.isFeatureEnabled()`,
`LinkedWordsManager.loadLinkedWorlds()`) — this is an opt-in feature, never fail-open.

**Linked portals feature** (new in 3.0.0): `configs/LinkedPortalsManager` loads the `linked-portals` config
section (overworld name → `{nether, end}` world names) into bidirectional lookup maps.
`events/PortalLinkListener` handles `PlayerPortalEvent` for `NETHER_PORTAL`/`END_PORTAL`/`END_GATEWAY` causes;
it takes vanilla's own already-scaled/clamped `getTo()` location and just swaps out the target `World`,
instead of re-implementing nether coordinate scaling or End platform placement itself.

**In-game menu** (`dialogs/`) is built entirely on Paper's Dialog API (`io.papermc.paper.dialog.Dialog` /
`io.papermc.paper.registry.data.dialog.*`), introduced in Paper 1.21.7 and still marked `@ApiStatus.Experimental`
upstream. Dialogs are created **ad-hoc** per open via `Dialog.create(factory -> factory.empty().base(...).type(...))`
and shown with `player.showDialog(dialog)` — there is no bootstrap-time registry registration (that path exists
in the API but requires migrating to `paper-plugin.yml`, which this plugin deliberately does not need). Button
actions use inline `DialogAction.customClick(DialogActionCallback, ClickCallback.Options)` callbacks rather than
a central `PlayerCustomClickEvent` listener — each button owns its own handler, no `Key`-based dispatch table.
One class per screen (`MainMenuDialog`, `CreatorDialog`, `EditorListDialog`, `WorldOptionsDialog`,
`GameRuleListDialog`, `GameRuleEditDialog`, `LoaderDialog`), each with a static `build(...)` factory method that
reconstructs the screen from live config/world state — same "always rebuild, never mutate a cached instance"
convention the old chest-GUI system used. `DialogUtils` centralizes MiniMessage/lang-key text building and
button/callback construction, mirroring `messages/MessageUtils`'s role for chat messages. Manual pagination
(world lists, game rule lists) works by having Prev/Next buttons call back into the same screen's `build(...)`
with `page±1` and re-show the result — there's no native Dialog pagination widget.

**World creation/loading** goes through Bukkit's `WorldCreator` (type/environment/seed/generator), then is
persisted into `worlds.yml` via `WorldManager.addWorld`/`removeWorld` (delegating to `WorldsConfig`) so worlds
survive restarts. `worlds/EmptyWorldGenerator` provides a void/empty chunk generator option. Any world name
that comes from user input (chat, dialog form fields, command args) must be checked with
`util/WorldNameValidator.isValid(name)` before being used to build a `File`/`WorldCreator` — this closes a
path-traversal gap fixed in 3.0.0. `util/WorldFolders` centralizes the "scan the world container for folders
containing `level.dat`" logic (previously duplicated three times).

## Conventions to preserve

- Static-singleton access to plugin-wide config/state via `WorldManager.*` (including `WorldManager.getInstance()`
  for the plugin instance itself) — don't introduce a DI framework.
- All player/console-facing text goes through `LangConfig` + `MessageUtils`/`DialogUtils`, sourced from the lang
  YAMLs, never hardcoded strings — this keeps the 6 shipped translations complete. Add new keys to **all six**
  `lang/*.yml` files in the same pass that introduces them, not just `en.yml`.
- Permission checks (`worldmanager.<subcommand>`) happen in `WorldManagerCommand` before a subcommand class is
  even constructed, not inside the subcommand itself.
- Heavy I/O (zip pack/unpack, world-folder deletion, HTTP calls) must run via
  `Bukkit.getScheduler().runTaskAsynchronously(...)`, hopping back to the main thread only for the specific
  Bukkit calls that require it (`Bukkit.createWorld`, `Bukkit.unloadWorld`, sending messages). Several such
  main-thread-blocking bugs were fixed in 3.0.0 — don't reintroduce the pattern.
- Package layout: `commands/subcommands` for command logic, `configs` for YAML-backed managers, `dialogs` for
  the Dialog-based menu, `database` for the inventory persistence layer, `messages` for chat output formatting,
  `events` for Bukkit listeners, `util` for stateless helpers (`WorldNameValidator`, `WorldFolders`,
  `UpdateChecker`).
