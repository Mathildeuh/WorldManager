# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

WorldManager is a **Paper-only** server plugin (Java 21) for creating, loading, unloading, deleting, backing
up, restoring, teleporting between, and pre-generating Minecraft worlds, with a chest-inventory-based in-game
menu, per-world flags (PvP/mob-spawning/animal-spawning/weather-lock/custom spawn), and a family of optional
per-world-group and per-region features: granular per-group player profiles (inventory/health/hunger/
experience/bed-spawn/last-location, chosen independently per group), linked Nether/End portals (with optional
custom coordinate scaling), sign-based teleport portals, and named custom portal regions (arbitrary
destination, permission gating, an optional Vault price, and launch/cannon velocity) — together covering the
same ground as Multiverse-Core plus its Inventories/NetherPortals/SignPortals/Portals addons, in one plugin.
Distributed on Hangar, Modrinth, CurseForge, and SpigotMC (SpigotMC/legacy Spigot builds are not supported —
the 3.0.0 GUI rewrite dropped the shaded chest-GUI library it depended on, and that split hasn't been
revisited even though the 4.0.0 menu is chest-based again).

## Build & run

- Build a shaded plugin jar: `./gradlew shadowJar` (or `./gradlew build`). This uses `build.gradle`, which
  targets Paper API `1.20.1-R0.1-SNAPSHOT` (deliberately the *oldest* supported version, not the latest —
  Paper's Bukkit-facing API is backward-additive, so compiling against the floor gives the widest real
  compatibility range, advertised as 1.20.1 → 26.2 and boot-verified against a real 1.20.1 server) and, as a
  `doLast` step, **copies the built jar into `server/plugins/`** — a local Paper server checkout used for
  manual testing (gitignored).
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
order: default config → PlaceholderAPI hook → lang file → bStats metrics → the `worldmanager` command → event
listeners (`JoinListener`, `WorldChangeListener`, `PortalLinkListener`, `WorldFlagsListener`,
`SignPortalListener`, `CustomPortalListener`, `GuiListener`) → backup/worlds config files → database →
linked-worlds/linked-portals/sign-portals/custom-portals config → Vault economy hook → update checker.
Plugin-wide singletons (`langConfig`, `backupConfig`, `worldsConfig`, `configFile`) are exposed as **static
fields on `WorldManager`** and referenced directly from anywhere in the codebase instead of being
dependency-injected — follow this pattern for new global state.

**Command dispatch**: A single `worldmanager` command (alias `wm`) is registered in `plugin.yml`. All routing
happens in `commands/WorldManagerCommand.java` via a manual `switch` on `args[0]` (including short aliases like
`c`/`del`/`l`/`u`/`tp`), with a per-subcommand permission check (`worldmanager.<name>`) before dispatch. Each
subcommand is its own class in `commands/subcommands/` (`Create`, `Delete`, `Load`, `Unload`, `Backup`,
`Restore`, `Teleport`, `Lists`, `Gui`, `SignPortalCommand`, `PortalCommand`), instantiated with the
`CommandSender` and invoked via an `execute(...)` method. Tab completion is hand-rolled in the same class,
branching on `args.length` and the subcommand name. World pre-generation is its own nested sub-area under
`commands/subcommands/pregenerator/` (`Pregen`, `ChunkGenerator`), since it's stateful/async
(start/stop/pause/resume) rather than a one-shot action. `PortalCommand` handles every `/wm portal <action>`
(`pos1`/`pos2`/`create`/`remove`/`destination`/`permission`/`price`/`launch`/`list`) in one class with a
private per-action method, mirroring how `Pregen` fans out its own sub-actions.

