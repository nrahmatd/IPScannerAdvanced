package tej.androidnetworktools.lib

import java.io.IOException
import java.net.InetAddress


class Ping(private val ipAddress: String, private val timeout: Int) : Runnable {
    private var ipAndDeviceHashMap: HashMap<String, Device>? = null

    fun setIpAndDeviceHashMap(ipAndDeviceHashMap: HashMap<String, Device>?) {
        this.ipAndDeviceHashMap = ipAndDeviceHashMap
    }

    override fun run() {
        try {
            val inetAddress = InetAddress.getByName(ipAddress)

            if (inetAddress.isReachable(timeout) && ipAndDeviceHashMap != null) {
                val device = Device()
                device.ipAddress = ipAddress
                device.hostname = inetAddress.hostName

                ipAndDeviceHashMap!![ipAddress] = device
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
