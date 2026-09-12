# TAH roadmap — honest path to a finished product

This is the plan. **Finished** means every advertised tool has a real, labeled effect inside the product contract. It does **not** mean Termux, a swarm, or a Play Store listing.

## Already shipped

| Tag | What landed |
|-----|-------------|
| v0.3.0-m2 | Board, Dispatch, Ask cards, FG service, BYOK/Ollama, demo stream |
| v0.4.0-m3 | Multi-tool planner loop, real `memory.write`, SAF skill import, honest README |

## M4 — this build (`0.5.0-m4`)

Close the “receipt-only” hole **where it is safe**:

1. App-private **workspace** (`filesDir/workspace`) — `fs.read` / `fs.write` persist real files the user can see under Skills → Workspace.
2. **`web.fetch`** — OkHttp GET of the URL on the card after Ask. Size-capped. No scrape farm.
3. **`shell.exec`** — in-process allowlist (`date`, `echo`, `ls` of workspace). Anything else is refused. Still never `/bin/sh`.
4. Stop a live run from Session detail. Cancelled chip on Done.
5. JVM unit tests for planner, policy, URL + workspace names. CI runs them before assemble.

## M5 — polish product (next)

- Model-native tool JSON when the live provider supports it; heuristic planner stays fallback
- Replay a Done session (new run, same prompt + skill)
- Workspace export via SAF create-document
- Stronger empty / error / offline copy on every screen
- Signed release APK if you supply a keystore (debug stays the sideload default)

## M6 — only if you accept the risk

- Constrained SAF-backed *external* file read (user picks the file; no all-storage permission)
- Optional LAN-only fetch rules
- Not in scope unless you say so: multi-agent swarm, Play Console, unrestricted shell

## Never claimed

- Silent device filesystem
- Silent shell
- Immortal background after OEM kill
- Multi-agent orchestration inside the phone app
