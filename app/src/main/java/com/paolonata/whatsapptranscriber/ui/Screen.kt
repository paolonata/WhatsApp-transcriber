package com.paolonata.whatsapptranscriber.ui

sealed class Screen {
    data object Home : Screen()
    data class Detail(val id: Long) : Screen()
}
