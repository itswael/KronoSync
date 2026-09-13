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
      - Added BootRescheduleReceiver with rescheduler to re-sync after boot (goAsync + coroutine)
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

               - 2026-09-09: Settings screen (Step 11)
                  - Implemented Settings screen with backup toggles, appearance controls, check-in interval, and quote frequency sliders
                  - Wired AppSettings and BackupSettings repos; Hilt-provided DAOs

   - 2026-09-09: Backup scaffolding (Step 10)
      - Added serializable backup DTOs for schedule/progress
      - Implemented DriveBackupService (placeholder writes JSON to internal storage)
      - Wired BackupRepository to serialize data and trigger exports
      - Added "Backup now" actions in Settings
      - Added BackupOptInManager (3-day heuristic by first log timestamp) and gentle opt-in dialog scaffold
      - Persisted opt-in prompt state in AppSettings (shown/dismissed) and prevent re-prompts
      - Bumped Room schema to v2 (temporary fallbackToDestructiveMigration during dev)

   - 2026-09-09: Room schema export
      - Applied AndroidX Room Gradle plugin and wired schemaDirectory to app/schemas
      - Generated initial schema JSON (version 1) under app/schemas/com.kronosync.data.db.KronoDatabase/1.json

Next:
- Integrate Google Drive API writes (App Data)
- Gate backups on toggles (done for placeholder writer)
- Navigate from opt-in dialog to Settings (done)
- Add DAO tests for new fields and backup flow

