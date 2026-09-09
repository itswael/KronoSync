# KronoSync — Build Instructions

These are the working instructions for implementing KronoSync. The full product/feature/data/notification/UI spec lives in [day-schedule-app-spec.md](day-schedule-app-spec.md) — **read it in full before writing any code.** It is the single source of truth; this file only adds stack decisions, project conventions, and an execution order on top of it.

## What this is

KronoSync is a standalone **Android** app (Kotlin) for building an hourly day schedule, getting exact-time "do this now" notifications, gently checking whether the previous task was followed, and showing non-punitive analytics on how the day/week/month went. No backend, no ads, no accounts beyond optional Google sign-in for Drive backup. Local-first (Room/SQLite), everything else optional.

## Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 (see §7 of the spec for the full design system — dynamic color, Google Sans/Roboto type scale, M3 expressive shapes/motion). Do not use the legacy View system or Material 2.
- **Architecture:** MVVM — `ViewModel` + `StateFlow`/`UiState` exposed to Compose screens, unidirectional data flow (events up, state down). No business logic in Composables.
- **Persistence:** Room (entities per §3 of the spec: `Template`, `ScheduleBlock`, `DailyLogEntry`, `BackupSettings`, `AppSettings`).
- **Scheduling:** `AlarmManager` (`setExactAndAllowWhileIdle` / `setAlarmClock`) for start alerts, chained just-in-time per §4 — never bulk-schedule a month of alarms. `BroadcastReceiver` for `BOOT_COMPLETED` re-sync.
- **Navigation:** Navigation Compose, 3 top-level destinations (Schedule, Analytics, Settings) behind a `NavigationBar`.
- **DI:** Hilt.
- **Backup:** Google Drive Android API (App Data folder or user-visible folder — pick one and be consistent), only ever invoked after the §2.6 opt-in flow.
- **Testing:** JUnit + Turbine for ViewModel/Flow tests, Room in-memory DB for DAO tests, Compose UI tests for critical flows (quick-add, check-in, copy-day).

## Project structure (suggested)

```
app/src/main/java/.../kronosync/
  data/
    db/            Room entities, DAOs, Database class, migrations
    repository/    Repositories wrapping DAOs + Drive backup
    alarm/         AlarmManager scheduling + BroadcastReceivers
  domain/          Use cases where logic is non-trivial (copy-day, analytics aggregation, quote selection)
  ui/
    theme/         Color.kt, Type.kt, Shape.kt, Theme.kt (M3 theme per spec §7)
    schedule/      List + Grid screens, quick-add bottom sheet, block editor
    checkin/       Check-in bottom sheet + notification action handling
    analytics/     Day/Week/Month dashboard
    settings/
    navigation/    NavHost + top-level NavigationBar scaffold
  di/              Hilt modules
```

## Ground rules

- **Tone principle is non-negotiable** (spec §1): every string, color, and interaction is encouraging, never punitive. No red for missed/skipped tasks, no shaming streak resets, no "failure" language anywhere — including code comments, logs, and commit messages that touch user-facing copy.
- **UI must follow spec §7 exactly**: Material 3 theme tokens only (no hardcoded hex colors or one-off font sizes), M3 type scale, dynamic color support on Android 12+, both light and dark themes from the start.
- **Respect the non-goals** (spec §5): no backend server, no ads, no accounts beyond optional Google sign-in, no permanent progress archive (3-month rolling window only), no cross-platform work.
- **Copy-day independence** (spec §2.2): copying must always create independent `ScheduleBlock` rows. Never wire up a "live reference" back to a source day or template — this is a correctness requirement, not just a UI detail.
- **Alarms stay just-in-time** (spec §4): only ever have the next 1–2 start alerts scheduled at once; chain the next one when one fires; re-sync on app open and on boot.
- If a requirement in the spec is ambiguous or you have to make a judgment call, prefer the option that's simplest and most consistent with the tone principle — and flag it rather than silently deciding, especially for anything listed in spec §8 (Open Items).

## Implementation order

Work through these roughly in order — each builds on the last. Don't start backup/analytics before the schedule + alarm core is solid; that core is the riskiest and most load-bearing part of the app.

1. **Project scaffold** — Compose + Material 3 theme (light/dark, dynamic color per spec §7), Hilt setup, Navigation Compose shell with the three empty destinations behind a `NavigationBar`.
   - *Done when:* app builds and launches to an empty Schedule tab; toggling system dark mode and (on API 31+) wallpaper updates the theme live.
2. **Room schema** — entities/DAOs for `Template`, `ScheduleBlock`, `DailyLogEntry`, `BackupSettings`, `AppSettings` per spec §3.
   - *Done when:* DAO unit tests cover create/update/delete for each entity, including the template → independent-day-copy relationship.
3. **Schedule List UI** — quick-add `ModalBottomSheet` (`time + task name`, collapsed optional tag row per §2.5), list rendering grouped by day, edit/delete.
   - *Done when:* a user can add, edit, and delete a block end-to-end against Room, matching the visual spec in §7.5.
4. **Alarm-chaining for start alerts** (spec §4) — exact alarm scheduling, chain-to-next-on-fire, `BOOT_COMPLETED` re-sync, the plain-language pre-permission screen before the `SCHEDULE_EXACT_ALARM` system prompt.
   - *Done when:* a scheduled block fires a notification within the exact minute in manual testing, survives Doze, and correctly re-chains after a device reboot.
