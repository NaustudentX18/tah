# TAH AC checklist — M3 (0.4.0-m3)

Honest status vs S1–S11. **Green** = met in this tree. **Partial** = usable with noted gaps. **Remaining** = not done.

Legend: ✅ green · 🟨 partial · ⬜ remaining

## S1–S11

| ID | Capability | Status | Notes |
|----|------------|--------|-------|
| S1 | Native Android shell | ✅ | Compose UI; not WebView-chat-only |
| S2 | Session board | ✅ | Working / Needs you / Done; Failed & Budget hit chips on Done |
| S3 | Single-agent run loop | 🟨 | Multi-tool planner loop + budgets; `memory.write` persists; FS/shell/web still receipt-only; planner is prompt-heuristic, not model tool-JSON |
| S4 | Permission modes | ✅ | Ask default / Allow reads / Allow edits; exec stays Ask |
| S5 | Permission cards | ✅ | Approve / Reject / Guide; Reject ends tool; Guide independent |
| S6 | Needs-you notifications | ✅ | Local notif + deep-link; dismiss ≠ approve |
| S7 | Provider wizard | ✅ | BYOK + Ollama LAN; model switcher; on-device keys |
| S8 | Skills + memory | ✅ | Bundled + paste + **SAF file import**; memory CRUD; loop + `memory.write` |
| S9 | Touch Steer + Keys | ✅ | Touch Steer default; Keys persists |
| S10 | Foreground / wake | 🟨 | FG service for active runs + OEM honesty; no mid-tool HTTP resume after death |
| S11 | Distinct TAH brand | ✅ | `app.tah.shell`, Signal Deck, harness glyph |

## Detailed ACs

### AC-Run
- [x] Start single-agent run with prompt + optional skill
- [x] Tokens stream on-device (demo or SSE)
- [x] Compact expandable tool cards
- [x] Iteration / wall-clock budget → Done / Budget hit, not hang
- [x] Multi-tool loop until wrap-up, reject, or budget
- [x] `memory.write` applies for real
- [ ] Model-native function-calling JSON — remaining
- [ ] Real FS/shell execution — out of MVP (receipt-only)

### AC-Skills-Memory
- [x] Load markdown skill pack and apply to a run
- [x] Create/edit memory notes the loop can read
- [x] SAF / file-picker import

### AC-Modes-Lifecycle
- [x] Touch Steer default
- [x] FG/wake documented; no immortal claim
- [ ] Automatic mid-tool loop resume after death — remaining

## Ship gate

| Gate | Status |
|------|--------|
| Session board | ✅ |
| Approvals | ✅ |
| Streaming tool cards | ✅ |
| Multi-tool loop | ✅ (planner) |
| Honest receipts | ✅ |

GitHub APK sideload is the M3 channel. Do not ship Play until product owners accept remaining S3/S10 partials.
