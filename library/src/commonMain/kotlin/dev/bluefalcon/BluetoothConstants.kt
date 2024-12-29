package dev.bluefalcon

import kotlinx.datetime.Instant

// Change to the method that items are shown in the list. If RETAINED, they are "bumped" but not deleted.
internal const val SCAN_RETAIN_REMOVED = true
internal const val SCAN_ALLOW_DUPLICATES = true // iOS: When false, the RSSI etc do not get live updates; but devices are discovered first time ok.
internal const val SCAN_EXCLUDE_UNKNOWN = true // Skip unnamed (n/a) devices until we can see their advertised name. This is fine for our beacons, may not work for other scanned devices.

// Values below -90 dBm are considered an unusable connection range.
// The minimum detectable RSSI can sometimes go as low as -110 dBm or even -120 dBm for some Bluetooth chipsets, but at these levels, the signal is essentially non-existent for practical purposes.
const val RSSI_UNSET = -150

const val DEFAULT_THRESHOLD_RSSI = -80
const val DEFAULT_THRESHOLD_DETECT_MS = 2500
const val DEFAULT_THRESHOLD_REMOVE_MS = 5000
const val DEFAULT_THROTTLE_MS = 750L

// FIXME change to dataclass
internal val INSTANT_UNSET = Instant.DISTANT_FUTURE
internal val scanThresholdRssi: Int get() = DEFAULT_THRESHOLD_RSSI //Services.configuration.scanThresholdRssi
internal val scanThresholdMillis: Int get() = DEFAULT_THRESHOLD_DETECT_MS //Services.configuration.scanThresholdMillis
internal val scanRemoveMillis: Int get() = DEFAULT_THRESHOLD_REMOVE_MS //Services.configuration.scanRemoveMillis