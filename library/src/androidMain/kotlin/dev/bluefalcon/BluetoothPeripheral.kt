package dev.bluefalcon

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

actual class BluetoothPeripheral actual constructor() {

    lateinit var bluetoothDevice: BluetoothDevice
    constructor(bluetoothDevice: BluetoothDevice) : this() {
        this.name = bluetoothDevice.name ?: UnknownDeviceName
        this.uuid = bluetoothDevice.address
        this.bluetoothDevice = bluetoothDevice
    }

    actual var name: String = UnknownDeviceName
    actual val services: List<BluetoothService>
        get() = servicesFlow.value
    actual var uuid: String = ""

    actual var rssi: Float? = null

    internal actual val servicesFlow = MutableStateFlow<List<BluetoothService>>(emptyList())

    private var isConnected: Boolean = false
    actual var connected: Boolean
        get() = isConnected
        set(value) {
            isConnected = value
        }

    private var wasLastSeen = Clock.System.now()
    actual var lastSeen: Instant
        get() = wasLastSeen
        set(value) {
            wasLastSeen = value
        }
    actual var firstSeen: Instant = Clock.System.now()

    private var isInsideThresholdStart: Instant = INSTANT_UNSET
    actual var insideThresholdStart: Instant
        get() = if (this.isInsideThreshold) isInsideThresholdStart else INSTANT_UNSET
        set(value) {
            isInsideThresholdStart = value
        }

    private var isOutsideThresholdStart: Instant = INSTANT_UNSET
    actual var outsideThresholdStart: Instant
        get() = if (!this.isInsideThreshold) isOutsideThresholdStart else INSTANT_UNSET
        set(value) {
            isOutsideThresholdStart = value
        }

    actual override fun toString(): String {
        return asString()
    }
}