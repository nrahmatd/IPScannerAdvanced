package com.codelabs.ipscanneradvanced

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import tej.androidnetworktools.lib.Route
import tej.androidnetworktools.lib.scanner.OnTracerouteListener
import tej.androidnetworktools.lib.scanner.Traceroute
import java.net.HttpURLConnection
import java.net.URL

fun isNetworkAvailable(activity: Activity): Boolean {
    val connectivityManager =
        activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}

fun isConnectedToMobileData(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
}

fun getPublicIp(): String? {
    return try {
        val url = URL("https://checkip.amazonaws.com")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.inputStream.bufferedReader().use { it.readLine() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getAppVersion(context: Context): String {
    var result = ""

    try {
        result =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager
                    .getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                    .versionName
                    .orEmpty()
            } else {
                context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName
                    .orEmpty()
            }
        result = result.replace("[a-zA-Z-+.^:,]|-".toRegex(), "")
    } catch (e: PackageManager.NameNotFoundException) {
        Log.d("App Version", "Package name not found")
    }

    return result
}

fun traceroute(
    onRouteAdd: (Route?) -> Unit,
    onComplete: (List<Route?>?) -> Unit,
    onFailed: () -> Unit
) {
    Traceroute.start("google.com", object : OnTracerouteListener {
        override fun onRouteAdd(route: Route?) {
            onRouteAdd(route)
        }

        override fun onComplete(routes: List<Route?>?) {
            onComplete(routes)
        }

        override fun onFailed() {
            onFailed()
        }
    })
}
