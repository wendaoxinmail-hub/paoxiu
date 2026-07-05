package com.wendao.run.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Cultivate("cultivate", "修炼", Icons.Outlined.Home),
    Grotto("grotto", "洞府", Icons.Outlined.Landscape),
    Sect("sect", "宗门", Icons.Outlined.Groups),
    Profile("profile", "我", Icons.Outlined.AccountCircle),
}
