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

# Per-ABI ELF architecture, 16 KiB LOAD alignment and native dependencies.
# These are real checks: the files contain explicit OK/FAIL markers consumed by CI.
if command -v python3 >/dev/null 2>&1; then
  python3 - "$TMP" "$OUT_DIR" <<'PY'
import glob, os, re, subprocess, sys

root, out_dir = sys.argv[1], sys.argv[2]
sos = sorted(glob.glob(os.path.join(root, 'jni', '*', '*.so')))
expected = {
    'arm64-v8a': ('ELF64', 'AArch64'),
    'armeabi-v7a': ('ELF32', 'ARM'),
    'x86': ('ELF32', 'Intel 80386'),
    'x86_64': ('ELF64', 'Advanced Micro Devices X86-64'),
}
abi_re = re.compile(r'jni[/\\]([^/\\]+)[/\\][^/\\]+\.so$')

arch_lines, align_lines, needed_lines = [], [], []
arch_ok = True
align_ok = True

if not sos:
    arch_lines.append('NO_SO_FILES')
    align_lines.append('NO_SO_FILES')
    arch_ok = False
    align_ok = False

for so in sos:
    m = abi_re.search(so)
    abi = m.group(1) if m else 'unknown'
    hdr = subprocess.run(['readelf', '-h', so], capture_output=True, text=True).stdout
    cls = re.search(r'^\s*Class:\s+(\S+)', hdr, re.M)
    mach = re.search(r'^\s*Machine:\s+(.+?)\s*$', hdr, re.M)
    cls_v = cls.group(1) if cls else 'unknown'
    mach_v = mach.group(1) if mach else 'unknown'
    exp = expected.get(abi)
    file_ok = exp is not None and exp == (cls_v, mach_v)
    if not file_ok:
        arch_ok = False
    arch_lines.append(f'{abi} {os.path.basename(so)}: class={cls_v} machine={mach_v} expected={exp} -> {"OK" if file_ok else "MISMATCH"}')

    # Dynamic dependencies (NEEDED entries).
    dyn = subprocess.run(['readelf', '-d', so], capture_output=True, text=True).stdout
    needed = re.findall(r'\(NEEDED\).*?\[(.+?)\]', dyn)
    needed_lines.append(f'{abi} {os.path.basename(so)}: ' + (', '.join(needed) if needed else 'none'))

    # LOAD alignment must be >= 16 KiB (0x4000) for 16 KiB page-size devices.
    prog = subprocess.run(['readelf', '-lW', so], capture_output=True, text=True).stdout
    loads = 0
    for line in prog.splitlines():
        parts = line.split()
        if len(parts) >= 2 and parts[0] == 'LOAD':
            loads += 1
            align_s = parts[-1]
            try:
                align = int(align_s, 16)
            except ValueError:
                align = -1
            ok = align >= 0x4000
            if not ok:
                align_ok = False
            align_lines.append(f'{abi} {os.path.basename(so)}: LOAD align={align_s} -> {"OK" if ok else "FAIL(<0x4000)"}')
    if loads == 0:
        align_ok = False
        align_lines.append(f'{abi} {os.path.basename(so)}: no LOAD segments found -> FAIL')

arch_lines.append('ELF_ARCH_OK' if arch_ok else 'ELF_ARCH_MISMATCH')
align_lines.append('ALL_LOAD_ALIGNED_16K' if align_ok else 'LOAD_ALIGNMENT_FAILED')

open(os.path.join(out_dir, 'elf_arch_full.txt'), 'w').write('\n'.join(arch_lines) + '\n')
open(os.path.join(out_dir, 'elf_arch_check.txt'), 'w').write(arch_lines[-1] + '\n')
open(os.path.join(out_dir, 'sixteen_kb.txt'), 'w').write('\n'.join(align_lines) + '\n')
open(os.path.join(out_dir, 'needed_libs.txt'), 'w').write('\n'.join(needed_lines) + '\n')
PY
  else
    echo "NO_PYTHON" > "$OUT_DIR/elf_arch_check.txt"
    echo "NO_PYTHON" > "$OUT_DIR/sixteen_kb.txt"
    echo "NO_PYTHON" > "$OUT_DIR/needed_libs.txt"
  fi

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
  # Exact JVM surface of the gomobile entry class (class/method names are never guessed).
  javap -classpath "$CLASSES_JAR" -public libXray.LibXray > "$OUT_DIR/libXray_class.txt" 2>&1
  javap -classpath "$CLASSES_JAR" -public go.Seq > "$OUT_DIR/go_Seq.txt" 2>&1
  grep -E 'invoke|Invoke' "$OUT_DIR/libXray_class.txt" > "$OUT_DIR/invoke_signature.txt"
else
  echo "NO_CLASSES_JAR" > "$OUT_DIR/classes.list.txt"
fi

# Manifest / metadata if present
[[ -f "$TMP/AndroidManifest.xml" ]] && cp "$TMP/AndroidManifest.xml" "$OUT_DIR/AndroidManifest.xml" || true
[[ -f "$TMP/R.txt" ]] && cp "$TMP/R.txt" "$OUT_DIR/R.txt" || true

echo "REPORT_DONE" | tee "$OUT_DIR/status.txt"
