# TAH SPEC — M3 (0.4.0-m3)

## Purpose

TAH (The Agent Harness) is a **phone-first Android shell** for steering one AI agent at a time. Home is a **Session Board**, not a chat transcript. Tools appear as cards. Risky tools stop on an Ask card. You approve, reject, or guide.

## Audience and install

- You, sideloading a debug APK from GitHub Actions
- Not a Play Store product in this milestone

## Non-goals (honest)

- Real on-device filesystem or shell execution
- Multi-agent orchestration
- Play Console listing
- Defeating OEM battery killers
- Full OpenAI function-calling JSON (planner is deterministic from the prompt + prior tools)

## Screens

| Screen | Primary actions |
|--------|-----------------|
| Onboard | Wire a provider or skip to Board |
| Board | Scan Working / Needs you / Done; open a session; start Dispatch |
| Dispatch | Prompt + skill + start run |
| Session detail | Stream, tool cards, Approve / Reject / Guide |
| Providers | Demo / BYOK / Ollama LAN, probe, model switch |
| Skills & Memory | Enable packs, SAF or paste import, memory CRUD |
| Settings | Ask / Allow reads / Allow edits, Touch Steer / Keys, OEM honesty |

## Data

- `AgentSession` + timeline items persisted on-device
- Skill packs (bundled + imported markdown)
- Memory notes (CRUD + `memory.write` from the loop)
- Provider keys in EncryptedSharedPreferences with fallback

## Permissions

- `INTERNET` / `ACCESS_NETWORK_STATE` — BYOK and Ollama
- `POST_NOTIFICATIONS` — Needs-you
- `FOREGROUND_SERVICE` + `DATA_SYNC` — active-run notification only
- SAF document picker — no extra storage permission

## Offline

Demo stream works with no key. Live stream requires a reachable OpenAI-compatible endpoint. Process death keeps session metadata and Needs-you; it does **not** auto-resume a mid-tool HTTP stream.
