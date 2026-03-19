# Changelog for Claim My Land 1.20.1

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

> ⚠️ **IMPORTANT — Breaking Change & Backup Warning**
>
> **v2.x is not save-compatible with v1.x.** Parcel data saved by any v1 release **will not load** in v2. If you are upgrading an existing world, your claimed parcels will be lost.
>
> **Back up your world before installing v2.** Copy your entire world folder to a safe location before upgrading. Once you have loaded the world in v2, downgrading back to v1 is not supported.

---

## [2.2.0] - 2026-03-xx
### 🎉 Highlights

- **Multi-dimension support** — parcels can now be claimed in any dimension (Nether, End, modded dimensions), with per-dimension protection controlled via server config
- **Visual border renderer** — parcel borders are now rendered as 3D wireframes, replacing physical border blocks for all parcel sizes. Borders are only visible to the parcel owner.
- **JourneyMap Foundation Stone preview** — when placing a Foundation Stone, a coloured polygon appears on the JourneyMap showing the parcel boundary before you commit the claim
- **Structure intersection protection** — claiming parcels that overlap configured structure types can now be blocked via server config
- **Spaces in estate and parcel names** — rename commands now accept names with spaces; use quotes in the command: `/cml estate rename myEstate "My Estate"`
- **Dimension display** — parcel and estate detail commands now show which dimension a parcel is in
- **Fire spread prevention** — estate owners can now prevent fire from spreading within their claimed parcels

---

### ➕ Added

#### Multi-Dimension Parcel Support
- Parcels can now be claimed in any dimension
- Server config `excludedDimensions` list controls which dimensions are unclaimable; an empty list means all dimensions are claimable (including modded ones)
- Parcel chunk index is now dimension-aware — block events in unclaimed chunks in any dimension exit efficiently

#### Visual Border Renderer
- Parcel boundaries are now rendered as client-side 3D wireframes — no physical blocks are placed in the world
- Three visual layers per parcel: full wireframe box in ownership colour, buffer bracket quads using a stripe texture, and a horizontal area plane at border stone Y
- Borders are visible to the parcel owner only — no visual pollution for other players
- Conflict state (overlapping claims) renders the wireframe in red
- Borders persist correctly across server restarts and chunk reloads
- Physical border block, buffer block, and horizontal area block classes removed entirely

#### JourneyMap Foundation Stone Preview
- Placing a Foundation Stone now shows a live polygon overlay on the JourneyMap showing the exact parcel boundary that will be claimed
- Overlay is green when the area is clear, red when it intersects an existing parcel
- Preview clears automatically when the Foundation Stone is broken or the claim is committed

#### Structure Intersection Protection
- Server config can specify structure types (by resource location) that cannot be overlapped by claimed parcels
- Policy is lazily built from config on first use and invalidated on config reload
- Structures matched via `getAllStructuresAt()` with tag membership resolution — handles modded structures via tags

#### Fire Spread Prevention
- New per-estate property: `preventFireSpread` (default: `true` — fire spread blocked)
- Covers both `minecraft:fire` and `minecraft:soul_fire` via `claimmyland:fire_blocks` block tag; modded fire blocks can be added to the tag via datapack
- New command: `/cml estate preventFireSpread <estateName> <true|false>`
- Master on/off switch available in server config

#### Display Dimension in Commands
- Estate detail, parcel list, parcel detail, and `/cml claimedby` commands all now include a **Dimension** line showing the resource location (e.g. `minecraft:overworld`, `minecraft:the_nether`)

#### Estate & Parcel Name Improvements
- Estate and parcel names can now contain spaces — use quotes when entering them in commands: `"My Estate"`
- Tab-complete suggestions for names with spaces are automatically presented with quotes

---

### ⚙️ Changed

- **Border stones no longer expire** — since borders are only visible to the owner there is no visual pollution concern; the owner removes the stone at their discretion
- **Server backup system** — `reinitBackup()` now correctly initialises at `ServerStartingEvent` (was incorrectly called during `common_setup` before config values were available)

