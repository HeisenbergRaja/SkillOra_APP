#!/bin/bash
set -e

echo "=== INITIALIZING ENVIRONMENT ==="
mkdir -p android-emulator-diagnostics

function collect_diagnostics() {
  echo "=== COLLECTING DIAGNOSTICS ==="
  adb devices -l > android-emulator-diagnostics/adb_devices.txt || true
  adb get-state > android-emulator-diagnostics/adb_state.txt 2>&1 || true
  emulator -list-avds > android-emulator-diagnostics/avds.txt 2>&1 || true
  ps aux | grep '[e]mulator' > android-emulator-diagnostics/emulator_process.txt || true
  ps aux | grep '[a]db' > android-emulator-diagnostics/adb_process.txt || true
  which adb > android-emulator-diagnostics/which_adb.txt || true
  which emulator > android-emulator-diagnostics/which_emulator.txt || true
  which sdkmanager > android-emulator-diagnostics/which_sdkmanager.txt || true
  which avdmanager > android-emulator-diagnostics/which_avdmanager.txt || true
  echo "$ANDROID_HOME" > android-emulator-diagnostics/android_home.txt || true
  echo "$ANDROID_SDK_ROOT" > android-emulator-diagnostics/android_sdk_root.txt || true
  echo "$PATH" > android-emulator-diagnostics/path.txt || true
  if [ -n "$ANDROID_DEVICE" ]; then
    adb logcat -d > android-emulator-diagnostics/logcat.txt || true
  fi
}

echo "=== VERIFYING PREREQUISITES ==="
for cmd in adb emulator sdkmanager avdmanager; do
  if ! command -v $cmd &> /dev/null; then
    echo "ERROR: $cmd could not be found!"
    collect_diagnostics
    exit 1
  fi
done

echo "=== STARTING ADB SERVER ==="
adb start-server

echo "=== PHASE A: WAITING FOR ADB DEVICE ==="
TIMEOUT=300
ELAPSED=0
DEVICE_ID=""

while [ $ELAPSED -lt $TIMEOUT ]; do
  # Get the first attached device that is not offline
  DEVICE_ID=$(adb devices | grep -w "device" | awk '{print $1}' | head -n 1 || true)
  if [ -n "$DEVICE_ID" ]; then
    echo "Detected device: $DEVICE_ID"
    break
  fi
  sleep 5
  ELAPSED=$((ELAPSED + 5))
  echo "Waiting for adb device registration... (${ELAPSED}s / ${TIMEOUT}s)"
done

if [ -z "$DEVICE_ID" ]; then
  echo "ERROR: ANDROID_EMULATOR_STARTUP_FAILED (Timeout waiting for adb device)"
  collect_diagnostics
  exit 1
fi

export ANDROID_DEVICE="$DEVICE_ID"
export ANDROID_DEVICE_NAME="$DEVICE_ID"

echo "=== VERIFYING EMULATOR PROCESS ==="
if ! ps aux | grep -q '[e]mulator'; then
  echo "ERROR: No emulator process found!"
  collect_diagnostics
  exit 1
fi

echo "=== PHASE B: WAITING FOR BOOT COMPLETION ==="
echo "Waiting for device to be online..."
adb -s "$ANDROID_DEVICE" wait-for-device

BOOT_TIMEOUT=300
BOOT_ELAPSED=0
BOOT_COMPLETE=0

while [ $BOOT_ELAPSED -lt $BOOT_TIMEOUT ]; do
  SYS_BOOT=$(adb -s "$ANDROID_DEVICE" shell getprop sys.boot_completed | tr -d '\r')
  DEV_BOOT=$(adb -s "$ANDROID_DEVICE" shell getprop dev.bootcomplete | tr -d '\r')
  
  if [ "$SYS_BOOT" = "1" ] || [ "$DEV_BOOT" = "1" ]; then
    echo "Emulator boot completed."
    BOOT_COMPLETE=1
    break
  fi
  
  sleep 5
  BOOT_ELAPSED=$((BOOT_ELAPSED + 5))
  echo "Waiting for boot complete... (${BOOT_ELAPSED}s / ${BOOT_TIMEOUT}s)"
done

if [ "$BOOT_COMPLETE" -eq 0 ]; then
  echo "ERROR: ANDROID_EMULATOR_STARTUP_FAILED (Timeout waiting for boot completion)"
  collect_diagnostics
  exit 1
fi

echo "=== DISABLING ANIMATIONS ==="
adb -s "$ANDROID_DEVICE" shell settings put global window_animation_scale 0
adb -s "$ANDROID_DEVICE" shell settings put global transition_animation_scale 0
adb -s "$ANDROID_DEVICE" shell settings put global animator_duration_scale 0

echo "=== DEVICE DIAGNOSTICS ==="
echo "SDK Version: $(adb -s "$ANDROID_DEVICE" shell getprop ro.build.version.sdk | tr -d '\r')"
echo "Model: $(adb -s "$ANDROID_DEVICE" shell getprop ro.product.model | tr -d '\r')"

echo "=== INSTALLING APK ==="
APK_PATH=$(find appium-tests/app -name "*.apk" | head -n 1)
if [ -z "$APK_PATH" ]; then
  echo "ERROR: Could not find APK in appium-tests/app"
  collect_diagnostics
  exit 1
fi

echo "Installing $APK_PATH on $ANDROID_DEVICE..."
adb -s "$ANDROID_DEVICE" install -r "$APK_PATH"

echo "Verifying installation..."
# Dynamically getting package from APK using aapt if available, else fallback
APP_PACKAGE=$(aapt dump badging "$APK_PATH" | grep package | awk '{print $2}' | sed s/name=//g | sed s/\'//g || echo "com.simats.skillora")
echo "Detected package: $APP_PACKAGE"

if ! adb -s "$ANDROID_DEVICE" shell pm list packages | grep -q "$APP_PACKAGE"; then
  echo "ERROR: Package $APP_PACKAGE not installed!"
  collect_diagnostics
  exit 1
fi
echo "Package $APP_PACKAGE installed successfully."

export APP_PACKAGE

echo "=== STARTING APPIUM ==="
cd appium-tests
npx appium &
APPIUM_PID=$!

echo "Waiting for Appium to start..."
APPIUM_READY=0
for i in {1..30}; do
  if curl -s http://127.0.0.1:4723/status > /dev/null; then
    echo "Appium server is ready."
    APPIUM_READY=1
    break
  fi
  sleep 2
done

if [ "$APPIUM_READY" -eq 0 ]; then
  echo "ERROR: Appium server failed to start."
  kill $APPIUM_PID || true
  collect_diagnostics
  exit 1
fi

echo "=== RUNNING TESTS ==="
npm run wdio
TEST_EXIT_CODE=$?

kill $APPIUM_PID || true
exit $TEST_EXIT_CODE
