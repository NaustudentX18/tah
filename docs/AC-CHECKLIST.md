# TAH AC checklist — M2 (0.3.0-m2)

Honest status vs PRD §6 and S1–S11. **Green** = met in this APK. **Partial** = usable but with noted gaps. **Remaining** = not done.

Legend: ✅ green · 🟨 partial · ⬜ remaining

## S1–S11 capability map

| ID | Capability | Status | Notes |
|----|------------|--------|-------|
| S1 | Native Android shell | ✅ | Compose UI; not WebView-chat-only |
| S2 | Session board | ✅ | Working / Needs you / Done; Failed & Budget hit chips on Done |
| S3 | Single-agent run loop | 🟨 | Stream + tool cards + budgets work; tool exec is receipt-only; single gated tool after first turn |
| S4 | Permission modes | ✅ | Ask default / Allow reads / Allow edits; no silent full-bypass; exec stays Ask |
| S5 | Permission cards | ✅ | Approve / Reject / Guide separate; Reject ends tool; Guide independent |
| S6 | Needs-you notifications | ✅ | Local notif + deep-link; dismiss ≠ approve |
| S7 | Provider wizard | ✅ | BYOK + Ollama LAN; model switcher; on-device keys; capability badge |
| S8 | Skills + memory | ✅ | Markdown packs (bundled + paste-import); memory CRUD; loop reads them |
| S9 | Touch Steer + Keys | ✅ | Touch Steer default; Keys toggle persists; same session model |
| S10 | Foreground / wake | 🟨 | FG service for active runs + documented OEM limits; not immortal; no auto-resume mid-tool after death |
| S11 | Distinct TAH brand | ✅ | `app.tah.shell`, Signal Deck, harness glyph; no OFH/third-party marks |

## Detailed ACs

### AC-Board
- [x] Columns Working / Needs you / Done
- [x] Failed / budget-hit as chips on Done — no fourth column
- [x] Tap → detail with stream + tool timeline
- [x] Empty states per column (witty UX copy) + first-run empty install
- [x] Needs-you badge matches awaiting sessions

### AC-Run
- [x] Start single-agent run with prompt + optional skill
- [x] Tokens stream on-device (demo or SSE)
- [x] Compact expandable tool cards
- [x] Iteration / wall-clock budget → clear Done/failed, not hang
- [ ] Full multi-tool function-calling loop — **remaining** (single gated tool after first turn)
- [ ] Real FS/shell execution — **out of MVP scope** (receipt-only)

### AC-Permissions
- [x] Ask default for write/exec-class
- [x] Approve / Reject / Guide separate
- [x] Reject ends tool (no Guide required)
- [x] Guide injects and continues
- [x] No silent full-bypass UI
- [x] Allow reads / Allow edits explicit in Settings
- [x] Allow edits ≠ exec

### AC-Notify
- [x] Needs-you posts local notification
- [x] Deep-link to permission card
- [x] Dismiss does not auto-approve

### AC-Providers
- [x] First-run / Settings wizard BYOK or Ollama
- [x] Keys on-device (EncryptedSharedPreferences + fallback)
- [x] Model switcher
- [x] Honest offline / capability badge

### AC-Skills-Memory
- [x] Load markdown skill pack and apply to a run
- [x] Create/edit memory notes the loop can read
- [ ] SAF / file-picker import — **remaining** (paste-import works)

### AC-Modes-Lifecycle
- [x] Touch Steer default
- [x] Keys persists; same session model
- [x] FG/wake documented; OEM tips; no immortal claim; FG service for active runs
- [x] Process death: metadata + Needs-you survive
- [ ] Automatic mid-tool loop resume after death — **remaining** (honest: user re-opens; state preserved)

### AC-Brand-Ship
- [x] applicationId / name TAH-specific
- [x] No OFH / Claude / Warp / Termux / OpenClaw branding
- [x] Installable APK via CI Actions artifact + documented local recipe

## Ship gate

| Gate | Status |
|------|--------|
| Session board | ✅ |
| Approvals | ✅ |
| Streaming tool cards | ✅ |

**Do not ship Play** until product owners accept remaining S3/S10 partials. GitHub APK sideload is the M2 channel.
