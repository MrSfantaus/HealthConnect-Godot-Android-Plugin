# Godot Health Connect Plugin

A high-performance Android plugin for **Godot 4.2+** that integrates with **Android Health Connect**. It allows Godot games and apps to seamlessly read, write, and aggregate health and fitness data on Android 14+ devices.

---

## 📖 Table of Contents
1. [Features](#features)
2. [Requirements](#requirements)
3. [Installation](#installation)
4. [Mandatory Android 14+ Configuration](#mandatory-android-14-configuration)
5. [Quick Start](#quick-start)
6. [API Reference](#api-reference)
7. [Building](#building)
8. [Troubleshooting](#troubleshooting)

---

## ✨ Features
- **Android 14+ Native**: Built for the integrated Health Connect system (API 34+).
- **Comprehensive CRUD**: Create, Read, Update, and Delete health records.
- **Aggregation Support**: Calculate totals, averages, and min/max with time-range slicing (Daily/Weekly/Monthly).
- **Supported Data Types**: Steps, Heart Rate, Weight, Height, Distance, Active/Total Calories, Hydration, Exercise Sessions, and Sleep Sessions.
- **JSON Bridge Reliability**: Uses JSON serialization for complex data exchange between Kotlin and GDScript, ensuring 100% reliability across all Android versions.
- **Local Timezone Aware**: Automatically handles system timezone offsets for accurate data recording.
- **Asynchronous Design**: All operations are non-blocking and communicate via Godot signals.

---

## 🛠 Requirements
- **Godot Engine**: 4.2 or higher.
- **Android SDK**: Min SDK 34 (Android 14+).
- **Build System**: Android Custom Build enabled in Godot.
- **Device**: Physical device with Android 14+ (or Android 13 with the Health Connect app installed).

---

## 🚀 Installation
1. Download the plugin and place the `GodotHealthConnect` folder into your Godot project's `addons/` directory.
2. In Godot: **Project -> Project Settings -> Plugins**, and enable **GodotHealthConnect**.
3. **Project -> Export -> Android**:
   - Enable **Use Custom Build**.
   - In **Plugins**, check **Godot Health Connect**.

---

## 🔐 Mandatory Android 14+ Configuration
Health Connect requires specific manifest entries to allow the permission UI to open.

### 1. Activity Alias for Privacy Policy
Add this to your `android/build/AndroidManifest.xml` (or your Godot export template) inside the `<application>` tag. Replace `com.godot.game.GodotApp` with your actual Godot activity name.

```xml
<!-- Health Connect Privacy Policy Rationale -->
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

### 2. Access Property & Permissions
Inside the `<application>` tag:
```xml
<property android:name="android.health.connect.ALLOW_ACCESS_PERMISSION_RATIONALE" android:value="true" />
```

Declare all permissions your app needs:
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

---

## 🏁 Quick Start

```gdscript
extends Node

var health

func _ready():
	if Engine.has_singleton("GodotHealthConnect"):
		health = Engine.get_singleton("GodotHealthConnect")
		health.permissions_result.connect(_on_permissions_result)
		health.records_read.connect(_on_records_read)
		
		if health.initialize():
			print("Health Connect Initialized")
			_request_permissions()

func _request_permissions():
	health.request_permissions({
		"permissions": [
			{"type": "STEPS", "access": "READ"},
			{"type": "STEPS", "access": "WRITE"}
		]
	})

func _on_permissions_result(result: Dictionary):
	print("Permissions Result: ", result)

func _on_records_read(json_string: String):
	var records = JSON.parse_string(json_string)
	if records is Array:
		print("Found %d records" % records.size())
```

---

## 📚 API Reference

### Methods
| Method | Description |
| :--- | :--- |
| `initialize() -> bool` | Initializes the plugin. Returns `true` if successful. |
| `is_available() -> bool` | Checks if Health Connect is available on the device. |
| `get_sdk_status() -> Dictionary` | Returns detailed info about the SDK availability and version. |
| `request_permissions(config: Dictionary)` | Opens the system UI to request permissions. |
| `check_permissions(config: Dictionary) -> Dictionary` | Synchronously checks if specific permissions are granted. |
| `get_granted_permissions() -> Array` | Returns a list of all granted health permission strings. |
| `read_records(config: Dictionary)` | Reads records for a specific type and time range. (Signal: `records_read`) |
| `insert_record(record: Dictionary)` | Inserts a single health record. (Signal: `record_inserted`) |
| `read_aggregate_data(config: Dictionary)` | Queries aggregated data (Total, Avg, etc). (Signal: `aggregate_data_read`) |
| `open_health_connect_settings()` | Opens the system Health Connect settings screen. |

### Signals
- `permissions_result(result: Dictionary)`
- `records_read(json_string: String)`: Returns an array of records as a JSON string.
- `aggregate_data_read(json_string: String)`: Returns an array of aggregated buckets as a JSON string.
- `record_read(json_string: String)`: Returns a single record as a JSON string.
- `record_inserted(record_id: String)`
- `error_occurred(code: String, message: String)`

---

## 💡 GDScript Examples

### 1. Reading Steps from the Last 24 Hours
```gdscript
func read_last_day_steps():
	var end_time = Time.get_datetime_string_from_system(true) + "Z"
	var start_time = Time.get_datetime_string_from_unix_time(Time.get_unix_time_from_system() - 86400) + "Z"
	
	health.read_records({
		"record_type": "STEPS",
		"start_time": start_time,
		"end_time": end_time
	})

func _on_records_read(json_string: String):
	var records = JSON.parse_string(json_string)
	for record in records:
		print("Steps: %d from %s" % [record.count, record.start_time])
```

### 2. Inserting a Weight Record
```gdscript
func record_weight(kg: float):
	health.insert_record({
		"type": "WEIGHT",
		"value": kg,
		"time": Time.get_datetime_string_from_system(true) + "Z"
	})
```

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
