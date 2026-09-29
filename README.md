# Days Matter Android

[English](README.md) | [简体中文](README.zh-CN.md)

A native Kotlin + Jetpack Compose countdown app backed by [Days Matter for Cloudflare Workers](https://github.com/wynnok/DaysMatter). The web and Android clients share the same account and D1 data when connected to the same Worker.

## Features

- Login and registration with existing accounts. Password fields request an English password keyboard and accept only English letters, digits, and half-width symbols (ASCII 33–126; no spaces).
- Countdown lists and details, pull-to-refresh, category drawer filters, grid/list layouts, pinning, repeat rules, and sub-events.
- Solar and lunar dates, category colors and 83 category icon identifiers.
- Webhook channels, profile editing, a default avatar, JSON import/export, and light/dark/system appearance modes.
- Device-local reminders and cached events for offline reading. Changes require a network connection; syncing resumes on reconnection or pull-to-refresh.

## Architecture

```text
Android app ── HTTPS /api/ ── Cloudflare Worker ── D1
Web app     ── /api/ ────────┘        │
                               Cron → Webhook
Android device → local notifications
```

The Worker owns accounts, categories, events, Webhook channels, lunar/repeating next-occurrence calculations, and server backups. Android stores its session encrypted with Android Keystore and keeps a private offline snapshot. Local reminder choices and appearance settings belong to the device.

## Cloudflare backend setup

### 1. Get the backend

This repository contains the Android client. Obtain the Worker source from the [backend repository](https://github.com/wynnok/DaysMatter); its `package.json`, `wrangler.toml`, `schema.sql`, and `build.js` are needed below. A local `DaysMatter-CF/` folder, if present, is a reference copy and is not included in this repository's tracked files.

Run the following **inside the backend checkout**, using a Node.js version supported by its Wrangler dependency:

```bash
npm install --cache .tools/npm-cache
npx wrangler login
```

Wrangler is installed in the backend's `node_modules`; no global installation is needed. Login opens Cloudflare authorization in your browser.

### 2. Bind D1

If the web app already has a working Worker and D1 database, reuse that deployment and skip database creation and initialization. Point Android at its `/api/` address in the build section below.

For a new deployment:

```bash
npx wrangler d1 create days-matter-db
```

Update the backend's `wrangler.toml` with the returned database ID. Retain the backend's compatibility settings; the relevant configuration is:

```toml
name = "days-matter"
main = "src/index.ts"
compatibility_date = "2024-01-01"
compatibility_flags = ["nodejs_compat"]

[[d1_databases]]
binding = "DB"
database_name = "days-matter-db"
database_id = "YOUR_DATABASE_ID"

[triggers]
crons = ["*/5 * * * *"]

[vars]
ENVIRONMENT = "production"
```

The binding name must be `DB`, matching `env.DB` in the Worker. Use your own Worker name and database ID. The current authentication implementation does not read `JWT_SECRET`; the optional type declaration alone does not make it a required secret. Webhook URLs and channel credentials are configured through the app's reminder channels.

### 3. Initialize and deploy

For a **new remote database**, run:

```bash
npx wrangler d1 execute days-matter-db --remote --file=./schema.sql
npm run deploy
```

The schema defines `users`, `categories`, `remind_channels`, `events`, and `sub_events`. `npm run deploy` first builds the embedded web UI and lunar assets, then deploys the generated `src/index.ts`. Existing databases need migrations for any missing columns; rerunning `CREATE TABLE IF NOT EXISTS` does not upgrade existing tables.

Open the deployed Worker URL to use the web app, and check `https://YOUR_WORKER.YOUR_SUBDOMAIN.workers.dev/api/health` for `status: "running"`. Then register/sign in and create an event to verify the database binding; health alone does not query D1. You can use a custom HTTPS domain in place of the `workers.dev` address.

The configured cron scans reminders every five minutes. The Worker's `scheduled` handler is enabled; business dates and reminder times use Asia/Shanghai. Enable a Webhook channel and select it on an event to receive server reminders. Phone-local notifications run independently of this cron.

### 4. Local backend development

Inside the backend checkout:

```bash
npx wrangler d1 execute days-matter-db --local --file=./schema.sql
npm run build
npm run dev
```

Local D1 and remote D1 are separate. Local changes do not populate production. Rebuild after changing the web UI or generated assets. An Android emulator reaches the development server at `http://10.0.2.2:8787/api/`; a physical phone needs a reachable LAN address and Wrangler listening on that interface.

See Cloudflare's [D1 setup guide](https://developers.cloudflare.com/d1/get-started/) and [D1 CLI reference](https://developers.cloudflare.com/d1/wrangler-commands/) for database commands and local/remote options.

## Build the Android app

### Environment

Open this repository in Android Studio and sync Gradle. The project uses AGP 9.4, Gradle 9.6, Compose BOM 2026.09.00, `compileSdk` 37, `targetSdk` 36, `minSdk` 26 (Android 8.0), a Gradle daemon on JDK 21, and JDK 17 bytecode.

Configure your SDK path in Android Studio / the untracked `local.properties`. For a contained setup, use `.tools/android-sdk` and set **Gradle user home** to `.tools/gradle-home` using absolute paths. Gradle user home stores caches and must not be added to `PATH`. Keep machine-specific settings out of Git.

Install Android SDK Platform 37, Build Tools 36.0.0, and Platform Tools with SDK Manager. If the Android command-line tools are already installed in the project, run these commands from the Android repository root:

```bash
ANDROID_USER_HOME="$PWD/.tools/android-user-home" ./.tools/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root="$PWD/.tools/android-sdk" --licenses
ANDROID_USER_HOME="$PWD/.tools/android-user-home" ./.tools/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root="$PWD/.tools/android-sdk" --install 'platforms;android-37.0' 'build-tools;36.0.0' 'platform-tools'
```

### Set the API address and build

The default API address is `https://dm.zwtx.top/api/`. For your own backend, supply its base URL including `/api/`:

```bash
ANDROID_USER_HOME="$PWD/.tools/android-user-home" GRADLE_USER_HOME="$PWD/.tools/gradle-home" ./gradlew :app:assembleDebug -PapiBaseUrl=https://YOUR_WORKER.YOUR_SUBDOMAIN.workers.dev/api/
```

For the emulator and local Wrangler:

```bash
ANDROID_USER_HOME="$PWD/.tools/android-user-home" GRADLE_USER_HOME="$PWD/.tools/gradle-home" ./gradlew :app:assembleDebug -PapiBaseUrl=http://10.0.2.2:8787/api/
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The API address is compiled into the APK; rebuild after changing it. Debug permits HTTP for local development; release requires HTTPS.

For Android Studio builds, set `apiBaseUrl=https://YOUR_WORKER.YOUR_SUBDOMAIN.workers.dev/api/` in the `gradle.properties` under your configured Gradle user home (for example `.tools/gradle-home/gradle.properties`), then sync. Do not put it in `local.properties`, which is only used for the SDK path here.

### Installable release APK

In Android Studio, select **Build → Generate Signed App Bundle / APK → APK**, choose the `app` module, select or create your keystore, and build the `release` variant. The wizard shows the destination; use **Locate** after completion to find the signed APK. Creating a keystore alone does not build the app. This project has no automatic release signing configuration, so `assembleRelease` alone produces an unsigned APK.

Keep the keystore and passwords private and backed up; future updates need the same signing key. Do not commit keystores or generated APKs. Increase `versionCode` in `app/build.gradle.kts` for each published update.

### Verification

```bash
ANDROID_USER_HOME="$PWD/.tools/android-user-home" GRADLE_USER_HOME="$PWD/.tools/gradle-home" ./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Password-filter tests cover allowed ASCII, Chinese/full-width/emoji rejection, whitespace, and mixed pasted input. Keyboard layout depends on the installed IME: the app requests English password input and disables autocorrect; character filtering also applies to pasted text. Existing web passwords containing disallowed characters cannot be entered in this Android client.

## Reminders and troubleshooting

Local reminders use the next occurrence returned by the Worker and schedule the chosen time in Asia/Shanghai. The app requests notification permission when enabled and restores alarms after reboot. Power management can delay notifications; prolonged offline use does not calculate unlimited future repeats. Device-local reminder choices are not included in server backups.

| Symptom | Check |
| --- | --- |
| Login or sync fails | API address includes `/api/`, Worker is reachable, account belongs to that backend. |
| Health works but events fail | D1 binding is named `DB`, schema and required columns exist. |
| Release cannot reach the server | Use HTTPS; release disables cleartext HTTP. |
| Emulator cannot reach localhost | Use `10.0.2.2` for the host machine, not `localhost`. |
| Webhook reminders do not arrive | Cron and `scheduled` handler, enabled channel, event reminder time, public HTTPS Webhook URL; inspect `npx wrangler tail` in the backend checkout. |
| Local notifications do not arrive | Notification permission, device reminder setting, battery restrictions, and last successful sync. |

## Design language

Blue-white surfaces, a blue primary action, coral accents, and a blue-to-teal summary establish the hierarchy. Page gutters are 20 dp, card insets 18 dp, item gaps 12 dp, and section gaps 20 dp. Form fields are 56 dp and action buttons 48 dp. Shared values live in `ui/Theme.kt`. Glass treatment is reserved for floating navigation and actions; content panels remain readable.

List cards emphasize event names and day counts. Details lead with the countdown and next occurrence, then group origin date, repeat rules, and reminders below.

## Project records

- [Domain vocabulary](CONTEXT.md)
- [Backend boundary and local reminder ownership](docs/adr/0001-native-client-and-reminders.md)
- [Third-party notices](THIRD_PARTY_NOTICES.md)
