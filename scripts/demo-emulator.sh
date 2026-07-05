#!/usr/bin/env bash
# 模拟器自动化演示：构建 → 安装 → 游客入道 → 模拟 GPS 移动 → 截图
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID="$ROOT/android"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
EMU="$SDK/emulator/emulator"
AVD_NAME="${PAOXIU_AVD:-PaoxiuDemo}"
PACKAGE="com.wendao.run"
ACTIVITY="${PACKAGE}/.MainActivity"

export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export PATH="$JAVA_HOME/bin:$SDK/platform-tools:$SDK/emulator:$PATH"

log() { echo "▶ $*"; }

wait_device() {
  log "等待设备就绪..."
  "$ADB" wait-for-device
  for _ in $(seq 1 60); do
    boot=$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') || true
    [ "$boot" = "1" ] && return 0
    sleep 2
  done
  echo "设备启动超时" >&2
  exit 1
}

tap_text() {
  local text="$1"
  log "点击: $text"
  "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 || true
  "$ADB" pull /sdcard/ui.xml /tmp/paoxiu-ui.xml >/dev/null 2>&1 || true
  if [ -f /tmp/paoxiu-ui.xml ]; then
    local bounds
    bounds=$(python3 -c "
import re, sys
xml=open('/tmp/paoxiu-ui.xml',encoding='utf-8',errors='ignore').read()
for m in re.finditer(r'text=\"%s\"[^>]*bounds=\"\\[(\d+),(\d+)\\]\\[(\d+),(\d+)\\]\"' % sys.argv[1], xml):
    x1,y1,x2,y2=map(int,m.groups())
    print((x1+x2)//2, (y1+y2)//2)
    break
" "$text" 2>/dev/null || true)
    if [ -n "$bounds" ]; then
      read -r x y <<< "$bounds"
      "$ADB" shell input tap "$x" "$y"
      return 0
    fi
  fi
  log "未找到「$text」，跳过后备点击"
  return 1
}

mock_route() {
  log "模拟 GPS 路线（约 1km）..."
  local lats=(39.9042 39.9055 39.9068 39.9081 39.9094 39.9107 39.9120 39.9133)
  local lng=116.4074
  for lat in "${lats[@]}"; do
    "$ADB" emu geo fix "$lng" "$lat" 2>/dev/null || \
      "$ADB" shell am broadcast -a android.intent.action.RUN -e cmd geo fix "$lng" "$lat" 2>/dev/null || true
    sleep 8
  done
}

screenshot() {
  local name="$1"
  local out="$ROOT/scripts/demo-screenshots/${name}.png"
  mkdir -p "$ROOT/scripts/demo-screenshots"
  "$ADB" exec-out screencap -p > "$out"
  log "截图: $out"
}

# --- 启动模拟器 ---
if ! "$ADB" devices | grep -q 'emulator'; then
  if ! "$EMU" -list-avds | grep -q "^${AVD_NAME}$"; then
    echo "未找到模拟器「${AVD_NAME}」。请先在 Android Studio → Device Manager 创建任意 Pixel 设备，"
    echo "或运行: PAOXIU_AVD=你的AVD名称 bash scripts/demo-emulator.sh"
    echo ""
    echo "正在打开 Android Studio 项目..."
    open -a "Android Studio" "$ANDROID"
    exit 1
  fi
  log "启动模拟器 ${AVD_NAME}..."
  "$EMU" -avd "$AVD_NAME" -no-snapshot-save &
  sleep 5
fi

wait_device

# --- 构建安装 ---
log "构建 Debug APK..."
cd "$ANDROID"
./gradlew :app:assembleDebug -q

APK="$ANDROID/app/build/outputs/apk/debug/app-debug.apk"
log "安装 APK..."
"$ADB" install -r "$APK"

log "启动 App..."
"$ADB" shell am force-stop "$PACKAGE" || true
"$ADB" shell am start -n "$ACTIVITY"
sleep 3

# 授予权限
"$ADB" shell pm grant "$PACKAGE" android.permission.ACCESS_FINE_LOCATION 2>/dev/null || true
"$ADB" shell pm grant "$PACKAGE" android.permission.ACCESS_COARSE_LOCATION 2>/dev/null || true
"$ADB" shell pm grant "$PACKAGE" android.permission.POST_NOTIFICATIONS 2>/dev/null || true

screenshot "01-login"

tap_text "游客入道" || "$ADB" shell input tap 540 1400
sleep 3
screenshot "02-after-login"

tap_text "开始灵根测试" || tap_text "开始修炼" || true
sleep 2
tap_text "开始灵根测试" 2>/dev/null || true
sleep 5

# 初始定位
"$ADB" emu geo fix 116.4074 39.9042 2>/dev/null || true
sleep 3
screenshot "03-run-start"

mock_route &
ROUTE_PID=$!

# 等待灵根测试或跑步进行
sleep 40
kill "$ROUTE_PID" 2>/dev/null || true
wait "$ROUTE_PID" 2>/dev/null || true

screenshot "04-run-progress"

tap_text "完成试炼" 2>/dev/null || tap_text "收功" 2>/dev/null || "$ADB" shell input tap 800 2200
sleep 8
screenshot "05-result"

tap_text "踏入修仙之路" 2>/dev/null || tap_text "返回修炼页" 2>/dev/null || true
sleep 2
screenshot "06-cultivate"

log "演示完成。截图目录: $ROOT/scripts/demo-screenshots/"
