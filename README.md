# TAH — The Agent Harness

**Phone-first agent harness.** The home screen is a live **Signal Deck** board — not a chat transcript.

| | |
|---|---|
| Package | `app.tah.shell` |
| Version | **0.6.0-m5** |
| Min SDK | 26 (Android 8.0+) |
| UI | Jetpack Compose · Material 3 |

## Why TAH

Most mobile AI apps trap you in chat that never touches the device, or run tools with no visible receipt. TAH inverts that:

1. **Board first** — columns `Working` / `Needs you` / `Done` (Failed chip on Done when it fails).
2. **Cards for tools** — if it did not show up on a card, it did not run.
3. **Ask-default gates** — Approve / Reject / Guide. Reject ends that tool. Exec stays Ask. No silent full-bypass.
4. **Sandboxed local effects** — app-private workspace, clipboard, memory notes, allowlist shell, Ask-gated fetch.

## What is real (M5)

| Tool | Effect |
|------|--------|
| `fs.read` / `fs.write` / `fs.list` | App-private `filesDir/workspace` only |
| `clipboard.read` / `clipboard.write` | Device clipboard after Ask card |
| `memory.write` | On-device notes for later runs |
| `web.fetch` | HTTP GET of the card URL (32 KiB cap) after Ask |
| `shell.exec` | In-process `date` / `echo` / `ls` — refuses everything else |

**Not in product:** unrestricted `/bin/sh`, shared phone storage via tools, multi-agent swarm, Play Console.

## Quick start

1. Install the debug APK from [Actions](https://github.com/NaustudentX18/tah/actions) (`tah-debug-apk`) or build locally:
   ```bash
   ./gradlew :app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
2. **Providers** — Demo offline, or BYOK / Ollama LAN under Settings.
3. **Dispatch** — prompt + enabled skill pack → watch the board.
4. **Needs you** — Approve, Reject, or Guide.

## Skills & Memory

- Import markdown packs via **SAF** or paste.
- Enable/disable toggles what Dispatch can apply.
- Workspace tab lists agent files; **Export** uses SAF CreateDocument.

## Build & test

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

CI runs unit tests then `assembleDebug` on every push to `main`.

## Docs

- [SUMMARY.md](SUMMARY.md) — this slice
- [docs/ROADMAP.md](docs/ROADMAP.md) — honest path forward
- [docs/AC-CHECKLIST.md](docs/AC-CHECKLIST.md) — S1–S11 status
- [docs/GUIDE.md](docs/GUIDE.md) — operator notes

MIT · 2026 · TAH Project
