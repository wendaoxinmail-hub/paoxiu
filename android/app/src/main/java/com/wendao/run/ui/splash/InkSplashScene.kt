package com.wendao.run.ui.splash

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.wendao.run.ui.theme.PaoxiuColors
import kotlin.math.atan2
import kotlin.math.sin

internal data class SplashPathSample(
    val position: Offset,
    val angleRad: Float,
)

internal object InkSplashSceneMath {

    private data class RunnerCurve(
        val p0: Offset,
        val p1: Offset,
        val p2: Offset,
        val p3: Offset,
    )

    private fun curveFor(w: Float, h: Float): RunnerCurve = RunnerCurve(
        p0 = Offset(w * 0.10f, h * 0.78f),
        p1 = Offset(w * 0.34f, h * 0.75f),
        p2 = Offset(w * 0.62f, h * 0.69f),
        p3 = Offset(w * 0.88f, h * 0.63f),
    )

    private fun cubicAt(c: RunnerCurve, t: Float): Offset {
        val u = 1f - t
        val tt = t * t
        val uu = u * u
        return Offset(
            uu * u * c.p0.x + 3f * uu * t * c.p1.x + 3f * u * tt * c.p2.x + tt * t * c.p3.x,
            uu * u * c.p0.y + 3f * uu * t * c.p1.y + 3f * u * tt * c.p2.y + tt * t * c.p3.y,
        )
    }

    private fun cubicTangent(c: RunnerCurve, t: Float): Offset {
        val u = 1f - t
        return Offset(
            3f * u * u * (c.p1.x - c.p0.x) + 6f * u * t * (c.p2.x - c.p1.x) + 3f * t * t * (c.p3.x - c.p2.x),
            3f * u * u * (c.p1.y - c.p0.y) + 6f * u * t * (c.p2.y - c.p1.y) + 3f * t * t * (c.p3.y - c.p2.y),
        )
    }

