package com.wendao.run.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 字体层级：标题偏国风衬线，数据区偏大字号（对齐 Keep 跑步页可读性）。
 */
val PaoxiuTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        color = PaoxiuColors.KeepTextPrimary,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        color = PaoxiuColors.KeepTextPrimary,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = PaoxiuColors.KeepTextPrimary,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = PaoxiuColors.KeepTextPrimary,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = PaoxiuColors.KeepTextPrimary,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = PaoxiuColors.KeepTextPrimary,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = PaoxiuColors.KeepTextPrimary,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = PaoxiuColors.KeepTextSecondary,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp,
        color = PaoxiuColors.KeepTextPrimary,
    ),
)

/** Keep 风格大号跑步数据 */
val StatNumberStyle = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 32.sp,
    color = PaoxiuColors.KeepTextPrimary,
)

val StatLabelStyle = TextStyle(
    fontSize = 12.sp,
    color = PaoxiuColors.KeepTextSecondary,
    letterSpacing = 1.sp,
)
