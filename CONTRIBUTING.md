# Contributing to Keshu

Thank you for helping improve Keshu. Focused bug reports, compatibility findings, documentation fixes, and well-scoped code changes are welcome.

## Before you start

- Search existing [issues](https://github.com/huishingcheung/Keshu/issues) before opening a new one.
- Open an issue before beginning a large feature, architectural change, or new university integration.
- Keep the product and shared interfaces university-neutral. School-specific URLs, page fields, scripts, and parsing belong in that school's data-source adapter.
- Do not submit credentials, cookies, access tokens, student identifiers, authenticated URLs, or unredacted portal captures.

## Development setup

The project requires Android Studio with Android SDK 35 and JDK 17. Clone the repository, let Gradle finish syncing, and run the `app` configuration on an Android 8.0 or newer device or emulator.

Command-line checks use the included Gradle Wrapper:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew lintDebug
```

On Windows, replace `./gradlew` with `.\gradlew.bat`.

## Reporting bugs

Please include:

- the Keshu version and Android version;
- the affected screen or data source;
- clear reproduction steps;
- the expected and actual behavior;
- a redacted error report when the app provides one.

For portal import failures, describe the visible page state and field labels after removing all personal and authentication data. Synthetic minimal samples are preferred over real portal documents.

## Pull requests

1. Create a focused branch from the latest `main`.
2. Keep unrelated formatting or refactoring out of the change.
3. Add or update tests for behavior changes. Portal parser tests must use sanitized synthetic fixtures stored in the repository.
4. Update both `README.md` and `README.zh-CN.md` when public behavior or setup changes.
5. Run the complete checks listed above.
6. Explain the user-visible result, verification performed, and any remaining limitation in the pull request.

Pull requests should preserve local-first storage, explicit privacy boundaries, accessible UI behavior, and partial-success handling during academic imports.

## Licensing

Keshu is licensed under the [GNU General Public License version 3](LICENSE). By submitting a contribution, you agree that it may be distributed under `GPL-3.0-only`.

The project name and visual identity are addressed separately in [TRADEMARKS.md](TRADEMARKS.md). This does not change the rights granted to the source code by the GPL.
