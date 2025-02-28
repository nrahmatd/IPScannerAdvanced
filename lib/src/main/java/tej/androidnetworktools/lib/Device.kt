package tej.androidnetworktools.lib

import tej.androidnetworktools.lib.parsers.DeviceInfo

public data class Device(
    var hostname: String? = null,
    var ipAddress: String? = null,
    var macAddress: String = DeviceInfo.UNKNOWN,
    var vendorName: String = DeviceInfo.UNKNOWN
)