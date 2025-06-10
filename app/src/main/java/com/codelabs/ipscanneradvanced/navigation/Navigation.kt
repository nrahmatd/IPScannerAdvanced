package com.codelabs.ipscanneradvanced.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.codelabs.ipscanneradvanced.ui.AboutScreen
import com.codelabs.ipscanneradvanced.ui.HomeScreen
import com.codelabs.ipscanneradvanced.ui.TracerouteScreen
import com.codelabs.ipscanneradvanced.ui.UpdateScreen

@Composable
fun Navigation(
    activity: Activity,
    modifier: Modifier,
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
    onScanCallback: () -> Unit
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(activity = activity) {
                onScanCallback()
            }
        }
        composable(route = Screen.About.route) {
            AboutScreen()
        }
        composable(route = Screen.Update.route) {
            UpdateScreen()
        }
        composable(route = Screen.TraceRoute.route) {
            TracerouteScreen(activity = activity) { onScanCallback() }
        }
    }
}