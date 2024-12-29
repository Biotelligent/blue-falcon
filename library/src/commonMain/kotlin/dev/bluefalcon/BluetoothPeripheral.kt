package dev.bluefalcon

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.max

/** @suppress */
enum class PeripheralThreshold {
    InsideThreshold,
    OutsideThreshold,
    OverThreshold,
    OutOfThreshold
}

/** @suppress */
expect class BluetoothPeripheral() {
    var uuid: String
    var name: String
    var rssi: Float?
    var connected: Boolean
    var firstSeen: Instant
    var lastSeen: Instant
    var insideThresholdStart: Instant
    var outsideThresholdStart: Instant

    val services: List<BluetoothService>
    internal val servicesFlow: MutableStateFlow<List<BluetoothService>>

    override fun toString(): String
}

/**
 * Management functions for handling changes (such as name or rssi) which should be propagated to the UI
 */
val UnknownDeviceName: String = ""
val allowedUnseenMillis: Long get() = max(scanThresholdMillis, scanRemoveMillis).toLong()
val BluetoothPeripheral.displayName: String get() = if (this.name.isNotBlank()) this.name else this.uuid
val BluetoothPeripheral.rssiInt: Int get() = (this.rssi ?: 0f).toInt()
val BluetoothPeripheral.lastSeenMillis: Long get() = (Clock.System.now() - this.lastSeen).inWholeMilliseconds
val BluetoothPeripheral.isInsideThreshold: Boolean get() = rssiInt >= scanThresholdRssi
val BluetoothPeripheral.insideThresholdMillis: Long get() = if (isInsideThreshold &&
    (lastSeenMillis < allowedUnseenMillis) &&
    insideThresholdStart != INSTANT_UNSET
) {
    (Clock.System.now() - this.insideThresholdStart).inWholeMilliseconds
} else {
    0L
}
val BluetoothPeripheral.isOverThreshold: Boolean get() = if (isInsideThreshold) (insideThresholdMillis >= scanThresholdMillis) else false
val BluetoothPeripheral.outsideThresholdMillis: Long get() {
    if (lastSeenMillis > scanRemoveMillis) return lastSeenMillis
    return if (outsideThresholdStart != INSTANT_UNSET && !isInsideThreshold) (Clock.System.now() - this.outsideThresholdStart).inWholeMilliseconds else 0L
}
val BluetoothPeripheral.isOutOfThreshold: Boolean get() = (lastSeenMillis > scanRemoveMillis) || (outsideThresholdMillis > scanRemoveMillis)
val BluetoothPeripheral.threshold: PeripheralThreshold get() {
    if (isOutOfThreshold) return PeripheralThreshold.OutOfThreshold
    if (isOverThreshold) return PeripheralThreshold.OverThreshold
    if (isInsideThreshold) return PeripheralThreshold.InsideThreshold
    return PeripheralThreshold.OutsideThreshold
}

/**
 * May need to be called before isChanged
 */
fun BluetoothPeripheral.updateThreshold() {
    if (isInsideThreshold) {
        outsideThresholdStart = INSTANT_UNSET
        if (insideThresholdStart == INSTANT_UNSET) {
            insideThresholdStart = Clock.System.now()
        }
    } else {
        insideThresholdStart = INSTANT_UNSET
        if (outsideThresholdStart == INSTANT_UNSET) {
            outsideThresholdStart = Clock.System.now()
        }
    }
}

/**
 * Specific display for the BLE scanner - less than 10 seconds, display as ms, otherwise as seconds or minutes
 */
private fun durationString(millis: Long): String {
    if (millis == 0L) return millis.toString()
    if (millis < 10001L) return "${millis}ms"
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        seconds > 0 -> "${seconds}s"
        else -> "${millis}ms"
    }
}

fun BluetoothPeripheral.asString(): String = "$displayName ${durationString(lastSeenMillis).padStart(8, ' ')}"
fun BluetoothPeripheral.asThresholdString(): String {
    val plusMinus = if (isOverThreshold) {
        "++"
    } else if (isInsideThreshold) {
        "+"
    } else {
        "-"
    }
    val insideDuration = if (isInsideThreshold) durationString(insideThresholdMillis) else durationString(insideThresholdMillis)
    return "$plusMinus$insideDuration ${rssiInt}dBm"
}

fun BluetoothPeripheral.asIndicationString() = when (this.threshold) {
    PeripheralThreshold.InsideThreshold -> "^"
    PeripheralThreshold.OutsideThreshold -> "v"
    PeripheralThreshold.OverThreshold -> "IN"
    PeripheralThreshold.OutOfThreshold -> "OUT"
}

fun BluetoothPeripheral.isEqual(other: BluetoothPeripheral): Boolean = this.uuid == other.uuid || (this.name != UnknownDeviceName && this.name == other.name)

private fun BluetoothPeripheral.equalsState(other: BluetoothPeripheral): Boolean = isEqual(other) &&
    this.rssiInt == other.rssiInt &&
    this.name == other.name &&
    this.connected == other.connected

fun BluetoothPeripheral.isChanged(other: BluetoothPeripheral): Boolean = !this.equalsState(other)

/**
 * Copy non-transient properties
 */
fun BluetoothPeripheral.copyFrom(source: BluetoothPeripheral) {
    this.firstSeen = source.firstSeen
    this.insideThresholdStart = source.insideThresholdStart
    this.outsideThresholdStart = source.outsideThresholdStart
}

fun BluetoothPeripheral.dumpString() {
    println("Name: $name")
    println("UUID: $uuid")
    println("RSSI: $rssi")
}