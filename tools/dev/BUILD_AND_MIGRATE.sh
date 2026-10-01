#!/data/data/com.termux/files/usr/bin/bash
set -e
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
echo "=== AURORA 5.0: preparação única ==="
if ! command -v java >/dev/null 2>&1; then echo "Java ausente. Instale OpenJDK 17/21."; exit 1; fi
if [ -d "$HOME/Nova-Aurora/data" ]; then "$ROOT/tools/migrate_2_to_5.sh" "$HOME/Nova-Aurora/data"; fi
if ! command -v gradle >/dev/null 2>&1; then echo "Gradle ausente. Instale Gradle 9.x."; exit 1; fi
if [ -z "${ANDROID_HOME:-}" ] && [ -z "${ANDROID_SDK_ROOT:-}" ]; then echo "ANDROID_HOME/ANDROID_SDK_ROOT não está configurado."; exit 2; fi
gradle --no-daemon --offline assembleDebug || gradle --no-daemon assembleDebug
echo "APK: $ROOT/app/build/outputs/apk/debug/app-debug.apk"
