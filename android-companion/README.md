# android-companion

Android phone companion: settings UI + OkHttp client for the Tailscale `relay.js` HTTP API.
Wear Data Layer bridging lands in a later phase.

Open the Gradle root at [`../android/`](../android/) in Android Studio.

Package: `com.hermeswatch.phone`

## Phase 1

1. Run `android-companion` on a phone (or emulator) that can reach your PC relay
   (same Tailscale tailnet, or LAN / port-forward for debug).
2. Enter **base URL** (e.g. `https://your-pc.your-tailnet.ts.net:8453` or
   `http://100.x.x.x:8453`) and **Bearer token** (= `config.json` `token`).
3. Tap **檢查 /health** — expects `{ok, stt, hermes}` (no auth).
4. Tap **文字 ask** with a short question — `POST /watch/ask` with `{text}`;
   if the relay returns `kind=working`, the client polls `GET /watch/result`.

Unit tests (URL + error mapping):

```bash
cd android && ./gradlew :android-companion:testDebugUnitTest
```
