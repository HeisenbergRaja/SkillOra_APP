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
echo "=== CHECKING AVD ==="
echo "Requested AVD: $AVD_NAME"
echo "=== RAW EMULATOR AVD LIST ==="
emulator -list-avds | cat -A || true

AVD_LIST="$(emulator -list-avds 2>/dev/null | tr -d '\r')"
echo "Available AVD names:"
printf '%s\n' "$AVD_LIST"

if printf '%s\n' "$AVD_LIST" | grep -Fxq "$AVD_NAME"; then
    echo "AVD: PASS ($AVD_NAME)"
else
    echo "AVD: FAIL ($AVD_NAME not found)"
    FAILED=1
    echo "=== AVAILABLE AVDS ==="
    emulator -list-avds || true
    echo "=== AVDMANAGER DETAILS ==="
    avdmanager list avd || true
fi

# Verify SDK Environment
if [ -z "${ANDROID_SDK_ROOT:-}" ]; then
    export ANDROID_SDK_ROOT="${ANDROID_HOME:-}"
fi
if [ -z "${ANDROID_SDK_ROOT:-}" ]; then
    echo "ERROR: ANDROID_SDK_ROOT is not set"
    exit 1
fi
export PATH="$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"

# Verify AVD file
AVD_HOME="${ANDROID_AVD_HOME:-$HOME/.config/.android/avd}"
AVD_DIR="$AVD_HOME/${AVD_NAME}.avd"
echo "Expected AVD directory: $AVD_DIR"
if [ -d "$AVD_DIR" ]; then
    echo "AVD directory: PASS"
else
    echo "AVD directory: FAIL"
    ls -la "$AVD_HOME" || true
    FAILED=1
fi

if [ -f "$AVD_DIR/config.ini" ]; then
    echo "AVD config: PASS"
else
    echo "AVD config: FAIL"
    FAILED=1
fi

if [ $FAILED -ne 0 ]; then
  echo "ANDROID_EMULATOR_PREFLIGHT=FAIL"
  echo "=== OS ===" > android-emulator-diagnostics/os_release.txt
  cat /etc/os-release >> android-emulator-diagnostics/os_release.txt || true
  exit 1
else
  echo "ANDROID_EMULATOR_PREFLIGHT=PASS"
fi
