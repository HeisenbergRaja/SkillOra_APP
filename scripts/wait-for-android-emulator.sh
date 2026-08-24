#!/usr/bin/env bash
set -euo pipefail

export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"

if [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "ERROR: ANDROID_SDK_ROOT is not set"
    exit 1
fi

export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$HOME/.config/.android/avd}"
mkdir -p "$ANDROID_AVD_HOME"
export PATH="$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"

echo "=== ANDROID ENVIRONMENT ==="
echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
echo "ANDROID_AVD_HOME=$ANDROID_AVD_HOME"
echo "HOME=$HOME"

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
  if [ -n "${ANDROID_DEVICE:-}" ]; then
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

echo "=========================================="
echo "ANDROID AVD DIAGNOSTICS"
echo "=========================================="
echo "HOME=$HOME"
echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
echo "ANDROID_AVD_HOME=$ANDROID_AVD_HOME"
echo "=== AVD LIST ==="
emulator -list-avds
echo "=== AVD DIRECTORY ==="
ls -la "$ANDROID_AVD_HOME" || true
echo "=== SKILLORA AVD ==="
ls -la "$ANDROID_AVD_HOME/skillora-test.avd" || true
echo "=== AVD CONFIG FILES ==="
find "$HOME/.config/.android" -maxdepth 3 -name "skillora-test.ini" -o -name "skillora-test.avd" -print || true

echo "=== STARTING ADB SERVER ==="
adb start-server

AVD_NAME="${AVD_NAME:-skillora-test}"
if ! emulator -list-avds | tr -d '\r' | grep -Fxq "$AVD_NAME"; then
    echo "ERROR: AVD '$AVD_NAME' is not visible"
    emulator -list-avds || true
    exit 1
fi

echo "=== STARTING EMULATOR ==="
emulator -avd "$AVD_NAME" -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect > emulator.log 2>&1 &
EMULATOR_PID=$!

echo "=== PHASE A: WAITING FOR ADB DEVICE ==="
for i in $(seq 1 60); do
    ANDROID_DEVICE="$(adb devices | awk '/^emulator-[0-9]+[[:space:]]+device$/ {print $1; exit}')"
    if [ -n "$ANDROID_DEVICE" ]; then
        echo "Android device detected: $ANDROID_DEVICE"
        break
    fi
    sleep 5
done

if [ -z "$ANDROID_DEVICE" ]; then
    echo "ERROR: Android emulator device was not detected"
    adb devices -l || true
    echo "=== EMULATOR LOG ==="
    cat emulator.log || true
    collect_diagnostics
    exit 1
fi

export ANDROID_DEVICE="$ANDROID_DEVICE"
export ANDROID_DEVICE_NAME="$ANDROID_DEVICE"

echo "=== VERIFYING EMULATOR PROCESS ==="
if ! ps aux | grep -q '[e]mulator'; then
  echo "ERROR: No emulator process found!"
  collect_diagnostics
  exit 1
fi

echo "=== PHASE B: WAITING FOR BOOT COMPLETION ==="
echo "Waiting for device to be online..."
adb -s "$ANDROID_DEVICE" wait-for-device

for i in $(seq 1 60); do
    BOOT_COMPLETED="$(adb -s "$ANDROID_DEVICE" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    if [ "$BOOT_COMPLETED" = "1" ]; then
        echo "Android boot completed"
        break
    fi
    echo "Waiting for Android boot... attempt $i/60"
    sleep 5
done

if [ "$BOOT_COMPLETED" != "1" ]; then
    echo "ERROR: Android emulator failed to boot"
    adb -s "$ANDROID_DEVICE" shell getprop sys.boot_completed || true
    adb devices -l || true
    cat emulator.log || true
    collect_diagnostics
    exit 1
fi

AVD_NAME="${AVD_NAME:-skillora-test}"
ANDROID_VERSION=$(adb -s "$ANDROID_DEVICE" shell getprop ro.build.version.release | tr -d '\r')
API_LEVEL=$(adb -s "$ANDROID_DEVICE" shell getprop ro.build.version.sdk | tr -d '\r')

echo "=== ANDROID DEVICE READY ==="
echo "Device: $ANDROID_DEVICE"
echo "Android Version: $ANDROID_VERSION"
echo "API Level: $API_LEVEL"
echo "Boot Completed: $BOOT_COMPLETED"
echo "Model: $(adb -s "$ANDROID_DEVICE" shell getprop ro.product.model | tr -d '\r')"
echo "=========================================="
adb devices
echo "=========================================="

echo "=== DISABLING ANIMATIONS ==="
adb -s "$ANDROID_DEVICE" shell settings put global window_animation_scale 0
adb -s "$ANDROID_DEVICE" shell settings put global transition_animation_scale 0
adb -s "$ANDROID_DEVICE" shell settings put global animator_duration_scale 0

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

echo "Installing UiAutomator2 driver..."
npx appium driver list --installed | grep -q uiautomator2 || npx appium driver install uiautomator2

npx appium --address 127.0.0.1 --port 4723 --base-path / > appium.log 2>&1 &
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
  cat appium.log || true
  collect_diagnostics
  exit 1
fi

echo "=== RUNNING SMOKE TEST ==="
npm run test:android:smoke
SMOKE_EXIT_CODE=$?

if [ "$SMOKE_EXIT_CODE" -ne 0 ]; then
  echo "ERROR: Smoke test failed. Aborting full test suite."
  kill $APPIUM_PID || true
  collect_diagnostics
  exit 1
fi

echo "=== RUNNING FULL TEST SUITE ==="
npm run test:android
TEST_EXIT_CODE=$?

kill $APPIUM_PID || true
exit $TEST_EXIT_CODE
