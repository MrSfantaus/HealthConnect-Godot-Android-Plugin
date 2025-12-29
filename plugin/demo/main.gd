extends Node2D

var _plugin_name = "GodotHealthConnect"
var _android_plugin

# UI References (will be updated in main.tscn)
@onready var status_label = $CanvasLayer/VBoxContainer/Header/StatusLabel
@onready var log_text = $CanvasLayer/VBoxContainer/LogContainer/LogText

func _ready():
	if Engine.has_singleton(_plugin_name):
		_android_plugin = Engine.get_singleton(_plugin_name)
		_connect_signals()
		
		# Auto-initialize check
		if _android_plugin.is_available():
			_update_status(true)
			_log("Health Connect is available.")
		else:
			_update_status(false)
			_log("Health Connect NOT available.")
	else:
		_log("Plugin '" + _plugin_name + "' singleton not found.")

func _connect_signals():
	if not _android_plugin: return
	_android_plugin.permissions_result.connect(_on_permissions_result)
	_android_plugin.records_read.connect(_on_records_read)
	_android_plugin.record_read.connect(_on_record_read)
	_android_plugin.aggregate_data_read.connect(_on_aggregate_data_read)
	_android_plugin.record_inserted.connect(_on_record_inserted)
	_android_plugin.records_inserted.connect(_on_records_inserted)
	_android_plugin.record_updated.connect(_on_record_updated)
	_android_plugin.record_deleted.connect(_on_record_deleted)
	_android_plugin.records_deleted.connect(_on_records_deleted)
	_android_plugin.error_occurred.connect(_on_error_occurred)

func _update_status(available: bool):
	if status_label:
		status_label.text = "Health Connect: " + ("AVAILABLE" if available else "UNAVAILABLE")
		if available:
			status_label.modulate = Color.GREEN
		else:
			status_label.modulate = Color.RED

func _log(msg):
	print(msg)
	if log_text:
		log_text.text += str(msg) + "\n"

# --- Utilities ---

func _get_iso_time(offset_minutes: int = 0) -> String:
	# Returns current time + offset in ISO 8601 format (UTC implied for simplicity in demo)
	var time = Time.get_datetime_dict_from_system(true)
	var unix = Time.get_unix_time_from_datetime_dict(time) + (offset_minutes * 60)
	return Time.get_datetime_string_from_unix_time(unix) + "Z"

# --- UI Button Handlers ---

func _on_init_pressed():
	_log("Initializing...")
	if _android_plugin:
		var success = _android_plugin.initialize()
		_update_status(success)
		_log("Init result: " + str(success))
		if success:
			var status = _android_plugin.get_sdk_status()
			_log("SDK Info: " + str(status))

func _on_settings_pressed():
	if _android_plugin:
		_android_plugin.open_health_connect_settings()

func _on_req_permissions_pressed():
	if not _android_plugin: return
	var perms = [
		{"type": "STEPS", "access": "READ"}, {"type": "STEPS", "access": "WRITE"},
		{"type": "WEIGHT", "access": "READ"}, {"type": "WEIGHT", "access": "WRITE"},
		{"type": "HEART_RATE", "access": "READ"}, {"type": "HEART_RATE", "access": "WRITE"},
		{"type": "DISTANCE", "access": "READ"}, {"type": "DISTANCE", "access": "WRITE"},
		{"type": "ACTIVE_CALORIES_BURNED", "access": "READ"}, {"type": "ACTIVE_CALORIES_BURNED", "access": "WRITE"},
		{"type": "EXERCISE_SESSION", "access": "READ"}, {"type": "EXERCISE_SESSION", "access": "WRITE"},
		{"type": "SLEEP_SESSION", "access": "READ"}, {"type": "SLEEP_SESSION", "access": "WRITE"}
	]
	_android_plugin.request_permissions({"permissions": perms})
	_log("Requesting all permissions...")

func _on_check_permissions_pressed():
	if not _android_plugin: return
	var perms_to_check = [
		{"type": "STEPS", "access": "READ"},
		{"type": "STEPS", "access": "WRITE"}
	]
	var result = _android_plugin.check_permissions({"permissions": perms_to_check})
	_log("Permission Check: " + str(result))
	var granted = _android_plugin.get_granted_permissions()
	_log("All Granted: " + str(granted))

# --- Steps Operations ---

func _on_ins_steps_pressed():
	if not _android_plugin: return
	var start = _get_iso_time(-30) # 30 mins ago
	var end = _get_iso_time(0)
	var steps = {
		"type": "STEPS",
		"count": 500,
		"start_time": start,
		"end_time": end
	}
	_android_plugin.insert_record(steps)
	_log("Inserting 500 steps...")

