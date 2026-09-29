# Days Matter Android

Kotlin + Jetpack Compose Android client for the existing Days Matter Cloudflare Worker and D1 database. The web app remains available and both clients use the same account and data.

## Features

- Login and registration with existing accounts
- Event list and detail, pull-to-refresh, category drawer filters, grid/list layout, pinning, repeat rules, and sub-events
- Solar and lunar date entry, using the same `lunar-java` family as the Worker’s lunar library
- Category color/icon management and the existing 83 category icon identifiers
- Webhook channels, profile editing, system-following dark mode, JSON import and export
- Device-local reminders, separate from Webhook channels; the next occurrence is scheduled using the Worker’s Asia/Shanghai date result
- Cached event data for offline reading; changes require a network connection, and syncing resumes when connectivity returns or the user pulls down to refresh

## Design language

The interface treats dates as a quiet journal: warm neutral surfaces, slate text and actions, and one dark summary card for the next event. Page gutters are 20 dp, card insets 18 dp, item gaps 12 dp, and section gaps 20 dp. The drawer combines a 12 dp outer inset with an 8 dp inner inset to keep the same alignment. Form fields are 56 dp high and action buttons are 48 dp high. Frosted blur is reserved for the floating navigation; content panels use solid surfaces and fine borders for readability. The shared values live in `ui/Theme.kt`.

## Build

Open this folder in Android Studio and sync Gradle. The project uses Android Gradle Plugin 9.4, Gradle 9.6, Compose BOM 2026.09.00, `compileSdk` 37, `minSdk` 26, and JDK 17 bytecode (JDK 21 can run Gradle).

Set Android Studio’s SDK location to this repository’s `.tools/android-sdk`, accept the Android SDK license in SDK Manager, and install Android SDK Platform 37, Build Tools 36.0.0, and Platform Tools before building. From a terminal, the interactive license step is:

```bash
ANDROID_USER_HOME="$PWD/.tools/android-user-home" ./.tools/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root="$PWD/.tools/android-sdk" --licenses
```

Then install the packages:

```bash
ANDROID_USER_HOME="$PWD/.tools/android-user-home" ./.tools/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root="$PWD/.tools/android-sdk" --install 'platforms;android-37.0' 'build-tools;36.0.0' 'platform-tools'
```

The default API endpoint is `https://dm.zwtx.top/api/`. To run against Wrangler from an Android emulator, use:

```bash
GRADLE_USER_HOME="$PWD/.tools/gradle-home" ./gradlew :app:assembleDebug -PapiBaseUrl=http://10.0.2.2:8787/api/
```

The SDK is kept in `.tools/android-sdk/` and Gradle distributions/caches in `.tools/`; both are ignored by Git. In Android Studio, set **Gradle user home** to this project’s `.tools/gradle-home` if you want IDE sync caches to stay here too. `local.properties` is machine-specific and must not be committed.

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Reminder behavior

Each device stores its own local reminder preference. The app schedules the next notification at the chosen time in Asia/Shanghai, requests notification permission when the user enables it, and restores pending alarms after reboot. Android power management can delay delivery. The app refreshes the next repeat occurrence after a successful sync; prolonged offline use does not calculate an unlimited sequence of future reminders.

## API and data

The Worker remains the source of truth for accounts, categories, events, Webhook channels, lunar/repeating next-occurrence calculations, and JSON backups. Android stores the session encrypted with Android Keystore and keeps a private local snapshot for offline reading. Device-local reminder choices are not included in server backups.

## Project records

- [CONTEXT.md](CONTEXT.md) — domain vocabulary
- [ADR 0001](docs/adr/0001-native-client-and-reminders.md) — backend boundary and local reminder ownership
- [Third-party notices](THIRD_PARTY_NOTICES.md)
