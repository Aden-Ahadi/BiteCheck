# BiteCheck — Assignment Requirements Checklist

Tracks each assignment requirement against where it is (or will be) satisfied in the code.
Status: ✅ done · 🔨 UI built, logic pending · ⏳ planned (phase noted)

## Required activities (≥7; 14 named)

| Assignment name | Class | Status |
|---|---|---|
| SplashActivity | `ui/SplashActivity` | ✅ Phase 1 |
| LoginActivity | `ui/LoginActivity` | ✅ Phase 2 — internet + SQLite + Supabase health checks, then real sign-in |
| DashboardActivity | `ui/DashboardActivity` | ✅ Phase 4 — live calories/water/weight from SQLite, sync on resume |
| Sign_up_Activity | `ui/SignUpActivity` | ✅ Phase 2 — creates Supabase account (handles email-confirmation flow) |
| Reset_password_Activity | `ui/ResetPasswordActivity` | ✅ Phase 2 — sends Supabase recovery email |
| UserFeedbackActivity | `ui/UserFeedbackActivity` | ✅ Phase 4 — saves to Supabase `feedback` |
| SettingsActivity | `ui/SettingsActivity` | ✅ Phase 4 — reminder toggle + TimePicker + devices link |
| AccountActivity | `ui/AccountActivity` | ✅ Phase 4 — profile summary, edit (re-run setup), logout |
| PrivacyPolicyActivity | `ui/PrivacyPolicyActivity` | ✅ Phase 1 (static content) |
| ComplainsActivity | `ui/ComplainsActivity` | ✅ Phase 4 — saves to Supabase `complaints` |
| HelpActivity | `ui/HelpActivity` | ✅ Phase 1 (static content) |
| ContactActivity | `ui/ContactActivity` | ✅ Phase 1 (dial + email intents) |
| MainActivity | `ui/MainActivity` (chat meal logging — the app's main feature) | ✅ Phase 5 — Gemini parses messages to structured JSON, confirm dialog, offline keyword fallback |
| ReportActivity | `ui/ReportActivity` | ✅ Phase 6 — Daily/Weekly/Monthly tabs, MPAndroidChart calorie bars (+ target line), water bars, weight trend line, SMS share |

Extra activities: `OnboardingActivity`, `FoodAdvisorActivity`, `MealHistoryActivity`, `WaterTrackerActivity`, `WeightTrackerActivity`, `DevicesActivity` → **20 total**.

## Other requirements

| Requirement | Where | Status |
|---|---|---|
| Navigation Drawer or Bottom Navigation | Both — `ui/BaseNavActivity` (drawer + 4-tab bottom nav) | ✅ Phase 1 |
| Styles, colors, theme | `values/colors.xml`, `values/themes.xml` (+night), Material 3 | ✅ Yuka-style palette per docs/design.md |
| Menus | Drawer menu, bottom-nav menu, Dashboard options menu | ✅ Phase 1 |
| Handling user input | Auth forms with validation, onboarding form, feedback/complaint forms | ✅ Phase 1 |
| Date and time | `DatePickerDialog` (onboarding DOB + history filter), `TimePickerDialog` (reminder time) | ✅ Phase 4 |
| Launcher icon | Custom adaptive icon (fork & knife on brand green) | ✅ Phase 1 |
| Notifications | Daily reminder: `NotificationChannel` + `AlarmManager` + `ReminderReceiver`, time via TimePicker, runtime permission (33+) | ✅ Phase 4 |
| SMS | Reports → "Share via SMS": `SmsManager` + `SEND_SMS` runtime permission, intent fallback | ✅ Phase 4 |
| APIs & phone calls | Supabase REST + Gemini API (`data/remote/GeminiClient`); `ACTION_DIAL` in ContactActivity | ✅ Phase 5 |
| Network connection | `NetworkUtil` (ConnectivityManager) at login + before every sync/submit | ✅ Phase 2 |
| Bluetooth | `DevicesActivity` — permission flow, enable BT, paired-device list | ✅ Phase 4 |
| SQLite | `BiteCheckDbHelper` + Profile/Meal/Water/Weight DAOs, offline-first with `synced` flags | ✅ Phase 4 |
| Cloud database | Supabase Auth + Postgres via REST; `SyncManager` pushes unsynced rows | ✅ Phase 4 — run `docs/supabase-schema.sql` once in the Supabase SQL editor |
