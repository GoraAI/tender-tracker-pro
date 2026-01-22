package com.tendertracker.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    // Auth screens
    @Serializable
    data object Login : Screen()

    @Serializable
    data object Register : Screen()

    // Main screens
    @Serializable
    data object Dashboard : Screen()

    @Serializable
    data object TenderList : Screen()

    @Serializable
    data class TenderDetail(val tenderId: String) : Screen()

    @Serializable
    data class Chat(val tenderId: String? = null) : Screen()

    @Serializable
    data object Settings : Screen()

    @Serializable
    data object ModelManager : Screen()

    // Visualization screens
    @Serializable
    data object Analytics : Screen()

    @Serializable
    data object Compliance : Screen()

    @Serializable
    data object Pipeline : Screen()
}
