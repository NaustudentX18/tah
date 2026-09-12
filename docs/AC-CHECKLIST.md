# TAH AC checklist — M4 (0.5.0-m4)

Honest status vs S1–S11. **Green** = met in this tree. **Partial** = usable with noted gaps. **Remaining** = not done.

Legend: ✅ green · 🟨 partial · ⬜ remaining

## S1–S11

| ID | Capability | Status | Notes |
|----|------------|--------|-------|
| S1 | Native Android shell | ✅ | Compose UI; not WebView-chat-only |
| S2 | Session board | ✅ | Working / Needs you / Done; Failed, Budget hit, Cancelled chips |
| S3 | Single-agent run loop | 🟨 | Multi-tool loop; workspace + fetch + allowlist shell are real; planner is still heuristic |
| S4 | Permission modes | ✅ | Ask default / Allow reads / Allow edits; exec and network stay Ask |
| S5 | Permission cards | ✅ | Approve / Reject / Guide; Reject ends tool; Guide independent |
| S6 | Needs-you notifications | ✅ | Local notif + deep-link; dismiss ≠ approve |
| S7 | Provider wizard | ✅ | BYOK + Ollama LAN; model switcher; on-device keys |
| S8 | Skills + memory | ✅ | Bundled + paste + SAF; memory CRUD; workspace tab |
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
- [x] `fs.read` / `fs.write` apply to app workspace
- [x] `web.fetch` GET after Ask
- [x] `shell.exec` allowlist or honest refuse
- [x] Stop control
- [ ] Model-native function-calling JSON — remaining
- [ ] Shared-storage FS / `/bin/sh` — out of product

### AC-Skills-Memory
- [x] Load markdown skill pack and apply to a run
- [x] Create/edit memory notes the loop can read
- [x] SAF / file-picker import
- [x] Workspace list / write / delete

### AC-Modes-Lifecycle
- [x] Touch Steer default
- [x] FG/wake documented; no immortal claim
- [ ] Automatic mid-tool loop resume after death — remaining
