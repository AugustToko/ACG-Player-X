#!/usr/bin/env bash
set -euo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
command -v kotlinc >/dev/null || { echo 'kotlinc is required' >&2; exit 1; }
command -v java >/dev/null || { echo 'java is required' >&2; exit 1; }
work="$(mktemp -d)"
trap 'rm -rf -- "$work"' EXIT
package='top/geek_studio/chenlongcould/musicplayer/ui'
kotlinc \
  "$root/modern-app/src/main/kotlin/$package/ImportPreviewPreparation.kt" \
  "$root/modern-app/src/main/kotlin/$package/StatisticsMetadataCompleteness.kt" \
  "$root/modern-app/src/test/kotlin/$package/ImportPreviewPreparationCases.kt" \
  -include-runtime -d "$work/preparation-tests.jar"
java -jar "$work/preparation-tests.jar"
