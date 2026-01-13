package com.tendertracker.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    data object TenderList : Screen()

    @Serializable
    data class TenderDetail(val tenderId: String) : Screen()

    @Serializable
    data object Chat : Screen()

    @Serializable
    data object Settings : Screen()

    @Serializable
    data object ModelManager : Screen()
}
