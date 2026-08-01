# Android Guardian

Jetpack Compose app that force-recompiles installed packages at the ART `speed`
filter (full AOT) via the [Shizuku](https://github.com/RikkaApps/Shizuku) API,
avoiding the need for root or a permanently tethered ADB shell.

## How it works

| Action | Command executed |
|---|---|
| Optimize | `cmd package compile -m speed -f <package>` |
| Reset | `cmd package compile --reset <package>` |
| Status check | `dumpsys package <package> \| grep -E "dexopt\|status="` |

All three run through `Shizuku.newProcess(...)` on `Dispatchers.IO`, streaming
stdout/stderr back into a Compose `LazyColumn` in real time.

**Requirement:** [Shizuku](https://shizuku.rikka.app/) must be installed and its
service started (via ADB pairing or root) on the target device. The app's
built-in banner walks the user through `ServiceDead → Pending → Granted/Denied`.

## Project layout

```
Android-Guardian/
├── build.gradle.kts                 # root: plugin versions only
├── settings.gradle.kts              # repositories + module includes
├── gradle.properties
├── gradlew / gradlew.bat            # wrapper launcher scripts
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties    # pins Gradle 8.9
└── app/
    ├── build.gradle.kts             # AGP 8.6 / Kotlin 2.0.20 / compileSdk 35
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/borgorninja/androidguardian/
        │   ├── MainActivity.kt
        │   ├── AndroidGuardianApp.kt
        │   ├── shizuku/
        │   │   └── ShizukuManager.kt      # lifecycle + process execution
        │   ├── data/
        │   │   ├── AppInfo.kt
        │   │   ├── CompileStatus.kt
        │   │   └── AppRepository.kt        # PackageManager + dexopt parsing
        │   └── ui/
        │       ├── theme/{Color,Type,Theme}.kt
        │       ├── viewmodel/{MainViewModel,UiState}.kt
        │       ├── components/{AppListItem,ConsoleLog,ShizukuStatusBanner}.kt
        │       ├── screens/{AppListScreen,DashboardScreen}.kt
        │       └── navigation/NavGraph.kt
        └── res/
            ├── values/strings.xml
            ├── drawable/ic_launcher_{background,foreground}.xml
            └── mipmap-anydpi-v26/ic_launcher{,_round}.xml
```

## Building from the command line (no Android Studio)

### 1. Install the JDK and Android command-line tools

```bash
# JDK 17 (required by AGP 8.6 / Kotlin 2.0)
sudo apt-get update && sudo apt-get install -y openjdk-17-jdk unzip

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

# Android command-line tools
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
curl -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip cmdline-tools.zip
mv cmdline-tools latest

export ANDROID_HOME=~/android-sdk
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

### 2. Accept licenses and install required SDK packages

```bash
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

### 3. Clone and build

```bash
git clone https://github.com/BorgorNinja/Android-Guardian.git
cd Android-Guardian
./gradlew assembleDebug
```

Output APK:

```
app/build/outputs/apk/debug/app-debug.apk
```

Release build (unsigned unless you configure a signing block in
`app/build.gradle.kts`):

```bash
./gradlew assembleRelease
```

### 4. Install directly (device/emulator connected via adb)

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- `Shizuku.newProcess` is invoked via reflection in `ShizukuManager.kt` — it is
  present in the `dev.rikka.shizuku:api` runtime but not part of the
  officially stable public surface, so reflection avoids a hard compile-time
  dependency on an internal signature that has shifted across Shizuku
  versions.
- `QUERY_ALL_PACKAGES` is declared to allow enumerating all installed apps
  (not just ones this app has interacted with) on API 30+; Play Store
  distribution of an app with this permission requires a declared use-case
  in the Play Console — irrelevant for sideloaded/Shizuku-class tooling but
  worth knowing if you ever publish this.