func _on_read_steps_pressed():
	if not _android_plugin: return
	var start = _get_iso_time(-1440) # 24 hours ago
	var end = _get_iso_time(0)
	var config = {
		"record_type": "STEPS",
		"start_time": start,
		"end_time": end
	}
	_android_plugin.read_records(config)
	_log("Reading steps (last 24h)...")

func _on_agg_steps_pressed():
	if not _android_plugin: return
	var start = _get_iso_time(-10080) # 7 days ago
	var end = _get_iso_time(0)
	var config = {
		"record_type": "STEPS",
		"aggregation_type": "TOTAL",
		"time_range_slicer": "DAY", # Group by Day
		"start_time": start,
		"end_time": end
	}
	_android_plugin.read_aggregate_data(config)
	_log("Aggregating steps (Weekly daily breakdown)...")

# --- Weight Operations ---

func _on_ins_weight_pressed():
	if not _android_plugin: return
	var record = {
		"type": "WEIGHT",
		"value": 75.5, # kg
		"time": _get_iso_time()
	}
	_android_plugin.insert_record(record)
	_log("Inserting weight 75.5kg...")

func _on_read_weight_pressed():
	if not _android_plugin: return
	var start = _get_iso_time(-43200) # 30 days
	var end = _get_iso_time(0)
	_android_plugin.read_records({
		"record_type": "WEIGHT",
		"start_time": start,
		"end_time": end
	})
	_log("Reading weight history...")

# --- Heart Rate Operations ---

func _on_ins_hr_pressed():
	if not _android_plugin: return
	# Create a series of samples
	var samples = []
	for i in range(5):
		samples.append({
			"time": _get_iso_time(-5 + i), # 1 min apart
			"value": 60 + i * 5 # 60, 65, 70...
		})
	
	var record = {
		"type": "HEART_RATE",
		"start_time": _get_iso_time(-5),
		"end_time": _get_iso_time(0),
		"samples": samples
	}
	_android_plugin.insert_record(record)
	_log("Inserting Heart Rate Series...")

# --- Session Operations ---

func _on_ins_exercise_pressed():
	if not _android_plugin: return
	var record = {
		"type": "EXERCISE_SESSION",
		"start_time": _get_iso_time(-60),
		"end_time": _get_iso_time(0),
		"exercise_type": 56, # RUNNING (example ID)
		"title": "Morning Run",
		"notes": "Feeling good"
	}
	_android_plugin.insert_record(record)
	_log("Inserting Exercise Session...")

func _on_ins_sleep_pressed():
	if not _android_plugin: return
	var record = {
		"type": "SLEEP_SESSION",
		"start_time": _get_iso_time(-480), # 8 hours ago
		"end_time": _get_iso_time(0),
		"title": "Night Sleep",
		"notes": "Deep sleep"
	}
	_android_plugin.insert_record(record)
	_log("Inserting Sleep Session...")

# --- Delete ---

func _on_delete_all_pressed():
	if not _android_plugin: return
	var start = _get_iso_time(-10080) # 7 days
	var end = _get_iso_time(10) # slightly future
	_android_plugin.delete_records({
		"record_type": "STEPS",
		"start_time": start,
		"end_time": end
	})
	_log("Deleting STEPS from last 7 days...")

# --- Signal Callbacks ---

func _on_permissions_result(result):
	_log("Permissions Result: " + str(result))

func _on_records_read(json_string):
	var records = JSON.parse_string(json_string)
	if records is Array:
		_log("Records Found: " + str(records.size()))
		for r in records:
			if r.has("type") and r.type == "STEPS":
				_log(" - Steps: " + str(r.count) + " (" + r.start_time + ")")
			elif r.has("type") and r.type == "WEIGHT":
				_log(" - Weight: " + str(r.value) + "kg (" + r.time + ")")
			else:
				_log(" - " + str(r))
	else:
		_log("Records Read Error: Invalid format")

func _on_record_read(record):
	_log("Single Record: " + str(record))

func _on_aggregate_data_read(json_string):
	var aggregates = JSON.parse_string(json_string)
	if aggregates is Array:
		_log("Aggregates Found: " + str(aggregates.size()))
		for a in aggregates:
			_log(" - " + str(a))
	else:
		_log("Aggregate Read Error: Invalid format")

func _on_record_inserted(record_id):
	_log("SUCCESS: Record Inserted (ID: " + str(record_id) + ")")

func _on_records_inserted(record_ids):
	_log("SUCCESS: Batch Inserted IDs: " + str(record_ids))

func _on_record_updated(success):
	_log("Update Success: " + str(success))

func _on_record_deleted(success):
	_log("Delete Success: " + str(success))

func _on_records_deleted(count):
	_log("Batch Delete Success. Count: " + str(count))

func _on_error_occurred(code, message):
	_log("ERROR [" + str(code) + "]: " + str(message))