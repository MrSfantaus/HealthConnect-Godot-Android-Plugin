# Godot Health Connect Plugin - Demo & Integration Guide

This directory contains the official demo project and integration documentation for the **Godot Health Connect** Android plugin. This plugin provides a high-performance, idiomatic GDScript interface for the Android Health Connect API, optimized for **Android 14+ (API 34-36)**.

## ✨ Key Features
- **JSON Bridge Reliability**: Data is passed as JSON strings between Kotlin and GDScript. This bypasses Godot's internal Java-object conversion limits, ensuring 100% stability for complex nested data.
- **Android 14+ Optimized**: Specifically designed for the integrated system-level Health Connect service (built into Android 14, 15, and 16).
- **Asynchronous API**: All Health Connect operations run on background threads and return results via signals, preventing frame drops in your game.
- **Timezone Aware**: Automatically handles system timezone offsets. Data written is stored with the correct local time, and data read is converted to ISO 8601 UTC strings.
- **Comprehensive Data Support**: Support for all common health metrics including steps, heart rate, hydration, and sleep.

---

## 📊 Supported Data Types & Units
The plugin uses standard SI units. When reading or writing, ensure your data matches these formats:

| Type Constant | Format | Unit | Description |
| :--- | :--- | :--- | :--- |
| `STEPS` | `count` | Integer | Step counts over an interval. |
| `DISTANCE` | `value` | Meters (float) | Distance traveled over an interval. |
| `ACTIVE_CALORIES_BURNED`| `value` | kcal (float) | active calories burned. |
| `TOTAL_CALORIES_BURNED` | `value` | kcal (float) | Total (Basal + Active) calories. |
| `WEIGHT` | `value` | kg (float) | Body mass (Instant record). |
| `HEIGHT` | `value` | meters (float) | Body height (Instant record). |
| `HYDRATION` | `value` | liters (float) | Water intake over an interval. |
| `HEART_RATE` | `samples` | Array | Array of `{"time": ISO_STRING, "value": BPM}`. |
| `EXERCISE_SESSION` | Session | - | Includes `exercise_type` (int), `title`, and `notes`. |
| `SLEEP_SESSION` | Session | - | Includes sleep stages (if available), `title`, and `notes`. |

---

## 🚀 Installation & Requirements

### 1. Requirements
- **Godot Engine**: 4.2 or higher.
- **Android SDK**: Build targeted for API 34+.
- **Device**: A physical device with Android 14+. For Android 13, the "Health Connect" app must be installed from the Play Store.

### 2. Plugin Setup
1. Copy the `addons/GodotHealthConnect` folder into your project.
2. Enable the plugin in **Project -> Project Settings -> Plugins**.
3. In your **Android Export Preset**:
   - Enable **Use Custom Build**.
   - Under **Plugins**, check **Godot Health Connect**.

### 3. Mandatory Manifest Configuration
Android 14+ will **reject** any health data request if these entries are missing from your `AndroidManifest.xml` (located in `res://android/build/AndroidManifest.xml` after installing the Android Build Template).

#### A. Permissions (Inside `<manifest>`)
```xml
<uses-permission android:name="android.permission.health.READ_STEPS"/>
<uses-permission android:name="android.permission.health.WRITE_STEPS"/>
<uses-permission android:name="android.permission.health.READ_WEIGHT"/>
<uses-permission android:name="android.permission.health.WRITE_WEIGHT"/>
<uses-permission android:name="android.permission.health.READ_HEIGHT"/>
<uses-permission android:name="android.permission.health.WRITE_HEIGHT"/>
<uses-permission android:name="android.permission.health.READ_HEART_RATE"/>
<uses-permission android:name="android.permission.health.WRITE_HEART_RATE"/>
<uses-permission android:name="android.permission.health.READ_DISTANCE"/>
<uses-permission android:name="android.permission.health.WRITE_DISTANCE"/>
<uses-permission android:name="android.permission.health.READ_ACTIVE_CALORIES_BURNED"/>
<uses-permission android:name="android.permission.health.WRITE_ACTIVE_CALORIES_BURNED"/>
<uses-permission android:name="android.permission.health.READ_TOTAL_CALORIES_BURNED"/>
<uses-permission android:name="android.permission.health.WRITE_TOTAL_CALORIES_BURNED"/>
<uses-permission android:name="android.permission.health.READ_HYDRATION"/>
<uses-permission android:name="android.permission.health.WRITE_HYDRATION"/>
<uses-permission android:name="android.permission.health.READ_EXERCISE"/>
<uses-permission android:name="android.permission.health.WRITE_EXERCISE"/>
<uses-permission android:name="android.permission.health.READ_SLEEP"/>
<uses-permission android:name="android.permission.health.WRITE_SLEEP"/>
```

#### B. Activity Alias & Property (Inside `<application>`)
```xml
<!-- Health Connect Property -->
<property android:name="android.health.connect.ALLOW_ACCESS_PERMISSION_RATIONALE" android:value="true" />

<!-- Privacy Policy Rationale Entry Point -->
<activity-alias
    android:name="com.godot.game.HealthRationaleActivity"
    android:targetActivity="com.godot.game.GodotApp"
    android:exported="true"
    android:permission="android.permission.health.READ_STEPS">
    <intent-filter>
        <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
        <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
    </intent-filter>
</activity-alias>
```

---

## 📖 API Usage (GDScript)

### 1. Initialization
```gdscript
var health = Engine.get_singleton("GodotHealthConnect")

func _ready():
    # Signals use JSON strings for lists/complex records
    health.records_read.connect(_on_records_read)
    health.aggregate_data_read.connect(_on_aggregate_data_read)
    health.error_occurred.connect(_on_error)
    
    if health.initialize():
        print("SDK Status: ", health.get_sdk_status())
```

### 2. Writing Data
```gdscript
func save_steps(count: int):
    health.insert_record({
        "type": "STEPS",
        "count": count,
        "start_time": "2025-12-30T10:00:00Z",
        "end_time": "2025-12-30T11:00:00Z"
    })
```

### 3. Reading Data
```gdscript
func _on_records_read(json_string: String):
    var records = JSON.parse_string(json_string)
    if records is Array:
        for r in records:
            match r.type:
                "STEPS": print("Steps: ", r.count)
                "WEIGHT": print("Weight: ", r.value, "kg")
```

---

## 🛠 Building & Debugging

1. **Build Plugin**: Run `./gradlew assemble` from project root.
2. **Export APK**: Export from Godot with **Custom Build** enabled.
3. **Sign APK**: Health Connect **requires a signed APK** (debug or release). Unsigned APKs will fail to open the permission UI.
4. **Logcat**: Use `adb logcat | grep HealthConnectManager` to see internal plugin logs.

## 📄 License
MIT License. Created by MrSfantaus.