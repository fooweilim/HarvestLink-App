package com.example.githubdemo.nav

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController

fun NavHostController.popBackStackSafely(
    fromEntry: NavBackStackEntry?
): Boolean {
    val currentEntry = currentBackStackEntry ?: return false

    if (fromEntry == null || currentEntry.id != fromEntry.id) {
        return false
    }

    if (
        !currentEntry.lifecycle.currentState.isAtLeast(
            Lifecycle.State.RESUMED
        )
    ) {
        return false
    }

    if (previousBackStackEntry == null) {
        return false
    }

    return popBackStack()
}