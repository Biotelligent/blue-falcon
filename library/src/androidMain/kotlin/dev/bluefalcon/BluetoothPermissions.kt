package dev.bluefalcon

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.core.content.PermissionChecker

/**
 * Permission helper class for Bluetooth SDK
 */
class BluetoothPermissions(private val context: Context) {
    fun isBluetoothEnabled(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter?.isEnabled == true
    }

    fun isBluetoothPermitted(): Boolean {
        val permissions = java.util.ArrayList<String>()

        if (Build.VERSION.SDK_INT <= 30) {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        if (Build.VERSION.SDK_INT > 30) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        return hasPermissions(*permissions.toTypedArray())
    }

    fun isLocationRequired(): Boolean {
        return Build.VERSION.SDK_INT <= 30
    }

    fun isLocationPermitted(): Boolean {
        if (isLocationRequired()) {
            val permissions = java.util.ArrayList<String>()
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            return hasPermissions(*permissions.toTypedArray())
        } else {
            return true
        }
    }

    private fun hasPermissions(vararg permissions: String): Boolean = permissions.all {
        PermissionChecker.checkSelfPermission(
            context,
            it
        ) == PermissionChecker.PERMISSION_GRANTED
    }
}