package io.github.mrsfantaus.godot.healthconnect

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.health.connect.client.HealthConnectClient

class VersionCompatibility(private val activity: Activity) {

    companion object {
        private const val TAG = "VersionCompatibility"
        private const val INTEGRATED_PACKAGE = "com.android.healthconnect.controller"
    }

    fun isHealthConnectAvailable(): Boolean {
        Log.i(TAG, "isHealthConnectAvailable() called")
        val sdkInt = android.os.Build.VERSION.SDK_INT
        Log.i(TAG, "Current SDK: $sdkInt")
        
        // On Android 14+, Health Connect is integrated and always 'available' as a system service.
        // We skip getSdkStatus() because it's causing hangs on some Android 14+ environments.
        return if (sdkInt >= 34) {
            Log.i(TAG, "Android 14+ detected, assuming Health Connect is available")
            true
        } else {
            // For older versions (which we officially removed but keeping logic safe)
            try {
                val status = HealthConnectClient.getSdkStatus(activity)
                status == HealthConnectClient.SDK_AVAILABLE
            } catch (e: Exception) {
                false
            }
        }
    }

    fun getVersionType(): String {
        return "integrated"
    }

    fun requiresInstallation(): Boolean {
        return false
    }

    fun getHealthConnectPackageName(): String {
        return INTEGRATED_PACKAGE
    }

    fun openHealthConnectSettings() {
        Log.i(TAG, "openHealthConnectSettings() called")
        try {
            // Android 14+ System Intent
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setPackage("com.google.android.apps.healthdata") // Legacy package name sometimes used for controller
            intent.action = "android.health.connect.action.HEALTH_CONNECT_SETTINGS"
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Standard intent failed, trying HealthConnectClient intent...")
            try {
                val sdkIntent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                activity.startActivity(sdkIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "All intents failed. Opening general settings as fallback.")
                try {
                    activity.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                } catch (e3: Exception) {}
            }
        }
    }
}
