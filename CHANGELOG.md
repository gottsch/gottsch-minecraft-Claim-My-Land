# Changelog for Claim My Land 1.21.1

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

# Claim My Land — Changelog

## [2.7.0] - 2026-04-22

### 🎉 Highlights

- **Parcel rename via Border Stone** — right-click any Border Stone with any
  sign variant to open the sign editor pre-filled with the current parcel name.
  All four lines are concatenated into the new name. Sign is consumed on submit.
  Owner only.
- **Parcel rename via Iron Name Tag** — name a tag in an anvil, then right-click
  a Border Stone to instantly rename the parcel. Tag consumed on success.
- **Estate rename via Gold Name Tag** — name a tag in an anvil, then right-click
  a Border Stone to rename its estate across all parcels that share it. Tag
  consumed on success.
- **Parcel and estate rename persistence fixed for Nether/End** — a pre-existing
  bug caused all persistence operations (rename, claim, demolish, etc.) to
  silently fail for players in the Nether or End. Fixed centrally in
  `CommandHelper.save()`.
- **`/cml-ops opslist` management command** — add, remove, and list CML ops
  directly in-game without editing `claimmyland-server.toml`.
- **Client-side double cancel fix** — deeds placed inside a protected parcel
  no longer fire a cosmetic double cancel on the client.
- **Enderpearl teleport protection** — opt-in server config to block enderpearl
  teleports into protected parcels. Nations always bypass. Default: off.
- **Deeds as Loot** — deeds appear as chest loot in YUNG's
  Better Dungeons, and Twilight Forest structures now. Configurable via
  `enableDeedLoot` server config. Four tiers: common, uncommon, rare, epic.

---

### ➕ Added

#### Parcel rename via Border Stone (sign)
- Right-click any Border Stone while holding any sign variant (any wood type).
- Sign editor opens on the adjacent face of the Border Stone, pre-filled with
  the current parcel name on line 1.
- Placement face: clicked face used if horizontal and unobstructed; falls back
  through NORTH → SOUTH → EAST → WEST; failure message if all four blocked.
- All non-empty sign lines concatenated with spaces → new parcel name.
- Sign consumed on submit (not in creative mode). Owner only.
- JM overlay updates live. HUD updates immediately if owner is standing in the
  parcel; otherwise updates on re-entry.
- New blocks: `CMLRenameSignBlock`, `CMLRenameSignBlockEntity`.
- New lang keys: `parcel.rename.sign.no_space`, `parcel.rename.sign.empty`,
  `parcel.rename.sign.not_owner`, `parcel.rename.sign.success`.

#### Iron Name Tag — parcel rename
- New item `IronNameTagItem` (`claimmyland:iron_name_tag`).
- Crafted: vanilla name tag + iron nugget (shapeless).
- Obtainable via `/cml give iron_name_tag`.
- Must be named in an anvil before use — failure message if unnamed.
- Right-click a Border Stone to rename the parcel to the tag's name.
- Tag consumed on success. Owner only.
- New lang keys: `parcel.rename.tag.not_named`, `parcel.rename.tag.not_owner`,
  `parcel.rename.tag.success`.

#### Gold Name Tag — estate rename
- New item `GoldNameTagItem` (`claimmyland:gold_name_tag`).
- Crafted: vanilla name tag + gold nugget (shapeless).
- Obtainable via `/cml give gold_name_tag`.
- Must be named in an anvil before use — failure message if unnamed.
- Right-click a Border Stone to rename its estate. Rename applies to all parcels
  sharing that estate simultaneously.
- Tag consumed on success. Owner only.
- New lang keys: `estate.rename.tag.not_named`, `estate.rename.tag.not_owner`,
  `estate.rename.tag.success`.

#### `/cml give` — new items
- `iron_name_tag` and `gold_name_tag` added to the give command suggestion list
  and switch cases.

#### `/cml-ops opslist` management command
- New subcommand group under `/cml-ops`: `opslist list`, `opslist add <player>`,
  `opslist remove <player>`.
- `list` — chat display with ✚ add / ✘ remove interactive icons per entry.
  UUIDs shown as hoverable 12-char suffixes via `FormatterConstants.hoverableUuid`.
- `add` — suggests online players; falls back to PlayerRegistry → Mojang-cache
  UUID resolution for offline players.
