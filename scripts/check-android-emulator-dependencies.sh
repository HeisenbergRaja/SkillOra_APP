#!/bin/bash
set -e

echo "=== ANDROID EMULATOR PRE-FLIGHT ==="
mkdir -p android-emulator-diagnostics

# Track failures
FAILED=0

# 1-4. Check Executables
for cmd in adb emulator sdkmanager avdmanager; do
  if command -v $cmd &> /dev/null; then
    echo "$cmd: PASS"
  else
    echo "$cmd: FAIL"
    FAILED=1
  fi
done

# 5. Check libpulse.so.0
if ldconfig -p | grep -q "libpulse.so.0"; then
  echo "libpulse.so.0: PASS"
else
  echo "libpulse.so.0: FAIL"
  FAILED=1
  echo "=== LIBPULSE ===" > android-emulator-diagnostics/libpulse.txt
  ldconfig -p | grep pulse >> android-emulator-diagnostics/libpulse.txt || true
  dpkg -l | grep pulse >> android-emulator-diagnostics/libpulse.txt || true
fi

# 6. Check QEMU dependencies
QEMU_BIN="$ANDROID_SDK_ROOT/emulator/qemu/linux-x86_64/qemu-system-x86_64"
if [ -f "$QEMU_BIN" ]; then
  # Build a library path for emulator bundled libraries
  EMULATOR_LD_LIBRARY_PATH=$(find "$ANDROID_SDK_ROOT/emulator" -type f -name "*.so*" -printf '%h\n' | sort -u | paste -sd ":" -)
  MISSING_DEPS=$(LD_LIBRARY_PATH="$EMULATOR_LD_LIBRARY_PATH${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}" ldd "$QEMU_BIN" | grep "not found" || true)
  if [ -n "$MISSING_DEPS" ]; then
    echo "QEMU dependencies: FAIL (missing libraries)"
    echo "=== QEMU MISSING LIBRARIES ==="
    echo "$MISSING_DEPS"
    FAILED=1
    echo "=== QEMU DEPENDENCIES ===" > android-emulator-diagnostics/qemu_deps.txt
    LD_LIBRARY_PATH="$EMULATOR_LD_LIBRARY_PATH${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}" ldd "$QEMU_BIN" >> android-emulator-diagnostics/qemu_deps.txt || true
  else
    echo "QEMU dependencies: PASS"
  fi
else
  echo "QEMU dependencies: WARNING (qemu-system-x86_64 not found at standard path)"
fi

# 7. Check AVD
AVD_NAME="${AVD_NAME:-skillora-test}"
AVD_FOUND=false
while IFS= read -r avd; do
    if [ "$avd" = "$AVD_NAME" ]; then
        AVD_FOUND=true
        break
    fi
done < <(emulator -list-avds 2>/dev/null || true)

if [ "$AVD_FOUND" = true ]; then
    echo "AVD: PASS ($AVD_NAME)"
else
    echo "AVD: FAIL ($AVD_NAME not found)"
    FAILED=1
    echo "=== AVAILABLE AVDS ==="
    emulator -list-avds || true
    avdmanager list avd || true
fi

if [ $FAILED -ne 0 ]; then
  echo "ANDROID_EMULATOR_PREFLIGHT=FAIL"
  echo "=== OS ===" > android-emulator-diagnostics/os_release.txt
  cat /etc/os-release >> android-emulator-diagnostics/os_release.txt || true
  exit 1
else
  echo "ANDROID_EMULATOR_PREFLIGHT=PASS"
fi
