package tej.androidnetworktools.lib

import android.content.Context
import android.net.ConnectivityManager

object NetworkConnection {
    @JvmStatic
    fun isWifiConnected(connectivityManager: ConnectivityManager): Boolean {
        val wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        return wifiInfo!!.isConnected
    }

    fun connectivityManager(context: Context): ConnectivityManager {
        return context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
    }

    fun isWifiConnected(context: Context): Boolean {
        return isWifiConnected(connectivityManager(context))
    }

    @JvmStatic
    fun isInternetAvailable(connectivityManager: ConnectivityManager): Boolean {
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    fun isInternetAvailable(context: Context): Boolean {
        return isInternetAvailable(connectivityManager(context))
    }
}
