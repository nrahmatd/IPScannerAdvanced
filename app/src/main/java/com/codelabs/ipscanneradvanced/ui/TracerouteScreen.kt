package com.codelabs.ipscanneradvanced.ui

import android.app.Activity
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.codelabs.ipscanneradvanced.R
import com.codelabs.ipscanneradvanced.isNetworkAvailable
import com.codelabs.ipscanneradvanced.traceroute

@Composable
fun TracerouteScreen(
    activity: Activity,
    onScanCallback: () -> Unit
) {
    var devicesList by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(true) }
    var isDeviceEmpty by remember { mutableStateOf(devicesList.isEmpty()) }

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
                try {
                    traceroute(
                        onRouteAdd = {
                            isLoading = true
                            devicesList = devicesList.toMutableList().apply { add(it?.ipAddress.orEmpty()) }
                        },
                        onComplete = {
                            isLoading = false
                        },
                        onFailed = {
                            activity.runOnUiThread {
                                Toast.makeText(
                                    activity, "Failed to traceroute",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                } catch (e: Exception) {
                    activity.runOnUiThread {
                        Toast.makeText(
                            activity, e.printStackTrace().toString(),
                            Toast.LENGTH_SHORT
                        ).show()
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
                Text(text = "Traceroute to Google.com")
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
                                        text = "=> IP Address ${devicesList[index]}",
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