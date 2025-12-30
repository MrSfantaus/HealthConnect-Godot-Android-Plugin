package io.github.mrsfantaus.godot.healthconnect

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.records.*
import androidx.health.connect.client.response.*
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.aggregate.AggregateMetric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.godotengine.godot.Dictionary
import org.godotengine.godot.Godot
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.Period
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.reflect.KClass

class HealthConnectManager(private val activity: Activity, private val godot: Godot) {

    companion object {
        private const val TAG = "HealthConnectManager"
        
        private val TYPE_MAP: Map<String, KClass<out Record>> = mapOf(
            "STEPS" to StepsRecord::class,
            "WEIGHT" to WeightRecord::class,
            "HEIGHT" to HeightRecord::class,
            "HEART_RATE" to HeartRateRecord::class,
            "DISTANCE" to DistanceRecord::class,
            "ACTIVE_CALORIES_BURNED" to ActiveCaloriesBurnedRecord::class,
            "TOTAL_CALORIES_BURNED" to TotalCaloriesBurnedRecord::class,
            "HYDRATION" to HydrationRecord::class,
            "EXERCISE_SESSION" to ExerciseSessionRecord::class,
            "SLEEP_SESSION" to SleepSessionRecord::class
        )

        private val METRIC_MAP = mapOf(
            "STEPS_TOTAL" to StepsRecord.COUNT_TOTAL,
            "DISTANCE_TOTAL" to DistanceRecord.DISTANCE_TOTAL,
            "ACTIVE_CALORIES_BURNED_TOTAL" to ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
            "TOTAL_CALORIES_BURNED_TOTAL" to TotalCaloriesBurnedRecord.ENERGY_TOTAL,
            "WEIGHT_AVG" to WeightRecord.WEIGHT_AVG,
            "WEIGHT_MIN" to WeightRecord.WEIGHT_MIN,
            "WEIGHT_MAX" to WeightRecord.WEIGHT_MAX,
            "HEART_RATE_AVG" to HeartRateRecord.BPM_AVG,
            "HEART_RATE_MIN" to HeartRateRecord.BPM_MIN,
            "HEART_RATE_MAX" to HeartRateRecord.BPM_MAX,
            "HYDRATION_TOTAL" to HydrationRecord.VOLUME_TOTAL
        )
    }

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(activity) }
    private val permissionHandler = PermissionHandler(activity, healthConnectClient, godot)
    private val dataTypeMapper = DataTypeMapper()
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        Log.i(TAG, "HealthConnectManager initialized")
    }

    fun requestPermissions(permissionsDict: Dictionary) {
        Log.i(TAG, "requestPermissions called with: $permissionsDict")
        permissionHandler.requestPermissions(permissionsDict)
    }

    suspend fun checkPermissionsSync(permissionsDict: Dictionary): Dictionary {
        return permissionHandler.checkPermissions(permissionsDict)
    }

    suspend fun getGrantedPermissionsSync(): Array<String> {
        return permissionHandler.getGrantedPermissions()
    }

    fun insertRecord(recordDict: Dictionary, plugin: GodotHealthConnect) {
        scope.launch {
            try {
                val record = dataTypeMapper.mapToRecord(recordDict)
                if (record != null) {
                    val response = healthConnectClient.insertRecords(listOf(record))
                    val recordId = response.recordIdsList.firstOrNull()
                    if (recordId != null) {
                        plugin.emitPluginSignal("record_inserted", recordId)
                    } else {
                        plugin.emitPluginSignal("error_occurred", "ERROR_WRITE_FAILED", "No record ID returned")
                    }
                } else {
                    plugin.emitPluginSignal("error_occurred", "ERROR_INVALID_DATA", "Could not map dictionary to record")
                }
            } catch (e: Exception) {
                plugin.emitPluginSignal("error_occurred", "ERROR_WRITE_FAILED", e.message ?: "Unknown error")
            }
        }
    }

    fun insertRecords(recordsArray: Array<Any>, plugin: GodotHealthConnect) {
        scope.launch {
            try {
                val records = recordsArray.mapNotNull {
                    if (it is Dictionary) dataTypeMapper.mapToRecord(it) else null
                }
                if (records.isNotEmpty()) {
                    val response = healthConnectClient.insertRecords(records)
                    val signalDict = Dictionary()
                    signalDict["record_ids"] = response.recordIdsList.toTypedArray()
                    plugin.emitPluginSignal("records_inserted", signalDict)
                } else {
                    plugin.emitPluginSignal("error_occurred", "ERROR_INVALID_DATA", "No valid records to insert")
                }
            } catch (e: Exception) {
                plugin.emitPluginSignal("error_occurred", "ERROR_WRITE_FAILED", e.message ?: "Unknown error")
            }
        }
    }

    fun readRecords(config: Dictionary, plugin: GodotHealthConnect) {
        scope.launch {
            try {
                val typeStr = config["record_type"] as? String ?: return@launch
                val recordClass = TYPE_MAP[typeStr] ?: return@launch
                
                val startTime = Instant.parse(config["start_time"] as String)
                val endTime = Instant.parse(config["end_time"] as String)
                
                val request = ReadRecordsRequest(
                    recordType = recordClass,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
                
                val response = healthConnectClient.readRecords(request)
                val resultList = response.records.map { dataTypeMapper.mapFromRecord(it) }
                
                Log.i(TAG, "readRecords: Found ${resultList.size} records")
                
                val jsonArray = JSONArray()
                resultList.forEach { dict ->
                    val jsonObj = JSONObject()
                    dict.forEach { (key, value) ->
                        jsonObj.put(key.toString(), value)
                    }
                    jsonArray.put(jsonObj)
                }
                plugin.emitPluginSignal("records_read", jsonArray.toString())
            } catch (e: Exception) {
                Log.e(TAG, "readRecords error: ${e.message}", e)
                plugin.emitPluginSignal("error_occurred", "ERROR_READ_FAILED", e.message ?: "Unknown error")
            }
        }
    }

    fun readRecordById(recordId: String, typeStr: String, plugin: GodotHealthConnect) {
        scope.launch {
            try {
                val recordClass = TYPE_MAP[typeStr] ?: return@launch
                val response = healthConnectClient.readRecord(recordClass, recordId)
                val result = dataTypeMapper.mapFromRecord(response.record)
                Log.i(TAG, "readRecordById: Success")
                
                val jsonObj = JSONObject()
                result.forEach { (key, value) ->
                    jsonObj.put(key.toString(), value)
                }
                plugin.emitPluginSignal("record_read", jsonObj.toString())
            } catch (e: Exception) {
                Log.e(TAG, "readRecordById error: ${e.message}", e)
                plugin.emitPluginSignal("error_occurred", "ERROR_RECORD_NOT_FOUND", e.message ?: "Unknown error")
            }
        }
    }

    fun readAggregateData(config: Dictionary, plugin: GodotHealthConnect) {
        scope.launch {
            try {
                val startTime = Instant.parse(config["start_time"] as String)
                val endTime = Instant.parse(config["end_time"] as String)
                val typeStr = config["record_type"] as? String ?: return@launch
                val aggType = config["aggregation_type"] as? String ?: "TOTAL"
                val slicer = config["time_range_slicer"] as? String
                
                val metricKey = "${typeStr}_${aggType}"
                val metric = METRIC_MAP[metricKey] ?: return@launch

                val jsonArray = JSONArray()

                if (slicer != null) {
                    val period = when (slicer) {
                        "DAY" -> Period.ofDays(1)
                        "WEEK" -> Period.ofWeeks(1)
                        "MONTH" -> Period.ofMonths(1)
                        else -> Period.ofDays(1)
                    }
                    
                    val zoneId = ZoneId.systemDefault()
                    val startLDT = LocalDateTime.ofInstant(startTime, zoneId)
                    val endLDT = LocalDateTime.ofInstant(endTime, zoneId)

                    val request = AggregateGroupByPeriodRequest(
                        metrics = setOf(metric),
                        timeRangeFilter = TimeRangeFilter.between(startLDT, endLDT),
                        timeRangeSlicer = period
                    )
                    
                    val response = healthConnectClient.aggregateGroupByPeriod(request)
                    response.forEach { bucket ->
                        val jsonObj = JSONObject()
                        jsonObj.put("start_time", bucket.startTime.toString())
                        jsonObj.put("end_time", bucket.endTime.toString())
                        val value = bucket.result[metric]
                        if (value != null) {
                            jsonObj.put("value", value)
                        }
                        jsonArray.put(jsonObj)
                    }
                } else {
                    val request = AggregateRequest(
                        metrics = setOf(metric),
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )

                    val response = healthConnectClient.aggregate(request)
                    val jsonObj = JSONObject()
                    val value = response[metric]
                    if (value != null) {
                        jsonObj.put("value", value)
                    }
                    jsonArray.put(jsonObj)
                }
                
                Log.i(TAG, "readAggregateData: Emitting ${jsonArray.length()} buckets")
                plugin.emitPluginSignal("aggregate_data_read", jsonArray.toString())
            } catch (e: Exception) {
                Log.e(TAG, "readAggregateData error: ${e.message}", e)
                plugin.emitPluginSignal("error_occurred", "ERROR_READ_FAILED", e.message ?: "Unknown error")
            }
        }
    }

    fun deleteRecord(recordId: String, typeStr: String, plugin: GodotHealthConnect) {
        scope.launch {
            try {
                val recordClass = TYPE_MAP[typeStr] ?: return@launch
                healthConnectClient.deleteRecords(
                    recordType = recordClass,
                    recordIdsList = listOf(recordId),
                    clientRecordIdsList = emptyList()
                )
                plugin.emitPluginSignal("record_deleted", true)
            } catch (e: Exception) {
                plugin.emitPluginSignal("record_deleted", false)
                plugin.emitPluginSignal("error_occurred", "ERROR_DELETE_FAILED", e.message ?: "Unknown error")
            }
        }
    }

    fun deleteRecords(config: Dictionary, plugin: GodotHealthConnect) {
        scope.launch {
            try {
                val typeStr = config["record_type"] as? String ?: return@launch
                val recordClass = TYPE_MAP[typeStr] ?: return@launch
                
                if (config.containsKey("record_ids")) {
                    val ids = (config["record_ids"] as Array<*>).filterIsInstance<String>()
                    healthConnectClient.deleteRecords(
                        recordType = recordClass,
                        recordIdsList = ids,
                        clientRecordIdsList = emptyList()
                    )
                    plugin.emitPluginSignal("records_deleted", ids.size)
                } else {
                    val startTime = Instant.parse(config["start_time"] as String)
                    val endTime = Instant.parse(config["end_time"] as String)
                    healthConnectClient.deleteRecords(
                        recordType = recordClass,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                    plugin.emitPluginSignal("records_deleted", -1) // Count not available for range delete
                }
            } catch (e: Exception) {
                plugin.emitPluginSignal("error_occurred", "ERROR_DELETE_FAILED", e.message ?: "Unknown error")
            }
        }
    }

    fun updateRecord(recordDict: Dictionary, plugin: GodotHealthConnect) {
        scope.launch {
            try {
                val record = dataTypeMapper.mapToRecord(recordDict)
                if (record != null && record.metadata.id.isNotEmpty()) {
                    healthConnectClient.updateRecords(listOf(record))
                    plugin.emitPluginSignal("record_updated", true)
                } else {
                    plugin.emitPluginSignal("record_updated", false)
                    plugin.emitPluginSignal("error_occurred", "ERROR_INVALID_DATA", "Record ID is required for update")
                }
            } catch (e: Exception) {
                plugin.emitPluginSignal("record_updated", false)
                plugin.emitPluginSignal("error_occurred", "ERROR_WRITE_FAILED", e.message ?: "Unknown error")
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, plugin: GodotHealthConnect) {
        Log.i(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        if (requestCode == PermissionHandler.PERMISSIONS_REQUEST_CODE) {
            scope.launch {
                val permissions = healthConnectClient.permissionController.getGrantedPermissions()
                Log.i(TAG, "Permissions updated: $permissions")
                val result = Dictionary()
                permissions.forEach { p ->
                    result[p] = true
                }
                plugin.emitPluginSignal("permissions_result", result)
            }
        }
    }
}