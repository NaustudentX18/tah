# 📖 TAH User & Developer Guide

Welcome to the **TAH (The Agent Harness)** guide! This walkthrough covers how to set up your phone, steer agent swarms on the Signal Deck, manage permissions, and develop new tools.

---

## 🚀 Installation & First Run

### 📥 1. Getting the App
- **From GitHub Releases / Actions:** Download the latest `app-debug.apk` and sideload it to your phone using `adb install -r app-debug.apk` or tap the APK in your phone's file manager.
- **From Source:** Run `.\scripts\build.ps1 -Install` with your phone connected via USB.

### ⚙️ 2. Provider Setup
TAH connects directly from your phone to your AI provider of choice without any middleman proxy:
1. Open **Settings → Providers**.
2. **Demo (Offline):** Instant on-device simulations. Perfect for trying the UI without network or API keys.
3. **Ollama on LAN:** Run open-source models on your local machine (`http://192.168.x.x:11434/v1`). Great for private on-premise swarms!
4. **BYOK (OpenAI / Groq / OpenRouter / DeepSeek):** Enter your standard OpenAI-compatible Base URL and API key.
5. Tap **Probe** to verify connectivity and load available model IDs.

---

## 📋 The Signal Deck: Board-First Workflow

Unlike traditional AI chat apps that produce endless text logs, TAH organizes your agent's work onto a tactile, glanceable board:

- 🔵 **Working:** Active agents currently reasoning, streaming thoughts, or executing approved tool steps.
- 🟡 **Needs You:** Execution pauses here whenever a tool requires human oversight. A notification alerts you immediately.
- 🟢 **Done:** Finished missions marked with status chips (`Done`, `Failed`, `Budget hit`, or `Cancelled`).

### 🎴 How to Handle "Needs You" Cards
When a card appears in **Needs You**:
- ✅ **Approve:** Tap to allow the exact action shown on the card to execute on your device.
- ❌ **Reject:** Refuses this tool call immediately. The agent will close the turn or pivot.
- ✍️ **Guide:** Type custom feedback or corrections. For example: *"Don't delete that file, rename it to backup.txt instead."* The agent injects your guidance directly into its thought loop!

---

## 🛠️ Tool Arsenal

Every action the agent takes is backed by real execution:

| Tool | Action & Scope | Permission Gate |
|---|---|---|
| `shell.exec` | Dual-mode: fast built-ins (`date`, `echo`, `ls`) or full `/system/bin/sh` process commands with stdout/stderr capture and timeout limits. | **Always Ask** |
| `fs.read` | Reads files from either the app workspace or explicit device paths (e.g. `/sdcard/Download/data.csv`). | Ask / Allow Reads |
| `fs.write` | Writes text to workspace or device filesystem with byte counts and atomic save. | Ask / Allow Edits |
| `fs.list` | Inspects directory contents and file sizes. | Ask / Allow Reads |
| `web.fetch` | Performs an HTTP GET request of the URL shown on the card (capped at 32 KiB). | **Always Ask** |
| `memory.write` | Saves persistent knowledge notes to Skills & Memory that persist across reboots. | Auto or Ask |
| `agent.spawn` | Spawns a specialized subagent on the board (Planner, Coder, Researcher, Verifier). | Ask / Allow Edits |

---

## 🤖 Multi-Agent Swarm Modes

TAH supports specialized agent roles that can collaborate:
- **👑 Orchestrator:** The primary session that decomposes your goal and steers the swarm.
- **📐 Planner:** Formulates step-by-step roadmaps and task lists.
- **💻 Coder:** Executes shell scripts, compiles code, and manipulates files.
- **🔍 Researcher:** Browses web endpoints, parses documentation, and gathers facts.
- **✅ Verifier:** Validates results, checks outputs, and runs test scripts.

---

## 🔋 Battery & Background Persistence

To keep agents running while your phone is locked:
1. **Foreground Service:** Displays an active notification with a direct "Stop" action.
2. **CPU WakeLock:** Acquires a partial `PowerManager.WakeLock` during active loops to prevent Android from putting the CPU to sleep mid-run.
3. **Battery Optimization Exemption:** Go to your phone's App Info → Battery → set to **Unrestricted** to prevent aggressive OEM killers from stopping the service.

---

## 🧪 Developer Commands

From your terminal in the repository root:
```powershell
# Run the complete unit test suite
.\scripts\test.ps1

# Build the debug APK
.\scripts\build.ps1

# Check Android CLI status
android info
```
