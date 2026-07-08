# BiteCheck — Design Reference

Direction: **Yuka-style** (clean food-scanner aesthetic), per project owner. ZOE's yellow/navy
was considered; Yuka's white + green + carrot orange fits a calorie tracker better and matches
the app's health positioning.

## Palette (`res/values/colors.xml`)

| Role | Color | Hex |
|---|---|---|
| Primary (buttons, toolbar, highlights) | Fresh health green | `#1FA55A` |
| Primary dark (status bar) | Deep green | `#15803D` |
| Primary container (chips, soft fills) | Mint | `#DCF5E7` |
| Secondary / accent (CTAs, alerts) | Carrot orange | `#EA562A` |
| Background | Warm off-white | `#FAFAF8` |
| Surface (cards) | White | `#FFFFFF` |
| Text primary | Dark charcoal | `#1F2933` |
| Text secondary | Cool grey | `#6B7280` |

### Rating colors (Nutri-Score style — Food Advisor verdicts)

| Verdict | Hex |
|---|---|
| Excellent | `#1FA55A` |
| Good | `#7DCB56` |
| Mediocre | `#F2A93B` |
| Poor | `#E51B31` |

## Style rules

- Material 3 components, `Theme.BiteCheck` (parent `Theme.Material3.DayNight.NoActionBar`).
- Generous white space; content on white cards (16dp corner radius) over off-white background.
- One accent per screen: green for primary actions, orange reserved for advisor warnings/CTAs.
- Rounded, friendly shapes; filled primary buttons, outlined secondary buttons.
- Icons: Material rounded set, tinted green on white surfaces.
- Dark theme: same hues, Material 3 dark surfaces (see `values-night/themes.xml`).
