# KronoSync — Implementation Spec (v1)

## 1. Concept

**App name:** KronoSync.

A standalone Android app that helps users build an hourly (or finer) day schedule, get exact-time notifications on what to do next, gently check whether the previous task was followed, and see friendly, non-guilt-inducing analytics on how their day/week/month went.

No backend. No ads. No payments. Local-first storage, with optional, user-controlled Google Drive backup.

**Tone principle (applies to every screen, string, and notification):** the app should feel encouraging, never punitive. No "failure" language, no shaming streak resets, no red/alarming visuals for missed tasks.

---

## 2. Core Features

### 2.1 Schedule creation
- **List view (primary creation path):** quick-add rows of `time : task`. Fastest way to build or edit a day.
- **Grid/calendar view (secondary, overview):** visual block layout of the day/week. Tap a block to jump into edit (routes back through the same list-based editor — grid is a renderer, not a separate data path).
- **Granularity:** default hour blocks; any block can be split into finer intervals (30/15 min) by the user.
- **Recurring templates:** a day's schedule can be saved as a template and applied to future days (daily/weekly repetition).

### 2.2 Copy day-to-day, with per-day modification
- User can copy an existing day's schedule onto another day (or several days at once).
- Copying is **not a rigid clone** — after copying, the target day opens directly in the editor so the user can adjust just that day (move a block, delete one, add something new) without affecting the source day or the template it may have come from.
- Internally: copying creates an independent set of `ScheduleBlock` rows for the target date, decoupled from the source day's rows. Editing target-day blocks never mutates the source day.
- If the copy originated from a saved **template**, the target day is still a free-standing instance — later edits to the template do not retroactively change days that already copied from it (avoids surprise mutations to a day the user already customized).

### 2.3 Notifications & check-ins
- **Start alert:** fires at the exact start time of a block — "Now: [task]". Precision matters here; this is core to the app's value.
- **Check-in:** asks about the previous block(s), batched rather than per-block (e.g., every 2–3 hours, or user-configurable), answerable directly from the notification shade: `Done / Skipped / Partial`.
- **Motivational quotes:** periodic, frequency configurable by the user; tone adapts a little based on recent performance (encouraging after a rough stretch, celebratory after a good one) — computed locally from a bundled quote bank, no network call needed.

### 2.4 Analytics dashboard
- **Views:** Day / Week / Month.
- **Metrics:**
  - Hours planned vs. hours completed
  - Completion % (Done vs. Skipped vs. Partial)
  - "Followed schedule" hours vs. unplanned/idle hours
  - Trend vs. previous period (today vs. yesterday, this week vs. last week, this month vs. last month)
  - If the user has used optional tags: breakdown by tag (e.g., "6h work, 2h rest")
- **Presentation:** framed positively — progress and trends, not red/failure indicators.

### 2.5 Tasks — flat by default, tag optional
- Minimum required input to add a task: `time` + `task name`.
- An optional, collapsed "add tag" affordance sits below the field — visible, skippable, never required.
- Tag-based analytics only appear once a user has actually used tags.

