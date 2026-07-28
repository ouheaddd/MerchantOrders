# Merchant Orders — Trade Crate update

Added the modular Trade Crate sale system and delayed physical sack delivery.

## Purchase delivery
- `Send Order` no longer puts a sack directly in the player inventory.
- The terminal stores the packed purchase and places an order sack nearby after 200 ticks (10 seconds by default).
- The pending delivery is saved in the terminal block entity.
- If no valid block position exists, the sack drops as an item above the terminal.

## Trade Crate
- New `trade_crate` block, item, block entity, menu and screen.
- 36 input slots.
- The offer rerolls at the beginning of every Minecraft day and is deterministic for player + contents + day.
- Personal sale cooldown: 3 Minecraft days by default.
- Unknown items still contribute a low fallback value; the whole non-empty batch gets at least one emerald.
- Rewards can include tag-selected items, including modded items that correctly join vanilla item tags.
- Rare tagged bonus chance is included for sufficiently valuable batches.
- On sale, input is consumed and a payment sack appears beside the crate after 10 seconds.

## Modular GUI textures
All Trade Crate textures are under:
`assets/merchant_orders/textures/gui/trade_crate/`

- `background.png` — 380x260
- `value_panel.png` — 178x62
- `reward_panel.png` — 178x58
- `status_panel.png` — 178x54
- `sell_button.png` — 178x22
- `sell_button_hover.png` — 178x22
- `sell_button_disabled.png` — 178x22
- `progress_background.png` — 98x6
- `progress_fill.png` — 96x4

Developer: overyourhead
