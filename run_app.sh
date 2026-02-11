#!/bin/bash

echo "🔍 Checking for connected devices..."
# Check if adb sees any device
RAW_DEVICES=$(/opt/homebrew/share/android-commandlinetools/platform-tools/adb devices)
echo "DEBUG: Raw devices output:"
echo "$RAW_DEVICES"

DEVICE_COUNT=$(echo "$RAW_DEVICES" | grep -v "List of devices attached" | grep -v "^$" | wc -l | tr -d ' ')
echo "DEBUG: Device count detected: '$DEVICE_COUNT'"

if [ "$DEVICE_COUNT" = "0" ] || [ -z "$DEVICE_COUNT" ]; then
    echo "📱 No device found. Starting emulator pixel_9_pro_api_34..."
    # Check if emulator is already running but offline?

    /opt/homebrew/share/android-commandlinetools/emulator/emulator -avd pixel_9_pro_api_34 -netdelay none -netspeed full &

    echo "⏳ Waiting for emulator to boot..."
    /opt/homebrew/share/android-commandlinetools/platform-tools/adb wait-for-device

    echo "⏳ Waiting for system boot..."
    while [ "$(/opt/homebrew/share/android-commandlinetools/platform-tools/adb shell getprop sys.boot_completed | tr -d '\r')" != "1" ]; do
        sleep 1
    done

    echo "🎉 Emulator started!"
else
    echo "📱 Device found ($DEVICE_COUNT). Proceeding..."
fi

echo "🚀 Building APK..."
./gradlew assembleDebug --no-daemon

if [ $? -eq 0 ]; then
    echo "📦 Installing APK..."
    /opt/homebrew/share/android-commandlinetools/platform-tools/adb install -r build/outputs/apk/debug/green-go-client-debug.apk

    echo "📱 Launching App..."
    /opt/homebrew/share/android-commandlinetools/platform-tools/adb shell am force-stop green.go
    /opt/homebrew/share/android-commandlinetools/platform-tools/adb shell am start -n green.go/.LoginActivity

    echo "✅ Done!"
else
    echo "❌ Build failed!"
    exit 1
fi
