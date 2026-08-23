#!/bin/bash
set -e

echo "=== STARTING ADB SERVER ==="
adb start-server

echo "=== WAITING FOR ADB TO DETECT EMULATOR ==="
# Wait up to 2 minutes for any device to appear
TIMEOUT=120
ELAPSED=0
DEVICE_ID=""

while [ $ELAPSED -lt $TIMEOUT ]; do
  # Get the first attached device
  DEVICE_ID=$(adb devices | grep -w "device" | awk '{print $1}' | head -n 1 || true)
  if [ -n "$DEVICE_ID" ]; then
    echo "Detected device: $DEVICE_ID"
    break
  fi
  sleep 5
  ELAPSED=$((ELAPSED + 5))
  echo "Still waiting for device... (${ELAPSED}s)"
done

if [ -z "$DEVICE_ID" ]; then
  echo "ERROR: Timeout waiting for adb to detect device."
  echo "=== DIAGNOSTICS ==="
  adb devices -l || true
  ps aux | grep emulator || true
  exit 1
fi

export ANDROID_DEVICE="$DEVICE_ID"
export ANDROID_DEVICE_NAME="$DEVICE_ID"

echo "=== WAITING FOR BOOT COMPLETION ==="
# Wait up to 5 minutes for boot_completed
BOOT_TIMEOUT=300
BOOT_ELAPSED=0
BOOT_COMPLETE=0

while [ $BOOT_ELAPSED -lt $BOOT_TIMEOUT ]; do
  SYS_BOOT=$(adb -s "$DEVICE_ID" shell getprop sys.boot_completed | tr -d '\r')
  DEV_BOOT=$(adb -s "$DEVICE_ID" shell getprop dev.bootcomplete | tr -d '\r')
  
  if [ "$SYS_BOOT" = "1" ] || [ "$DEV_BOOT" = "1" ]; then
    echo "Emulator boot completed."
    BOOT_COMPLETE=1
    break
  fi
  
  sleep 5
  BOOT_ELAPSED=$((BOOT_ELAPSED + 5))
  echo "Still waiting for boot complete... (${BOOT_ELAPSED}s)"
done

if [ "$BOOT_COMPLETE" -eq 0 ]; then
  echo "ERROR: Timeout waiting for emulator to boot."
  echo "=== DIAGNOSTICS ==="
  adb devices -l || true
  adb -s "$DEVICE_ID" shell getprop || true
  exit 1
fi

echo "=== DISABLING ANIMATIONS ==="
adb -s "$DEVICE_ID" shell settings put global window_animation_scale 0
adb -s "$DEVICE_ID" shell settings put global transition_animation_scale 0
adb -s "$DEVICE_ID" shell settings put global animator_duration_scale 0

echo "=== DEVICE DIAGNOSTICS ==="
echo "SDK Version: $(adb -s "$DEVICE_ID" shell getprop ro.build.version.sdk | tr -d '\r')"
echo "Model: $(adb -s "$DEVICE_ID" shell getprop ro.product.model | tr -d '\r')"

echo "=== INSTALLING APK ==="
# Find APK
APK_PATH=$(find appium-tests/app -name "*.apk" | head -n 1)
if [ -z "$APK_PATH" ]; then
  echo "ERROR: Could not find APK in appium-tests/app"
  exit 1
fi

echo "Installing $APK_PATH on $DEVICE_ID..."
adb -s "$DEVICE_ID" install -r "$APK_PATH"

echo "Verifying installation..."
APP_PACKAGE="com.simats.skillora"
if ! adb -s "$DEVICE_ID" shell pm list packages | grep -q "$APP_PACKAGE"; then
  echo "ERROR: Package $APP_PACKAGE not installed!"
  exit 1
fi
echo "Package $APP_PACKAGE installed successfully."

export APP_PACKAGE
export APP_ACTIVITY="com.simats.skillora.MainActivity"

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
  exit 1
fi

echo "=== RUNNING TESTS ==="
npm run wdio
TEST_EXIT_CODE=$?

kill $APPIUM_PID || true
exit $TEST_EXIT_CODE
