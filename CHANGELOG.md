# Changelog for Claim My Land 1.20.1

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.0] - 2025-

### Changed
- Zone parcel inherits Nation parcel's whitelists (block/item tags, blocks/items, friends) values.
- Border Stones now display horizontal area blocks/highlight along with the border blocks outline.
- updated Player name/uuid check - first looks for online players, then in the PlayerRegistry, then from Mojang API.
- update the /cml parcel list command to include parcels that the player is a friend of. these are shown in grey with an (*).
- many commands were updated to use the new Player check.
- re-enabled block placed denial message when attempting to place block on claimed area.

### Added
- offline Player check from Mojang API
- config option to enable/disable Mojang API check
- friends whitelist - players that can access/use the owner's parcel just like the owners ie. break/place blocks, use blcoks/items etc.
- blockTag, block, itemTag, and item whitelists for parcels available from player command (/cml).
  - whitelists allow blocks and items to be used within claimed parcels.
- Horizontal Area blocks - displays parcel boundaries on the XZ plane.
- Tag for Macaw's Furniture.

## [1.2.0] - 2025-02-27

### Changed

- fixed citizen deeds not able to claim parcels.
- fixed out-of-world-limits check bug.
- updated gottschcore range.
- fixed Player Commands not showing up.
- fixed Citizen Deed's Nation ID not displaying.
- fixed /cml claimed_by command to display message if land is not claimed, and if parcel is abandoned.
- mod events short-circuit true on client-side.
- changed bounding box / wireframe for Border Stone to match actual shape.
- temporarily remove Player whitelist commands. these are not functioning as intended.

### Added
- blockTag, block, itemTag, and item whitelists for parcels.
  - whitelists allow blocks and items to be used within claimed parcels.
- commands to modify the whitelists.
- common blockTags and itemTags that can be used from commands.
- item event to prevent items not to be used in claimed parcels (except for whitelisted ones).
- blocks names for Border and Buffer blocks to lang file.
- out-of-box block/item tag integration with Treasure2, MageFlame and Legacy Vault.


## [1.1.0] - 2024-11-28

### Changed

- activated all the events that need protection!
- Foundation Stones should now face the player when placed.
- fixed landing page of Patchouli book.
- fixed homepage link in update.json

## [1.0.0] - 2024-10-27

### Added 

- Initial release as beta.