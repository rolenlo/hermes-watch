# Hermes Watch → Wear OS 移植

> 目標裝置：**Xiaomi Watch 2 Pro**（Wear OS，`M2233`／`M2234`）+ **Android** 手機 companion。  
> 上游 Amazfit／Zepp OS app（`zepp-app/`）**保留唔改**；PC 端 `relay.js` **重用**。

## Phase 0 狀態

已完成：

- `wear-app/` — Wear OS Kotlin + Wear Compose 骨架（Talk／Waiting／Answer／ApproveDeny stub）
- `android-companion/` — 手機 companion 骨架
- `android-shared/` — Data Layer path 同 16 kHz mono WAV／PCM 常數
- `android/` — Gradle 專案根（用 Android Studio 開呢個目錄）

## Phase 1 狀態（而家）

已完成（`android-companion/`）：

- **OkHttp `RelayClient`**：`GET /health`、`POST /watch/ask`（`{text}`）、`GET /watch/result`（`kind=working` 時自動輪詢）
- **設定持久化**：`SharedPreferences` 存 base URL + Bearer token；URL 會 trim／去尾 `/`，並驗證 `http(s)://`
- **UI**：檢查 `/health`（顯示 `ok`／`stt`／`hermes` JSON 摘要）、文字 ask（EditText + 答案／錯誤）
- **錯誤訊息**：timeout、connection refused、unknown host、401 bad token、TLS／其他 HTTP
- **單元測試**：`RelayUrlTest`、`RelayErrorsTest`（純 JVM）

未做（Phase 2+）：

- 真機錄音、Data Layer 傳音檔、`askWithWav`
- Approve／Deny 接 `POST /watch/confirm`
- TTS／語音朗讀、Wear UI 接真資料

本機 **冇 Android SDK** 時唔會跑 Gradle build；有 SDK 時喺 `android/` 執行：

```bash
./gradlew :android-companion:assembleDebug
./gradlew :android-companion:testDebugUnitTest
```

## 架構（MVP）

```
Wear OS app (wear-app)
  -> Android companion (android-companion)   Data Layer: /hermeswatch/ask|result|confirm
  -> https://<pc>.<tailnet>.ts.net:8453     Tailscale HTTPS + Bearer token
  -> relay.js (重用，唔改協議)
       -> STT + Hermes Agent API
```

音訊：**16 kHz mono WAV／PCM**（MVP；Phase 2）。Phase 1 只用純文字 `{text}` 打 `/watch/ask`。

## 用 Android Studio 開啟

1. 安裝 [Android Studio](https://developer.android.com/studio)（含 Android SDK 35、Wear OS emulator 可選）。
2. **File → Open**，選 repo 入面嘅 **`android/`** 目錄（唔好只開 `wear-app/`）。
3. 等 Gradle sync。第一次會下載 AGP／依賴；`local.properties` 會由 IDE 自動寫入 `sdk.dir`（已 gitignore）。
4. Run configurations：
   - `wear-app` → 實體 Watch 2 Pro（ADB）或 Wear emulator
   - `android-companion` → 配對咗嘅 Android 手機

範例 SDK 路徑見 `android/local.properties.example`。

## Phase 1：對住你部 PC relay 跑 companion

1. 喺 PC 跟上游 README 開好 Tailscale serve 同 `node relay.js`（`config.json` 嘅 `token`／`relayUrl`）。
2. 手機安裝 Tailscale，加入**同一個 tailnet**（或者暫時用 `http://100.x.x.x:8453` 直打 Tailscale IP）。
3. Android Studio Run `android-companion` 去手機。
4. 開 app，填：
   - **Base URL**：例如 `https://your-pc.your-tailnet.ts.net:8453`（同 `config.example.json` 嘅 `relayUrl`）
   - **Bearer token**：同 `config.json` 嘅 `token`
5. 撳 **Save settings**（可選；檢查／ask 亦會自動存）。
6. 撳 **檢查 /health**（**唔使** Authorization），預期摘要類似：
   ```
   ok=true  stt=true  hermes=true
   {"ok":true,"stt":true,"hermes":true}
   ```
   `stt`／`hermes` 視乎你部 PC 嗰兩個服務係咪起咗；`ok` 只要 relay 本身回應就會係 `true`。
7. 喺文字框打一句（例如 `what time is it`），撳 **文字 ask**：
   - 送出 `POST /watch/ask` + `Authorization: Bearer <token>` + body `{"text":"..."}`
   - 若 `kind=working`，client 會 poll `GET /watch/result?job=` 直至有 `answer`／`confirm` 或超時
8. 常見錯誤：
   - **Timeout** → relay 未開／Tailscale 未連
   - **Connection refused** → host／port 錯，或者 `relay.js` 冇 listen
   - **401** → token 同 `config.json` 唔一致
   - **Unknown host** → MagicDNS／hostname 錯

完整 Relay API 見上游 root [`README.md`](../README.md) 嘅 **Relay HTTP API** 一節。

## 模組同 package

| 路徑 | Package | 說明 |
| --- | --- | --- |
| `wear-app/` | `com.hermeswatch.wear` | 錶 UI stub |
| `android-companion/` | `com.hermeswatch.phone` | 手機 HTTP +（之後）Data Layer |
| `android-shared/` | `com.hermeswatch.shared` | `DataLayerPaths`、`AudioFormat` |
| `android/` | — | Gradle root |

## 下一步（Phase 2 草案）

1. Wear：`AudioRecord` → 16 kHz mono WAV；`MessageClient`／DataClient 傳去手機。
2. Companion：收 Data Layer → `askWithWav`；結果推返上錶。
3. Approve／Deny → `POST /watch/confirm`。

## 決策鎖定

- 裝置：Xiaomi Watch 2 Pro（Wear OS）+ Android companion  
- 音訊：16 kHz mono WAV／PCM  
- Repo：fork／基於 `hermes-watch`，加 `wear-app/` 同 `android-companion/`  
- 重用 `relay.js`；唔重寫 Zepp app  