**Configuration is split across several YAML files**, each with its own manager class under `configs/`:
- `config.yml` — main plugin config (Bukkit's standard `getConfig()`), holds `lang`, `database.*`,
  `enable-linked-inventory`, `linked-worlds-inventory.<group>` (legacy flat world list, or a `worlds:`+
  `share:` section - see the linked-worlds feature below), and `linked-portals.<world>` (`nether`/`end`/
  `nether-scale`).
- `worlds.yml` (`WorldsConfig`) — persisted metadata per created/loaded world: `type`, `environment`,
  `generator`, `createdBy`, `alias`, `spawn`, per-world `flags.*`, and per-world `gameRules`; worlds are
  recreated from this file on every startup.
- `signportals.yml` (`SignPortalsManager`) — one entry per sign portal, keyed by a random id: `world`/`x`/`y`/
  `z` of the sign block, `destination` world name, `createdBy`.
- `portals.yml` (`CustomPortalsManager`) — one entry per custom portal region, keyed by its user-chosen unique
  name: `world`, `pos1`/`pos2` corners, `createdBy`, and the independently-settable `destination`,
  `permission`, `price`, `launch` fields.
- `backups.yml` under `backups/WorldManager/` (`BackupConfig`) — backup registry, separate from `worlds.yml`.
- `lang/<locale>.yml` (`LangConfig`) — one file per supported locale (`en`, `fr`, `es`, `de`, `pl`, `ru`);
  messages use MiniMessage markup with positional placeholders (`{0}`, `{1}`, ...) substituted by
  `LangConfig.formatMessage`. Add new user-facing strings to **every** lang file, not just `en.yml`.

**Messaging**: never build/send `Component`s ad hoc. Look up text via `WorldManager.langConfig.sendError/
sendSuccess/sendWaiting(sender, "path.to.key", args...)`, which resolves the MiniMessage string from the
active lang file, wraps it with a status icon + "World Manager" prefix (`messages/MessageUtils.java`), and
sends it via `CommandSender#sendMessage(Component)` directly — Paper's `CommandSender` (`Player`,
`ConsoleCommandSender`, ...) implements Adventure's `Audience` natively, so there is no
`adventure-platform-bukkit`/`BukkitAudiences` bridge to route through (deliberately removed; see
`messages/MessageUtils.java`'s doc comment for why going through it can silently break behind a
signed-chat-enforcing proxy).

**Database layer** (`database/`) backs the *linked-worlds profile* feature only (not world data itself).
`DatabaseConnection` is an abstract base with `SQLiteConnection` and `MySQLConnection` implementations;
`DatabaseFactory.createConnection()` picks one based on `config.yml`'s `database.type`. `DatabaseManager`
serializes a `configs/PlayerInventoryManager.ProfileData` to a YAML blob stored as a BLOB column, keyed by
`(player_uuid, group_name)`. The `health`/`food_level`/`saturation`/`held_item_slot` SQL columns predate the
per-aspect "share" system and are kept as-is to avoid a schema migration; every other aspect (inventory
contents, level/exp, bed spawn, last location, and the authoritative `captured` set recording which aspects a
given row actually holds real data for - see `ProfileType`) lives inside the YAML blob itself, which was
already schema-free enough to grow without a migration. Extend the blob, not the SQL schema, for any future
per-profile field.

