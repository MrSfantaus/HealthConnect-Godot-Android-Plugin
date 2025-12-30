package io.github.mrsfantaus.godot.healthconnect

import android.util.Log
import android.content.Intent
import org.godotengine.godot.Godot
import org.godotengine.godot.Dictionary
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.UsedByGodot
import org.godotengine.godot.plugin.SignalInfo
import kotlinx.coroutines.runBlocking

class GodotHealthConnect(godot: Godot): GodotPlugin(godot) {

    companion object {
        private const val TAG = "GodotHealthConnect"
    }

    private var healthConnectManager: HealthConnectManager? = null

    override fun getPluginName() = "GodotHealthConnect"

    override fun getPluginSignals(): Set<SignalInfo> {
        return setOf(
            SignalInfo("permissions_result", Dictionary::class.java),
            SignalInfo("records_read", String::class.java),
            SignalInfo("aggregate_data_read", String::class.java),
            SignalInfo("record_read", String::class.java),
            SignalInfo("record_inserted", String::class.java),
            SignalInfo("records_inserted", Dictionary::class.java),
            SignalInfo("record_updated", Boolean::class.javaObjectType),
            SignalInfo("record_deleted", Boolean::class.javaObjectType),
            SignalInfo("records_deleted", Integer::class.java),
            SignalInfo("error_occurred", String::class.java, String::class.java)
        )
    }

    @UsedByGodot
    fun initialize(): Boolean {
        Log.i(TAG, "initialize() called")
        return try {
            val currentActivity = activity ?: return false
            Log.i(TAG, "Initializing HealthConnectManager directly...")
            healthConnectManager = HealthConnectManager(currentActivity, godot)
            Log.i(TAG, "HealthConnectManager initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize HealthConnectManager: ${e.message}", e)
            false
        }
    }

    @UsedByGodot
    fun is_available(): Boolean {
        val currentActivity = activity ?: return false
        return VersionCompatibility(currentActivity).isHealthConnectAvailable()
    }

    @UsedByGodot
    fun get_sdk_status(): Dictionary {
        val status = Dictionary()
        val currentActivity = activity ?: return status
        val vc = VersionCompatibility(currentActivity)
        status["available"] = vc.isHealthConnectAvailable()
        status["version_type"] = vc.getVersionType()
        status["android_version"] = android.os.Build.VERSION.SDK_INT
        status["requires_installation"] = vc.requiresInstallation()
        status["package_name"] = vc.getHealthConnectPackageName()
        return status
    }

    @UsedByGodot
    fun open_health_connect_settings() {
        Log.i(TAG, "open_health_connect_settings() called")
        activity?.let {
            VersionCompatibility(it).openHealthConnectSettings()
        }
    }

    @UsedByGodot
    fun request_permissions(permissions_dict: Dictionary) {
        Log.i(TAG, "request_permissions() called")
        val manager = healthConnectManager
        if (manager == null) {
            Log.w(TAG, "Manager not initialized. Calling initialize() automatically.")
            if (initialize()) {
                healthConnectManager?.requestPermissions(permissions_dict)
            }
            return
        }
        manager.requestPermissions(permissions_dict)
    }

    @UsedByGodot
    fun check_permissions(permissions_dict: Dictionary): Dictionary {
        val manager = healthConnectManager ?: return Dictionary()
        return runBlocking {
            manager.checkPermissionsSync(permissions_dict)
        }
    }

    @UsedByGodot
    fun get_granted_permissions(): Array<String> {
        val manager = healthConnectManager ?: return arrayOf()
        return runBlocking {
            manager.getGrantedPermissionsSync()
        }
    }

    @UsedByGodot
    fun insert_record(record: Dictionary) {
        healthConnectManager?.insertRecord(record, this)
    }

    @UsedByGodot
    fun insert_records(records: Array<Any>) {
        healthConnectManager?.insertRecords(records, this)
    }

    @UsedByGodot
    fun update_record(record: Dictionary) {
        healthConnectManager?.updateRecord(record, this)
    }

    @UsedByGodot
    fun read_records(config: Dictionary) {
        healthConnectManager?.readRecords(config, this)
    }

    @UsedByGodot
    fun read_record_by_id(record_id: String, record_type: String) {
        healthConnectManager?.readRecordById(record_id, record_type, this)
    }

    @UsedByGodot
    fun read_aggregate_data(config: Dictionary) {
        healthConnectManager?.readAggregateData(config, this)
    }

    @UsedByGodot
    fun delete_record(record_id: String, record_type: String) {
        healthConnectManager?.deleteRecord(record_id, record_type, this)
    }

    @UsedByGodot
    fun delete_records(config: Dictionary) {
        healthConnectManager?.deleteRecords(config, this)
    }

    @UsedByGodot
    fun get_supported_record_types(): Array<String> {
        return arrayOf(
            "STEPS", "DISTANCE", "ACTIVE_CALORIES_BURNED", "TOTAL_CALORIES_BURNED",
            "WEIGHT", "HEIGHT", "HEART_RATE", "HYDRATION", "EXERCISE_SESSION", "SLEEP_SESSION"
        )
    }

    @UsedByGodot
    fun convert_units(value: Float, from_unit: String, to_unit: String): Float {
        if (from_unit == "KG" && to_unit == "LBS") return value * 2.20462f
        if (from_unit == "LBS" && to_unit == "KG") return value / 2.20462f
        if (from_unit == "METERS" && to_unit == "MILES") return value / 1609.34f
        if (from_unit == "MILES" && to_unit == "METERS") return value * 1609.34f
        return value
    }

    fun emitPluginSignal(signalName: String, vararg args: Any) {
        emitSignal(signalName, *args)
    }

    override fun onMainActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onMainActivityResult(requestCode, resultCode, data)
        healthConnectManager?.onActivityResult(requestCode, resultCode, data, this)
    }
}