---

### 🐛 Fixed

- Fixed missing lang values.
- Fixed block placement protection incorrectly denying access when a player stands in a
  parent parcel (Nation/Zone) and places into a child parcel (Citizen/Player) they own —
  `resolveParcelCached()` now bypasses the region cache for non-leaf parcel types and
  falls through to a BST lookup to find the most specific (least significant) parcel at
  the target position.
- Fixed block interaction protection incorrectly denying access when a player clicks on 
  the "floor" of a parcel (Zone/Citizen) - now uses the correct position, which is 1 above
  the clicked block, to resolve the parcel access.

---

### 🗑️ Removed

- Removed physical `BorderBlock`, `NationBorderBlock`, `BufferBlock`, and `HorizontalAreaBlock` block classes and all their registrations
- Removed physical block placement and removal methods from `BorderStoneBlockEntity` and `CitizenPlacementBlockEntity`

---

### 🔧 Technical / Developer Notes

- New `ParcelBorderRenderer` — `RenderLevelStageEvent`-based client-side renderer; three render passes: wireframe (`RenderType.lines()`), buffer bracket quads (atlas-sampled stripe texture, UV-tiled per block), horizontal area plane
- `ACTIVE_BORDER_STONES` — tracks all live `BorderStoneBlockEntity` instances; used for login drain and visibility sync
- `CMLNetwork.syncBorderVisibleToOwner()` — sends `BorderVisibilityPacket(true)` to the parcel owner only; no-ops if owner is offline
- `CMLNetwork.syncBorderVisibilityToDimension()` — uses `PacketDistributor.DIMENSION` for hide packets from `onRemove()` where no player reference is available
- `SyncParcelPacket.handle()` now preserves `isBorderVisible`, `conflictState`, and `borderStoneY` from the existing `ClientParcelRegistry` entry when updating a parcel — prevents any parcel update (rename, sync) from inadvertently hiding an active border
- `ParcelPolygonOverlayFactory` — new `showPreviewOverlay()` / `clearPreviewOverlay()` methods for Foundation Stone JM preview; stored separately from `ACTIVE_OVERLAYS`
- `FoundationStoneEvents` — new `@OnlyIn(Dist.CLIENT)` event handler class that triggers the JM preview overlay on Foundation Stone right-click
- `StructurePolicyFactory` — lazy-built, invalidated on config reload; structure tag membership resolved via `holder.is(tagKey)`
- `isInProtectedDimension()` — centralised dimension protection check; uses blacklist semantics (empty exclusion list = all dimensions protected)
- `StringArgumentType.escapeIfRequired()` applied to all estate and parcel name suggestion streams so names with spaces are presented with quotes in the tab-complete list

---

## [2.1.0] - 2026-03-08

