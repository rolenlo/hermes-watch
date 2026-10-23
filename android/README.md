# Hermes Watch — Android project root

Open **this folder** (`android/`) in Android Studio.

Modules (sibling directories, wired via `settings.gradle.kts`):

| Module | Path | Role |
| --- | --- | --- |
| `:wear-app` | `../wear-app` | Wear OS app (Xiaomi Watch 2 Pro / Wear OS) |
| `:android-companion` | `../android-companion` | Phone companion (HTTP → Tailscale relay) |
| `:android-shared` | `../android-shared` | Data Layer paths, capabilities, wire JSON (`WireCodec`) |

See [`../docs/WEAR-PORT.md`](../docs/WEAR-PORT.md) for Phase 0–2 status, install (ADB over Wi‑Fi to the watch) and troubleshooting.

Both apps use **applicationId `com.hermeswatch`** (Wear Data Layer requirement) — build them
from the same machine so they share the debug signing key.

```bash
./gradlew test                 # all JVM unit tests
./gradlew assembleDebug        # wear-app + android-companion APKs
```
