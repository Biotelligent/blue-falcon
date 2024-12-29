package dev.bluefalcon

import kotlinx.coroutines.flow.MutableStateFlow

/** @suppress */
expect class BluetoothService {
    val name: String?
    val characteristics: List<BluetoothCharacteristic>
    internal val characteristicsFlow: MutableStateFlow<List<BluetoothCharacteristic>>
}