- `remove` — suggests current ops list members by name; UUID resolved per entry.
- Self-removal permitted (matches vanilla `/deop` semantics).
- Malformed UUIDs in config are logged and skipped from display and suggestions.
- New files: `OpsListSubCommand.java`, `OpsListFormatter.java`.
- New lang keys: `opslist.add.success`, `opslist.add.already_op`,
  `opslist.remove.success`, `opslist.remove.not_op` (title + body pairs).

#### `FormatterConstants.hoverableUuid(UUID)`
- New static helper that displays the trailing 12 characters of a UUID with the
  full UUID on hover. Canonical way to show parcel/estate/player UUIDs compactly
  in chat output. Used by `OpsListFormatter`.

#### Client-side double cancel fix
- `ClientEvents.onPlayerInteractBlock` now bypasses the protected-parcel cancel
  when the player is holding a `Deed` in either hand.
- Covers the legitimate case of a non-owner placing a deed inside someone else's
  Nation — `canPlaceAt` / `PlacementResult` on the server handles actual
  permission; the client cancel was purely cosmetic interference.
- Scope is Deed-only: other CML items (`CitizenTool`, `ZoningTool`, name tags)
  are owner-only by design and never trigger the cancel in their valid use case.

#### Enderpearl teleport protection
- New server config boolean `enableEnderpearlTeleport` (default: `false`).
- When enabled, blocks enderpearl teleports into Zone, Citizen, and Player
  parcels unless the teleporting player is the owner, on the whitelist, or a
  CML op.
- Nation parcels always bypass — Nations are public-transit by design.
- Guard order mirrors `onChorusFruitTeleport`: server-side check → feature flag
  → chunk pre-filter → dimension guard → ops bypass → Nation bypass →
  `hasAccess` gate.
- New lang key: `teleport.enderpearl.blocked`.

---

### 🐛 Fixed

#### Parcel/estate persistence silently fails for players in the Nether or End
- **Root cause:** `CommandHelper.save(Level)` passed the caller's level directly
  to `PersistedData.get()`. `PersistedData` uses `DimensionDataStorage`, which
  is per-dimension. `ParcelRegistry` is stored in the Overworld. Any persistence
  call originating from a player in the Nether or End received `null` from
  `PersistedData.get()`, which the null check silently swallowed — dirty flag
  never set, changes never written to disk.
- **Affected operations:** all commands and interactions that call
  `CommandHelper.save()`, including parcel rename, estate rename, claim,
  demolish, relinquish, whitelist edits, and the new Border Stone rename.
- **Fix:** `CommandHelper.save()` now resolves to the Overworld `ServerLevel`
  internally before calling `PersistedData.get()`. All call sites are unchanged
  — the fix is transparent and centralized.

---

*Requires GottschCore NeoForge v2.6 or later.*

---

## [2.6.1] - 2026-04-16

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

## [2.6.0] - 2026-04-16

### 🎉 Highlights

- **JM fullscreen tooltip picks the correct nested parcel** — was Y-coordinate
  dependent, now uses 2D point-in-footprint with smallest-volume tiebreaker.
- **Citizen deeds carry the bound Nation name in their tooltip** — players can
  see at a glance which Nation a Citizen deed will join.
- **Phantom Player parcel after relinquished-Citizen reclaim no longer appears**
  on the placing player's JM.
- **Reclaim preview no longer falsely renders red** when the relinquished Citizen
  has an adjacent same-owner sibling.
- **JM tooltip ghost text cleaned up** — no more floating "No Ow…" fragments
  near the cursor on the fullscreen map.
- **Relinquished parcels no longer disappear from `/cml-ops parcel relinquish`
  suggestions** — duplicate estate names produced by the relinquish split now
  use `defaultName(ownerId)` for guaranteed uniqueness.
- **Red conflict borders no longer appear on committed parcels after periodic
  resync or relog** — `syncAllParcelsToPlayer` was calling `resolveConflictState`
  on committed parcels (same root cause as the v2.5.1 `onChunkWatch` fix in a
  different code path).
- **Red conflict borders no longer appear on a neighbor's committed parcel when
  placing a border stone** — `placeParcelBorder()` was calling
  `resolveConflictState` unconditionally and passing the result to the committed-
  parcel broadcast path. Third `resolveConflictState` call-site fix.
