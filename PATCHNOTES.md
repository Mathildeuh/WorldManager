# WorldManager 4.0.0 — Patch Notes

## The big change: back to a chest GUI, and a much wider version range

3.0.0 rebuilt the entire in-game menu on Minecraft's native Dialog system, which only exists
starting with Paper 1.21.7. That's a hard floor: it made the plugin structurally unable to run
on anything from 1.20 through 1.21.6, which is a lot of still-common server versions. 4.0.0
reverses that call: the menu is now a modern chest-inventory GUI again (`fr.mathildeuh.worldmanager.guis`),
built from scratch — not a revert of the old pre-3.0.0 chest GUI code, which is gone for good —
with the same screens (create / edit / load / per-world options / game rules) redesigned around
colored borders, representative icons per world type/action, and a real text-entry mechanism
(`ChatInput` — closes the menu and captures the next chat message, `cancel` aborts) for the one
thing chest GUIs have always struggled with: typing a name, seed, or generator without leaving
the menu.

**Supported versions: Paper 1.20.1 → 26.2.** Compiled against the 1.20.1 API (the floor, not the
ceiling — Paper's Bukkit-facing API is backward-additive, so a plugin built against an old API
version keeps working on newer servers) and boot-tested end to end against a real Paper 1.20.1
server with zero exceptions.

Spigot/CraftBukkit are still not supported — that split happened in 3.0.0 for unrelated reasons
(the shaded chest-GUI library was dropped) and isn't being revisited here.

## Multiverse-addon parity: granular profiles, portal scaling, sign portals, and custom portal regions

Beyond the menu rewrite, this release also rounds out the feature parity with Multiverse's addon
ecosystem that was the whole point of folding everything into one plugin.

### Granular per-group player profiles (Multiverse-Inventories parity)

Linked-world groups (`linked-worlds-inventory` in `config.yml`) could previously only share the
whole bundle of inventory+health+hunger, all or nothing. Each group can now pick exactly which
aspects it shares — `inventory`, `health`, `hunger`, `experience`, `bed-spawn`, `location` — via a
new `worlds:` + `share:` config shape:

```yaml
linked-worlds-inventory:
  survival-group:
    worlds: [world, world_nether]
    share: [inventory, health, hunger, experience]
```

The old flat-list shape (`group: [world, test]`) still works exactly as before — it's read as
sharing inventory/health/hunger, the original behavior — so no existing config needs to change.
`location` restores the player's exact last position in that group (captured the instant they
leave it) instead of just dropping them at the destination world's default spawn.

### Custom Nether portal scaling (Multiverse-NetherPortals parity)

Linked Nether pairs (`linked-portals` in `config.yml`) can now set a `nether-scale` other than
vanilla's 8:1 — useful for a 1:1 "twin world" nether, or any other custom ratio. Leave it unset and
nothing changes; the plugin keeps trusting vanilla's own portal math exactly as before.

### Sign portals (Multiverse-SignPortals parity)

Place a sign, write `[WorldManager]` on the first line and a destination world's name on the
second, and right-clicking it teleports there. `/wm sign list` shows every registered sign portal.
Gated by two new permissions: `worldmanager.sign.create` (op by default) and
`worldmanager.sign.use` (everyone, by default).

### Custom portal regions (Multiverse-Portals parity)

`/wm portal pos1`/`pos2` select a cuboid (WorldEdit-wand style), `/wm portal create <name>` turns
it into a named portal region, and its behavior is built up from there: `destination` (a world, or
your current position), `permission` (an extra node beyond the base `worldmanager.portal`),
`price` (requires an installed Vault + economy plugin — free otherwise, Vault is a soft
dependency, not a requirement), and `launch <dx> <dy> <dz>` (a velocity applied on entry, for
cannon-style portals with or without a teleport destination). Works for players on foot and for
occupied minecarts/boats. `/wm portal list` shows every registered region.

### Also in this release

- **Critical fix**: `PlayerQuitEvent` was wiping a player's *entire* linked-inventory history
  (every group, not just the one they were just saved into) from the database on every
  disconnect — the opposite of what "persistent linked inventories" is supposed to do. Quitting
  now only clears the in-memory cache; the database is untouched.
- Inventory saves/loads for linked worlds now happen off the main thread.
- `worldmanager.*` permissions are now properly declared in `plugin.yml` (`default: op`), and
  operator status no longer overrides an explicit permission negation from a permissions plugin.