**Linked-worlds profile feature** (granular per-aspect sharing): `LinkedWorldsManager` loads
`linked-worlds-inventory` groups from `config.yml` into in-memory maps
(world→group) plus, per group, a `Set<ProfileType>` of exactly which aspects it shares
(`inventory`/`health`/`hunger`/`experience`/`bed-spawn`/`location`) via `getShares(groupName)`. Two config
shapes are both accepted (detected via `FileConfiguration#isList` on the group's value): a legacy flat list of
world names (still shares inventory/health/hunger, matching every version before per-aspect sharing existed,
via `DEFAULT_SHARES`), or a `worlds:` + `share:` section picking aspects explicitly - never assume only one shape
exists in a given install. `events/WorldChangeListener` reacts to three events: `PlayerTeleportEvent` (fires
*before* a cross-world move completes, with the real pre-move `Location`) captures the player's exact last
position into that group's profile when `location` is shared - `PlayerChangedWorldEvent` fires too late for
this, it only exposes the prior `World`, not coordinates; `PlayerChangedWorldEvent` does the actual
save-outgoing/restore-or-reset-incoming work via `configs/PlayerInventoryManager` (backed by
`DatabaseManager`), restoring `location` (if shared and on record) via a follow-up `teleportAsync` once the
world-change teleport has settled; `PlayerQuitEvent` saves the current group's profile one last time. If
source and destination worlds share a group, nothing is touched; if **neither** world is in a configured
group, nothing is touched either (this is load-bearing - a bug here in 2.2.0–2.2.1 wiped inventories on every
world change between two ungrouped worlds, regardless of whether the feature was even enabled; see
`AUDIT.md`). The `enable-linked-inventory` config check **must default to `false`** everywhere it's read
(`WorldChangeListener.isFeatureEnabled()`, `LinkedWordsManager.loadLinkedWorlds()`) — this is an opt-in
feature, never fail-open. `PlayerInventoryManager.clearPlayerData` (called on quit) only ever drops the
in-memory cache for that player, **never** touches the database — the whole point of granular per-group
profiles is that they persist across sessions; don't reintroduce the 2.2.0-era bug of wiping DB rows on quit.

**Linked portals feature** (new in 3.0.0, custom Nether coordinate scaling added in 4.0.0):
`configs/LinkedPortalsManager` loads the `linked-portals` config section (overworld name → `{nether, end,
nether-scale}`) into bidirectional lookup maps plus a per-link Nether scale (default `8.0`, vanilla's own
ratio). `events/PortalLinkListener` handles `PlayerPortalEvent` for `NETHER_PORTAL`/`END_PORTAL`/
`END_GATEWAY` causes. For End travel it takes vanilla's own already-computed `getTo()` location and just
swaps out the target `World`, instead of re-implementing End platform placement - vanilla never derives End
coordinates from the player's position (entering always lands on the fixed exit platform, returning always
goes back to the player's last overworld position), so there's nothing to scale. For Nether travel, when a
link's `nether-scale` is left at the vanilla default `PortalLinkListener` also just swaps the world; only when
it's been configured to something else does `resolveNetherTarget` recompute X/Z from scratch (origin
coordinate × or ÷ scale) with a safe-Y lookup (`World#getHighestBlockYAt`, clamped into the world's height
range) instead of trusting vanilla's always-8:1 math.

**Sign portals** (Multiverse-SignPortals parity, new in 4.0.0): `configs/SignPortalsManager` persists
sign→destination-world bindings keyed by a random id (the block position is the real lookup key, held in an
in-memory `Map<BlockPos, SignPortal>`). `events/SignPortalListener` has two halves: `onSignChange` recognizes
a sign whose first line is exactly `[WorldManager]` and whose second line names a loaded world, requires
`worldmanager.sign.create`, and registers it; `onInteract` (right-click) requires `worldmanager.sign.use`
(granted to everyone by default) and teleports to the destination world's stored spawn (falling back to its
vanilla spawn, same resolution `Teleport` uses) via `WorldsConfig.getSpawn`. `onBreak` un-registers a portal
whose sign is broken. Deliberately uses the deprecated `String`-based `SignChangeEvent`/`Sign` line accessors
(`getLine`/`setLine`) rather than the newer per-side `Component` API - confirmed via Paper's own javadoc that
they're still fully functional through the 26.2 ceiling, and they're the only sign text API that exists at
all on this plugin's 1.20.1 floor.

