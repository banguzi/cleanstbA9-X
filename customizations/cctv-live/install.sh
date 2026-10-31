#!/usr/bin/env bash
# Build CCTV Live add-on, install on STB, grant overlay + Home watcher, and push private RTSP config.
# Usage (from upstream cleanyourtv checkout after copying customizations/cctv-live to addons/cctv-live):
#   TV_HOST=<TV_IP> bash addons/cctv-live/install.sh <jalur/ke/cctv-config.json>
# Berkas konfigurasi berisi alamat RTSP pribadi: simpan di luar git (mis. backup/) dan jangan di-commit.
set -euo pipefail

CONFIG="${1:?Pemakaian: TV_HOST=<TV_IP> $0 <cctv-config.json>}"
: "${TV_HOST:?Setel TV_HOST ke alamat IP STB}"
CONFIG="$(cd "$(dirname "$CONFIG")" && pwd)/$(basename "$CONFIG")"

cd "$(dirname "$0")"
TOOLS="$(cd ../../tools && pwd)"
PY="$TOOLS/venv/bin/python"
[ -x "$PY" ] || PY="$TOOLS/venv/Scripts/python.exe"
PKG="com.banguzi.tv.cctv"

tv() { "$PY" -I ../../tools/tv.py "$@"; }

"$PY" -I - "$CONFIG" <<'EOF'
import json, sys
c = json.load(open(sys.argv[1], encoding="utf-8"))
u = str(c.get("rtspUrl", "")).strip()
if not (u.startswith("rtsp://") or u.startswith("rtsps://")) or "<" in u:
    sys.exit("rtspUrl pada konfigurasi harus berupa alamat rtsp:// yang valid")
EOF

if [ ! -f gradlew ] && [ -f ../frigate-glance/gradlew ]; then
    cp -R ../frigate-glance/gradle ../frigate-glance/gradlew .
    chmod +x gradlew
fi

if [ ! -f local.properties ]; then
    for sdk in "${ANDROID_HOME:-}" "/opt/homebrew/share/android-commandlinetools" "$HOME/Library/Android/sdk"; do
        if [ -n "$sdk" ] && [ -d "$sdk/platforms" ]; then
            echo "sdk.dir=$sdk" > local.properties
            break
        fi
    done
fi

if [ -z "${JAVA_HOME:-}" ] && [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
fi
if [ -n "${JAVA_HOME:-}" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

./gradlew --no-daemon lintRelease assembleRelease

"$PY" -I ../../tools/b64push.py app/build/outputs/apk/release/app-release.apk /data/local/tmp/cctv-live.apk
OUT=$(tv 'pm install -r /data/local/tmp/cctv-live.apk; rm -f /data/local/tmp/cctv-live.apk')
echo "$OUT"
case "$OUT" in *Success*) ;; *) echo "ERROR: pm install gagal" >&2; exit 1 ;; esac

tv "appops set $PKG SYSTEM_ALERT_WINDOW allow; echo overlay_granted"

STAGE="/data/local/tmp/cctv-config.json"
tv --push "$CONFIG" "$STAGE" >/dev/null
OUT=$(tv "if content write --uri content://$PKG.config/config.json < $STAGE; then echo config_pushed; else echo config_failed; fi; rm -f $STAGE")
echo "$OUT"
case "$OUT" in *config_pushed*) ;; *) echo "ERROR: gagal mengirim konfigurasi ke STB" >&2; exit 1 ;; esac

SVC="$PKG/.HomeWatchService"
tv "s=\$(settings get secure enabled_accessibility_services); case \"\$s\" in *$SVC*) settings put secure enabled_accessibility_services ''; settings put secure accessibility_enabled 0; sleep 1; settings put secure enabled_accessibility_services \"\$s\" ;; null|'') settings put secure enabled_accessibility_services $SVC ;; *) settings put secure enabled_accessibility_services \"\$s:$SVC\" ;; esac; settings put secure accessibility_enabled 1; echo home_watch_enabled"

tv "input keyevent KEYCODE_HOME" >/dev/null
echo "Selesai. Jawab 'Show card' pada prompt di TV bila kartu CCTV Live baru pertama kali dipasang."
