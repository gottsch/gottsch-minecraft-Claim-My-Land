<img src="https://raw.githubusercontent.com/wiki/gottsch/gottsch-minecraft-Claim-My-Land/images/curseforge/headings/cml_title.png" width="500px">

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B132)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.2.0%2B-1E2D4F)](https://files.minecraftforge.net/)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue)](LICENSE)
[![CurseForge](https://img.shields.io/badge/CurseForge-claim--my--land-F16436)](https://www.curseforge.com/minecraft/mc-mods/claim-my-land)
[![Modrinth](https://img.shields.io/badge/Modrinth-claim--my--land-00AF5C)](https://modrinth.com/mod/claim-my-land)

## **Mod ID**: `claimmyland`

A land-claiming mod for Minecraft 1.20.1 (Forge) that gives players free-form parcel sizing, a Nation/Zone/Citizen hierarchy, and a full protection system — no chunk grid, no compromises.

---

## Features

- **Free-form parcel sizing** — define any rectangular area, not locked to chunk boundaries
- **Nation / Zone / Citizen hierarchy** — tiered ownership model for server communities
- **Deed-based claiming** — deeds drop from vanilla and Treasure2 loot tables, or can be issued by ops
- **Visual border rendering** — in-world wireframe borders with per-type color coding
- **JourneyMap integration** — parcel polygon overlays on the full-screen and minimap
- **Seven whitelist types** — player, block, item, entity, and their respective Forge tags
- **Dimension-aware** — parcels are scoped to the dimension they are claimed in
- **Rolling JSON backups** — automatic periodic saves with configurable retention
- **Interactive chat UI** — clickable icon buttons for rename, demolish, whitelist management

Full feature details on the [Wiki](https://github.com/gottsch/gottsch-minecraft-Claim-My-Land/wiki).

---

## Installation

1. Install [Minecraft Forge](https://files.minecraftforge.net/) 47.2.0+ for Minecraft 1.20.1
2. Install [GottschCore](https://www.curseforge.com/minecraft/mc-mods/gottschcore) 2.6.0+ (required dependency)
3. Download Claim My Land from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/claim-my-land) or [Modrinth](https://modrinth.com/mod/claim-my-land)
4. Drop the `.jar` file into your `mods/` folder

**Recommended companions:**
- [JourneyMap](https://www.curseforge.com/minecraft/mc-mods/journeymap) — enables parcel overlays on the map
- [Chat Plus](https://www.curseforge.com/minecraft/mc-mods/chat-plus) — scrollable chat for long command output (**requires `"movableChatEnabled": false`** — see [Compatibility](https://github.com/gottsch/gottsch-minecraft-Claim-My-Land/wiki/Compatibility-and-Known-Issues))

---

## Documentation

The [GitHub Wiki](https://github.com/gottsch/gottsch-minecraft-Claim-My-Land/wiki) is the authoritative documentation.

| Topic | Link |
|---|---|
| First claim walkthrough | [Getting Started](https://github.com/gottsch/gottsch-minecraft-Claim-My-Land/wiki/Getting-Started) |
| Ownership model | [Estates and Parcels](https://github.com/gottsch/gottsch-minecraft-Claim-My-Land/wiki/Estates-and-Parcels) |
| Parcel hierarchy | [Nation / Zone / Citizen](https://github.com/gottsch/gottsch-minecraft-Claim-My-Land/wiki/Nation-Zone-Citizen) |
| Full command reference | [Commands Reference](https://github.com/gottsch/gottsch-minecraft-Claim-My-Land/wiki/Commands-Reference) |
| Protection and whitelists | [Protection System](https://github.com/gottsch/gottsch-minecraft-Claim-My-Land/wiki/Protection-System) |
| Server and client config | [Configuration](https://github.com/gottsch/gottsch-minecraft-Claim-My-Land/wiki/Configuration) |
| Known issues | [Compatibility & Known Issues](https://github.com/gottsch/gottsch-minecraft-Claim-My-Land/wiki/Compatibility-and-Known-Issues) |

---

## Building from Source

Claim My Land uses ForgeGradle. To build the mod jar yourself:

```bash
git clone https://github.com/gottsch/gottsch-minecraft-Claim-My-Land.git
cd gottsch-minecraft-Claim-My-Land
./gradlew build
```

The output jar will be in `build/libs/`.

**Dependencies:**
- Java 17
- GottschCore (fetched from the local Maven repo — see `build.gradle`)
- JourneyMap API (compile-only, fetched from the JourneyMap Maven)

**Development environment:**

```bash
./gradlew genEclipseRuns     # Eclipse
./gradlew genIntellijRuns    # IntelliJ IDEA
```

---

## Reporting Bugs

Please file bug reports on the [Issues](https://github.com/gottsch/gottsch-minecraft-Claim-My-Land/issues) page. A good report includes:

1. Minecraft version and Forge version
2. CML version and GottschCore version
3. Steps to reproduce
4. Server log (`logs/latest.log`) if applicable
5. Whether JourneyMap and/or Chat Plus are installed

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for the full version history.

---

## License

Claim My Land is licensed under the [GNU General Public License v3](LICENSE).

---

## Related Projects

- **[GottschCore](https://github.com/gottsch/gottsch-minecraft-GottschCore)** — required companion library
- **[Treasure2](https://www.curseforge.com/minecraft/mc-mods/treasure2)** — loot and treasure mod by the same author; CML deeds inject into Treasure2 chests

---

*By Mark Gottschling — author of [Treasure2](https://www.curseforge.com/minecraft/mc-mods/treasure2).*