> **💡 Recommended:** Since this mod is heavily command-based, we recommend using [Chat Plus](https://modrinth.com/mod/chat-plus/version/2.7.0) for a better command history and larger chat window.

### 🎉 Highlights

- **JourneyMap integration** — claimed parcels now appear as coloured polygon overlays on the map
- **Parcel HUD** — a small overlay in the bottom-left corner shows which parcel and estate you are standing in
- **Formatted command responses** — all command and tool feedback is now consistently styled with colour, icons, and structured detail lines
- **Unique estate names** — default estate names are now always unique per player and never produce duplicates after demolishing and re-claiming

---

### ➕ Added

#### JourneyMap Integration (optional)
- Parcel boundaries rendered as coloured polygon overlays on the JourneyMap fullscreen map and web map
- Colour scheme matches estate type: Nation (blue), Citizen (light purple), Zone (yellow), Player (green)
- Your own parcels render in vivid colours; other players' parcels render in muted/desaturated versions of the same hue so the type is still identifiable
- Parcel and owner labels displayed on fullscreen and web maps; hidden on the minimap to reduce clutter
- Overlays update live when parcels are claimed or demolished — no map reload required
- Integration is fully optional: the mod loads and functions normally without JourneyMap installed

#### Parcel HUD Overlay
- Small semi-transparent panel in the bottom-left corner of the screen, sitting just above the hotbar
- Displays estate name, parcel name, owner, and parcel type while standing inside a claimed parcel
- Hidden automatically in wilderness, in spectator/F1 mode, and when the GUI is hidden (F1)
- Parcel type label is colour-coded to match the JourneyMap overlay and command output convention

#### Client-Side Parcel Caching & Networking
- Server now syncs parcel data to clients via a lightweight packet layer
- Client-side cache eliminates the block-break redraw glitch (blocks no longer visually break and reappear when protection cancels the event)
- Full parcel registry synced to clients on login and dimension change
- Per-parcel updates broadcast to nearby players when parcels are claimed or demolished

#### Unique Estate & Parcel Default Names
- Default estate names are now unique per player: `PlayerName-estate-1`, `PlayerName-estate-2`, etc.
- Default parcel names are scoped to their estate: `PlayerName-parcel-1`, `PlayerName-parcel-2`, etc.
- Counter is monotonically increasing — demolishing an estate and re-claiming never produces a duplicate name
- Counter persists across server restarts via NBT

---

### ⚙️ Changed

#### Formatted Command & Tool Responses
- All command responses now use a consistent visual style: colour-coded header, separator line, bold title, and grey detail body
- Success responses use ✔ green, failures use ✘ red, warnings use ⚠ yellow, info uses ℹ aqua
- Whitelist add/remove responses show the entry name and the estate it was applied to
- Multi-reason failure responses (e.g. join, relinquish) now display as a formatted bullet list rather than plain indented text
- Tool and item messages (`ZoningTool`, `CitizenTool`, Deeds) use the same formatting system as commands

#### Performance Improvements
- Added chunk-index pre-filter (`ParcelChunkIndex`) in front of the 3D interval tree — block events in unclaimed chunks now exit in O(1) without touching the BST
- `findByParcelId()` is now O(1) via a direct `UUID → Parcel` map (was O(n) linear scan)
- `findAllByEstateId()` and `findAllByNationEstateId()` are now O(1) via an `EstateID → Parcels` multimap (was O(n))

#### Dimension Validation
- All nine inline `OVERWORLD` dimension checks in the event handler extracted to a single `isInProtectedDimension()` helper
- Centralised in one place — ready for multi-dimension support in v2.2

---

### 🐛 Fixed

- Fixed `DemolishEstateSubCommand` having an extra incorrect `.suggests()` in its command chain
- Fixed default estate naming producing duplicate names after demolishing and re-claiming parcels
- Fixed `hasAccess()` and `hasInteractAccess()` duplicating identical parcel-resolution logic — now share a single `resolveParcelAt()` helper
- Fixed `syncAllParcelsToPlayer()` using the logging-in player's own name as the owner name for every parcel in the world

---

### 🗑️ Removed

- Removed `NATIONS_BY_ID` multimap from `ParcelRegistry` (deprecated since 2.0)
- Removed `abandonParcel()` and `updateOwner()` from `ParcelRegistry` (deprecated since 2.0)
- Removed `findByNationId()` from `ParcelRegistry` (deprecated since 2.0)
- Removed all deprecated constant fields from `CommandHelper`
- Removed `getNations()` / `getNationById()` from `ParcelRegistry` — moved to `EstateRegistry` as on-demand stream filters

---

### 🔧 Technical / Developer Notes

- New networking layer: `CMLNetwork`, `CacheSyncPacket`, `SyncParcelPacket`, `SyncAllParcelsPacket`, `RemoveParcelPacket`
- New client-side classes: `ClientParcel` (record), `ClientParcelCache`, `ClientParcelRegistry`, `ParcelRegionCache`
- New JourneyMap classes: `JourneyMapIntegration`, `JourneyMapOverlayHandler`, `ParcelPolygonOverlayFactory`
- New command helpers: `CommandResponseFormatter`, `PlayerMessageHelper`, `ParcelDisplayFormatter`, `WhitelistFormatter`
- New estate/parcel naming helpers: `EstateHelper`, `ParcelHelper`
- `EstateTypeRegistry` gains a `CopyFactory` for estate copying that does not carry over whitelists
- `AbstractEstate.copyFrom()` added for controlled field copying
- `PlayerRegistry` gains a per-player estate name counter with NBT persistence
- `Parcel` interface gains `nameAndRegister()` as the single commit point for naming, registration, and save
- `ParcelRegistry.register()` and `unregisterParcel()` gain `ServerLevel` overloads that broadcast network packets without breaking existing call sites

---

## [2.0.0] - 2026-03-02

> **💡 Recommended:** Since this mod is heavily command-based, we recommend using [Chat Plus](https://modrinth.com/mod/chat-plus/version/2.7.0) for a better command history and larger chat window.

### 🎉 Major Changes

#### Estate System Introduction
**Parcels** now represent physical land/space, while **Estates** represent ownership & access control.

- **Relationship:** One Estate → Many Parcels (one-to-many)
- **Key Concept:** Each Estate can contain multiple Parcels, and every Parcel belongs to exactly one Estate
- **Benefits:**
  - Group multiple Parcels under shared ownership and access rules
  - Parcels within the same Estate don't need to be adjacent
  - Apply whitelist changes to all Parcels in an Estate at once
  
### ⚙️ Changed

- **Command Structure Reorganization:**
  - Moved all whitelist sub-commands from `/parcel` to `/estate` commands
  - Restructured `/cml` whitelist command syntax for clarity
  - Renamed `abandon` command → `relinquish`

- **Relinquish Rules:**
  - Only Citizen Estates and Parcels can be relinquished
  - Designed for nation owners to set up pre-claimed parcels that players can claim

- **Massively Improved Display & Formatting:** 🆕
  - **Enhanced visual formatting with:**
    - Tree-structured hierarchical displays using box-drawing characters
    - Color-coded output for different whitelist types
    - One entry per line for better readability

  - **Better command output:**
    - `estate list` - Shows estates organized by type (Nation/Citizen/Regular)
    - `parcel list` - Displays parcels with estate relationships
    - `estate whitelist` - Shows all whitelist types in organized sections
    - Whitelist displays now show item counts and categories
    - Nation hierarchy view shows the full chain: Nation → Citizens → Parcels

- **Code Quality:**
  - Massive refactoring to support Estate system
  - Improved code organization and maintainability
  - Generic methods reduce code duplication across whitelist types

### ➕ Added

- **Entity Spawn Whitelist:** Control which entities can spawn in your parcels
- **Entity Spawn Tag Whitelist:**
  - Pre-populated with default allowable spawns (cows, chickens, eggs, buckets, etc.)
  - Fixes bug where neutral entities couldn't spawn in owned parcels

- **Tag-Based Whitelists:** 🆕
  - Block Tags: Allow entire categories of blocks (e.g., `#minecraft:planks`)
  - Item Tags: Allow entire categories of items (e.g., `#minecraft:swords`)
  - Entity Tags: Control entity categories (e.g., `#minecraft:skeletons`)
  - Much easier than adding items individually

- **Estate Commands:** Full suite of commands for managing Estates
  - Create, rename, delete estates
  - Transfer ownership between players
  - View detailed estate information
  - Manage all whitelist types at the estate level

- **Whitelist Display Commands:** 🆕
  - View any whitelist type: players, blocks, items, entities, or their tags
  - See all whitelists for an estate in one organized view
  - Categorized display makes finding entries easy
  - Summary view shows counts across all whitelist types

- **Rolling JSON Backup System**: Implemented automatic periodic saving of parcel data with configurable retention
  - Parcel data is now automatically saved to timestamped JSON files at regular intervals
  - Configurable save frequency (default: every 10 minutes)
  - Automatic cleanup of old save files, keeping only the N most recent backups (default: 20 files)
  - Added ability to manually trigger saves on-demand
  - Added recovery system to load from most recent backup on server startup
  - Save files are stored in `world/data/claimmyland/` directory with timestamp naming format: `parcels_YYYY-MM-DD_HH-MM-SS.json`
  - Provides protection against data loss from server crashes or corruption
  > **Note:** Backup settings (interval, retention countm flie path, etc) are currently hardcoded. Configuration options will be added in a future versions.
### Technical Details
- Introduced `RollingJsonSaver` utility class for generic rolling file management
- Backup system operates on server tick events with configurable intervals
- Uses Gson for JSON serialization/deserialization
- Automatic file cleanup prevents disk space issues from backup accumulation

---

## [1.3.0] - 2025-01-11

### ⚙️ Changed

- **Whitelist Inheritance:**
  - Zone Parcels now inherit their Nation Parcel's whitelist values
  - Includes: block/item tags, blocks/items, and friends lists

- **Border Stone Improvements:**
  - Now displays horizontal area blocks/highlights along with vertical border outlines
  - Better visualization of claimed territory

- **Player Name/UUID Resolution:**
  - Improved lookup order: online players → PlayerRegistry → Mojang API
  - More reliable player identification across sessions

- **Command Updates:**
  - `/cml parcel list` now shows parcels you have friend access to (displayed in grey with `*`)
  - Many commands updated to use the improved player lookup system
  - Re-enabled block placement denial messages when attempting to place blocks in claimed areas

### ➕ Added

- **Offline Player Support:**
  - Player lookup via Mojang API for offline players
  - Config option to enable/disable Mojang API checks

- **Friends Whitelist:**
  - Add trusted players who can access your parcels
  - Friends can break/place blocks and use items just like owners
  - Managed per-parcel

- **Granular Whitelists:**
  - Block Tag Whitelist: Allow entire categories of blocks
  - Block Whitelist: Allow specific blocks
  - Item Tag Whitelist: Allow entire categories of items
  - Item Whitelist: Allow specific items
  - All accessible via `/cml` commands

- **Visual Aids:**
  - Horizontal Area Blocks: Shows parcel boundaries on the ground (XZ plane)
  - Better visualization of your territory

- **Mod Integration:**
  - Added tag support for Macaw's Furniture

---

## [1.2.0] - 2025-02-27

### 🐛 Fixed

- Citizen Deeds can now properly claim parcels
- Out-of-world-limits check no longer triggers incorrectly
- Player Commands now display correctly in menu
- Citizen Deed's Nation ID now displays properly
- `/cml claimed_by` command now shows appropriate messages for:
  - Unclaimed land
  - Abandoned parcels

### ⚙️ Changed

- Updated GottschCore dependency range
- Mod events now short-circuit on client-side for better performance
- Border Stone bounding box/wireframe now matches actual block shape
- Temporarily removed Player whitelist commands (not functioning as intended - to be fixed in future update)

### ➕ Added

- **Whitelist System (Foundation):**
  - Block Tag Whitelist
  - Block Whitelist
  - Item Tag Whitelist
  - Item Whitelist
  - Commands to modify all whitelist types

- **Common Tags:**
  - Pre-configured block and item tags available for quick whitelist setup

- **Protection Events:**
  - Item usage events prevent non-whitelisted items from being used in claimed parcels

- **Localization:**
  - Added Border and Buffer block names to language file

- **Mod Integration:**
  - Out-of-box block/item tag integration with:
    - Treasure2
    - MageFlame
    - Legacy Vault

---

## [1.1.0] - 2024-11-28

### ⚙️ Changed

- **Protection System:** All protection events are now active and functional
- **Foundation Stones:** Now properly orient to face the player when placed
- **Documentation:** Fixed Patchouli book landing page
- **Metadata:** Fixed homepage link in `update.json`

---

## [1.0.0] - 2024-10-27

### 🎉 Initial Release

- Beta release with core functionality
- Land claiming system with Parcels
- Basic protection mechanics
- Foundation and Border Stones
- Initial command structure