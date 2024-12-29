package dev.bluefalcon

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

/** @suppress */
expect class BlueFalcon(context: ApplicationContext, serviceUUID: String?) {
    val scope: CoroutineScope

    val delegates: MutableSet<BlueFalconDelegate>
    var isScanning: Boolean

    internal val peripheralsSet: MutableStateFlow<Set<BluetoothPeripheral>>
    val peripherals: NativeFlow<Set<BluetoothPeripheral>>

    fun connect(bluetoothPeripheral: BluetoothPeripheral, autoConnect: Boolean = false)

    fun disconnect(bluetoothPeripheral: BluetoothPeripheral)

    fun isBluetoothPermitted(): Boolean

    fun isBluetoothEnabled(): Boolean

    fun scan(): Boolean

    fun startScan(serviceUuid: String? = null, deviceName: String? = null, throttleMillis: Long = 500L, removeInactiveAfterMillis: Long = 0L): Boolean

    fun clearScanResults()

    fun stopScanning()

    fun readCharacteristic(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic)

    fun notifyCharacteristic(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, notify: Boolean)

    fun indicateCharacteristic(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, indicate: Boolean)

    fun notifyAndIndicateCharacteristic(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, enable: Boolean)

    fun writeCharacteristic(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, value: String, writeType: Int?)

    fun writeCharacteristic(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, value: ByteArray, writeType: Int?)

    fun writeCharacteristicWithoutEncoding(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, value: ByteArray, writeType: Int?)

    fun readDescriptor(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, bluetoothCharacteristicDescriptor: BluetoothCharacteristicDescriptor)

    fun changeMTU(bluetoothPeripheral: BluetoothPeripheral, mtuSize: Int)
}