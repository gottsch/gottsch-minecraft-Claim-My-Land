# Changelog for Claim My Land 1.20.1

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0] - 2025-03-02

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