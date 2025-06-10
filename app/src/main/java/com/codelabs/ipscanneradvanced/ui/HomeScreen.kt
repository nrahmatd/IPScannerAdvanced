package com.codelabs.ipscanneradvanced.ui

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.codelabs.ipscanneradvanced.R
import com.codelabs.ipscanneradvanced.getPublicIp
import com.codelabs.ipscanneradvanced.isConnectedToMobileData
import com.codelabs.ipscanneradvanced.isNetworkAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tej.androidnetworktools.lib.Device
import tej.androidnetworktools.lib.scanner.NetworkScanner
import tej.androidnetworktools.lib.scanner.OnNetworkScanListener

@Composable
fun HomeScreen(
    activity: Activity,
    onScanCallback: () -> Unit
) {
    var devicesList by remember { mutableStateOf<List<Device>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(true) }
    var isDeviceEmpty by remember { mutableStateOf(devicesList.isEmpty()) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                onScanCallback()
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
                        activity.runOnUiThread {
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