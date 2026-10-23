# Hermes Watch → Wear OS 移植（Phase 0）

> 目標裝置：**Xiaomi Watch 2 Pro**（Wear OS，`M2233`／`M2234`）+ **Android** 手機 companion。  
> 上游 Amazfit／Zepp OS app（`zepp-app/`）**保留唔改**；PC 端 `relay.js` **重用**。

## Phase 0 狀態（而家）

已完成：

- `wear-app/` — Wear OS Kotlin + Wear Compose 骨架（Talk／Waiting／Answer／ApproveDeny stub）
- `android-companion/` — 手機 companion 骨架（`RelayClient` 文件化 `GET /health`、`POST /watch/ask`）
- `android-shared/` — Data Layer path 同 16 kHz mono WAV／PCM 常數
- `android/` — Gradle 專案根（用 Android Studio 開呢個目錄）

未做（Phase 1+）：

- 真機錄音、Data Layer 傳音檔、OkHttp 打 relay
- Approve／Deny 接 `POST /watch/confirm`
- TTS／語音朗讀、分片重試

本機 **冇 Android SDK** 時唔會跑 Gradle build；骨架仍然可以喺裝咗 SDK 嘅機用 Android Studio 開啟。

## 架構（MVP）

```
Wear OS app (wear-app)
  -> Android companion (android-companion)   Data Layer: /hermeswatch/ask|result|confirm
  -> https://<pc>.<tailnet>.ts.net:8453     Tailscale HTTPS + Bearer token
  -> relay.js (重用，唔改協議)
       -> STT + Hermes Agent API
```

音訊：**16 kHz mono WAV／PCM**（MVP）。上游 Zepp 路徑用 Opus；Wear port 刻意用 WAV，減少錶上編碼複雜度。Companion 將 WAV base64 放入 `POST /watch/ask` 嘅 `audio_b64`（同現有 Relay API 兼容；STT worker 需確認接受 WAV——Phase 1 驗證）。

## 用 Android Studio 開啟

1. 安裝 [Android Studio](https://developer.android.com/studio)（含 Android SDK 35、Wear OS emulator 可選）。
2. **File → Open**，選 repo 入面嘅 **`android/`** 目錄（唔好只開 `wear-app/`）。
3. 等 Gradle sync。第一次會下載 AGP／依賴；`local.properties` 會由 IDE 自動寫入 `sdk.dir`（已 gitignore）。
4. Run configurations：
   - `wear-app` → 實體 Watch 2 Pro（ADB）或 Wear emulator
   - `android-companion` → 配對咗嘅 Android 手機

範例 SDK 路徑見 `android/local.properties.example`。

## 指住 Tailscale relay 同打 `/health`

1. 喺 PC 跟上游 README 開好 Tailscale serve 同 `relay.js`（`config.json` 嘅 `token`／`relayUrl`）。
2. 手機安裝 Tailscale，加入同一個 tailnet。
3. 開 companion app，填：
   - **Base URL**：例如 `https://your-pc.your-tailnet.ts.net:8453`（同 `config.example.json` 嘅 `relayUrl`）
   - **Bearer token**：同 `config.json` 嘅 `token`
4. Phase 0 撳 **Check /health** 只會顯示打算打嘅 URL（OkHttp 未接）；Phase 1 會真正 `GET /health`（**唔使** Authorization），預期 JSON：`{ "ok": true, "stt": ..., "hermes": ... }`。
5. 之後 `POST /watch/ask` 等路徑需要：`Authorization: Bearer <token>`。

完整 Relay API 見上游 root [`README.md`](../README.md) 嘅 **Relay HTTP API** 一節（`/watch/ask`、`/watch/confirm`、`/watch/result`、`/health` 等）。

## 模組同 package

| 路徑 | Package | 說明 |
| --- | --- | --- |
| `wear-app/` | `com.hermeswatch.wear` | 錶 UI stub |
| `android-companion/` | `com.hermeswatch.phone` | 手機 HTTP +（之後）Data Layer |
| `android-shared/` | `com.hermeswatch.shared` | `DataLayerPaths`、`AudioFormat` |
| `android/` | — | Gradle root |

## 下一步（Phase 1 草案）

1. Companion：OkHttp 實現 `health()`／`askWithWav()`；設定持久化 token。
2. Wear：`AudioRecord` → WAV；`MessageClient`／DataClient 傳去手機。
3. 真機驗證：短問一句 → 文字答上錶；再接 Approve／Deny。

## 決策鎖定

- 裝置：Xiaomi Watch 2 Pro（Wear OS）+ Android companion  
- 音訊：16 kHz mono WAV／PCM  
- Repo：fork／基於 `hermes-watch`，加 `wear-app/` 同 `android-companion/`  
- 重用 `relay.js`；唔重寫 Zepp app  
