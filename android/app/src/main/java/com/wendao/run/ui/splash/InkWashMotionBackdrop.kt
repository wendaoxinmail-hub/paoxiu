package com.wendao.run.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import com.wendao.run.ui.theme.PaoxiuColors

/** 引导页静态水墨山水背景（Story / 灵根测试） */
@Composable
fun InkWashMotionBackdrop(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") trailProgress: Float = 1f,
    showTrail: Boolean = false,
) {
    val waterTransition = rememberInfiniteTransition(label = "intro_water")
    val waterWave by waterTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wave",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PaoxiuColors.Parchment,
                        PaoxiuColors.InkWash,
                        PaoxiuColors.ParchmentDeep,
                    ),
                ),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawInkLandscape(
                parallax = if (showTrail) Offset(size.width * 0.12f, 0f) else Offset.Zero,
                waterWave = waterWave,
            )
        }
    }
}
