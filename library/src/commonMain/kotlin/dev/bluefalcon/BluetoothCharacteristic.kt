package dev.bluefalcon

import kotlinx.coroutines.flow.MutableStateFlow

/** @suppress */
expect class BluetoothCharacteristic {
    val name: String?
    val value: ByteArray?
    val descriptors: List<BluetoothCharacteristicDescriptor>
    internal val descriptorsFlow: MutableStateFlow<List<BluetoothCharacteristicDescriptor>>
}

expect class BluetoothCharacteristicDescriptor