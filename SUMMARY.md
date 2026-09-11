# TAH M2 progress (0.3.0-m2)

## Ship gate

Board + streaming tool cards + permission cards remain wired. This is **not** a chat-only shell.

## M2 landed

| Goal | Status |
|------|--------|
| Skills & memory MVP | Markdown skill packs (bundled + paste-import), enable/disable, create/edit/delete memory notes; agent loop reads enabled packs + notes; empty/error copy |
| FG / wake | Lightweight `AgentRunForegroundService` while runs are active; Settings/About documents honest OEM limits + battery tips; no immortality claim |
| Brand polish | Signal Deck tokens; harness glyph (launcher + UI); witty empty states per UX pack |
| Onboarding | `SCR-ONBOARD` first-run → Providers or skip → Board; M1 upgrades with history auto-skip |
| Version | `0.3.0-m2` (versionCode 3) |
| AC checklist | [docs/AC-CHECKLIST.md](docs/AC-CHECKLIST.md) — S1–S11 honest green vs remaining |
| CI | Same `assembleDebug` workflow; deps unchanged except FG/service permissions |

## Works (playable on device)

| Area | Behavior |
|------|----------|
| AC-Board | Columns Working / Needs you / Done. Failed and Budget hit are **chips on Done**. Badge count = Needs-you sessions. Per-column empty copy + first-run empty install. Tap opens detail. |
| AC-Run | Dispatch starts a session. Demo or live SSE stream. Tool cards expandable. Budget → Done + Budget hit chip. FG notification while active. |
| AC-Permissions | Default **Ask**. Reject · Guide · Approve. Reject ends tool. Guide independent. Allow edits ≠ exec. |
| AC-Notify | `tah.needs_you` deep-link. Dismiss ≠ approve. |
| AC-Providers | BYOK or Ollama LAN; probe; model switcher; on-device keys; capability badge. |
| AC-Skills-Memory | Load/import markdown packs; memory CRUD; loop injects into system prompt. |
| AC-Modes-Lifecycle | Touch Steer default; Keys persists; FG service + OEM honesty; prefs survive process death. |
| AC-Brand-Ship | `app.tah.shell`, Signal Deck, harness glyph, GitHub Actions APK. |

## Demo-only / scaffolding (honest gaps)

- Tool **execution** is still receipt-only — no real shell/FS on device.
- Live BYOK/Ollama: after first model turn, still proposes a **single gated tool** (not full multi-tool function-calling).
- Import is paste-markdown (no SAF file picker yet).
- FG service raises priority; does **not** defeat OEM killers.
- Fresh installs start with an **empty** board (empty states visible). Prior M1 persisted graphs (incl. old demo seeds) still load.

## Locked rules honored

- `applicationId` `app.tah.shell` · display **TAH** · Signal Deck
- Failed = chip on Done · Reject ends tool · Guide independent · Allow edits ≠ exec · Touch Steer default
- No OFH / Claude / Warp / Termux / OpenClaw marks

## Out of M2

Play Console, real shell/FS tools, multi-agent, SAF skill import, signed Play release.
