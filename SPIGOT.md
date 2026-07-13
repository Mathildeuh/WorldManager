[CENTER][SIZE=6][COLOR=#02a876]WorldManager[/COLOR][/SIZE]
[SIZE=3]Create, load, unload, delete, back up, restore, teleport between, and pre-generate Minecraft worlds — all from one command.[/SIZE]

[COLOR=#7d66ff][B]Version 3.0.0[/B][/COLOR] — the entire GUI is now built on Minecraft's native [B]Dialog[/B] UI. No more chest-inventory menus.

[URL='https://github.com/Mathildeuh/WorldManager/issues'][B]Report a bug / suggest a feature[/B][/URL]
[/CENTER]

[HR][/HR]
[SIZE=4][B]Features[/B][/SIZE]

[LIST]
[*][B]World management[/B] — create (normal, flat, large biomes, amplified, nether, the end, or a custom generator), load existing world folders, unload, and permanently delete worlds.
[*][B]Backups[/B] — zip a world's folder on demand and restore it later. Heavy I/O is kept off the main thread so backing up a large world doesn't freeze the server.
[*][B]Pre-generation[/B] — generate chunks in a bounded radius around a center point ahead of time, with start/pause/resume/stop control and a boss-bar progress indicator. Multiple worlds can pre-generate at once.
[*][B]Teleportation[/B] — jump yourself or another player straight to a world's spawn.
[*][B]Game rules[/B] — browse and edit every GameRule supported by a world, per-world, from the in-game menu.
[*][B]Linked-worlds inventories[/B] [I](optional)[/I] — group worlds together so players keep one shared inventory across the group, backed by SQLite or MySQL/MariaDB.
[*][B]Linked Nether/End portals[/B] [I](optional)[/I] — point a world's Nether/End portals at specific worlds instead of Bukkit's auto-derived <world>_nether/<world>_the_end pair.
[*][B]In-game Dialog UI[/B] — [ICODE]/wm gui[/ICODE] opens a native Dialog menu: create a world through a single form, browse and manage loaded worlds, edit game rules with real sliders and toggles, and confirm destructive actions through a proper Yes/No dialog.
[*][B]PlaceholderAPI support[/B] — [ICODE]%worldmanager_current_world%[/ICODE], [ICODE]%worldmanager_loaded%[/ICODE], [ICODE]%worldmanager_total_player_amount%[/ICODE], and more.
[*][B]Six languages out of the box[/B] — English, French, Spanish, German, Polish, Russian.
[/LIST]

[HR][/HR]
[SIZE=4][B]Commands[/B][/SIZE]
[ICODE]/worldmanager[/ICODE] (alias [ICODE]/wm[/ICODE])

[TABLE]
[TR][TH]Command[/TH][TH]Description[/TH][/TR]
[TR][TD][ICODE]/wm[/ICODE] or [ICODE]/wm gui[/ICODE][/TD][TD]Open the main Dialog menu[/TD][/TR]
[TR][TD][ICODE]/wm create <name> [type] [seed] [generator][/ICODE][/TD][TD]Create a new world[/TD][/TR]
[TR][TD][ICODE]/wm load <name> [dimension] [generator][/ICODE][/TD][TD]Load an existing world folder[/TD][/TR]
[TR][TD][ICODE]/wm unload <name>[/ICODE][/TD][TD]Unload a world[/TD][/TR]
[TR][TD][ICODE]/wm delete <name>[/ICODE][/TD][TD]Permanently delete a world[/TD][/TR]
[TR][TD][ICODE]/wm backup <name>[/ICODE][/TD][TD]Back up a world[/TD][/TR]
[TR][TD][ICODE]/wm restore <name>[/ICODE][/TD][TD]Restore a world from its latest backup[/TD][/TR]
[TR][TD][ICODE]/wm teleport <world> [player][/ICODE][/TD][TD]Teleport yourself or another player[/TD][/TR]
[TR][TD][ICODE]/wm pregen <start|stop|pause|resume> <world>[/ICODE][/TD][TD]Control world pre-generation[/TD][/TR]
[TR][TD][ICODE]/wm list[/ICODE][/TD][TD]List currently loaded worlds[/TD][/TR]
[/TABLE]

[HR][/HR]
[SIZE=4][B]Permissions[/B][/SIZE]

[TABLE]
[TR][TH]Permission[/TH][TH]Grants[/TH][/TR]
[TR][TD][ICODE]worldmanager.command[/ICODE][/TD][TD]Base access to /worldmanager[/TD][/TR]
[TR][TD][ICODE]worldmanager.gui[/ICODE][/TD][TD]Open the Dialog menu[/TD][/TR]
[TR][TD][ICODE]worldmanager.create[/ICODE][/TD][TD]Create worlds[/TD][/TR]
[TR][TD][ICODE]worldmanager.load[/ICODE][/TD][TD]Load worlds[/TD][/TR]
[TR][TD][ICODE]worldmanager.unload[/ICODE][/TD][TD]Unload worlds[/TD][/TR]
[TR][TD][ICODE]worldmanager.delete[/ICODE][/TD][TD]Delete worlds[/TD][/TR]
[TR][TD][ICODE]worldmanager.backup[/ICODE][/TD][TD]Back up worlds[/TD][/TR]
[TR][TD][ICODE]worldmanager.restore[/ICODE][/TD][TD]Restore worlds[/TD][/TR]
[TR][TD][ICODE]worldmanager.teleport[/ICODE][/TD][TD]Teleport between worlds[/TD][/TR]
[TR][TD][ICODE]worldmanager.pregen[/ICODE][/TD][TD]Control pre-generation[/TD][/TR]
[TR][TD][ICODE]worldmanager.list[/ICODE][/TD][TD]List loaded worlds[/TD][/TR]
[/TABLE]

[HR][/HR]
[SIZE=4][B]Supported versions[/B][/SIZE]
[COLOR=#02a876][B]Paper 1.21.7 → 26.2.[/B][/COLOR] Dialogs first shipped client-side in Minecraft 1.21.6 and became available to plugins in Paper 1.21.7 — that's the floor 3.0.0 targets, while staying current with the latest release.
[COLOR=red][B]Spigot/CraftBukkit are not supported from 3.0.0 onward[/B][/COLOR] — the Dialog API is Paper-exclusive.

[HR][/HR]
[SIZE=4][B]What's new in 3.0.0[/B][/SIZE]
[LIST]
[*][COLOR=red][B]Critical fix[/B][/COLOR]: a bug (present since 2.2.0) where players could lose their inventory when changing worlds even with linked-worlds-inventory disabled, or between two worlds neither of which was in a configured group.
[*]New: linked Nether/End portals (see Features above).
[*]Entire GUI rebuilt from scratch on the Dialog API, replacing the old chest-inventory menus.
[*]World creation is now a single form instead of a multi-step chat-capture flow.
[*]Integer game rules are edited with a proper slider instead of ±1 buttons or typing in chat.
[*]Numerous other correctness and reliability fixes: main-thread-blocking I/O moved off the main thread (backups, the update checker, join-time release-note fetch), a bug where custom generators passed to /wm load were silently ignored, a double-teleport bug on /wm unload, and "success" messages that could fire before the underlying async work actually finished.
[/LIST]

[HR][/HR]
[CENTER][SIZE=3][B]Links[/B][/SIZE]
[URL='https://github.com/Mathildeuh/WorldManager']GitHub[/URL] | [URL='https://hangar.papermc.io/Mathildeuh/Easy-WorldManager']Hangar[/URL] | [URL='https://modrinth.com/plugin/easy-worldmanager']Modrinth[/URL] | [URL='https://www.curseforge.com/minecraft/bukkit-plugins/easy-worldmanager']CurseForge[/URL][/CENTER]
