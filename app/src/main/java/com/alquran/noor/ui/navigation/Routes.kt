package com.alquran.noor.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SearchRoute

@Serializable
data object BookmarksRoute

@Serializable
data object SettingsRoute

@Serializable
data class ReaderRoute(val surah: Int, val ayah: Int = 1)
