# TAH (Signal Deck) — M0 Shell

Android Kotlin + Jetpack Compose shell for **TAH**. Home is the **Session Board** (Working / Needs you / Done), not a chat-only screen.

- **Package / applicationId:** `app.tah.shell`
- **Label:** TAH
- **minSdk 26 · compileSdk 35 · Compose + Material3 + Navigation**

## Open in Android Studio

1. Clone this repo.
2. Open the project root in Android Studio (Giraffe+ / Koala recommended).
3. Let Gradle sync (wrapper uses Gradle 8.9).

## Build debug APK

```bash
./gradlew :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

> First clone may need a Gradle wrapper jar. From Android Studio use **File → Sync**, or run `gradle wrapper` if you have Gradle installed.

## Sideload

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or drag the APK onto an emulator / use Android Studio **Run**.

## What’s in M0

- Board home with witty empty states + badge stub
- Session detail with timeline/stream stub + tool/permission card chrome stubs
- Dispatch (prompt / skill / model / budget stubs)
- Providers (BYOK / Ollama stubs)
- Skills & Memory stub
- Settings: permission mode, Touch Steer | Keys (default Touch Steer), notifications, about
- Signal Deck theme tokens

See [SUMMARY.md](SUMMARY.md) for M0 vs M1.