    /** 按弧长查表，与 getSegment 使用同一套弧长比例 */
    private fun sampleByArcLength(c: RunnerCurve, progress: Float): Pair<Offset, Float> {
        val steps = 96
        var total = 0f
        var prev = cubicAt(c, 0f)
        val cumLen = FloatArray(steps + 1)
        val cumT = FloatArray(steps + 1)
        cumLen[0] = 0f
        cumT[0] = 0f
        for (i in 1..steps) {
            val t = i / steps.toFloat()
            val pt = cubicAt(c, t)
            total += (pt - prev).getDistance()
            cumLen[i] = total
            cumT[i] = t
            prev = pt
        }
        val target = total * progress.coerceIn(0f, 1f)
        var lo = 0
        var hi = steps
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (cumLen[mid] <= target) lo = mid else hi = mid - 1
        }
        if (lo >= steps) {
            val tan = cubicTangent(c, 1f)
            return cubicAt(c, 1f) to atan2(tan.y, tan.x)
        }
        val span = (cumLen[lo + 1] - cumLen[lo]).coerceAtLeast(0.001f)
        val frac = ((target - cumLen[lo]) / span).coerceIn(0f, 1f)
        val t = cumT[lo] + (cumT[lo + 1] - cumT[lo]) * frac
        val pos = cubicAt(c, t)
        val tan = cubicTangent(c, t)
        return pos to atan2(tan.y, tan.x)
    }

    /** 左下（远）→ 右上（近），沿水岸 */
    fun runnerPath(w: Float, h: Float): Path {
        val c = curveFor(w, h)
        return Path().apply {
            moveTo(c.p0.x, c.p0.y)
            cubicTo(c.p1.x, c.p1.y, c.p2.x, c.p2.y, c.p3.x, c.p3.y)
        }
    }

    fun parallaxOffset(runProgress: Float, w: Float, h: Float): Offset =
        Offset(runProgress * w * 0.14f, -runProgress * h * 0.07f)

    fun samplePath(w: Float, h: Float, progress: Float): SplashPathSample {
        val (pos, angle) = sampleByArcLength(curveFor(w, h), progress)
        return SplashPathSample(pos, angle)
    }

    /** 墨痕路径：与人物采样同一弧长算法，端点即人物脚底 */
    fun trailPathUpTo(w: Float, h: Float, progress: Float): Path {
        val path = Path()
        if (progress <= 0.005f) return path
        val c = curveFor(w, h)
        val segments = (72f * progress).toInt().coerceIn(2, 72)
        for (i in 0..segments) {
            val (pos, _) = sampleByArcLength(c, progress * i / segments)
            if (i == 0) path.moveTo(pos.x, pos.y) else path.lineTo(pos.x, pos.y)
        }
        return path
    }

    fun mountainPath(
        w: Float,
        h: Float,
        baseY: Float,
        peakScale: Float,
        offsetX: Float,
        parallax: Float,
    ): Path = Path().apply {
        val ox = offsetX - parallax
        moveTo(-w * 0.12f + ox, h)
        cubicTo(
            w * 0.06f + ox, baseY - h * peakScale,
            w * 0.20f + ox, baseY - h * peakScale * 0.35f,
            w * 0.34f + ox, baseY,
        )
        cubicTo(
            w * 0.50f + ox, baseY - h * peakScale * 0.92f,
            w * 0.66f + ox, baseY - h * peakScale * 0.28f,
            w * 0.80f + ox, baseY,
        )
        cubicTo(
            w * 0.92f + ox, baseY - h * peakScale * 0.62f,
            w * 1.04f + ox, baseY - h * peakScale * 0.18f,
            w * 1.18f + ox, baseY,
        )
        lineTo(w * 1.18f + ox, h)
        close()
    }

    fun waterPath(w: Float, h: Float, parallax: Float, wave: Float): Path = Path().apply {
        val ox = -parallax
        val surface = h * 0.58f
        moveTo(-w * 0.1f + ox, h)
        lineTo(-w * 0.1f + ox, surface)
        cubicTo(
            w * 0.15f + ox, surface - h * 0.02f + wave,
            w * 0.35f + ox, surface + h * 0.015f - wave,
            w * 0.55f + ox, surface - h * 0.01f + wave * 0.6f,
        )
        cubicTo(
            w * 0.75f + ox, surface + h * 0.018f - wave,
            w * 0.95f + ox, surface - h * 0.012f + wave * 0.8f,
            w * 1.2f + ox, surface + wave * 0.4f,
        )
        lineTo(w * 1.2f + ox, h)
        close()
    }

    fun pineTree(center: Offset, scale: Float): Path = Path().apply {
        moveTo(center.x, center.y - 28f * scale)
        lineTo(center.x - 10f * scale, center.y - 6f * scale)
        lineTo(center.x + 10f * scale, center.y - 6f * scale)
        close()
        moveTo(center.x, center.y - 20f * scale)
        lineTo(center.x - 8f * scale, center.y - 2f * scale)
        lineTo(center.x + 8f * scale, center.y - 2f * scale)
        close()
        moveTo(center.x - 2f * scale, center.y - 2f * scale)
        lineTo(center.x - 2f * scale, center.y + 10f * scale)
        lineTo(center.x + 2f * scale, center.y + 10f * scale)
        close()
    }
}