### 2.6 Backup (Google Drive, fully optional)
- **Prompted**, not forced — surfaced after roughly 3 days of real usage (i.e., once there's something worth losing), not on first launch.
- **User selects what to back up**, via two independent checkboxes:
  - ☐ Schedule & templates (pre-checked by default)
  - ☐ Progress history — last 3 months (unchecked by default)
- Selection is saved as a setting and reused for all future backups (manual or auto) until the user changes it in Settings.
- **Storage structure:** schedule and progress are stored as two **separate** Drive objects/files, so toggling one off later cleanly removes just that piece without touching the other.
- **Progress sync scope (if enabled):** mirrors the same local 3-month rolling window — Drive never accumulates a permanent archive either. When local entries age out, the Drive copy prunes to match.
- Recovery flow: reinstall / new device → sign in to Drive (optional) → restore whichever objects exist.

### 2.7 Local data retention
- Progress/status data (per-block Done/Skipped/Partial history) is cached locally for a **rolling 3 months**, then auto-pruned. This is intentional — the app is about near-term trend comparison, not a permanent life-logging archive.
- Schedule/template data has no expiry — it persists until the user deletes it.

---

## 3. Data Model (Room/SQLite — suggested schema)

```
Template
- id (PK)
- name
- created_at

ScheduleBlock
- id (PK)
- date (nullable — null if this block belongs to a Template rather than a concrete day)
- template_id (nullable FK -> Template, set if this block originated from / belongs to a template)
- start_time
- end_time
- task_name
- tag (nullable)
- is_recurring_instance (bool) -- true if generated from a template but now independently editable
- created_at
- updated_at

DailyLogEntry
- id (PK)
- schedule_block_id (FK -> ScheduleBlock)
- date
- status (enum: DONE / SKIPPED / PARTIAL / UNMARKED)
- marked_at (nullable — when the user actually responded)

BackupSettings
- backup_schedule_enabled (bool)
- backup_progress_enabled (bool)
- last_backup_at (nullable)

AppSettings
- checkin_batch_interval_minutes
- quote_frequency
- exact_alarm_permission_granted (bool)
```

**Key design point:** `ScheduleBlock` rows for a specific `date` are always independent copies — never live references back to a `Template` or to another day's blocks. This is what makes "copy with per-day modification" (2.2) safe by construction: there's nothing shared to accidentally mutate.

---

## 4. Notification & Alarm Strategy

This is the trickiest technical piece, since precision and battery both matter.

- **Start alerts:** use `AlarmManager.setExactAndAllowWhileIdle()` (or `setAlarmClock()` for the strictest guarantee) — designed to survive Doze mode and fire at the exact intended minute.
- **Just-in-time scheduling:** do not schedule every alarm for the next month up front. Schedule only the next 1–2 upcoming alarms; when one fires, chain-schedule the next. Re-sync the chain on app open and on device boot (`BOOT_COMPLETED` receiver). This avoids OEM throttling around "too many exact alarms" and keeps background footprint minimal — no persistent background service needed.
- **Check-ins:** separate, lower-priority trigger than start alerts — can tolerate a few minutes of slack, batched per the user's configured interval.
- **Permission flow:** request `SCHEDULE_EXACT_ALARM` with a one-time, plain-language explanation screen before hitting the system prompt ("this app needs exact timing to actually work as a schedule") rather than relying on a generic OS dialog with no context.

---

## 5. Non-Goals (explicitly out of scope for v1)

- No backend server, no external database, no user accounts beyond optional Google sign-in for Drive
- No permanent/long-term progress archive (local or Drive) — 3-month rolling window only
- No ads, no in-app purchases, no premium tier
- No social/sharing features
- No cross-platform (Android only for v1)

---

## 6. Suggested Build Order

1. Room schema + local CRUD for `Template` / `ScheduleBlock` (list-view creation first)
2. Alarm-chaining logic for start alerts (get exact-timing right early — it's the riskiest piece)
3. Check-in notification + `DailyLogEntry` status writes
4. Grid/calendar overview view (reads from same data as list)
5. Copy-day flow with post-copy editing (2.2)
6. Analytics dashboard (day → week → month, in that order)
7. Motivational quote bank + adaptive selection
8. Google Drive backup (schedule object first, progress object second)
9. Settings screen (batch interval, quote frequency, backup toggles)

---

## 7. Visual Design & UI System (Pixel-style / Material 3)

KronoSync should look and feel like a modern Google Pixel app — Material Design 3 ("Material You"), not a generic/default Android look. This section is the source of truth for theming; screens should be built against these tokens rather than one-off values.

### 7.1 Design language
- Material Design 3 throughout — dynamic color, expressive shapes, adaptive layouts, edge-to-edge by default (draw under the status/nav bars, handle insets properly), matching current Pixel system apps.
- Full support for Android 12+ dynamic color (wallpaper-derived palette). Provide a curated static fallback palette (calm blues/greens, per the tone principle) for older devices or users who opt out of dynamic color.

### 7.2 Color system
- Use M3 color roles (`primary`, `secondary`, `tertiary`, `surface` + `surfaceContainer` tiers, `outline`, `error`) via the Compose theme — never hardcode hex values in UI code.
- **Tone principle override:** `error`/red is reserved strictly for real app errors (e.g., a failed save), never for user behavior. Map task status to non-alarming roles: Done → primary/success-tinted green, Partial → secondary/amber-tonal, Skipped → neutral (surfaceVariant/tertiary) — never red, never a crossed-out/"X" treatment.
- Both light and dark themes are required. Dark theme uses elevated `surfaceContainer` tiers rather than pure black, with an optional "true black" (AMOLED) toggle in Settings for battery savings on Pixel OLED screens.

### 7.3 Typography
- Display/headline/title styles: "Google Sans" (or Google Sans Text/Flex where licensing allows) to match Pixel system typography; body/label styles: Roboto or Roboto Flex. If Google Sans can't be bundled, fall back to Roboto Flex end-to-end with weight/width axes adjusted to approximate the same feel — don't substitute an unrelated font.
- Use the full M3 type scale (`displaySmall` → `labelSmall`) defined once in the theme's `Typography` — no ad hoc font sizes in individual screens.
- Must remain legible and unbroken at system font-scale up to at least 200%.

### 7.4 Shape & layout
- M3 expressive shapes: generous corner radii (12–28dp, larger on primary elements like the FAB and task cards, smaller on dense elements like chips).
- 8dp baseline spacing grid; 16dp screen margins (24dp on tablet/foldable widths via `WindowSizeClass`), single-pane list on phones, list+detail two-pane on larger screens.
- Bottom `NavigationBar` with three destinations — **Schedule**, **Analytics**, **Settings** — kept one-handed reachable, matching Pixel app conventions. A single FAB ("+ Add") on the Schedule tab drives quick-add.

### 7.5 Key screens
- **Schedule — List (primary):** scrolling list of time-slot cards; each row is a time chip + task name + optional tag chip. FAB opens a `ModalBottomSheet` (not a full-screen page) with `time + task name` per §2.5, plus a collapsed "add tag" affordance below — visible, skippable, never required.
- **Schedule — Grid/Calendar (secondary):** a segmented toggle switches List ↔ Grid in place; grid renders the same underlying data as tonal blocks, and tapping one opens the same block-editor bottom sheet used by List (grid is a renderer, not a separate data path — consistent with §2.1).
- **Check-in:** batched check-in is a notification with inline `Done / Skipped / Partial` actions; the in-app equivalent is a lightweight bottom sheet, never a blocking dialog.
- **Analytics:** Day/Week/Month via `SegmentedButton`; metrics as tonal stat cards plus simple bar/line charts using theme colors only (no red); trend deltas framed neutrally-to-positively (e.g., an arrow + "on par with last week"), never a red percentage.
- **Settings:** grouped `ListItem` sections (Notifications, Backup, Appearance), mirroring the Pixel Settings app's visual pattern.

### 7.6 Components (Material 3 / Compose)
- `NavigationBar`, `FloatingActionButton`, `ModalBottomSheet`, `SegmentedButton`, `AssistChip`/`FilterChip` (tags), `Card`/`ElevatedCard` (schedule rows, stat tiles), `Switch`, `Slider`, `Snackbar` (undo after delete/copy), progress indicators for completion %.
- Icons: Material Symbols, **rounded** style (matches the current Pixel icon set) — filled for active/selected state, outlined for inactive.

### 7.7 Motion
- M3 expressive, spring-based transitions (not linear) for navigation and bottom sheets; a shared-element-style expansion when a grid block opens into its editor.
- Keep motion quick and purposeful (~200–300ms) — this is a utility app opened often, not a showcase piece.

### 7.8 Empty states & tone-sensitive UI
- Empty schedule: friendly illustration + encouraging copy ("Nothing planned yet — let's block out your day"), never framed as a gap or failure.
- A broken streak is shown neutrally (counting simply resumes) — no crossed-out flame, no red "X", no shaming visual, per the tone principle in §1.

### 7.9 Accessibility
- Minimum 48dp touch targets; verify WCAG AA contrast on all custom-tinted status chips, in both light and dark themes.
- All notification quick-actions and check-in controls must be fully operable via TalkBack with clear content descriptions.

### 7.10 At-a-glance widget (stretch goal, non-blocking)
- Optional home-screen widget (Glance API) showing the next upcoming block, visually consistent with Pixel's "At a Glance" style. Not required for v1 — keep off the critical path per §5.

---

## 8. Open Items for Later (not blocking v1 start)

- Exact wording/copy for notification and dashboard strings (tone-check pass before release)
- Curated quote bank content and licensing (avoid copyrighted quote sources)
- Whether multi-day bulk copy (2.2, copying to several days at once) applies the same schedule to all target days before allowing edits, or opens each target day individually — worth deciding once the copy UI is being built
