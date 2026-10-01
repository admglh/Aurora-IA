#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
chmod +x ./gradlew
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

# Resiliente: tenta cache local primeiro; se faltar alguma dependência, o Gradle
# usa a rede disponível para completar o cache. Não usa CMake nem adb.
if ./gradlew --offline :app:assembleDebug; then
  echo "Build offline concluído."
else
  echo "Cache incompleto; tentando build normal..."
  ./gradlew :app:assembleDebug
fi

mkdir -p "$HOME/storage/downloads"
out="$HOME/storage/downloads/Aurora-9.0.apk"
cp app/build/outputs/apk/debug/app-debug.apk "$out"
echo
echo "========================================"
echo " AURORA 9.0 GERADA COM SUCESSO"
echo " $out"
echo "========================================"
