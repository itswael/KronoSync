# KronoSync

KronoSync is a friendly, local‑first Android app that helps you plan your day by the hour, get exact-time "do this now" reminders, check in gently on progress, and review encouraging analytics — without accounts, ads, or a backend.

- Encouraging tone throughout — never punitive, never red for missed tasks.
- Local storage via Room/SQLite; optional Google Drive backup (opt‑in).
- Exact alarms chained just‑in‑time to save battery and survive Doze.
- Jetpack Compose + Material 3 with dynamic color and dark mode.

## Features
- Hourly schedule with quick add, edit, and delete
- Exact start notifications with inline Done/Partial/Skipped actions
- In‑app check‑in bottom sheet mirroring notification actions
- Copy day or week, always creating independent rows (no links back)
- List, Now (focus ring), and Week Grid views
- Non‑punitive analytics for Day → Week → Month
- Local quote bank with adaptive, kind motivation
- Optional Google Drive backup (App Data or user folder — opt‑in)

## Tech Stack
- Language: Kotlin
- UI: Jetpack Compose + Material 3 (dynamic color)
- Architecture: MVVM (`ViewModel`, `StateFlow`, unidirectional data flow)
- Persistence: Room (SQLite)
- Scheduling: `AlarmManager` (`setExactAndAllowWhileIdle`, `setAlarmClock`)
- Navigation: Navigation Compose (3 tabs: Schedule, Analytics, Settings)
- DI: Hilt
- Testing: JUnit, Turbine, Compose UI tests

## Project Structure
```
app/src/main/java/.../kronosync/
  data/
    db/            Room entities, DAOs, Database, migrations
    repository/    Repositories (DAOs + backup)
    alarm/         AlarmManager scheduling + receivers
  domain/          Use cases (copy‑day/week, analytics aggregation)
  ui/
    theme/         Material 3 theme (colors, type, shapes)
    schedule/      List/Grid/Now, quick‑add sheet, editors
    checkin/       Check‑in sheet + notification handling
    analytics/     Day/Week/Month dashboards + charts
    settings/      App/backup settings
    navigation/    NavHost + bottom NavigationBar
  di/              Hilt modules
```

See the implementation guide in [CLAUDE.md](CLAUDE.md) and the full product spec in [day-schedule-app-spec.md](day-schedule-app-spec.md).

## Getting Started
### Prerequisites
- Android Studio (Koala or newer) with Android Gradle Plugin 8.5+
- JDK 17 (Android Studio bundles one)
- Android SDK platforms and build tools installed

### Build & Run
- Open the project in Android Studio and click Run, or use the Gradle wrapper:

```powershell
./gradlew assembleDebug
./gradlew installDebug
```

### Tests
```powershell
./gradlew testDebugUnitTest
```

## Permissions & Behavior
- Exact alarms: prompts with a plain‑language explanation before directing to system settings. Required for minute‑accurate start alerts on Android 12+.
- Notifications: requested at runtime on Android 13+.
- Boot re‑sync: alarms re‑chained after device reboot (`BOOT_COMPLETED`).

## Design & Tone
- Material 3 throughout; dynamic color on Android 12+; light and dark themes
- No red for missed/skipped; language is supportive and non‑punitive by design

## Contributing
Issues and PRs are welcome. Please follow these guidelines:
- Keep the encouraging tone principle intact.
- Use Material 3 tokens (no hardcoded colors/fonts).
- Add tests for non‑trivial logic (DAOs, use cases, alarm scheduling).
- Keep copy‑day/week creating independent rows (no references to sources).

## License
This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.
