# TAH M5 progress (0.6.0-m5)

## Ship gate

Board + streaming tool cards + permission cards remain wired. Ask-default; Reject ends tool; Guide independent; exec-class stays Ask. No silent full-bypass. Package `app.tah.shell`, Signal Deck, Failed chip-on-Done. No OFH branding.

## M5 landed

| Goal | Status |
|------|--------|
| Workspace files | `fs.read` / `fs.write` / `fs.list` — **app-private** `filesDir/workspace` only |
| Clipboard | `clipboard.read` / `clipboard.write` — real device clipboard after Ask card |
| Memory | `memory.write` persists notes the next run can read |
| Shell | In-process allowlist only: `date`, `echo`, `ls`. **No** `/bin/sh` / ProcessShell |
| Skills | SAF import + paste; enable/disable; Dispatch applies enabled packs; workspace SAF export |
| Lockdown | Removed Jake Malby overreach: unrestricted shell, shared FS paths, `agent.spawn` swarm |
| Version | `0.6.0-m5` (versionCode 6) |

## Real vs receipt-only

| Tool | Effect |
|------|--------|
| `memory.write` | **Real** — MemoryStore |
| `fs.read` / `fs.write` / `fs.list` | **Real** — app-private workspace |
| `clipboard.read` / `clipboard.write` | **Real** — ClipboardManager |
| `web.fetch` | **Real** — Ask-gated HTTP GET (32 KiB) |
| `shell.exec` | **Real allowlist** or honest refuse — never arbitrary binaries |
| Unknown / removed (`agent.spawn`) | Receipt / ignored |

## Still not this product

- Shared / external device filesystem (SAF import/export only by user pick)
- Unrestricted shell
- Multi-agent swarm
- Play Store
- Native model function-calling is best-effort when provider streams tool JSON; heuristic planner remains fallback

See [docs/ROADMAP.md](docs/ROADMAP.md).
