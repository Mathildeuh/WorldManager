# WorldManager 4.0.0 — Audit

This document covers the full review and rewrite performed for the 4.0.0 release: a ground-up
audit of the entire codebase, a GUI rewrite (back to a chest inventory, this time built to run
from Paper 1.20.1), every bug found and fixed, a new per-world-flags feature, and full parity with
Multiverse's Inventories/NetherPortals/SignPortals/Portals addons (granular per-group player
profiles, custom Nether portal scaling, sign portals, and custom portal regions). The 3.0.0 audit
below this section is kept as-is for history.

## Methodology

Three parallel passes covered the whole codebase: the Dialog-based `dialogs/` package and
`messages/`, every subcommand plus `commands/WorldManagerCommand`'s dispatch/tab-completion, and
`configs/`/`database/`/`events/`/`plugin.yml`/the Gradle build files. Every finding was verified
against the actual file before being called a bug, and every version-sensitive API decision (the
text-input mechanism's actual runtime behavior, `ChunkGenerator`'s legacy generation methods,
`GameRule.getByName`, `SignChangeEvent`'s deprecated line accessors) was checked against real
Paper Javadoc for both the 1.20.1 floor and the 26.2 ceiling — not assumed from memory, and in one
case (see "Text input" below) not trusted even after confirming it compiled.

## Critical fix: linked-inventory history wiped on every disconnect

`events/WorldChangeListener.onPlayerQuit` correctly saved the player's current group's inventory
on quit, then called `PlayerInventoryManager.clearPlayerData(uuid)` — which, despite its name
suggesting a memory-cache clear, also called `DatabaseManager.deleteAllPlayerInventories(uuid)`,
deleting **every** linked-inventory row for that player from the database, across **every**
group, not just the one just saved. On a server using `enable-linked-inventory` with more than
one group, a player's inventory in any group other than the one they most recently occupied was
permanently lost the moment they disconnected — directly contradicting the point of the feature
(inventories that persist across sessions, not just across a single online session).

**Fix**: `clearPlayerData` now only removes the in-memory cache entry
(`configs/PlayerInventoryManager.java`); the database is never touched on quit.
`DatabaseManager.deleteAllPlayerInventories` (now unreferenced) was removed. Inventory
saves/loads were also moved off the main thread (`SchedulerUtil.runAsync`) while this code was
already open, since a linked-worlds server calls `saveInventory` on every relevant world change.

## Version / compatibility strategy

- Compiled against `paper-api:1.20.1-R0.1-SNAPSHOT` — the plugin's new floor — rather than the
  newest, for the same reason as 3.0.0: Paper's Bukkit-facing API is backward-additive, so
  building against the floor is the only way to honestly advertise a "1.20.1 → 26.2" range with
  one jar.
- This is a **lower** floor than 3.0.0's 1.21.7, specifically to undo the compatibility loss that
  release caused: the Dialog API used for the whole 3.0.0 GUI simply doesn't exist before Paper
  1.21.7, so the plugin could not load at all on 1.20.x–1.21.6. Moving the GUI back to a chest
  inventory removes that constraint entirely.
- Verified end-to-end beyond just compiling: downloaded a real Paper 1.20.1 server build (build
  196, the last one published for that version) and boot-tested the actual built jar against it
  twice (once after the GUI rewrite + version floor change, once again after the per-world-flags
  feature) — both times the server reached `Done` with the plugin fully enabled and zero
  exceptions, and shut down cleanly afterward. See Verification below.
- `EmptyWorldGenerator`'s use of the legacy `ChunkGenerator.generateChunkData`/`createChunkData`
  methods was flagged as a version-compatibility risk during the commands audit (they're
  deprecated). Checked against Paper's 26.2 Javadoc: still present, not removed, just deprecated
  in favor of a newer split-up generation API — no change needed.

## GUI rewrite: back to a chest inventory, redesigned

The `dialogs/` package (9 files, built on `io.papermc.paper.dialog.Dialog`) is gone, replaced by
`guis/` (`fr.mathildeuh.worldmanager.guis`) — a small custom chest-inventory GUI framework, no
external dependency:

- `WMGui` is the per-screen contract (`getInventory()` / `onClick` / `onClose`); `GuiManager`
  tracks the one `WMGui` each player has open; a single `GuiListener` cancels all inventory
  interaction while a `WMGui` is open and routes clicks on the GUI's own inventory to it — no
  per-screen event registration, matching the "each screen owns its handling" spirit of the old
  Dialog callbacks.
- Every screen except one is stateless (a static `open(...)` that rebuilds the `Inventory` from
  live state each time): `MainMenuGui`, `EditorListGui`, `LoaderGui`, `WorldOptionsGui`,
  `WorldFlagsGui`, `GameRuleCategoryGui`, `GameRuleListGui`, `GameRuleEditGui`, `ConfirmGui`.
  **`CreatorGui`** is the deliberate exception: a chest inventory has no native multi-field form,
  so it holds its draft (name/type/seed/generator/empty) as instance state across the several
  round trips needed to fill it in.
- **Text input** (world name, seed, generator, an exact game-rule value) goes through `ChatInput`:
  closes the menu, prompts in chat, and captures the player's next chat message (`cancel` aborts),
  registering itself as a short-lived `Listener` per prompt and unregistering on confirm/cancel/
  quit. **This took three attempts to get right, and none of them involved chat at first.** The
  original design used a virtual anvil inventory (`AnvilInput`) reading typed text via
  `AnvilInventory#getRenameText()` — deliberately the plain, pre-`AnvilView` methods, confirmed
  present all the way through 26.2 via Paper's own Javadoc. That never reflected what the player
  actually typed, so a second attempt read the value from `PrepareAnvilEvent#getResult()` instead;
  when that also never fired, a third attempt read the clicked result item directly at click time.
  All three failed for the same underlying reason, root-caused with an unconditional diagnostic
  listener across two separate real in-game tests: `PrepareAnvilEvent` **never fires at all** for a
  virtual, non-block-backed anvil inventory (`Bukkit.createInventory(null, InventoryType.ANVIL,
  ...)`) on this server, even though the client-side rename text field itself visibly accepts
  input and the API used to read it is valid, documented, and compiles cleanly. This is the
  clearest example in this audit of compiling successfully proving nothing about runtime behavior
  — the fix was to abandon anvils entirely and capture text via `AsyncChatEvent` instead, which
  the project owner confirmed working in-game.
- **Confirmations** use a generic reusable `ConfirmGui` (green/red concrete Yes/No) instead of a
  bespoke screen per destructive action.
- **Integer game rules** are edited with immediate-apply ±1/±10 buttons plus a "set exact value"
  button that opens an `AnvilInput`, instead of the old slider-then-confirm flow — a small UX
  improvement (immediate feedback) that also happens to be simpler to build on a chest inventory.
- **Visual design**: every screen fills unused slots with a light-gray glass-pane border,
  outcome-colored action items (green = create/confirm, red = cancel/destructive), and a
  representative `Material` icon per world type/action (grass block for a normal world,
  netherrack for Nether, end stone for The End, a compass for "set spawn", etc.) instead of the
  old Dialog screens, which had no icon support at all (`ActionButton` has no item-icon slot in
  that API).
- Found and fixed while porting: `CreatorDialog`'s "empty world" creation path never called
  `WorldNameValidator.isValid(name)` before building a `WorldCreator` directly — a path-traversal
  gap that the regular (non-empty) creation path already guarded against via `Create.execute`.
  `CreatorGui.submit()` now validates the name the same way on both paths.

## Bugs found and fixed

| # | File | Bug | Fix |
|---|---|---|---|
| 1 | `configs/PlayerInventoryManager` / `events/WorldChangeListener` | Player disconnect wiped *all* linked-inventory groups from the database, not just the current one (see Critical fix above) | `clearPlayerData` now only clears the memory cache |
| 2 | `commands/WorldManagerCommand.hasPermission` | `sender.hasPermission(permission) \|\| sender.isOp()` meant an operator's explicit permission negation (set by a permissions plugin) could never take effect | Removed the `isOp()` fallback; `plugin.yml` now declares every `worldmanager.*` permission with `default: op`, which already grants operators access without the redundant/unsafe OR |
| 3 | `commands/subcommands/pregenerator/Pregen` + `Delete`/`Unload`/`BackupConfig` | Pre-generation could keep writing chunks into a world folder that `delete`/`unload`/`backup`/`restore` was simultaneously acting on — no shared lock between them | New `util/WorldOperationLock` (a `tryLock`/`unlock` per-world-name mutex), acquired by all five operations before doing real work |
| 4 | `commands/subcommands/pregenerator/ChunkGenerator.stop` | `generatedChunkCount / Math.max(elapsedTime, 1) * 1000.0` divided as integers before the `* 1000.0`, so the logged chunks/s was `0` whenever `elapsedTime > generatedChunkCount` (almost always) | Cast to `double` before dividing |
| 5 | `commands/subcommands/pregenerator/Pregen`/`ChunkGenerator` | `radius:n` was actually a *total chunk count*, not a radius — `radius:64` generated an ~8×8 square, not a radius-64 one | `ChunkGenerator` now takes a real chunk radius and computes a `(2n+1)²` square from it |
| 6 | `commands/WorldManagerCommand` | `backup`/`restore`/`delete`/`unload` with a missing world argument showed a *permission* error even when the sender had permission, because the args-length and permission checks were combined in one `&&` | Split into permission-check-first, then a proper `<cmd>.usage` message on missing args |
| 7 | `commands/WorldManagerCommand#onTabComplete` | `pregen`'s 4th+ argument suggested online player names (leftover/wrong — `pregen` never takes a player argument); `create`'s 3rd argument (world type) had no completion at all, unlike `load` | Fixed to suggest `center:`/`radius:` tokens for pregen, and the actual accepted type list for `create` |
| 8 | `commands/subcommands/Create.createWorld` | `catch (Exception ignored) {}` around the post-creation `world.save()` — a real save failure was silently swallowed while the command still reported success | Logged at `WARNING` |
| 9 | `configs/BackupConfig.restoreWorld` | `World.Environment.valueOf(config.getString(...))` would throw an unclear `NullPointerException` if a backup zip existed but its `backups.yml` metadata entry didn't | Explicit null-check with a new `restore.missing_metadata` message before attempting to parse |
| 10 | `build.gradle` | `shadowJar`'s `doLast` copied the built jar to a Windows path hardcoded to one developer's home directory — broken on any other machine/OS/CI | Now `layout.projectDirectory.dir("server/plugins")` |
| 11 | `util/UpdateChecker.getVersion` | Used the deprecated `new URL(...).openConnection()` | `URI.create(...).toURL().openConnection()` |
| 12 | `WorldManager.loadLangFile` | `defaultLangs` fallback list omitted `"pl"` despite `lang/pl.yml` existing and being a documented supported locale | Added |
| 13 | `commands/subcommands/Create` (empty-world path, via `guis/CreatorGui`) | Missing `WorldNameValidator` check on the empty-world creation path (see GUI rewrite section above) | Added |
| 14 | Database schema (`SQLiteConnection`/`MySQLConnection`/`DatabaseManager`) | A player's held hotbar slot wasn't persisted, silently resetting to slot 0 after a server restart | New `held_item_slot` column, with a defensive `ALTER TABLE ... ADD COLUMN` migration for existing databases |
| 15 | `guis/AnvilInput` (removed) | `PrepareAnvilEvent` never fires for a virtual, non-block-backed anvil inventory on this server, despite using valid, documented, compiling API (see "Text input" above) | Replaced entirely with `ChatInput` (chat-message-based text capture), confirmed working in-game |
| 16 | `lang/*.yml` (`gui.confirm.yes`/`gui.confirm.no`) | Both keys were written as unquoted YAML mapping keys (`yes:`/`no:`); YAML 1.1's implicit-typing resolver coerces unquoted `yes`/`no`/`true`/`false`/`on`/`off` to booleans even as a *key*, so the real parsed path became `gui.confirm.true`/`gui.confirm.false` — silently different from what `ConfirmGui.java` actually looks up. Caught via a live "Missing GUI message key" warning, not by the lang-parity script, which uses the same parser and has the identical blind spot across all 6 languages | Quoted (`"yes":`/`"no":`) in all 6 `lang/*.yml` files; the on-disk upgrade-merge mechanism (`WorldManager.mergeMissingLangKeys`) patches existing installs automatically on next restart |

## New feature: per-world flags

`configs/WorldsConfig` gained `getFlag`/`setFlag`/`applyFlags`, storing named boolean flags per
world in `worlds.yml` (`worlds.<name>.flags.<flag>`) in the spirit of Multiverse-Core's per-world
properties: `pvp` (`World#setPVP`), `mobSpawning`/`animalSpawning` (`World#setSpawnFlags`), and
`weatherLocked` (forces clear weather and cancels any `WeatherChangeEvent` trying to start a
storm, via the new `events/WorldFlagsListener`). A custom per-world spawn point
(`getSpawn`/`setSpawn`) is also stored there and consumed by `Teleport`, falling back to the
world's vanilla spawn when unset. All of it is reachable from a new `WorldFlagsGui` screen off
each world's options menu. Flags are re-applied any time a world transitions from unloaded to
loaded (server startup recreation and manual `/wm load`), matching how game rules are already
handled.

Deliberately *not* added as a separate flag: a "time lock." `doDaylightCycle` is already an
editable game rule via the existing Game Rules screens, and a second, parallel way to achieve the
same effect would just be a confusing duplicate.

## Multiverse-addon parity: granular profiles, portal scaling, sign portals, custom portal regions

Four more features, closing the remaining gaps against the Multiverse addon ecosystem this plugin
folds into one jar. Each was compiled and boot-tested independently before moving to the next;
none required a database schema migration or a change to the 1.20.1 API floor.

### Granular per-group player profiles (Multiverse-Inventories parity)

`configs/ProfileType` is a new enum (`INVENTORY`/`HEALTH`/`HUNGER`/`EXPERIENCE`/`BED_SPAWN`/
`LOCATION`) replacing the previous all-or-nothing inventory+health+hunger bundle.
`LinkedWorldsManager.getShares(group)` resolves a group's configured `Set<ProfileType>`, reading
either the legacy flat world-list shape (mapped to `DEFAULT_SHARES` =
`{INVENTORY, HEALTH, HUNGER}`, byte-for-byte the old behavior) or a new `worlds:`+`share:` shape —
detected via `FileConfiguration#isList`, so both shapes coexist across different groups in the
same file. `configs/PlayerInventoryManager.ProfileData` carries a `captured: EnumSet<ProfileType>`
recording which aspects a given snapshot actually holds, so `restoreProfile`/`applyTo` never
apply a stale or absent aspect. `location` sharing needed a new capture point:
`PlayerChangedWorldEvent` fires *after* a cross-world move completes and only exposes the
destination `World`, not the player's prior coordinates, so the plugin also listens for
`PlayerTeleportEvent` (which fires before the move, with the real `getFrom()` location) purely to
snapshot "last position in this group" the instant a shared-location group is left.
`database/DatabaseManager` serializes the new fields (`level`/`exp`/`bedSpawn.*`/
`lastLocation.*`) plus the `captured` list into the existing YAML blob column — the SQL schema
(`health`/`food_level`/`saturation`/`held_item_slot` columns) is untouched, avoiding a migration
entirely.

### Custom Nether portal scaling (Multiverse-NetherPortals parity)

`configs/LinkedPortalsManager` gained an optional `nether-scale` per link (default `8.0`, vanilla's
own ratio - untouched configs see zero behavior change). `events/PortalLinkListener` only
recomputes Nether X/Z coordinates by hand (scaling the origin position and doing a
`getHighestBlockYAt` safe-Y lookup) when a link's scale differs from the vanilla default;
otherwise it keeps trusting vanilla's own already-correct `PlayerPortalEvent#getTo()`, exactly as
before. No equivalent was added for the End: confirmed vanilla never derives End coordinates from
the player's position in the first place (entry always lands on the fixed exit platform, return
always goes to the player's last overworld position), so there is nothing there to scale.

### Sign portals (Multiverse-SignPortals parity)

New `configs/SignPortalsManager` (persistence, `signportals.yml`) and
`events/SignPortalListener` (`SignChangeEvent` to register, `PlayerInteractEvent` right-click to
use, `BlockBreakEvent` to clean up). Deliberately kept to the deprecated `String`-based
`SignChangeEvent#getLine`/`Sign#setLine` accessors rather than the newer per-side `Component` API
— confirmed via Context7 against Paper's 26.2 javadoc that they're deprecated but still fully
functional at the ceiling, and they're the only sign-text API available at all on the 1.20.1
floor, so this is the same "compile against the floor" strategy already used elsewhere in this
plugin, not an oversight.

### Custom portal regions (Multiverse-Portals parity)

New `configs/CustomPortal`/`CustomPortalsManager` (`portals.yml`) and
`events/CustomPortalListener`. Selection is a WorldEdit-wand-style two-point flow
(`/wm portal pos1`/`pos2`, held only in memory, never persisted) consumed by
`/wm portal create <name>`; a portal's destination/permission/price/launch vector are each set
independently afterward. Containment checks run on `PlayerMoveEvent` (short-circuited unless the
mover's block position actually changed, since a server fires several same-block move events per
tick from head rotation alone) and, for vehicles, `VehicleMoveEvent` — a player riding a vehicle
is handled only through the vehicle path, never double-triggering through both. A short per-entity
cooldown after any trigger prevents an overlapping or destination-side portal from immediately
firing again. Pricing is backed by `util/EconomyHook`, a soft dependency on Vault
(`compileOnly com.github.MilkBowl:VaultAPI` via JitPack, `softdepend` in `plugin.yml`) — modeled
directly on the existing PlaceholderAPI optional-hook pattern in `WorldManager.java`; every portal
is simply free whenever Vault or a registered economy plugin isn't present, so this never becomes
a hard dependency.

### Guided command UX

`/wm sign`/`/wm pregen`/`/wm portal` invoked bare or with an unrecognized action now show a
clickable step-by-step guide (a `<lang-key>.guide` string list, mirroring the top-level `help:`
list's existing format) instead of a single terse usage line. `/wm load`/`/wm tp` with a missing
required argument list loaded/loadable worlds as clickable suggestions instead of just erroring.
`create`/`load` success messages suggest the natural next step (`/wm tp <world>`, `/wm gui`).
Verified empirically (not assumed) that MiniMessage renders an unrecognized tag name as literal
text rather than erroring, using the exact pinned `adventure-text-minimessage:4.17.0` dependency
in an isolated standalone test, before relying on that behavior for placeholder text like
`<world>`/`<amount>` inside these new guide strings.

## Verification performed

- `./gradlew compileJava` / `./gradlew shadowJar` clean at every stage (after the critical-bug
  fixes, again after the GUI rewrite + version floor change, again after per-world flags, and
  again after each of the four Multiverse-parity features and the guided-command-UX pass).
- `./gradlew -b alt.gradle compileJava` also clean; `alt.gradle`'s `paper-api` version and
  dependency list were kept in sync with `build.gradle` throughout, including the later addition
  of the optional Vault dependency.
- All 6 lang YAML files, `plugin.yml`, and `config.yml` parsed successfully with a YAML parser.
  Every `lang/*.yml` file's flattened key set was diffed against `en.yml`'s after every batch of
  new keys — all six stayed identical throughout (204 keys in the final release).
- Downloaded a real Paper 1.20.1 server (build 196) and boot-tested the actual built jar multiple
  times across the session: plugin loads and enables with **zero exceptions**, SQLite schema
  (including the `held_item_slot` migration) initializes cleanly, linked-worlds-inventory
  correctly reports disabled by default, the server reaches `Done`, and it shuts down cleanly on
  `stop`. The Multiverse-parity features were additionally boot/shutdown-tested against the 26.2
  ceiling jar with a clean enable/disable cycle and zero exceptions or errors anywhere in either
  log.
- Unlike the GUI rewrite itself (verified only by headless boot-testing at the time this document
  was first written), the in-game interactive flow **was** subsequently verified by the project
  owner by hand: every menu screen was opened, a world was created through it, world flags were
  toggled, and the `ChatInput` text-entry flow (world name / seed / generator) was confirmed
  working end-to-end — after replacing the anvil-based approach that turned out not to work in
  practice (see bug #15 above). The Multiverse-parity features and guided-command-UX pass were
  verified via compilation, boot-testing, and isolated dependency-level checks (the MiniMessage
  tag-rendering test above); manual in-game click-through of those specific features is still
  recommended before considering them fully proven.

---

# WorldManager 3.0.0 — Audit

This document covers the full review performed for the 3.0.0 release: the version/compatibility
strategy, the Dialog-based GUI rewrite, every bug found and fixed, what was deliberately left
alone, and remaining known risks. It's meant to be read by whoever maintains this plugin next
(possibly future-you), not just as a changelog entry.

## Methodology

Three independent passes were run over the full codebase (GUI screens, every subcommand +
config/database layer, and messages/lang files/misc utilities), cross-referenced against the
actual Paper 1.21.7 API sources (extracted from the real `paper-api` sources jar, not
documentation summaries, given how new and experimental the Dialog API is). Every finding below
was verified against the current file before being called a bug.

## Critical fix: inventory loss on world change (present since 2.2.0)

**This is almost certainly the cause of the "new update causes players to lose their inventory
when switching between worlds" report on 2.2.1.**

`events/WorldChangeListener.onPlayerChangeWorld` computed `fromGroup`/`toGroup` via
`LinkedWorldsManager.getWorldGroup(...)`, which returns `null` for any world not listed in
`linked-worlds-inventory`. The linking check (`areWorldsLinked`) only special-cases *same world*
or *same non-null group* — so two ordinary, ungrouped worlds (the overwhelmingly common case on
any multi-world server) were **not** considered "linked," which sent execution into the
save/restore branch. Since `toGroup` was `null`, it hit:

```java
} else {
    // Entering a world not in any group, clear inventory
    clearPlayerInventory(player);
}
```

wiping the player's entire inventory, armor, health, food, and saturation on **every** transition
into a world that simply wasn't part of any configured group — regardless of where they came
from. On a server with `enable-linked-inventory: true` and even one group defined, every other
world change (survival ↔ creative-test ↔ minigame worlds, etc.) would silently wipe the player.

Compounding this: both `WorldChangeListener.isFeatureEnabled()` and
`LinkedWorldsManager.loadLinkedWorlds()` defaulted to **`true`** when the `enable-linked-inventory`
config read failed or the key was absent (`getConfig().getBoolean("enable-linked-inventory", true)`).
A fail-*open* default on a feature that actively destroys inventory data is backwards — any config
corruption or migration hiccup would silently turn this on server-wide.

**Fix**: `WorldChangeListener` now returns early (leaves the inventory untouched) when *neither*
the source nor destination world is in a configured group. Both default-value fallbacks were
changed from `true` to `false` (fail closed). Server owners on 2.2.x should upgrade immediately.

## Version / compatibility strategy

- Compiled against `paper-api:1.21.7-R0.1-SNAPSHOT` — the oldest Paper version exposing the
  Dialog API — rather than the newest (26.2), because Paper's Bukkit-facing API is
  backward-additive: a plugin built against an older API reliably runs on newer servers, not the
  reverse. This is the only way to honestly advertise a "1.21.7 → 26.2" range with one jar.
- This choice also avoided a much larger, unrelated migration: Paper 26.2 requires Java 25 and
  Gradle 9.1+, while 1.21.7 keeps the existing Java 21 / Gradle 8.14.5 / shadow 8.3.0 toolchain
  working as-is.
- The Dialog API is still marked `@ApiStatus.Experimental` by Paper upstream as of 1.21.7. Its
  shape could change in a future Paper release; if a server owner reports dialog-related errors on
  a very new Paper build, that's the first thing to check.
- Verified end-to-end: `./gradlew compileJava`/`shadowJar` and `./gradlew -b alt.gradle shadowJar`
  both succeed, and a real Paper 1.21.7 server boots the built jar cleanly with no exceptions
  (headless smoke test — see Verification below).
- **Spigot/CraftBukkit support is dropped as of 3.0.0.** The Dialog API is Paper-exclusive; there
  is no equivalent on vanilla Spigot/CraftBukkit. Servers not on Paper should stay on the 2.x line.

## GUI rewrite (Dialog API)

The entire `guis/` package (stefvanschie IF-based chest inventories: `GUISManager`, `CreatorGUI`,
`EditorGUI`, `EditorOptionsGUI`, `GameRuleEditorGUI`, `LoaderGUI`, `GUIList`) and the now-unused
`ItemBuilder` were deleted and replaced with a new `dialogs/` package built on
`io.papermc.paper.dialog.Dialog`. The `com.github.stefvanschie.inventoryframework:IF` dependency
and its shade-relocate rule were removed from both `build.gradle` and `alt.gradle`.

This wasn't a mechanical port — the Dialog API is a fundamentally different paradigm (no item
slots, no inventory-click events) and several parts of the old GUI were redesigned rather than
reproduced:

- **World creation**: the old flow required closing the inventory, typing in chat, and reopening
  it three separate times (name, seed, generator), plus a "click to cycle" lore hack for picking a
  world type. It's now one form dialog with real text inputs and a dropdown (`singleOption`).
- **Destructive-action confirmation**: the old GUI hand-rolled a bespoke 1-row "confirm or back"
  chest screen. `DialogType.confirmation(yes, no)` is a first-class Dialog type — this is a
  genuine simplification, not just a reskin.
- **Integer game rules**: the old GUI offered ±1 buttons or a chat-capture path for typing an exact
  value. `DialogInput.numberRange(...)` replaces both with a single slider.
- Button click handling uses inline `DialogAction.customClick(DialogActionCallback, Options)`
  callbacks — no `PlayerCustomClickEvent` listener or `Key`-based dispatch table was needed, since
  every dialog here is built ad-hoc per-open rather than registered in a bootstrap-time registry.
- Manual pagination (world lists, game rule lists) is preserved as a pattern: Prev/Next buttons
  call back into the same screen's `build(...)` with `page±1`.
- Icons are gone. `ActionButton` has no item-icon slot in this API — only `DialogBody.item(...)`
  can show an item, as static (non-clickable) content, not as decoration on a button. This is an
  inherent limitation of the API, not an oversight.

## Bugs found and fixed

| # | File | Bug | Fix |
|---|---|---|---|
| 1 | `events/WorldChangeListener` | Inventory wiped on any ungrouped→ungrouped world change (see Critical fix above) | Skip when neither world is grouped; fail-closed config defaults |
| 2 | `commands/subcommands/Load.java` | `worldCreator.generator(generator)` was never called — custom generators passed to `/wm load` were recorded in metadata but silently never applied | Now applied before `Bukkit.createWorld(...)` |
| 3 | `commands/subcommands/Unload.java` | Players in the unloading world were teleported/messaged twice (redundant nested loop over the same set) | Collapsed to one pass |
| 4 | `commands/subcommands/Unload.java` | No feedback when `Bukkit.unloadWorld(...)` returns `false` without throwing | New `unload.rejected` message |
| 5 | `commands/subcommands/Delete.java` | `delete.success` and `WorldManager.removeWorld(...)` fired *before* the async folder deletion actually completed; failures were silently rethrown as `RuntimeException` on a scheduler thread with no player feedback | Success/removal now happen inside the async task's completion; new `delete.async_failed` message on failure |
| 6 | `configs/BackupConfig.backupWorld` | `ZipUtil.pack(...)` and the config save ran via `runTask` (main thread) despite being the heaviest I/O in the plugin | Moved to `runTaskAsynchronously` |
| 7 | `configs/BackupConfig.restoreWorld` | Same for `ZipUtil.unpack(...)`; additionally `restore.finished` fired immediately after *scheduling* the restore, not after it actually completed, with no failure path at all | Unpack moved to async, world recreation hops back to main thread, success/failure messages moved to the real completion points; new `restore.failed` message |
| 8 | `events/JoinListener.getLatestReleaseNote` | Synchronous `HttpClient.send(...)` called directly inside the `PlayerJoinEvent` handler — a real network call on the main thread, up to ~10s worst case, on every OP's first join per session | Fetch moved to `runTaskAsynchronously`, message send hops back to main thread |
| 9 | `util/UpdateChecker.getVersion` | Scheduled via `runTask` (main thread), blocking `URL#openStream()` with no connect/read timeout (a prior commit claimed timeouts were added; they weren't) | Moved to `runTaskAsynchronously` with explicit 5s connect/read timeouts |
| 10 | `commands/subcommands/pregenerator/ChunkGenerator.stop` | Divided by `generatedChunkCount` unconditionally for final stats — `NaN`/`Infinity` if stopped before any chunk generated | Guarded with a zero-check |
| 11 | `commands/subcommands/pregenerator/ChunkGenerator` | Dead `Set<Chunk> generatedChunks` retained strong references to every generated `Chunk` for the whole run, purely for a containment check the grid-walk already guarantees never triggers | Removed |
| 12 | `commands/WorldManagerCommand` / `Pregen` / `ChunkGenerator` | Pre-generation state was one global static field — only one world could pre-generate at a time server-wide, nulled from two different call sites | Rescoped to `Map<worldName, ChunkGenerator>`; `ChunkGenerator` is now the sole owner of its own map entry |
| 13 | `commands/subcommands/pregenerator/Pregen` | Usage text and "unknown action" bypassed the lang system entirely (hardcoded, non-localized) | Routed through `pregen.usage`/`pregen.unknown_action` lang keys |
| 14 | `configs/PlayerInventoryManager` | Backing maps were plain `HashMap` despite documented synchronous DB access from event handlers; a DB-load failure in `restoreInventory` was silently swallowed with no logging | Switched to `ConcurrentHashMap`; failures now logged at `WARNING` |
| 15 | `database/SQLiteConnection` / `MySQLConnection` | `Class.forName(driver)` reflection repeated on every single `getConnection()` call | Moved to a `static {}` initializer, run once per class load |
| 16 | `configs/WorldsConfig.loadGameRules` | Called `GameRule.key()`, which doesn't exist on the Paper 1.21.7 API the plugin now targets (added in a later Paper version) — **this would have failed to compile at all** against the new target | Replaced the manual `GameRule.values()` linear search with `GameRule.getByName(...)`, which exists on 1.21.7 and is simpler |
| 17 | Multiple subcommands (`Load`, `Delete`, `Create`, `BackupConfig`) | World names from user input (chat, command args, dialog fields) were used directly in `new File(...)`/`new WorldCreator(...)` with no validation — a path-traversal gap | New `util/WorldNameValidator`; wired into all four |
| 18 | `commands/subcommands/Backup`, `Restore`, `Gui`, `Pregen` | Silently no-op'd for non-player senders with zero feedback (inconsistent with `Teleport`, which sends a proper error) | All four now send `general.players_only` |
| 19 | `src/main/resources/plugin.yml` | **`version` was hardcoded to `'2.2.1'`** instead of the `${version}` Gradle template placeholder `processResources` is set up to substitute — the plugin has been reporting a stale version at every startup regardless of the actual build, undetected until a live server boot test showed `Loading server plugin WorldManager v2.2.1` from a jar literally named `WorldManager-3.0.0.jar` | Restored the `${version}` placeholder |
| 20 | `alt.gradle` | Stray `relocate("dev.triumphteam.gui", "fr.mathildeuh.gui")` shade rule for a dependency that was never actually declared in that file | Removed alongside the IF dependency cleanup |
| 21 | `pl.yml` | Entire `delete:` and `list:` sections were missing — those messages silently failed to resolve for Polish users | Sections restored (translated) |
| 22 | `ru.yml` | Code looks up `teleport.console_need_player`; the Russian file had the same text under a different key, `teleport.errorFromConsole` | Key renamed to match |

## New feature: linked Nether/End portals

Config-driven, same spirit as `linked-worlds-inventory`: `linked-portals` in `config.yml` maps an
overworld name to specific Nether/End world names (e.g. `TEST` → `Nether_TEST`/`End_TEST`),
overriding Bukkit's auto-derived `<world>_nether`/`<world>_the_end` pairing. Handled by
`PortalLinkListener` on `PlayerPortalEvent`, in both directions (entering *and* returning).
Implementation deliberately reuses vanilla's own already-scaled/clamped destination coordinates
(`event.getTo()`) and only swaps the target `World`, rather than re-implementing nether coordinate
scaling or End platform placement — this avoids a whole class of off-by-one/world-border bugs a
from-scratch coordinate transform would risk. Ships disabled by default (empty `linked-portals: {}`).

## Deliberately not changed

- `commands/subcommands/Lists.java` only lists loaded worlds, one chat line per world, no
  pagination. Left as-is — out of scope for this pass, low severity, not GUI-related.
- No connection pooling library (e.g. HikariCP) was introduced for the database layer. Player
  inventory saves/loads happen at most once per world change per player — not a hot path — so the
  fix applied (removing repeated `Class.forName` reflection) addresses the actual measurable
  overhead without adding a new dependency for a low-concurrency use case. A high-traffic server
  with many worlds/groups may still want to revisit this.
- `messages/MessageManager.parse(String)`'s legacy hardcoded-prefix message parsing was flagged as
  fragile/possibly-dead code during review but not removed, since removing it without confirming
  no caller depends on it risked a silent regression outside this pass's scope.

## Known risks / things to watch

- The Dialog API is experimental upstream; a future Paper release could change method signatures
  in ways that only surface at compile time on a version bump, not silently at runtime.
- Compiling against 1.21.7 while targeting up to 26.2 relies on Paper's general backward-API-compat
  policy holding across that whole range. If a server owner on a very new Paper build reports the
  GUI or portal linking behaving oddly, check whether a newer Paper release deprecated/changed
  something the plugin depends on.
- The interactive Dialog UI (actual button clicks, form submission, nested navigation) could not be
  verified with a real Minecraft client in this environment — only compilation against the real
  API and a clean headless server boot (plugin loads, `onEnable()` completes with no exceptions)
  were verified. In-game click-through testing is still recommended before a full release.

## Migration notes for server owners upgrading from 2.x

- `worldmanager.gui` permission is unchanged; `/wm gui` now opens a Dialog instead of a chest
  inventory — the look changes, the command and permission don't.
- `worlds.yml`/`backups.yml`/database schema are unchanged — no data migration needed.
- **If you use `enable-linked-inventory`, re-read the Critical fix section above** — this release
  changes actual runtime behavior for ungrouped worlds (previously destructive, now a no-op, which
  is what most server owners actually wanted).
- If you're on Spigot/CraftBukkit (not Paper), do not upgrade to 3.0.0 — stay on the 2.x line.
- New optional `linked-portals` config section, disabled (empty) by default — opt in per-world.

## Verification performed

- `./gradlew compileJava` / `./gradlew shadowJar` — clean, no errors, no leftover
  `com.github.stefvanschie` references.
- `./gradlew -b alt.gradle shadowJar --no-daemon` — CI build path also clean.
- All 6 lang YAML files and `config.yml`/`plugin.yml` parsed successfully with a YAML parser
  (catches indentation/syntax errors the compiler can't).
- Downloaded a real Paper 1.21.7 server build, installed the built jar, and booted it headless:
  plugin loads and enables with **zero exceptions**, database initializes, linked-worlds-inventory
  correctly reports disabled (matching default config), server reaches `Done`. Confirmed the
  `plugin.yml` version bug (#19 above) live from this boot, before it was fixed.
