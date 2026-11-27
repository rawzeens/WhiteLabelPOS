#!/usr/bin/env bash
# Installs the simulator build and saves a picture of each screen in each of the 5 designs, using the demo shops.
# Usage: scripts/screenshots.sh <path to WhiteLabelPOS.app> <output folder>
set -euo pipefail
APP="$1"
OUT="$2"
HERE="$(cd "$(dirname "$0")" && pwd)"
CONFIG="$HERE/../../whitelabel/build.xcconfig"
APP_ID=$(grep -E '^APP_ID *=' "$CONFIG" | sed -E 's/^APP_ID *= *//' | tr -d '[:space:]')
mkdir -p "$OUT"
echo "App id $APP_ID"

pick() {
  xcrun simctl list devices available -j | python3 -c '
import json, sys
kind = sys.argv[1]
d = json.load(sys.stdin)["devices"]
xs = [x for rt, l in d.items() if "iOS" in rt for x in l if x["name"].startswith(kind)]
if kind == "iPhone":
    best = [x for x in xs if "Pro" in x["name"] and "Max" not in x["name"]] or xs
else:
    best = [x for x in xs if "Pro" in x["name"]] or xs
print(best[-1]["udid"] if best else "")' "$1"
}

start() {
  xcrun simctl boot "$1" 2>/dev/null || true
  xcrun simctl bootstatus "$1" -b
  xcrun simctl install "$1" "$APP"
  xcrun simctl status_bar "$1" override --time "9:41" --batteryState charged --batteryLevel 100 --wifiBars 3 --cellularBars 4 || true
}

# shot <device> <file name> <demo type> <design> <screen> [wait]
shot() {
  xcrun simctl terminate "$1" "$APP_ID" 2>/dev/null || true
  xcrun simctl launch "$1" "$APP_ID" -demo "$3" -design "$4" -screen "$5" >/dev/null
  sleep "${6:-4}"
  xcrun simctl io "$1" screenshot "$OUT/$2.png" >/dev/null
  echo "saved $2"
}

PHONE=$(pick iPhone)
echo "Using iPhone simulator $PHONE"
start "$PHONE"

# The first launch, before setup.
xcrun simctl terminate "$PHONE" "$APP_ID" 2>/dev/null || true
xcrun simctl launch "$PHONE" "$APP_ID" >/dev/null
sleep 6
xcrun simctl io "$PHONE" screenshot "$OUT/00-first-launch.png" >/dev/null
echo "saved 00-first-launch"

# Each design with the demo shop made for it.
n=1
for pair in grocery:fresh beauty:midnight cafe:cafe food:street retail:atelier; do
  type="${pair%%:*}"
  design="${pair##*:}"
  for screen in till cart checkout receipt; do
    shot "$PHONE" "0$n-$design-${screen/till/sell}" "$type" "$design" "$screen"
  done
  shot "$PHONE" "0$n-$design-design-picker" "$type" "$design" setup3
  n=$((n + 1))
done

# Setup, step by step (grocery demo, Fresh design).
for i in 0 1 2 3 4 5; do
  shot "$PHONE" "1$i-setup-step-$i" grocery fresh "setup$i"
done

# The other tabs.
shot "$PHONE" 20-orders grocery fresh orders
shot "$PHONE" 21-products grocery fresh products
shot "$PHONE" 22-edit-product grocery fresh product
shot "$PHONE" 23-reports grocery fresh reports
shot "$PHONE" 24-settings grocery fresh settings
shot "$PHONE" 25-midnight-reports beauty midnight reports
shot "$PHONE" 26-atelier-products retail atelier products

# Wide screen: the sale stays open next to the products.
PAD=$(pick iPad)
if [ -n "$PAD" ]; then
  echo "Using iPad simulator $PAD"
  start "$PAD"
  shot "$PAD" 30-ipad-fresh-sell grocery fresh till 6
  shot "$PAD" 31-ipad-cafe-sell cafe cafe till
  shot "$PAD" 32-ipad-street-sell food street till
  shot "$PAD" 33-ipad-midnight-reports beauty midnight reports
  xcrun simctl shutdown "$PAD" || true
fi