- **PlayerDeed → Citizen conversions now fire purple Citizen fireworks** instead
  of green Player fireworks. The celebration code in `Deed.useOn` was reading
  off the transient pre-claim parcel reference; post-claim re-resolve now uses
  a chained lookup strategy that handles both conversion paths.
- **Conflict preview overlays no longer broadcast to all players** — only the
  placing player sees red/orange conflict highlights in JM and in-world.
- **JM orange conflict overlays now clear correctly** on distance timeout after
  the foundation stone is removed.
- **Foundation stone repositioning now works correctly** — old border stones are
  removed from the client when a new position is selected.
- **Deed placement now gives informative failure messages** — `canPlaceAt()`
  returns a rich `PlacementResult` enum so players know exactly why a deed
  cannot be placed (closed Nation, blacklisted, wrong parent type, etc.).
---

### ➕ Added

#### `PlacementResult` enum
- New enum `PlacementResult` with values `SUCCESS`, `OUTSIDE_WORLD`,
  `NATION_CLOSED`, `NATION_BLACKLISTED`, `OUTSIDE_VALID_PARENT`,
  `INVALID_PARENT_TYPE`, `ACCESS_DENIED`, `UNKNOWN_FAILURE`.
- `Parcel.canPlaceAt()` default changed from `boolean` to `PlacementResult`.
- `NationParcel`, `ZoneParcel`, `CitizenParcel`, `PlayerParcel` each override
  `canPlaceAt()` to return the appropriate specific value.
- `Deed.useOn()` maps the result to a lang key via `placementLangKey(PlacementResult)`.
- Exhaustive `switch` in `placementLangKey` — adding a new enum value produces
  a compile error, not a silent fallthrough.
#### Citizen deed Nation name tooltip
- `DeedFactory.createCitizenDeed(Box size, UUID nationId, String nationName)` —
  added `nationName` parameter, persisted to `CustomData` alongside the existing
  `NATION_ESTATE_ID` via the read-modify-writeback pattern.
- New `Deed.NATION_ESTATE_NAME = "nationEstateName"` constant.
- `CitizenDeed.appendHoverText` reads `NATION_ESTATE_NAME` and renders a gold
  `Nation: <name>` line. No-op when absent (old deeds without the bound name).
