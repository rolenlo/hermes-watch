# Hermes Watch → Wear OS 移植

> 目標裝置：**Xiaomi Watch 2 Pro**（Wear OS，`M2233`／`M2234`）+ **Android** 手機 companion。  
> 上游 Amazfit／Zepp OS app（`zepp-app/`）**保留唔改**；PC 端 `relay.js` **重用**。

## Phase 0 狀態

已完成：

- `wear-app/` — Wear OS Kotlin + Wear Compose 骨架（Talk／Waiting／Answer／ApproveDeny stub）
- `android-companion/` — 手機 companion 骨架
- `android-shared/` — Data Layer path 同 16 kHz mono WAV／PCM 常數
- `android/` — Gradle 專案根（用 Android Studio 開呢個目錄）

## Phase 1 狀態

已完成（`android-companion/`）：

- **OkHttp `RelayClient`**：`GET /health`、`POST /watch/ask`（`{text}`）、`GET /watch/result`（`kind=working` 時自動輪詢）
- **設定持久化**：`SharedPreferences` 存 base URL + Bearer token；URL 會 trim／去尾 `/`，並驗證 `http(s)://`
- **UI**：檢查 `/health`（顯示 `ok`／`stt`／`hermes` JSON 摘要）、文字 ask（EditText + 答案／錯誤）
- **錯誤訊息**：timeout、connection refused、unknown host、401 bad token、TLS／其他 HTTP
- **單元測試**：`RelayUrlTest`、`RelayErrorsTest`（純 JVM）

當時未做 → Phase 2 已做：Wear UI 接真資料、Approve／Deny 接 `POST /watch/confirm`。仍未做（Phase 3）：真機錄音、傳音檔、`askWithWav`、TTS。

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

音訊：**16 kHz mono WAV／PCM**（MVP；Phase 3）。Phase 1–2 只用純文字 `{text}` 打 `/watch/ask`。

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
| `wear-app/` | `com.hermeswatch.wear` | 錶 UI（Compose for Wear OS）+ 狀態機 |
| `android-companion/` | `com.hermeswatch.phone` | 手機 HTTP + Data Layer bridge |
| `android-shared/` | `com.hermeswatch.shared` | `DataLayerPaths`、`Capabilities`、wire JSON（`WireCodec`） |

> **Package（Kotlin namespace）** 同 **applicationId** 唔同：兩個 app 嘅 `applicationId` 都係 **`com.hermeswatch`**（Data Layer 規定），`namespace` 仍然分開做 `…wear`／`…phone`，所以 R class 同 source package 唔撞。
| `android/` | — | Gradle root |

## Phase 2 狀態（而家）：Wear UI + Data Layer 文字來回

已完成：

- **錶 UI（Compose for Wear OS Material 1.4）**，全部由一個狀態機驅動（`wear-app/…/state/WearReducer.kt`，純 Kotlin，有單元測試）：

  | 畫面 | 內容 | 操作 |
  | --- | --- | --- |
  | **Talk** | 96 dp 琥珀色大掣（咪高峰 icon +「講嘢」）、細「新對話」chip、TimeText | 撳大掣 → 系統輸入（語音聽寫／鍵盤／預設問題）；新對話 → relay 開新 session |
  | **Waiting** | 沿錶圈轉嘅進度環、「諗緊…」（12 秒後變「仲諗緊…」）、灰色引述你問咗乜、一粒取消掣 | 取消／向右掃 = 返 Talk；等候期間保持亮屏 |
  | **Answer** | 灰色「你問嘅嘢」、答案分段（`TextChunks`）放入 `ScalingLazyColumn` | **錶冠／旋鈕捲動**（Wear Compose 1.4 內建）、PositionIndicator、TimeText 會捲走；「再問」／「完成」 |
  | **Approve／Deny** | 琥珀色標題「要批准？」（或 Yes/No 卡「確認？」）、完整 prompt 可捲 | 「批准」（主色）／「拒絕」（暗紅）；Yes/No 卡顯示「好」／「唔好」 |
  | **Error** | 警告 icon、2–5 字標題（例如「連唔到 PC」）、一行點修 | 「再試」（新 requestId 重送）／「返回」 |

  - 所有非 Talk 畫面都包喺 `SwipeToDismissBox`：**向右掃返回**（底下見到 Talk），硬件返回掣一樣。
  - 畫面之間 fade + 輕微 scale 過場；答案／確認卡／錯誤會有唔同手腕震動。
  - 圓形屏安全邊距：固定佈局用 14.6% inset（Wear 設計指引），長內容用 `ScalingLazyColumn`。
  - 「Reply」（對住確認卡講嘢）延後到 Phase 3（要真錄音）。
  - **暫代咪高峰**：撳「講嘢」會開 Wear 系統 **RemoteInput**，入面已經有 **語音聽寫**（Google 語音輸入）、鍵盤／手寫同 3 個預設問題，所以 Phase 2 已經可以「講」。Phase 3 先換成自己錄 16 kHz WAV。

