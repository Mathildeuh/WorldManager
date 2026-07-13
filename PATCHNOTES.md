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
