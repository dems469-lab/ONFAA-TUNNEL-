#!/usr/bin/env bash
set -euo pipefail

AAR_PATH="${1:-libXray.aar}"
OUT_DIR="${2:-poc-report}"
mkdir -p "$OUT_DIR"

if [[ ! -f "$AAR_PATH" ]]; then
  echo "AAR not found: $AAR_PATH" >&2
  exit 1
fi

sha256sum "$AAR_PATH" | tee "$OUT_DIR/aar.sha256"
stat -c '%n %s bytes' "$AAR_PATH" | tee "$OUT_DIR/aar.size.txt" || stat "$AAR_PATH" > "$OUT_DIR/aar.size.txt"

unzip -l "$AAR_PATH" > "$OUT_DIR/aar.contents.txt"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
unzip -q "$AAR_PATH" -d "$TMP"

# Native libs / ABIs
if [[ -d "$TMP/jni" ]]; then
  find "$TMP/jni" -type f | sort > "$OUT_DIR/jni.files.txt"
  find "$TMP/jni" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort > "$OUT_DIR/abis.txt"
else
  echo "NO_JNI_DIR" > "$OUT_DIR/jni.files.txt"
  : > "$OUT_DIR/abis.txt"
fi

# ELF and 16K alignment checks
{
  echo "# ELF info"
  if command -v readelf >/dev/null 2>&1; then
    while IFS= read -r so; do
      echo "--- $so ---"
      readelf -h "$so" || true
      readelf -l "$so" | sed -n '1,120p' || true
    done < <(find "$TMP/jni" -type f -name '*.so' 2>/dev/null | sort)
  elif command -v llvm-readelf >/dev/null 2>&1; then
    while IFS= read -r so; do
      echo "--- $so ---"
      llvm-readelf -h "$so" || true
      llvm-readelf -l "$so" | sed -n '1,120p' || true
    done < <(find "$TMP/jni" -type f -name '*.so' 2>/dev/null | sort)
  else
    echo "readelf not available"
  fi
} > "$OUT_DIR/elf.txt" 2>&1

# 16K alignment summary (LOAD segments align >= 0x4000)
{
  if command -v python3 >/dev/null 2>&1; then
    python3 - "$TMP" <<'PY'
import glob, subprocess, sys, os
root=sys.argv[1]
sos=sorted(glob.glob(os.path.join(root,'jni','*','*.so')))
if not sos:
    print('NO_SO_FILES')
for so in sos:
    print(f'FILE {so}')
    try:
        out=subprocess.check_output(['readelf','-l',so], text=True, stderr=subprocess.STDOUT)
    except Exception as e:
        print('READERR', e)
        continue
    for line in out.splitlines():
        if 'LOAD' in line:
            print(line)
PY
  fi
} > "$OUT_DIR/load_segments.txt" 2>&1

# JVM classes inspection (gomobile generates classes.jar in AAR)
CLASSES_JAR="$TMP/classes.jar"
if [[ -f "$CLASSES_JAR" ]]; then
  cp "$CLASSES_JAR" "$OUT_DIR/classes.jar"
  jar tf "$CLASSES_JAR" | sort > "$OUT_DIR/classes.list.txt"
  {
    echo "# javap public signatures"
    if command -v javap >/dev/null 2>&1; then
      while IFS= read -r cls; do
        [[ "$cls" == *.class ]] || continue
        cname="${cls%.class}"
        cname="${cname//\//.}"
        echo "--- $cname ---"
        javap -classpath "$CLASSES_JAR" -public "$cname" 2>&1 || true
      done < <(grep '\.class$' "$OUT_DIR/classes.list.txt")
    else
      echo "javap not available"
    fi
  } > "$OUT_DIR/javap.txt" 2>&1
  grep -n "invoke" -i "$OUT_DIR/javap.txt" > "$OUT_DIR/invoke_matches.txt" 2>&1 || true
else
  echo "NO_CLASSES_JAR" > "$OUT_DIR/classes.list.txt"
fi

# Manifest / metadata if present
[[ -f "$TMP/AndroidManifest.xml" ]] && cp "$TMP/AndroidManifest.xml" "$OUT_DIR/AndroidManifest.xml" || true
[[ -f "$TMP/R.txt" ]] && cp "$TMP/R.txt" "$OUT_DIR/R.txt" || true

echo "REPORT_DONE" | tee "$OUT_DIR/status.txt"
