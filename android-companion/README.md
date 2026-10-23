# android-companion

Android phone companion: settings UI + OkHttp client for the Tailscale `relay.js` HTTP API,
and (Phase 2) the Wear Data Layer bridge that answers the watch.

Open the Gradle root at [`../android/`](../android/) in Android Studio.

- **applicationId:** `com.hermeswatch` (same as the watch app — required by the Wear Data Layer;
  build both from the same machine / keystore)
- **Kotlin namespace:** `com.hermeswatch.phone`

## Phase 1

1. Run `android-companion` on a phone (or emulator) that can reach your PC relay
   (same Tailscale tailnet, or LAN / port-forward for debug).
2. Enter **base URL** (e.g. `https://your-pc.your-tailnet.ts.net:8453` or
   `http://100.x.x.x:8453`) and **Bearer token** (= `config.json` `token`).
3. Tap **檢查 /health** — expects `{ok, stt, hermes}` (no auth).
4. Tap **文字 ask** with a short question — `POST /watch/ask` with `{text}`;
   if the relay returns `kind=working`, the client polls `GET /watch/result`.

## Phase 2 (Data Layer)

`datalayer/HermesWearListenerService` (manifest, woken by Play services) →
`datalayer/WatchRequestCoordinator`:

- `/hermeswatch/ask {requestId, text}` → `POST /watch/ask {text, rid}`
- `/hermeswatch/confirm {requestId, id, yes}` → `POST /watch/confirm {id, yes, rid}`
- `kind=working` → polls `GET /watch/result?job=` (2 s, up to 10 min), heartbeat to the watch every 10 s
- reply on `/hermeswatch/result {requestId, kind, text, heard, id?, approve?, errorCode?}`
- `RequestDeduper`: a watch retry with the same requestId never calls the relay twice

The main screen shows the latest watch request (「最近手錶請求」).

Unit tests (URL, error mapping, dedupe, reply mapping):

```bash
cd android && ./gradlew :android-companion:testDebugUnitTest
```
