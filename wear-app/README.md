# wear-app (Wear OS)

Compose for Wear OS app for **Xiaomi Watch 2 Pro** (and other round Wear OS watches).

Open the Gradle root at [`../android/`](../android/) in Android Studio — do not open this folder alone.

- **applicationId:** `com.hermeswatch` (same as the phone companion — required by the Wear Data Layer)
- **Kotlin namespace:** `com.hermeswatch.wear`

## Layout

| Path | What |
| --- | --- |
| `state/` | Pure-Kotlin state machine (`WearReducer`), UI state / events / effects, `TextChunks` — unit-tested |
| `WearSession.kt` | Process-wide owner: runs the reducer, executes effects (send, 25 s retry timer, haptics) |
| `data/PhoneLink.kt` | CapabilityClient node discovery + MessageClient send |
| `data/ResultListenerService.kt` | Receives `/hermeswatch/result` from the phone (works with the screen off) |
| `ui/` | Talk · Waiting · Answer · Approve/Deny · Error screens, theme, RemoteInput launcher |

Debug-only screen preview without a phone:

```bash
adb shell am start -n com.hermeswatch/com.hermeswatch.wear.MainActivity --es demo answer
```

See [`../docs/WEAR-PORT.md`](../docs/WEAR-PORT.md) (Phase 2) for install / ADB-over-Wi-Fi / troubleshooting.
