package dev.bluefalcon

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBService

actual class BluetoothPeripheral actual constructor() {

    lateinit var bluetoothDevice: CBPeripheral
    // var rssiValue: Float? = null

    constructor(bluetoothDevice: CBPeripheral, rssiValue: Float?) : this() {
        this.name = bluetoothDevice.name ?: UnknownDeviceName
        this.uuid = bluetoothDevice.identifier.UUIDString
        this.rssi = rssiValue
        this.bluetoothDevice = bluetoothDevice
    }
    actual var name: String = UnknownDeviceName
    actual var rssi: Float? = null
    actual val services: List<BluetoothService> get() =
        bluetoothDevice.services?.map {
            BluetoothService(it as CBService)
        } ?: emptyList()
    actual var uuid: String = ""

    internal actual val servicesFlow = MutableStateFlow<List<BluetoothService>>(emptyList())

    private var isConnected: Boolean = false
    actual var connected: Boolean
        get() = isConnected
        set(value) {
            isConnected = value
        }

    private var waslastSeen = Clock.System.now()
    actual var lastSeen: Instant
        get() = waslastSeen
        set(value) {
            waslastSeen = value
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