- New lang key `tooltip.claimmyland.deed.nation`.
#### `ClientParcelRegistry.findAt(int x, int z, String dimension)` overload
- 2D point-in-footprint lookup for the JourneyMap fullscreen tooltip. Ignores Y
  entirely — the fullscreen map is a top-down view. When multiple parcels'
  footprints contain the point, the smallest by area wins (same "smallest
  containing parcel" rule used by the in-world HUD).
---

### 🐛 Fixed

#### JM fullscreen tooltip showed Zone instead of nested Citizen depending on cursor Y
- **Root cause:** `JourneyMapOverlayHandler.onMapMouseMoved()` was calling
  `ClientParcelRegistry.findAt(x, y, z, dimension)` with the cursor's full 3D
  BlockPos. JM provides the Y of the topmost block under the cursor (surface
  block, tree leaf, tower top — whichever is highest). The 3D containment check
  was therefore inconsistent: cursor on a tall block inside a Citizen's Y range
  picked Citizen, cursor on bare ground below the Citizen's Y range picked the
  enclosing Nation.
- **Fix:** added 2D `findAt(int x, int z, String dimension)` overload to
  `ClientParcelRegistry` that ignores Y. `onMapMouseMoved` now calls the 2D
  overload.
#### Phantom Player parcel after relinquished-Citizen reclaim
- **Root cause:** `Deed.useOn()` was explicitly calling
  `CMLNetwork.syncParcelToPlayer(serverLevel, serverPlayer, parcel)` after a
  successful claim, where `parcel` is the *transient* pre-claim object created
  from the deed item. In the normal claim path this was a redundant duplicate of
  the broadcast already performed by `nameAndRegister()` →
  `ParcelRegistry.register(ServerLevel, Parcel)` → `syncParcelToTrackingPlayers`.
  In the relinquished-Citizen reclaim path
  (`AbstractClaimableParcel.claimRelinquishedCitizenParcel` →
  `ParcelRegistry.transferParcelOwnership`) and the
  `PlayerParcel.claimWithinZone` → fresh `CitizenParcel` conversion path, the
  registered parcel is a *different object* than `parcel`. The explicit sync
  added the transient pre-claim object to the placing player's
  `ClientParcelRegistry` as a phantom green overlay that persisted until the
  next periodic resync (~5 minutes).
- **Fix:** deleted the explicit `syncParcelToPlayer(parcel)` block from the
  `if (claimResult.isSuccess())` branch in `Deed.useOn()`. The placing player is
  always within `TRACKING_CHUNK` range of their own claim, so the existing
  `syncParcelToTrackingPlayers` broadcast inside `nameAndRegister`/
  `transferParcelOwnership` already reaches them.
#### Reclaim preview shows red conflict against adjacent same-owner sibling
- **Root cause:** when reclaiming a relinquished Citizen adjacent to a
  non-relinquished sibling Citizen owned by the original (relinquishing) player,
  the Foundation Stone preview rendered red even though the commit succeeded.
  `resolveConflictState()` correctly excluded the relinquished Citizen itself
  from the direct-overlap check via `excludeParcelId`, but the buffer/inflated
  checks still found the adjacent sibling and evaluated
  `isConflict(citizen, citizen, reclaimer, originalOwner)` — which returned true
  because the same-owner sibling exception that allowed the original adjacency
  no longer applied (the reclaimer is a different owner).
- **Fix:** added a reclaim exemption at the top of `resolveConflictState()` that
  returns `0` immediately when `excludeParcelId` resolves to a relinquished
  Citizen whose geometry exactly matches the proposed box. The exemption is
  narrow — geometric equality is required — so it never fires for non-reclaim
  placements.
#### JM tooltip ghost text and polygon label cleanup
- **Root cause:** During earlier debugging we added
  `setTitle(buildFullLabel(parcel))` to JM polygon overlays. JM's
  `Overlay.setTitle()` is a rollover text JM renders independently of our
  `ParcelMapTooltipRenderer`. With both active, they competed and clipped each
  other, producing a small floating "No Ow…" fragment near the cursor.
- **Fix:** removed `.setTitle(...)` from `buildOverlay()`. Also changed
  `setLabel(parcel.estateName())` to `setLabel("")` on the fullscreen overlay
  since the rich custom tooltip already shows the estate name on hover. Minimap
  overlay unchanged — it has no hover tooltip and the label is the only way to
  identify parcels at a glance.
#### Relinquished parcels disappear from `/cml-ops parcel relinquish` suggestions
- **Root cause:** `RelinquishParcelSubCommand.relinquishParcel()` peels the
  relinquished parcel off its shared estate by creating a new `Estate` object,
  but copied the old name verbatim via `newEstate.setName(oldEstate.getName())`.
  When two Citizens shared an estate and one was relinquished, the result was
  two estates with identical names but different UUIDs. The suggestion providers
  (`OPS_OWNER_CITIZEN_ESTATE_NAMES`, `OWNER_CITIZEN_ESTATE_NAMES`) collect names
  into a `Set<String>` keyed on `Estate::getName`, collapsing duplicates to a
  single entry. The user-visible symptom was that one of the two estates became
  unreachable from the relinquish command suggestions, even though the parcel
  data was intact in the registry. `transferParcelOwnership()` had the same bug
  on the reclaim side and was patched in parallel.
- **Fix:** both `RelinquishParcelSubCommand.relinquishParcel()` and
  `ParcelRegistry.transferParcelOwnership()` now call
  `newEstate.defaultName(ownerId)`, which uses the authoritative
  `PlayerRegistry.nextEstateNameIndex(ownerId)` counter and is collision-free
  by construction. Relinquished and reclaimed estates are now indistinguishable
  in form from any other freshly-created estate.
#### Red conflict borders on committed parcels after periodic resync or relog
- **Root cause:** `CMLNetwork.syncAllParcelsToPlayer()` called
  `ParcelRegistry.resolveConflictState()` on every committed parcel before
  building the resync packets. `resolveConflictState()` is designed for
  Foundation Stone placement preview only — when called on a committed Nation
  parcel whose box contains nested Zones or Citizens, the children overlap the
  parent box and the method returns `1`, which the client renders as red
  borders. This is the same bug the v2.5.1 fix patched in `onChunkWatch`,
  reproduced in a different code path.
- **Fix:** both branches of the per-parcel `map(...)` in
  `syncAllParcelsToPlayer()` now pass `0` literally for `conflictState` instead
  of calling `resolveConflictState()`. Mirrors the v2.5.1 `onChunkWatch` fix.
#### Committed parcel border stone placement broadcasts wrong conflictState to tracking players
- **Root cause:** `BorderStoneBlockEntity.placeParcelBorder()` called
  `resolveConflictState()` unconditionally at the top of the method and passed
  the result into both the committed-parcel branch and the preview branch.
  `resolveConflictState()` is valid only for Foundation Stone placement preview —
  when called for a committed parcel with an adjacent different-owner sibling,
  it returns `1`, which `syncBorderVisibilityToTrackingPlayersAndSelf` broadcast
  to all tracking players. The affected player saw the adjacent committed parcel
  render red in JM. Self-healed on the ~5-minute periodic resync. Same root
  cause as the v2.5.1 `onChunkWatch` fix and the v2.6 `syncAllParcelsToPlayer`
  fix — the third `resolveConflictState` call-site on committed parcels.
- **Fix:** `resolveConflictState()` moved into the preview (`else if`) branch
  only. Both committed-parcel send paths (`placingPlayer != null` and
  `placingPlayer == null`) now pass `0` literally for `conflictState`.
#### PlayerDeed → Citizen fireworks render green instead of purple
- **Root cause:** `Deed.useOn()`'s success branch re-resolved the registered
  parcel via `ParcelRegistry.findByParcelId(parcel.getId())`, but this missed
  in both conversion paths. In the `PlayerParcel.claimWithinZone()` →
  `CitizenParcel` path, the pre-claim `PlayerParcel` built from the deed item
  has no UUID (`getId()` returns `null`), so `findByParcelId(null)` returned
  empty. In the relinquished-Citizen reclaim path (`transferParcelOwnership`),
  the registered parcel retains the relinquished parcel's UUID — the UUID of
  the foundation stone's existing block entity — not the freshly pre-assigned
  UUID on the transient `PlayerParcel`. Both cases fell back to the stale
  `PlayerParcel` via `.orElse(parcel)` and fired green fireworks.
- **Fix:** `Deed.useOn()` pre-assigns `UUID.randomUUID()` to the pre-claim
  parcel when `getId() == null`, before `handleClaim()` is called. The
  re-resolve in the success branch then chains two lookups: first
  `findByParcelId(parcel.getId())` (finds the registered `CitizenParcel` in
  the `claimWithinZone` path), then `findByParcelId(foundationStoneBlockEntity
  .getParcelId())` (finds the transferred parcel in the relinquished reclaim
  path, whose UUID is the foundation stone's pre-existing parcel ID). The
  `orElse(parcel)` defensive fallback is retained as last resort.
#### Conflict preview overlays broadcast to all tracking-chunk players
- **Root cause:** `BorderStoneBlockEntity.placeParcelBorder()` called
  `CMLNetwork.syncPreviewParcelToTrackingPlayersAndSelf(...)` with the real
  `conflictState` for all players. Every client receiving a preview packet with
  `conflictState > 0` ran `showConflictOverlays()`, so all nearby players saw
  the red/orange conflict highlights in JM — not just the placing player.
- **Fix:** split the preview send into two calls. Tracking players (excluding
  the placer) receive `conflictState=0` so they see the JM preview outline but
  never conflict highlighting. The placing player receives a separate
  player-targeted send with the real `conflictState`. Both
  `syncPreviewParcelToTrackingPlayers` and `syncPreviewParcelToOwner` are used;
  the former excludes the placing player from the tracking broadcast.
#### JM orange conflict overlays persist after foundation stone removal and distance clear
- **Root cause:** `rebuildTransientOverlays()` (called from `pushAllOverlays()`
  when the JM map is opened) did a raw `CONFLICT_OVERLAYS.clear()` and
  `CONFLICT_BUFFER_OVERLAYS.clear()` without first calling `jmApi.remove()` on
  the old overlay objects. This orphaned the original overlay objects in JM —
  they remained visible and could never be removed because no reference to them
  survived. When `clearConflictOverlays()` later fired (on distance/timeout), it
  removed the replacement objects but the originals persisted as orange fills.
- **Fix:** `rebuildTransientOverlays()` now calls `jmApi.remove()` on all
  entries in both conflict maps before clearing them, matching the pattern used
  by `removeAllPermanentOverlays()`. Same remove-before-clear discipline now
  applies consistently across all overlay lifecycle paths.
#### Foundation stone not removed from client when repositioning with deed
- **Root cause:** `Deed.useOn()` fell through to `return super.useOn(context)`
  (which returns `InteractionResult.PASS`) after a successful foundation stone
  placement. `PASS` tells NeoForge the interaction was not handled, suppressing
  the block-change sync to the client. The server correctly placed the new stone
  and removed the old one (including border stones), but the client never
  received the updates and continued to show the old border stones. They
  appeared unbreakable (mod protection active on preview parcels) and
  disappeared on relog when the client received fresh chunk data. This matched
  the v2.4 code which returned `InteractionResult.SUCCESS` explicitly.
- **Fix:** the foundation stone placement branch now returns
  `InteractionResult.SUCCESS` on success and `InteractionResult.FAIL` on
  failure, never falling through to `super.useOn(context)`.
#### Deed placement fails silently with no player message
- **Root cause:** `canPlaceAt()` returned `boolean` — callers knew placement
  was denied but not why. Players using a Citizen deed directly inside a Nation
  (outside any Zone), inside a closed Nation, or while blacklisted received
  no feedback.
- **Fix:** `canPlaceAt()` now returns `PlacementResult`. Each concrete parcel
  type's override returns the specific failure reason. `Deed.useOn()` maps the
  result to a lang key and sends an informative failure message.
---

### Removed

#### Dead-code overloads in `CMLNetwork`

Two `CMLNetwork` overloads were latent traps with no live callers:

- `syncBorderVisibleToOwner(ServerLevel, Parcel, int borderStoneY)` — 3-arg
  version that resolved `conflictState` internally via
  `resolveConflictState()`.
- `syncBorderVisibleToOwnerAndPlacer(ServerLevel, Parcel, int borderStoneY, UUID placingPlayerId)`
  — 4-arg version that resolved `conflictState` internally.
  Both methods called `resolveConflictState()` on what would have been committed
  parcels (any caller would have been a Border Stone toggle path), reproducing
  the same bug as the v2.5.1 `onChunkWatch` and v2.6 `syncAllParcelsToPlayer`
  fixes. With no callers, the cleanest action is deletion — the surviving
  explicit-`conflictState` overloads (which take the value as a parameter) cover
  every legitimate use, and callers always know whether they're in a preview or
  committed context.

#### Stale `@Deprecated` on `INationParcel.getBlacklist()` / `setBlacklist()`
The blacklist mechanism is current and in active use. The `@Deprecated(forRemoval
= true, since = "2.0")` annotations on `getBlacklist()`, `setBlacklist()`, and
the corresponding `NationParcel.blacklist` field were incorrect. Removed.
 
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

- **Citizen claim disconnect on failed placement** — `EncoderException: Failed to encode
  packet 'clientbound/minecraft:system_chat'` when a Citizen deed claim failed inside a
  Nation. Root cause: `Deed.useOn()` passed raw `Coords` objects as translation args to
  `Component.translatable("...unable_to_claim.detail", parcel.getMinCoords(), ModUtil.getSize(...))`.
  NeoForge 1.21.1's `ComponentSerialization` codec is strict and only accepts
  `Component`/`String`/`Number`/`Boolean` args — any other type throws `"This value needs
  to be parsed as component"` at packet encode time, disconnecting the client. Fix:
  added a defensive `coerceArgs()` helper in `CommandResponseFormatter` that wraps any
  non-primitive/non-Component arg in `Component.literal(arg.toString())` before passing
  to `Component.translatable(...)`. Applied at the single chokepoint where `args` meets
  `Component.translatable`, so all callers of `sendFailure` / `sendSuccess` are covered
  without call-site edits. NeoForge 1.21.1 only — Forge 1.20.1's serializer tolerated
  raw object args via `toString()` fallback.

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