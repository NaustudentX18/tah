# TAH SPEC — M5 (0.6.0-m5)

## Purpose

TAH (The Agent Harness) is a **phone-first Android shell** for steering one AI agent at a time. Home is a **Session Board** (Signal Deck), not a chat transcript. Tools appear as cards. Risky tools stop on an Ask card. You approve, reject, or guide.

## Audience and install

- You, sideloading a debug APK from GitHub Actions / Releases
- Not a Play Store product in this milestone

## Non-goals (honest)

- Shared device filesystem or unrestricted `/bin/sh`
- Multi-agent orchestration / `agent.spawn`
- Play Console listing
- Defeating OEM battery killers
- Guaranteed native OpenAI function-calling (heuristic planner remains the fallback)

## Screens

| Screen | Primary actions |
|--------|-----------------|
| Onboard | Wire a provider or skip to Board |
| Board | Scan Working / Needs you / Done; open a session; start Dispatch |
| Dispatch | Prompt + skill + start run |
| Session detail | Stream, tool cards, Approve / Reject / Guide / Stop |
| Providers | Demo / BYOK / Ollama LAN, probe, model switch |
| Skills & Memory | Packs (SAF + paste), enable/disable, notes, workspace (+ SAF export) |
| Settings | Ask / Allow reads / Allow edits, Touch Steer / Keys, OEM honesty |

## Data

- `AgentSession` + timeline items persisted on-device
- Skill packs (bundled + imported markdown)
- Memory notes (CRUD + `memory.write`)
- App-private workspace files (`fs.read` / `fs.write` / `fs.list`)
- Provider keys in EncryptedSharedPreferences with fallback

## Tool contract

| Tool | Effect |
|------|--------|
| `memory.write` | Persists a Memory note |
| `fs.read` / `fs.write` / `fs.list` | App-private workspace only |
| `clipboard.read` / `clipboard.write` | Device clipboard after Ask |
| `web.fetch` | HTTP GET of the URL on the card after Ask (32 KiB) |
| `shell.exec` | `date` / `echo` / `ls` in-process. Else refuse |

## Permissions

- `INTERNET` / `ACCESS_NETWORK_STATE` — BYOK, Ollama, fetch
- `POST_NOTIFICATIONS` — Needs-you
- `FOREGROUND_SERVICE` + `DATA_SYNC` — active-run notification only
- SAF document picker — skill import / workspace export; no broad storage permission

## Offline

Demo stream works with no key. Live stream and `web.fetch` need a network. Process death keeps session metadata and Needs-you; it does **not** auto-resume a mid-tool HTTP stream.

## Locks

Package `app.tah.shell`, Signal Deck, Failed chip-on-Done, no OFH branding.
