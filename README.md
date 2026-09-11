# TAH (Signal Deck) — M2 Installable APK

Android Kotlin + Jetpack Compose shell for **TAH**. Home is the **Session Board** (Working / Needs you / Done) with streaming tool cards and Ask-default permission gates — not a chat-only screen.

- **Package / applicationId:** `app.tah.shell`
- **Label:** TAH
- **minSdk 26 · compileSdk 35 · Compose + Material3 + Navigation**
- **Version:** 0.3.0-m2

## Sideload without a local SDK (recommended)

GitHub Actions builds a debug APK on every push to `main`:

1. Open [Actions](https://github.com/NaustudentX18/tah/actions)
2. Open the latest **Android debug APK** run
3. Download the **tah-debug-apk** artifact
4. `adb install -r` the APK (or open it on-device)

Tagged builds (`v*`) also publish a GitHub Release with `tah-debug.apk` attached.

Local recipe (if you have an Android SDK):

```bash
gradle :app:assembleDebug
# APK: app/build/outputs/apk/debug/
```

## What’s in M2

- Everything from M1 (live board, dispatch, stream + tool cards, Ask permissions, Needs-you deep-link, BYOK/Ollama)
- Skills & Memory MVP: markdown packs (bundled + paste-import), enable/disable, memory notes the agent loop reads
- Lightweight foreground service while runs are active + honest OEM / battery tips in Settings → About
- First-run onboarding (`SCR-ONBOARD` → Providers → Board)
- Signal Deck brand polish: harness glyph, witty empty states
- AC checklist: [docs/AC-CHECKLIST.md](docs/AC-CHECKLIST.md)

## Locks

- Failed / budget-hit = **chips on Done** (no fourth column)
- Reject ends the tool; Guide is independent
- Allow edits ≠ exec (shell stays Ask)
- Touch Steer default
- No OFH branding

See [SUMMARY.md](SUMMARY.md) for live vs demo-only gaps.
