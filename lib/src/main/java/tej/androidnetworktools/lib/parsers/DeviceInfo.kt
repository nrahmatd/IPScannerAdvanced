package tej.androidnetworktools.lib.parsers

import android.content.Context
import android.os.Build
import org.json.JSONException
import org.json.JSONObject
import tej.androidnetworktools.lib.Device
import tej.androidnetworktools.lib.scanner.NetworkScanner
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException
import java.util.Locale


object DeviceInfo {
    var UNKNOWN: String = "-"

    @JvmStatic
    fun parse(ipAndDeviceHashMap: HashMap<String, Device>) {
        if (NetworkScanner.isShowMacAddress()) {
            setMacAddress(ipAndDeviceHashMap)
        }

        if (NetworkScanner.isShowVendorInfo()) {
            var device: Device?
            var vendorName: String

            for (key in ipAndDeviceHashMap.keys) {
                device = ipAndDeviceHashMap[key]

                if (device != null) {
                    vendorName = getVendorName(device.macAddress)
                    device.vendorName = vendorName
                }
            }
        }

        val currentDevice = ipAndDeviceHashMap[NetworkScanner.getCurrentIPAddress()]

        if (currentDevice != null) {
            currentDevice.hostname = Build.MODEL
            currentDevice.vendorName = Build.MANUFACTURER
        }
    }

    fun setMacAddress(ipAndDeviceHashMap: HashMap<String, Device>) {
        val runtime = Runtime.getRuntime()
        try {
            val process = runtime.exec("ip n")
            process.waitFor()

            val code = process.exitValue()
            if (code != 0) {
                return
            }

            val inputStream = process.inputStream
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            var line: String

            var cols: Array<String?>
            var ipAddress: String?
            var macAddress: String?

            var device: Device?

            while ((bufferedReader.readLine().also { line = it }) != null) {
                cols = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                if (cols.size > 4) {
                    ipAddress = cols[0]
                    macAddress = cols[4]

                    // Insert mac address
                    device = ipAndDeviceHashMap[ipAddress]
                    if (device != null) {
                        device.macAddress = macAddress!!
                    }
                }
            }

            // Set mac address for current device
            val currentIPAddress = NetworkScanner.getCurrentIPAddress()
            device = ipAndDeviceHashMap[currentIPAddress]

            if (device != null) {
                device.macAddress = getCurrentDeviceMacAddress(currentIPAddress)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun getCurrentDeviceMacAddress(ipAddress: String?): String {
        try {
            val localIP = InetAddress.getByName(ipAddress)
            val networkInterface = NetworkInterface.getByInetAddress(localIP)
                ?: return UNKNOWN

            val hardwareAddress = networkInterface.hardwareAddress ?: return UNKNOWN

            val stringBuilder = StringBuilder(18)
            for (b in hardwareAddress) {
                if (stringBuilder.length > 0) {
                    stringBuilder.append(":")
                }

                stringBuilder.append(String.format("%02x", b))
            }

            return stringBuilder.toString()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        } catch (e: SocketException) {
            e.printStackTrace()
        }

        return UNKNOWN
    }

    private fun vendorLookup(macAddress: String): JSONObject? {
        try {
            val jsonArray = NetworkScanner.getVendorsJsonArray()

            var macAddressPrefix: String

            for (i in 0 until jsonArray!!.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                macAddressPrefix = jsonObject.getString("m")

                if (macAddress.lowercase(Locale.getDefault()).startsWith(
                        macAddressPrefix.lowercase(
                            Locale.getDefault()
                        )
                    )
                ) {
                    return jsonObject
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    fun getVendorName(macAddress: String?): String {
        if (macAddress == null) {
            return UNKNOWN
        }

        val jsonObject = vendorLookup(macAddress)
        if (jsonObject != null) {
            try {
                return jsonObject.getString("n")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        return UNKNOWN
    }

    fun getVendorInfo(context: Context, macAddress: String): JSONObject? {

        return vendorLookup(macAddress)
    }
}