- 2026-09-12: Build tooling + crash fixes + UI overhaul (Claude, not Copilot)
   - **Build tooling was broken**: `gradle/wrapper/gradle-wrapper.jar` was corrupted (missing wrapper support classes — threw `NoClassDefFoundError` for `org.gradle.wrapper.IDownload`), so `./gradlew` could not run from the command line at all. Regenerated it from a local Gradle 8.13 install. Also found ~400MB of full Gradle distributions (`gradle-8.7/8.8/8.13` dirs + zips) committed straight into the repo root from earlier troubleshooting — removed them from disk and the git index, added `/gradle-*/` and `/gradle-*.zip` to `.gitignore`. **Note:** these were already committed in earlier history (`gradle-8.13-bin.zip` alone is ~136MB, over GitHub's 100MB per-file hard limit) — if this repo is ever pushed to a GitHub remote, that push will be rejected until history is rewritten (e.g. `git filter-repo`) to purge those blobs. Not done automatically since it changes commit SHAs; ask before doing it.
   - **Critical crash fix**: `AlarmScheduler.scheduleExact()` called `AlarmManager.setExactAndAllowWhileIdle()` with no permission check. On Android 12+ (this app targets API 34), `SCHEDULE_EXACT_ALARM` is not auto-granted, so this threw `SecurityException` on the very first "add block" action. Added `ExactAlarmPermission` helper (`canScheduleExactAlarms` / `openExactAlarmSettings`), made `scheduleExact()` return `Boolean` instead of crashing, and added the spec §4 "plain-language explanation" dialog in `ScheduleScreen` that routes to system settings when permission is missing.
   - **Critical silent-failure fix**: `POST_NOTIFICATIONS` was never declared in the manifest or requested at runtime, so on Android 13+ no notification (start alert or check-in) could ever appear. Added the manifest permission and a runtime request from `MainActivity` on launch.
   - **Data-integrity fix**: notification check-in actions (`Notifier.buildAction`) never included `EXTRA_BLOCK_ID` in the `CheckInReceiver` intent, so every check-in logged from a notification button had `blockId = null`. Also fixed a `PendingIntent` request-code collision (all three actions across every block used the same `status.hashCode()` code, so `FLAG_UPDATE_CURRENT` could clobber a different block's pending intent). Threaded `blockId` through `StartAlertReceiver` → `Notifier` → the action intents, and made request codes unique per (block, status).
   - **Dead UI fixed**: the visible "copy day" icon in `ScheduleScreen`'s top bar was a `/* TODO */` no-op (a working copy flow existed only inside `ScheduleListScreen`, reachable through a confusing tri-state Dial/Grid/List icon-toggle that could get stuck). `CheckInSheet` (the in-app check-in bottom sheet from spec §2.3) was fully built but never invoked anywhere. Restructured `ScheduleScreen` to own a single `Scaffold` (fixes a double-`TopAppBar`/overlapping-`Box` layout bug from the old sibling `TopAppBar` + `Box` structure) with a clear List/Grid/Now `SegmentedButton` row, a working FAB, and a working copy action; `ScheduleListScreen` is now a pure renderer that opens `CheckInSheet` when an already-started block is tapped.
   - **Settings not applied**: dynamic-color and AMOLED-true-black toggles were persisted to `AppSettings` but `KronoSyncTheme` in `MainActivity` never read them back — toggling had zero visible effect. Wired `SettingsViewModel` state into `KronoSyncTheme(dynamicColor=…, amoledTrueBlack=…)`.
   - **Visual design system implemented** (spec §7 was not implemented at all before this): `Typography`/`Shapes` were the bare M3 defaults (`Typography()`, `Shapes()`), and the color seed was the stock M3 tutorial purple (`#6750A4`). Added `ui/theme/Color.kt` with a curated calm teal/indigo light+dark palette plus an AMOLED true-black variant, and non-red `StatusColors` for Done/Partial/Skipped (tone principle: `error` role reserved for real errors only). Filled in a full M3 type scale in `Type.kt` and an expressive corner-radius scale in `Shape.kt`. Restyled `ScheduleListScreen` (card rows, empty state), `ScheduleGridScreen` (was showing raw `@480`-style minute integers — now formatted, tonal cards), `AnalyticsScreens` (tonal stat-tile cards with icons instead of plain `Card { Text(...) }`, plus a new trend-vs-previous-period comparison that spec §2.4 asked for but was missing entirely), `SettingsScreen` (grouped `ListItem` sections with icons instead of a flat `Text`/`Switch` column), and `CheckInSheet` (status-colored buttons, encouraging copy). Replaced `CopyDaySheet`'s raw comma-separated-date text field with a `DatePicker` + removable date chips.
   - Verified: `./gradlew assembleDebug` succeeds cleanly (zero warnings), and the resulting APK installs and launches without crashing on a physical device via `adb`. Full manual click-through/visual QA on-device was not done this session (device was locked) — worth doing before considering the UI pass final.
   - Not touched: Google Drive backup is still the placeholder JSON-to-local-file writer from Step 10 — real Drive API integration remains open.

- 2026-09-13: Timezone bug fix, edge-to-edge, duration/edit/copy features, week grid, charts (Claude)
   - **Root-cause fix for "notification fires immediately instead of at start time"**: every `dayEpoch` in the app (block storage, quick-add date picker, copy-day picker, analytics period boundaries) was computed as **UTC midnight** (`LocalDate.atStartOfDay().toInstant(ZoneOffset.UTC)`) while `startMinute` is local wall-clock minutes-since-midnight. In any non-UTC timezone this silently shifted every block's absolute trigger time by the device's UTC offset — e.g. in UTC-4, a 5pm block computed a trigger instant of 1pm local, which if already past fires the alarm the instant it's created (`AlarmManager` fires immediately for any trigger time ≤ now). Added `util/DayEpoch.kt` (`DayEpoch.of(date)` uses `ZoneId.systemDefault()`, not UTC) and replaced every UTC-based epoch computation with it — `ScheduleViewModel`, `AnalyticsScreens`, `AddOrCopySheet` (the old `CopyDaySheet`). Note: Compose's `DatePickerState.selectedDateMillis` is a special case that genuinely is always UTC-midnight internally (a Compose quirk, not related to this bug) — extracting the picked `LocalDate` from it still correctly uses `ZoneOffset.UTC`, only the *subsequent* conversion back to a `dayEpoch` was fixed to use local zone. Known remaining limitation: block trigger times are still computed as `dayEpoch + startMinute*60000`, which is off by an hour on the two DST-transition days per year — fixing that fully would mean computing each trigger fresh via `LocalDateTime.atZone(zone).toInstant()` rather than precomputed arithmetic; not done this pass.
   - **Week now starts Monday everywhere**, regardless of device locale: added `util/WeekStart.kt` (`TemporalAdjusters.previousOrSame(MONDAY)`), replacing the old `WeekFields.of(Locale.getDefault())` in `AnalyticsScreens` (which started weeks on Sunday for a US locale) and using it in the new week-grid view and week-copy flow.
   - **Edge-to-edge / full-screen**: this was also the fix for the purple status bar (it was the Material Components library's default `colorPrimary`, since `themes.xml` never set one). Added `enableEdgeToEdge()` in `MainActivity.onCreate()`, set a real `colorPrimary` + light/dark `windowBackground` in `values/themes.xml` and new `values-night/themes.xml` (avoids a purple flash before Compose draws). Removed the per-screen `TopAppBar`/`Scaffold` topBar from `ScheduleScreen`, `AnalyticsTabScreen`, and `SettingsScreen` — each now applies `Modifier.statusBarsPadding()` directly and shows a plain large-title `Text` instead, since the bottom `NavigationBar` already communicates which tab you're on.
   - **Duration on add/edit**: `AddOrCopySheet`'s "New task" tab and the new `EditBlockSheet` both have a duration `FilterChip` row (15/30/45/60/90/120 min, defaulting to 60) instead of the old hardcoded 60-minute block.
   - **Copy moved out of the title bar into the add flow**: replaced the standalone `CopyDaySheet` with `ui/schedule/AddOrCopySheet.kt` — a single sheet (opened from the FAB) with a 3-way segmented control: **New** (task form), **Copy day** (same date-chip picker as before), **Copy week** (new — pick any day in a target week, copies the whole Mon–Sun span). `CopyDayUseCase` gained `copyWeek()` alongside the existing `copyDay()`, both still producing fully independent rows per the spec's copy-independence rule. Hour-level copy ("copy an hour to the current hour") lives in `EditBlockSheet` as a "Copy to current hour, today" button — duplicates that block's title/tag/duration to today's current hour via `ScheduleViewModel.copyBlockToNow()`.
   - **Edit added for current/future tasks, List and Grid only**: new `EditBlockSheet` (title, time, duration, save/delete/copy-to-now). A pencil icon on each `ScheduleListScreen` row opens it; tapping a `ScheduleGridScreen` tile does too. Both are gated to blocks that haven't fully ended yet (`block.startMinute + block.durationMinutes` vs. now) — a task already in the past shows no edit affordance, matching what was asked. The "Now" (dial) view intentionally has no edit entry point.
   - **List view**: now finds the in-progress block (`startMinute <= nowMinute < startMinute+duration`), visually emphasizes it (`primaryContainer`, a "Now" badge), and auto-scrolls the `LazyColumn` to it on load so today's past/future blocks are still reachable by scrolling either direction.
   - **Grid rebuilt as an actual week table** (previously a 2-column `LazyVerticalGrid` of just today's blocks, not a week view at all): `ui/schedule/grid/WeekGridViewModel.kt` observes the Mon–Sun range (`ScheduleBlockDao.observeBetweenDays`, new query) and joins each block against its latest `DailyLogEntry` status. `ScheduleGridScreen` renders a fixed hour-ruler (0–23h) beside 7 horizontally-scrollable day columns, each block absolutely positioned by `startMinute`/`durationMinutes` and colored by status (`StatusColors`, never red) — closer to the literal Outlook/Teams week-grid the request described than the old per-day grid was.
   - **"Now" screen redesign** (was a plain auto-scrolling vertical list, functionally fine but not distinctive): now a circular "focus ring" (`Canvas`/`drawArc`) showing elapsed-time progress through the current block with title/time-left in the center, an "All clear" / "Free time, `<next task>` in `<countdown>`" state when nothing is active, and a motivational quote card below it (`QuoteBank`, instantiated directly — it has no dependencies, so this bypasses Hilt for something this trivial). Order is Today label → ring → quote, per follow-up feedback (see below).
   - **Analytics charts**: `AnalyticsAggregator.activityBreakdown()` (new) sums `ScheduleBlock.durationMinutes` grouped by tag (falling back to title when untagged) over a date range, folding anything past the top 6 into "Other". `ui/analytics/ActivityCharts.kt` renders this as a hand-drawn Canvas pie chart + a proportional horizontal bar list, shown on the Week and Month tabs (not Day — a single day's 2-3 blocks aren't meaningful as a pie). No charting library added — kept to Compose `Canvas` to avoid a new dependency for something this simple.
   - Verified: `./gradlew testDebugUnitTest assembleDebug` passes cleanly.

- 2026-09-13 (follow-up, same day): fixed three issues found from an actual on-device screenshot pass (device reconnected)
   - **Large gap between status bar and screen title, on every tab**: root cause was double-counted status-bar inset. The outer `KronoNavHost` `Scaffold` has no `topBar` (each screen owns its own header), so its default `contentWindowInsets` pushed the status-bar inset into the `padding` handed to the `NavHost` — and then each screen's own `Modifier.statusBarsPadding()` applied it a second time. On this device the status bar is ~44dp (camera-cutout Pixel-style), so doubled it was a very visible ~88dp gap. Fixed by setting `contentWindowInsets = WindowInsets(0,0,0,0)` on both `KronoNavHost`'s `Scaffold` and `ScheduleScreen`'s own `Scaffold` (used for its FAB) — each screen's manual `statusBarsPadding()` is now the only place the inset is applied. Confirmed fixed via `adb exec-out screencap` on Schedule, Analytics, and Settings.
   - **"Now" screen showed "nothing planned" twice**: the ring's own empty state ("All clear — Nothing left scheduled today") and a separate mini-timeline empty state ("Nothing planned yet today") were both visible at once. Removed the mini-timeline entirely (it wasn't part of the requested layout) and replaced it with a motivational quote card.
   - **"Now" screen layout order**: was ring → "Today" label → timeline. Reordered to "Today" label (now center-aligned) → ring → quote card, per explicit request.
   - Verified all three fixes live via `adb`: installed the rebuilt APK on the connected device, force-stopped and relaunched, and screenshotted Schedule (Now/List/Grid), Analytics, and Settings. Gap is now a normal single status-bar inset on all screens; Now screen shows the correct single-message, correctly-ordered layout. Grid was also visually confirmed rendering a real week table with a pre-existing "test" block correctly positioned by day/hour.

- 2026-09-13 (second follow-up, same day): Grid focus + edge padding, Now-screen centering, from a second on-device pass
   - **Grid wasn't focusing on today/current hour**: `ScheduleGridScreen` had independent `horizontalScroll`/`verticalScroll` states with no initial scroll target at all — it always opened at Monday 12 AM. Wrapped the scrollable body in `BoxWithConstraints` to get real viewport dimensions, and added a `LaunchedEffect(todayIndex)` that `animateScrollTo`s both axes once `WeekGridViewModel`'s data loads: horizontally centers today's day-column (clamped at the week's edges by `ScrollState`'s own bounds — confirmed via screenshot that viewing the last day of the week correctly shows it flush against the right edge with the preceding days for context, rather than trying to over-center past the end of available content), vertically centers the current time-of-day.
   - **Hour ruler touching the left screen edge**: added `Modifier.padding(start = 8.dp)` to `ScheduleGridScreen`'s root `Column`.
   - **"Now" screen elements too small / not vertically centered**: the real bug was that `ScheduleDialScreen(vm = vm)` was placed in `ScheduleScreen`'s `Column` without `Modifier.weight(1f)`, so it (and `ScheduleListScreen`/`ScheduleGridScreen`, though harmless for those two) was being measured against the **full** Scaffold content height rather than the actual remaining space below the segmented-button row — its internal `Arrangement.Center` was centering within an invisible box far taller than the visible viewport, which is why content hugged the top with a large dead zone below rather than appearing centered. Wrapped the `when (view)` block in `Box(Modifier.weight(1f).fillMaxWidth())` so each view gets the correct bounded space, and added `verticalArrangement = Arrangement.Center` to `ScheduleDialScreen`'s root `Column`. Also enlarged the ring (220dp → 268dp, stroke 14dp → 16dp), "Today" label (titleSmall → titleMedium), in-ring text sizes, and the quote card's padding/text size, per "make the elements a little bigger."
   - Verified all three live via `adb`: real test data on the device showed the ring correctly rendering an in-progress task's elapsed-time arc for the first time (previous checks only had the empty "All clear" state), Grid opened already scrolled to the current week's day/hour, and List/Grid's edit-affordance gating was incidentally re-confirmed (a past block showed no pencil icon, the current and a future block both did).
