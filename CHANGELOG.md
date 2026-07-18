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

> **💡 Recommended:** Since this mod is heavily command-based, we recommend using 🔗 [Chat Plus](https://modrinth.com/mod/chat-plus/version/2.7.0) for a better command history and larger chat window.

---

## [2.5.3] - 2026-07-18

### 🐛 Fixed

- **Dedicated server crash on launch when JourneyMap is installed server-side** —
  `CommonSetup` only checked whether the JourneyMap mod was present before
  initializing Claim My Land's JourneyMap integration, not whether it was
  running on the physical client. Since JourneyMap supports being installed on
  a dedicated server for its own server-side features, this let the
  integration attempt to load a client-only class (`JourneyMapOverlayHandler`)
  during `common_setup` on the server, crashing it immediately. The
  integration now also checks that it is running on the physical client
  before initializing, so JourneyMap can be installed server-side without
  crashing the server.

---

## [2.5.2] - 2026-04-18

### ➕ Added

- **Client config option `showParcelHud`** (under `GUI` category) — toggle the
  in-world parcel HUD overlay on/off. Default `true` (preserves prior behavior).

### 🐛 Fixed

- **Client config key `enableProtectionChatMessages` malformed** — the define
  key string had a stray trailing colon, producing an invalid TOML path. Users
  who had previously set this to `true` will find it reset to the default
  (`false`) on first load after updating; re-enable from the config file if
  desired. Orphaned entries under the old key are harmless and can be deleted.

---

## [2.5.1] - 2026-04-09

### 🎉 Highlights

- **Cold-start red borders fixed** — parcel borders no longer render red for the owning
  player on a fresh server + client start when nested parcels exist inside a Nation.
- **Overlapping parcel claim prevented** — placement tools (CitizenTool, ZoningTool) now
  correctly block claiming a parcel that visually shows as conflicting with an existing
  sibling parcel. Previously the conflict preview was client-side only and the server
  allowed the claim through.
- **Placement blocks preserved on failed Zone claim** — Zone placement blocks are no
  longer removed when a Zone claim fails due to conflict. Players can reposition and
  retry without replacing the blocks.

---

### 🐛 Fixed

- **Committed parcel borders render red on cold server + client start** — `onChunkWatch`
  was calling `resolveConflictState()` on already-committed parcels when sending border
  visibility to joining players. `resolveConflictState()` is designed for Foundation Stone
  placement preview only; when called on committed parcels with nested children (e.g.
  Zones and Citizens inside a Nation), it incorrectly returned conflict `1` because the
  child parcels overlap the parent's box without being the same parcel. Fix: pass
  `conflictState=0` unconditionally for committed border stone visibility in `onChunkWatch`.
  Preview parcels (Foundation Stone not yet claimed) still call `resolveConflictState()`
  correctly since they are handled by `FoundationStoneBlockEntity` events, not
  `onChunkWatch`. Likely also present in Forge 1.20.1 branch.

- **Overlapping sibling parcel claim allowed via CitizenTool and ZoningTool** —
  `claimWithinZone()` in `CitizenParcel` and `PlayerParcel`, and `handleEmbeddedClaim()`
  in `ZoneParcel`, were missing a direct box overlap check against sibling parcels. Only
  buffer zone checks were performed, so a directly overlapping same-owner same-type
  sibling was never caught — `ParcelConflictResolver.isConflict()` returns `false` for
  same-owner same-type pairs, and the buffer check alone does not cover direct overlap.
  Fix: added `ParcelHelper.checkDirectSiblingOverlap()` and called it from all three
  claim paths before the buffer checks. The helper takes an `excludeZones` parameter —
  `true` for Citizen/Player (zone is a valid parent, not a conflict), `false` for Zone
  (sibling zones inside a Nation must be checked against each other). Likely also present
  in Forge 1.20.1 branch.

- **Zone placement blocks removed on failed claim** — `ZoningTool.handleZoneCreation()`
  called `clear()` unconditionally after `tryCreateZoneParcel()` regardless of whether
  the claim succeeded or failed. Fix: `tryCreateZoneParcel()` now returns
  `InteractionResult.CONSUME` on success and `InteractionResult.SUCCESS` on failure;
  `handleZoneCreation()` only calls `clear()` and removes item tag coords on `CONSUME`.
  Players whose Zone claim fails due to conflict now retain their placement blocks and
  receive an appropriate failure message.

- **Citizen placement blocks removed on failed claim** — `CitizenTool.tryCreateCitizenParcel()`
  always returned `InteractionResult.SUCCESS` regardless of `ClaimResult`, and
  `handleParcelCreation()` cleared placement blocks and item tag coords unconditionally
  after the call. With the v2.5.1 `checkDirectSiblingOverlap()` fix now surfacing real
  failure results from `claimWithinZone()`, this caused players to lose both Citizen
  placement blocks on a failed claim. Fix: `tryCreateCitizenParcel()` now returns
  `InteractionResult.CONSUME` on success and `InteractionResult.SUCCESS` on failure;
  `handleParcelCreation()` only calls `clear()` and removes `COORDS1`/`COORDS2` on
  `CONSUME`. Mirrors the v2.5.1 ZoningTool fix. Likely also present in Forge 1.20.1 branch.

---

### 🔧 Technical Notes

- `onChunkWatch` iterates `ActiveBorderStoneRegistry` which tracks Border Stones only —
  Foundation Stone preview parcels never appear here, so the unconditional
  `conflictState=0` is safe for all parcels reached via this path.
- `ParcelHelper.checkDirectSiblingOverlap()` mirrors the direct overlap logic in
  `Parcel.handleClaim()` for top-level parcels, closing the parity gap between top-level
  and embedded claim validation.
- `excludeZones=true` is the safe default for the convenience overload — Citizen and
  Player parcels should never treat a containing Zone as a sibling conflict.
- `ZoningTool.tryCreateZoneParcel()` previously always returned `InteractionResult.SUCCESS`
  regardless of `ClaimResult` — the distinction between success and failure now relies on
  `CONSUME` vs `SUCCESS` so callers can guard cleanup correctly.

---

*Requires GottschCore v2.6 or later.*

---
## [2.5.0] - 2026-03-28

### 🎉 Highlights

- **Backup config hot-reload** — changes to backup settings in `claimmyland-server.toml` now take effect immediately without a server restart.
- **Claim celebration fireworks** — committing a claim now launches firework rockets at the parcel centre, visible to all nearby players. Rocket count and color scale with parcel type.
- **Foundation Stone centering** — the Foundation Stone is now placed at the XZ centre of the parcel by default, making claim placement more intuitive.
- **Farmland trample & chorus fruit protection** — two new protection gaps closed: players and mobs can no longer trample farmland inside foreign parcels, and chorus fruit teleportation into protected parcels is blocked.
- **CML OpsList** — specific players can now be granted CML ops-level permission via UUID list in config, without requiring server op status.
- **Deeds as Loot** — Deed items now appear in vanilla dungeon, stronghold, nether, and end loot tables, and in Treasure2 chest tiers, allowing land claiming to emerge organically through gameplay.

---

### ➕ Added

#### Backup Config Hot-Reload
- `RollingJsonSaver` is now reinitialized when `claimmyland-server.toml` is reloaded at runtime.
- `ModConfigEvent.Reloading` handler added to `ClaimMyLand` — calls `reinitBackup()` when the CML config is reloaded.
- If backup is disabled in config the saver is set to null; existing null-guards in `BackupSubCommand` and the tick handler handle this safely.

#### Claim Celebration Fireworks
- Committing a claim now spawns firework rockets at the parcel centre, visible to all nearby players via standard entity replication.
- Rocket count scales with parcel type: 1 for CITIZEN/PLAYER, 2 for ZONE, 3 for NATION.
- Rocket burst color is type-coded: NATION → blue (`0x00AAFF`), CITIZEN → purple (`0xAA55FF`), ZONE → yellow (`0xFFFF55`), PLAYER → green (`0x55FF55`).
- Rockets spawn at the surface heightmap position above the parcel centre (not at the parcel floor) to ensure visibility.
- New config toggle: `Config.SERVER.celebration.fireworksEnabled` (default: `true`). Disable on busy servers where fireworks are unwanted.
- New class: `CelebrationHelper` in `core.util`.

#### Foundation Stone Centering
- The Foundation Stone is now placed at the XZ centre of the parcel box by default.
- Odd-sized deeds floor the half-offset (e.g. a 15-wide deed places 7 blocks left of stone, 8 right).
- Legacy behaviour (stone = min corner) is available via `Config.SERVER.general.foundationStoneCentered = false`.
- Single change point: `FoundationStoneBlockEntity.getAbsoluteBox()` — all deed types benefit automatically.
- Existing committed parcels are unaffected — their absolute coords are already stored and are not recalculated.
- `Deed.useOn()` updated: after `getAbsoluteBox()`, the parcel's coords and size are synced to the centered absolute box via `applyWorldPosition()` before claim validation.
- `handleClaim()` and `handleEmbeddedClaim()` `Box` parameter removed — callers now use `parcel.getBox()` internally. Call sites simplified.

#### Additional Protection Events
- **Farmland trample**: `BlockEvent.FarmlandTrampleEvent` handler added to `ModEvents`. Prevents players and mobs from trampling farmland inside protected parcels. Config flag: `enableFarmlandTrampleEvent` (default: `true`).
- **Chorus fruit teleport**: `EntityTeleportEvent.ChorusFruit` handler added to `ModEvents`. Prevents players from teleporting into protected parcels via chorus fruit. Config flag: `enableChorusFruitTeleport` (default: `true`).

#### CML OpsList
- New config entry `opsList` in `Config.SERVER.general` — a list of player UUID strings granted CML ops-level permission without requiring server op status.
- `ModEvents.hasOpsPermission()` updated to check the list.
- `/cml-ops` Brigadier `requires()` predicate updated to check the list.
- Management command (`/cml-ops opslist`) deferred to v2.6 — direct config file editing is sufficient for v2.5.

#### Deeds as Loot — Vanilla
- Deed items now appear in 10 vanilla loot tables via Forge Global Loot Modifier.
- Loot is gated by `Config.SERVER.general.enableDeedLoot` (default: `true`).
- Four tiers with tuned chance and deed pool:
  - **Common** (chance 0.15): simple dungeon, nether bridge, village weaponsmith, fishing treasure → `player_deed_10/16/32`
  - **Uncommon** (chance 0.12): stronghold corridor, bastion treasure → `player_deed_10/16/32`
  - **Rare** (chance 0.08): stronghold library, woodland mansion → `player_deed_16/32`, `nation_deed_100`
  - **Epic** (chance 0.05): end city treasure, ancient city → `player_deed_32`, `nation_deed_100`
- Higher tier chests have higher deed chances (harder to find = more reliable reward when opened).
- Loot deeds have no owner set — the using player becomes the owner at claim time.
- New classes: `DeedLootModifier`, `ModLootModifiers` in `core.loot`.
- New data files: `deed_loot_common/uncommon/rare/epic.json` under `data/claimmyland/loot_modifiers/`.
- `DeedFactory.createTieredDeed(RandomSource, String tier)` added.
- `DeedFactory.createNationDeedForLoot()` added — creates a full world-height nation deed without requiring a `Level` reference.

#### Deeds as Loot — Treasure2 Integration
- Deed items now inject into Treasure2 chest loot tables for servers running Treasure2.
- Six tiers mapped to CML deed pools (Treasure2 `common` excluded — too low tier for deeds):
  - `uncommon` → common pool (chance tuned)
  - `scarce` → uncommon pool
  - `rare` → rare pool
  - `epic` → epic pool
  - `legendary` → epic pool (higher chance than standard epic)
  - `mythical` → epic pool (highest chance)
- Inject files under `data/treasure2/loot_tables/injects/chests/` prefixed with `cml_` (e.g. `cml_uncommon.json`).

---

### ⚙️ Changed

- `@Deprecated(since="2.4")` dimension-blind `ParcelRegistry` overloads removed — all call sites now use dimension-aware signatures. Affected methods: `find()`, `findBuffer()`, `findBoxes()`, `intersectsParcel()`, `resolveConflictState()`, `findLeastSignificant()`, `findMostSignificant()`, `isFireSpreadPrevented()`, `findRaw()`, `findBufferRaw()`, `resolveParcelAt()`.
- `FoundationStoneBlockEntity.getAbsoluteBox()` — centering logic added, guarded by `Config.SERVER.general.foundationStoneCentered`.
- `Deed.useOn()` — `applyWorldPosition()` called after `getAbsoluteBox()` to sync parcel coords/size to the centered absolute box before claim validation. `handleClaim()` / `handleEmbeddedClaim()` no longer receive a `Box` parameter.
- `ModEvents.onLivingDestroyBlock()` — fixed missing `event.setCanceled(true)` call. Previously the dimension and intersection checks ran but never actually cancelled the event.
- `ModEvents.onSpawnEntity()` — `isEntityWhitelisted()` extracted as a private helper method for clarity and reuse.
- `ClaimMyLand` constructor — `ModLootModifiers.register(modEventBus)` added.
- Conflict resolution centralized into `ParcelConflictResolver.isConflict()` — single authoritative conflict determination replaces duplicated logic across five code paths (`handleClaim()`, `resolveConflictState()`, `ClientParcelRegistry.findConflicting()`, `FoundationStoneEvents`, and `DemolishParcelSubCommand`). Four-rule priority: hierarchical → foreign owner → same-owner sibling → same-owner cross-type.
- Buffer conflict checks are now bidirectional — both "does existing parcel's buffer reach proposed box" (Rule 2a) and "does proposed parcel's buffer reach existing parcel box" (Rule 2b) are checked during claim and conflict state resolution.
- Nation tenant cleanup on demolish centralized into `CommandHelper.cleanupNationTenants()` — `DemolishEstateSubCommand` and `DemolishParcelSubCommand` now use a shared helper that finds tenants via `ParcelRegistry.findAllByNationEstateId()` and properly unregisters both parcels and estates.

---

### 🐛 Fixed

- `ModEvents.onLivingDestroyBlock()` — event was never cancelled even when a mob attempted to destroy a block inside a protected parcel. `event.setCanceled(true)` now called correctly.
- Nation Foundation Stone placement blocked in wilderness — `NationParcel.canPlaceAt()` now checks for empty wilderness directly, bypassing the default embedded placement check that always failed for Nations.
- Nation parcel not removed from registry on demolish — `DeedFactory.createNationDeed()` mutated the live parcel's size Box in-place, shifting the `PARCELS_BY_COORDS` map key so `unregisterCoords()` silently failed. Fixed by creating a defensive copy of the size Box before modifying Y values.
- Nation demolish did not remove tenant parcels (Citizens, Zones) — `DemolishEstateSubCommand.demolish()` only iterated the Nation estate's own parcels, leaving orphaned tenants in the registry and NBT.
- Foundation Stone preview inside foreign Nation/Zone could not be broken by the placing player — `ModEvents.onBlockBreak()` now exempts Foundation Stones when the breaker is the block entity's owner.

---

## [2.4.0] - 2026-03-26

* 🔗 Requires [GottschCore](https://www.curseforge.com/minecraft/mc-mods/gottschcore) v2.6 or later.*

### 🎉 Highlights

- **Multi-dimension support** — parcels are now fully dimension-aware. Protection works correctly in the Nether, End, and modded dimensions.
- **Parcel entry title** — a title overlay announces the estate name when crossing into a Nation, Citizen, or Player parcel, with a configurable cooldown to prevent spam.
- **Claim celebration** — committing a claim triggers a perimeter particle wave visible only to the claiming player.
- **Foundation Stone particles** — placing a Foundation Stone emits a dust displacement effect radiating outward from the stone.
- **Nation player blacklist** — nation owners can now block specific players from claiming land within their nation, with a generic denial message that protects the owner's privacy.

---

### ➕ Added

#### Multi-Dimension Support (BST Dimension Refactor)
- Parcels are now fully dimension-aware in the internal spatial index (BST). Previously all parcel lookups were dimension-blind and relied on post-filtering, which could cause cross-dimension false-positives.
- The Foundation Stone, Border Stone, and all protection events now correctly resolve parcels per-dimension, enabling reliable parcel protection in the Nether, End, and modded dimensions.
- GottschCore `CoordsInterval` updated: `dimension` is now a first-class field and the primary sort key in the BST.
- Fixed a pre-existing bug in `CoordsIntervalTreeNBTSerializer` where the RIGHT child was incorrectly wired to the LEFT slot during deserialization.

#### Parcel Entry Title Display
- When crossing into a Nation, Citizen, or Player parcel, a title overlay is now displayed using Minecraft's vanilla title system.
- Nation parcels show the estate name as a large bold title with type and owner as a subtitle.
- Citizen and Player parcels show a smaller subtitle only, visually distinguishing them from Nation announcements.
- Zone parcel boundaries and wilderness transitions are silent.
- A per-parcel cooldown (default 30 seconds) prevents repeated title spam when crossing the same border multiple times.
- Server master switch: `enableParcelEntryTitle` in `claimmyland-server.toml` (default: `true`).
- Client opt-out: `enableParcelEntryTitle` in `claimmyland-client.toml` (default: `true`).
- Timing is configurable: fade-in, stay, and fade-out ticks all adjustable in client config.
- Cooldown clears on dimension change and logout so familiar parcels re-announce after a context switch.

#### Foundation Stone Placement Particles
- Placing a Foundation Stone now emits a dust displacement particle effect.
- Phase 1: 24 `POOF` particles radiate outward at ground level from the stone's position.
- Phase 2: 10 `SMOKE` particles rise from the stone's centre, simulating displaced air.

#### Claim Celebration Effects
- Successfully committing a claim now triggers a perimeter particle wave visible only to the claiming player (intentionally private — avoids revealing position on PvP servers).
- For small parcels (perimeter ≤ 128 blocks): particles emit along the full XZ perimeter.
- For large parcels (perimeter > 128 blocks): particles emit for up to 12 blocks outward from the Foundation Stone along each of the four edges.
- Each perimeter position emits `HAPPY_VILLAGER` (rising) and `POOF` (radiating outward) particles.

#### Nation Player Blacklist
- Nation owners can now blacklist specific players from claiming land within their nation, even if those players possess a valid Deed.
- Blacklisted players receive a generic denial message that does not reveal they are blacklisted or identify the nation, preserving the nation owner's privacy.
- The nation owner is always exempt from their own blacklist.
- Blacklist blocks all claim paths: Deed use on a Foundation Stone, CitizenTool parcel creation, and ZoningTool zone creation.
- New commands:
  - `/cml estate blacklist add <estateName> <playerName>` — add a player to the blacklist
  - `/cml estate blacklist remove <estateName> <playerName>` — remove a player from the blacklist
  - `/cml estate blacklist list <estateName>` — view the blacklist (output sent privately to the command sender only)
- Blacklist is displayed in estate details (`/cml estate details`) for nation owners.
- Blacklist count and add/remove icons shown in the estate list view for nation estates.

#### Server Config Sync
- A new `ServerConfigSyncPacket` now syncs selected server config values to clients on login and dimension change.
- Currently synced values: `parcelBufferRadius`, `nationParcelBufferRadius`, `enableParcelEntryTitle`.
- Ensures client-side features (JourneyMap buffer overlay, parcel entry title) always reflect the current server configuration.

### ⚙️ Changed

- `EstateDisplayFormatter`: Nation estate entries now show a Blacklisted Players section in the detailed view, and a blacklist count with add icon in the list view.
- `FormatterConstants`: Fixed `/cml-ops` icon builders — they were incorrectly using `/cml` commands instead of `/cml-ops` commands.
- `FormatterConstants`: Fixed `playerWhitelistAddIcon()` and `playerWhitelistRemoveIcon()` — command paths were incorrect (`whitelist add player` → `whitelist friends add/remove`).
- `FormatterConstants`: Added ops variants for all estate icon builders (`estateRenameIconOps`, `estateDemolishIconOps`, `estateRemoveIconOps`, `estateTransferIconOps`, `playerWhitelistAddIconOps`, `playerWhitelistRemoveIconOps`).
- `FormatterConstants`: Added blacklist icon builders (`playerBlacklistAddIcon`, `playerBlacklistRemoveIcon`, and their ops variants).
- `FormatterConstants`: Teleport icon changed from `↗` (small, hard to read) to `➤` (solid arrowhead, more visible at Minecraft chat font size).
- `WhitelistFormatter`: Added `formatEstateListPlayerBlacklist()` to display blacklisted players with correct remove icons and ops/non-ops command variants.
- `ParcelRegistry`: All `find()`, `findBuffer()`, `findBoxes()`, `intersectsParcel()`, `resolveConflictState()`, `findLeastSignificant()`, `findMostSignificant()`, `isFireSpreadPrevented()`, `resolveParcelCached()`, and `syncParcelToClient()` methods now have dimension-aware overloads. Old dimension-blind signatures are retained but deprecated.

### 🐛 Fixed

- Fixed `CoordsIntervalTreeNBTSerializer` RIGHT child deserialization bug — the RIGHT child was being wired to the LEFT slot, corrupting the in-memory BST structure after a world load.
- Fixed `/cml-ops` estate action icons firing `/cml` commands instead of `/cml-ops` commands.
- Fixed Friends Whitelist add/remove icon commands using wrong command path.
- Fixed cross-dimension parcel false-positives — parcels in the Nether/End could incorrectly match queries in the Overworld due to the dimension-blind BST.

### 🔧 Technical Notes

- `BlockEvent.EntityPlaceEvent` is server-side only in Forge 1.20.1 — Foundation Stone placement particles are triggered via `SyncParcelPacket.handle()` instead (on new preview arrival).
- Fluid spread prevention was investigated but no reliable cancellable Forge 1.20.1 event exists for fluid flow — deferred to v2.5.
- Fire and lava-ignited fire prevention was confirmed already working via the existing `BlockEvent.EntityPlaceEvent` + `isFireSpreadPrevented()` path and `ModTags.Blocks.FIRE_BLOCKS`.

---

* 🔗 Requires [GottschCore](https://www.curseforge.com/minecraft/mc-mods/gottschcore) v2.6 or later.*

---

## [2.3.0] - 2026-03-22

### 🎉 Highlights

#### JourneyMap — Parcel Hover Tooltip
- Hovering the cursor over a claimed parcel on the JourneyMap fullscreen map now displays
  a tooltip showing estate name, parcel type, owner, parcel name (if different from estate
  name), nation name (Zone and Citizen parcels only), and parcel dimensions in blocks.

#### JourneyMap — Buffer Zone Overlay
- Each parcel polygon on the JourneyMap now shows a second outer ring representing its
  buffer zone boundary, matching the in-world buffer bracket visual.
- Buffer ring color matches conflict state: white when clear, red when conflicting.

#### JourneyMap — Conflict Color
- Parcels with an active conflict state now render in red on the JourneyMap overlay,
  matching the in-world wireframe conflict color.

#### JourneyMap — Foundation Stone Conflict Overlays
- When a Foundation Stone preview conflicts with an existing parcel, the conflicting
  parcel(s) are highlighted in orange on the JourneyMap, showing exactly what the
  proposed claim is bumping into.

#### In-World — Conflict Highlight Rendering
- Conflicting parcels are also highlighted in orange in the world-space border renderer
  when a Foundation Stone conflict is detected.
- Highlights auto-clear when the player moves more than 32 blocks away from the Foundation
  Stone or after a configurable timeout (default 60 seconds,
  `Config.CLIENT.rendering.conflictHighlightTimeoutSeconds`).

#### In-World — Quad-Tube Wireframe Borders
- Parcel border wireframes are now rendered using quad-tube geometry instead of GL lines.
- Provides true controllable border thickness independent of GPU driver `lineWidth` clamping.
- Preview borders pulse in both alpha and tube width.

#### Interactive Chat Buttons
- Estate and parcel listings in chat now include clickable icon buttons for common actions.
- Estate icons: `ℹ` details, `✎` rename, `✘` demolish/remove, `⇄` transfer ownership.
- Parcel icons: `↗` teleport, `✘` demolish.
- Player whitelist icons: `✚` add, `✘` remove per entry.
- Clicking a non-destructive action (details, teleport) runs the command immediately.
- Clicking a destructive or multi-step action (demolish, rename, transfer) pre-fills the
  chat bar so the player must press Enter to confirm.

#### On-Demand Backup Command
- New ops command `/cml-ops backup` triggers an immediate parcel data backup outside the
  normal auto-save interval.
- Reports the filename of the backup written on success.
- Reports a clear error if the backup system is disabled in config.

### ⚙️ Changed

- **JourneyMap preview overlay color** — Foundation Stone preview parcels now render in
  the parcel type's own color at reduced opacity on the JourneyMap, making it easier to
  distinguish preview overlays from committed parcels. Conflict previews remain red.
- **Preview/minimap consistency** — Preview overlays now appear on both the minimap and
  fullscreen map immediately when a Foundation Stone is placed, without needing to open
  the fullscreen map first.
- **Whitelist display** — Player whitelist entries in estate detail output are now shown
  one per line (previously multi-per-row), enabling per-entry remove icons.

### 🐛 Fixed

- **Preview conflict false-positive** — Two players placing overlapping Foundation Stones
  simultaneously no longer incorrectly show each other's preview as a conflict. Only
  committed parcels trigger conflict detection; previews are excluded.

### 🔧 Technical / Developer Notes

- `RollingJsonSaver.save()` return type changed from `void` to `Optional<File>`.
  Auto-save tick path discards the return value; on-demand backup command uses it to
  report the saved filename.
- `ClientParcel` record: added `nationName` field (non-null for Zone and Citizen parcels).
- `SyncParcelPacket` and `CacheSyncPacket`: encode/decode `nationName`.
- `ClientParcelRegistry`: added `findConflicting()`, `hoveredParcel` volatile bridge
  field, and `get`/`set` accessors for the tooltip renderer.
- `JourneyMapOverlayHandler`: subscribed to `MAP_MOUSE_MOVED`; writes hovered parcel to
  `ClientParcelRegistry` bridge on each cursor move.
- New `ServerConfigSyncPacket` (S→C, packet ID 5): syncs `parcelBufferRadius` and
  `nationParcelBufferRadius` from server config to client on login, enabling correct
  buffer zone sizing in `ParcelPolygonOverlayFactory` and `ParcelBorderRenderer`.
- New `ClientServerConfig`: client-side holder for synced server config values.
- New `DimensionHelper`: shared utility for converting dimension ID strings to
  `ResourceKey<Level>`.
- New `ParcelMapTooltipRenderer`: `ScreenEvent.Render.Post` subscriber that draws the
  JM hover tooltip above the fullscreen map screen.
- `ParcelBorderRenderer`: replaced `RenderType.lines()` wireframe with custom quad-tube
  geometry via lazily-initialised `TUBE_RENDER_TYPE`; added orange conflict highlight
  render pass driven by `CONFLICT_HIGHLIGHT_IDS`.
- `ParcelPolygonOverlayFactory`: added `BUFFER_OVERLAYS`, `CONFLICT_OVERLAYS`,
  `CONFLICT_BUFFER_OVERLAYS` maps; added `buildBufferOverlay()`, `showConflictOverlays()`,
  `clearConflictOverlays()`; updated `buildOverlay()` for conflict color and preview color.
- `Config.CLIENT.rendering`: added `conflictHighlightTimeoutSeconds` (default 60, range 10–300).

## [2.2.0] - 2026-03-19
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