#!/usr/bin/env bash
# The whisper.android JNI bridge (vendored from the whisper.cpp submodule)
# hardcodes the transcription language to English. Since every voice note
# this app transcribes is Italian, force it to "it" instead - otherwise
# whisper.cpp tends to hallucinate captions like "(speaking in foreign
# language)" on non-English audio it's told is English.
set -euo pipefail

cd "$(dirname "$0")/.."

JNI_FILE="third_party/whisper.cpp/examples/whisper.android/lib/src/main/jni/whisper/jni.c"

if [ ! -f "$JNI_FILE" ]; then
  echo "error: $JNI_FILE not found - did you run 'git submodule update --init --recursive'?" >&2
  exit 1
fi

sed -i.bak 's/params\.language = "en";/params.language = "it";/' "$JNI_FILE"
rm -f "$JNI_FILE.bak"

grep -n 'params.language' "$JNI_FILE"
