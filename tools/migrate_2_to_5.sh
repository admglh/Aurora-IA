#!/data/data/com.termux/files/usr/bin/bash
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${1:-$HOME/Nova-Aurora/data}"
DST="$ROOT/app/src/debug/assets/legacy/data"
mkdir -p "$DST"
for f in memoria.json conversas.jsonl; do [ -f "$SRC/$f" ] && cp "$SRC/$f" "$DST/$f" || true; done
echo "Dados da Aurora 2.0 copiados para o pacote 5.0."
