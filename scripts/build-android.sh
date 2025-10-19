#!/bin/bash

# Android Build Script
# Usage: ./build-android.sh [debug|release]

set -e

BUILD_TYPE=${1:-debug}
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="$PROJECT_ROOT/android-app"

echo "🤖 Building Android App - Build Type: $BUILD_TYPE"

# Check if Android project exists
if [ ! -d "$ANDROID_DIR" ]; then
    echo "❌ Android project directory not found: $ANDROID_DIR"
    exit 1
fi

# Navigate to Android directory
cd "$ANDROID_DIR"

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "❌ Gradle wrapper not found. Please run 'gradle wrapper' first."
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

echo "🧹 Cleaning previous builds..."
./gradlew clean

if [ "$BUILD_TYPE" = "release" ]; then
    echo "🔨 Building release APK..."
    ./gradlew assembleRelease
    
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
    
    if [ -f "$APK_PATH" ]; then
        echo "✅ Release APK built successfully!"
        echo "📱 APK Location: $ANDROID_DIR/$APK_PATH"
        
        # Show APK info
        echo ""
        echo "📋 APK Information:"
        ls -lh "$APK_PATH"
        
        # Copy to root directory for easy access
        cp "$APK_PATH" "$PROJECT_ROOT/missed-call-auto-responder-release.apk"
        echo "📁 APK copied to: $PROJECT_ROOT/missed-call-auto-responder-release.apk"
        
    else
        echo "❌ Release APK not found at expected location"
        exit 1
    fi
    
else
    echo "🔨 Building debug APK..."
    ./gradlew assembleDebug
    
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    
    if [ -f "$APK_PATH" ]; then
        echo "✅ Debug APK built successfully!"
        echo "📱 APK Location: $ANDROID_DIR/$APK_PATH"
        
        # Show APK info
        echo ""
        echo "📋 APK Information:"
        ls -lh "$APK_PATH"
        
        # Copy to root directory for easy access
        cp "$APK_PATH" "$PROJECT_ROOT/missed-call-auto-responder-debug.apk"
        echo "📁 APK copied to: $PROJECT_ROOT/missed-call-auto-responder-debug.apk"
        
    else
        echo "❌ Debug APK not found at expected location"
        exit 1
    fi
fi

echo ""
echo "🎉 Android build completed successfully!"
echo ""
echo "📋 Next Steps:"
echo "1. Install the APK on your Android device"
echo "2. Grant required permissions (Phone, Call Log, Notifications)"
echo "3. Configure the backend API URL in app settings"
echo "4. Enable auto-responder and test with a missed call"