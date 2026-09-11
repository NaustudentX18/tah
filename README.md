# TAH (Signal Deck) — M1 Playable TUI

Android Kotlin + Jetpack Compose shell for **TAH**. Home is the **Session Board** (Working / Needs you / Done) with streaming tool cards and Ask-default permission gates — not a chat-only screen.

- **Package / applicationId:** `app.tah.shell`
- **Label:** TAH
- **minSdk 26 · compileSdk 35 · Compose + Material3 + Navigation**
- **Version:** 0.2.0-m1

## Sideload without a local SDK (recommended)

GitHub Actions builds a debug APK on every push to `main`:

1. Open [Actions](https://github.com/NaustudentX18/tah/actions)
2. Open the latest **Android debug APK** run
3. Download the **tah-debug-apk** artifact
4. `adb install -r` the APK (or open it on-device)

Tagged builds (`v*`) also publish a GitHub Release with `tah-debug.apk` attached.

## What’s in M1

- Live Session Board: Working / Needs you / Done + Failed & Budget-hit **chips on Done** (no 4th column)
- Seeded demo sessions plus a real in-memory session graph (survives process death)
- Dispatch starts a run with prompt / skill / model / iteration + wall-clock budget
- Timeline streams tokens (offline demo stream, or BYOK / Ollama OpenAI-compatible SSE)
- `CMP-TOOL-CARD` expandable tool receipts
- `CMP-PERMISSION-CARD` Approve / Reject / Guide (Ask default; Reject ends the tool; Guide is independent)
- Needs-you notification (`tah.needs_you`) deep-links to the permission card — dismiss ≠ approve
- Providers: BYOK OpenAI-compatible or Ollama LAN; model switcher; keys on-device; honest offline badge
- Settings persist permission mode + Touch Steer / Keys (Touch Steer default)

See [SUMMARY.md](SUMMARY.md) for what is live vs demo-only.
