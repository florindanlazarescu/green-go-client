#!/bin/bash

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
