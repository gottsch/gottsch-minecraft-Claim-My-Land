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