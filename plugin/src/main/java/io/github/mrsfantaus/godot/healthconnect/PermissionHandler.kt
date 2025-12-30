package io.github.mrsfantaus.godot.healthconnect

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import org.godotengine.godot.Dictionary
import org.godotengine.godot.Godot
import kotlin.reflect.KClass

class PermissionHandler(
    private val activity: Activity,
    private val healthConnectClient: HealthConnectClient,
    private val godot: Godot
) {

    companion object {
        private const val TAG = "HC_Permissions"
        const val PERMISSIONS_REQUEST_CODE = 1001

        private val TYPE_MAP: Map<String, KClass<out Record>> = mapOf(
            "STEPS" to StepsRecord::class,
            "DISTANCE" to DistanceRecord::class,
            "ACTIVE_CALORIES_BURNED" to ActiveCaloriesBurnedRecord::class,
            "TOTAL_CALORIES_BURNED" to TotalCaloriesBurnedRecord::class,
            "EXERCISE_SESSION" to ExerciseSessionRecord::class,
            "SPEED" to SpeedRecord::class,
            "POWER" to PowerRecord::class,
            "FLOORS_CLIMBED" to FloorsClimbedRecord::class,
            "WEIGHT" to WeightRecord::class,
            "HEIGHT" to HeightRecord::class,
            "BODY_FAT" to BodyFatRecord::class,
            "BMR" to BasalMetabolicRateRecord::class,
            "LEAN_BODY_MASS" to LeanBodyMassRecord::class,
            "HEART_RATE" to HeartRateRecord::class,
            "BLOOD_PRESSURE" to BloodPressureRecord::class,
            "BLOOD_GLUCOSE" to BloodGlucoseRecord::class,
            "OXYGEN_SATURATION" to OxygenSaturationRecord::class,
            "BODY_TEMPERATURE" to BodyTemperatureRecord::class,
            "RESPIRATORY_RATE" to RespiratoryRateRecord::class,
            "RESTING_HEART_RATE" to RestingHeartRateRecord::class,
            "SLEEP_SESSION" to SleepSessionRecord::class,
            "HYDRATION" to HydrationRecord::class,
            "NUTRITION" to NutritionRecord::class,
            "MENSTRUATION" to MenstruationPeriodRecord::class,
            "OVULATION_TEST" to OvulationTestRecord::class,
            "CERVICAL_MUCUS" to CervicalMucusRecord::class
        )
    }

    fun getPermissionsFromDictArray(permissionsDict: Dictionary): Set<String> {
        val permissionsArray = permissionsDict["permissions"] as? Array<*> ?: return emptySet()
        val permissions = mutableSetOf<String>()
        for (item in permissionsArray) {
            if (item is Dictionary) {
                val type = item["type"] as? String ?: continue
                val access = item["access"] as? String ?: "READ"
                
                val recordClass = TYPE_MAP[type]
                if (recordClass != null) {
                    if (access == "READ") {
                        permissions.add(HealthPermission.getReadPermission(recordClass))
                    } else if (access == "WRITE") {
                        permissions.add(HealthPermission.getWritePermission(recordClass))
                    }
                }
            }
        }
        return permissions
    }

    suspend fun checkPermissions(permissionsDict: Dictionary): Dictionary {
        val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
        val permissionsArray = permissionsDict["permissions"] as? Array<*> ?: return Dictionary()
        
        val result = Dictionary()
        for (item in permissionsArray) {
            if (item is Dictionary) {
                val type = item["type"] as? String ?: continue
                val access = item["access"] as? String ?: "READ"
                val recordClass = TYPE_MAP[type] ?: continue
                
                val permission = if (access == "READ") {
                    HealthPermission.getReadPermission(recordClass)
                } else {
                    HealthPermission.getWritePermission(recordClass)
                }
                
                result["${type}_${access}"] = grantedPermissions.contains(permission)
            }
        }
        return result
    }

    suspend fun getGrantedPermissions(): Array<String> {
        return healthConnectClient.permissionController.getGrantedPermissions().toTypedArray()
    }

    fun requestPermissions(permissionsDict: Dictionary) {
        val permissions = getPermissionsFromDictArray(permissionsDict)
        Log.i(TAG, "requestPermissions() called on thread: ${Thread.currentThread().name}")
        Log.i(TAG, "Requesting permissions: $permissions")
        
        if (permissions.isEmpty()) {
            Log.w(TAG, "No permissions to request")
            return
        }

        activity.runOnUiThread {
            try {
                Log.i(TAG, "Launching Health Connect permissions UI via explicit Intent...")
                
                val intent = Intent("android.health.connect.action.REQUEST_HEALTH_PERMISSIONS")
                intent.putExtra("android.health.connect.extra.HEALTH_PERMISSIONS", permissions.toTypedArray())
                
                Log.i(TAG, "Intent: $intent")
                activity.startActivityForResult(intent, PERMISSIONS_REQUEST_CODE)
                Log.i(TAG, "startActivityForResult called successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Explicit intent failed: ${e.message}", e)
                try {
                    // Final fallback: just open settings
                    val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                    activity.startActivity(intent)
                } catch (e2: Exception) {
                    Log.e(TAG, "Everything failed: ${e2.message}")
                }
            }
        }
    }
}