internal fun DrawScope.drawInkLandscape(
    parallax: Offset,
    waterWave: Float,
) {
    val w = size.width
    val h = size.height
    val px = parallax.x
    val py = parallax.y

    drawPath(
        InkSplashSceneMath.mountainPath(w, h, h * 0.62f, 0.26f, w * 0.02f, px * 0.35f),
        PaoxiuColors.InkMountainFar,
    )
    repeat(3) { i ->
        drawCircle(
            color = PaoxiuColors.InkMist.copy(alpha = 0.42f - i * 0.08f),
            radius = w * (0.11f + i * 0.015f),
            center = Offset(w * (0.18f + i * 0.28f) - px * 0.25f + px, h * (0.22f + i * 0.04f) + py),
        )
    }
    drawPath(
        InkSplashSceneMath.mountainPath(w, h, h * 0.70f, 0.18f, -w * 0.04f, px * 0.55f),
        PaoxiuColors.InkMountainMid,
    )
    drawPath(
        InkSplashSceneMath.waterPath(w, h, px * 0.45f, waterWave),
        Brush.verticalGradient(
            colors = listOf(
                PaoxiuColors.InkWater.copy(alpha = 0.55f),
                PaoxiuColors.InkWaterDeep.copy(alpha = 0.72f),
            ),
            startY = h * 0.52f,
            endY = h,
        ),
    )
    val rippleY = h * 0.62f
    repeat(4) { i ->
        val rx = w * (0.12f + i * 0.22f) - px * 0.4f
        drawLine(
            color = PaoxiuColors.InkWaterRipple,
            start = Offset(rx, rippleY + i * 8f),
            end = Offset(rx + w * 0.14f, rippleY + i * 8f + waterWave * 0.3f),
            strokeWidth = 1.2f,
        )
    }
    drawPath(
        InkSplashSceneMath.mountainPath(w, h, h * 0.78f, 0.11f, w * 0.06f, px * 0.85f),
        PaoxiuColors.InkMountainNear,
    )
    listOf(
        Offset(w * 0.14f - px * 0.5f, h * 0.58f),
        Offset(w * 0.62f - px * 0.45f, h * 0.50f),
    ).forEach { center ->
        drawPath(InkSplashSceneMath.pineTree(center, 1f), PaoxiuColors.InkBlack.copy(alpha = 0.35f))
    }
}

internal fun DrawScope.drawInkTrail(trailStroke: Path, trailProgress: Float) {
    if (trailProgress <= 0.01f) return
    val lineW = 1.5f + 4.5f * trailProgress
    val auraW = 6f + 12f * trailProgress
    drawPath(
        trailStroke,
        PaoxiuColors.InkBlack.copy(alpha = 0.08f),
        style = Stroke(width = auraW, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
        trailStroke,
        PaoxiuColors.InkBlack.copy(alpha = 0.40f),
        style = Stroke(width = lineW, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private data class InkBirdSpec(
    val formationOffset: Float,
    val altitude: Float,
    val size: Float,
    val phaseOffset: Float,
)

internal fun DrawScope.drawInkBirds(
    runProgress: Float,
    wingPhase: Float,
    parallax: Offset,
) {
    val w = size.width
    val h = size.height
    val flock = listOf(
        InkBirdSpec(0f, 0.18f, 1.1f, 0f),
        InkBirdSpec(0.06f, 0.16f, 0.95f, 0.35f),
        InkBirdSpec(0.12f, 0.20f, 1f, 0.7f),
    )
    flock.forEach { bird ->
        val t = (runProgress * 0.5f + bird.formationOffset).coerceIn(0f, 1f)
        val cx = w * (0.10f + t * 0.65f) + parallax.x * 0.06f
        val cy = h * (0.14f + bird.altitude * 0.03f) + parallax.y * 0.1f
        val bob = sin(wingPhase * 6.283f + bird.phaseOffset) * 3f
        drawInkBird(
            center = Offset(cx, cy + bob),
            size = bird.size * (w / 360f).coerceIn(0.85f, 1.2f),
            wingPhase = wingPhase + bird.phaseOffset,
        )
    }
}

private fun DrawScope.drawInkBird(
    center: Offset,
    size: Float,
    wingPhase: Float,
) {
    val flap = sin(wingPhase * 6.283f) * 0.22f
    val ink = PaoxiuColors.InkBlack.copy(alpha = 0.62f)
    val span = 16f * size
    val lift = 5f * size * flap

    // 左翅
    drawPath(
        Path().apply {
            moveTo(center.x, center.y)
            quadraticTo(
                center.x - span * 0.55f,
                center.y - lift - span * 0.35f,
                center.x - span,
                center.y - lift + span * 0.08f,
            )
        },
        ink,
        style = Stroke(width = 2.2f * size, cap = StrokeCap.Round),
    )
    // 右翅
    drawPath(
        Path().apply {
            moveTo(center.x, center.y)
            quadraticTo(
                center.x + span * 0.55f,
                center.y - lift - span * 0.35f,
                center.x + span,
                center.y - lift + span * 0.08f,
            )
        },
        ink,
        style = Stroke(width = 2.2f * size, cap = StrokeCap.Round),
    )
    drawCircle(ink.copy(alpha = 0.85f), radius = 1.8f * size, center = center)
}
