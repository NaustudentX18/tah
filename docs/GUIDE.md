# TAH guide

## Install (no local SDK)

1. Open [Actions](https://github.com/NaustudentX18/tah/actions) on this repo.
2. Open the latest green **Android debug APK** run.
3. Download the **tah-debug-apk** artifact.
4. On the phone: allow install from that source, or `adb install -r app-debug.apk`.

Tagged builds (`v*`) also attach `tah-debug.apk` to a GitHub Release.

## First run

1. Onboarding → **Wire a provider** or skip.
2. Providers: leave **Demo** to try the board offline, or paste a BYOK base URL + key, or an Ollama LAN URL.
3. Probe before you trust the badge.
4. Dispatch a short prompt. Watch the Board, not a chat list.

## Permission rules (locked)

- Default mode is **Ask**.
- **Reject** ends that tool. Guide is optional.
- **Guide** injects text and the loop may continue if budget remains.
- **Allow edits** can auto-allow write-class tools. **Exec stays Ask.**
- Dismissing a Needs-you notification does **not** approve the tool.

## What actually runs

| Tool | After Approve |
|------|----------------|
| `memory.write` | Creates a real note under Skills & Memory |
| `fs.read` / `fs.write` | App-private workspace file (Skills → Workspace) |
| `web.fetch` | HTTP GET of the URL on the card (32 KiB cap) |
| `shell.exec` | `date` / `echo` / `ls` in-process; otherwise refused |

## Skills

- Bundled packs ship in the APK.
- **Import from file** uses the system document picker (markdown / plain text).
- Paste import still works.
- Enabled packs are injected into the system prompt at run start.

## Battery / OEM

The foreground service raises priority **while a run is active**. It does not survive aggressive OEM killers and does not claim to. Settings → About lists the usual steps (unrestricted battery, lock the app in recents) without promising immortality.

## Local build

Android Studio Giraffe+ / Koala, JDK 17, Gradle 8.9 (CI installs Gradle; this tree may not vendor `gradle/wrapper/gradle-wrapper.jar`).

```bash
gradle :app:assembleDebug
# APK: app/build/outputs/apk/debug/
```