- **Data Layer 文字來回**（`MessageClient`）：

  | Path | 方向 | JSON |
  | --- | --- | --- |
  | `/hermeswatch/ask` | 錶 → 手機 | `{requestId, text}` |
  | `/hermeswatch/confirm` | 錶 → 手機 | `{requestId, id, yes}` |
  | `/hermeswatch/result` | 手機 → 錶 | `{requestId, kind, text?, heard?, id?, approve?, errorCode?}` |

  - `kind`：`answer` | `confirm` | `working`（心跳）| `error`（`errorCode`：`not_configured`、`unauthorized`、`unreachable`、`relay`、`too_slow`、`bad_request`）。
  - JSON 用 `kotlinx.serialization`（`android-shared/…/Protocol.kt` 嘅 `WireCodec`），未知欄位會忽略，壞 payload 唔會 crash。
  - **新對話** = 送 `{"text":"start new session"}`（同上游 Zepp phone side 一樣，relay 嘅 `isResetCommand` 會接）。

- **重試 + 去重**（跟上游 `POLL_STUCK_MS = 25000` 設計）：
  1. 錶送出後 **25 秒冇任何回覆** → 用**同一個 requestId** 再送，最多 4 次；4 次都靜晒 → 「手機冇回應」。
  2. 手機一收到就即刻回 `working`，之後每 **10 秒**心跳一次；錶每收到心跳就重置計時同次數 → Hermes 慢都唔會誤判斷線。
  3. 手機 `RequestDeduper`：同一 requestId 進行中 → 只回心跳唔重打 relay；已完成 → 直接重播結果（上次可能喺藍牙途中甩咗）。
  4. 手機打 relay 時帶 `rid = requestId`，`relay.js` 本身都會去重 → 就算手機 process 俾系統殺咗再起，Hermes／動作都只會行一次。
  5. relay 回 `kind=working` → 手機自己 poll `GET /watch/result?job=`（每 2 秒，最多 10 分鐘）。
  6. 用戶取消／掃走之後先到嘅回覆，因為 requestId 唔對，會被忽略。

- **Node discovery（`wear.xml` capability）**：手機宣告 `hermeswatch_phone_relay`，錶宣告 `hermeswatch_watch_app`。錶用 `CapabilityClient`（`FILTER_REACHABLE`，優先 `isNearby`）搵手機；搵唔到再試 `connectedNodes`（新裝 app 時 capability 可能遲啲先同步）。手機直接回覆 message 嘅 `sourceNodeId`。

- **手機 companion**：`HermesWearListenerService`（manifest 宣告，Play services 會喚醒）→ process 級 `WatchRequestCoordinator` → `RelayClient.ask(text, rid)`／`confirm(id, yes, rid)`。主畫面多咗一行「最近手錶請求」方便除錯。

### ⚠️ 同一個 applicationId + 同一條簽名 key

Wear Data Layer **只會喺 package 名相同、簽名相同嘅 app 之間傳 message**。所以：

- `wear-app` 同 `android-companion` 嘅 `applicationId` 都係 **`com.hermeswatch`**（Phase 1 嘅舊 id `com.hermeswatch.phone`／`com.hermeswatch.wear` 已經改咗 → 手機上舊嗰個 Phase 1 app 可以剷走，URL／token 要喺新 app 再填一次）。
- **兩個 app 要喺同一部電腦、同一個 Android Studio 用同一個 debug keystore**（`~/.android/debug.keystore`）build。喺兩部電腦分別 build = 簽名唔同 = 收唔到 message。
- 將來出 release：兩個都用同一個 release keystore；如果上 Play，兩個 `versionCode` 要唔同（而家 phone `3`、wear `2`）。

### 安裝（Android Studio + ADB over Wi‑Fi 去 Watch 2 Pro）

**前提：錶已經同部 Android 手機配對**（用小米／Wear OS 嘅正常配對流程，即係系統層面經 Google Play services 連住）。Data Layer 行嘅係呢條系統連線；冇配對 = 搵唔到 node。

