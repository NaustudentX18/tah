# TAH operator guide (0.6.0-m5)

Welcome to **TAH (The Agent Harness)**. This walkthrough covers setup, the Signal Deck, Ask cards, and the on-device tools that actually run.

---

## Installation & first run

### Getting the app
- **GitHub Actions / Releases:** download `app-debug.apk` and sideload (`adb install -r app-debug.apk` or open the APK on-device).
- **From source:** `./gradlew :app:assembleDebug` (or `.\scripts\build.ps1 -Install` with USB).

### Provider setup
TAH talks to your provider from the phone — no middleman proxy:
1. Open **Settings → Providers**.
2. **Demo (Offline):** UI + simulated stream, no network or key.
3. **Ollama on LAN:** e.g. `http://192.168.x.x:11434/v1`.
4. **BYOK** (OpenAI / Groq / OpenRouter / DeepSeek): Base URL + API key.
5. Tap **Probe** to verify connectivity and list models.

---

## Signal Deck (board-first)

Home is a board, not a chat log:

- **Working** — active run (stream + tool cards).
- **Needs You** — Ask card; Approve / Reject / Guide. Notification when parked here.
- **Done** — finished runs with chips (`Done`, `Failed`, `Budget hit`, `Cancelled`). Failed chip stays on Done.

### Needs You
- **Approve** — run exactly the tool shown.
- **Reject** — end that tool; no silent bypass.
- **Guide** — inject feedback into the loop (independent of Approve).

Ask-default. Exec-class tools stay Ask. No silent full-bypass.

---

## Tools (M5 — real vs refuse)

| Tool | Effect | Gate |
|------|--------|------|
| `memory.write` | Persists a Memory note | Auto or Ask |
| `fs.read` / `fs.write` / `fs.list` | App-private `filesDir/workspace` only | Ask / Allow reads or edits |
| `clipboard.read` / `clipboard.write` | Device clipboard | **Always Ask** |
| `web.fetch` | HTTP GET of the URL on the card (32 KiB cap) | **Always Ask** |
| `shell.exec` | In-process allowlist: `date`, `echo`, `ls`. Else honest refuse | **Always Ask** |

Not in product: unrestricted `/bin/sh`, shared device FS paths, `agent.spawn` / multi-agent swarm, Play Store.

---

## Skills & Memory

- **Skills:** markdown packs — SAF import, paste import, enable/disable; Dispatch applies enabled packs.
- **Memory:** notes injected into the system prompt; `memory.write` from tools.
- **Workspace:** app-private files the agent can `fs.*`; optional SAF **Export** from the Workspace tab.

---

## Battery & background

- Foreground service + notification with Stop while a run is active.
- Partial WakeLock during the loop.
- OEM killers still win sometimes — set App Info → Battery → **Unrestricted** if you need longer runs. Process death does **not** auto-resume a mid-tool HTTP stream.

---

## Developer commands

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```
