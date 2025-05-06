package tej.androidnetworktools.lib.scanner

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONException
import tej.androidnetworktools.lib.Device
import tej.androidnetworktools.lib.NetworkConnection.isWifiConnected
import tej.androidnetworktools.lib.Ping
import tej.androidnetworktools.lib.parsers.DeviceInfo.parse
import tej.androidnetworktools.lib.parsers.Utils.parseIpAddress
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NetworkScanner private constructor(context: Context) {
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val ipAndDeviceHashMap = HashMap<String, Device>()

    private var ipPrefix: String? = null
    private var currentIPAddress: String? = null
    private var showVendorInfo = true
    private var showMacAddress = true
    private var taskRunning = false
    private var scanTimeout = 500
    private var reachableRescanCount = 3
    private var vendorsJson: JSONArray? = null

    init {
        initIPConfigs(context)
        initVendorsJson(context)
    }

    private fun initIPConfigs(context: Context) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddressLong = wifiManager.dhcpInfo.ipAddress
        val ipAddress = parseIpAddress(ipAddressLong.toLong())
        val currentIPAddressLong = wifiManager.connectionInfo.ipAddress
        currentIPAddress = parseIpAddress(currentIPAddressLong.toLong())

        ipAddress?.let {
            val lastDotIndex = it.lastIndexOf(".")
            ipPrefix = it.substring(0, lastDotIndex + 1)
        }
    }

    private fun initVendorsJson(context: Context) {
        try {
            context.assets.open("vendors.json").use { inputStream ->
                val json = inputStream.bufferedReader().use { it.readText() }
                vendorsJson = JSONArray(json)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun isTaskRunning(): Boolean = taskRunning

    fun setScanTimeout(timeout: Int) {
        scanTimeout = timeout
    }

    fun scanNetwork(onNetworkScanListener: OnNetworkScanListener) {
        if (taskRunning || !isWifiConnected(connectivityManager)) {
            onNetworkScanListener.onFailed()
            return
        }

        taskRunning = true
        ipAndDeviceHashMap.clear()

        Thread {
            val executorService = Executors.newFixedThreadPool(255 * reachableRescanCount)

            ipPrefix?.let { prefix ->
                for (i in 1..255) {
                    val ipAddress = "$prefix$i"
                    val ping = Ping(ipAddress, scanTimeout).apply {
                        setIpAndDeviceHashMap(ipAndDeviceHashMap)
                    }

                    repeat(reachableRescanCount) {
                        executorService.execute(ping)
                    }
                }
            }

            executorService.shutdown()

            try {
                val completed = executorService.awaitTermination(5, TimeUnit.MINUTES)
                if (completed) {
                    parse(ipAndDeviceHashMap)
                    handler.post {
                        onNetworkScanListener.onComplete(ArrayList(ipAndDeviceHashMap.values))
                    }
                    taskRunning = false
                    return@Thread
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            taskRunning = false
            handler.post { onNetworkScanListener.onFailed() }
        }.start()
    }

    companion object {
        @Volatile
        private var instance: NetworkScanner? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = NetworkScanner(context)
                    }
                }
            }
        }

        fun getInstance(): NetworkScanner = instance!!

        fun scan(onNetworkScanListener: OnNetworkScanListener) {
            instance?.scanNetwork(onNetworkScanListener)
        }

        fun getCurrentIPAddress(): String? = instance?.currentIPAddress
        fun isShowVendorInfo(): Boolean = instance?.showVendorInfo ?: true
        fun setShowVendorInfo(enable: Boolean) {
            instance?.showVendorInfo = enable
        }

        fun isShowMacAddress(): Boolean = instance?.showMacAddress ?: true
        fun setShowMacAddress(enable: Boolean) {
            instance?.showMacAddress = enable
        }

        fun setTimeout(timeout: Int) {
            instance?.setScanTimeout(timeout)
        }

        fun isRunning(): Boolean = instance?.isTaskRunning() ?: false
        fun getReachableRescanCount(): Int = instance?.reachableRescanCount ?: 3
        fun setReachableRescanCount(count: Int) {
            instance?.reachableRescanCount = count
        }

        fun getVendorsJsonArray(): JSONArray? = instance?.vendorsJson
    }
}
