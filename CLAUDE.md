# BiteCheck — Agent Guide

BiteCheck is a **Java Android** app built for a Mobile Application Development class
assignment (individual, 20 marks). It is a calorie tracker whose headline feature is
**natural-language chat meal logging** ("I had 2 eggs and toast" → parsed & logged)
plus a **Food Advisor** ("should I eat this?" via text or photo). The visual target is a
Yuka / Cal-AI / iOS-style look.

This file tells a new agent how the project is organized and, importantly, the
**conventions and design system** to follow so new work stays consistent.

> Companion docs: `docs/design.md` (palette/style spec), `docs/requirements-checklist.md`
> (maps each assignment requirement → where it's satisfied), `docs/supabase-schema.sql`
> (run once in the Supabase SQL editor). There is also a persistent agent memory at
> `C:\Users\ahadi\.claude\projects\A--BiteCheck\memory\` — read `MEMORY.md` there first.

---

## Build, run, verify

- Windows environment. Primary shell is PowerShell; a Bash tool is also available.
- Build: `.\gradlew.bat assembleDebug` (from repo root `A:\BiteCheck`). After **every** code
  change, build to confirm it compiles before moving on ("keep cooking" workflow).
- Java 11, `com.example.bitecheck`, minSdk 24, targetSdk 36, AGP 9.2.1, Gradle 9.4.1,
  Material Components 1.14.0. Dependency versions live in `gradle/libs.versions.toml`
  (version catalog); wire new libs there + `app/build.gradle.kts`.
- **On-device verification is expected.** The user tests on a physical **Samsung Galaxy S22
  (OneUI, ~1080×2340, density ~2.65)** over **wireless adb**. It drops off intermittently —
  re-check `adb devices` and reinstall when it does.
  - adb lives at `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`.
  - Install + relaunch:
    ```
    adb install -r app\build\outputs\apk\debug\app-debug.apk
    adb shell input keyevent KEYCODE_WAKEUP; adb shell wm dismiss-keyguard
    adb shell am start -n com.example.bitecheck/.ui.SplashActivity
    ```
  - Screenshot: `adb shell screencap -p /sdcard/s.png` then `adb pull /sdcard/s.png <local>`,
    then Read the PNG. **Only `SplashActivity` is exported** — launch other screens by driving
    the UI with `adb shell input tap X Y` (coordinates are real device px; a pulled screenshot
    is already real-px). Trust the pulled 1080×2340 image.

## Credentials (never commit)

`local.properties` (git-ignored) holds `SUPABASE_URL`, `SUPABASE_ANON_KEY`,
`GEMINI_API_KEY`. These are surfaced as `BuildConfig.*` via `app/build.gradle.kts`
`buildConfigField`. The Supabase anon key is a public client key (fine in BuildConfig; RLS
restricts each user to their own rows). The user's Gemini key starts `AQ.` (not the usual
`AIza`) but is valid — if the API returns 401/403 have them re-issue at aistudio.google.com.

---

## Architecture

Package root `com.example.bitecheck`:

```
BiteCheckApp.java        Application: forces AppCompatDelegate.MODE_NIGHT_NO (light-only, Yuka style)
ui/                      All Activities, adapters, custom views
data/local/              BiteCheckDbHelper (SQLiteOpenHelper) + Dao classes (offline source of truth)
data/remote/             SupabaseClient/Auth/Db (OkHttp REST, no SDK), GeminiClient
model/                   UserProfile, Meal, WaterLog, WeightLog, FoodItem
util/                    NetworkUtil, SessionManager, CalorieCalculator, DateUtil, SyncManager,
                         FoodMatcher, ThinkingWords, ReminderScheduler/Receiver
```

### Navigation is Activity-only (no Fragments). Three base shells in `ui/`:

- **`BaseNavActivity`** — toolbar (light, no title) + drawer + 4-tab bottom nav. Child gives
  `getContentLayoutId()`. Used by the two tab screens that don't take text input:
  **DashboardActivity** and **MealHistoryActivity**. Bottom-nav tabs: Dashboard, Chat, Advisor,
  History. (Chat/Advisor tabs launch full-screen typing screens — see below.)
- **`BaseSecondaryActivity`** — toolbar with Up arrow, no title. Drawer destinations:
  Water, Weight, Report, Settings, Account, Feedback, Complaints, Help, Contact, Privacy.
- **`BaseTypingActivity`** — full-screen shell for text-entry screens with a **circular back
  button top-left, no bottom nav, no title** (`activity_fullscreen.xml`). Used by
  **MainActivity (Chat)** and **FoodAdvisorActivity**. Both are `windowSoftInputMode=adjustResize`
  in the manifest.

**All toolbar titles are intentionally blank** (`setTitle("")`); every screen leads with its own
bold heading in its content layout instead.

### Launch flow
`SplashActivity` (only launcher/exported activity) routes:
- logged-in + onboarded → `DashboardActivity`
- logged-in, not onboarded → `OnboardingActivity`
- logged-out → `IntroActivity` (first run, sets `intro_seen` flag in prefs `bitecheck_ui`) →
  `WelcomeActivity`; thereafter straight to `WelcomeActivity`.
Auth: Welcome → Login / SignUp → (SignUp →) Onboarding → Dashboard.

### Data layer (offline-first)
- **SQLite** (`BiteCheckDbHelper`, DB v2) is the source of truth. Tables: `profile`, `meals`,
  `water_logs`, `weight_logs`, `foods` (34 seeded common foods for the offline chat fallback).
  Every user row has a `synced` flag. `clearUserData(userId)` wipes a user's rows (account delete).
- **Supabase** cloud via **OkHttp REST** (no SDK). `SupabaseClient` (headers `apikey` + Bearer),
  `SupabaseAuth` (GoTrue: signup/login/recover), `SupabaseDb` (PostgREST: `upsert`, `insert`,
  `selectOne`, `delete`; callbacks run on the main thread). `SyncManager.pushAll()` pushes
  unsynced rows on resume. Login restores the profile from SQLite → cloud `profiles` so returning
  users skip onboarding (`LoginActivity.restoreProfileAndContinue`).
- **Auth-user deletion** (GoTrue user record) needs a server/service role — the anon key can't do
  it. "Delete account" therefore wipes the user's **data** (local + cloud rows) and signs out; it
  does not delete the login record. That's the correct client-side approach.

### Smart features — Gemini (user's explicit choice over Claude)
`data/remote/GeminiClient` hits `gemini-2.5-flash` (`generateContent`, header `x-goog-api-key`):
- **Chat parse** (`parseMeal`): structured JSON via `response_schema` (root OBJECT
  `{foods:[{food,quantity,unit,meal_type,calories}], question}`). If a quantity is genuinely
  ambiguous the model returns an empty `foods` + a `question`; MainActivity folds the answer back
  in for one re-parse round, else falls back to `FoodMatcher`.
- **Advisor** (`advise`): text +/- base64 JPEG (`inline_data`) + remaining calories/goal → verdict.
- **503/429 auto-retry** (MAX_RETRIES=2, backoff), friendly final message. 503 "high demand" is
  Google shedding load, not a bug. Offline / no-key → `FoodMatcher` against the seeded `foods`.
- **No emojis** in AI output or UI strings (enforced in prompts + strings). `ThinkingWords` rotates
  food-pun status words while waiting.

---

## Design system — READ BEFORE TOUCHING UI

The app has a single, consistent look. New screens must match it. Everything is centralized so
re-theming is a small number of files.

### Foundations (theme = `Theme.BiteCheck`, `res/values/themes.xml`)
- **Light only** (dark mode disabled app-wide via `BiteCheckApp`). Cream background
  `bc_background #FAFAF8`, white surfaces. The theme overrides M3's whole surface-color system to
  kill the default purple tint — keep those overrides.
- **Font: Inter** everywhere (bundled in `res/font/`, set as theme `fontFamily`). Weights:
  `inter_regular/medium/semibold/bold`. `script_bold` (Dancing Script) is only for the intro
  slides. Use `android:fontFamily="@font/inter_semibold"` etc. directly for weight control.
- **Palette** (`res/values/colors.xml`, the single re-theming point):
  `bc_primary #1FA55A` (green), `bc_secondary #EA562A` (carrot), `bc_primary_container #DCF5E7`
  (pale green), `bc_text_primary #1F2933`, `bc_text_secondary #6B7280`, `bc_field_fill #F2F2F7`
  (iOS field grey), `bc_card_stroke #ECECEC` (hairline), `bc_water #2E7CD6`, score colors
  `bc_score_excellent/good/poor/bad`.
- **No drop shadows anywhere.** Buttons: `Widget.BiteCheck.Button` (filled **pill**, 28dp radius,
  `stateListAnimator=@null`, elevation 0). Cards: `Widget.BiteCheck.Card` (**Outlined**, elevation
  0, 20dp radius, 1dp `bc_card_stroke`). Don't add `cardElevation`/`app:elevation` — flat is the look.
- **Text fields are iOS-filled** by default: theme `textInputStyle = Widget.BiteCheck.TextInput`
  (FilledBox, grey fill, no underline, 14dp radius). **Do NOT set an explicit
  `style="...OutlinedBox"` on a TextInputLayout** — leaving style off makes it inherit the filled
  look. That's how the whole app matches.
- Toolbars are light (`bc_background` bg, dark title via `TextAppearance.BiteCheck.ToolbarTitle`,
  dark drawer arrow) with `app:popupTheme="@style/ThemeOverlay.BiteCheck.PopupMenu"`.

### Reusable patterns (prefer these over inventing new UI)
- **Bottom sheets** for confirmations/pickers/menus (on-brand, rounded 28dp top). Theme
  `R.style.Theme_BiteCheck_BottomSheet`. Examples: `sheet_confirm_meal`, `sheet_add_meal`,
  `sheet_meal_type`, `sheet_edit_name`, `sheet_avatar_source`, `sheet_delete_account`. Include a
  `BottomSheetDragHandleView` at the top.
  - **Sheet with text inputs** MUST wrap content in a `NestedScrollView` and, in
    `setOnShowListener`, set the window `SOFT_INPUT_ADJUST_RESIZE` and expand the behavior
    (`BottomSheetBehavior.from(findViewById(com.google.android.material.R.id.design_bottom_sheet))
    .setState(STATE_EXPANDED)`) — otherwise the keyboard covers the lower fields (see
    `MealHistoryActivity.showAddMealDialog`).
  - Nested "drawer over a sheet" (meal-type picker rising over the add-meal sheet) works because
    each `BottomSheetDialog` is its own window; add an `OvershootInterpolator` translationY bounce
    on show for polish (see `showMealTypePicker`).
- **Circular icon chips**: a `bg_circle_white` drawable tinted with a pale container color
  (`bc_primary_container`, `bc_water_container`, `bc_secondary_container`) with a tinted 22–26dp
  icon centered. Used on dashboard rows, meal rows, avatar, etc.
- **Selectable option rows** (onboarding wizard): `Widget.BiteCheck.Option` style +
  `bg_option_card` (green stroke + pale fill on `state_activated`); toggle with `view.setActivated()`.
- **Circular buttons** (chat send, history add): a plain `ImageButton` with `bg_circle_primary`
  background — NOT a `FloatingActionButton` (FABs force an elevation shadow OneUI won't drop).
- **Hero calorie ring**: `ui/CalorieRingView` (custom View, animated arc, turns orange over target).
- **Detail list rows**: `<include layout="@layout/item_account_row" android:id="@+id/row_x"/>`
  separated by `<View style="@style/Widget.BiteCheck.RowDivider"/>`; set values via
  `findViewById(rowId).findViewById(R.id.row_label/row_value)`.

### OneUI (Samsung) gotcha — important
Samsung's OneUI **re-styles the platform options/overflow menu** (`MenuPopupWindow`) as a flat
square and **ignores `popupBackground`**. So:
- `AutoCompleteTextView` / `ListPopupWindow` dropdowns *do* respect `android:popupBackground` — fine.
- The **toolbar overflow menu does not.** The Dashboard "⋮" is therefore a **custom anchored
  `PopupWindow`**: menu item uses `app:actionLayout=@layout/action_more_button` (a real anchor
  View), and `DashboardActivity.showMorePopup` inflates `menu_more_popup.xml` (rounded
  `bg_popup_menu`) shown via `showAsDropDown(anchor, -8dp, 4dp, Gravity.END)` with
  `Animation.BiteCheck.Popup` (scale+alpha overshoot). Reuse this pattern for any anchored menu.

### Branding
Logo is the **avocado** (`res/drawable/logo_bitecheck.jpg`, from `app/bite.jpg`) shown on a white
circle — intro slide 1, welcome, drawer header, and the adaptive launcher icon
(`mipmap-anydpi-v26` = white bg + `ic_launcher_avocado_fg`). Legacy pre-API26 webp mipmaps still
show the old fork/knife mark. `docs/design.md` is the style source of truth.

---

## Conventions & gotchas

- Match surrounding code: 4-space indent, Inter-weight fonts by resource, strings in
  `res/values/strings.xml` (no hardcoded UI text, **no emojis**).
- AGP 9.2: watch for `xmlns:...res-auto` typos (broke a build once). A dotted style name with no
  parent (e.g. `Widget.BiteCheck.Option`) needs an explicit `parent=""` or AAPT infers a missing
  parent and fails.
- Greeting (Dashboard) is time-based (`buildGreeting`: morning/afternoon/evening) + **first name
  only** (`name.split("\\s+")[0]`); the subtitle is state-aware/randomized (`buildSubtitle`), not
  static. Display name falls back to a capitalized email handle
  (`SessionManager.getDisplayName()`) — never the app name.
- Onboarding doubles as the "edit health details" flow: launched with
  `OnboardingActivity.EXTRA_EDIT=true`, it pre-fills every step from the saved profile, shows a
  **Save** button, and `finish()`es back to Account instead of routing to Dashboard.
- Dancing Script may render as sans-serif on some devices despite the valid TTF; there's a
  programmatic `setTypeface` fallback in `IntroActivity`.
- Bluetooth (`DevicesActivity`) is assignment-mandated, framed as smart-scale pairing — real
  permission/enable/paired-list flow, no actual data transfer.

## Adding a new screen (checklist)
1. Decide the shell: tab (`BaseNavActivity`) / secondary with Up (`BaseSecondaryActivity`) /
   full-screen typing (`BaseTypingActivity`). Provide `getContentLayoutId()`.
2. Content layout leads with a bold `inter_bold` ~26sp heading (no toolbar title).
3. Use theme defaults — don't set OutlinedBox field styles or card elevation. Reuse the patterns
   above (sheets, chips, option rows, circular buttons).
4. Register the Activity in `AndroidManifest.xml` (`exported="false"`; add `adjustResize` if it
   has text input).
5. Strings in `strings.xml`, colors from `colors.xml`. Build with `.\gradlew.bat assembleDebug`,
   then install and screenshot-verify on the device.