**Custom portal regions** (Multiverse-Portals parity, new in 4.0.0): `configs/CustomPortalsManager` persists
named cuboid regions (`configs/CustomPortal` - immutable bounds/name/world, but mutable `destination`/
`permission`/`price`/`launch` fields, each set independently via its own `/wm portal` subcommand after the
region is created). Selection is WorldEdit-wand-style: `/wm portal pos1`/`pos2` record the player's current
block position into a **never-persisted** in-memory selection map, and `/wm portal create <name>` consumes
both to build the region. `events/CustomPortalListener` checks containment on `PlayerMoveEvent` (skipped
unless the mover's block position actually changed, since a server fires many same-block move events per tick
from head rotation alone) and, for vehicles, `VehicleMoveEvent` (a player on foot inside a vehicle is handled
only by the vehicle path, never both). A short per-entity cooldown (`COOLDOWN_MS`) after any trigger stops the
destination side of a portal - or an overlapping one - from immediately re-triggering. A portal with neither
`destination` nor `launch` set is inert (created but not yet configured); one with only `launch` set is a
pure velocity "cannon" with no teleport. Pricing goes through `util/EconomyHook`, a **soft dependency on
Vault** (`compileOnly`, resolved via JitPack's `com.github.MilkBowl:VaultAPI`) following the exact same
optional-hook pattern as PlaceholderAPI (`WorldManager.setupPlaceholderAPI`) - `EconomyHook.isEnabled()` is
`false` whenever Vault or a registered economy plugin is absent, and every portal is simply free in that case
regardless of its configured `price`; never make Vault a hard dependency.

**In-game menu** (`guis/`, rewritten in 4.0.0 — replaces a Dialog-API-based menu that only worked on Paper
1.21.7+, incompatible with this plugin's 1.20.1 floor) is a custom chest-inventory GUI framework, no external
library. `WMGui` is the screen interface (`getInventory()` / `onClick(InventoryClickEvent)` /
`onClose(InventoryCloseEvent)`); `GuiManager` tracks which `WMGui` each player has open by UUID; a single
`GuiListener` (registered once in `onEnable()`) cancels every click while any `WMGui` is open and forwards
clicks on the GUI's own inventory to it — there is no per-screen event registration. Most screens are
stateless: a static `open(player, ...)`/private constructor pair that rebuilds the `Inventory` from live
config/world state every time (`MainMenuGui`, `EditorListGui`, `LoaderGui`, `WorldOptionsGui`,
`WorldFlagsGui`, `GameRuleListGui`, `GameRuleEditGui`, `ConfirmGui`) — same
"always rebuild, never mutate a cached instance" convention the pre-3.0.0 chest-GUI system used.
**`CreatorGui` is the deliberate exception**: since a chest inventory has no native multi-field form, it holds
its draft (name/type/seed/generator/empty) as instance state across the several round trips needed to fill it
in, re-rendering the same `Inventory` in place. `GuiUtils` centralizes MiniMessage/lang-key text building, item
construction and pagination math, mirroring `messages/MessageUtils`'s role for chat messages. Manual pagination
(world lists, game rule lists) works by having Prev/Next buttons re-open the same screen with `page±1`.
Every screen calls `GuiUtils.fillBorder(inventory, theme)` with a `GuiUtils.Theme` (`NEUTRAL`/`CREATE`/
`DESTRUCTIVE`/`INFO` - each a base pane color plus an accent, deliberately never red/lime since those are
reserved for the real cancel/confirm buttons) whose border panes get PDC-tagged with the accent color;
`GuiAnimator` (a single global repeating task started in `onEnable`/stopped in `onDisable`, not one task per
screen) sweeps a "chasing sparkle" pattern through every currently-open `WMGui`'s tagged border slots by
reading `GuiManager.openGuis()` each tick. `GuiListener` also plays a click sound on every handled click, and
`GuiManager.open` plays an open/navigation sound - both are single choke points, so don't add per-screen sound
calls for the generic cases.
Text input (world name, seed, generator, an exact game-rule value) goes through `ChatInput` — closes the GUI,
prompts in chat, and captures the player's next `AsyncChatEvent` message (`cancel` aborts), registering itself
as a short-lived `Listener` per prompt and unregistering on confirm/cancel/quit. **This was a virtual anvil
inventory (`AnvilInput`) through several iterations first** - reading typed text back out of a
`Bukkit.createInventory(null, InventoryType.ANVIL, ...)` result slot via `AnvilInventory#getRenameText()`,
then via `PrepareAnvilEvent#getResult()`, then via reading the clicked item directly, all failed in practice:
server-side diagnostic logging confirmed `PrepareAnvilEvent` never fires at all for that virtual inventory on
at least some servers, even though the client-side rename field itself accepts input. If a future change ever
reconsiders an anvil-based input again, verify `PrepareAnvilEvent` actually fires end-to-end in-game first,
not just that it compiles.

**World creation/loading** goes through Bukkit's `WorldCreator` (type/environment/seed/generator), then is
persisted into `worlds.yml` via `WorldManager.addWorld`/`removeWorld` (delegating to `WorldsConfig`) so worlds
survive restarts. `worlds/EmptyWorldGenerator` provides a void/empty chunk generator option. Any world name
that comes from user input (chat, GUI form fields, command args) must be checked with
`util/WorldNameValidator.isValid(name)` before being used to build a `File`/`WorldCreator` — this closes a
path-traversal gap fixed in 3.0.0 (and re-closed in `CreatorGui`'s empty-world path in 4.0.0, which had
regressed it). `util/WorldFolders` centralizes the "scan the world container for folders containing
`level.dat`" logic (previously duplicated three times). `util/WorldOperationLock` is a simple per-world-name
mutex (`tryLock`/`unlock`) that `pregen start`, `backup`, `restore`, `unload`, and `delete` all acquire before
doing real work, so they can't race each other on the same world — acquire it as the very first thing after
validating the world exists, and make sure every exit path (success and failure) releases it exactly once.
`configs/WorldsConfig` also stores/applies per-world flags (`getFlag`/`setFlag`/`applyFlags`) and a custom
spawn point (`getSpawn`/`setSpawn`, consumed by `Teleport`) alongside the existing type/environment/generator/
game-rule metadata — `applyFlags(world)` must be called any time a world transitions from unloaded to loaded
(see its call sites in `WorldsConfig.createWorld` and `Load`).

## Conventions to preserve

- Static-singleton access to plugin-wide config/state via `WorldManager.*` (including `WorldManager.getInstance()`
  for the plugin instance itself) — don't introduce a DI framework.
- All player/console-facing text goes through `LangConfig` + `MessageUtils`/`GuiUtils`, sourced from the lang
  YAMLs, never hardcoded strings — this keeps the 6 shipped translations complete. Add new keys to **all six**
  `lang/*.yml` files in the same pass that introduces them, not just `en.yml`. A quick way to catch a missed
  language: every `lang/*.yml` file must have the exact same set of dotted keys (flatten and diff them).
- A raw, user-typed string (a `ChatInput` result, anything not authored in a lang file) must never be fed
  through the MiniMessage parser (`GuiUtils.mini`/`miniFromLang`) — build a literal `Component.text(...)`
  instead, so a player can't inject MiniMessage tags into a rendered message/item name.
- Permission checks (`worldmanager.<subcommand>`) happen in `WorldManagerCommand` before a subcommand class is
  even constructed, not inside the subcommand itself; every one of them must have a matching `permissions:`
  entry in `plugin.yml`. `hasPermission()` must stay a plain `sender.hasPermission(...)` call — don't OR it
  with `sender.isOp()`, which would make an explicit permission negation from a permissions plugin unenforceable
  for operators.
- Heavy I/O (zip pack/unpack, world-folder deletion, HTTP calls, database reads/writes) must run via
  `util/SchedulerUtil` (`runAsync`/`runGlobal`/`runGlobalDelayed`/`runAtRegion` — thin wrappers over Paper's
  Folia-safe schedulers), hopping back to the main thread only for the specific Bukkit calls that require it
  (`Bukkit.createWorld`, `Bukkit.unloadWorld`, sending messages). Never call `Bukkit.getScheduler()` or use a
  `BukkitRunnable` directly — that bypasses Folia support. Several main-thread-blocking bugs were fixed across
  3.0.0/4.0.0 — don't reintroduce the pattern.
- A long-running or destructive per-world operation (pre-generation, backup, restore, unload, delete) must
  acquire `util/WorldOperationLock` before starting and release it on every exit path — see the "World
  creation/loading" section above.
- Package layout: `commands/subcommands` for command logic, `configs` for YAML-backed managers, `guis` for
  the chest-inventory menu, `database` for the inventory persistence layer, `messages` for chat output
  formatting, `events` for Bukkit listeners, `util` for stateless helpers (`WorldNameValidator`, `WorldFolders`,
  `WorldOperationLock`, `SchedulerUtil`, `UpdateChecker`).
