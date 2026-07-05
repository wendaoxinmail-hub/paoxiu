package com.wendao.run.ui.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.drawable.toBitmap
import com.wendao.run.R
import com.wendao.run.ui.theme.PaoxiuColors
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

private const val TAU = 6.2831855f

data class SplashRunnerFrames(
    val frameA: ImageBitmap,
    val frameB: ImageBitmap,
    /** 脚底锚点（bitmap 像素坐标） */
    val footA: Offset,
    val footB: Offset,
)

@Composable
fun rememberSplashRunnerFrames(): SplashRunnerFrames {
    val context = LocalContext.current
    return remember {
        fun load(id: Int): Pair<ImageBitmap, Offset> {
            val drawable = requireNotNull(context.getDrawable(id)) { "runner frame missing" }
            val bitmap = drawable.toBitmap().asImageBitmap()
            val foot = Offset(bitmap.width / 2f, bitmap.height - 1f)
            return bitmap to foot
        }
        val (a, footA) = load(R.drawable.ic_splash_runner_1)
        val (b, footB) = load(R.drawable.ic_splash_runner_2)
        SplashRunnerFrames(frameA = a, frameB = b, footA = footA, footB = footB)
    }
}

/** 0→1 平滑插值，用于由远及近 */
private fun smoothStep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

internal fun DrawScope.drawCultivatorRunner(
    frames: SplashRunnerFrames,
    footPoint: Offset,
    angleRad: Float,
    stridePhase: Float,
    depthProgress: Float,
) {
    val cycle = stridePhase - floor(stridePhase)
    val frameBlend = (1f - cos(cycle * TAU)) * 0.5f

    val depth = smoothStep(depthProgress)
    val sizeFactor = 0.26f + 0.74f * depth
    val fadeIn = smoothStep((depthProgress / 0.04f).coerceIn(0f, 1f)).coerceAtLeast(0.35f)
    val dstH = size.height * 0.26f * sizeFactor
    val degrees = Math.toDegrees(angleRad.toDouble()).toFloat()
    val bobY = sin(cycle * TAU) * 2.2f * sizeFactor

    val footStrike = if (abs(sin(cycle * TAU)) < 0.22f) {
        (1f - abs(sin(cycle * TAU)) / 0.22f) * depth
    } else {
        0f
    }
    if (footStrike > 0.06f) {
        val foot = Offset(footPoint.x, footPoint.y + bobY + 2f * sizeFactor)
        val a = footStrike * sizeFactor
        drawCircle(
            PaoxiuColors.InkBlack.copy(alpha = 0.22f * a),
            radius = 8f + 10f * sizeFactor * a,
            center = foot,
        )
    }

    fun drawFrame(bitmap: ImageBitmap, foot: Offset, alpha: Float) {
        if (alpha <= 0.01f) return
        val scale = dstH / bitmap.height
        val scaledW = bitmap.width * scale
        val scaledH = bitmap.height * scale

        withTransform({
            translate(footPoint.x, footPoint.y + bobY)
            rotate(degrees, pivot = Offset.Zero)
        }) {
            drawImage(
                image = bitmap,
                dstOffset = IntOffset((-foot.x * scale).toInt(), (-foot.y * scale).toInt()),
                dstSize = IntSize(scaledW.toInt(), scaledH.toInt()),
                alpha = alpha * fadeIn,
            )
        }
    }

    drawFrame(frames.frameA, frames.footA, 1f - frameBlend)
    drawFrame(frames.frameB, frames.footB, frameBlend)
}