1. **手機**：開 USB debugging，USB 插電腦，Android Studio 揀 `android-companion` → Run。開 app 填 relay URL + token → 撳「檢查 /health」確認 `ok=true`。
2. **錶開開發者模式**：錶上 **設定 → 系統 → 關於**（或「關於手錶」）→ 連撳 **版本號／Build number** 7 下。
3. **開 Wi‑Fi 除錯**：錶同電腦接**同一個 Wi‑Fi** → **設定 → 開發者選項** → 開 **ADB 偵錯**，再開 **無線偵錯（Wireless debugging）**。
4. **配對 + 連線**（Wear OS 3+ 用配對碼）：
   - 喺「無線偵錯」入面撳 **使用配對碼配對裝置**，錶會顯示 `IP:配對port` 同 6 位碼：
     ```bash
     adb pair 192.168.1.50:37123      # 輸入錶上 6 位碼
     ```
   - 返去「無線偵錯」主畫面，睇 **IP 地址同 port**（同配對 port 唔同）：
     ```bash
     adb connect 192.168.1.50:41234
     adb devices                      # 應該見到錶
     ```
   - 舊式「Debug over Wi‑Fi」（固定 5555）都得：`adb connect <錶IP>:5555`。
5. Android Studio 頂部裝置揀個錶 → 揀 `wear-app` → Run。或者命令行：
   ```bash
   cd android
   ./gradlew :wear-app:installDebug :android-companion:installDebug
   ```
   （兩部裝置都連住時，用 `adb -s <serial> install …/wear-app-debug.apk` 指定。）
6. 錶上開 **Hermes** → 撳「講嘢」→ 揀預設問題或者講句嘢 → 等答案。

### 單元測試

```bash
cd android
./gradlew test     # WireCodecTest、WearReducerTest、TextChunksTest、RequestDeduperTest、ReplyMapperTest …
```

### Debug：唔使手機都睇到每個畫面

Debug build 先有效（release 會忽略）：

```bash
adb -s <錶> shell am start -n com.hermeswatch/com.hermeswatch.wear.MainActivity --es demo answer
# demo = talk | waiting | answer | approve | yesno | error
```

### 疑難排解

| 現象 | 原因／處理 |
| --- | --- |
| 錶即刻顯示「連唔到手機」 | 搵唔到 node：錶未同手機配對、藍牙關咗、手機太遠。睇錶嘅連線狀態。 |
| 等 ~100 秒後「手機冇回應」 | message 送到但冇人答：① 手機未裝 companion；② **applicationId／簽名唔一致**（兩個 app 唔係同一部電腦 build）；③ 手機電池優化殺咗 app → companion 設成「不受限制」。 |
| 「手機未設定」 | Phase 2 改咗 applicationId，新 companion 要重新填 URL + token。 |
| 「連唔到 PC」 | 手機到 relay 嗰段：relay.js 未開、Tailscale 未連、URL 錯。先喺手機 app 撳「檢查 /health」。 |
| 「Token 唔啱」 | token 同 `config.json` 唔同。 |
| 「Hermes 出錯」 | relay 回 `ok:false`（例如 STT 未開）；細字會顯示 PC 回嘅原文。 |
| 「開唔到輸入」 | 錶冇 RemoteInput（語音／鍵盤）輸入法；確認 Gboard／語音輸入有裝同開。 |
| 想睇 log | `adb logcat -s HermesBridge`（手機）／`adb logcat -s HermesWear HermesPhoneLink`（錶）。手機 app「最近手錶請求」都會顯示最後一單。 |

### 要喺真機確認

- Watch 2 Pro 嘅 RemoteInput 有冇 **語音聽寫**（同支唔支援廣東話）。
- 錶冠捲答案順唔順；96 dp TALK 掣同 chips 喺 1.43" 屏嘅比例。
- 熄屏／抬手時答案到嘅震動同顯示。
- 手機 app 喺背景時（電池優化）Data Layer 喚醒係咪穩定。

## 下一步（Phase 3 草案）

1. Wear：`AudioRecord` → 16 kHz mono WAV；`ChannelClient`／分片 message 傳去手機（上游 24 KB 分片設計）。
2. Companion：`askWithWav`；確認卡「Reply」語音回覆。
3. 可選：TTS／`/watch/speech` 朗讀、`/watch/last` 恢復中斷嘅問題、長任務用 foreground service。

## 決策鎖定

- 裝置：Xiaomi Watch 2 Pro（Wear OS）+ Android companion  
- 音訊：16 kHz mono WAV／PCM  
- Repo：fork／基於 `hermes-watch`，加 `wear-app/` 同 `android-companion/`  
- 重用 `relay.js`；唔重寫 Zepp app  
