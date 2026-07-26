# Merchant Orders GUI texture guide

All GUI elements are now separate PNG files. They can be replaced without changing Java coordinates.
Use nearest-neighbour scaling and keep the exact canvas size.

| File | Exact size | Purpose |
|---|---:|---|
| `order_terminal.png` | 380 x 240 | Main window, trade-list backing and player inventory grid |
| `tier_0.png` | 38 x 36 | Full Novice category button |
| `tier_1.png` | 38 x 36 | Full Apprentice category button |
| `tier_2.png` | 38 x 36 | Full Journeyman category button |
| `tier_3.png` | 38 x 36 | Full Expert category button |
| `tier_4.png` | 38 x 36 | Full Master category button |
| `tier_selected_overlay.png` | 38 x 36 | Overlay drawn over the selected category |
| `tier_locked_overlay.png` | 38 x 36 | Overlay drawn over a locked category, including the cross |
| `trade_row.png` | 128 x 24 | Normal trade row |
| `trade_row_selected.png` | 128 x 24 | Selected trade row |
| `current_trade_panel.png` | 103 x 81 | Current Trade panel, including three slot frames and arrow |
| `basket_panel.png` | 71 x 81 | Basket panel. Intentionally contains no sack image |
| `send_order_button.png` | 176 x 22 | Enabled Send Order button |
| `send_order_button_hover.png` | 176 x 22 | Hovered Send Order button |
| `send_order_button_disabled.png` | 176 x 22 | Disabled Send Order button |
| `xp_bar_background.png` | 176 x 7 | XP bar background |
| `xp_bar_fill.png` | 174 x 5 | XP fill cropped dynamically according to progress |
| `scrollbar_track.png` | 4 x 186 | Trade-list scrollbar track |
| `scrollbar_thumb.png` | 4 x 24 | Scrollbar thumb |

## Fixed GUI coordinates

- Categories: `x=8`, `y=26`, each `38x36`, vertical gap `3`.
- Trade rows: `x=50`, `y=26`, each `128x24`, 8 visible rows.
- Scrollbar: `x=180`, `y=29`, `4x186`.
- XP bar: `x=196`, `y=17`, `176x7`.
- Current Trade: `x=196`, `y=29`, `103x81`.
- Basket: `x=301`, `y=29`, `71x81`.
- Send Order: `x=196`, `y=113`, `176x22`.
- Inventory slots: first slot `x=202`, `y=143`; hotbar `y=201`.

The basket counters are still text rendered by code: occupied slots at the lower left, total item count at the lower right. Paint the sack/basket illustration directly into `basket_panel.png`.
