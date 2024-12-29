package dev.bluefalcon

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Retains a list of the scanned peripherals with updates throttled on a timer basis
 * (To remove items no longer in the scan, the timer thread must keep inspecting the list while there is more than one item in case the sole remaining item has stopped advertising)
 */
/** @suppress */
class ScannedPeripheralList {
    internal var scannedPeripherals: Set<BluetoothPeripheral> = emptySet()
    internal val peripherals = MutableStateFlow(scannedPeripherals)
    private var isBatched: Boolean = false
    private var removeAfterMillis: Long = 0
    private var isChanged = false

    // upsert
    // reset
    // remove after x
    // clearscan
    // emit
    fun start(throttleMillis: Long = 500L, removeDevicesAfterMillis: Long = 5000L) {
        stop()
        clear()
        isBatched = throttleMillis > 0
        removeAfterMillis = removeDevicesAfterMillis
        setRepeatingTimer(throttleMillis)
    }

    fun stop() {
        cancelTimer()
    }

    private fun copyOfScannedPeripherals(): MutableSet<BluetoothPeripheral> {
        return scannedPeripherals.toMutableSet()
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun updateScannedPeripherals(source: Set<BluetoothPeripheral>) {
        // synchronized(scannedPeripherals) {
        scannedPeripherals = source.toSet()
        // }
    }

    /**
     * Add or update any items in the list
     * Returns true if this was a new item (so delegates are also informed)
     */
    fun upsert(bluetoothPeripheral: BluetoothPeripheral): Boolean {
        var isSetChanged = false
        var isAdded = false
        val tmpPeripherals = copyOfScannedPeripherals()
        val index = tmpPeripherals.indexOfFirst { it.isEqual(bluetoothPeripheral) }
        if (index > -1) {
            val peripheral = tmpPeripherals.elementAt(index)
            peripheral.updateThreshold()
            if (peripheral.isChanged(bluetoothPeripheral)) {
                bluetoothPeripheral.copyFrom(peripheral)
                tmpPeripherals.remove(peripheral)
                tmpPeripherals.add(bluetoothPeripheral)
                isSetChanged = true
            }
        } else if (bluetoothPeripheral.name.isNotBlank() || !SCAN_EXCLUDE_UNKNOWN) {
            bluetoothPeripheral.updateThreshold()
            tmpPeripherals.add(bluetoothPeripheral)
            isSetChanged = true
            isAdded = true
        }
        if (isSetChanged) {
            // log("upsert ${bluetoothPeripheral.name} changed=$isSetChanged added=$isAdded")
            updateScannedPeripherals(tmpPeripherals)
        }

        isChanged = isChanged || isSetChanged
        if (!isBatched) {
            emitChanges()
        }
        return isAdded
    }

    /**
     * Remove any records which haven't been seen for the last x milliseconds
     * (Active devices normally post an rssi or other change)
     */
    private fun removeInactive() {
        if (removeAfterMillis > 0) {
            var isSetChanged = false
            val removedPeripherals = mutableSetOf<BluetoothPeripheral>()
            val tmpPeripherals = copyOfScannedPeripherals()
            val iterator = tmpPeripherals.iterator()
            while (iterator.hasNext()) {
                val peripheral = iterator.next()
                if (peripheral.lastSeenMillis > removeAfterMillis) {
                    // log("BT Removing inactive ${peripheral.name} ${peripheral.rssiInt}")
                    if (SCAN_RETAIN_REMOVED) {
                        removedPeripherals.add(clonePeripheral(peripheral))
                    }
                    iterator.remove()
                    isSetChanged = true
                }
            }
            if (isSetChanged) {
                if (SCAN_RETAIN_REMOVED) {
                    tmpPeripherals.addAll(removedPeripherals)
                }
                updateScannedPeripherals(tmpPeripherals)
                isChanged = isChanged || isSetChanged
            }
        }
    }

    /**
     * The peripheral must be a cloned copy for the emitted stateflow to register an item change
     */
    private fun clonePeripheral(peripheral: BluetoothPeripheral): BluetoothPeripheral {
        val clone = BluetoothPeripheral()
        clone.name = peripheral.name
        clone.uuid = peripheral.uuid
        clone.copyFrom(peripheral)
        clone.lastSeen = peripheral.lastSeen
        clone.rssi = RSSI_UNSET.toFloat()
        clone.updateThreshold()
        return clone
    }

    fun clear() {
        updateScannedPeripherals(emptySet())
        emit()
    }

    fun emitChanges() {
        removeInactive()
        if (isChanged) emit()
    }

    var logNotified = 0
    fun emit() {
        isChanged = false // ATOMIC
        peripherals.value = scannedPeripherals
        peripherals.tryEmit(scannedPeripherals)
        if (logNotified++ % 10 == 0) log("emitting ${peripherals.value.size} items")
    }

    var timerScope: CoroutineScope? = null

    /**
     * Every x milliseconds, check if there are any new peripherals or significant changes to existing peripherals to update the scan result list
     */
    fun setRepeatingTimer(repeatIntervalMillis: Long) {
        cancelTimer()
        if (isBatched) {
            try {
                timerScope = CoroutineScope(Dispatchers.Default) // FIXME: scope
                timerScope?.launch {
                    while (isActive) { // log("setRepeatingTimer delaying $repeatIntervalMillis ms")
                        delay(repeatIntervalMillis)
                        emitChanges()
                    }
                }
            } catch (e: Exception) {
                log("setRepeatingTimer exception: ${e.message}")
            }
        }
    }

    fun cancelTimer() {
        timerScope?.let {
            if (it.coroutineContext.isActive) {
                log("cancel repeating timer")
                try {
                    it.cancel()
                } catch (e: CancellationException) {
                    log("cancelTimer coroutine CancellationException: ${e.message}")
                }
            }
        }
    }
}