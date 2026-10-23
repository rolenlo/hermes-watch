# Hermes Watch — Android project root

Open **this folder** (`android/`) in Android Studio.

Modules (sibling directories, wired via `settings.gradle.kts`):

| Module | Path | Role |
| --- | --- | --- |
| `:wear-app` | `../wear-app` | Wear OS app (Xiaomi Watch 2 Pro / Wear OS) |
| `:android-companion` | `../android-companion` | Phone companion (HTTP → Tailscale relay) |
| `:android-shared` | `../android-shared` | Shared Data Layer path constants |

See [`../docs/WEAR-PORT.md`](../docs/WEAR-PORT.md) for Phase 0 status and setup.
