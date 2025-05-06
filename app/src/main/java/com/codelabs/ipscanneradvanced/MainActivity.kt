package com.codelabs.ipscanneradvanced

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.codelabs.ipscanneradvanced.ui.theme.IPScannerAdvanceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tej.androidnetworktools.lib.Device
import tej.androidnetworktools.lib.scanner.NetworkScanner
import tej.androidnetworktools.lib.scanner.OnNetworkScanListener
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashscreen = installSplashScreen()
        var keepSplashScreen = true
        super.onCreate(savedInstanceState)
        splashscreen.setKeepOnScreenCondition { keepSplashScreen }
        lifecycleScope.launch {
            delay(2000)
            keepSplashScreen = false
        }
        enableEdgeToEdge()
        NetworkScanner.init(this)
        setContent {
            IPScannerAdvanceTheme {
                Navigation(activity = this, navController = rememberNavController())
            }
        }
    }
}

@Composable
fun Navigation(
    activity: Activity,
    navController: NavHostController,
    startDestination: String = "home"
) {
    androidx.navigation.compose.NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("home") {
            IPScannerApp(activity, navController)
        }
        composable("about") {
            AboutScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IPScannerApp(activity: Activity, navController: NavHostController) {
    var devicesList by remember { mutableStateOf<List<Device>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(true) }
    var isDeviceEmpty by remember { mutableStateOf(devicesList.isEmpty()) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                title = { Text("IP Scanner Advanced") },
                actions = {
                    IconButton(onClick = { navController.navigate("about") }) {
                        Image(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.ic_info),
                            contentDescription = null
                        )
                    }
                }
            )
        },
        containerColor = Color.White,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    isConnected = false
                    isDeviceEmpty = false
                    isLoading = true
                    isConnected = isNetworkAvailable(activity = activity)
                    if (!isConnected) {
                        isLoading = false
                        return@Button
                    }
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            if (isConnectedToMobileData(activity)) {
                                val publicIp = getPublicIp()
                                println("HAHAHA $publicIp")
                                publicIp?.let {
                                    devicesList = listOf(
                                        Device(
                                            hostname = "Public IP",
                                            ipAddress = it,
                                            macAddress = "-",
                                            vendorName = "-"
                                        )
                                    )
                                    isLoading = false
                                }
                            } else {
                                NetworkScanner.scan(object : OnNetworkScanListener {
                                    override fun onComplete(devices: List<Device>) {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            withContext(Dispatchers.Main) {
                                                devicesList = devices
                                                isLoading = false
                                            }
                                        }
                                        for (device in devices) {
                                            Log.d(
                                                "HAHAHA", """
                                                Device: ${device.hostname}
                                                IP Address: ${device.ipAddress}
                                                Mac Address: ${device.macAddress}
                                                Vendor Name: ${device.vendorName}
                                                """.trimIndent()
                                            )
                                        }
                                    }

                                    override fun onFailed() {
                                        activity.runOnUiThread {
                                            Toast.makeText(
                                                activity,
                                                "Failed to scan, trying alternative method...",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                        coroutineScope.launch(Dispatchers.IO) {
                                            val publicIp = getPublicIp()
                                            withContext(Dispatchers.Main) {
                                                if (publicIp != null) {
                                                    devicesList = listOf(
                                                        Device(
                                                            hostname = "Public IP",
                                                            ipAddress = publicIp,
                                                            macAddress = "-",
                                                            vendorName = "-"
                                                        )
                                                    )
                                                } else {
                                                    Toast.makeText(
                                                        activity,
                                                        "Failed to get public IP",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                isLoading = false
                                            }
                                        }
                                    }
                                })
                            }
                        } catch (e: Exception) {
                            activity.runOnUiThread() {
                                Toast.makeText(
                                    activity, e.printStackTrace().toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .aspectRatio(1f)
                    )
                } else {
                    Text(text = "Scan Now")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                !isConnected -> {
                    isLoading = false
                    AnimatedVisibility(visible = !isConnected) {
                        EmptyStatePlaceholder(
                            image = R.drawable.ic_connection_not_found,
                            title = "No Connection"
                        )
                    }
                }

                devicesList.isEmpty() -> {
                    isLoading = false
                    AnimatedVisibility(visible = isDeviceEmpty) {
                        EmptyStatePlaceholder(
                            image = R.drawable.ic_no_data,
                            title = ""
                        )
                    }
                }

                else -> {
                    AnimatedVisibility(visible = isDeviceEmpty.not()) {
                        LazyColumn {
                            items(devicesList.size, key = { it }) { index ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "IP: ${devicesList[index].ipAddress}",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "Hostname: ${devicesList[index].hostname}",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "MAC: ${devicesList[index].macAddress}",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "Vendor: ${devicesList[index].vendorName}",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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

@Composable
fun EmptyStatePlaceholder(image: Int, title: String) {
    Column {
        AsyncImage(
            model = image,
            contentDescription = title,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title, textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            modifier = Modifier.size(72.dp),
            model = R.drawable.ic_logo,
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            text = "IP Scanner Advanced"
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            text = "Version 1.0.0"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    IPScannerAdvanceTheme {
//        IPScannerApp(this@MainActivity, navController = rememberNavController())
    }
}