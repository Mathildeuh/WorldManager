# WorldManager

Create, load, unload, delete, back up, restore, teleport between, and pre-generate Minecraft
worlds — all from one command, with a native in-game menu. No chest-GUI hacks: as of **3.0.0**,
every menu screen is built on Minecraft's own **Dialog** UI.

> Report bugs or suggest features on [GitHub Issues](https://github.com/Mathildeuh/WorldManager/issues).

## Features

- **World management** — create (normal, flat, large biomes, amplified, nether, the end, or a
  custom generator), load existing world folders, unload, and permanently delete worlds.
- **Backups** — zip a world's folder on demand and restore it later, with the heavy I/O kept off
  the main thread so it doesn't freeze the server.
- **Pre-generation** — generate chunks in a bounded radius around a center point ahead of time,
  with start/pause/resume/stop control and a boss-bar progress indicator. Multiple worlds can
  pre-generate concurrently.
- **Teleportation** — jump yourself or another player straight to a world's spawn.
- **Game rules** — browse and edit every `GameRule` supported by a world, per-world, without
  touching `bukkit.yml` or server commands.
- **Linked-worlds inventories** *(optional)* — group worlds together so players keep one shared
  inventory across the group, and get a clean slate (or their group's saved inventory) when they
  leave it. Backed by SQLite or MySQL/MariaDB.
- **Linked Nether/End portals** *(optional)* — point a world's Nether/End portals at specific
  worlds instead of Bukkit's auto-derived `<world>_nether`/`<world>_the_end` pair, e.g. send
  `TEST`'s portals to `Nether_TEST`/`End_TEST`. Both directions are handled automatically.
- **In-game dialog UI** — `/wm gui` opens a native Dialog menu: create a world through a single
  form (name, type, seed, generator, empty-world toggle), browse and manage loaded worlds, edit
  game rules with real sliders and toggles, and confirm destructive actions (delete/unload/
  restore) through a proper Yes/No dialog — no more "type in chat, don't type cancel."
- **PlaceholderAPI support** — `%worldmanager_current_world%`, `%worldmanager_loaded%`,
  `%worldmanager_total_player_amount%`, and more.
- **Six languages out of the box** — English, French, Spanish, German, Polish, Russian. Every
  player-facing message is defined in a lang file, not hardcoded.

## Commands

All commands are under `/worldmanager` (alias `/wm`).

| Command | Description |
|---|---|
| `/wm` or `/wm gui` | Open the main Dialog menu |
| `/wm create <name> [type] [seed] [generator]` | Create a new world |
| `/wm load <name> [dimension] [generator]` | Load an existing world folder |
| `/wm unload <name>` | Unload a world |
| `/wm delete <name>` | Permanently delete a world |
| `/wm backup <name>` | Back up a world |
| `/wm restore <name>` | Restore a world from its latest backup |
| `/wm teleport <world> [player]` | Teleport yourself or another player to a world |
| `/wm pregen <start\|stop\|pause\|resume> <world> [center:x,z] [radius:n]` | Control world pre-generation |
| `/wm list` | List currently loaded worlds |

## Permissions

| Permission | Grants |
|---|---|
| `worldmanager.command` | Base access to `/worldmanager` |
| `worldmanager.gui` | Open the Dialog menu |
| `worldmanager.create` | Create worlds |
| `worldmanager.load` | Load worlds |
| `worldmanager.unload` | Unload worlds |
| `worldmanager.delete` | Delete worlds |
| `worldmanager.backup` | Back up worlds |
| `worldmanager.restore` | Restore worlds |
| `worldmanager.teleport` | Teleport between worlds |
| `worldmanager.pregen` | Control pre-generation |
| `worldmanager.list` | List loaded worlds |

## Supported versions

**Paper 1.21.7 → 26.2 only.** Dialogs first shipped client-side in Minecraft 1.21.6 and became
available to plugins in Paper 1.21.7 — WorldManager 3.0.0 targets that as its floor so the
Dialog UI works everywhere it possibly can, while staying current with the latest release.
**Spigot/CraftBukkit are not supported from 3.0.0 onward** — the Dialog API this plugin's GUI is
built on is Paper-exclusive. If you run Spigot/CraftBukkit, stay on the 2.x release line.

## What's new in 3.0.0

- **Critical fix**: a bug (present since 2.2.0) where players could lose their inventory when
  changing worlds even with linked-worlds-inventory *disabled*, or between two worlds neither of
  which was in a configured group. If you saw inventory loss after updating to 2.2.x, this is why
  — see [`AUDIT.md`](https://github.com/Mathildeuh/WorldManager/blob/master/AUDIT.md) for details.
- New: linked Nether/End portals (see Features above).
- Entire GUI rebuilt from scratch on the Dialog API, replacing the old chest-inventory menus.
- World creation is now a single form instead of a multi-step chat-capture flow.
- Integer game rules are edited with a proper slider instead of ±1 buttons or typing in chat.
- Numerous other correctness and reliability fixes: main-thread-blocking I/O moved off the main
  thread (backups, the update checker, join-time release-note fetch), a bug where custom
  generators passed to `/wm load` were silently ignored, a double-teleport bug on `/wm unload`,
  and "success" messages that could fire before the underlying async work actually finished.
  Full details in [`AUDIT.md`](https://github.com/Mathildeuh/WorldManager/blob/master/AUDIT.md).

## Configuration

```yaml
lang: en # en, fr, es, de, pl, ru
database:
  type: sqlite # or mysql
enable-linked-inventory: false
linked-worlds-inventory:
  group1:
    - world
    - world_nether
linked-portals:
  TEST:
    nether: Nether_TEST
    end: End_TEST
```

## Links

- [Source & issue tracker](https://github.com/Mathildeuh/WorldManager)
- [Hangar](https://hangar.papermc.io/Mathildeuh/Easy-WorldManager)
- [Modrinth](https://modrinth.com/plugin/easy-worldmanager)
- [SpigotMC resource page](https://www.spigotmc.org/resources/worldmanager.117043/)
