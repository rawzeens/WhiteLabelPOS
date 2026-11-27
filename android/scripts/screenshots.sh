#!/usr/bin/env bash
# Installs the debug app on the running emulator and saves a picture of each screen in each design.
# Usage: android/scripts/screenshots.sh <app-debug.apk> <output folder>
set -euo pipefail
APK="$1"
OUT="$2"
HERE="$(cd "$(dirname "$0")" && pwd)"
APP_ID=$(grep -E '^APP_ID' "$HERE/../../whitelabel/build.xcconfig" | sed 's/.*= *//')
ACT="$APP_ID/com.r4xn.whitelabelpos.ui.MainActivity"
mkdir -p "$OUT"
adb install -r "$APK" >/dev/null
# Tidy status bar: 9:41, full battery, no notifications.
adb shell settings put global sysui_demo_allowed 1 >/dev/null || true
adb shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null || true
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941 >/dev/null || true
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null || true
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null || true

shot() { # file demo design screen [seconds]
  adb shell am start -S -W -n "$ACT" --es demo "$2" --es design "$3" --es open "$4" >/dev/null 2>&1
  sleep "${5:-4}"
  adb exec-out screencap -p > "$OUT/$1.png"
  echo "saved $1"
}

i=1
# Each design with the demo shop it suits best.
for pair in grocery:fresh beauty:midnight cafe:cafe food:street retail:atelier; do
  demo=${pair%%:*}; design=${pair##*:}
  for screen in till cart checkout receipt; do
    shot "$(printf %02d $i)-$design-${screen/till/sell}" "$demo" "$design" "$screen"; i=$((i+1))
  done
done
# Setup steps (the design step shows all five looks).
for step in 0 1 2 3 4 5; do shot "$(printf %02d $i)-setup-$step" grocery fresh "setup$step" 5; i=$((i+1)); done
for screen in orders products product reports settings; do shot "$(printf %02d $i)-$screen" cafe cafe "$screen"; i=$((i+1)); done
shot "$(printf %02d $i)-midnight-reports" beauty midnight reports; i=$((i+1))
shot "$(printf %02d $i)-street-settings" food street settings; i=$((i+1))

# Tablet: the cart stays open next to the products.
adb shell wm size 2560x1600 && adb shell wm density 320
sleep 2
for pair in cafe:cafe food:street retail:atelier; do
  demo=${pair%%:*}; design=${pair##*:}
  shot "$(printf %02d $i)-tablet-$design" "$demo" "$design" till 5; i=$((i+1))
done
adb shell wm size reset && adb shell wm density reset

if adb logcat -d -s AndroidRuntime:E | grep -q "FATAL EXCEPTION"; then
  echo "The app crashed while taking screenshots:"
  adb logcat -d -s AndroidRuntime:E | tail -40
  exit 1
fi
