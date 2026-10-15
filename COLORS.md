# TickOff color scheme

TickOff uses a warm, reddish plum taken from the app icon, on light or aubergine surfaces. It is deliberately different from Obsidian's cool violet on black.

The app follows the phone's light or dark setting. Colors are defined in:

- [Theme.kt](app/src/main/kotlin/io/github/kloeggaa/tickoff/ui/theme/Theme.kt): the app screens (Material 3 color scheme)
- [values/colors.xml](app/src/main/res/values/colors.xml) and [values-night/colors.xml](app/src/main/res/values-night/colors.xml): the widgets and the window background
- [ic_launcher_background.xml](app/src/main/res/drawable/ic_launcher_background.xml): the launcher icon
- [Components.kt](app/src/main/kotlin/io/github/kloeggaa/tickoff/ui/theme/Components.kt): the donate button

## Brand colors (app icon)

| Color | Hex | Use |
|---|---|---|
| Plum | `#5B237B` | Icon background (top-left half), light primary |
| Light plum | `#75369F` | Icon background (bottom-right half), widget checkboxes (light) |
| Lavender | `#F2EDF8` | Checklist card in the icon |
| Muted violet | `#8F79B2` | Checklist lines in the icon, outlines (light) |
| Coral | `#FF8A65` | Ticked box in the icon, donate button |

## App screens

| Role | Light | Dark |
|---|---|---|
| primary | `#5B237B` | `#D7B8F0` |
| onPrimary | `#FFFFFF` | `#3E1257` |
| primaryContainer | `#EBDCF6` | `#5B237B` |
| onPrimaryContainer | `#2A0B3D` | `#F2E1FF` |
| secondary | `#6E5B8C` | `#CBBDE6` |
| onSecondary | `#FFFFFF` | `#33284A` |
| secondaryContainer | `#E6DDF2` | `#4A3D63` |
| onSecondaryContainer | `#2A1F3B` | `#E9DDFF` |
| tertiary | `#B4532F` | `#FFB59C` |
| onTertiary | `#FFFFFF` | `#561F0B` |
| background | `#FBF8FD` | `#16111A` |
| onBackground | `#1E1A22` | `#EAE0EE` |
| surface | `#FFFFFF` | `#1F1824` |
| onSurface | `#1E1A22` | `#EAE0EE` |
| surfaceVariant | `#EDE5F4` | `#2F2637` |
| onSurfaceVariant | `#4A4453` | `#CDC2D6` |
| outline | `#8F79B2` | `#978BA2` |
| errorContainer | `#FFDAD6` | `#93000A` |
| onErrorContainer | `#410002` | `#FFDAD6` |

The donate button uses coral `#FF8A65` with dark brown text `#3B1400` in both modes, so its contrast stays readable either way.

## Widgets and window

| Resource | Light | Dark | Use |
|---|---|---|---|
| `widget_background` | `#FFFFFF` | `#1F1824` | Task list widget background |
| `widget_text` | `#1E1A22` | `#EAE0EE` | Task text |
| `widget_text_secondary` | `#4A4453` | `#CDC2D6` | File name, due dates, refresh icon |
| `widget_checkbox` | `#75369F` | `#D7B8F0` | Checkbox outline |
| `widget_refresh_background` | `#EDE5F4` | `#2F2637` | Round refresh button |
| `window_background` | `#FBF8FD` | `#16111A` | Window behind the app screens |

The widget colors match the app's surface, onSurface, onSurfaceVariant, surfaceVariant and background roles, so widgets and app look the same in each mode.
