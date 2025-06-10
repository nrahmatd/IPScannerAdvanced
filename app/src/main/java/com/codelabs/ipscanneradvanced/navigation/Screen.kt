package com.codelabs.ipscanneradvanced.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object TraceRoute : Screen("traceroute")
    object About : Screen("about")
    object Update : Screen("update")
}