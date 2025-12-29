# Godot Health Connect Plugin - Demo

This directory contains the demo project for the **Godot Health Connect** Android plugin. It showcases how to interact with the Health Connect API directly from GDScript.

## ✨ Features Showcased
- **Permission Management**: Request and check permissions for multiple health data types.
- **CRUD Operations**: Insert, read, and delete records for steps, weight, heart rate, and more.
- **Data Aggregation**: Group data by Day, Week, or Month (e.g., total steps per day).
- **SDK Status**: Verify if Health Connect is installed and available on the device.
- **System Integration**: Open Health Connect settings directly from the app.

## 📊 Supported Data Types
The demo and plugin currently support:
- `STEPS`
- `DISTANCE`
- `ACTIVE_CALORIES_BURNED`
- `WEIGHT`
- `HEART_RATE` (Series data)
- `EXERCISE_SESSION`
- `SLEEP_SESSION`

## 🚀 Requirements for Android 14+
This plugin is optimized for Android 14, 15, and 16 where Health Connect is a system component.

1.  **Custom Build**: You must enable "Use Custom Build" in the Godot Android Export settings.
2.  **Manifest Configuration**: The demo includes an `activity-alias` in `android/build/AndroidManifest.xml` to handle the mandatory Privacy Policy/Rationale requirement.
3.  **Signing**: To test Health Connect, the app **must be signed** with a valid developer key. The build command below uses `-Pperform_signing=true` to sign with the debug key.

## 🛠 How to Build & Run
1. **Build the plugin**:
   ```bash
   ./gradlew assemble
   ```
   (This automatically copies the plugin to the `demo/addons` folder).

2. **Build the demo APK**:
   ```bash
   cd plugin/demo/android/build
   ./gradlew clean assembleDebug -Pperform_signing=true
   ```

3. **Install & Run**:
   ```bash
   adb install -t -r plugin/demo/android/build/outputs/apk/debug/android-debug.apk
   ```

## 🔐 Permission Handling
When you press **"Request Permissions"**, the plugin launches the system Health Connect UI. On Android 14+, the system verifies that the app declares a valid Privacy Policy entry point via the `activity-alias` in the Manifest. Without this, the permission UI may fail to open or be blocked.
