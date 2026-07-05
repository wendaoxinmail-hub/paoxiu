package com.wendao.run.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PaoxiuColorScheme = lightColorScheme(
    primary = PaoxiuColors.KeepGreen,
    onPrimary = PaoxiuColors.KeepSurface,
    secondary = PaoxiuColors.ImmortalGold,
    onSecondary = PaoxiuColors.KeepTextPrimary,
    tertiary = PaoxiuColors.InkWash,
    background = PaoxiuColors.KeepCanvas,
    onBackground = PaoxiuColors.KeepTextPrimary,
    surface = PaoxiuColors.KeepSurface,
    onSurface = PaoxiuColors.KeepTextPrimary,
    surfaceVariant = PaoxiuColors.KeepSectionBg,
    onSurfaceVariant = PaoxiuColors.KeepTextSecondary,
    error = PaoxiuColors.HeartDemon,
    outline = PaoxiuColors.KeepDivider,
    primaryContainer = PaoxiuColors.BreakthroughGlow,
    onPrimaryContainer = PaoxiuColors.KeepGreenDark,
)

/**
 * 跑修全局主题：Keep 浅色运动风 + 国风赤金点缀。
 */
@Composable
fun PaoxiuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PaoxiuColorScheme,
        typography = PaoxiuTypography,
        shapes = PaoxiuShapes,
        content = content,
    )
}

// 兼容旧引用
val InkBlack = PaoxiuColors.KeepTextPrimary
val InkBlue = PaoxiuColors.KeepSurface
val SpiritGold = PaoxiuColors.ImmortalGold
val SpiritCyan = PaoxiuColors.SpiritJade
val PaperWhite = PaoxiuColors.KeepSurface
val DemonRed = PaoxiuColors.HeartDemon
