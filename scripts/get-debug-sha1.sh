#!/usr/bin/env bash
# 获取 Android Debug 签名 SHA1（百度地图 Key 申请用）
set -euo pipefail

KEYSTORE="${HOME}/.android/debug.keystore"
ALIAS="androiddebugkey"
STORE_PASS="android"

find_java() {
  if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/keytool" ]; then
    echo "${JAVA_HOME}/bin/keytool"
    return 0
  fi
  for candidate in \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" \
    "/opt/homebrew/opt/openjdk@17/bin/keytool" \
    "/usr/libexec/java_home" \
    "$(command -v keytool 2>/dev/null || true)"
  do
    if [ -x "$candidate" ] 2>/dev/null; then
      echo "$candidate"
      return 0
    fi
  done
  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    local home
    home="$(/usr/libexec/java_home 2>/dev/null || true)"
    if [ -n "$home" ] && [ -x "$home/bin/keytool" ]; then
      echo "$home/bin/keytool"
      return 0
    fi
  fi
  return 1
}

KEYTOOL="$(find_java || true)"
if [ -z "$KEYTOOL" ]; then
  echo "未找到 Java / keytool。请先安装 JDK 17 或用 Android Studio 打开项目后重试。"
  echo "Android Studio 路径: Gradle → app → Tasks → android → signingReport"
  exit 1
fi

if [ ! -f "$KEYSTORE" ]; then
  echo "未找到 debug.keystore，正在生成（与 Android Studio 默认一致）..."
  mkdir -p "${HOME}/.android"
  "$KEYTOOL" -genkeypair -v \
    -keystore "$KEYSTORE" \
    -alias "$ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$STORE_PASS" -keypass "$STORE_PASS" \
    -dname "CN=Android Debug,O=Android,C=US"
fi

echo ""
echo "=== Debug 签名指纹（包名 com.wendao.run）==="
"$KEYTOOL" -list -v \
  -keystore "$KEYSTORE" \
  -alias "$ALIAS" \
  -storepass "$STORE_PASS" -keypass "$STORE_PASS" \
  | rg -i "Alias name|SHA1:|SHA256:|MD5:|Valid from"

echo ""
echo "百度地图申请请填写 SHA1（去掉冒号或保留均可，按控制台要求）。"
echo "也可在项目 android 目录执行: ./gradlew :app:signingReport"
