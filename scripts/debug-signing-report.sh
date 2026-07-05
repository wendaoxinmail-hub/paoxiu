#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../android"
./gradlew :app:signingReport 2>/dev/null | rg -A2 "Variant: debug" | rg "SHA1|MD5" || ./gradlew :app:signingReport
