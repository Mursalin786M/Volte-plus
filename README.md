# VoltPulse — Screen Time & Battery Drain Monitor (Android)

A native Android app (Kotlin + Jetpack Compose + Material 3) that monitors:
- **Screen Time**: App-by-app foreground usage, launch counts, daily limits, and 7-day history (`UsageStatsManager`).
- **Battery Health & Live Telemetry**: Percentage, temperature (°C), voltage (V), live current (mA), power (W), health, technology, and charging source (`BatteryManager`).
- **Battery Drain Breakdown**: App-by-app estimated drain (% and mAh), Screen-On vs Screen-Off drain rates, and background snapshot history (`WorkManager`).
- **Smart Alerts**: Full-charge alarm (80%/90%/100%), low-battery alert, and overheat warning.

## How to Get the Installable `.apk`

### Option A: Build with Android Studio (PC)
1. Unzip this folder.
2. Open **Android Studio** -> **Open** -> select the `VoltPulse` folder.
3. Wait for Gradle sync to finish.
4. Click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Copy `app/build/outputs/apk/debug/app-debug.apk` to your phone and install it.

### Option B: Build Free in the Cloud (GitHub Actions — No PC Required)
1. Create a free repository on GitHub and upload all files from this folder.
2. Open the **Actions** tab in your repository and run **Build Android Debug APK**.
3. After ~3 minutes, download **VoltPulse-Debug-APK** from the Artifacts section and install `app-debug.apk` on your phone.
