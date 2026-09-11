# TAH M1 progress

## Ship gate

Board + streaming tool cards + permission cards are wired. This is **not** a chat-only shell.

## Works (playable on device)

| Area | Behavior |
|------|----------|
| AC-Board | Columns Working / Needs you / Done. Failed and Budget hit are **chips on Done**. Badge count = Needs-you sessions. Per-column empty copy. Tap opens detail. |
| AC-Run | Dispatch starts a session. Demo stream appends tokens into the timeline. Tool calls render as expandable cards. Iteration / wall-clock budget → Done + Budget hit chip (no hang). |
| AC-Permissions | Default **Ask**. Card: Reject · Guide · Approve. Reject ends that tool call. Guide is its own composer (never required after Reject). Allow edits ≠ exec (exec always Ask). No silent full-bypass. |
| AC-Notify | Entering Needs you posts `tah.needs_you`. Tap opens `tah://session/{id}?focus=permission`. Dismiss does not approve. POST_NOTIFICATIONS requested on API 33+. |
| AC-Providers | BYOK OpenAI-compatible endpoint + key (EncryptedSharedPreferences, fallback prefs) **or** Ollama LAN. Probe `GET /models`. Model switcher. Honest capability badge. Keys never leave the device via a TAH proxy. |
| Settings | Permission mode + input mode persist. Touch Steer is default. About documents OEM / FG honesty. |

Seeded demo sessions (`demo-working`, `demo-needs-you`, `demo-done`, `demo-failed`, `demo-budget`) ship so the board is not empty on first launch. Approving the seeded Needs-you card completes that run.

## Demo-only / scaffolding

- Offline / unconfigured provider uses a **fake token stream** + heuristic tool proposal (`fs.write` / `fs.read` / `web.fetch` / `shell.exec`).
- Live BYOK / Ollama uses a real OpenAI-compatible SSE client (`POST /v1/chat/completions`). After the first model turn, M1 still proposes a **single gated tool** (not full multi-tool function-calling).
- Tool **execution** is receipt-only — TAH does not actually write the filesystem or exec a shell on device in M1.
- Skills & Memory are light: bundled packs + simple notes the loop reads. Import polish is M2.
- Foreground service is **documented**, not shipped. Process death keeps session metadata + Needs-you + timeline snapshot; the run loop does not resume automatically.

## Out of M1 (M2)

Full skills/memory polish, FG service, signed release, brand polish.

## CI break risks

- New deps: `okhttp:4.12.0`, `security-crypto:1.0.0`, lifecycle ViewModel/Compose artifacts. All Maven Central.
- `assembleDebug` only (existing workflow). No unit-test task added.
- Cleartext permitted for Ollama LAN (`http://192.168.x`).
- EncryptedSharedPreferences falls back to plain prefs if Keystore init fails (runtime, not compile).
- Compose Material3 `LinearProgressIndicator(progress = { })` matches BOM `2024.10.01` / Material3 1.3.

## Locked rules honored

- `applicationId` `app.tah.shell` · display **TAH** · Signal Deck tokens
- Failed = chip on Done
- Reject ends the tool; Guide independent
- Allow edits ≠ exec
- Touch Steer default
