package tej.androidnetworktools.lib.scanner

import tej.androidnetworktools.lib.Device

interface OnNetworkScanListener {
    fun onComplete(devices: List<Device>)
    fun onFailed()
}
