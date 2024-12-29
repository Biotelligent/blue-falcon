package dev.bluefalcon

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerScanOptionAllowDuplicatesKey
import platform.CoreBluetooth.CBCharacteristicWriteWithResponse
import platform.CoreBluetooth.CBCharacteristicWriteWithoutResponse
import platform.CoreBluetooth.CBManagerState
import platform.CoreBluetooth.CBManagerStatePoweredOff
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBManagerStateResetting
import platform.CoreBluetooth.CBManagerStateUnauthorized
import platform.CoreBluetooth.CBManagerStateUnknown
import platform.CoreBluetooth.CBManagerStateUnsupported
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

@OptIn(kotlinx.cinterop.BetaInteropApi::class)
actual class BlueFalcon actual constructor(
    private val context: ApplicationContext,
    private val serviceUUID: String?
) {
    actual val delegates: MutableSet<BlueFalconDelegate> = mutableSetOf()

    private val centralManager: CBCentralManager
    private val bluetoothPeripheralManager = BluetoothPeripheralManager(this)
    actual var isScanning: Boolean = false
    actual val scope = CoroutineScope(Dispatchers.Default)
    val scannedPeripheralList = ScannedPeripheralList()
    internal actual val peripheralsSet = scannedPeripheralList.peripherals
    actual val peripherals: NativeFlow<Set<BluetoothPeripheral>> = peripheralsSet.toNativeType(scope)

    init {
        centralManager = CBCentralManager(bluetoothPeripheralManager, null)
    }

    actual fun connect(bluetoothPeripheral: BluetoothPeripheral, autoConnect: Boolean) {
        // auto connect is ignored due to not needing it in iOS
        centralManager.connectPeripheral(bluetoothPeripheral.bluetoothDevice, null)
    }

    actual fun disconnect(bluetoothPeripheral: BluetoothPeripheral) {
        centralManager.cancelPeripheralConnection(bluetoothPeripheral.bluetoothDevice)
    }

    actual fun clearScanResults() {
        scannedPeripheralList.clear()
    }

    actual fun stopScanning() {
        isScanning = false
        scannedPeripheralList.stop()
        centralManager.stopScan()
    }

    actual fun isBluetoothEnabled(): Boolean {
        return when (centralManager.state) {
            CBManagerStateUnsupported, CBManagerStatePoweredOff -> false
            else -> true
        }
    }

    actual fun isBluetoothPermitted(): Boolean {
        return when (centralManager.state) {
            CBManagerStateUnauthorized -> false
            else -> true
        }
    }

    var storedServiceUuid: String? = null
    var storedDeviceName: String? = null
    var storedThrottleMillis: Long = 750L
    var storedRemoveInactiveAfterMillis: Long = 500L
    var wasScanRequested = false

    fun resumeScanning(state: CBManagerState) {
        if (wasScanRequested && !isScanning) {
            log("resuming scanning w previous settings")
            wasScanRequested = false
            tryStartScan(serviceUuid = storedServiceUuid, throttleMillis = storedThrottleMillis, removeInactiveAfterMillis = storedRemoveInactiveAfterMillis, state)
        }
    }

    actual fun startScan(serviceUuid: String?, deviceName: String?, throttleMillis: Long, removeInactiveAfterMillis: Long): Boolean {
        log("BT startScan request for serviceUuid $serviceUuid isScanning=$isScanning")
        storedServiceUuid = serviceUuid
        storedDeviceName = deviceName
        storedThrottleMillis = throttleMillis
        storedRemoveInactiveAfterMillis = removeInactiveAfterMillis
        wasScanRequested = true

        if (isScanning) {
            stopScanning()
        }
        clearScanResults()

        val isEnabled = isBluetoothEnabled()
        val isPermitted = isBluetoothPermitted()
        if (!isPermitted || !isEnabled) {
            log("BT startScan request isPermitted=$isPermitted isEnabled=$isEnabled")
            return false
        }

        log("BT startScan checking state")
        when (centralManager.state) {
            CBManagerStateUnsupported -> {
                log("Bluetooth is not supported on this device")
                return false
            }
            CBManagerStateUnauthorized -> {
                log("Bluetooth is not permitted")
                return false
            }
            CBManagerStatePoweredOff -> {
                log("Bluetooth is off")
                return false
            }
            CBManagerStateUnknown,
            CBManagerStateResetting,
            CBManagerStatePoweredOn -> {
                log("Bluetooth state is powered on ${centralManager.state} - assume that resume is required")
                tryStartScan(serviceUuid, throttleMillis, removeInactiveAfterMillis, centralManager.state)
            }
        }
        return isScanning
    }

    private fun tryStartScan(serviceUuid: String?, throttleMillis: Long, removeInactiveAfterMillis: Long, state: CBManagerState) {
        log("BT tryStartScan enter from state $state isscanning = $isScanning")
        scannedPeripheralList.start(throttleMillis = throttleMillis, removeDevicesAfterMillis = removeInactiveAfterMillis)
        val scanServiceUuid: String? = serviceUuid ?: serviceUUID
        try {
            if (scanServiceUuid != null) {
                val serviceCBUUID = CBUUID.UUIDWithString(scanServiceUuid)
                centralManager.scanForPeripheralsWithServices(listOf(serviceCBUUID), mapOf(CBCentralManagerScanOptionAllowDuplicatesKey to SCAN_ALLOW_DUPLICATES))
                isScanning = true
            } else {
                centralManager.scanForPeripheralsWithServices(null, mapOf(CBCentralManagerScanOptionAllowDuplicatesKey to SCAN_ALLOW_DUPLICATES))
            }
            isScanning = state == CBManagerStatePoweredOn
        } catch (e: Exception) {
            log("BT tryStartScan exception ${e.message}")
            isScanning = false
        }
        log("BT tryStartScan exit isscanning = $isScanning")
    }

    actual fun scan(): Boolean {
        return startScan(serviceUUID, null)
    }

    actual fun readCharacteristic(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic) {
        bluetoothPeripheral.bluetoothDevice.readValueForCharacteristic(bluetoothCharacteristic.characteristic)
    }

    actual fun notifyCharacteristic(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, notify: Boolean) {
        bluetoothPeripheral.bluetoothDevice.setNotifyValue(notify, bluetoothCharacteristic.characteristic)
    }

    actual fun indicateCharacteristic(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, indicate: Boolean) {
        notifyCharacteristic(bluetoothPeripheral, bluetoothCharacteristic, indicate)
    }

    actual fun notifyAndIndicateCharacteristic(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, enable: Boolean) {
        notifyCharacteristic(bluetoothPeripheral, bluetoothCharacteristic, enable)
    }

    actual fun writeCharacteristic(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, value: String, writeType: Int?) {
        sharedWriteCharacteristic(
            bluetoothPeripheral,
            bluetoothCharacteristic,
            NSString.create(string = value),
            writeType
        )
    }

    actual fun writeCharacteristic(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, value: ByteArray, writeType: Int?) {
        sharedWriteCharacteristic(
            bluetoothPeripheral,
            bluetoothCharacteristic,
            NSString.create(string = value.decodeToString()),
            writeType
        )
    }

    actual fun writeCharacteristicWithoutEncoding(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, value: ByteArray, writeType: Int?) {
        sharedWriteCharacteristic(
            bluetoothPeripheral,
            bluetoothCharacteristic,
            value.toData(),
            writeType
        )
    }

    private fun sharedWriteCharacteristic(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, value: NSString, writeType: Int?) {
        value.dataUsingEncoding(NSUTF8StringEncoding)?.let {
            sharedWriteCharacteristic(
                bluetoothPeripheral,
                bluetoothCharacteristic,
                it,
                writeType
            )
        }
    }

    private fun sharedWriteCharacteristic(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, value: NSData, writeType: Int?) {
        bluetoothPeripheral.bluetoothDevice.writeValue(
            value,
            bluetoothCharacteristic.characteristic,
            when (writeType) {
                1 -> CBCharacteristicWriteWithoutResponse
                else -> CBCharacteristicWriteWithResponse
            }
        )
    }

    actual fun readDescriptor(bluetoothPeripheral: BluetoothPeripheral, bluetoothCharacteristic: BluetoothCharacteristic, bluetoothCharacteristicDescriptor: BluetoothCharacteristicDescriptor) {
        bluetoothPeripheral.bluetoothDevice.discoverDescriptorsForCharacteristic(bluetoothCharacteristic.characteristic)
    }

    actual fun changeMTU(bluetoothPeripheral: BluetoothPeripheral, mtuSize: Int) {
        println("Change MTU size called but not needed.")
        delegates.forEach {
            it.didUpdateMTU(bluetoothPeripheral)
        }
    }
}