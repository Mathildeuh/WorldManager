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
