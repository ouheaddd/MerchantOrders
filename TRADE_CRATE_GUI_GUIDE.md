# Trade Crate GUI 1.1.0

The screen is 380x240 and all decorative pieces are modular PNG files under:

`src/main/resources/assets/merchant_orders/textures/gui/trade_crate/`

- `background.png` — 380x240
- `left_panel.png` — 178x219
- `value_panel.png` — 182x64
- `reward_panel.png` — 182x91
- `status_panel.png` — 182x27
- `slot.png` — 18x18
- `reward_slot.png` — 18x18
- `sell_button.png` — 182x22
- `sell_button_hover.png` — 182x22
- `sell_button_disabled.png` — 182x22
- `progress_background.png` — 100x8
- `progress_fill.png` — 91x4
- `clock.png` — 16x16

Layout:
- Trade crate: 6x6 slots (36 total), x=16, y=34.
- Player inventory: 3x9, x=16, y=160.
- Hotbar: 1x9, x=16, y=218.
- Rewards: 4x2, x=204, y=120, horizontal gap 37, vertical gap 22.

Reward items, counts, labels, timers, emerald estimate and button text are rendered dynamically and should not be painted into the PNG files.