- A world-operation lock now prevents `pregen`/`backup`/`restore`/`unload`/`delete` from racing
  each other on the same world (previously, pre-generation could keep writing chunks into a
  world folder that `delete` was simultaneously removing).
- `pregen start ... radius:n` now means an actual chunk radius (a `(2n+1)²` square), not a
  vaguely-related "total chunk count" that didn't match the option's name.
- New per-world flags in the spirit of Multiverse-Core: PvP, mob spawning, animal spawning, a
  weather lock, and a custom spawn point (used by `/wm tp` once set). Reachable from each world's
  options screen.
- Fixed a handful of smaller bugs: a `chunks/s` stat that always printed `0` (integer division),
  a permission-vs-usage message mix-up on `backup`/`restore`/`delete`/`unload` with missing
  arguments, broken tab-completion on `pregen`'s extra arguments, a hardcoded Windows path in
  the Gradle build, and a world-name validation gap in the creator flow's "empty world" path.
- Guided command help: running `/wm sign`, `/wm pregen`, or `/wm portal` with no arguments (or an
  unrecognized action) now shows a clickable step-by-step guide instead of a bare usage line;
  `/wm load`/`/wm tp` with a missing name now list loaded/loadable worlds as clickable
  suggestions; and `create`/`load` success messages suggest the natural next step
  (`/wm tp <world>`, `/wm gui`).

Full technical writeup in [`AUDIT.md`](AUDIT.md).

---

# WorldManager 3.0.0 — Patch Notes

## The big change: Spigot support is dropped, the GUI moves to native Dialogs

Starting with 3.0.0, **WorldManager is Paper-only.** Spigot and vanilla CraftBukkit are no longer
supported — if you run one of those, stay on the 2.x release line, which will keep working as
before.

### Why

The plugin's entire in-game menu (`/wm gui`) has been rebuilt from scratch on top of Minecraft's
native **Dialog** system — the same UI framework Mojang added to the game itself in 1.21.6, exposed
to plugins starting with **Paper 1.21.7**. Dialogs are server-driven forms and menus rendered by the
client itself: real text fields, dropdowns, sliders, and Yes/No confirmation screens, instead of
plugins faking a "menu" out of a chest inventory (clickable items, chat prompts standing in for
text input, etc.) — which is what WorldManager's GUI, and most Bukkit plugin GUIs in general, have
always had to do.

This is a **Paper-exclusive API**. There is no equivalent on Spigot or CraftBukkit, and no
reasonable way to keep both a Dialog-based menu and a chest-based fallback maintained side by side
without either doubling the GUI code forever or watering down the new one. Given that Paper has
been the de facto standard for new server setups for years now, the decision was to go all-in on
the native UI rather than keep carrying the old approach.

**What this means for you:**
- Running Paper (1.21.7 or newer, up to the current 26.2): update freely, everything keeps working
  and `/wm gui` now opens a proper native dialog instead of a chest menu.
- Running Spigot/CraftBukkit: **do not update to 3.0.0.** The plugin will not load the way you
  expect — stick with the last 2.x release.
- Running a Paper fork that doesn't implement Paper's own API surface: not supported, same as
  before.

### What actually changed in the menu

The rewrite wasn't just a reskin — a few flows are genuinely better now that they're not
constrained by "items in an inventory":

- **Creating a world** used to mean closing the menu, typing a name in chat, reopening the menu,
  closing it again to type a seed, reopening again for a generator, and clicking through a "cycle
  forward" button to pick a world type. It's now one form: name, type dropdown, seed, generator,
  and an empty-world toggle, all on one screen, submitted at once.
- **Deleting / unloading / restoring a world** now goes through a proper Yes/No confirmation
  dialog instead of a hand-built one-row chest menu.
- **Editing an integer game rule** is now a slider with the current value pre-filled, instead of
  clicking +1/-1 buttons or typing a number in chat.

Everything else — permissions, commands, config files, world/backup data — is unchanged.

---

## Also in this release

- **Critical fix**: an inventory-loss bug (since 2.2.0) where players could lose their entire
  inventory switching between two ordinary worlds that weren't even part of a linked-inventory
  group. Full details in [`AUDIT.md`](AUDIT.md).
- New: **linked Nether/End portals** — point a world's portals at specific companion worlds
  instead of Bukkit's auto-generated `<world>_nether`/`<world>_the_end` pair.
- A round of correctness/reliability fixes across backups, restores, pre-generation, and the
  update checker — see [`AUDIT.md`](AUDIT.md) for the full list.

Supported versions: **Paper 1.21.7 → 26.2.**
