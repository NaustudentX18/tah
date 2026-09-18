# TAH roadmap — honest path to a finished product

This is the plan. **Finished** means every advertised tool has a real, labeled effect inside the product contract. It does **not** mean Termux, a swarm, or a Play Store listing.

## Already shipped

| Tag | What landed |
|-----|-------------|
| v0.3.0-m2 | Board, Dispatch, Ask cards, FG service, BYOK/Ollama, demo stream |
| v0.4.0-m3 | Multi-tool planner loop, real `memory.write`, SAF skill import, honest README |
| v0.5.0-m4 | Workspace fs, `web.fetch`, allowlist shell, stop, JVM tests |
| v0.6.0-m5 | Clipboard tools, workspace SAF export, lockdown of unrestricted shell/FS/swarm claims |

## M5 — this build (`0.6.0-m5`)

1. **Clipboard** — `clipboard.read` / `clipboard.write` with Ask-default permission cards.
2. **Workspace stays sandboxed** — no arbitrary `/path` device FS from tool cards.
3. **Shell stays allowlist** — `date` / `echo` / `ls` only; ProcessShell removed from the product path.
4. **Skills polish** — SAF import, enable/disable, Dispatch apply, workspace Export via CreateDocument.
5. **Honest docs** — no multi-agent swarm marketing; no OFH branding.

## M6 — polish / optional

- Replay a Done session (new run, same prompt + skill)
- Stronger empty / error / offline copy on every screen
- Signed release APK if you supply a keystore
- Constrained SAF-backed *external* file read (user picks the file) — only if accepted

## Never claimed

- Silent device filesystem
- Silent / unrestricted shell
- Immortal background after OEM kill
- Multi-agent orchestration inside the phone app
