package com.reiraku.hyperpower.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {
    @Serializable
    data object Dashboard : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object SuperIslandSettings : Route

    @Serializable
    data object LiveNotificationSettings : Route
}
