# TAH (Signal Deck) — M0 Shell

Android Kotlin + Jetpack Compose shell for **TAH**. Home is the **Session Board** (Working / Needs you / Done), not a chat-only screen.

- **Package / applicationId:** `app.tah.shell`
- **Label:** TAH
- **minSdk 26 · compileSdk 35 · Compose + Material3 + Navigation**

## Sideload without a local SDK (recommended)

GitHub Actions builds a debug APK on every push to `main`:

1. Open [Actions](https://github.com/NaustudentX18/tah/actions)
2. Open the latest **Android debug APK** run
3. Download the **tah-debug-apk** artifact
4. `adb install -r` the APK (or open it on-device)

Tagged builds (`v*`) also publish a GitHub Release with `tah-debug.apk` attached.

## Open in Android Studio

1. Clone this repo.
2. Open the project root in Android Studio (Giraffe+ / Koala recommended).
3. Let Gradle sync (Gradle 8.9).

## Build debug APK locally

```bash
./gradlew :app:assembleDebug
# or: gradle :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## What’s in M0

- Board home with witty empty states + badge stub
- Session detail with timeline/stream stub + tool/permission card chrome stubs
- Dispatch (prompt / skill / model / budget stubs)
- Providers (BYOK / Ollama stubs)
- Skills & Memory stub
- Settings: permission mode, Touch Steer | Keys (default Touch Steer), notifications, about
- Signal Deck theme tokens

See [SUMMARY.md](SUMMARY.md) for M0 vs M1.
