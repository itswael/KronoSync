# Contributing to KronoSync

Thanks for your interest in contributing! Our north star is an encouraging, non‑punitive experience. Please keep this tone principle in mind across code, copy, and reviews.

## Development
- Android Studio Koala+; JDK 17
- Build: `./gradlew assembleDebug`
- Tests: `./gradlew testDebugUnitTest`

## Guidelines
- Use Material 3 tokens (no hardcoded colors/fonts) and follow the theme setup.
- Keep business logic in ViewModels/use cases — not in Composables.
- Maintain copy‑day/week independence (no links back to source rows).
- Add tests for DAOs, use cases, and alarm scheduling logic.
- Prefer small, focused PRs with clear scope and screenshots for UI changes.

## Commit messages
- Use imperative mood, e.g., "Add", "Fix", "Refactor".
- Reference issues with `Fixes #123` when applicable.

## Code of Conduct
By participating, you agree to follow our [Code of Conduct](CODE_OF_CONDUCT.md).
