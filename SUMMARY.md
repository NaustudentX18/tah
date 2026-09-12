# TAH M4 progress (0.5.0-m4)

## Ship gate

Board + streaming tool cards + permission cards remain wired. Tools that the product advertises now have a real, labeled effect inside the contract.

## M4 landed

| Goal | Status |
|------|--------|
| Workspace files | `fs.read` / `fs.write` persist under app-private `filesDir/workspace` |
| Live fetch | `web.fetch` does an Ask-gated HTTP GET of the card URL (32 KiB cap) |
| Shell | In-process allowlist: `date`, `echo`, `ls`. Anything else refused. No `/bin/sh` |
| Stop | Session detail stop control → Cancelled chip |
| Tests | JVM unit tests on planner, policy, URL + names; CI runs them |
| Version | `0.5.0-m4` (versionCode 5) |

## Still not this product

- Shared / external device filesystem
- Unrestricted shell
- Native model function-calling JSON (planner is still heuristic)
- Mid-tool HTTP resume after process death
- Multi-agent swarm
- Play Store

See [docs/ROADMAP.md](docs/ROADMAP.md).
