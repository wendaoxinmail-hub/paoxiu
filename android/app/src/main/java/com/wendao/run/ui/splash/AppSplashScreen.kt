package com.wendao.run.ui.splash

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wendao.run.ui.theme.PaoxiuColors
import kotlinx.coroutines.launch

private const val RUN_DURATION_MS = 5800
private const val SPLASH_HOLD_MS = 6200

/**
 * 开屏：修仙者沿路径由远及近慢跑，双帧平滑步态。
 */
@Composable
fun InkRunnerSplashScene(
    modifier: Modifier = Modifier,
    runProgress: Float,
    stridePhase: Float,
    titleAlpha: Float,
    runnerFrames: SplashRunnerFrames,
) {
    val waterTransition = rememberInfiniteTransition(label = "water")
    val waterWave by waterTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wave",
    )
    val wingPhase by waterTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wings",
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
            val w = size.width
            val h = size.height
            val trail = InkSplashSceneMath.trailPathUpTo(w, h, runProgress)
            val sample = InkSplashSceneMath.samplePath(w, h, runProgress)
            val parallax = InkSplashSceneMath.parallaxOffset(runProgress, w, h)

            drawInkLandscape(
                parallax = parallax,
                waterWave = waterWave,
            )
            drawInkTrail(trail, runProgress)
            drawInkBirds(
                runProgress = runProgress,
                wingPhase = wingPhase,
                parallax = parallax,
            )
            drawCultivatorRunner(
                frames = runnerFrames,
                footPoint = sample.position,
                angleRad = sample.angleRad,
                stridePhase = stridePhase,
                depthProgress = runProgress,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
                .alpha(titleAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InkSealLabel(text = "跑修")
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "以足为剑，以汗为丹",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = PaoxiuColors.InkBlack.copy(alpha = 0.75f),
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "跑步即修行",
                style = MaterialTheme.typography.bodyMedium,
                color = PaoxiuColors.InkMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun AppSplashScreen(
    modifier: Modifier = Modifier,
) {
    var runProgress by remember { mutableFloatStateOf(0f) }
    var stridePhase by remember { mutableFloatStateOf(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val runnerFrames = rememberSplashRunnerFrames()

    LaunchedEffect(Unit) {
        launch {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(RUN_DURATION_MS, easing = LinearEasing),
            ) { value, _ -> runProgress = value }
        }
        launch {
            animate(
                initialValue = 0f,
                targetValue = 5.5f,
                animationSpec = tween(RUN_DURATION_MS, easing = LinearEasing),
            ) { value, _ -> stridePhase = value }
        }
        launch {
            titleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(1000, delayMillis = (RUN_DURATION_MS * 0.62f).toInt()),
            )
        }
    }

    InkRunnerSplashScene(
        modifier = modifier,
        runProgress = runProgress,
        stridePhase = stridePhase,
        titleAlpha = titleAlpha.value,
        runnerFrames = runnerFrames,
    )
}

internal val splashHoldMillis: Int = SPLASH_HOLD_MS
