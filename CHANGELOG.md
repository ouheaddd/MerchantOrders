## 1.1.0

- Rebuilt the Trade Crate screen to match the approved two-column layout.
- Added a visible 6x6 trade-storage grid and the complete 3x9 player inventory plus hotbar.
- Expanded dynamic reward preview from 4 to 8 slots arranged as 4x2.
- Replaced internal `value` display with an approximate emerald range.
- Added batch grades and a daily offer refresh countdown.
- Reduced the lower cooldown panel and kept delivery/cooldown status dynamic.
- Split every visual element into replaceable modular PNG textures.
- Preserved delayed physical sack delivery for purchases and sales.
- Included the Level/ServerLevel ticker fixes for both delivery block entities.

## 1.0.4

- Removed the separator line between Send Order and the player inventory.
- Flattened the terminal background to fully opaque pixels so the texture renders sharply without world-color bleed-through.
- Removed text shadows from Current Trade, Basket, and Send Order labels.
- Right-aligned the XP value to the end of the XP bar.

# Changelog

## 1.0.2

- Vanilla-style result-slot purchasing: click once or Shift-click for maximum.
- The action button now sends/claims the completed order sack.
- Simplified basket preview with one-line counters.
- Larger centered tier icons, no tier numbers, centered lock mark.
- Softer selected-tier green and aligned tier/trade columns.
- Fixed selected trade highlight overlapping the scrollbar.
- Flattened Current Trade and Basket panels and removed the Inventory label.


## 1.0.0-prototype

- Added Order Terminal block and menu.
- Added five player progression tiers.
- Added deterministic per-player villager trade catalogs.
- Added compatible standard/modded villager trade capture.
- Added payment slots, auto-refill, single and batch purchases.
- Added persistent 18-slot basket.
- Added portable/placeable extraction-only Order Sack.
- Added wandering trader terminal offer.
- Added temporary GUI, textures, models, sounds, and localizations.
- Added common config and dedicated client/common/core/mixin structure.

## 1.0.1
- Reworked the terminal layout to the approved parchment interface.
- Kept five square tier selectors beside the full-height trade list.
- Added separate Current Trade and Order Basket panels and a wide Add to Order button.
- Moved the vanilla 3-row inventory and hotbar below the order controls.
- Expanded order sacks from 18 to 36 slots.
- Order sacks now accept ordinary items after delivery.
- Nested order sacks and shulker boxes are blocked for safety.
- Removed all hardcoded fallback trades; catalogs now come from villager trade events.

## 1.0.3
- Repacked the approved compact GUI layout.
- Moved the inventory upward and shortened the upper-right panel.
- Basket artwork is now entirely texture-based and intentionally blank by default.
- Converted category buttons, trade rows, XP bar, scrollbar, panels and Send Order button into replaceable PNG textures.
- Added `GUI_TEXTURE_GUIDE.md` with exact pixel dimensions and coordinates.
