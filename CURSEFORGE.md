# WorldManager

Create, load, unload, delete, back up, restore, teleport between, and pre-generate Minecraft
worlds — all from one command, with a modern in-game menu, per-world flags, and shared/linked
inventories and portals between worlds. One plugin covering the same ground as Multiverse-Core
plus its Inventories and NetherPortals addons.

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
- **In-game menu** — `/wm gui` opens a modern chest-inventory menu: create a world through a
  single form (name, type, seed, generator, empty-world toggle), browse and manage loaded worlds,
  edit game rules with click-to-toggle/±1/±10 controls, and confirm destructive actions
  (delete/unload/restore) through a proper Yes/No screen — no chat prompts to type a name or seed
  into, the menu opens a real text-entry prompt for that.
- **Per-world flags** — PvP, mob spawning, animal spawning, a weather lock, and a custom spawn
  point per world, editable from the same menu.
- **PlaceholderAPI support** — `%worldmanager_current_world%`, `%worldmanager_loaded%`,
  `%worldmanager_total_player_amount%`, and more.
- **Six languages out of the box** — English, French, Spanish, German, Polish, Russian. Every
  player-facing message is defined in a lang file, not hardcoded.

## Commands

All commands are under `/worldmanager` (alias `/wm`).

| Command | Description |
|---|---|
| `/wm` or `/wm gui` | Open the main menu |
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
| `worldmanager.gui` | Open the menu |
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

**Paper 1.20.1 → 26.2 only.** Compiled against the 1.20.1 API (Paper's Bukkit-facing API is
backward-additive, so building against the floor rather than the latest keeps it running on
every release in between) and boot-tested against a real 1.20.1 server. **Spigot/CraftBukkit are
not supported** — a shaded chest-GUI library the plugin depended on before 3.0.0 was dropped and
hasn't been reintroduced. If you run Spigot/CraftBukkit, stay on the 2.x release line.

## What's new in 4.0.0

- **Much wider version support**: 3.0.0's menu required Paper 1.21.7+ (it was built on
  Minecraft's Dialog UI, which doesn't exist before then). 4.0.0 moves back to a chest-inventory
  menu — redesigned, not a revert — specifically to bring the floor down to **1.20.1**.
- **Critical fix**: disconnecting could wipe a player's *entire* linked-inventory history (every
  group, not just the current one) from the database. Fixed — quitting now only clears the
  in-memory cache.
- New: **per-world flags** — PvP, mob/animal spawning, a weather lock, and a custom spawn point,
  editable from each world's menu screen (see Features above).
- A world-operation lock now prevents pre-generation from racing a concurrent
  unload/delete/backup/restore on the same world.
- `worldmanager.*` permissions are now properly declared (`default: op`), and an operator's
  explicit permission negation (via a permissions plugin) is now actually respected.
- Numerous smaller correctness fixes across pre-generation, backups, and command feedback — full
  details in [`AUDIT.md`](https://github.com/Mathildeuh/WorldManager/blob/master/AUDIT.md).

### Earlier: what changed in 3.0.0

- **Critical fix**: a bug (present since 2.2.0) where players could lose their inventory when
  changing worlds even with linked-worlds-inventory *disabled*, or between two worlds neither of
  which was in a configured group. If you saw inventory loss after updating to 2.2.x, this is why.
- New: linked Nether/End portals (see Features above).
- Numerous correctness and reliability fixes: main-thread-blocking I/O moved off the main thread,
  a bug where custom generators passed to `/wm load` were silently ignored, a double-teleport bug
  on `/wm unload`, and "success" messages that could fire before the underlying async work
  actually finished.

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