5. **Check-in flow** (spec §2.3) — batched notification with inline Done/Skipped/Partial actions writing `DailyLogEntry`, plus the in-app bottom-sheet equivalent.
6. **Grid/calendar view** (spec §2.1, §7.5) — alternate renderer over the same data as List, toggled via segmented control, tapping a block opens the same editor sheet.
7. **Copy-day flow** (spec §2.2) — copy to one or more target days, target day opens directly in the editor post-copy, independent rows guaranteed (see Ground rules above).
8. **Analytics dashboard** (spec §2.4, §7.5) — Day → Week → Month, in that order; tonal stat cards, non-red trend framing.
9. **Motivational quote bank** (spec §2.3) — bundled local quote bank, adaptive tone selection based on recent Done/Skipped/Partial mix. No network calls.
10. **Google Drive backup** (spec §2.6) — schedule object first, then progress object; opt-in prompt after ~3 days of real usage, not on first launch; two independent toggles.
11. **Settings screen** (spec §2.6, §7.5) — check-in batch interval, quote frequency, backup toggles, appearance (dynamic color on/off, AMOLED true-black toggle).
12. **Accessibility & tone pass** — contrast-check every custom-tinted status color in both themes, TalkBack labels on all notification actions and check-in controls, review empty-state and error copy against the tone principle.

## Definition of done (applies to every task above)

- Matches the referenced spec section(s) — re-read them, don't work from memory of this file alone.
- Uses Material 3 theme tokens (no hardcoded colors/fonts) per spec §7.
- No punitive/red framing anywhere in the touched UI or copy.
- Builds and runs on a Pixel emulator image at the project's target API level.
- New logic (DAOs, use cases, alarm scheduling, copy-day) has tests, not just a manual check.

## Implementation Log

- 2026-09-09: Initialized Android project scaffold per Implementation Step 1
   - Set up Gradle (AGP 8.5.2, Kotlin 2.0, Gradle 8.8 wrapper)
   - Created app module with Jetpack Compose + Material 3 theme
   - Added Hilt setup with `KronoSyncApp` and `@AndroidEntryPoint` `MainActivity`
   - Implemented `KronoNavHost` with three top-level destinations (Schedule, Analytics, Settings) behind a `NavigationBar`
   - Implemented dynamic color support and light/dark themes per spec §7
   - Added minimal icons and resources; app launches to empty Schedule tab placeholder

   - 2026-09-09: Implemented Room schema and DAOs (Step 2)
      - Added entities: Template, ScheduleBlock, DailyLogEntry, BackupSettings, AppSettings
      - Implemented DAOs with Flow observations and upserts for settings
      - Created Room database and Hilt module wiring
      - Added unit tests for Template and ScheduleBlock DAOs (in-memory DB)

   - 2026-09-09: Schedule List UI (Step 3)
      - Implemented Schedule List screen with quick-add bottom sheet (time minutes + task name)
      - Wired ViewModel (StateFlow) and Repository to Room DAO
      - Hooked into NavHost as the Schedule tab; uses Material 3 components

   - 2026-09-09: Build fix
      - Added Kotlin Compose Compiler Gradle plugin for Kotlin 2.0 (org.jetbrains.kotlin.plugin.compose)
      - Removed deprecated composeOptions kotlinCompilerExtensionVersion

   - 2026-09-09: Alarm-chaining scaffolding (Step 4)
      - Implemented AlarmScheduler using AlarmManager.setExactAndAllowWhileIdle
      - Added StartAlertReceiver to show notifications and chain next
      - Added BootRescheduleReceiver with rescheduler to re-sync after boot
      - Declared RECEIVE_BOOT_COMPLETED and SCHEDULE_EXACT_ALARM in manifest
      - Wired ScheduleViewModel to schedule on add; Notifier creates high-priority alerts

   - 2026-09-09: Check-in flow (Step 5)
      - Added inline notification actions (Done/Partial/Skipped) wired to `CheckInReceiver`
      - Implemented `CheckInRepository` to write `DailyLogEntry`
      - Added in-app `CheckInSheet` replicating the actions

   - 2026-09-09: Grid view (Step 6)
      - Added basic `ScheduleGridScreen` rendering the same data as list
      - Prepared navigation to allow a toggle (to be wired with segmented control)

   - 2026-09-09: Copy-day use case (Step 7)
      - Implemented `CopyDayUseCase` ensuring independent row creation
      - Extended DAO with day query to support copy-day

      - 2026-09-09: Analytics dashboard (Step 8)
               - Implemented Day, Week, Month analytics with aggregator and tonal stat cards
               - Wired Analytics tab to show Day view; Week/Month screens added and ready to integrate under tab UI

               - 2026-09-09: Motivational quote bank (Step 9)
                  - Bundled local quote bank with encouraging, gentle, and neutral sets
                  - Adaptive selection hooked into Analytics screens based on recent Done/Partial/Skipped

Next:
- Complete Step 2: Room schema for Template, ScheduleBlock, DailyLogEntry, BackupSettings, AppSettings
- Add unit tests for DAOs using in-memory Room database and Turbine
