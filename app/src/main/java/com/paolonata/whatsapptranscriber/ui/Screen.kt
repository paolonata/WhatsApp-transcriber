package com.paolonata.whatsapptranscriber.ui

sealed class Screen {
    data object Home : Screen()
    data class Detail(val id: Long) : Screen()
    data class Downloading(val progress: Float) : Screen()
    data class Processing(val message: String) : Screen()
    data class Error(val message: String) : Screen()
}
